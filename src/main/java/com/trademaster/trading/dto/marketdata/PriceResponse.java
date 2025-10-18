package com.trademaster.trading.dto.marketdata;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Price Response DTO
 *
 * Immutable data transfer object for current market price from market-data-service.
 *
 * @param symbol Trading symbol
 * @param exchange Exchange identifier (NSE, BSE)
 * @param price Current market price
 * @param bid Best bid price
 * @param ask Best ask price
 * @param volume Current trading volume
 * @param timestamp Price timestamp
 * @param message Response message or error
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Records)
 */
public record PriceResponse(
    String symbol,
    String exchange,
    BigDecimal price,
    BigDecimal bid,
    BigDecimal ask,
    Long volume,
    Instant timestamp,
    String message
) {}
