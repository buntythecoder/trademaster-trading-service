package com.trademaster.trading.dto;

/**
 * Compliance Check Result
 * 
 * Immutable record for regulatory compliance results
 */
public record ComplianceResult(
    String status,
    boolean patternDayTradingCheck,
    boolean regulatoryLimit
) {}