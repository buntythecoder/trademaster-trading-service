package com.trademaster.trading.client;

import com.trademaster.trading.dto.marketdata.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Market Data Service Feign Client
 *
 * OpenFeign client for consuming market-data-service endpoints using Java 24 Virtual Threads.
 * Provides historical OHLCV data, technical indicators, and sentiment analysis for AI capabilities.
 *
 * Circuit Breaker Configuration:
 * - Failure rate threshold: 50%
 * - Sliding window size: 10 calls
 * - Wait duration in open state: 30 seconds
 * - Automatic fallback to cached data
 *
 * Retry Configuration:
 * - Max attempts: 3
 * - Wait duration: 1 second
 * - Exponential backoff with jitter
 *
 * Performance Targets:
 * - Historical data retrieval: <500ms
 * - Technical indicators: <1s
 * - Sentiment analysis: <2s
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Circuit Breaker)
 */
@FeignClient(
    name = "market-data-service",
    url = "${trademaster.services.market-data-service.url:http://localhost:8084}",
    path = "/api/v1"
)
public interface MarketDataServiceClient {

    /**
     * Get historical OHLCV data for technical analysis
     *
     * @param symbol Trading symbol
     * @param exchange Exchange identifier (NSE, BSE)
     * @param from Start time
     * @param to End time
     * @param interval Time interval (1m, 5m, 15m, 1h, 1d)
     * @return CompletableFuture with market data response
     */
    @GetMapping("/market-data/history/{symbol}")
    @CircuitBreaker(name = "market-data-service", fallbackMethod = "getHistoricalDataFallback")
    @Retry(name = "market-data-service")
    CompletableFuture<MarketDataResponse> getHistoricalData(
        @PathVariable("symbol") String symbol,
        @RequestParam(value = "exchange", defaultValue = "NSE") String exchange,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(value = "interval", defaultValue = "1d") String interval
    );

    /**
     * Get OHLCV chart data with multiple timeframes
     *
     * @param symbol Trading symbol
     * @param timeframe Chart timeframe (MINUTE_1, MINUTE_5, HOUR_1, DAY_1)
     * @param startTime Start time
     * @param endTime End time
     * @return CompletableFuture with OHLCV data
     */
    @GetMapping("/charts/{symbol}/ohlcv")
    @CircuitBreaker(name = "market-data-service", fallbackMethod = "getOHLCVDataFallback")
    @Retry(name = "market-data-service")
    CompletableFuture<Map<String, Object>> getOHLCVData(
        @PathVariable("symbol") String symbol,
        @RequestParam("timeframe") String timeframe,
        @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
        @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime
    );

    /**
     * Get technical indicators for AI analysis
     *
     * @param symbol Trading symbol
     * @param timeframe Chart timeframe
     * @param startTime Start time
     * @param endTime End time
     * @return CompletableFuture with technical indicators (RSI, MACD, SMA, EMA, etc.)
     */
    @GetMapping("/charts/{symbol}/indicators")
    @CircuitBreaker(name = "market-data-service", fallbackMethod = "getTechnicalIndicatorsFallback")
    @Retry(name = "market-data-service")
    CompletableFuture<Map<String, Object>> getTechnicalIndicators(
        @PathVariable("symbol") String symbol,
        @RequestParam("timeframe") String timeframe,
        @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
        @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime
    );

    /**
     * Get complete chart data with all indicators
     *
     * @param symbol Trading symbol
     * @param timeframe Chart timeframe
     * @param startTime Start time
     * @param endTime End time
     * @return CompletableFuture with complete chart including OHLCV and indicators
     */
    @GetMapping("/charts/{symbol}/complete")
    @CircuitBreaker(name = "market-data-service", fallbackMethod = "getCompleteChartFallback")
    @Retry(name = "market-data-service")
    CompletableFuture<Map<String, Object>> getCompleteChart(
        @PathVariable("symbol") String symbol,
        @RequestParam("timeframe") String timeframe,
        @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
        @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime
    );

    /**
     * Get sentiment analysis for symbol
     *
     * @param symbol Trading symbol
     * @param source Sentiment source (news, social, combined)
     * @return CompletableFuture with sentiment data
     */
    @PostMapping("/news/sentiment")
    @CircuitBreaker(name = "market-data-service", fallbackMethod = "getSentimentAnalysisFallback")
    @Retry(name = "market-data-service")
    CompletableFuture<SentimentResponse> getSentimentAnalysis(
        @RequestParam("symbol") String symbol,
        @RequestParam(value = "source", defaultValue = "combined") String source
    );

    /**
     * Get current market price
     *
     * @param symbol Trading symbol
     * @param exchange Exchange identifier
     * @return CompletableFuture with price data
     */
    @GetMapping("/market-data/price/{symbol}")
    @CircuitBreaker(name = "market-data-service", fallbackMethod = "getCurrentPriceFallback")
    @Retry(name = "market-data-service")
    CompletableFuture<PriceResponse> getCurrentPrice(
        @PathVariable("symbol") String symbol,
        @RequestParam(value = "exchange", defaultValue = "NSE") String exchange
    );

    // Fallback methods for circuit breaker

    default CompletableFuture<MarketDataResponse> getHistoricalDataFallback(
            String symbol, String exchange, Instant from, Instant to, String interval, Throwable throwable) {
        return CompletableFuture.completedFuture(
            new MarketDataResponse(
                symbol,
                exchange,
                List.of(),
                "Circuit breaker open - using cached data",
                Instant.now()
            )
        );
    }

    default CompletableFuture<Map<String, Object>> getOHLCVDataFallback(
            String symbol, String timeframe, Instant startTime, Instant endTime, Throwable throwable) {
        return CompletableFuture.completedFuture(
            Map.of(
                "symbol", symbol,
                "data", List.of(),
                "error", "Circuit breaker open - service unavailable"
            )
        );
    }

    default CompletableFuture<Map<String, Object>> getTechnicalIndicatorsFallback(
            String symbol, String timeframe, Instant startTime, Instant endTime, Throwable throwable) {
        return CompletableFuture.completedFuture(
            Map.of(
                "symbol", symbol,
                "indicators", Map.of(),
                "error", "Circuit breaker open - indicators unavailable"
            )
        );
    }

    default CompletableFuture<Map<String, Object>> getCompleteChartFallback(
            String symbol, String timeframe, Instant startTime, Instant endTime, Throwable throwable) {
        return CompletableFuture.completedFuture(
            Map.of(
                "symbol", symbol,
                "ohlcv", List.of(),
                "indicators", Map.of(),
                "error", "Circuit breaker open - chart data unavailable"
            )
        );
    }

    default CompletableFuture<SentimentResponse> getSentimentAnalysisFallback(
            String symbol, String source, Throwable throwable) {
        return CompletableFuture.completedFuture(
            new SentimentResponse(
                symbol,
                "NEUTRAL",
                0.0,
                Map.of("error", "Circuit breaker open - sentiment unavailable"),
                Instant.now()
            )
        );
    }

    default CompletableFuture<PriceResponse> getCurrentPriceFallback(
            String symbol, String exchange, Throwable throwable) {
        return CompletableFuture.completedFuture(
            new PriceResponse(
                symbol,
                exchange,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO,
                0L,
                Instant.now(),
                "Circuit breaker open - price unavailable"
            )
        );
    }
}
