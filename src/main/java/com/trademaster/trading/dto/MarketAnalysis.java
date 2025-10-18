package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Market Analysis Data Transfer Objects
 *
 * Contains DTOs for market analysis results including technical indicators,
 * sentiment analysis, risk metrics, and trade recommendations.
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
public sealed interface MarketAnalysis {

    /**
     * OHLCV (Open, High, Low, Close, Volume) candlestick data.
     */
    record OHLCVData(
        String symbol,
        Instant timestamp,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume,
        String timeframe  // 1m, 5m, 15m, 1h, 1d
    ) implements MarketAnalysis {}

    /**
     * Technical indicators result with calculated values.
     */
    record TechnicalIndicators(
        String symbol,
        Instant timestamp,

        // Trend Indicators
        BigDecimal sma20,        // 20-period Simple Moving Average
        BigDecimal sma50,        // 50-period Simple Moving Average
        BigDecimal ema12,        // 12-period Exponential Moving Average
        BigDecimal ema26,        // 26-period Exponential Moving Average

        // Momentum Indicators
        BigDecimal rsi,          // Relative Strength Index (0-100)
        BigDecimal macd,         // MACD line
        BigDecimal macdSignal,   // MACD signal line
        BigDecimal macdHistogram, // MACD histogram

        // Volatility Indicators
        BigDecimal bollingerUpper,  // Bollinger Bands upper
        BigDecimal bollingerMiddle, // Bollinger Bands middle (SMA20)
        BigDecimal bollingerLower,  // Bollinger Bands lower
        BigDecimal atr,             // Average True Range

        // Volume Indicators
        BigDecimal volumeSMA,       // Volume moving average
        BigDecimal volumeRatio,     // Current volume / Average volume

        // Derived Signals
        TrendSignal trend,          // BULLISH, BEARISH, NEUTRAL
        MomentumSignal momentum,    // STRONG_BUY, BUY, NEUTRAL, SELL, STRONG_SELL
        Integer signalStrength      // 0-100 confidence score
    ) implements MarketAnalysis {

        public enum TrendSignal {
            STRONG_BULLISH, BULLISH, NEUTRAL, BEARISH, STRONG_BEARISH
        }

        public enum MomentumSignal {
            STRONG_BUY, BUY, NEUTRAL, SELL, STRONG_SELL
        }
    }

    /**
     * Sentiment analysis result based on price action and market behavior.
     */
    record SentimentAnalysis(
        String symbol,
        Instant timestamp,

        // Price-based Sentiment
        BigDecimal priceChangePercent,  // Daily price change %
        BigDecimal volatilityScore,     // 0-100 (higher = more volatile)

        // Volume-based Sentiment
        BigDecimal volumeScore,         // 0-100 (higher = stronger conviction)
        BigDecimal buyPressure,         // 0-100 (buy volume intensity)
        BigDecimal sellPressure,        // 0-100 (sell volume intensity)

        // Market Breadth
        Integer advanceDeclineRatio,    // Advancing vs declining issues
        BigDecimal marketStrength,      // 0-100 overall market strength

        // Composite Sentiment
        SentimentScore overallSentiment,  // VERY_BULLISH to VERY_BEARISH
        Integer confidenceScore,          // 0-100 confidence in sentiment
        String sentimentReason            // Explanation of sentiment
    ) implements MarketAnalysis {

        public enum SentimentScore {
            VERY_BULLISH, BULLISH, NEUTRAL, BEARISH, VERY_BEARISH
        }
    }

    /**
     * Risk assessment metrics for a trading opportunity.
     */
    record RiskAssessment(
        String symbol,
        Instant timestamp,

        // Risk Metrics
        BigDecimal expectedReturn,      // Expected return %
        BigDecimal volatility,          // Annualized volatility %
        BigDecimal sharpeRatio,         // Risk-adjusted return
        BigDecimal maxDrawdown,         // Maximum historical drawdown %
        BigDecimal valueAtRisk,         // VaR at 95% confidence

        // Position Sizing
        BigDecimal recommendedPosition,  // Position size as % of portfolio
        BigDecimal stopLossPrice,       // Recommended stop-loss
        BigDecimal takeProfitPrice,     // Recommended take-profit

        // Risk Classification
        RiskLevel riskLevel,            // LOW, MEDIUM, HIGH, EXTREME
        Integer riskScore,              // 0-100 (higher = riskier)
        String riskFactors              // Key risk factors identified
    ) implements MarketAnalysis {

        public enum RiskLevel {
            LOW, MEDIUM, HIGH, EXTREME
        }
    }

    /**
     * Comprehensive trade recommendation with multi-agent analysis.
     */
    record TradeRecommendation(
        String recommendationId,
        String symbol,
        Instant timestamp,

        // Recommendation Details
        Action action,                  // BUY, SELL, HOLD
        BigDecimal entryPrice,          // Recommended entry price
        BigDecimal targetPrice,         // Price target
        BigDecimal stopLoss,            // Stop-loss price
        Integer quantity,               // Recommended quantity

        // Multi-Agent Analysis
        TechnicalIndicators technicalAnalysis,
        SentimentAnalysis sentimentAnalysis,
        RiskAssessment riskAssessment,

        // Composite Scores
        Integer overallScore,           // 0-100 overall recommendation strength
        Integer confidenceLevel,        // 0-100 confidence in recommendation
        String strategy,                // MOMENTUM, MEAN_REVERSION, BREAKOUT, etc.
        String timeframe,               // SHORT_TERM, MEDIUM_TERM, LONG_TERM

        // Reasoning
        String primaryReason,           // Main reason for recommendation
        List<String> supportingFactors, // Additional supporting factors
        List<String> risks,             // Identified risks

        // Metadata
        String generatedBy,             // Agent that generated recommendation
        Instant expiresAt               // Recommendation expiration
    ) implements MarketAnalysis {

        public enum Action {
            STRONG_BUY, BUY, HOLD, SELL, STRONG_SELL
        }
    }

    /**
     * Pattern recognition result identifying chart patterns.
     */
    record PatternRecognition(
        String symbol,
        Instant timestamp,

        // Detected Patterns
        List<DetectedPattern> patterns,

        // Pattern-based Signal
        String dominantPattern,         // Most significant pattern
        BigDecimal patternConfidence,   // 0-100 pattern recognition confidence
        String expectedMove             // Expected price movement
    ) implements MarketAnalysis {}

    /**
     * Individual detected chart pattern.
     */
    record DetectedPattern(
        String patternName,             // HEAD_AND_SHOULDERS, TRIANGLE, etc.
        PatternType type,               // REVERSAL, CONTINUATION
        BigDecimal confidence,          // 0-100 pattern confidence
        String signal,                  // BULLISH, BEARISH, NEUTRAL
        Instant startTime,
        Instant endTime
    ) {
        public enum PatternType {
            REVERSAL, CONTINUATION, CONSOLIDATION
        }
    }

    /**
     * Multi-timeframe analysis result.
     */
    record MultiTimeframeAnalysis(
        String symbol,
        Instant timestamp,

        // Different Timeframe Signals
        TimeframeSignal shortTerm,      // 5m, 15m analysis
        TimeframeSignal mediumTerm,     // 1h, 4h analysis
        TimeframeSignal longTerm,       // 1d, 1w analysis

        // Alignment Score
        Integer alignmentScore,         // 0-100 (timeframes aligned)
        String trend,                   // ALIGNED_BULLISH, MIXED, ALIGNED_BEARISH
        String recommendation           // Overall multi-timeframe recommendation
    ) implements MarketAnalysis {}

    /**
     * Timeframe-specific analysis signal.
     */
    record TimeframeSignal(
        String timeframe,
        String signal,                  // BUY, SELL, HOLD
        Integer strength,               // 0-100 signal strength
        BigDecimal price,
        TechnicalIndicators indicators
    ) {}
}
