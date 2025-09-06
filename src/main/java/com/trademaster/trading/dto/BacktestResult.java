package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backtest Result DTO
 * 
 * Comprehensive backtesting results with:
 * - Performance metrics and analytics
 * - Trade-by-trade execution details
 * - Risk analysis and drawdown statistics
 * - Benchmark comparison and attribution
 * - Optimization recommendations
 * - Statistical significance testing
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResult {
    
    /**
     * Backtest Metadata
     */
    private String backtestId;
    private String strategyId;
    private String strategyName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant completedAt;
    private Long executionTimeMs;
    private String backtestVersion;
    
    /**
     * Backtest Configuration
     */
    private BacktestConfig config;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BacktestConfig {
        private BigDecimal initialCapital;
        private BigDecimal commissionRate;
        private BigDecimal slippageModel;
        private String dataFrequency;
        private String benchmarkIndex;
        private Boolean adjustForDividends;
        private Boolean adjustForSplits;
        private Integer totalDataPoints;
        private String dataSource;
    }
    
    /**
     * Performance Summary
     */
    private PerformanceSummary performance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceSummary {
        // Return Metrics
        private BigDecimal totalReturn; // Total strategy return %
        private BigDecimal annualizedReturn; // Annualized return %
        private BigDecimal compoundAnnualGrowthRate; // CAGR %
        private BigDecimal benchmarkReturn; // Benchmark return %
        private BigDecimal excessReturn; // Excess return vs benchmark
        private BigDecimal activeReturn; // Active return %
        
        // Risk Metrics
        private BigDecimal volatility; // Annualized volatility
        private BigDecimal sharpeRatio; // Risk-adjusted return
        private BigDecimal sortinoRatio; // Downside risk-adjusted return
        private BigDecimal calmarRatio; // Return / max drawdown
        private BigDecimal informationRatio; // Excess return / tracking error
        private BigDecimal treynorRatio; // Return / beta
        private BigDecimal jensenAlpha; // Alpha vs benchmark
        private BigDecimal beta; // Beta vs benchmark
        private BigDecimal trackingError; // Tracking error vs benchmark
        private BigDecimal rSquared; // R-squared vs benchmark
        
        // Drawdown Analysis
        private BigDecimal maxDrawdown; // Maximum drawdown %
        private LocalDate maxDrawdownStart; // Max drawdown start date
        private LocalDate maxDrawdownEnd; // Max drawdown end date
        private Integer maxDrawdownDays; // Max drawdown duration
        private BigDecimal averageDrawdown; // Average drawdown %
        private Integer drawdownRecoveryDays; // Average recovery time
        private Integer totalDrawdownPeriods; // Number of drawdown periods
        
        // Value at Risk
        private BigDecimal dailyVaR95; // 95% daily VaR
        private BigDecimal dailyVaR99; // 99% daily VaR
        private BigDecimal expectedShortfall; // Conditional VaR
        private BigDecimal maxDailyLoss; // Worst single day
        private BigDecimal maxDailyGain; // Best single day
    }
    
    /**
     * Trading Statistics
     */
    private TradingStatistics tradingStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradingStatistics {
        // Trade Counts
        private Integer totalTrades; // Total number of trades
        private Integer longTrades; // Number of long trades
        private Integer shortTrades; // Number of short trades
        private Integer winningTrades; // Number of winning trades
        private Integer losingTrades; // Number of losing trades
        
        // Win/Loss Analysis
        private BigDecimal winRate; // Win rate percentage
        private BigDecimal averageWin; // Average winning trade %
        private BigDecimal averageLoss; // Average losing trade %
        private BigDecimal largestWin; // Largest winning trade %
        private BigDecimal largestLoss; // Largest losing trade %
        private BigDecimal profitFactor; // Gross profit / gross loss
        private BigDecimal payoffRatio; // Average win / average loss
        
        // Trade Duration
        private BigDecimal averageHoldingPeriod; // Average trade duration (days)
        private BigDecimal longestTrade; // Longest trade duration (days)
        private BigDecimal shortestTrade; // Shortest trade duration (days)
        private Map<String, Integer> holdingPeriodDistribution; // Duration histogram
        
        // Consecutive Performance
        private Integer maxConsecutiveWins; // Maximum consecutive wins
        private Integer maxConsecutiveLosses; // Maximum consecutive losses
        private BigDecimal maxConsecutiveWinReturn; // Max consecutive win return
        private BigDecimal maxConsecutiveLossReturn; // Max consecutive loss return
        
        // Trade Frequency
        private BigDecimal tradesPerMonth; // Average trades per month
        private BigDecimal tradingFrequency; // Trading frequency score
        private Map<String, Integer> monthlyTradeDistribution; // Monthly trade counts
        
        // Execution Quality
        private BigDecimal averageSlippage; // Average execution slippage
        private BigDecimal totalCommissions; // Total commission costs
        private BigDecimal totalSlippageCost; // Total slippage costs
        private BigDecimal netProfitAfterCosts; // Net profit after all costs
    }
    
    /**
     * Monthly/Yearly Performance Breakdown
     */
    private List<PerformancePeriod> monthlyReturns;
    private List<PerformancePeriod> yearlyReturns;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformancePeriod {
        private String period; // "2024-01" or "2024"
        private BigDecimal strategyReturn; // Strategy return for period
        private BigDecimal benchmarkReturn; // Benchmark return for period
        private BigDecimal excessReturn; // Excess return for period
        private Integer numberOfTrades; // Trades in period
        private BigDecimal volatility; // Volatility for period
        private BigDecimal maxDrawdown; // Max drawdown in period
        private BigDecimal sharpeRatio; // Sharpe ratio for period
    }
    
    /**
     * Detailed Trade List
     */
    private List<TradeRecord> trades;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradeRecord {
        private String tradeId;
        private LocalDate entryDate;
        private LocalDate exitDate;
        private String direction; // LONG, SHORT
        private BigDecimal entryPrice;
        private BigDecimal exitPrice;
        private Integer quantity;
        private BigDecimal grossReturn; // Return before costs
        private BigDecimal netReturn; // Return after costs
        private BigDecimal commission; // Commission cost
        private BigDecimal slippage; // Slippage cost
        private Integer holdingPeriod; // Days held
        private String exitReason; // STOP_LOSS, TAKE_PROFIT, SIGNAL, TIMEOUT
        private BigDecimal runupPercent; // Maximum favorable excursion
        private BigDecimal drawdownPercent; // Maximum adverse excursion
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
        // Drawdown Analysis
        private List<DrawdownPeriod> drawdownPeriods;
        private BigDecimal drawdownFrequency; // Drawdowns per year
        private BigDecimal averageDrawdownDuration; // Average duration in days
        private BigDecimal drawdownRecoveryRatio; // Recovery vs drawdown time
        
        // Tail Risk
        private BigDecimal skewness; // Return distribution skewness
        private BigDecimal kurtosis; // Return distribution kurtosis
        private Boolean normalityTest; // Returns normally distributed
        private BigDecimal tailRatio; // Tail risk ratio
        
        // Rolling Performance
        private List<RollingMetric> rollingReturns; // 12-month rolling returns
        private List<RollingMetric> rollingSharpe; // 12-month rolling Sharpe
        private List<RollingMetric> rollingVolatility; // 12-month rolling volatility
        private BigDecimal returnStability; // Consistency of returns
        
        // Stress Testing
        private List<StressTestScenario> stressTests; // Stress test results
        private BigDecimal worstMonthReturn; // Worst monthly return
        private BigDecimal worstQuarterReturn; // Worst quarterly return
        private BigDecimal worstYearReturn; // Worst annual return
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrawdownPeriod {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate troughDate;
        private BigDecimal drawdownPercent;
        private Integer durationDays;
        private Integer recoveryDays;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollingMetric {
        private LocalDate date;
        private BigDecimal value;
        private String metric; // RETURN, SHARPE, VOLATILITY
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StressTestScenario {
        private String scenarioName;
        private BigDecimal scenarioReturn;
        private String description;
    }
    
    /**
     * Attribution Analysis
     */
    private AttributionAnalysis attribution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributionAnalysis {
        // Factor Attribution
        private Map<String, BigDecimal> factorExposures; // Factor exposures
        private Map<String, BigDecimal> factorReturns; // Factor contributions
        private BigDecimal factorReturn; // Total factor return
        private BigDecimal specificReturn; // Specific/residual return
        
        // Sector/Asset Attribution
        private Map<String, BigDecimal> sectorWeights; // Sector allocations
        private Map<String, BigDecimal> sectorReturns; // Sector contributions
        private Map<String, BigDecimal> assetReturns; // Individual asset returns
        
        // Timing Attribution
        private BigDecimal marketTimingReturn; // Market timing contribution
        private BigDecimal securitySelectionReturn; // Security selection contribution
        private BigDecimal interactionReturn; // Interaction effect
        private BigDecimal allocationReturn; // Asset allocation contribution
    }
    
    /**
     * Statistical Analysis
     */
    private StatisticalAnalysis statistics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticalAnalysis {
        // Significance Testing
        private BigDecimal tStatistic; // T-statistic for returns
        private BigDecimal pValue; // P-value for significance
        private Boolean statisticallySignificant; // Significant at 95% level
        private Integer degreesOfFreedom; // Degrees of freedom
        private BigDecimal confidenceInterval; // 95% confidence interval
        
        // Return Distribution
        private BigDecimal meanReturn; // Mean daily return
        private BigDecimal medianReturn; // Median daily return
        private BigDecimal standardDeviation; // Standard deviation
        private BigDecimal variance; // Variance
        private Integer totalObservations; // Number of observations
        
        // Autocorrelation
        private List<BigDecimal> autocorrelations; // Return autocorrelations
        private Boolean serialCorrelation; // Significant serial correlation
        private BigDecimal ljungBoxStatistic; // Ljung-Box test statistic
        
        // Benchmark Analysis
        private BigDecimal correlation; // Correlation with benchmark
        private BigDecimal upCapture; // Up market capture ratio
        private BigDecimal downCapture; // Down market capture ratio
        private BigDecimal battingAverage; // Periods outperforming benchmark
    }
    
    /**
     * Optimization Recommendations
     */
    private List<OptimizationRecommendation> recommendations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationRecommendation {
        private String category; // RISK_MANAGEMENT, SIGNAL_GENERATION, EXECUTION
        private String recommendation;
        private String rationale;
        private BigDecimal potentialImprovement; // Estimated improvement
        private String priority; // HIGH, MEDIUM, LOW
        private String implementation; // How to implement
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if backtest was successful
     */
    public boolean isSuccessful() {
        return performance != null && 
               tradingStats != null && 
               tradingStats.getTotalTrades() != null &&
               tradingStats.getTotalTrades() > 0;
    }
    
    /**
     * Check if strategy outperformed benchmark
     */
    public boolean outperformedBenchmark() {
        return performance != null && 
               performance.getExcessReturn() != null &&
               performance.getExcessReturn().compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get overall strategy rating
     */
    public String getStrategyRating() {
        if (performance == null || performance.getSharpeRatio() == null) {
            return "UNKNOWN";
        }
        
        BigDecimal sharpe = performance.getSharpeRatio();
        if (sharpe.compareTo(new BigDecimal("2.0")) >= 0) return "EXCELLENT";
        if (sharpe.compareTo(new BigDecimal("1.5")) >= 0) return "VERY_GOOD";
        if (sharpe.compareTo(new BigDecimal("1.0")) >= 0) return "GOOD";
        if (sharpe.compareTo(new BigDecimal("0.5")) >= 0) return "FAIR";
        return "POOR";
    }
    
    /**
     * Get backtest summary
     */
    public Map<String, Object> getBacktestSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("backtestId", backtestId != null ? backtestId : "N/A");
        summary.put("strategyName", strategyName != null ? strategyName : "Unknown");
        summary.put("period", startDate + " to " + endDate);
        summary.put("totalReturn", performance != null && performance.getTotalReturn() != null ? 
                   performance.getTotalReturn() : BigDecimal.ZERO);
        summary.put("annualizedReturn", performance != null && performance.getAnnualizedReturn() != null ? 
                   performance.getAnnualizedReturn() : BigDecimal.ZERO);
        summary.put("sharpeRatio", performance != null && performance.getSharpeRatio() != null ? 
                   performance.getSharpeRatio() : BigDecimal.ZERO);
        summary.put("maxDrawdown", performance != null && performance.getMaxDrawdown() != null ? 
                   performance.getMaxDrawdown() : BigDecimal.ZERO);
        summary.put("totalTrades", tradingStats != null && tradingStats.getTotalTrades() != null ? 
                   tradingStats.getTotalTrades() : 0);
        summary.put("winRate", tradingStats != null && tradingStats.getWinRate() != null ? 
                   tradingStats.getWinRate() : BigDecimal.ZERO);
        summary.put("rating", getStrategyRating());
        summary.put("outperformed", outperformedBenchmark());
        summary.put("completedAt", completedAt != null ? completedAt : Instant.EPOCH);
        return Collections.unmodifiableMap(summary);
    }
    
    /**
     * Static factory method for failed backtest
     */
    public static BacktestResult failed(String backtestId, String error) {
        return BacktestResult.builder()
            .backtestId(backtestId)
            .completedAt(Instant.now())
            .recommendations(List.of(
                OptimizationRecommendation.builder()
                    .category("ERROR")
                    .recommendation("Backtest failed: " + error)
                    .priority("HIGH")
                    .build()
            ))
            .build();
    }
}