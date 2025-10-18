package com.trademaster.trading.agentos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

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
     * Records successful execution of a capability - eliminates if-statement with Optional
     */
    public void recordSuccessfulExecution(String capability) {
        Optional.ofNullable(capabilityMetrics.get(capability))
            .ifPresent(metrics -> {
                metrics.recordSuccess();
                log.debug("Recorded successful execution for capability: {}", capability);
            });
    }
    
    /**
     * Records failed execution of a capability - eliminates if-statement with Optional
     */
    public void recordFailedExecution(String capability, Exception error) {
        Optional.ofNullable(capabilityMetrics.get(capability))
            .ifPresent(metrics -> {
                metrics.recordFailure(error);
                log.warn("Recorded failed execution for capability: {} - Error: {}",
                        capability, error.getMessage());
            });
    }
    
    /**
     * Records execution time for performance tracking - eliminates if-statement with Optional
     */
    public void recordExecutionTime(String capability, Duration executionTime) {
        Optional.ofNullable(capabilityMetrics.get(capability))
            .ifPresent(metrics -> {
                metrics.recordExecutionTime(executionTime);
                log.debug("Recorded execution time for capability: {} - Duration: {}ms",
                        capability, executionTime.toMillis());
            });
    }
    
    /**
     * Gets current health score for a specific capability - eliminates ternary with Optional
     */
    public Double getCapabilityHealthScore(String capability) {
        return Optional.ofNullable(capabilityMetrics.get(capability))
            .map(CapabilityMetrics::getHealthScore)
            .orElse(0.0);
    }

    /**
     * Gets success rate for a specific capability - eliminates ternary with Optional
     */
    public Double getCapabilitySuccessRate(String capability) {
        return Optional.ofNullable(capabilityMetrics.get(capability))
            .map(CapabilityMetrics::getSuccessRate)
            .orElse(0.0);
    }

    /**
     * Gets average execution time for a specific capability - eliminates ternary with Optional
     */
    public Double getCapabilityAverageExecutionTime(String capability) {
        return Optional.ofNullable(capabilityMetrics.get(capability))
            .map(CapabilityMetrics::getAverageExecutionTime)
            .orElse(0.0);
    }
    
    /**
     * Calculates overall agent health score across all capabilities - eliminates if-statement with Optional
     */
    public Double calculateOverallHealthScore() {
        return Optional.of(capabilityMetrics)
            .filter(metrics -> !metrics.isEmpty())
            .map(metrics -> {
                double totalHealth = metrics.values().stream()
                        .mapToDouble(CapabilityMetrics::getHealthScore)
                        .sum();

                double overallHealth = totalHealth / metrics.size();

                log.debug("Calculated overall health score: {}", overallHealth);
                return overallHealth;
            })
            .orElse(0.0);
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
     * Resets metrics for a specific capability - eliminates if-statement with Optional
     */
    public void resetCapabilityMetrics(String capability) {
        Optional.ofNullable(capabilityMetrics.get(capability))
            .ifPresent(metrics -> {
                metrics.reset();
                log.info("Reset metrics for capability: {}", capability);
            });
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
        
        /**
         * Gets success rate - eliminates ternary with Optional for division-by-zero protection
         */
        public Double getSuccessRate() {
            long total = successCount.get() + failureCount.get();
            return Optional.of(total)
                .filter(t -> t > 0)
                .map(t -> (double) successCount.get() / t)
                .orElse(1.0);
        }

        /**
         * Gets average execution time - eliminates ternary with Optional for division-by-zero protection
         */
        public Double getAverageExecutionTime() {
            int count = executionCount.get();
            return Optional.of(count)
                .filter(c -> c > 0)
                .map(c -> (double) totalExecutionTime.get() / c)
                .orElse(0.0);
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
        
        /**
         * Calculate recency score - eliminates if-else chain with Stream pattern
         */
        private Double getRecencyScore() {
            Duration timeSinceLastExecution = Duration.between(lastExecution, LocalDateTime.now());
            long minutesSinceExecution = timeSinceLastExecution.toMinutes();

            // Score decreases over time - use functional pattern with threshold records
            record TimeThreshold(long maxMinutes, double score) {}

            return Stream.of(
                new TimeThreshold(5, 1.0),    // Within 5 minutes
                new TimeThreshold(30, 0.8),   // Within 30 minutes
                new TimeThreshold(120, 0.6),  // Within 2 hours
                new TimeThreshold(360, 0.4)   // Within 6 hours
            )
            .filter(threshold -> minutesSinceExecution <= threshold.maxMinutes())
            .findFirst()
            .map(TimeThreshold::score)
            .orElse(0.2);  // More than 6 hours
        }
        
        /**
         * Calculate performance score - eliminates if-else chain with Stream pattern
         */
        private Double getPerformanceScore(double avgTimeMs) {
            // Score based on average execution time - use functional pattern with threshold records
            record TimeThreshold(double maxTime, double score) {}

            return Stream.of(
                new TimeThreshold(50, 1.0),     // Excellent
                new TimeThreshold(200, 0.8),    // Good
                new TimeThreshold(500, 0.6),    // Average
                new TimeThreshold(1000, 0.4)    // Poor
            )
            .filter(threshold -> avgTimeMs <= threshold.maxTime())
            .findFirst()
            .map(TimeThreshold::score)
            .orElse(0.2);  // Very poor
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