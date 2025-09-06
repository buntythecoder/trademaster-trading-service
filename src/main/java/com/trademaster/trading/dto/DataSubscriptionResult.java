package com.trademaster.trading.dto;

import java.time.Instant;
import java.util.Set;

/**
 * Record representing the result of a data subscription operation.
 */
public record DataSubscriptionResult(
    String subscriptionId,
    String strategyId,
    boolean success,
    String message,
    Set<String> subscribedSymbols,
    Set<String> subscribedDataTypes,
    Instant timestamp,
    String errorCode,
    String correlationId
) {}