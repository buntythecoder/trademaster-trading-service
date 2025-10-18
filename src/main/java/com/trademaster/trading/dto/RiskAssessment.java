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
 * Risk Assessment DTO
 * 
 * Comprehensive pre-trade and post-trade risk assessment with:
 * - Multi-layered risk validation results
 * - Real-time risk metrics and predictions
 * - Regulatory compliance status
 * - Impact analysis and recommendations
 * - Machine learning risk predictions
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {
    
    /**
     * Assessment Metadata
     */
    private String assessmentId;
    private Long userId;
    private String orderId;
    private String symbol;
    private Instant assessmentTime;
    private String assessmentType; // PRE_TRADE, POST_TRADE, INTRADAY, OVERNIGHT
    
    /**
     * Overall Risk Assessment
     */
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private BigDecimal riskScore; // 0.0 to 1.0
    private Boolean approved; // Overall assessment approval
    private String rejectionReason; // Reason if not approved
    private List<String> warnings; // Risk warnings
    private List<String> recommendations; // Risk mitigation recommendations
    
    /**
     * Pre-Trade Risk Validation
     */
    private PreTradeRisk preTradeRisk;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreTradeRisk {
        private Boolean buyingPowerSufficient;
        private BigDecimal requiredMargin;
        private BigDecimal availableBuyingPower;
        private Boolean positionLimitCompliant;
        private Boolean dayTradingCompliant;
        private BigDecimal concentrationRisk; // 0.0 to 1.0
        private Boolean liquidityAdequate;
        private BigDecimal estimatedImpact; // Market impact percentage
    }
    
    /**
     * Portfolio Impact Analysis
     */
    private PortfolioImpact portfolioImpact;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioImpact {
        private BigDecimal varImpact; // VaR change
        private BigDecimal exposureChange; // Portfolio exposure change
        private BigDecimal correlationImpact; // Correlation effect
        private BigDecimal leverageChange; // Leverage ratio change
        private BigDecimal concentrationChange; // Concentration change
        private Map<String, BigDecimal> sectorExposureChange; // Sector exposure changes
        private BigDecimal liquidityImpact; // Liquidity impact
    }
    
    /**
     * Regulatory Compliance
     */
    private RegulatoryCompliance compliance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryCompliance {
        private Boolean sebiCompliant;
        private Boolean rbiCompliant;
        private Boolean circuitBreakerCompliant;
        private Boolean marginTradingCompliant;
        private List<String> violatedRules;
        private List<String> complianceWarnings;
        private Map<String, String> regulatoryLimits;
    }
    
    /**
     * Risk Metrics
     */
    private RiskMetrics riskMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskMetrics {
        private BigDecimal portfolioVaR; // Current portfolio VaR
        private BigDecimal newVaR; // VaR after order
        private BigDecimal expectedShortfall; // Expected shortfall
        private BigDecimal beta; // Position beta
        private BigDecimal volatility; // Position volatility
        private BigDecimal sharpeRatio; // Risk-adjusted return
        private BigDecimal maxDrawdown; // Maximum drawdown risk
        private BigDecimal correlationRisk; // Correlation risk score
    }
    
    /**
     * ML Risk Predictions
     */
    private MLRiskPrediction mlPrediction;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MLRiskPrediction {
        private BigDecimal predictedVolatility; // ML predicted volatility
        private BigDecimal anomalyScore; // Trading pattern anomaly score
        private BigDecimal riskPrediction; // ML risk prediction (0.0 to 1.0)
        private String marketRegime; // BULL, BEAR, VOLATILE, STABLE
        private BigDecimal confidenceLevel; // Prediction confidence
        private List<String> riskFactors; // Identified risk factors
        private Map<String, BigDecimal> featureImportance; // ML feature importance
    }
    
    /**
     * Stress Test Results
     */
    private List<StressTestScenario> stressTests;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StressTestScenario {
        private String scenarioName; // Market crash, volatility spike, etc.
        private BigDecimal pnlImpact; // P&L impact in scenario
        private BigDecimal varImpact; // VaR impact in scenario
        private BigDecimal probabilityOfLoss; // Probability of loss
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private List<String> mitigationStrategies; // Risk mitigation options
    }
    
    /**
     * Performance Metrics
     */
    private PerformanceMetrics performance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private Long assessmentDurationMs; // Assessment processing time
        private Long mlModelLatencyMs; // ML model execution time
        private Long dataRetrievalMs; // Data retrieval time
        private Long validationTimeMs; // Validation processing time
        private Integer rulesEvaluated; // Number of rules evaluated
        private Integer scenariosAnalyzed; // Number of scenarios analyzed
    }
    
    /**
     * Risk Limit Analysis
     */
    private List<RiskLimitCheck> riskLimitChecks;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskLimitCheck {
        private String limitType; // POSITION, SECTOR, VAR, LEVERAGE, etc.
        private BigDecimal currentValue;
        private BigDecimal limitValue;
        private BigDecimal utilizationPercent;
        private Boolean withinLimit;
        private String severity; // INFO, WARNING, CRITICAL
        private String recommendation;
    }
    
    /**
     * Market Data Context
     */
    private MarketDataContext marketContext;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketDataContext {
        private String marketStatus; // OPEN, CLOSED, PRE_MARKET, AFTER_HOURS
        private BigDecimal currentPrice;
        private BigDecimal bidAskSpread;
        private BigDecimal volatility;
        private BigDecimal volume;
        private Instant lastUpdate;
        private String liquidityCondition; // HIGH, MEDIUM, LOW
        private List<String> marketAlerts; // Circuit breakers, news, etc.
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if assessment is approved for execution
     */
    public boolean isApproved() {
        return approved != null && approved;
    }
    
    /**
     * Check if assessment indicates high risk
     */
    public boolean isHighRisk() {
        return "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel);
    }
    
    /**
     * Get overall risk score as percentage
     */
    public BigDecimal getRiskScorePercent() {
        // Eliminates ternary operator using Optional.ofNullable().map().orElse()
        return Optional.ofNullable(riskScore)
            .map(score -> score.multiply(BigDecimal.valueOf(100)))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Check if any regulatory violations exist
     */
    public boolean hasRegulatoryViolations() {
        return compliance != null && 
               compliance.getViolatedRules() != null && 
               !compliance.getViolatedRules().isEmpty();
    }
    
    /**
     * Check if position limits are exceeded
     */
    public boolean hasPositionLimitViolation() {
        return preTradeRisk != null && 
               preTradeRisk.getPositionLimitCompliant() != null && 
               !preTradeRisk.getPositionLimitCompliant();
    }
    
    /**
     * Check if margin is sufficient
     */
    public boolean hasMarginDeficit() {
        return preTradeRisk != null && 
               preTradeRisk.getBuyingPowerSufficient() != null && 
               !preTradeRisk.getBuyingPowerSufficient();
    }
    
    /**
     * Get critical risk factors
     */
    public List<String> getCriticalRiskFactors() {
        // Eliminates ternary operator using Optional.ofNullable().map().orElse()
        return Optional.ofNullable(warnings)
            .map(w -> w.stream()
                .filter(warning -> warning.contains("CRITICAL"))
                .toList())
            .orElse(List.of());
    }
    
    /**
     * Get total processing time
     */
    public Long getTotalProcessingTime() {
        // Eliminates if-statement using Optional.ofNullable().map().orElse()
        return Optional.ofNullable(performance)
            .map(PerformanceMetrics::getAssessmentDurationMs)
            .orElse(0L);
    }
    
    /**
     * Check if assessment was completed within SLA
     */
    public boolean withinSLA() {
        Long processingTime = getTotalProcessingTime();
        return processingTime != null && processingTime <= 10L; // <10ms SLA
    }
    
    /**
     * Get risk assessment summary
     */
    public Map<String, Object> getSummary() {
        // Eliminates ternary operators using Optional.ofNullable().orElse() and .map().orElse()
        return Map.of(
            "riskLevel", Optional.ofNullable(riskLevel).orElse("UNKNOWN"),
            "riskScore", getRiskScorePercent(),
            "approved", isApproved(),
            "processingTime", getTotalProcessingTime(),
            "withinSLA", withinSLA(),
            "hasViolations", hasRegulatoryViolations(),
            "warningsCount", Optional.ofNullable(warnings).map(List::size).orElse(0)
        );
    }
    
    /**
     * Static factory methods
     */
    public static RiskAssessment rejected(String userId, String reason) {
        return RiskAssessment.builder()
            .userId(Long.valueOf(userId))
            .approved(false)
            .rejectionReason(reason)
            .riskLevel("CRITICAL")
            .riskScore(BigDecimal.ONE)
            .assessmentTime(Instant.now())
            .build();
    }
    
    public static RiskAssessment approved(String userId) {
        return RiskAssessment.builder()
            .userId(Long.valueOf(userId))
            .approved(true)
            .riskLevel("LOW")
            .riskScore(BigDecimal.ZERO)
            .assessmentTime(Instant.now())
            .build();
    }
}