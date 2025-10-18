package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Portfolio Analytics Data Transfer Objects
 *
 * Contains DTOs for portfolio analytics including performance metrics, risk assessment,
 * attribution analysis, and optimization recommendations.
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
public sealed interface PortfolioAnalytics {

    /**
     * Portfolio position with market value and P&L.
     */
    record Position(
        String symbol,
        Integer quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnL,
        BigDecimal pnlPercent,
        Instant entryDate
    ) implements PortfolioAnalytics {}

    /**
     * Comprehensive portfolio performance metrics.
     */
    record PerformanceMetrics(
        String portfolioId,
        Instant timestamp,

        // Value Metrics
        BigDecimal totalValue,
        BigDecimal totalCost,
        BigDecimal totalPnL,
        BigDecimal totalPnLPercent,

        // Return Metrics
        BigDecimal avgDailyReturn,
        BigDecimal annualizedReturn,
        BigDecimal cumulativeReturn,

        // Risk Metrics
        BigDecimal volatility,
        BigDecimal downsideDeviation,
        BigDecimal maxDrawdown,

        // Risk-Adjusted Metrics
        BigDecimal sharpeRatio,
        BigDecimal sortinoRatio,
        BigDecimal beta,
        BigDecimal alpha,
        BigDecimal treynorRatio,

        // Benchmark Comparison
        BigDecimal trackingError,
        BigDecimal informationRatio,

        // Trading Metrics
        BigDecimal winRate,
        BigDecimal profitFactor,
        Integer positionCount
    ) implements PortfolioAnalytics {}

    /**
     * Portfolio risk metrics including VaR, CVaR, and concentration.
     */
    record RiskMetrics(
        String portfolioId,
        Instant timestamp,

        // Value at Risk
        BigDecimal var95,
        BigDecimal var99,
        BigDecimal cvar95,
        BigDecimal cvar99,

        // Concentration Risk
        BigDecimal concentrationRisk,
        BigDecimal herfindahlIndex,
        BigDecimal diversificationRatio,

        // Position Risk
        String largestPosition,
        BigDecimal largestPositionPercent,

        // Overall Risk
        RiskLevel riskLevel,
        List<String> recommendations
    ) implements PortfolioAnalytics {

        public enum RiskLevel {
            LOW, MEDIUM, HIGH
        }
    }

    /**
     * Performance attribution analysis by asset and sector.
     */
    record AttributionAnalysis(
        String portfolioId,
        Instant timestamp,
        BigDecimal totalPnL,

        // Asset-Level Attribution
        List<AssetAttribution> assetAttributions,

        // Sector-Level Attribution
        List<SectorAttribution> sectorAttributions,

        // Top Contributors
        AssetAttribution topContributor,
        AssetAttribution topDetractor
    ) implements PortfolioAnalytics {

        public record AssetAttribution(
            String symbol,
            BigDecimal pnL,
            BigDecimal contribution,  // Contribution to total P&L (%)
            BigDecimal returnPercent,
            BigDecimal weight         // Portfolio weight (%)
        ) {}

        public record SectorAttribution(
            String sector,
            BigDecimal pnL,
            BigDecimal contribution,  // Contribution to total P&L (%)
            BigDecimal weight,        // Portfolio weight (%)
            Integer assetCount
        ) {}
    }

    /**
     * Portfolio optimization recommendations.
     */
    record OptimizationRecommendation(
        String portfolioId,
        Instant timestamp,

        // Current Portfolio
        BigDecimal currentSharpeRatio,
        BigDecimal currentVolatility,
        BigDecimal currentReturn,

        // Optimal Portfolio
        BigDecimal optimalSharpeRatio,
        BigDecimal optimalVolatility,
        BigDecimal optimalReturn,

        // Rebalancing Actions
        List<RebalancingAction> rebalancingActions,

        // Expected Improvement
        BigDecimal expectedReturnImprovement,
        BigDecimal expectedVolatilityReduction,
        BigDecimal estimatedCost
    ) implements PortfolioAnalytics {

        public record RebalancingAction(
            String symbol,
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            BigDecimal changePercent,
            String action  // BUY, SELL, HOLD
        ) {}
    }

    /**
     * Asset allocation analysis.
     */
    record AssetAllocation(
        String portfolioId,
        Instant timestamp,

        // Asset Class Breakdown
        List<AllocationBreakdown> assetClassBreakdown,

        // Sector Breakdown
        List<AllocationBreakdown> sectorBreakdown,

        // Geographic Breakdown
        List<AllocationBreakdown> geographicBreakdown,

        // Allocation Quality
        BigDecimal diversificationScore,
        String allocationQuality  // EXCELLENT, GOOD, FAIR, POOR
    ) implements PortfolioAnalytics {

        public record AllocationBreakdown(
            String category,
            BigDecimal value,
            BigDecimal weight,
            Integer assetCount
        ) {}
    }

    /**
     * Portfolio correlation matrix.
     */
    record CorrelationMatrix(
        String portfolioId,
        Instant timestamp,
        List<String> symbols,
        List<List<BigDecimal>> correlations,
        BigDecimal avgCorrelation,
        BigDecimal maxCorrelation,
        BigDecimal minCorrelation
    ) implements PortfolioAnalytics {}

    /**
     * Rolling performance metrics over time.
     */
    record RollingMetrics(
        String portfolioId,
        Instant timestamp,
        String period,  // 30D, 90D, 1Y

        // Rolling Performance
        List<TimeSeriesPoint> rollingReturns,
        List<TimeSeriesPoint> rollingVolatility,
        List<TimeSeriesPoint> rollingSharpe,

        // Trends
        String returnTrend,      // IMPROVING, STABLE, DECLINING
        String volatilityTrend,  // INCREASING, STABLE, DECREASING
        String sharpeTrend       // IMPROVING, STABLE, DECLINING
    ) implements PortfolioAnalytics {

        public record TimeSeriesPoint(
            Instant timestamp,
            BigDecimal value
        ) {}
    }
}
