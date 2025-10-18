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
import java.util.Optional;

/**
 * Risk Metrics DTO
 * 
 * Comprehensive risk metrics and analytics with:
 * - Portfolio-level risk measurements
 * - Real-time risk indicators
 * - Historical risk analysis
 * - Predictive risk models
 * - Stress testing results
 * - Regulatory compliance metrics
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskMetrics {
    
    /**
     * Metrics Metadata
     */
    private String metricsId;
    private Long userId;
    private Instant calculatedAt;
    private LocalDate metricsDate;
    private String calculationMethod; // HISTORICAL, MONTE_CARLO, PARAMETRIC
    private Integer confidenceLevel; // 95, 99, etc.
    private Integer timeHorizon; // Time horizon in days
    private String currency; // INR, USD, etc.
    
    /**
     * Value at Risk (VaR) Metrics
     */
    private VaRMetrics varMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VaRMetrics {
        private BigDecimal oneDayVaR95; // 1-day 95% VaR
        private BigDecimal oneDayVaR99; // 1-day 99% VaR
        private BigDecimal tenDayVaR95; // 10-day 95% VaR
        private BigDecimal tenDayVaR99; // 10-day 99% VaR
        private BigDecimal expectedShortfall95; // Expected Shortfall 95%
        private BigDecimal expectedShortfall99; // Expected Shortfall 99%
        private BigDecimal componentVaR; // Component VaR
        private BigDecimal marginalVaR; // Marginal VaR
        private BigDecimal incrementalVaR; // Incremental VaR
        private Map<String, BigDecimal> sectorVaR; // VaR by sector
    }
    
    /**
     * Volatility Metrics
     */
    private VolatilityMetrics volatilityMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolatilityMetrics {
        private BigDecimal realizedVolatility; // Historical realized volatility
        private BigDecimal impliedVolatility; // Option-implied volatility
        private BigDecimal ewmaVolatility; // EWMA volatility estimate
        private BigDecimal garchVolatility; // GARCH model volatility
        private BigDecimal volatilityOfVolatility; // Vol of vol
        private BigDecimal volSkew; // Volatility skew
        private BigDecimal volSurface; // Volatility surface
        private BigDecimal annualizedVolatility; // Annualized volatility
        private Map<String, BigDecimal> positionVolatilities; // Per position vol
    }
    
    /**
     * Portfolio Greeks
     */
    private GreeksMetrics greeksMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GreeksMetrics {
        private BigDecimal portfolioDelta; // Portfolio delta
        private BigDecimal portfolioGamma; // Portfolio gamma
        private BigDecimal portfolioTheta; // Portfolio theta
        private BigDecimal portfolioVega; // Portfolio vega
        private BigDecimal portfolioRho; // Portfolio rho
        private BigDecimal deltaNormal; // Delta-normal VaR
        private BigDecimal gammaEffect; // Gamma effect on risk
        private Map<String, BigDecimal> underlyingGreeks; // Greeks by underlying
        private BigDecimal hedgeRatio; // Optimal hedge ratio
    }
    
    /**
     * Correlation and Beta Metrics
     */
    private CorrelationMetrics correlationMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorrelationMetrics {
        private BigDecimal portfolioBeta; // Portfolio beta to market
        private BigDecimal portfolioAlpha; // Portfolio alpha
        private BigDecimal correlation; // Correlation to benchmark
        private BigDecimal trackingError; // Tracking error
        private BigDecimal informationRatio; // Information ratio
        private BigDecimal activeBeta; // Active beta
        private Map<String, BigDecimal> correlationMatrix; // Inter-asset correlations
        private BigDecimal avgCorrelation; // Average correlation
        private BigDecimal maxCorrelation; // Maximum correlation
    }
    
    /**
     * Drawdown Analysis
     */
    private DrawdownMetrics drawdownMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrawdownMetrics {
        private BigDecimal maxDrawdown; // Maximum drawdown
        private BigDecimal currentDrawdown; // Current drawdown
        private Integer drawdownDuration; // Current drawdown duration (days)
        private Integer maxDrawdownDuration; // Max drawdown duration
        private BigDecimal underwaterCurve; // Underwater curve value
        private LocalDate maxDrawdownDate; // Date of max drawdown
        private BigDecimal recoveryTime; // Expected recovery time
        private List<DrawdownPeriod> historicalDrawdowns; // Historical drawdowns
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrawdownPeriod {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate troughDate;
        private BigDecimal maxDrawdown;
        private Integer duration;
        private Integer recoveryDays;
    }
    
    /**
     * Concentration Risk
     */
    private ConcentrationRisk concentrationRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConcentrationRisk {
        private BigDecimal herfindahlIndex; // Concentration index
        private BigDecimal top5Concentration; // Top 5 positions concentration
        private BigDecimal top10Concentration; // Top 10 positions concentration
        private BigDecimal sectorConcentration; // Sector concentration
        private BigDecimal industryConcentration; // Industry concentration
        private BigDecimal geographicConcentration; // Geographic concentration
        private BigDecimal maxSinglePosition; // Largest single position %
        private BigDecimal effectiveNPositions; // Effective number of positions
        private Map<String, BigDecimal> concentrationByType; // Concentration by type
    }
    
    /**
     * Liquidity Risk
     */
    private LiquidityRisk liquidityRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiquidityRisk {
        private BigDecimal portfolioLiquidity; // Overall portfolio liquidity score
        private BigDecimal averageBidAskSpread; // Average bid-ask spread
        private BigDecimal liquidityAdjustedVaR; // Liquidity-adjusted VaR
        private Integer avgDaysToLiquidate; // Average liquidation time
        private BigDecimal liquidityCost; // Cost of liquidation
        private BigDecimal iliquidityDiscount; // Illiquidity discount
        private Map<String, BigDecimal> positionLiquidity; // Liquidity by position
        private BigDecimal liquidityBuffer; // Liquidity buffer requirement
        private String liquidityProfile; // HIGH, MEDIUM, LOW
    }
    
    /**
     * Leverage Metrics
     */
    private LeverageMetrics leverageMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeverageMetrics {
        private BigDecimal grossLeverage; // Gross leverage ratio
        private BigDecimal netLeverage; // Net leverage ratio
        private BigDecimal financialLeverage; // Financial leverage
        private BigDecimal operatingLeverage; // Operating leverage
        private BigDecimal marginUtilization; // Margin utilization %
        private BigDecimal leverageAdjustedReturns; // Leverage-adjusted returns
        private BigDecimal leverageRisk; // Risk from leverage
        private BigDecimal optimalLeverage; // Optimal leverage level
        private BigDecimal debtToEquity; // Debt-to-equity ratio
    }
    
    /**
     * Stress Test Results
     */
    private List<StressTestResult> stressTestResults;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StressTestResult {
        private String scenarioName; // Scenario name
        private String scenarioType; // HISTORICAL, HYPOTHETICAL, REGULATORY
        private BigDecimal pnlImpact; // P&L impact
        private BigDecimal varImpact; // VaR impact
        private BigDecimal capitalImpact; // Capital impact
        private BigDecimal probabilityOfLoss; // Probability of loss
        private String severity; // LOW, MEDIUM, HIGH, EXTREME
        private Map<String, BigDecimal> sectorImpacts; // Impact by sector
        private List<String> affectedPositions; // Most affected positions
        private Instant scenarioDate; // Scenario reference date
    }
    
    /**
     * Performance Attribution
     */
    private PerformanceAttribution performanceAttribution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceAttribution {
        private BigDecimal totalReturn; // Total portfolio return
        private BigDecimal benchmarkReturn; // Benchmark return
        private BigDecimal activeReturn; // Active return
        private BigDecimal assetAllocationReturn; // Asset allocation return
        private BigDecimal stockSelectionReturn; // Stock selection return
        private BigDecimal interactionReturn; // Interaction return
        private BigDecimal residualReturn; // Residual return
        private Map<String, BigDecimal> sectorAttribution; // Sector attribution
        private Map<String, BigDecimal> factorAttribution; // Factor attribution
    }
    
    /**
     * Market Risk Factors
     */
    private MarketRiskFactors marketRiskFactors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketRiskFactors {
        private BigDecimal equityRisk; // Equity risk exposure
        private BigDecimal interestRateRisk; // Interest rate risk
        private BigDecimal creditRisk; // Credit risk exposure
        private BigDecimal currencyRisk; // Currency risk exposure
        private BigDecimal commodityRisk; // Commodity risk exposure
        private BigDecimal inflationRisk; // Inflation risk
        private BigDecimal liquidityRisk; // Market liquidity risk
        private BigDecimal volatilityRisk; // Volatility risk
        private Map<String, BigDecimal> factorLoadings; // Factor loadings
    }
    
    /**
     * Regulatory Capital
     */
    private RegulatoryCapital regulatoryCapital;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryCapital {
        private BigDecimal tier1Capital; // Tier 1 capital
        private BigDecimal tier2Capital; // Tier 2 capital
        private BigDecimal totalCapital; // Total regulatory capital
        private BigDecimal riskWeightedAssets; // Risk-weighted assets
        private BigDecimal capitalRatio; // Capital adequacy ratio
        private BigDecimal leverageRatio; // Basel leverage ratio
        private BigDecimal bufferRequirement; // Capital buffer requirement
        private String capitalCategory; // WELL_CAPITALIZED, ADEQUATE, etc.
    }
    
    /**
     * Real-time Risk Indicators
     */
    private RealTimeIndicators realTimeIndicators;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealTimeIndicators {
        private String riskLevel; // NORMAL, ELEVATED, HIGH, CRITICAL
        private BigDecimal riskScore; // Overall risk score (0-100)
        private List<String> activeAlerts; // Active risk alerts
        private Map<String, String> riskSignals; // Risk signals by category
        private BigDecimal confidenceScore; // Model confidence score
        private Instant lastUpdate; // Last update timestamp
        private Integer alertsCount; // Number of active alerts
        private String marketRegime; // Current market regime
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Get overall risk level - eliminates if-statement with Optional
     */
    public String getOverallRiskLevel() {
        return Optional.ofNullable(realTimeIndicators)
            .flatMap(rti -> Optional.ofNullable(rti.getRiskLevel()))
            .orElse("UNKNOWN");
    }
    
    /**
     * Check if metrics are stale - eliminates if-statement with Optional
     */
    public boolean isStale(int maxAgeMinutes) {
        return Optional.ofNullable(calculatedAt)
            .map(calculated -> {
                Instant threshold = Instant.now().minusSeconds(maxAgeMinutes * 60L);
                return calculated.isBefore(threshold);
            })
            .orElse(true);
    }
    
    /**
     * Get risk score as percentage - eliminates if-statement with Optional
     */
    public BigDecimal getRiskScorePercent() {
        return Optional.ofNullable(realTimeIndicators)
            .flatMap(rti -> Optional.ofNullable(rti.getRiskScore()))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Check if any critical alerts are active - eliminates if-statement with Optional
     */
    public boolean hasCriticalAlerts() {
        return Optional.ofNullable(realTimeIndicators)
            .flatMap(rti -> Optional.ofNullable(rti.getActiveAlerts()))
            .map(alerts -> alerts.stream().anyMatch(alert -> alert.contains("CRITICAL")))
            .orElse(false);
    }
    
    /**
     * Get VaR utilization percentage - eliminates if-statement with Optional
     */
    public BigDecimal getVaRUtilization(BigDecimal varLimit) {
        return Optional.ofNullable(varMetrics)
            .flatMap(vm -> Optional.ofNullable(vm.getOneDayVaR95()))
            .flatMap(var -> Optional.ofNullable(varLimit)
                .map(limit -> var.divide(limit, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Get effective portfolio beta - eliminates if-statement with Optional
     */
    public BigDecimal getEffectiveBeta() {
        return Optional.ofNullable(correlationMetrics)
            .flatMap(cm -> Optional.ofNullable(cm.getPortfolioBeta()))
            .orElse(BigDecimal.ONE); // Default beta of 1.0
    }
    
    /**
     * Calculate risk-adjusted return (Sharpe ratio) - eliminates if-statement with Optional
     */
    public BigDecimal calculateSharpeRatio(BigDecimal riskFreeRate) {
        return Optional.ofNullable(performanceAttribution)
            .flatMap(pa -> Optional.ofNullable(pa.getTotalReturn())
                .flatMap(totalReturn -> Optional.ofNullable(volatilityMetrics)
                    .flatMap(vm -> Optional.ofNullable(vm.getAnnualizedVolatility())
                        .map(annualizedVol -> {
                            BigDecimal excessReturn = totalReturn.subtract(riskFreeRate);
                            return excessReturn.divide(annualizedVol, 4, java.math.RoundingMode.HALF_UP);
                        }))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Get metrics summary - eliminates all 3 ternaries with Optional patterns
     */
    public Map<String, Object> getMetricsSummary() {
        return Map.of(
            "riskLevel", getOverallRiskLevel(),
            "riskScore", getRiskScorePercent(),
            "isStale", isStale(15), // 15 minutes staleness threshold
            "hasCriticalAlerts", hasCriticalAlerts(),
            "lastUpdate", Optional.ofNullable(calculatedAt).orElse(Instant.EPOCH),
            "alertsCount", Optional.ofNullable(realTimeIndicators)
                .flatMap(rti -> Optional.ofNullable(rti.getAlertsCount()))
                .orElse(0),
            "confidenceLevel", Optional.ofNullable(confidenceLevel).orElse(95)
        );
    }
    
    /**
     * Static factory method for empty metrics
     */
    public static RiskMetrics empty(Long userId) {
        return RiskMetrics.builder()
            .userId(userId)
            .calculatedAt(Instant.now())
            .metricsDate(LocalDate.now())
            .confidenceLevel(95)
            .realTimeIndicators(RealTimeIndicators.builder()
                .riskLevel("NORMAL")
                .riskScore(BigDecimal.ZERO)
                .alertsCount(0)
                .lastUpdate(Instant.now())
                .build())
            .build();
    }
}