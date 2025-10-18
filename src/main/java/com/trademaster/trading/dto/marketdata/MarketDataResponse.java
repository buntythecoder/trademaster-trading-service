package com.trademaster.trading.dto.marketdata;

import java.time.Instant;
import java.util.List;

/**
 * Market Data Response DTO
 *
 * Immutable data transfer object for historical market data from market-data-service.
 * Contains OHLCV data for technical analysis and AI recommendations.
 *
 * @param symbol Trading symbol
 * @param exchange Exchange identifier (NSE, BSE)
 * @param data List of OHLCV data points
 * @param message Response message or error
 * @param timestamp Response timestamp
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Records)
 */
public record MarketDataResponse(
    String symbol,
    String exchange,
    List<OHLCVDataPoint> data,
    String message,
    Instant timestamp
) {
    /**
     * OHLCV Data Point
     *
     * @param timestamp Data point timestamp
     * @param open Opening price
     * @param high Highest price
     * @param low Lowest price
     * @param close Closing price
     * @param volume Trading volume
     */
    public record OHLCVDataPoint(
        Instant timestamp,
        java.math.BigDecimal open,
        java.math.BigDecimal high,
        java.math.BigDecimal low,
        java.math.BigDecimal close,
        Long volume
    ) {}
}
