package com.trademaster.trading.risk;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Risk Check Result
 * 
 * Encapsulates the outcome of pre-trade risk assessment
 * with detailed risk metrics and violation information.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class RiskCheckResult {
    
    /**
     * Whether risk check passed
     */
    private boolean passed;
    
    /**
     * Risk score (0.0 = no risk, 1.0 = maximum risk)
     */
    private double riskScore;
    
    /**
     * Risk violations detected
     */
    @Builder.Default
    private List<RiskViolation> violations = new ArrayList<>();
    
    /**
     * Risk warnings (non-blocking)
     */
    @Builder.Default
    private List<RiskWarning> warnings = new ArrayList<>();
    
    /**
     * Available buying power
     */
    private BigDecimal buyingPower;
    
    /**
     * Required margin for this order
     */
    private BigDecimal requiredMargin;
    
    /**
     * Current portfolio value
     */
    private BigDecimal portfolioValue;
    
    /**
     * Maximum position size allowed
     */
    private BigDecimal maxPositionSize;
    
    /**
     * Current exposure by symbol
     */
    private BigDecimal currentExposure;
    
    /**
     * Processing time in milliseconds
     */
    private long processingTimeMs;
    
    /**
     * Risk check engine that generated this result
     */
    private String engineName;
    
    /**
     * Create successful risk check result
     */
    public static RiskCheckResult pass(String engineName) {
        return RiskCheckResult.builder()
            .passed(true)
            .riskScore(0.0)
            .engineName(engineName)
            .build();
    }
    
    /**
     * Create failed risk check result
     */
    public static RiskCheckResult fail(String engineName, String violationReason) {
        RiskViolation violation = RiskViolation.builder()
            .type(RiskViolationType.GENERAL)
            .message(violationReason)
            .severity(RiskSeverity.HIGH)
            .build();
            
        return RiskCheckResult.builder()
            .passed(false)
            .riskScore(1.0)
            .violations(List.of(violation))
            .engineName(engineName)
            .build();
    }
    
    /**
     * Add risk violation
     */
    public void addViolation(RiskViolationType type, String message, RiskSeverity severity) {
        violations.add(RiskViolation.builder()
            .type(type)
            .message(message)
            .severity(severity)
            .build());

        // Eliminates if-statement using Optional.filter().ifPresent()
        Optional.of(severity)
            .filter(s -> s == RiskSeverity.HIGH || s == RiskSeverity.CRITICAL)
            .ifPresent(s -> passed = false);
    }
    
    /**
     * Add risk warning
     */
    public void addWarning(RiskWarningType type, String message) {
        warnings.add(RiskWarning.builder()
            .type(type)
            .message(message)
            .build());
    }
    
    /**
     * Check if result has critical violations
     */
    public boolean hasCriticalViolations() {
        return violations.stream()
            .anyMatch(v -> v.getSeverity() == RiskSeverity.CRITICAL);
    }
    
    /**
     * Check if result has high severity violations
     */
    public boolean hasHighSeverityViolations() {
        return violations.stream()
            .anyMatch(v -> v.getSeverity() == RiskSeverity.HIGH);
    }
    
    /**
     * Get consolidated violation message
     */
    public String getConsolidatedViolationMessage() {
        return violations.stream()
            .map(RiskViolation::getMessage)
            .reduce((a, b) -> a + "; " + b)
            .orElse("");
    }
    
    /**
     * Get consolidated warning message
     */
    public String getConsolidatedWarningMessage() {
        return warnings.stream()
            .map(RiskWarning::getMessage)
            .reduce((a, b) -> a + "; " + b)
            .orElse("");
    }
    
    /**
     * Calculate risk utilization percentage
     * Eliminates if-statements using Optional.filter().flatMap() chain
     */
    public double getRiskUtilization() {
        return Optional.ofNullable(maxPositionSize)
            .filter(max -> max.compareTo(BigDecimal.ZERO) > 0)
            .flatMap(max -> Optional.ofNullable(currentExposure)
                .map(exposure -> exposure.divide(max, 4, BigDecimal.ROUND_HALF_UP)
                                        .multiply(BigDecimal.valueOf(100))
                                        .doubleValue()))
            .orElse(0.0);
    }
    
    /**
     * Check if user is approaching risk limits (>80% utilization)
     */
    public boolean isApproachingLimits() {
        return getRiskUtilization() > 80.0;
    }
}