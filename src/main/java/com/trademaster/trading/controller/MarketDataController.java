package com.trademaster.trading.controller;

import com.trademaster.trading.agentos.agents.MarketDataAgent;
import com.trademaster.trading.dto.MarketDataMessage;
import com.trademaster.trading.service.MarketDataStreamingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data REST API Controller
 *
 * Provides HTTP endpoints for market data retrieval and WebSocket subscription management.
 * Delegates to MarketDataAgent for data fetching and MarketDataStreamingService for streaming.
 *
 * Endpoints:
 * - GET /api/v1/market-data/quote/{symbol}: Get real-time quote
 * - GET /api/v1/market-data/order-book/{symbol}: Get order book depth
 * - GET /api/v1/market-data/trades/{symbol}: Get recent trades
 * - GET /api/v1/market-data/status/{exchange}: Get market status
 * - GET /api/v1/market-data/subscription-stats: Get WebSocket subscription statistics
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/market-data")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Market Data", description = "Real-time market data and streaming APIs")
public class MarketDataController {

    private final MarketDataAgent marketDataAgent;
    private final MarketDataStreamingService streamingService;

    /**
     * Get real-time quote for a symbol.
     *
     * @param symbol Stock symbol (e.g., RELIANCE, TCS)
     * @param exchange Exchange identifier (NSE, BSE), defaults to NSE
     * @return CompletableFuture with PriceUpdate
     */
    @GetMapping("/quote/{symbol}")
    @Operation(summary = "Get real-time quote", description = "Fetches current market price with bid/ask spread")
    public CompletableFuture<ResponseEntity<MarketDataMessage.PriceUpdate>> getQuote(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "NSE") String exchange) {

        log.info("REST API: Fetching quote for symbol={}, exchange={}", symbol, exchange);

        return marketDataAgent.getRealTimeQuote(symbol, exchange)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to fetch quote for symbol={}", symbol, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Get order book depth for a symbol.
     *
     * @param symbol Stock symbol
     * @param exchange Exchange identifier, defaults to NSE
     * @return CompletableFuture with OrderBookUpdate
     */
    @GetMapping("/order-book/{symbol}")
    @Operation(summary = "Get order book", description = "Fetches top 5 bid and ask levels")
    public CompletableFuture<ResponseEntity<MarketDataMessage.OrderBookUpdate>> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "NSE") String exchange) {

        log.info("REST API: Fetching order book for symbol={}, exchange={}", symbol, exchange);

        return marketDataAgent.getOrderBook(symbol, exchange)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to fetch order book for symbol={}", symbol, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Get recent trades for a symbol.
     *
     * @param symbol Stock symbol
     * @param exchange Exchange identifier, defaults to NSE
     * @param limit Number of trades to return, defaults to 20
     * @return CompletableFuture with list of TradeExecution
     */
    @GetMapping("/trades/{symbol}")
    @Operation(summary = "Get recent trades", description = "Fetches recent trade executions")
    public CompletableFuture<ResponseEntity<List<MarketDataMessage.TradeExecution>>> getTrades(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("REST API: Fetching trades for symbol={}, exchange={}, limit={}",
                symbol, exchange, limit);

        return marketDataAgent.getTradeStream(symbol, exchange, limit)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to fetch trades for symbol={}", symbol, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Get market status for an exchange.
     *
     * @param exchange Exchange identifier (NSE, BSE)
     * @return CompletableFuture with MarketStatus
     */
    @GetMapping("/status/{exchange}")
    @Operation(summary = "Get market status", description = "Fetches current market session status")
    public CompletableFuture<ResponseEntity<MarketDataMessage.MarketStatus>> getMarketStatus(
            @PathVariable String exchange) {

        log.info("REST API: Fetching market status for exchange={}", exchange);

        return marketDataAgent.getMarketStatus(exchange)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to fetch market status for exchange={}", exchange, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Get cached price for a symbol (ultra-low latency).
     *
     * @param symbol Stock symbol
     * @return CompletableFuture with cached price
     */
    @GetMapping("/cached-price/{symbol}")
    @Operation(summary = "Get cached price", description = "Fetches cached price without external API call")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getCachedPrice(
            @PathVariable String symbol) {

        log.debug("REST API: Fetching cached price for symbol={}", symbol);

        return marketDataAgent.getCachedPrice(symbol)
            .thenApply(price -> ResponseEntity.ok(
                Map.<String, Object>of("symbol", symbol, "price", price)))
            .exceptionally(throwable -> {
                log.error("Failed to fetch cached price for symbol={}", symbol, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Get WebSocket subscription statistics.
     *
     * @return Subscription statistics including active connections and subscriptions
     */
    @GetMapping("/subscription-stats")
    @Operation(summary = "Get subscription statistics",
            description = "Fetches WebSocket subscription metrics for monitoring")
    public ResponseEntity<Map<String, Object>> getSubscriptionStats() {
        log.debug("REST API: Fetching subscription statistics");

        Map<String, Object> stats = streamingService.getSubscriptionStats();
        return ResponseEntity.ok(stats);
    }
}
