package com.trademaster.trading.config;

import com.trademaster.trading.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Counter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Security Monitoring Configuration
 * 
 * Provides comprehensive security monitoring with:
 * - Real-time security metrics
 * - Health indicators
 * - Automated reporting
 * - Threat detection monitoring
 * 
 * @author TradeMaster Security Team
 * @version 2.0.0
 */
@Configuration
@EnableScheduling
@Slf4j
public class SecurityMonitoringConfig {
    
    /**
     * Security Health Indicator
     */
    @Component
    @RequiredArgsConstructor
    public static class SecurityHealthIndicator implements HealthIndicator {
        
        private final SecurityAuditService securityAuditService;
        
        @Override
        public Health health() {
            Map<String, Long> metrics = securityAuditService.getSecurityMetrics();
            
            // Evaluate security health
            long failedAttempts = metrics.get("failedAuthenticationAttempts");
            long suspiciousActivity = metrics.get("suspiciousActivityDetected");
            long accessDenied = metrics.get("accessDeniedEvents");
            
            Health.Builder healthBuilder = Health.up();
            
            // Warning conditions
            if (failedAttempts > 50) {
                healthBuilder = Health.down()
                    .withDetail("reason", "High authentication failure rate")
                    .withDetail("failedAttempts", failedAttempts);
            } else if (suspiciousActivity > 10) {
                healthBuilder = Health.down()
                    .withDetail("reason", "High suspicious activity detected")
                    .withDetail("suspiciousEvents", suspiciousActivity);
            } else if (failedAttempts > 20 || suspiciousActivity > 5) {
                healthBuilder = Health.unknown()
                    .withDetail("reason", "Elevated security activity");
            }
            
            return healthBuilder
                .withDetail("totalEvents", metrics.get("totalSecurityEvents"))
                .withDetail("failedAttempts", failedAttempts)
                .withDetail("suspiciousActivity", suspiciousActivity)
                .withDetail("accessDenied", accessDenied)
                .withDetail("uniqueIPs", metrics.get("uniqueIPs"))
                .withDetail("lastCheck", Instant.now())
                .build();
        }
    }
    
    /**
     * Security Metrics Endpoint
     */
    @Component
    @Endpoint(id = "security-metrics")
    @RequiredArgsConstructor
    public static class SecurityMetricsEndpoint {
        
        private final SecurityAuditService securityAuditService;
        
        @ReadOperation
        public Map<String, Object> securityMetrics() {
            Map<String, Long> metrics = securityAuditService.getSecurityMetrics();
            
            return Map.of(
                "timestamp", Instant.now(),
                "metrics", metrics,
                "health", calculateSecurityHealth(metrics),
                "alerts", calculateActiveAlerts(metrics),
                "recommendations", generateRecommendations(metrics)
            );
        }
        
        private String calculateSecurityHealth(Map<String, Long> metrics) {
            long failed = metrics.get("failedAuthenticationAttempts");
            long suspicious = metrics.get("suspiciousActivityDetected");
            
            if (failed > 50 || suspicious > 10) return "CRITICAL";
            if (failed > 20 || suspicious > 5) return "WARNING";
            return "HEALTHY";
        }
        
        private Map<String, Object> calculateActiveAlerts(Map<String, Long> metrics) {
            return Map.of(
                "highFailureRate", metrics.get("failedAuthenticationAttempts") > 20,
                "suspiciousActivity", metrics.get("suspiciousActivityDetected") > 5,
                "accessDeniedSpike", metrics.get("accessDeniedEvents") > 10,
                "uniqueIPsHigh", metrics.get("uniqueIPs") > 100
            );
        }
        
        private Map<String, String> generateRecommendations(Map<String, Long> metrics) {
            return Map.of(
                "authentication", metrics.get("failedAuthenticationAttempts") > 10 ? 
                    "Consider implementing CAPTCHA or account lockout" : "Authentication levels normal",
                "monitoring", metrics.get("suspiciousActivityDetected") > 3 ? 
                    "Review suspicious activity patterns" : "No immediate action required",
                "access", metrics.get("accessDeniedEvents") > 5 ? 
                    "Review access control policies" : "Access patterns normal"
            );
        }
    }
    
    /**
     * Prometheus Security Metrics Registration
     */
    @Component
    @RequiredArgsConstructor
    public static class SecurityMetricsRegistrar {
        
        private final SecurityAuditService securityAuditService;
        private final MeterRegistry meterRegistry;
        
        @Bean
        public String registerSecurityMetrics() {
            // Register Prometheus gauges for security metrics
            Gauge.builder("trading_security_events_total", securityAuditService,
                    service -> service.getSecurityMetrics().get("totalSecurityEvents").doubleValue())
                .description("Total security events processed")
                .register(meterRegistry);
            
            Gauge.builder("trading_authentication_failures_total", securityAuditService,
                    service -> service.getSecurityMetrics().get("failedAuthenticationAttempts").doubleValue())
                .description("Total authentication failures")
                .register(meterRegistry);
            
            Gauge.builder("trading_suspicious_activity_total", securityAuditService,
                    service -> service.getSecurityMetrics().get("suspiciousActivityDetected").doubleValue())
                .description("Total suspicious activities detected")
                .register(meterRegistry);
            
            Gauge.builder("trading_access_denied_total", securityAuditService,
                    service -> service.getSecurityMetrics().get("accessDeniedEvents").doubleValue())
                .description("Total access denied events")
                .register(meterRegistry);
            
            Gauge.builder("trading_unique_ips_active", securityAuditService,
                    service -> service.getSecurityMetrics().get("uniqueIPs").doubleValue())
                .description("Number of unique IPs currently active")
                .register(meterRegistry);
            
            log.info("Security metrics registered with Prometheus");
            return "securityMetricsRegistered";
        }
    }
    
    /**
     * Scheduled Security Monitoring Tasks
     */
    @Component
    @RequiredArgsConstructor
    public static class SecurityMonitoringScheduler {
        
        private final SecurityAuditService securityAuditService;
        private final AtomicLong lastReportTime = new AtomicLong(System.currentTimeMillis());
        
        /**
         * Hourly security metrics report
         */
        @Scheduled(fixedRate = 3600000) // 1 hour
        public void generateHourlySecurityReport() {
            Map<String, Long> metrics = securityAuditService.getSecurityMetrics();
            
            log.info("=== Hourly Security Report ===");
            log.info("Total Security Events: {}", metrics.get("totalSecurityEvents"));
            log.info("Failed Authentication Attempts: {}", metrics.get("failedAuthenticationAttempts"));
            log.info("Suspicious Activities Detected: {}", metrics.get("suspiciousActivityDetected"));
            log.info("Access Denied Events: {}", metrics.get("accessDeniedEvents"));
            log.info("Unique Active IPs: {}", metrics.get("uniqueIPs"));
            log.info("Users with Failed Attempts: {}", metrics.get("usersWithFailures"));
            
            // Check for concerning patterns
            if (metrics.get("failedAuthenticationAttempts") > 20) {
                log.warn("HIGH ALERT: Authentication failure rate is elevated");
            }
            
            if (metrics.get("suspiciousActivityDetected") > 5) {
                log.warn("HIGH ALERT: Multiple suspicious activities detected");
            }
            
            lastReportTime.set(System.currentTimeMillis());
        }
        
        /**
         * Daily security counter reset
         */
        @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
        public void resetDailyCounters() {
            securityAuditService.resetSecurityCounters();
            log.info("Daily security counter reset completed");
        }
        
        /**
         * Security configuration validation
         */
        @Scheduled(fixedRate = 1800000) // 30 minutes
        public void validateSecurityConfiguration() {
            try {
                // Validate security components are functioning
                Map<String, Long> metrics = securityAuditService.getSecurityMetrics();
                
                // Check if metrics are being collected
                if (metrics.get("totalSecurityEvents") == 0) {
                    log.warn("Security metrics collection may not be functioning properly");
                }
                
                log.debug("Security configuration validation completed successfully");
                
            } catch (Exception e) {
                log.error("Security configuration validation failed", e);
            }
        }
        
        /**
         * Threat pattern analysis
         */
        @Scheduled(fixedRate = 900000) // 15 minutes
        public void analyzeThreatPatterns() {
            try {
                Map<String, Long> metrics = securityAuditService.getSecurityMetrics();
                
                // Analyze patterns for automated response
                long recentFailures = metrics.get("failedAuthenticationAttempts");
                long suspiciousEvents = metrics.get("suspiciousActivityDetected");
                
                if (recentFailures > 15 && suspiciousEvents > 3) {
                    log.warn("THREAT ANALYSIS: Coordinated attack pattern detected - " +
                           "Failures: {} Suspicious: {}", recentFailures, suspiciousEvents);
                }
                
                if (metrics.get("uniqueIPs") > 50 && recentFailures > 10) {
                    log.warn("THREAT ANALYSIS: Distributed brute force attack suspected");
                }
                
            } catch (Exception e) {
                log.error("Threat pattern analysis failed", e);
            }
        }
        
        public long getLastReportTime() {
            return lastReportTime.get();
        }
    }
}