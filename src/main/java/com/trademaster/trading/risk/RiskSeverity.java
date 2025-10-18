package com.trademaster.trading.risk;

/**
 * Risk Severity Levels
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #14 - Pattern Matching Excellence
 *
 * Enumeration of risk severity levels for risk violations.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum RiskSeverity {
    /**
     * Warning only - order can proceed with caution
     */
    LOW,

    /**
     * Caution required - manual review may be needed
     */
    MEDIUM,

    /**
     * Blocks order execution - order cannot proceed
     */
    HIGH,

    /**
     * Immediate attention required - critical risk violation
     */
    CRITICAL
}
