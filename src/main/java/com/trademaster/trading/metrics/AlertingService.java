package com.trademaster.trading.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trading Alerting Service
 * 
 * Real-time alerting system for trading operations and business metrics.
 * Monitors key performance indicators and triggers alerts when thresholds are breached.
 * 
 * Alert Categories:
 * - Performance Alerts (latency, throughput, error rates)
 * - Risk Management Alerts (exposure limits, loss thresholds)
 * - System Health Alerts (circuit breakers, connectivity issues)
 * - Business Alerts (SLA violations, compliance issues)
 * - Financial Alerts (PnL thresholds, fee anomalies)
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertingService {
    
    private final TradingMetricsService metricsService;
    
    // Alert Configuration Thresholds
    private static final BigDecimal MAX_DAILY_LOSS_THRESHOLD = new BigDecimal("50000"); // ₹50K
    private static final BigDecimal MAX_EXPOSURE_THRESHOLD = new BigDecimal("1000000"); // ₹10L
    private static final long MAX_ORDER_PROCESSING_TIME_MS = 100; // 100ms SLA
    private static final long MAX_RISK_CHECK_TIME_MS = 50; // 50ms SLA
    private static final double MIN_SUCCESS_RATE_PERCENT = 95.0; // 95% success rate
    private static final long MAX_ACTIVE_ORDERS_THRESHOLD = 5000;
    
    // Alert State Management
    private final ConcurrentHashMap<String, Instant> alertCooldowns = new ConcurrentHashMap<>();
    private final AtomicLong totalAlertsTriggered = new AtomicLong(0);
    private static final long ALERT_COOLDOWN_MINUTES = 5;
    
    // Alert Severity Levels
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL, EMERGENCY
    }
    
    // Alert Types
    public enum AlertType {
        PERFORMANCE_DEGRADATION,
        RISK_THRESHOLD_BREACH,
        SYSTEM_HEALTH_ISSUE,
        SLA_VIOLATION,
        FINANCIAL_THRESHOLD_BREACH,
        CIRCUIT_BREAKER_ACTIVATION,
        CONNECTIVITY_ISSUE,
        COMPLIANCE_VIOLATION
    }
    
    /**
     * Check order processing performance and trigger alerts if thresholds are breached
     */
    @Async
    public CompletableFuture<Void> checkOrderProcessingPerformance() {
        return CompletableFuture.runAsync(() -> {
            try {
                BigDecimal avgProcessingTime = metricsService.getAverageOrderProcessingTime();
                
                if (avgProcessingTime.longValue() > MAX_ORDER_PROCESSING_TIME_MS) {
                    triggerAlert(
                        AlertType.PERFORMANCE_DEGRADATION,
                        AlertSeverity.WARNING,
                        "Order Processing SLA Violation",
                        String.format("Average order processing time: %dms exceeds threshold: %dms", 
                            avgProcessingTime.longValue(), MAX_ORDER_PROCESSING_TIME_MS),
                        "order_processing_latency"
                    );
                }
                
                BigDecimal successRate = metricsService.getOrderSuccessRate();
                if (successRate.doubleValue() < MIN_SUCCESS_RATE_PERCENT) {
                    triggerAlert(
                        AlertType.PERFORMANCE_DEGRADATION,
                        AlertSeverity.CRITICAL,
                        "Low Order Success Rate",
                        String.format("Order success rate: %.2f%% below threshold: %.2f%%", 
                            successRate.doubleValue(), MIN_SUCCESS_RATE_PERCENT),
                        "order_success_rate"
                    );
                }
                
            } catch (Exception e) {
                log.error("Error checking order processing performance", e);
            }
        });
    }
    
    /**
     * Monitor risk thresholds and trigger alerts for violations
     */
    @Async
    public CompletableFuture<Void> checkRiskThresholds(BigDecimal currentExposure, BigDecimal dailyPnL) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Check total exposure threshold
                if (currentExposure.compareTo(MAX_EXPOSURE_THRESHOLD) > 0) {
                    triggerAlert(
                        AlertType.RISK_THRESHOLD_BREACH,
                        AlertSeverity.CRITICAL,
                        "Maximum Exposure Exceeded",
                        String.format("Current exposure: ₹%s exceeds maximum threshold: ₹%s", 
                            currentExposure.toString(), MAX_EXPOSURE_THRESHOLD.toString()),
                        "max_exposure_breach"
                    );
                    
                    // Record risk violation in metrics
                    metricsService.recordRiskViolation("EXPOSURE_LIMIT", "CRITICAL");
                }
                
                // Check daily loss threshold
                if (dailyPnL.compareTo(MAX_DAILY_LOSS_THRESHOLD.negate()) < 0) {
                    triggerAlert(
                        AlertType.FINANCIAL_THRESHOLD_BREACH,
                        AlertSeverity.EMERGENCY,
                        "Daily Loss Limit Exceeded",
                        String.format("Daily P&L: ₹%s exceeds maximum loss threshold: ₹%s", 
                            dailyPnL.toString(), MAX_DAILY_LOSS_THRESHOLD.toString()),
                        "daily_loss_limit"
                    );
                    
                    // Record risk violation in metrics
                    metricsService.recordRiskViolation("DAILY_LOSS", "EMERGENCY");
                }
                
            } catch (Exception e) {
                log.error("Error checking risk thresholds", e);
            }
        });
    }
    
    /**
     * Monitor system health and trigger alerts for issues
     */
    @Async
    public CompletableFuture<Void> checkSystemHealth() {
        return CompletableFuture.runAsync(() -> {
            try {
                long totalActiveEntities = metricsService.getTotalActiveEntities();
                
                if (totalActiveEntities > MAX_ACTIVE_ORDERS_THRESHOLD) {
                    triggerAlert(
                        AlertType.SYSTEM_HEALTH_ISSUE,
                        AlertSeverity.WARNING,
                        "High System Load",
                        String.format("Total active entities: %d exceeds threshold: %d", 
                            totalActiveEntities, MAX_ACTIVE_ORDERS_THRESHOLD),
                        "high_system_load"
                    );
                }
                
            } catch (Exception e) {
                log.error("Error checking system health", e);
            }
        });
    }
    
    /**
     * Handle circuit breaker events
     */
    public void handleCircuitBreakerTrip(String serviceName, String reason) {
        metricsService.recordCircuitBreakerTrip(serviceName, reason);
        
        triggerAlert(
            AlertType.CIRCUIT_BREAKER_ACTIVATION,
            AlertSeverity.CRITICAL,
            "Circuit Breaker Activated",
            String.format("Service: %s, Reason: %s", serviceName, reason),
            "circuit_breaker_" + serviceName.toLowerCase()
        );
    }
    
    /**
     * Handle broker connectivity issues
     */
    public void handleBrokerConnectivityIssue(String brokerName, String errorDetails) {
        triggerAlert(
            AlertType.CONNECTIVITY_ISSUE,
            AlertSeverity.WARNING,
            "Broker Connectivity Issue",
            String.format("Broker: %s, Error: %s", brokerName, errorDetails),
            "broker_connectivity_" + brokerName.toLowerCase()
        );
    }
    
    /**
     * Handle SLA violations
     */
    public void handleSLAViolation(String slaType, long actualTime, long slaThreshold) {
        metricsService.recordSLAViolation(slaType, actualTime, slaThreshold);
        
        AlertSeverity severity = actualTime > (slaThreshold * 2) ? 
            AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        
        triggerAlert(
            AlertType.SLA_VIOLATION,
            severity,
            "SLA Threshold Breached",
            String.format("SLA: %s, Actual: %dms, Threshold: %dms", 
                slaType, actualTime, slaThreshold),
            "sla_violation_" + slaType.toLowerCase()
        );
    }
    
    /**
     * Handle compliance violations
     */
    public void handleComplianceViolation(String violationType, String details) {
        triggerAlert(
            AlertType.COMPLIANCE_VIOLATION,
            AlertSeverity.CRITICAL,
            "Compliance Violation Detected",
            String.format("Violation Type: %s, Details: %s", violationType, details),
            "compliance_" + violationType.toLowerCase()
        );
    }
    
    /**
     * Core alert triggering mechanism with cooldown and deduplication
     */
    private void triggerAlert(AlertType alertType, AlertSeverity severity, 
                            String title, String message, String alertKey) {
        
        // Check cooldown period to prevent alert spam
        Instant now = Instant.now();
        Instant lastAlert = alertCooldowns.get(alertKey);
        
        if (lastAlert != null && 
            now.isBefore(lastAlert.plusSeconds(ALERT_COOLDOWN_MINUTES * 60))) {
            log.debug("Alert {} is in cooldown period, skipping", alertKey);
            return;
        }
        
        // Update cooldown
        alertCooldowns.put(alertKey, now);
        totalAlertsTriggered.incrementAndGet();
        
        // Create alert payload
        TradingAlert alert = TradingAlert.builder()
            .alertType(alertType)
            .severity(severity)
            .title(title)
            .message(message)
            .alertKey(alertKey)
            .timestamp(now)
            .correlationId(generateCorrelationId())
            .build();
        
        // Process alert based on severity
        processAlert(alert);
        
        log.info("Alert triggered: type={}, severity={}, key={}, message={}", 
            alertType, severity, alertKey, message);
    }
    
    /**
     * Process alert based on severity level
     */
    private void processAlert(TradingAlert alert) {
        switch (alert.getSeverity()) {
            case EMERGENCY -> {
                // Emergency alerts: Immediate notification + escalation
                sendImmediateNotification(alert);
                escalateToOnCall(alert);
                logCriticalAlert(alert);
            }
            case CRITICAL -> {
                // Critical alerts: Immediate notification + monitoring team
                sendImmediateNotification(alert);
                notifyMonitoringTeam(alert);
                logCriticalAlert(alert);
            }
            case WARNING -> {
                // Warning alerts: Standard notification + monitoring
                sendStandardNotification(alert);
                logStandardAlert(alert);
            }
            case INFO -> {
                // Info alerts: Log only
                logInfoAlert(alert);
            }
        }
        
        // Always record in metrics
        recordAlertMetrics(alert);
    }
    
    private void sendImmediateNotification(TradingAlert alert) {
        // Implementation would integrate with notification service
        log.error("IMMEDIATE ALERT: {} - {}", alert.getTitle(), alert.getMessage());
        
        // In real implementation, this would:
        // - Send SMS to on-call engineers
        // - Send email to critical alerts list
        // - Post to Slack critical channel
        // - Create PagerDuty incident
    }
    
    private void escalateToOnCall(TradingAlert alert) {
        // Implementation would page on-call engineer
        log.error("ESCALATING TO ON-CALL: {} - {}", alert.getTitle(), alert.getMessage());
        
        // In real implementation, this would:
        // - Create high-priority PagerDuty incident
        // - Call on-call rotation phone numbers
        // - Send escalation email to management
    }
    
    private void notifyMonitoringTeam(TradingAlert alert) {
        // Implementation would notify monitoring team
        log.warn("MONITORING TEAM ALERT: {} - {}", alert.getTitle(), alert.getMessage());
        
        // In real implementation, this would:
        // - Send email to monitoring team
        // - Post to monitoring Slack channel
        // - Create JIRA ticket for tracking
    }
    
    private void sendStandardNotification(TradingAlert alert) {
        // Implementation would send standard notification
        log.info("STANDARD ALERT: {} - {}", alert.getTitle(), alert.getMessage());
        
        // In real implementation, this would:
        // - Send email notification
        // - Post to general alerts channel
        // - Update monitoring dashboard
    }
    
    private void logCriticalAlert(TradingAlert alert) {
        log.error("CRITICAL TRADING ALERT [{}] {}: {} | Correlation ID: {}", 
            alert.getSeverity(), alert.getAlertType(), alert.getMessage(), alert.getCorrelationId());
    }
    
    private void logStandardAlert(TradingAlert alert) {
        log.warn("TRADING ALERT [{}] {}: {} | Correlation ID: {}", 
            alert.getSeverity(), alert.getAlertType(), alert.getMessage(), alert.getCorrelationId());
    }
    
    private void logInfoAlert(TradingAlert alert) {
        log.info("TRADING INFO [{}] {}: {} | Correlation ID: {}", 
            alert.getSeverity(), alert.getAlertType(), alert.getMessage(), alert.getCorrelationId());
    }
    
    private void recordAlertMetrics(TradingAlert alert) {
        // Record alert in business metrics
        metricsService.recordRiskAlert(alert.getAlertType().toString(), alert.getAlertKey());
    }
    
    private String generateCorrelationId() {
        return "ALERT-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int) (Math.random() * 65536));
    }
    
    // Alert Statistics and Health Check
    public long getTotalAlertsTriggered() {
        return totalAlertsTriggered.get();
    }
    
    public int getActiveCooldowns() {
        // Clean up expired cooldowns
        Instant cutoff = Instant.now().minusSeconds(ALERT_COOLDOWN_MINUTES * 60);
        alertCooldowns.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        return alertCooldowns.size();
    }
    
    public boolean isAlertingHealthy() {
        // Simple health check: alerting is healthy if we can process alerts
        // and we don't have too many active cooldowns (which might indicate spam)
        return getActiveCooldowns() < 100; // Arbitrary threshold
    }
}