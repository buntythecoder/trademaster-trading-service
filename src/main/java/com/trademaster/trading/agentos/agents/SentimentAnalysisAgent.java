package com.trademaster.trading.agentos.agents;

import com.trademaster.trading.agentos.AgentCapability;
import com.trademaster.trading.agentos.AgentOSComponent;
import com.trademaster.trading.agentos.EventHandler;
import com.trademaster.trading.agentos.TradingCapabilityRegistry;
import com.trademaster.trading.dto.MarketAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sentiment Analysis Agent for AgentOS Framework
 *
 * Analyzes market sentiment using price action, volume patterns, and market breadth indicators.
 * Implements quantitative sentiment scoring algorithms to gauge market psychology and conviction.
 *
 * Agent Capabilities:
 * - SENTIMENT_SCORING: Calculate sentiment from price/volume patterns
 * - VOLUME_ANALYSIS: Analyze buying/selling pressure
 * - MARKET_BREADTH: Assess overall market strength
 * - CONVICTION_MEASUREMENT: Measure market participant conviction
 * - SENTIMENT_PREDICTION: Predict sentiment shifts
 *
 * Sentiment Algorithms:
 * - Price Momentum: Rate of price change with decay function
 * - Volume Profile: Buy/sell pressure from volume and price direction
 * - Volatility Analysis: Sentiment uncertainty from price swings
 * - Market Breadth: Advance/decline ratios and market participation
 * - Composite Scoring: Multi-factor sentiment model
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalysisAgent implements AgentOSComponent {

    private final TradingCapabilityRegistry capabilityRegistry;

    private static final int LOOKBACK_PERIODS = 20;
    private static final BigDecimal HIGH_VOLATILITY_THRESHOLD = new BigDecimal("3.0");
    private static final BigDecimal STRONG_VOLUME_THRESHOLD = new BigDecimal("1.5");

    /**
     * Analyzes market sentiment based on price action and volume patterns.
     *
     * @param symbol Stock symbol
     * @param ohlcvData Historical OHLCV data
     * @return CompletableFuture with SentimentAnalysis
     */
    @EventHandler(event = "SentimentAnalysisRequest")
    @AgentCapability(
        name = "SENTIMENT_SCORING",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<MarketAnalysis.SentimentAnalysis> analyzeSentiment(
            String symbol, List<MarketAnalysis.OHLCVData> ohlcvData) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Analyzing sentiment for symbol={}, periods={}", symbol, ohlcvData.size());

            try {
                if (ohlcvData.size() < 2) {
                    throw new IllegalArgumentException("Insufficient data for sentiment analysis");
                }

                // Extract recent data
                MarketAnalysis.OHLCVData current = ohlcvData.get(ohlcvData.size() - 1);
                MarketAnalysis.OHLCVData previous = ohlcvData.get(ohlcvData.size() - 2);

                // Calculate price-based sentiment
                BigDecimal priceChangePercent = calculatePriceChangePercent(current.close(), previous.close());
                BigDecimal volatilityScore = calculateVolatilityScore(ohlcvData);

                // Calculate volume-based sentiment
                BigDecimal volumeScore = calculateVolumeScore(ohlcvData);
                BigDecimal buyPressure = calculateBuyPressure(current, ohlcvData);
                BigDecimal sellPressure = calculateSellPressure(current, ohlcvData);

                // Calculate market breadth
                Integer advanceDeclineRatio = calculateAdvanceDeclineRatio(ohlcvData);
                BigDecimal marketStrength = calculateMarketStrength(
                    priceChangePercent, volumeScore, buyPressure, sellPressure
                );

                // Determine overall sentiment
                var overallSentiment = determineOverallSentiment(
                    priceChangePercent, volatilityScore, buyPressure, sellPressure, marketStrength
                );
                Integer confidenceScore = calculateConfidenceScore(
                    volatilityScore, volumeScore, marketStrength
                );
                String sentimentReason = generateSentimentReason(
                    overallSentiment, priceChangePercent, buyPressure, sellPressure, volatilityScore
                );

                var sentiment = new MarketAnalysis.SentimentAnalysis(
                    symbol,
                    Instant.now(),
                    priceChangePercent,
                    volatilityScore,
                    volumeScore,
                    buyPressure,
                    sellPressure,
                    advanceDeclineRatio,
                    marketStrength,
                    overallSentiment,
                    confidenceScore,
                    sentimentReason
                );

                capabilityRegistry.recordSuccessfulExecution("SENTIMENT_SCORING");

                log.info("Sentiment analyzed: symbol={}, sentiment={}, confidence={}, reason={}",
                        symbol, overallSentiment, confidenceScore, sentimentReason);

                return sentiment;

            } catch (Exception e) {
                log.error("Failed to analyze sentiment for symbol={}", symbol, e);
                capabilityRegistry.recordFailedExecution("SENTIMENT_SCORING", e);
                throw new RuntimeException("Sentiment analysis failed", e);
            }
        });
    }

    /**
     * Calculates price change percentage.
     */
    private BigDecimal calculatePriceChangePercent(BigDecimal currentPrice, BigDecimal previousPrice) {
        if (previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentPrice.subtract(previousPrice)
            .divide(previousPrice, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates volatility score (0-100) based on price swings.
     * Higher volatility = higher uncertainty in sentiment.
     *
     * Uses average true range percentage to measure volatility.
     */
    private BigDecimal calculateVolatilityScore(List<MarketAnalysis.OHLCVData> ohlcvData) {
        int periods = Math.min(LOOKBACK_PERIODS, ohlcvData.size());
        if (periods < 2) {
            return new BigDecimal("50");  // Neutral volatility
        }

        List<MarketAnalysis.OHLCVData> recentData = ohlcvData.subList(ohlcvData.size() - periods, ohlcvData.size());

        // Calculate average true range percentage
        double avgTrueRangePercent = recentData.stream()
            .mapToDouble(candle -> {
                BigDecimal range = candle.high().subtract(candle.low());
                BigDecimal rangePercent = range.divide(candle.close(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                return rangePercent.doubleValue();
            })
            .average()
            .orElse(0.0);

        // Normalize to 0-100 scale (0-5% range maps to 0-100 score)
        BigDecimal volatilityScore = BigDecimal.valueOf(avgTrueRangePercent)
            .multiply(new BigDecimal("20"))  // Scale factor
            .setScale(2, RoundingMode.HALF_UP);

        // Cap at 100
        return volatilityScore.min(new BigDecimal("100"));
    }

    /**
     * Calculates volume score (0-100) based on recent volume trends.
     * Higher volume = stronger conviction.
     */
    private BigDecimal calculateVolumeScore(List<MarketAnalysis.OHLCVData> ohlcvData) {
        int periods = Math.min(LOOKBACK_PERIODS, ohlcvData.size());
        if (periods < 2) {
            return new BigDecimal("50");
        }

        List<MarketAnalysis.OHLCVData> recentData = ohlcvData.subList(ohlcvData.size() - periods, ohlcvData.size());

        // Calculate average volume
        double avgVolume = recentData.stream()
            .mapToLong(MarketAnalysis.OHLCVData::volume)
            .average()
            .orElse(1.0);

        // Get current volume
        long currentVolume = ohlcvData.get(ohlcvData.size() - 1).volume();

        // Calculate volume ratio
        double volumeRatio = currentVolume / avgVolume;

        // Normalize to 0-100 scale (0.5x-2.0x volume maps to 25-100)
        BigDecimal volumeScore = BigDecimal.valueOf(volumeRatio)
            .multiply(new BigDecimal("50"))
            .setScale(2, RoundingMode.HALF_UP);

        // Clamp between 0-100
        return volumeScore.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * Calculates buy pressure (0-100) based on price action and volume.
     * Higher buy pressure = more aggressive buying.
     *
     * Uses close position within range and volume to infer buying pressure.
     */
    private BigDecimal calculateBuyPressure(
            MarketAnalysis.OHLCVData current, List<MarketAnalysis.OHLCVData> ohlcvData) {

        // Calculate where close is within the day's range
        BigDecimal range = current.high().subtract(current.low());
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("50");
        }

        BigDecimal closePosition = current.close().subtract(current.low())
            .divide(range, 4, RoundingMode.HALF_UP);  // 0 = at low, 1 = at high

        // Weight by volume strength
        BigDecimal volumeScore = calculateVolumeScore(ohlcvData);
        BigDecimal volumeWeight = volumeScore.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        // Calculate weighted buy pressure
        BigDecimal basePressure = closePosition.multiply(new BigDecimal("100"));
        BigDecimal weightedPressure = basePressure.multiply(volumeWeight);

        // Blend base pressure (70%) with volume-weighted (30%)
        return basePressure.multiply(new BigDecimal("0.7"))
            .add(weightedPressure.multiply(new BigDecimal("0.3")))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates sell pressure (0-100) - inverse of buy pressure.
     */
    private BigDecimal calculateSellPressure(
            MarketAnalysis.OHLCVData current, List<MarketAnalysis.OHLCVData> ohlcvData) {

        BigDecimal buyPressure = calculateBuyPressure(current, ohlcvData);
        return new BigDecimal("100").subtract(buyPressure);
    }

    /**
     * Calculates advance/decline ratio as market breadth indicator.
     * Positive = more advancing than declining.
     */
    private Integer calculateAdvanceDeclineRatio(List<MarketAnalysis.OHLCVData> ohlcvData) {
        int periods = Math.min(LOOKBACK_PERIODS, ohlcvData.size());
        if (periods < 2) {
            return 0;
        }

        List<MarketAnalysis.OHLCVData> recentData = ohlcvData.subList(ohlcvData.size() - periods, ohlcvData.size());

        // Count advancing vs declining periods
        long advancing = recentData.stream()
            .filter(candle -> candle.close().compareTo(candle.open()) > 0)
            .count();

        long declining = periods - advancing;

        // Calculate ratio (-100 to +100)
        if (declining == 0) return 100;
        if (advancing == 0) return -100;

        return (int) ((advancing - declining) * 100 / periods);
    }

    /**
     * Calculates overall market strength (0-100).
     * Combines price momentum, volume, and buy/sell pressure.
     */
    private BigDecimal calculateMarketStrength(
            BigDecimal priceChange, BigDecimal volumeScore,
            BigDecimal buyPressure, BigDecimal sellPressure) {

        // Normalize price change to 0-100 scale
        BigDecimal priceScore = priceChange.add(new BigDecimal("10"))
            .divide(new BigDecimal("20"), 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        priceScore = priceScore.max(BigDecimal.ZERO).min(new BigDecimal("100"));

        // Calculate pressure differential
        BigDecimal pressureDiff = buyPressure.subtract(sellPressure);
        BigDecimal pressureScore = pressureDiff.add(new BigDecimal("100"))
            .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);

        // Weighted average: price (40%), volume (30%), pressure (30%)
        return priceScore.multiply(new BigDecimal("0.4"))
            .add(volumeScore.multiply(new BigDecimal("0.3")))
            .add(pressureScore.multiply(new BigDecimal("0.3")))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Determines overall sentiment based on multiple factors.
     */
    private MarketAnalysis.SentimentAnalysis.SentimentScore determineOverallSentiment(
            BigDecimal priceChange, BigDecimal volatility,
            BigDecimal buyPressure, BigDecimal sellPressure,
            BigDecimal marketStrength) {

        // Strong bullish: price up, high buy pressure, strong market
        if (priceChange.compareTo(new BigDecimal("2")) > 0 &&
            buyPressure.compareTo(new BigDecimal("70")) > 0 &&
            marketStrength.compareTo(new BigDecimal("70")) > 0) {
            return MarketAnalysis.SentimentAnalysis.SentimentScore.VERY_BULLISH;
        }

        // Bullish: price up or strong buy pressure
        if (priceChange.compareTo(BigDecimal.ZERO) > 0 ||
            buyPressure.compareTo(new BigDecimal("60")) > 0) {
            return MarketAnalysis.SentimentAnalysis.SentimentScore.BULLISH;
        }

        // Strong bearish: price down, high sell pressure, weak market
        if (priceChange.compareTo(new BigDecimal("-2")) < 0 &&
            sellPressure.compareTo(new BigDecimal("70")) > 0 &&
            marketStrength.compareTo(new BigDecimal("30")) < 0) {
            return MarketAnalysis.SentimentAnalysis.SentimentScore.VERY_BEARISH;
        }

        // Bearish: price down or strong sell pressure
        if (priceChange.compareTo(BigDecimal.ZERO) < 0 ||
            sellPressure.compareTo(new BigDecimal("60")) > 0) {
            return MarketAnalysis.SentimentAnalysis.SentimentScore.BEARISH;
        }

        return MarketAnalysis.SentimentAnalysis.SentimentScore.NEUTRAL;
    }

    /**
     * Calculates confidence score (0-100) in sentiment assessment.
     * Lower volatility + higher volume = higher confidence.
     */
    private Integer calculateConfidenceScore(
            BigDecimal volatility, BigDecimal volumeScore, BigDecimal marketStrength) {

        // Inverse volatility (low volatility = high confidence)
        BigDecimal volatilityConfidence = new BigDecimal("100").subtract(volatility)
            .max(BigDecimal.ZERO);

        // Volume confidence (high volume = high confidence)
        BigDecimal volumeConfidence = volumeScore;

        // Market strength confidence
        BigDecimal strengthConfidence = marketStrength;

        // Weighted average: volatility (40%), volume (35%), strength (25%)
        BigDecimal confidence = volatilityConfidence.multiply(new BigDecimal("0.4"))
            .add(volumeConfidence.multiply(new BigDecimal("0.35")))
            .add(strengthConfidence.multiply(new BigDecimal("0.25")));

        return confidence.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * Generates human-readable reason for sentiment assessment.
     */
    private String generateSentimentReason(
            MarketAnalysis.SentimentAnalysis.SentimentScore sentiment,
            BigDecimal priceChange, BigDecimal buyPressure,
            BigDecimal sellPressure, BigDecimal volatility) {

        return switch (sentiment) {
            case VERY_BULLISH -> String.format(
                "Strong positive momentum with %.2f%% price gain, %.0f%% buy pressure, and stable volatility",
                priceChange, buyPressure
            );
            case BULLISH -> String.format(
                "Positive sentiment driven by %.2f%% price movement and %.0f%% buy pressure",
                priceChange, buyPressure
            );
            case NEUTRAL -> String.format(
                "Balanced market with %.2f%% price change and mixed buy/sell pressure (%.0f%%/%.0f%%)",
                priceChange, buyPressure, sellPressure
            );
            case BEARISH -> String.format(
                "Negative sentiment with %.2f%% price decline and %.0f%% sell pressure",
                priceChange, sellPressure
            );
            case VERY_BEARISH -> String.format(
                "Strong selling pressure with %.2f%% loss, %.0f%% sell pressure, and elevated %.0f%% volatility",
                priceChange, sellPressure, volatility
            );
        };
    }

    // ========== AgentOSComponent Implementation ==========

    @Override
    public String getAgentId() {
        return "sentiment-analysis-agent";
    }

    @Override
    public String getAgentType() {
        return "SENTIMENT_ANALYSIS";
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
            "SENTIMENT_SCORING",
            "VOLUME_ANALYSIS",
            "MARKET_BREADTH",
            "CONVICTION_MEASUREMENT",
            "SENTIMENT_PREDICTION"
        );
    }

    @Override
    public Double getHealthScore() {
        return capabilityRegistry.calculateOverallHealthScore();
    }
}
