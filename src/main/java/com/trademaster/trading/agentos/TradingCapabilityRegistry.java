package com.trademaster.trading.agentos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trading Capability Registry
 * 
 * Tracks performance, health, and usage statistics for all trading capabilities
 * in the TradeMaster Agent ecosystem. Provides real-time metrics for the
 * Agent Orchestration Service to make intelligent routing decisions.
 * 
 * Capabilities Managed:
 * - ORDER_EXECUTION: Real-time order placement and execution tracking
 * - RISK_MANAGEMENT: Risk assessment performance and accuracy metrics
 * - BROKER_ROUTING: Broker selection effectiveness and execution quality
 * - POSITION_TRACKING: Position data accuracy and update latency
 * - COMPLIANCE_CHECK: Regulatory compliance validation speed and accuracy
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradingCapabilityRegistry {
    
    private final Map<String, CapabilityMetrics> capabilityMetrics = new ConcurrentHashMap<>();
    
    /**
     * Initialize capability metrics for trading agent
     */
    public void initializeCapabilities() {
        initializeCapability("ORDER_EXECUTION");
        initializeCapability("RISK_MANAGEMENT");
        initializeCapability("BROKER_ROUTING");
        initializeCapability("POSITION_TRACKING");
        initializeCapability("COMPLIANCE_CHECK");
        
        log.info("Trading capability registry initialized with {} capabilities", 
                capabilityMetrics.size());
    }
    
    /**
     * Records successful execution of a capability
     */
    public void recordSuccessfulExecution(String capability) {
        CapabilityMetrics metrics = capabilityMetrics.get(capability);
        if (metrics != null) {
            metrics.recordSuccess();
            log.debug("Recorded successful execution for capability: {}", capability);
        }
    }
    
    /**
     * Records failed execution of a capability
     */
    public void recordFailedExecution(String capability, Exception error) {
        CapabilityMetrics metrics = capabilityMetrics.get(capability);
        if (metrics != null) {
            metrics.recordFailure(error);
            log.warn("Recorded failed execution for capability: {} - Error: {}", 
                    capability, error.getMessage());
        }
    }
    
    /**
     * Records execution time for performance tracking
     */
    public void recordExecutionTime(String capability, Duration executionTime) {
        CapabilityMetrics metrics = capabilityMetrics.get(capability);
        if (metrics != null) {
            metrics.recordExecutionTime(executionTime);
            log.debug("Recorded execution time for capability: {} - Duration: {}ms", 
                    capability, executionTime.toMillis());
        }
    }
    
    /**
     * Gets current health score for a specific capability
     */
    public Double getCapabilityHealthScore(String capability) {
        CapabilityMetrics metrics = capabilityMetrics.get(capability);
        return metrics != null ? metrics.getHealthScore() : 0.0;
    }
    
    /**
     * Gets success rate for a specific capability
     */
    public Double getCapabilitySuccessRate(String capability) {
        CapabilityMetrics metrics = capabilityMetrics.get(capability);
        return metrics != null ? metrics.getSuccessRate() : 0.0;
    }
    
    /**
     * Gets average execution time for a specific capability
     */
    public Double getCapabilityAverageExecutionTime(String capability) {
        CapabilityMetrics metrics = capabilityMetrics.get(capability);
        return metrics != null ? metrics.getAverageExecutionTime() : 0.0;
    }
    
    /**
     * Calculates overall agent health score across all capabilities
     */
    public Double calculateOverallHealthScore() {
        if (capabilityMetrics.isEmpty()) {
            return 0.0;
        }
        
        double totalHealth = capabilityMetrics.values().stream()
                .mapToDouble(CapabilityMetrics::getHealthScore)
                .sum();
        
        double overallHealth = totalHealth / capabilityMetrics.size();
        
        log.debug("Calculated overall health score: {}", overallHealth);
        return overallHealth;
    }
    
    /**
     * Gets performance summary for all capabilities
     */
    public Map<String, String> getPerformanceSummary() {
        Map<String, String> summary = new ConcurrentHashMap<>();
        
        capabilityMetrics.forEach((capability, metrics) -> {
            summary.put(capability, String.format(
                "Success Rate: %.2f%%, Avg Time: %.2fms, Health: %.2f",
                metrics.getSuccessRate() * 100,
                metrics.getAverageExecutionTime(),
                metrics.getHealthScore()
            ));
        });
        
        return summary;
    }
    
    /**
     * Resets metrics for a specific capability
     */
    public void resetCapabilityMetrics(String capability) {
        CapabilityMetrics metrics = capabilityMetrics.get(capability);
        if (metrics != null) {
            metrics.reset();
            log.info("Reset metrics for capability: {}", capability);
        }
    }
    
    /**
     * Initializes a new capability with default metrics
     */
    private void initializeCapability(String capability) {
        capabilityMetrics.put(capability, new CapabilityMetrics(capability));
        log.debug("Initialized capability: {}", capability);
    }
    
    /**
     * Internal class to track metrics for each capability
     */
    private static class CapabilityMetrics {
        private final String capabilityName;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private volatile LocalDateTime lastExecution = LocalDateTime.now();
        private volatile String lastError = null;
        
        public CapabilityMetrics(String capabilityName) {
            this.capabilityName = capabilityName;
        }
        
        public void recordSuccess() {
            successCount.incrementAndGet();
            lastExecution = LocalDateTime.now();
        }
        
        public void recordFailure(Exception error) {
            failureCount.incrementAndGet();
            lastExecution = LocalDateTime.now();
            lastError = error.getMessage();
        }
        
        public void recordExecutionTime(Duration executionTime) {
            totalExecutionTime.addAndGet(executionTime.toMillis());
            executionCount.incrementAndGet();
        }
        
        public Double getSuccessRate() {
            long total = successCount.get() + failureCount.get();
            return total > 0 ? (double) successCount.get() / total : 1.0;
        }
        
        public Double getAverageExecutionTime() {
            int count = executionCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
        }
        
        public Double getHealthScore() {
            double successRate = getSuccessRate();
            double avgTime = getAverageExecutionTime();
            double recency = getRecencyScore();
            
            // Health score based on success rate (60%), performance (25%), recency (15%)
            return (successRate * 0.60) + 
                   (getPerformanceScore(avgTime) * 0.25) + 
                   (recency * 0.15);
        }
        
        private Double getRecencyScore() {
            Duration timeSinceLastExecution = Duration.between(lastExecution, LocalDateTime.now());
            long minutesSinceExecution = timeSinceLastExecution.toMinutes();
            
            // Score decreases over time, 1.0 if executed within 5 minutes
            if (minutesSinceExecution <= 5) return 1.0;
            if (minutesSinceExecution <= 30) return 0.8;
            if (minutesSinceExecution <= 120) return 0.6;
            if (minutesSinceExecution <= 360) return 0.4;
            return 0.2;
        }
        
        private Double getPerformanceScore(double avgTimeMs) {
            // Score based on average execution time
            if (avgTimeMs <= 50) return 1.0;    // Excellent
            if (avgTimeMs <= 200) return 0.8;   // Good
            if (avgTimeMs <= 500) return 0.6;   // Average
            if (avgTimeMs <= 1000) return 0.4;  // Poor
            return 0.2;  // Very poor
        }
        
        public void reset() {
            successCount.set(0);
            failureCount.set(0);
            totalExecutionTime.set(0);
            executionCount.set(0);
            lastExecution = LocalDateTime.now();
            lastError = null;
        }
    }
}