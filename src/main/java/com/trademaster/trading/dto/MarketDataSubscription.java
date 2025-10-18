package com.trademaster.trading.dto;

import java.util.Set;

/**
 * Market Data Subscription Request and Response DTOs
 *
 * Immutable records for managing client subscriptions to market data streams.
 * Supports symbol-based subscriptions with configurable data types.
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
public sealed interface MarketDataSubscription {

    /**
     * Client subscription request
     *
     * @param action Subscription action (SUBSCRIBE, UNSUBSCRIBE)
     * @param symbols Set of symbols to subscribe/unsubscribe
     * @param dataTypes Types of data requested (PRICE, ORDER_BOOK, TRADES, STATUS)
     * @param exchange Target exchange (NSE, BSE, or null for all)
     */
    record Request(
        Action action,
        Set<String> symbols,
        Set<DataType> dataTypes,
        String exchange
    ) implements MarketDataSubscription {

        public enum Action {
            SUBSCRIBE,
            UNSUBSCRIBE
        }

        public enum DataType {
            PRICE,
            ORDER_BOOK,
            TRADES,
            STATUS
        }
    }

    /**
     * Subscription confirmation response
     *
     * @param success Subscription operation success status
     * @param subscribedSymbols Successfully subscribed symbols
     * @param message Response message
     * @param activeSubscriptions Total active subscriptions for client
     */
    record Response(
        boolean success,
        Set<String> subscribedSymbols,
        String message,
        int activeSubscriptions
    ) implements MarketDataSubscription {}
}
