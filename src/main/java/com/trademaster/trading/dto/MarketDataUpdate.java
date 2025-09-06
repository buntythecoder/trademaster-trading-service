package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Record representing a market data update for a strategy.
 */
public record MarketDataUpdate(
    String updateId,
    String symbol,
    String dataType,
    BigDecimal value,
    BigDecimal previousValue,
    Instant timestamp,
    String source,
    Map<String, Object> additionalData,
    String correlationId
) {}