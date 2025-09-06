package com.trademaster.trading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Order Validation Result
 * 
 * Comprehensive validation result containing:
 * - Multi-dimensional validation status (risk, compliance, market, technical)
 * - Detailed validation messages with severity levels
 * - Risk scores and compliance ratings
 * - Performance impact estimates
 * - Corrective action recommendations
 * - Validation metadata for audit trail
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderValidationResult {
    
    /**
     * Overall validation status
     */
    private boolean isValid;
    
    /**
     * Validation timestamp
     */
    private Instant validationTimestamp;
    
    /**
     * Overall risk score (0.0 = no risk, 1.0 = maximum risk)
     */
    private BigDecimal overallRiskScore;
    
    /**
     * Overall compliance rating (0.0 = non-compliant, 1.0 = fully compliant)
     */
    private BigDecimal complianceRating;
    
    /**
     * Validation messages grouped by category
     */
    private List<String> validationMessages;
    private List<ValidationMessage> detailedMessages;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationMessage {
        private String category; // RISK, COMPLIANCE, MARKET, TECHNICAL, BUSINESS
        private String severity; // ERROR, WARNING, INFO
        private String code; // Validation rule code
        private String message; // Human-readable message
        private String field; // Field that failed validation
        private String recommendedAction; // How to fix the issue
        private Map<String, Object> context; // Additional context data
    }
    
    /**
     * Risk validation results
     */
    private RiskValidation riskValidation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskValidation {
        private boolean passed;
        private BigDecimal buyingPowerRequired;
        private BigDecimal availableBuyingPower;
        private BigDecimal buyingPowerUtilization; // Percentage
        private BigDecimal positionSizeRisk; // Risk from position concentration
        private BigDecimal portfolioVaR; // Portfolio Value at Risk
        private BigDecimal maxDrawdownRisk; // Maximum potential drawdown
        private BigDecimal leverageRatio; // Current leverage ratio
        private List<String> riskWarnings;
        private Map<String, BigDecimal> riskMetrics;
    }
    
    /**
     * Compliance validation results
     */
    private ComplianceValidation complianceValidation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceValidation {
        private boolean passed;
        private boolean requiresApproval; // Manual approval needed
        private String complianceStatus; // COMPLIANT, PENDING, VIOLATION
        private List<String> applicableRegulations; // Regulations that apply
        private List<String> complianceChecks; // Checks performed
        private Map<String, String> regulatoryLimits; // Limits that apply
        private String approvalWorkflow; // Required approval process
        private Instant nextReviewDate; // When next review is needed
    }
    
    /**
     * Market validation results
     */
    private MarketValidation marketValidation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketValidation {
        private boolean passed;
        private boolean marketOpen;
        private String tradingSession; // PRE_MARKET, REGULAR, AFTER_HOURS
        private BigDecimal currentPrice;
        private BigDecimal priceDeviation; // Deviation from current price
        private BigDecimal liquidityScore; // Market liquidity assessment
        private BigDecimal marketImpactEstimate; // Estimated market impact
        private String marketCondition; // NORMAL, VOLATILE, ILLIQUID, HALTED
        private List<String> marketWarnings;
    }
    
    /**
     * Technical validation results
     */
    private TechnicalValidation technicalValidation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicalValidation {
        private boolean passed;
        private List<String> fieldValidationErrors;
        private List<String> businessRuleViolations;
        private Map<String, String> parameterValidation;
        private boolean requiredFieldsComplete;
        private boolean formatValidationPassed;
        private boolean rangeValidationPassed;
        private List<String> crossFieldValidationIssues;
    }
    
    /**
     * Performance impact assessment
     */
    private PerformanceImpact performanceImpact;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceImpact {
        private BigDecimal estimatedExecutionCost; // Basis points
        private BigDecimal estimatedMarketImpact; // Basis points
        private BigDecimal estimatedTimingCost; // Basis points
        private BigDecimal probabilityOfFill; // 0.0-1.0
        private String expectedExecutionTime; // Time range
        private BigDecimal slippageRisk; // Expected slippage range
        private String optimalExecutionStrategy; // Recommended strategy
        private List<String> performanceWarnings;
    }
    
    /**
     * Corrective actions and recommendations
     */
    private List<CorrectiveAction> correctiveActions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorrectiveAction {
        private String actionType; // MODIFY, SPLIT, DELAY, CANCEL, APPROVE
        private String description; // What to do
        private String parameter; // Which parameter to change
        private String suggestedValue; // Recommended new value
        private String rationale; // Why this action is needed
        private BigDecimal impactScore; // Impact of taking this action
        private boolean required; // Is this action mandatory
        private Instant deadline; // When action must be taken
    }
    
    /**
     * Alternative execution suggestions
     */
    private List<AlternativeExecution> alternatives;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternativeExecution {
        private String strategyName;
        private String description;
        private Map<String, Object> parameters;
        private BigDecimal estimatedCost;
        private BigDecimal estimatedTime;
        private BigDecimal successProbability;
        private String rationale;
        private List<String> pros;
        private List<String> cons;
    }
    
    /**
     * Validation metadata
     */
    private ValidationMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationMetadata {
        private String validationVersion;
        private List<String> validationRulesApplied;
        private Map<String, String> marketDataSources;
        private Instant marketDataTimestamp;
        private String validationEngineVersion;
        private long validationDurationMs;
        private boolean usedCachedData;
        private Map<String, Object> systemContext;
    }
    
    /**
     * Helper methods
     */
    
    public boolean hasErrors() {
        return detailedMessages != null && 
               detailedMessages.stream().anyMatch(m -> "ERROR".equals(m.getSeverity()));
    }
    
    public boolean hasWarnings() {
        return detailedMessages != null && 
               detailedMessages.stream().anyMatch(m -> "WARNING".equals(m.getSeverity()));
    }
    
    public List<ValidationMessage> getErrors() {
        return detailedMessages != null ? 
               detailedMessages.stream()
                   .filter(m -> "ERROR".equals(m.getSeverity()))
                   .toList() : List.of();
    }
    
    public List<ValidationMessage> getWarnings() {
        return detailedMessages != null ? 
               detailedMessages.stream()
                   .filter(m -> "WARNING".equals(m.getSeverity()))
                   .toList() : List.of();
    }
    
    public List<CorrectiveAction> getRequiredActions() {
        return correctiveActions != null ? 
               correctiveActions.stream()
                   .filter(CorrectiveAction::isRequired)
                   .toList() : List.of();
    }
    
    public List<CorrectiveAction> getOptionalActions() {
        return correctiveActions != null ? 
               correctiveActions.stream()
                   .filter(action -> !action.isRequired())
                   .toList() : List.of();
    }
    
    public boolean requiresManualApproval() {
        return complianceValidation != null && complianceValidation.isRequiresApproval();
    }
    
    public boolean isHighRisk() {
        return overallRiskScore != null && 
               overallRiskScore.compareTo(new BigDecimal("0.7")) > 0;
    }
    
    public boolean isMarketImpactHigh() {
        return performanceImpact != null && 
               performanceImpact.getEstimatedMarketImpact() != null &&
               performanceImpact.getEstimatedMarketImpact().compareTo(new BigDecimal("50")) > 0; // 50 bps
    }
    
    public String getValidationSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Validation Status: ").append(isValid ? "PASSED" : "FAILED").append("\n");
        
        if (overallRiskScore != null) {
            summary.append("Risk Score: ").append(overallRiskScore).append("\n");
        }
        
        if (complianceRating != null) {
            summary.append("Compliance Rating: ").append(complianceRating).append("\n");
        }
        
        if (hasErrors()) {
            summary.append("Errors: ").append(getErrors().size()).append("\n");
        }
        
        if (hasWarnings()) {
            summary.append("Warnings: ").append(getWarnings().size()).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Static factory methods
     */
    public static OrderValidationResult passed() {
        return OrderValidationResult.builder()
            .isValid(true)
            .validationTimestamp(Instant.now())
            .overallRiskScore(BigDecimal.ZERO)
            .complianceRating(BigDecimal.ONE)
            .validationMessages(List.of())
            .build();
    }
    
    public static OrderValidationResult failed(String message) {
        return OrderValidationResult.builder()
            .isValid(false)
            .validationTimestamp(Instant.now())
            .validationMessages(List.of(message))
            .build();
    }
    
    public static OrderValidationResult failed(List<String> messages) {
        return OrderValidationResult.builder()
            .isValid(false)
            .validationTimestamp(Instant.now())
            .validationMessages(messages)
            .build();
    }
}