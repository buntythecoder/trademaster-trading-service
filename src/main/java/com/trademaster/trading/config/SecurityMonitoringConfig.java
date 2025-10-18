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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

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

            // Eliminates if-else chain with Stream pattern using threshold records
            record HealthThreshold(java.util.function.Predicate<Map<String, Long>> condition,
                                  java.util.function.Function<Map<String, Long>, Health.Builder> builder) {}

            Health.Builder healthBuilder = Stream.of(
                new HealthThreshold(
                    m -> m.get("failedAuthenticationAttempts") > 50,
                    m -> Health.down()
                        .withDetail("reason", "High authentication failure rate")
                        .withDetail("failedAttempts", m.get("failedAuthenticationAttempts"))),
                new HealthThreshold(
                    m -> m.get("suspiciousActivityDetected") > 10,
                    m -> Health.down()
                        .withDetail("reason", "High suspicious activity detected")
                        .withDetail("suspiciousEvents", m.get("suspiciousActivityDetected"))),
                new HealthThreshold(
                    m -> m.get("failedAuthenticationAttempts") > 20 || m.get("suspiciousActivityDetected") > 5,
                    m -> Health.unknown()
                        .withDetail("reason", "Elevated security activity"))
            )
            .filter(threshold -> threshold.condition().test(metrics))
            .findFirst()
            .map(threshold -> threshold.builder().apply(metrics))
            .orElse(Health.up());

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

            // Eliminates if-else chain with Stream pattern using threshold records
            record HealthLevel(java.util.function.Predicate<Map<String, Long>> condition, String level) {}

            return Stream.of(
                new HealthLevel(m -> m.get("failedAuthenticationAttempts") > 50 ||
                                    m.get("suspiciousActivityDetected") > 10, "CRITICAL"),
                new HealthLevel(m -> m.get("failedAuthenticationAttempts") > 20 ||
                                    m.get("suspiciousActivityDetected") > 5, "WARNING")
            )
            .filter(level -> level.condition().test(metrics))
            .findFirst()
            .map(HealthLevel::level)
            .orElse("HEALTHY");
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
            // Eliminates all 3 ternaries with Stream pattern using threshold records
            record RecommendationThreshold(String key, long threshold, String metricKey,
                                          String actionMessage, String normalMessage) {
                String getMessage(Map<String, Long> m) {
                    return Optional.of(m.get(metricKey) > threshold)
                        .filter(Boolean::booleanValue)
                        .map(exceeded -> actionMessage)
                        .orElse(normalMessage);
                }
            }

            return Stream.of(
                new RecommendationThreshold("authentication", 10, "failedAuthenticationAttempts",
                    "Consider implementing CAPTCHA or account lockout", "Authentication levels normal"),
                new RecommendationThreshold("monitoring", 3, "suspiciousActivityDetected",
                    "Review suspicious activity patterns", "No immediate action required"),
                new RecommendationThreshold("access", 5, "accessDeniedEvents",
                    "Review access control policies", "Access patterns normal")
            )
            .collect(java.util.stream.Collectors.toMap(
                RecommendationThreshold::key,
                rec -> rec.getMessage(metrics)
            ));
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
         * Hourly security metrics report - eliminates if-statements with Optional
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

            // Check for concerning patterns - eliminates if-statements with Optional
            Optional.of(metrics.get("failedAuthenticationAttempts"))
                .filter(attempts -> attempts > 20)
                .ifPresent(attempts -> log.warn("HIGH ALERT: Authentication failure rate is elevated"));

            Optional.of(metrics.get("suspiciousActivityDetected"))
                .filter(activities -> activities > 5)
                .ifPresent(activities -> log.warn("HIGH ALERT: Multiple suspicious activities detected"));

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
         * Security configuration validation - eliminates if-statement with Optional
         */
        @Scheduled(fixedRate = 1800000) // 30 minutes
        public void validateSecurityConfiguration() {
            try {
                // Validate security components are functioning
                Map<String, Long> metrics = securityAuditService.getSecurityMetrics();

                // Check if metrics are being collected - eliminates if-statement with Optional
                Optional.of(metrics.get("totalSecurityEvents"))
                    .filter(events -> events == 0)
                    .ifPresent(events -> log.warn("Security metrics collection may not be functioning properly"));

                log.debug("Security configuration validation completed successfully");

            } catch (Exception e) {
                log.error("Security configuration validation failed", e);
            }
        }
        
        /**
         * Threat pattern analysis - eliminates if-statements with Optional
         */
        @Scheduled(fixedRate = 900000) // 15 minutes
        public void analyzeThreatPatterns() {
            try {
                Map<String, Long> metrics = securityAuditService.getSecurityMetrics();

                // Analyze patterns for automated response
                long recentFailures = metrics.get("failedAuthenticationAttempts");
                long suspiciousEvents = metrics.get("suspiciousActivityDetected");

                // Eliminates if-statement with Optional pattern
                Optional.of(recentFailures)
                    .filter(failures -> failures > 15 && suspiciousEvents > 3)
                    .ifPresent(failures -> log.warn("THREAT ANALYSIS: Coordinated attack pattern detected - " +
                           "Failures: {} Suspicious: {}", failures, suspiciousEvents));

                // Eliminates if-statement with Optional pattern
                Optional.of(metrics.get("uniqueIPs"))
                    .filter(ips -> ips > 50 && recentFailures > 10)
                    .ifPresent(ips -> log.warn("THREAT ANALYSIS: Distributed brute force attack suspected"));

            } catch (Exception e) {
                log.error("Threat pattern analysis failed", e);
            }
        }
        
        public long getLastReportTime() {
            return lastReportTime.get();
        }
    }
}