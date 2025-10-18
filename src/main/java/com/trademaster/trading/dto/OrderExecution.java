package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * Get fill rate percentage - eliminates if-statement and ternary with Optional
     */
    public BigDecimal getFillRate() {
        return Optional.ofNullable(originalQuantity)
            .filter(qty -> qty != 0)
            .map(original -> {
                Integer executed = Optional.ofNullable(executedQuantity).orElse(0);
                return BigDecimal.valueOf(executed)
                    .divide(BigDecimal.valueOf(original), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            })
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Get total execution time in milliseconds - eliminates if-statement with Optional
     */
    public Long getExecutionTimeMs() {
        return Optional.ofNullable(orderCreated)
            .flatMap(created -> Optional.ofNullable(executionCompleted)
                .map(completed -> java.time.Duration.between(created, completed).toMillis()))
            .orElse(null);
    }
    
    /**
     * Get average fill price - eliminates if-statements and for loop with Optional and Stream
     */
    public BigDecimal getAverageFillPrice() {
        return Optional.ofNullable(fills)
            .filter(f -> !f.isEmpty())
            .map(fillList -> {
                BigDecimal totalValue = fillList.stream()
                    .map(fill -> fill.getFillPrice().multiply(BigDecimal.valueOf(fill.getFillQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                Integer totalQuantity = fillList.stream()
                    .map(ExecutionFill::getFillQuantity)
                    .reduce(0, Integer::sum);

                return Optional.of(totalQuantity)
                    .filter(qty -> qty != 0)
                    .map(qty -> totalValue.divide(BigDecimal.valueOf(qty), 4, java.math.RoundingMode.HALF_UP))
                    .orElse(BigDecimal.ZERO);
            })
            .orElse(averagePrice);
    }
    
    /**
     * Get total commission and fees - eliminates if-statement with Optional
     */
    public BigDecimal getTotalCommission() {
        return Optional.ofNullable(fills)
            .filter(f -> !f.isEmpty())
            .map(fillList -> fillList.stream()
                .filter(fill -> fill.getCommission() != null)
                .map(ExecutionFill::getCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Check if execution quality is good - eliminates if-statement with Optional
     */
    public boolean hasGoodExecutionQuality() {
        return Optional.ofNullable(executionQuality)
            .flatMap(quality -> Optional.ofNullable(quality.getQualityRating())
                .map(rating -> "EXCELLENT".equals(rating) || "GOOD".equals(rating)))
            .orElse(false);
    }
    
    /**
     * Check if execution had high latency - eliminates if-statement with Optional
     */
    public boolean hasHighLatency() {
        return Optional.ofNullable(latencyMetrics)
            .flatMap(metrics -> Optional.ofNullable(metrics.getTotalExecutionLatency())
                .map(latency -> latency > 1000L)) // >1ms is high latency
            .orElse(false);
    }
    
    /**
     * Get execution summary for reporting - eliminates ternary operators with Optional
     */
    public Map<String, Object> getExecutionSummary() {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("executionId", Optional.ofNullable(executionId).orElse("N/A"));
        summary.put("symbol", Optional.ofNullable(symbol).orElse("N/A"));
        summary.put("side", Optional.ofNullable(side).orElse("N/A"));
        summary.put("executedQuantity", Optional.ofNullable(executedQuantity).orElse(0));
        summary.put("averagePrice", getAverageFillPrice());
        summary.put("totalValue", Optional.ofNullable(totalValue).orElse(BigDecimal.ZERO));
        summary.put("executionStatus", Optional.ofNullable(executionStatus).orElse("UNKNOWN"));
        summary.put("venue", Optional.ofNullable(venue).orElse("N/A"));
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