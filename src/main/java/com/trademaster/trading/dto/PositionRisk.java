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
import java.util.stream.Stream;

/**
 * Position Risk DTO
 * 
 * Comprehensive position-level risk analytics with:
 * - Value at Risk (VaR) and Expected Shortfall calculations
 * - Greeks and options risk analytics
 * - Concentration and correlation risk metrics
 * - Stress testing and scenario analysis
 * - Liquidity and market risk assessment
 * - Real-time risk monitoring and alerting
 * - Regulatory risk and compliance metrics
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionRisk {
    
    /**
     * Risk Assessment Identification
     */
    private String riskId;
    private Long userId;
    private String symbol;
    private String exchange;
    private String assetClass;
    private Instant assessmentTime;
    private LocalDate assessmentDate;
    private String riskModelVersion;
    
    /**
     * Market Risk Metrics
     */
    private MarketRisk marketRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketRisk {
        // Value at Risk Metrics
        private BigDecimal dailyVaR95; // 95% confidence 1-day VaR
        private BigDecimal dailyVaR99; // 99% confidence 1-day VaR
        private BigDecimal weeklyVaR95; // 95% confidence 1-week VaR
        private BigDecimal expectedShortfall; // Expected Shortfall (CVaR)
        private String varMethodology; // PARAMETRIC, HISTORICAL, MONTE_CARLO
        
        // Volatility and Beta Metrics
        private BigDecimal volatilityDaily; // Daily price volatility
        private BigDecimal volatilityAnnualized; // Annualized volatility
        private BigDecimal beta; // Beta to market index
        private BigDecimal correlation; // Correlation to market
        private BigDecimal trackingError; // Tracking error vs benchmark
        
        // Price Sensitivity
        private BigDecimal priceSensitivity; // Price sensitivity per $1 move
        private BigDecimal percentSensitivity; // Sensitivity per 1% price move
        private BigDecimal duration; // Price duration for bonds
        private BigDecimal convexity; // Price convexity for bonds
        
        // Market Regime Analysis
        private String marketRegime; // BULL, BEAR, SIDEWAYS, HIGH_VOLATILITY
        private BigDecimal regimeConfidence; // Confidence in regime classification
        private Map<String, BigDecimal> regimeSensitivity; // Sensitivity by regime
    }
    
    /**
     * Options Greeks and Derivatives Risk
     */
    private OptionsRisk optionsRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionsRisk {
        // Primary Greeks
        private BigDecimal delta; // Price sensitivity
        private BigDecimal gamma; // Delta sensitivity
        private BigDecimal theta; // Time decay
        private BigDecimal vega; // Volatility sensitivity
        private BigDecimal rho; // Interest rate sensitivity
        
        // Advanced Greeks
        private BigDecimal epsilon; // Dividend sensitivity
        private BigDecimal lambda; // Leverage (elasticity)
        private BigDecimal vanna; // Volatility-price cross-sensitivity
        private BigDecimal volga; // Volatility-volatility sensitivity
        private BigDecimal charm; // Delta decay (time-price)
        
        // Options-Specific Risk
        private BigDecimal impliedVolatility; // Current implied volatility
        private BigDecimal volSkew; // Volatility skew exposure
        private BigDecimal pinRisk; // Pin risk at expiration
        private BigDecimal exerciseRisk; // Early exercise probability
        private BigDecimal assignmentRisk; // Assignment probability
        
        // Time and Expiration Risk
        private Integer daysToExpiration; // Days until expiration
        private BigDecimal timeDecayDaily; // Daily time decay amount
        private String expirationRisk; // LOW, MEDIUM, HIGH
        private List<LocalDate> nearExpirations; // Nearby expiration dates
    }
    
    /**
     * Liquidity Risk Assessment
     */
    private LiquidityRisk liquidityRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiquidityRisk {
        private BigDecimal bidAskSpread; // Current bid-ask spread
        private BigDecimal spreadPercent; // Spread as % of mid price
        private Long averageVolume; // Average daily volume
        private Long currentVolume; // Current day volume
        private BigDecimal volumeRatio; // Current vs average volume
        
        // Market Impact Assessment
        private BigDecimal marketImpact1Pct; // Impact to trade 1% of volume
        private BigDecimal marketImpact5Pct; // Impact to trade 5% of volume
        private BigDecimal marketImpact10Pct; // Impact to trade 10% of volume
        private BigDecimal liquidationTime; // Estimated time to liquidate (days)
        
        // Liquidity Scoring
        private String liquidityScore; // A, B, C, D, F rating
        private BigDecimal liquidityRank; // Percentile ranking
        private String liquidityCategory; // HIGH, MEDIUM, LOW, ILLIQUID
        
        // Circuit Breaker and Halt Risk
        private Boolean circuitBreakerRisk; // Circuit breaker proximity
        private Integer haltHistoryDays; // Trading halts last 30 days
        private BigDecimal haltRiskScore; // Halt probability score
    }
    
    /**
     * Concentration Risk Analysis
     */
    private ConcentrationRisk concentrationRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConcentrationRisk {
        private BigDecimal positionWeight; // Position as % of portfolio
        private BigDecimal sectorWeight; // Sector concentration
        private BigDecimal industryWeight; // Industry concentration
        private BigDecimal geographicWeight; // Geographic concentration
        private BigDecimal currencyWeight; // Currency concentration
        
        // Concentration Limits and Alerts
        private BigDecimal positionLimit; // Maximum allowed position size
        private BigDecimal utilizationPercent; // Current vs limit utilization
        private String concentrationLevel; // LOW, MEDIUM, HIGH, EXCESSIVE
        private List<String> concentrationAlerts; // Active alerts
        
        // Diversification Metrics
        private Integer numberOfPositions; // Total positions in portfolio
        private BigDecimal herfindahlIndex; // Portfolio concentration index
        private BigDecimal diversificationRatio; // Diversification effectiveness
        private BigDecimal correlationImpact; // Impact of correlations
    }
    
    /**
     * Credit and Counterparty Risk
     */
    private CounterpartyRisk counterpartyRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CounterpartyRisk {
        private String issuerRating; // Credit rating of issuer
        private String issuerOutlook; // Rating outlook
        private BigDecimal creditSpread; // Credit spread to treasury
        private BigDecimal probabilityOfDefault; // Default probability
        private BigDecimal recoveryRate; // Expected recovery rate
        private BigDecimal creditVaR; // Credit Value at Risk
        
        // Exposure Metrics
        private BigDecimal currentExposure; // Current credit exposure
        private BigDecimal potentialExposure; // Potential future exposure
        private BigDecimal wrongWayRisk; // Wrong-way risk assessment
        
        // Settlement and Operational Risk
        private String settlementRisk; // Settlement risk level
        private Integer settlementDays; // Standard settlement period
        private String custodianRisk; // Custodian risk assessment
    }
    
    /**
     * Stress Testing Results
     */
    private List<StressTestResult> stressTests;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StressTestResult {
        private String scenarioName; // Name of stress scenario
        private String scenarioType; // HISTORICAL, HYPOTHETICAL, MONTE_CARLO
        private String stressLevel; // MILD, MODERATE, SEVERE, EXTREME
        
        // Scenario Parameters
        private Map<String, BigDecimal> stressFactors; // Factor shocks
        private BigDecimal marketShock; // Market price shock %
        private BigDecimal volatilityShock; // Volatility shock
        private BigDecimal liquidityShock; // Liquidity impact
        
        // Stress Test Results
        private BigDecimal stressedValue; // Position value under stress
        private BigDecimal stressedPnL; // P&L impact of scenario
        private BigDecimal stressedVaR; // VaR under stress conditions
        private String stressImpact; // LOW, MEDIUM, HIGH, CRITICAL
        
        // Time Horizon and Probability
        private Integer stressHorizonDays; // Scenario time horizon
        private BigDecimal scenarioProbability; // Estimated probability
        private LocalDate lastObservedDate; // When scenario last occurred
    }
    
    /**
     * Regulatory Risk Metrics
     */
    private RegulatoryRisk regulatoryRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryRisk {
        // Position Limits
        private BigDecimal positionLimit; // Regulatory position limit
        private BigDecimal currentPosition; // Current position size
        private BigDecimal limitUtilization; // Utilization percentage
        private List<String> limitViolations; // Active limit violations
        
        // Capital Requirements
        private BigDecimal capitalRequirement; // Required regulatory capital
        private BigDecimal riskWeightedAssets; // Risk-weighted asset amount
        private BigDecimal leverageRatio; // Position leverage ratio
        private String capitalCategory; // Capital adequacy category
        
        // Compliance Monitoring
        private List<String> complianceFlags; // Active compliance flags
        private Boolean patternDayTradingRisk; // PDT rule proximity
        private Boolean washSaleRisk; // Wash sale rule risk
        private Boolean suitabilityRisk; // Investment suitability risk
        
        // Reporting Requirements
        private Boolean reportingRequired; // Position reporting required
        private List<String> reportingTypes; // Types of reports required
        private LocalDate nextReportingDate; // Next required report date
    }
    
    /**
     * Risk Monitoring and Alerts
     */
    private RiskMonitoring riskMonitoring;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskMonitoring {
        private String overallRiskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private BigDecimal overallRiskScore; // Composite risk score 0-100
        private List<RiskAlert> activeAlerts; // Currently active alerts
        private Map<String, BigDecimal> riskThresholds; // Configured thresholds
        
        // Risk Trend Analysis
        private String riskTrend; // IMPROVING, STABLE, DETERIORATING
        private BigDecimal riskTrendSlope; // Numerical trend slope
        private Integer trendPeriodDays; // Period for trend analysis
        
        // Historical Risk Metrics
        private BigDecimal maxRiskScore30d; // Max risk score last 30 days
        private BigDecimal avgRiskScore30d; // Avg risk score last 30 days
        private Integer riskViolations30d; // Risk violations last 30 days
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAlert {
        private String alertId;
        private String alertType; // VAR_BREACH, LIMIT_VIOLATION, CONCENTRATION
        private String severity; // INFO, WARNING, CRITICAL, EMERGENCY
        private String alertMessage;
        private BigDecimal thresholdValue;
        private BigDecimal currentValue;
        private Instant triggeredAt;
        private String status; // ACTIVE, ACKNOWLEDGED, RESOLVED
        private String acknowledgementRequired; // AUTO, MANUAL, MANAGER
    }
    
    /**
     * Performance Attribution Risk
     */
    private PerformanceRisk performanceRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceRisk {
        private BigDecimal trackingErrorAnnualized; // Annualized tracking error
        private BigDecimal informationRatio; // Information ratio
        private BigDecimal activeRisk; // Active risk vs benchmark
        private BigDecimal residualRisk; // Residual (specific) risk
        
        // Factor Risk Decomposition
        private Map<String, BigDecimal> factorExposures; // Factor risk exposures
        private Map<String, BigDecimal> factorContributions; // Risk contributions
        private BigDecimal factorRisk; // Risk from factors
        private BigDecimal specificRisk; // Company-specific risk
        
        // Risk-Adjusted Performance
        private BigDecimal sharpeRatio; // Risk-adjusted return
        private BigDecimal sortinoRatio; // Downside risk-adjusted return
        private BigDecimal maximumDrawdown; // Maximum peak-to-trough loss
        private Integer drawdownRecoveryDays; // Days to recover from drawdown
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if position is high risk - eliminates if-statement with Optional
     */
    public boolean isHighRisk() {
        return Optional.ofNullable(riskMonitoring)
            .flatMap(rm -> Optional.ofNullable(rm.getOverallRiskLevel()))
            .map(level -> "HIGH".equals(level) || "CRITICAL".equals(level))
            .orElse(false);
    }
    
    /**
     * Check if position has active critical alerts - eliminates if-statement with Optional
     */
    public boolean hasCriticalAlerts() {
        return Optional.ofNullable(riskMonitoring)
            .flatMap(rm -> Optional.ofNullable(rm.getActiveAlerts()))
            .map(alerts -> alerts.stream()
                .anyMatch(alert -> "CRITICAL".equals(alert.getSeverity()) ||
                                 "EMERGENCY".equals(alert.getSeverity())))
            .orElse(false);
    }
    
    /**
     * Get maximum VaR across all confidence levels - eliminates all if-statements with Stream.max()
     */
    public BigDecimal getMaxVaR() {
        return Optional.ofNullable(marketRisk)
            .map(mr -> Stream.of(
                    Optional.ofNullable(mr.getDailyVaR95()),
                    Optional.ofNullable(mr.getDailyVaR99()),
                    Optional.ofNullable(mr.getWeeklyVaR95())
                )
                .flatMap(Optional::stream)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Check if options position is near expiration - eliminates if-statement with Optional
     */
    public boolean isNearExpiration(int warningDays) {
        return Optional.ofNullable(optionsRisk)
            .flatMap(or -> Optional.ofNullable(or.getDaysToExpiration()))
            .map(days -> days <= warningDays)
            .orElse(false);
    }
    
    /**
     * Check if position exceeds concentration limits - eliminates all if-statements with Optional
     */
    public boolean exceedsConcentrationLimits() {
        return Optional.ofNullable(concentrationRisk)
            .map(cr ->
                // Check utilization percentage OR concentration level - eliminates nested if-statements
                Optional.ofNullable(cr.getUtilizationPercent())
                    .map(pct -> pct.compareTo(new BigDecimal("100")) >= 0)
                    .orElse(false) ||
                "EXCESSIVE".equals(cr.getConcentrationLevel())
            )
            .orElse(false);
    }
    
    /**
     * Get risk score breakdown - eliminates all 4 ternaries with Optional.flatMap() patterns
     */
    public Map<String, BigDecimal> getRiskScoreBreakdown() {
        return Map.of(
            "market_risk", Optional.ofNullable(marketRisk)
                .flatMap(mr -> Optional.ofNullable(mr.getDailyVaR99()))
                .orElse(BigDecimal.ZERO),
            "liquidity_risk", Optional.ofNullable(liquidityRisk)
                .flatMap(lr -> Optional.ofNullable(lr.getLiquidityRank()))
                .orElse(BigDecimal.ZERO),
            "concentration_risk", Optional.ofNullable(concentrationRisk)
                .flatMap(cr -> Optional.ofNullable(cr.getUtilizationPercent()))
                .orElse(BigDecimal.ZERO),
            "overall_risk", Optional.ofNullable(riskMonitoring)
                .flatMap(rm -> Optional.ofNullable(rm.getOverallRiskScore()))
                .orElse(BigDecimal.ZERO)
        );
    }
    
    /**
     * Static factory methods
     */
    
    public static PositionRisk lowRiskPosition(Long userId, String symbol) {
        return PositionRisk.builder()
            .riskId("LOW_" + System.currentTimeMillis())
            .userId(userId)
            .symbol(symbol)
            .assessmentTime(Instant.now())
            .assessmentDate(LocalDate.now())
            .marketRisk(MarketRisk.builder()
                .dailyVaR95(new BigDecimal("500.00"))
                .volatilityDaily(new BigDecimal("0.15"))
                .beta(new BigDecimal("0.8"))
                .build())
            .riskMonitoring(RiskMonitoring.builder()
                .overallRiskLevel("LOW")
                .overallRiskScore(new BigDecimal("25.0"))
                .riskTrend("STABLE")
                .build())
            .build();
    }
    
    public static PositionRisk highRiskPosition(Long userId, String symbol) {
        return PositionRisk.builder()
            .riskId("HIGH_" + System.currentTimeMillis())
            .userId(userId)
            .symbol(symbol)
            .assessmentTime(Instant.now())
            .assessmentDate(LocalDate.now())
            .marketRisk(MarketRisk.builder()
                .dailyVaR95(new BigDecimal("5000.00"))
                .dailyVaR99(new BigDecimal("7500.00"))
                .volatilityDaily(new BigDecimal("0.45"))
                .beta(new BigDecimal("1.8"))
                .build())
            .riskMonitoring(RiskMonitoring.builder()
                .overallRiskLevel("HIGH")
                .overallRiskScore(new BigDecimal("85.0"))
                .riskTrend("DETERIORATING")
                .build())
            .build();
    }
}