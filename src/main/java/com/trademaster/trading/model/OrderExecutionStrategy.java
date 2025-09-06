package com.trademaster.trading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Order Execution Strategy
 * 
 * Comprehensive execution strategy containing:
 * - Strategy type and parameters
 * - Cost estimates and performance projections
 * - Risk assessment and mitigation factors
 * - Market impact analysis
 * - Execution timeline and milestones
 * - Adaptive adjustment rules
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderExecutionStrategy {
    
    /**
     * Strategy identification
     */
    private String strategyType; // AGGRESSIVE, PATIENT, TWAP, VWAP, ICEBERG, etc.
    private String strategyId; // Unique identifier for this strategy instance
    private String version; // Strategy version for tracking
    private Instant createdAt;
    
    /**
     * Recommended order parameters
     */
    private OrderType recommendedOrderType;
    private TimeInForce recommendedTimeInForce;
    private BigDecimal recommendedLimitPrice;
    private BigDecimal recommendedStopPrice;
    private String recommendedExchange;
    
    /**
     * Execution timing
     */
    private Duration estimatedExecutionTime;
    private Instant recommendedStartTime;
    private Instant recommendedEndTime;
    private Duration maxExecutionWindow;
    private List<ExecutionMilestone> milestones;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionMilestone {
        private String name; // START, QUARTER_COMPLETE, HALF_COMPLETE, etc.
        private Duration targetTime; // Time from start
        private Integer targetQuantityFilled; // Expected quantity by this time
        private BigDecimal targetCostBasisPoints; // Expected cost by this milestone
        private List<String> checkpoints; // What to validate at this milestone
    }
    
    /**
     * Cost estimates (all in basis points unless specified)
     */
    private BigDecimal estimatedImpactCost; // Market impact cost
    private BigDecimal estimatedTimingCost; // Cost of timing delay
    private BigDecimal estimatedSpreadCost; // Bid-ask spread cost
    private BigDecimal estimatedCommissionCost; // Brokerage fees
    private BigDecimal estimatedOpportunityCost; // Cost of unfilled quantity
    private BigDecimal totalEstimatedCost; // Total execution cost
    private BigDecimal costConfidenceInterval; // 95% confidence interval
    
    /**
     * Performance projections
     */
    private BigDecimal expectedFillRatio; // 0.0-1.0
    private BigDecimal expectedExecutionQuality; // 0.0-1.0 quality score
    private Duration expectedTimeToFirstFill;
    private Duration expectedTimeToCompletion;
    private BigDecimal expectedSlippage; // Expected slippage in basis points
    private BigDecimal slippageStandardDeviation;
    
    /**
     * Risk assessment
     */
    private BigDecimal executionRisk; // Overall execution risk score 0.0-1.0
    private BigDecimal marketImpactRisk; // Risk of adverse market impact
    private BigDecimal timingRisk; // Risk from execution timing
    private BigDecimal liquidityRisk; // Risk from insufficient liquidity
    private BigDecimal volatilityRisk; // Risk from price volatility
    private Map<String, BigDecimal> riskFactors; // Additional risk metrics
    
    /**
     * Market conditions assumptions
     */
    private MarketConditions marketAssumptions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketConditions {
        private BigDecimal expectedVolatility; // Expected price volatility
        private BigDecimal expectedVolume; // Expected trading volume
        private BigDecimal expectedSpread; // Expected bid-ask spread
        private BigDecimal liquidityScore; // Market liquidity assessment
        private String marketTrend; // BULLISH, BEARISH, NEUTRAL
        private String marketRegime; // NORMAL, STRESSED, VOLATILE
        private Instant conditionsTimestamp; // When conditions were assessed
    }
    
    /**
     * Strategy parameters
     */
    private Map<String, Object> strategyParameters;
    
    // Common strategy-specific parameters
    private Integer numberOfSlices; // For TWAP/Iceberg
    private BigDecimal participationRate; // For VWAP
    private BigDecimal displayQuantity; // For Iceberg
    private BigDecimal riskAversion; // For Implementation Shortfall
    private Duration sliceInterval; // Time between slices
    private String childOrderType; // Type of child orders
    
    /**
     * Execution rules and constraints
     */
    private ExecutionRules executionRules;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionRules {
        private BigDecimal maxPriceDeviation; // Maximum price deviation allowed
        private BigDecimal maxMarketImpact; // Maximum market impact allowed
        private Duration maxExecutionDelay; // Maximum delay between attempts
        private Integer maxRetries; // Maximum retry attempts
        private BigDecimal minFillSize; // Minimum acceptable fill size
        private String[] allowedExchanges; // Allowed execution venues
        private boolean allowPartialFills; // Whether partial fills are acceptable
        private boolean requireIOC; // Whether to use Immediate-or-Cancel
    }
    
    /**
     * Adaptive adjustment rules
     */
    private List<AdaptationRule> adaptationRules;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdaptationRule {
        private String ruleId;
        private String description;
        private String triggerCondition; // When to trigger this rule
        private String adjustmentType; // PRICE, QUANTITY, TIMING, STRATEGY
        private Map<String, Object> adjustmentParameters;
        private BigDecimal triggerThreshold; // Threshold for triggering
        private Integer maxAdjustments; // Maximum times this rule can fire
        private boolean enabled;
    }
    
    /**
     * Performance monitoring
     */
    private PerformanceMonitoring monitoring;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMonitoring {
        private boolean enableRealTimeMonitoring;
        private Duration monitoringInterval;
        private List<String> kpiMetrics; // Key metrics to monitor
        private Map<String, BigDecimal> alertThresholds; // Alert thresholds
        private boolean enableAdaptiveAdjustment; // Auto-adjust based on performance
        private String escalationPolicy; // When to escalate issues
    }
    
    /**
     * Alternative strategies
     */
    private List<AlternativeStrategy> alternatives;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternativeStrategy {
        private String strategyType;
        private String description;
        private BigDecimal expectedCost;
        private Duration expectedTime;
        private BigDecimal successProbability;
        private String switchCondition; // When to switch to this strategy
        private Map<String, Object> parameters;
    }
    
    /**
     * Strategy confidence and quality metrics
     */
    private BigDecimal confidenceScore; // 0.0-1.0 confidence in strategy
    private BigDecimal strategyQualityScore; // Overall strategy quality
    private String strategyRating; // EXCELLENT, GOOD, FAIR, POOR
    private List<String> confidenceFactors; // Factors affecting confidence
    private List<String> qualityFactors; // Factors affecting quality
    
    /**
     * Execution context
     */
    private Map<String, Object> executionContext; // Additional context data
    private String portfolioContext; // Portfolio-level context
    private String marketContext; // Market-level context
    private String userPreferences; // User-specific preferences
    
    /**
     * Compliance and regulatory
     */
    private List<String> applicableRegulations;
    private Map<String, String> complianceRequirements;
    private boolean requiresApproval;
    private String approvalLevel; // Required approval level
    
    /**
     * Helper methods
     */
    
    public boolean isHighCost() {
        return totalEstimatedCost != null && 
               totalEstimatedCost.compareTo(new BigDecimal("100")) > 0; // 100 bps
    }
    
    public boolean isHighRisk() {
        return executionRisk != null && 
               executionRisk.compareTo(new BigDecimal("0.7")) > 0;
    }
    
    public boolean isComplexStrategy() {
        return List.of("TWAP", "VWAP", "ICEBERG", "IMPLEMENTATION_SHORTFALL", "BRACKET")
                   .contains(strategyType);
    }
    
    public boolean requiresRealTimeMonitoring() {
        return monitoring != null && monitoring.isEnableRealTimeMonitoring();
    }
    
    public boolean hasAlternatives() {
        return alternatives != null && !alternatives.isEmpty();
    }
    
    public Duration getEstimatedTimeToCompletion() {
        return expectedTimeToCompletion != null ? expectedTimeToCompletion : estimatedExecutionTime;
    }
    
    public BigDecimal getTotalExpectedCost() {
        return totalEstimatedCost != null ? totalEstimatedCost : BigDecimal.ZERO;
    }
    
    public String getStrategyComplexityLevel() {
        if (isComplexStrategy()) {
            return "HIGH";
        } else if ("PATIENT".equals(strategyType)) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    public boolean shouldAdaptDuringExecution() {
        return adaptationRules != null && 
               adaptationRules.stream().anyMatch(AdaptationRule::isEnabled);
    }
    
    /**
     * Calculate strategy score based on cost, risk, and confidence
     */
    public BigDecimal calculateStrategyScore() {
        BigDecimal costScore = BigDecimal.ONE;
        if (totalEstimatedCost != null) {
            // Lower cost = higher score (inverse relationship)
            costScore = BigDecimal.ONE.subtract(
                totalEstimatedCost.divide(new BigDecimal("1000"), 2, java.math.RoundingMode.HALF_UP)
            ).max(BigDecimal.ZERO);
        }
        
        BigDecimal riskScore = BigDecimal.ONE;
        if (executionRisk != null) {
            // Lower risk = higher score (inverse relationship)
            riskScore = BigDecimal.ONE.subtract(executionRisk);
        }
        
        BigDecimal confScore = confidenceScore != null ? confidenceScore : new BigDecimal("0.5");
        
        // Weighted average: cost 40%, risk 30%, confidence 30%
        return costScore.multiply(new BigDecimal("0.4"))
            .add(riskScore.multiply(new BigDecimal("0.3")))
            .add(confScore.multiply(new BigDecimal("0.3")));
    }
    
    /**
     * Get strategy recommendation summary
     */
    public String getRecommendationSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Strategy: ").append(strategyType).append("\n");
        summary.append("Expected Cost: ").append(totalEstimatedCost).append(" bps\n");
        summary.append("Expected Time: ").append(estimatedExecutionTime != null ? 
                                                estimatedExecutionTime.toMinutes() + " minutes" : "N/A").append("\n");
        summary.append("Risk Level: ").append(isHighRisk() ? "HIGH" : "NORMAL").append("\n");
        summary.append("Confidence: ").append(confidenceScore).append("\n");
        summary.append("Quality: ").append(strategyQualityScore).append("\n");
        
        return summary.toString();
    }
    
    /**
     * Static factory methods
     */
    public static OrderExecutionStrategy aggressive() {
        return OrderExecutionStrategy.builder()
            .strategyType("AGGRESSIVE")
            .recommendedOrderType(OrderType.MARKET)
            .recommendedTimeInForce(TimeInForce.IOC)
            .estimatedExecutionTime(Duration.ofSeconds(30))
            .expectedFillRatio(new BigDecimal("0.95"))
            .totalEstimatedCost(new BigDecimal("20")) // 20 bps
            .executionRisk(new BigDecimal("0.3"))
            .confidenceScore(new BigDecimal("0.8"))
            .createdAt(Instant.now())
            .build();
    }
    
    public static OrderExecutionStrategy patient() {
        return OrderExecutionStrategy.builder()
            .strategyType("PATIENT")
            .recommendedOrderType(OrderType.LIMIT)
            .recommendedTimeInForce(TimeInForce.GTC)
            .estimatedExecutionTime(Duration.ofMinutes(30))
            .expectedFillRatio(new BigDecimal("0.85"))
            .totalEstimatedCost(new BigDecimal("10")) // 10 bps
            .executionRisk(new BigDecimal("0.2"))
            .confidenceScore(new BigDecimal("0.9"))
            .createdAt(Instant.now())
            .build();
    }
}