package com.trademaster.trading.dto;

import java.time.Instant;
import java.util.List;

/**
 * Record representing the result of processing market data updates.
 */
public record MarketDataProcessingResult(
    String processingId,
    String strategyId,
    int updatesProcessed,
    int updatesSkipped,
    boolean success,
    String message,
    List<String> errors,
    Instant timestamp,
    long processingTimeMs,
    String correlationId
) {}