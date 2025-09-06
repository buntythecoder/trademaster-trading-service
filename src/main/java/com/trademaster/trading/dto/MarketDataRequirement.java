package com.trademaster.trading.dto;

import java.util.Set;

/**
 * Record representing market data requirements for a trading strategy.
 */
public record MarketDataRequirement(
    String strategyId,
    Set<String> requiredSymbols,
    Set<String> requiredDataTypes,
    String minimumFrequency,
    boolean realTimeRequired,
    int maxLatencyMs,
    boolean historicalDataRequired,
    int historicalDays,
    String correlationId
) {}