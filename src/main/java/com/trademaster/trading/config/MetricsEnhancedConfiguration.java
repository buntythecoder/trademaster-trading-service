package com.trademaster.trading.config;

import com.trademaster.trading.metrics.AlertingService;
import com.trademaster.trading.metrics.TradingMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Metrics Configuration
 * 
 * Configures comprehensive business metrics collection, alerting, and monitoring
 * for the trading service. Integrates with Prometheus and provides real-time
 * alerting capabilities.
 * 
 * Features:
 * - Custom meter registry configuration
 * - Automated metric collection scheduling
 * - Real-time alerting integration
 * - Performance monitoring
 * - SLA compliance tracking
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class MetricsEnhancedConfiguration {
    
    private final TradingMetricsService metricsService;
    private final AlertingService alertingService;
    
    public MetricsEnhancedConfiguration(@Lazy TradingMetricsService metricsService, 
                                      @Lazy AlertingService alertingService) {
        this.metricsService = metricsService;
        this.alertingService = alertingService;
    }
    
    
    /**
     * Scheduled task to monitor performance metrics and trigger alerts
     * Runs every minute to check critical performance indicators
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorPerformanceMetrics() {
        try {
            log.debug("Running scheduled performance metrics monitoring");
            
            CompletableFuture.allOf(
                alertingService.checkOrderProcessingPerformance(),
                alertingService.checkSystemHealth()
            ).join();
            
        } catch (Exception e) {
            log.error("Error in scheduled performance monitoring", e);
        }
    }
    
    /**
     * Scheduled task to monitor risk thresholds
     * Runs every 30 seconds for critical risk monitoring
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void monitorRiskThresholds() {
        try {
            log.debug("Running scheduled risk threshold monitoring");
            
            // Get current risk metrics (in real implementation, these would come from risk service)
            BigDecimal currentExposure = getCurrentTotalExposure();
            BigDecimal dailyPnL = getCurrentDailyPnL();
            
            alertingService.checkRiskThresholds(currentExposure, dailyPnL)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Error monitoring risk thresholds", throwable);
                    }
                });
            
        } catch (Exception e) {
            log.error("Error in scheduled risk monitoring", e);
        }
    }
    
    /**
     * Scheduled task to update business metrics
     * Runs every 5 minutes to refresh calculated metrics
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void updateBusinessMetrics() {
        try {
            log.debug("Updating business metrics");
            
            // Update calculated metrics
            updateCalculatedMetrics();
            
            // Clean up stale alert cooldowns
            int activeCooldowns = alertingService.getActiveCooldowns();
            log.debug("Active alert cooldowns: {}", activeCooldowns);
            
            // Record alerting health
            boolean alertingHealthy = alertingService.isAlertingHealthy();
            log.debug("Alerting system health: {}", alertingHealthy ? "HEALTHY" : "UNHEALTHY");
            
        } catch (Exception e) {
            log.error("Error updating business metrics", e);
        }
    }
    
    /**
     * Scheduled task for daily metrics reset and reporting
     * Runs at midnight to reset daily counters
     */
    @Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
    public void dailyMetricsReset() {
        try {
            log.info("Performing daily metrics reset");
            
            // Reset daily PnL (this would typically be handled by a dedicated service)
            metricsService.updateDailyPnL(BigDecimal.ZERO);
            
            // Log daily summary statistics
            logDailySummary();
            
            log.info("Daily metrics reset completed");
            
        } catch (Exception e) {
            log.error("Error in daily metrics reset", e);
        }
    }
    
    private void updateCalculatedMetrics() {
        // Update complex calculated metrics that require periodic refresh
        // These would typically come from dedicated services in a real implementation
        
        // Update total exposure (example calculation)
        BigDecimal totalExposure = calculateTotalExposure();
        metricsService.updateTotalExposure(totalExposure);
        
        // Update max daily loss threshold
        BigDecimal maxDailyLoss = calculateMaxDailyLoss();
        metricsService.updateMaxDailyLoss(maxDailyLoss);
        
        // Update total PnL
        BigDecimal totalPnL = calculateTotalPnL();
        metricsService.updateTotalPnL(totalPnL);
    }
    
    private void logDailySummary() {
        try {
            BigDecimal successRate = metricsService.getOrderSuccessRate();
            BigDecimal avgProcessingTime = metricsService.getAverageOrderProcessingTime();
            long totalAlerts = alertingService.getTotalAlertsTriggered();
            
            log.info("DAILY SUMMARY - Success Rate: {}%, Avg Processing Time: {}ms, Total Alerts: {}", 
                successRate, avgProcessingTime, totalAlerts);
                
        } catch (Exception e) {
            log.error("Error generating daily summary", e);
        }
    }
    
    // Helper methods for metric calculations
    // In a real implementation, these would integrate with actual trading services
    
    private BigDecimal getCurrentTotalExposure() {
        // This would integrate with portfolio service to get real exposure
        // For now, return a mock value
        return new BigDecimal("500000"); // ₹5L mock exposure
    }
    
    private BigDecimal getCurrentDailyPnL() {
        // This would integrate with PnL service to get real daily P&L
        // For now, return a mock value
        return new BigDecimal("2500"); // ₹2.5K mock daily profit
    }
    
    private BigDecimal calculateTotalExposure() {
        // Mock calculation - would integrate with risk management service
        return getCurrentTotalExposure();
    }
    
    private BigDecimal calculateMaxDailyLoss() {
        // Mock calculation - would be based on portfolio size and risk tolerance
        return new BigDecimal("25000"); // ₹25K max daily loss
    }
    
    private BigDecimal calculateTotalPnL() {
        // Mock calculation - would integrate with accounting service
        return new BigDecimal("125000"); // ₹1.25L total PnL
    }
    
    
    /**
     * Health check endpoint for metrics system
     */
    @Bean
    public TradingMetricsHealthIndicator tradingMetricsHealthIndicator() {
        return new TradingMetricsHealthIndicator(metricsService, alertingService);
    }
    
    /**
     * Custom health indicator for metrics system
     */
    public static class TradingMetricsHealthIndicator 
        implements org.springframework.boot.actuate.health.HealthIndicator {
        
        private final TradingMetricsService metricsService;
        private final AlertingService alertingService;
        
        public TradingMetricsHealthIndicator(TradingMetricsService metricsService, 
                                           AlertingService alertingService) {
            this.metricsService = metricsService;
            this.alertingService = alertingService;
        }
        
        @Override
        public org.springframework.boot.actuate.health.Health health() {
            try {
                boolean alertingHealthy = alertingService.isAlertingHealthy();
                int activeCooldowns = alertingService.getActiveCooldowns();
                long totalAlerts = alertingService.getTotalAlertsTriggered();
                
                if (alertingHealthy) {
                    return org.springframework.boot.actuate.health.Health.up()
                        .withDetail("alerting", "HEALTHY")
                        .withDetail("activeCooldowns", activeCooldowns)
                        .withDetail("totalAlerts", totalAlerts)
                        .withDetail("metricsCollection", "ACTIVE")
                        .build();
                } else {
                    return org.springframework.boot.actuate.health.Health.down()
                        .withDetail("alerting", "UNHEALTHY")
                        .withDetail("activeCooldowns", activeCooldowns)
                        .withDetail("issue", "Too many active alert cooldowns")
                        .build();
                }
                
            } catch (Exception e) {
                return org.springframework.boot.actuate.health.Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        }
    }
}