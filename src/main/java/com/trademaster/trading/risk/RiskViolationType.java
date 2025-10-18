package com.trademaster.trading.risk;

/**
 * Risk Violation Types
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #14 - Pattern Matching Excellence
 *
 * Enumeration of risk violation types for risk checking.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum RiskViolationType {
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
