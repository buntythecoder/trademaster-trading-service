package com.trademaster.trading.dto;

import java.time.Instant;
import java.util.Set;

/**
 * Record representing a market data subscription request.
 */
public record MarketDataSubscription(
    String subscriptionId,
    String strategyId,
    Set<String> symbols,
    Set<String> dataTypes,
    String frequency,
    boolean realTime,
    Instant startTime,
    Instant endTime,
    String userId,
    String correlationId
) {}