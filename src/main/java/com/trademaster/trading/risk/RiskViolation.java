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

/**
 * Risk Violation Types
 */
enum RiskViolationType {
    INSUFFICIENT_BUYING_POWER,
    POSITION_LIMIT_EXCEEDED,
    CONCENTRATION_RISK,
    DAILY_TRADE_LIMIT,
    ORDER_VALUE_LIMIT,
    PATTERN_DAY_TRADER,
    MARGIN_REQUIREMENT,
    SECTOR_EXPOSURE,
    GENERAL,
    
    // Additional constants required by BasicRiskCheckEngine
    SYSTEM_ERROR,
    MAX_OPEN_ORDERS_EXCEEDED,
    DAILY_TRADING_LIMIT_EXCEEDED,
    CONCENTRATION_RISK_EXCEEDED,
    ORDER_VALUE_EXCEEDED
}

/**
 * Risk Severity Levels
 */
enum RiskSeverity {
    LOW,      // Warning only
    MEDIUM,   // Caution required
    HIGH,     // Blocks order execution
    CRITICAL  // Immediate attention required
}