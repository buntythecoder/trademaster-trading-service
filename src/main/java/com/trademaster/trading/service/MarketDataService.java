package com.trademaster.trading.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data Service Interface
 * 
 * Provides real-time and historical market data for trading operations.
 * Supports multiple data sources and caching for high-performance access.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface MarketDataService {
    
    /**
     * Get current market price for a symbol
     */
    CompletableFuture<BigDecimal> getCurrentPrice(String symbol);
    
    /**
     * Get real-time quote data
     */
    CompletableFuture<QuoteData> getQuote(String symbol);
    
    /**
     * Get historical price data
     */
    CompletableFuture<List<PriceData>> getHistoricalPrices(String symbol, Instant from, Instant to);
    
    /**
     * Get market depth (order book)
     */
    CompletableFuture<MarketDepth> getMarketDepth(String symbol);
    
    /**
     * Get multiple quotes at once
     */
    CompletableFuture<Map<String, QuoteData>> getMultipleQuotes(List<String> symbols);
    
    /**
     * Check if market is open
     */
    CompletableFuture<Boolean> isMarketOpen();
    
    /**
     * Get market status
     */
    CompletableFuture<MarketStatus> getMarketStatus();
    
    /**
     * Subscribe to real-time price updates
     */
    void subscribeToPrice(String symbol, PriceUpdateListener listener);
    
    /**
     * Unsubscribe from price updates
     */
    void unsubscribeFromPrice(String symbol, PriceUpdateListener listener);
    
    // Supporting classes
    
    record QuoteData(
        String symbol,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal last,
        BigDecimal volume,
        Instant timestamp
    ) {}
    
    record PriceData(
        String symbol,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        Instant timestamp
    ) {}
    
    record MarketDepth(
        String symbol,
        List<OrderBookEntry> bids,
        List<OrderBookEntry> asks,
        Instant timestamp
    ) {}
    
    record OrderBookEntry(
        BigDecimal price,
        BigDecimal quantity,
        int orders
    ) {}
    
    enum MarketStatus {
        OPEN, CLOSED, PRE_MARKET, AFTER_HOURS, HOLIDAY
    }
    
    @FunctionalInterface
    interface PriceUpdateListener {
        void onPriceUpdate(String symbol, BigDecimal price, Instant timestamp);
    }
}