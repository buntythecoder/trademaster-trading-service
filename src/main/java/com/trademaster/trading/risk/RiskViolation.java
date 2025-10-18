package com.trademaster.trading.risk;

import lombok.Builder;
import lombok.Data;

/**
 * Risk Violation
 * 
 * Represents a specific risk rule violation that prevents
 * order execution or requires special handling.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class RiskViolation {
    
    /**
     * Type of risk violation
     */
    private RiskViolationType type;
    
    /**
     * Human-readable violation message
     */
    private String message;
    
    /**
     * Severity of the violation
     */
    private RiskSeverity severity;
    
    /**
     * Current value that triggered violation
     */
    private String currentValue;
    
    /**
     * Maximum allowed value
     */
    private String limitValue;
    
    /**
     * Risk rule that was violated
     */
    private String ruleCode;
    
    /**
     * Suggested action to resolve violation
     */
    private String suggestedAction;
}