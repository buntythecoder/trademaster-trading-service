package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Execution Report DTO
 * 
 * Comprehensive execution performance report with:
 * - Execution quality analytics
 * - Performance benchmarking
 * - Cost analysis and attribution
 * - Venue performance comparison
 * - Algorithm effectiveness metrics
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionReport {
    
    /**
     * Report Metadata
     */
    private String reportId;
    private Long userId;
    private String reportType; // REAL_TIME, DAILY, WEEKLY, MONTHLY, CUSTOM
    private LocalDate reportDate;
    private Instant generatedAt;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    /**
     * Executive Summary
     */
    private ExecutiveSummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutiveSummary {
        private Integer totalOrders; // Total orders executed
        private Integer completedOrders; // Successfully completed orders
        private Long totalVolume; // Total volume executed
        private BigDecimal totalValue; // Total value executed
        private BigDecimal overallFillRate; // Overall fill rate percentage
        private BigDecimal averageExecutionTime; // Average execution time in seconds
        private String executionQuality; // EXCELLENT, GOOD, AVERAGE, POOR
        private BigDecimal totalSavings; // Total cost savings vs. benchmark
        private String topPerformingVenue; // Best performing venue
        private String topStrategy; // Most used execution strategy
    }
    
    /**
     * Execution Performance Metrics
     */
    private PerformanceMetrics performance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private BigDecimal averageLatency; // Average execution latency (ms)
        private BigDecimal medianLatency; // Median execution latency
        private BigDecimal p95Latency; // 95th percentile latency
        private BigDecimal p99Latency; // 99th percentile latency
        private Long messagesPerSecond; // Peak messages per second
        private Long ordersPerSecond; // Peak orders per second
        private BigDecimal systemUptime; // System uptime percentage
        private Integer errorRate; // Error rate per 1000 orders
        private BigDecimal throughputMBps; // Data throughput MB/s
        private String performanceGrade; // A, B, C, D, F
    }
    
    /**
     * Execution Quality Analysis
     */
    private ExecutionQuality executionQuality;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionQuality {
        private BigDecimal averageImplementationShortfall; // Average IS
        private BigDecimal medianImplementationShortfall; // Median IS
        private BigDecimal averageSlippage; // Average slippage vs. arrival price
        private BigDecimal priceImprovementRate; // % orders with price improvement
        private BigDecimal averagePriceImprovement; // Average improvement amount
        private BigDecimal marketImpact; // Average market impact
        private BigDecimal timingCost; // Average timing cost
        private BigDecimal opportunityCost; // Average opportunity cost
        private String qualityTrend; // IMPROVING, STABLE, DECLINING
        private BigDecimal benchmarkOutperformance; // vs. market benchmarks
    }
    
    /**
     * Cost Analysis
     */
    private CostAnalysis costAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostAnalysis {
        private BigDecimal totalExecutionCosts; // Total execution costs
        private BigDecimal explicitCosts; // Commissions and fees
        private BigDecimal implicitCosts; // Market impact and slippage
        private BigDecimal averageCostPerOrder; // Average cost per order
        private BigDecimal costAsPercentOfValue; // Cost as % of traded value
        private BigDecimal commissionsAndFees; // Total commissions and fees
        private BigDecimal marketImpactCosts; // Market impact costs
        private BigDecimal timingCosts; // Timing-related costs
        private BigDecimal costSavingsVsBenchmark; // Savings vs. benchmark
        private String costEfficiency; // EXCELLENT, GOOD, AVERAGE, POOR
    }
    
    /**
     * Venue Performance Analysis
     */
    private List<VenuePerformance> venuePerformance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenuePerformance {
        private String venueName;
        private String venueType; // LIT, DARK, ATS, CROSSING
        private Integer orderCount; // Orders sent to venue
        private BigDecimal fillRate; // Fill rate at venue
        private BigDecimal averageLatency; // Average venue latency
        private BigDecimal executionQuality; // Execution quality score
        private BigDecimal marketShare; // Market share captured
        private BigDecimal costEffectiveness; // Cost effectiveness score
        private BigDecimal priceImprovement; // Average price improvement
        private String venueRating; // A, B, C, D, F
        private String performanceTrend; // IMPROVING, STABLE, DECLINING
    }
    
    /**
     * Algorithm Performance Analysis
     */
    private List<AlgorithmPerformance> algorithmPerformance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlgorithmPerformance {
        private String algorithmName;
        private String algorithmType; // TWAP, VWAP, IS, etc.
        private Integer ordersExecuted; // Orders using this algorithm
        private BigDecimal averageExecutionTime; // Average execution time
        private BigDecimal fillRateAchieved; // Fill rate achieved
        private BigDecimal benchmarkPerformance; // vs. algorithm benchmark
        private BigDecimal implementationShortfall; // Average IS for algorithm
        private BigDecimal participationRateAchieved; // Actual participation rate
        private String algorithmEffectiveness; // HIGH, MEDIUM, LOW
        private List<String> optimizationSuggestions; // Improvement suggestions
    }
    
    /**
     * Time-Based Analysis
     */
    private List<TimePerformance> timeAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimePerformance {
        private String timeSlot; // Hour of day or day of week
        private Integer orderCount; // Orders in this time slot
        private BigDecimal averageLatency; // Average latency
        private BigDecimal executionQuality; // Execution quality
        private BigDecimal fillRate; // Fill rate
        private String marketCondition; // Market condition during slot
        private BigDecimal volatility; // Average volatility
        private String performance; // EXCELLENT, GOOD, AVERAGE, POOR
    }
    
    /**
     * Symbol/Sector Performance
     */
    private List<SymbolPerformance> symbolPerformance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymbolPerformance {
        private String symbol;
        private String sector;
        private String industry;
        private Integer orderCount; // Orders for this symbol
        private BigDecimal totalVolume; // Volume traded
        private BigDecimal averageExecutionQuality; // Average quality score
        private BigDecimal marketImpact; // Average market impact
        private String bestVenue; // Best performing venue for symbol
        private String optimalStrategy; // Optimal execution strategy
        private List<String> recommendations; // Symbol-specific recommendations
    }
    
    /**
     * Risk Analysis
     */
    private RiskAnalysis riskAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAnalysis {
        private Integer riskViolations; // Number of risk violations
        private Integer blockedOrders; // Orders blocked by risk controls
        private BigDecimal averageRiskScore; // Average risk score
        private String riskProfile; // CONSERVATIVE, MODERATE, AGGRESSIVE
        private List<String> topRiskFactors; // Top risk factors
        private Integer emergencyStops; // Emergency stops triggered
        private BigDecimal riskAdjustedReturns; // Risk-adjusted returns
        private String riskTrend; // IMPROVING, STABLE, INCREASING
    }
    
    /**
     * Market Conditions Impact
     */
    private MarketConditionsImpact marketConditions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketConditionsImpact {
        private BigDecimal averageVolatility; // Average market volatility
        private BigDecimal averageSpread; // Average bid-ask spread
        private String predominantRegime; // Market regime during period
        private Integer haltedSymbols; // Symbols that were halted
        private Integer newsImpactedTrades; // Trades affected by news
        private BigDecimal liquidityScore; // Average market liquidity
        private List<String> challengingConditions; // Challenging market conditions
        private BigDecimal adaptationEffectiveness; // How well algos adapted
    }
    
    /**
     * Benchmarking
     */
    private Benchmarking benchmarking;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Benchmarking {
        private String primaryBenchmark; // ARRIVAL_PRICE, VWAP, TWAP
        private BigDecimal benchmarkOutperformance; // Outperformance vs. benchmark
        private BigDecimal industryPercentile; // Industry percentile ranking
        private String peerComparison; // vs. peer brokers/firms
        private List<BenchmarkMetric> benchmarkDetails; // Detailed benchmarks
        private String competitivePosition; // LEADING, COMPETITIVE, LAGGING
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkMetric {
        private String metricName;
        private BigDecimal ourValue;
        private BigDecimal benchmarkValue;
        private BigDecimal difference;
        private String performance; // OUTPERFORM, INLINE, UNDERPERFORM
    }
    
    /**
     * Recommendations and Action Items
     */
    private List<Recommendation> recommendations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String category; // VENUE, ALGORITHM, TIMING, RISK
        private String priority; // HIGH, MEDIUM, LOW
        private String recommendation;
        private String rationale;
        private BigDecimal estimatedImpact; // Estimated improvement
        private String implementationComplexity; // LOW, MEDIUM, HIGH
        private Integer estimatedTimeDays; // Implementation time
        private List<String> actionItems; // Specific action items
    }
    
    /**
     * Performance Trends
     */
    private List<PerformanceTrend> trends;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceTrend {
        private String metric; // LATENCY, FILL_RATE, EXECUTION_QUALITY
        private String trendDirection; // IMPROVING, STABLE, DECLINING
        private BigDecimal changePercent; // Change percentage
        private String timeFrame; // DAILY, WEEKLY, MONTHLY
        private List<BigDecimal> historicalValues; // Historical values
        private String forecast; // SHORT_TERM forecast
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Get overall execution grade
     */
    public String getOverallGrade() {
        if (performance != null && performance.getPerformanceGrade() != null) {
            return performance.getPerformanceGrade();
        }
        return "N/A";
    }
    
    /**
     * Check if performance is improving
     */
    public boolean isPerformanceImproving() {
        if (trends == null) return false;
        
        long improvingTrends = trends.stream()
            .filter(trend -> "IMPROVING".equals(trend.getTrendDirection()))
            .count();
        
        return improvingTrends > trends.size() / 2; // Majority improving
    }
    
    /**
     * Get top recommendation by priority
     */
    public List<Recommendation> getHighPriorityRecommendations() {
        if (recommendations == null) return List.of();
        
        return recommendations.stream()
            .filter(rec -> "HIGH".equals(rec.getPriority()))
            .toList();
    }
    
    /**
     * Get best performing venue
     */
    public String getBestVenue() {
        if (venuePerformance == null || venuePerformance.isEmpty()) {
            return "N/A";
        }
        
        return venuePerformance.stream()
            .max((v1, v2) -> {
                if (v1.getExecutionQuality() == null) return -1;
                if (v2.getExecutionQuality() == null) return 1;
                return v1.getExecutionQuality().compareTo(v2.getExecutionQuality());
            })
            .map(VenuePerformance::getVenueName)
            .orElse("N/A");
    }
    
    /**
     * Calculate total cost savings
     */
    public BigDecimal getTotalCostSavings() {
        if (costAnalysis != null && costAnalysis.getCostSavingsVsBenchmark() != null) {
            return costAnalysis.getCostSavingsVsBenchmark();
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get report summary for dashboard
     */
    public Map<String, Object> getReportSummary() {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("reportId", reportId != null ? reportId : "N/A");
        summary.put("reportType", reportType != null ? reportType : "UNKNOWN");
        summary.put("overallGrade", getOverallGrade());
        summary.put("totalOrders", this.summary != null && this.summary.getTotalOrders() != null ? 
                   this.summary.getTotalOrders() : 0);
        summary.put("overallFillRate", this.summary != null && this.summary.getOverallFillRate() != null ? 
                    this.summary.getOverallFillRate() : BigDecimal.ZERO);
        summary.put("executionQuality", this.summary != null && this.summary.getExecutionQuality() != null ? 
                   this.summary.getExecutionQuality() : "UNKNOWN");
        summary.put("bestVenue", getBestVenue());
        summary.put("totalSavings", getTotalCostSavings());
        summary.put("isImproving", isPerformanceImproving());
        summary.put("highPriorityRecommendations", getHighPriorityRecommendations().size());
        summary.put("generatedAt", generatedAt != null ? generatedAt : Instant.EPOCH);
        return java.util.Collections.unmodifiableMap(summary);
    }
    
    /**
     * Static factory methods
     */
    public static ExecutionReport dailyReport(Long userId, LocalDate reportDate) {
        return ExecutionReport.builder()
            .reportId("DAILY_" + reportDate.toString().replace("-", "") + "_" + userId)
            .userId(userId)
            .reportType("DAILY")
            .reportDate(reportDate)
            .generatedAt(Instant.now())
            .periodStart(reportDate)
            .periodEnd(reportDate)
            .build();
    }
    
    public static ExecutionReport realTimeReport(Long userId) {
        return ExecutionReport.builder()
            .reportId("RT_" + System.currentTimeMillis() + "_" + userId)
            .userId(userId)
            .reportType("REAL_TIME")
            .generatedAt(Instant.now())
            .build();
    }
}