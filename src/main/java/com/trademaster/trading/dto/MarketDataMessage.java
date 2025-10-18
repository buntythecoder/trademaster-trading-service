package com.trademaster.trading.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Base sealed interface for all market data messages in the real-time streaming system.
 *
 * Utilizes Java 24 sealed interfaces with pattern matching for type-safe message handling.
 * All market data messages are immutable records ensuring thread-safety in virtual thread environment.
 *
 * Message Types:
 * - PriceUpdate: Real-time price changes for instruments
 * - OrderBookUpdate: Bid/ask order book changes
 * - TradeExecution: Individual trade executions
 * - MarketStatus: Market open/close/circuit breaker status
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = MarketDataMessage.PriceUpdate.class, name = "PRICE_UPDATE"),
    @JsonSubTypes.Type(value = MarketDataMessage.OrderBookUpdate.class, name = "ORDER_BOOK_UPDATE"),
    @JsonSubTypes.Type(value = MarketDataMessage.TradeExecution.class, name = "TRADE_EXECUTION"),
    @JsonSubTypes.Type(value = MarketDataMessage.MarketStatus.class, name = "MARKET_STATUS")
})
public sealed interface MarketDataMessage
    permits MarketDataMessage.PriceUpdate,
            MarketDataMessage.OrderBookUpdate,
            MarketDataMessage.TradeExecution,
            MarketDataMessage.MarketStatus {

    /**
     * Real-time price update message
     *
     * @param symbol Stock symbol (e.g., "RELIANCE", "TCS")
     * @param exchange Exchange identifier (NSE, BSE)
     * @param lastPrice Current market price
     * @param bid Highest bid price
     * @param ask Lowest ask price
     * @param volume Trading volume
     * @param changePercent Percentage change from previous close
     * @param timestamp Message timestamp
     */
    record PriceUpdate(
        String symbol,
        String exchange,
        BigDecimal lastPrice,
        BigDecimal bid,
        BigDecimal ask,
        Long volume,
        BigDecimal changePercent,
        Instant timestamp
    ) implements MarketDataMessage {}

    /**
     * Order book depth update message
     *
     * @param symbol Stock symbol
     * @param exchange Exchange identifier
     * @param bidDepth Top 5 bid levels
     * @param askDepth Top 5 ask levels
     * @param timestamp Message timestamp
     */
    record OrderBookUpdate(
        String symbol,
        String exchange,
        java.util.List<OrderLevel> bidDepth,
        java.util.List<OrderLevel> askDepth,
        Instant timestamp
    ) implements MarketDataMessage {}

    /**
     * Individual trade execution message
     *
     * @param symbol Stock symbol
     * @param exchange Exchange identifier
     * @param price Execution price
     * @param quantity Execution quantity
     * @param side Trade side (BUY/SELL)
     * @param timestamp Execution timestamp
     */
    record TradeExecution(
        String symbol,
        String exchange,
        BigDecimal price,
        Long quantity,
        String side,
        Instant timestamp
    ) implements MarketDataMessage {}

    /**
     * Market status change message
     *
     * @param exchange Exchange identifier
     * @param status Market status (OPEN, CLOSED, PRE_OPEN, CIRCUIT_BREAKER)
     * @param message Status description
     * @param timestamp Status change timestamp
     */
    record MarketStatus(
        String exchange,
        String status,
        String message,
        Instant timestamp
    ) implements MarketDataMessage {}

    /**
     * Order book level with price and quantity
     *
     * @param price Price level
     * @param quantity Total quantity at price level
     * @param orders Number of orders at price level
     */
    record OrderLevel(
        BigDecimal price,
        Long quantity,
        Integer orders
    ) {}
}
