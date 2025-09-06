package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Order Execution DTO
 * 
 * Comprehensive order execution result with:
 * - Multi-venue execution details
 * - Performance metrics and analytics
 * - Market impact analysis
 * - Execution quality assessment
 * - Real-time status tracking
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderExecution {
    
    /**
     * Execution Identification
     */
    private String executionId;
    private Long orderId;
    private String parentOrderId; // For child orders from algorithms
    private Long userId;
    private String symbol;
    private String exchange;
    private String venue;
    
    /**
     * Order Details
     */
    private String orderType; // MARKET, LIMIT, STOP, etc.
    private String side; // BUY, SELL
    private Integer originalQuantity;
    private Integer executedQuantity;
    private Integer remainingQuantity;
    private BigDecimal limitPrice;
    private BigDecimal stopPrice;
    
    /**
     * Execution Details
     */
    private String executionStatus; // PENDING, PARTIAL, FILLED, CANCELLED, REJECTED
    private BigDecimal executionPrice;
    private BigDecimal averagePrice; // For multiple fills
    private BigDecimal totalValue;
    private String executionAlgorithm; // MARKET_SWEEP, TWAP, VWAP, etc.
    private String executionStrategy; // Strategy used for execution
    
    /**
     * Timing Information
     */
    private Instant orderCreated;
    private Instant orderSent;
    private Instant executionStarted;
    private Instant firstFill;
    private Instant lastFill;
    private Instant executionCompleted;
    
    /**
     * Latency Metrics (in microseconds)
     */
    private LatencyMetrics latencyMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatencyMetrics {
        private Long orderToMarketLatency; // Order creation to market
        private Long marketResponseLatency; // Market response time
        private Long fillReportingLatency; // Fill reporting latency
        private Long totalExecutionLatency; // Total execution time
        private Long venueLatency; // Venue-specific latency
        private Long networkLatency; // Network round-trip time
        private String latencyProfile; // EXCELLENT, GOOD, AVERAGE, POOR
    }
    
    /**
     * Fill Details
     */
    private List<ExecutionFill> fills;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionFill {
        private String fillId;
        private Instant fillTime;
        private Integer fillQuantity;
        private BigDecimal fillPrice;
        private String venue;
        private String counterparty;
        private BigDecimal commission;
        private BigDecimal fees;
        private String fillType; // NORMAL, OPENING, CLOSING, CROSS
        private Long latency; // Fill latency in microseconds
    }
    
    /**
     * Execution Quality Metrics
     */
    private ExecutionQuality executionQuality;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionQuality {
        private BigDecimal implementationShortfall; // Price impact + timing cost
        private BigDecimal marketImpact; // Market impact cost
        private BigDecimal timingCost; // Timing cost
        private BigDecimal commissionCost; // Commission and fees
        private BigDecimal totalCost; // Total execution cost
        private BigDecimal priceImprovement; // Price improvement vs. benchmark
        private String benchmark; // ARRIVAL_PRICE, VWAP, TWAP, CLOSE
        private BigDecimal benchmarkPrice;
        private String qualityRating; // EXCELLENT, GOOD, AVERAGE, POOR
        private BigDecimal executionScore; // 0-100 execution quality score
    }
    
    /**
     * Market Data Context
     */
    private MarketContext marketContext;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketContext {
        private BigDecimal bidPrice;
        private BigDecimal askPrice;
        private BigDecimal spread;
        private BigDecimal midPrice;
        private Long volume;
        private BigDecimal vwap;
        private BigDecimal volatility;
        private String marketCondition; // NORMAL, VOLATILE, ILLIQUID, FAST_MARKET
        private Integer orderBookDepth;
        private BigDecimal orderBookImbalance;
    }
    
    /**
     * Venue Information
     */
    private VenueExecution venueExecution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenueExecution {
        private String venueName;
        private String venueType; // LIT, DARK, ATS, CROSSING_NETWORK
        private String routingDecision; // PRICE, LIQUIDITY, SPEED, COST
        private BigDecimal venueRebate; // Rebate received
        private BigDecimal venueFee; // Fee charged by venue
        private Boolean primaryVenue; // Is this the primary venue
        private String venueOrderId; // Venue-specific order ID
        private Integer venueRank; // Venue ranking for this order
        private BigDecimal venueMarketShare; // Venue's market share for symbol
    }
    
    /**
     * Algorithm Execution Details
     */
    private AlgorithmExecution algorithmExecution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlgorithmExecution {
        private String algorithmType; // TWAP, VWAP, IS, ARRIVAL_PRICE, ICEBERG
        private Map<String, Object> algorithmParameters; // Algorithm-specific parameters
        private Integer childOrdersCreated; // Number of child orders
        private Integer childOrdersCompleted; // Number of completed child orders
        private BigDecimal participationRate; // Market participation rate achieved
        private BigDecimal targetVWAP; // Target VWAP for comparison
        private BigDecimal achievedVWAP; // Actual VWAP achieved
        private String algorithmStatus; // RUNNING, PAUSED, COMPLETED, CANCELLED
        private List<String> algorithmWarnings; // Algorithm warnings/alerts
    }
    
    /**
     * Risk Controls Applied
     */
    private RiskControls riskControls;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskControls {
        private List<String> preTradeChecks; // Pre-trade checks applied
        private List<String> postTradeChecks; // Post-trade checks applied
        private Boolean riskChecksPassed; // All risk checks passed
        private List<String> riskWarnings; // Risk warnings generated
        private String riskOverride; // Risk override reason if any
        private BigDecimal riskScore; // Overall risk score for execution
        private Boolean emergencyStop; // Emergency stop triggered
    }
    
    /**
     * Performance Metrics
     */
    private PerformanceMetrics performanceMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private Long messagesPerSecond; // Message processing rate
        private Long ordersPerSecond; // Order processing rate
        private BigDecimal throughputMBps; // Data throughput
        private Integer concurrentConnections; // Active connections
        private BigDecimal cpuUtilization; // CPU utilization percentage
        private BigDecimal memoryUtilization; // Memory utilization
        private Integer queueDepth; // Order queue depth
        private String performanceProfile; // HIGH, MEDIUM, LOW
    }
    
    /**
     * Regulatory Information
     */
    private RegulatoryInfo regulatoryInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryInfo {
        private String regulatoryReportingId; // Regulatory reporting ID
        private Boolean bestExecutionCompliant; // Best execution compliance
        private String executionReportingVenue; // Venue for regulatory reporting
        private List<String> regulatoryFlags; // Regulatory flags
        private String mifidFlags; // MiFID II reporting flags
        private Boolean systematicInternalizer; // SI execution
        private String clientIdentification; // Client ID for reporting
    }
    
    /**
     * Error Handling
     */
    private ExecutionError executionError;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionError {
        private String errorCode;
        private String errorMessage;
        private String errorCategory; // TECHNICAL, BUSINESS, RISK, REGULATORY
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private Instant errorTime;
        private String errorSource; // VENUE, SYSTEM, NETWORK, RISK_ENGINE
        private Boolean recoverable; // Can the error be recovered
        private String recoveryAction; // Suggested recovery action
        private Integer retryCount; // Number of retries attempted
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if execution is complete
     */
    public boolean isComplete() {
        return "FILLED".equals(executionStatus) || "CANCELLED".equals(executionStatus) || 
               "REJECTED".equals(executionStatus);
    }
    
    /**
     * Check if execution was successful
     */
    public boolean isSuccessful() {
        return "FILLED".equals(executionStatus) || "PARTIAL".equals(executionStatus);
    }
    
    /**
     * Get fill rate percentage
     */
    public BigDecimal getFillRate() {
        if (originalQuantity == null || originalQuantity == 0) {
            return BigDecimal.ZERO;
        }
        Integer executed = executedQuantity != null ? executedQuantity : 0;
        return BigDecimal.valueOf(executed)
               .divide(BigDecimal.valueOf(originalQuantity), 4, java.math.RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Get total execution time in milliseconds
     */
    public Long getExecutionTimeMs() {
        if (orderCreated == null || executionCompleted == null) {
            return null;
        }
        return java.time.Duration.between(orderCreated, executionCompleted).toMillis();
    }
    
    /**
     * Get average fill price
     */
    public BigDecimal getAverageFillPrice() {
        if (fills == null || fills.isEmpty()) {
            return averagePrice;
        }
        
        BigDecimal totalValue = BigDecimal.ZERO;
        Integer totalQuantity = 0;
        
        for (ExecutionFill fill : fills) {
            totalValue = totalValue.add(fill.getFillPrice().multiply(BigDecimal.valueOf(fill.getFillQuantity())));
            totalQuantity += fill.getFillQuantity();
        }
        
        if (totalQuantity == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalValue.divide(BigDecimal.valueOf(totalQuantity), 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Get total commission and fees
     */
    public BigDecimal getTotalCommission() {
        if (fills == null || fills.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return fills.stream()
               .filter(fill -> fill.getCommission() != null)
               .map(ExecutionFill::getCommission)
               .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Check if execution quality is good
     */
    public boolean hasGoodExecutionQuality() {
        if (executionQuality == null || executionQuality.getQualityRating() == null) {
            return false;
        }
        return "EXCELLENT".equals(executionQuality.getQualityRating()) || 
               "GOOD".equals(executionQuality.getQualityRating());
    }
    
    /**
     * Check if execution had high latency
     */
    public boolean hasHighLatency() {
        if (latencyMetrics == null || latencyMetrics.getTotalExecutionLatency() == null) {
            return false;
        }
        return latencyMetrics.getTotalExecutionLatency() > 1000L; // >1ms is high latency
    }
    
    /**
     * Get execution summary for reporting
     */
    public Map<String, Object> getExecutionSummary() {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("executionId", executionId != null ? executionId : "N/A");
        summary.put("symbol", symbol != null ? symbol : "N/A");
        summary.put("side", side != null ? side : "N/A");
        summary.put("executedQuantity", executedQuantity != null ? executedQuantity : 0);
        summary.put("averagePrice", getAverageFillPrice());
        summary.put("totalValue", totalValue != null ? totalValue : BigDecimal.ZERO);
        summary.put("executionStatus", executionStatus != null ? executionStatus : "UNKNOWN");
        summary.put("venue", venue != null ? venue : "N/A");
        summary.put("fillRate", getFillRate());
        summary.put("executionTimeMs", getExecutionTimeMs());
        summary.put("isSuccessful", isSuccessful());
        summary.put("hasGoodQuality", hasGoodExecutionQuality());
        return java.util.Collections.unmodifiableMap(summary);
    }
    
    /**
     * Static factory methods
     */
    public static OrderExecution pending(Long orderId, Long userId, String symbol) {
        return OrderExecution.builder()
            .orderId(orderId)
            .userId(userId)
            .symbol(symbol)
            .executionStatus("PENDING")
            .orderCreated(Instant.now())
            .executionId(generateExecutionId())
            .build();
    }
    
    public static OrderExecution rejected(Long orderId, String reason) {
        return OrderExecution.builder()
            .orderId(orderId)
            .executionStatus("REJECTED")
            .executionError(ExecutionError.builder()
                .errorMessage(reason)
                .errorCategory("BUSINESS")
                .severity("HIGH")
                .errorTime(Instant.now())
                .build())
            .build();
    }
    
    private static String generateExecutionId() {
        return "EXE_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int)(Math.random() * 65536)).toUpperCase();
    }
}