package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Trading Signal DTO
 * 
 * Comprehensive trading signal representation with:
 * - Multi-dimensional signal strength and confidence scoring
 * - Advanced signal attribution and source tracking
 * - Risk-adjusted signal sizing and timing recommendations
 * - Machine learning prediction integration
 * - Real-time signal validation and quality metrics
 * - Multi-timeframe signal analysis and confirmation
 * - Signal execution tracking and performance feedback
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingSignal {
    
    /**
     * Signal Identification and Metadata
     */
    private String signalId;
    private String strategyId;
    private String strategyName;
    private Long userId;
    private String symbol;
    private String exchange;
    private String assetClass;
    private Instant generatedAt;
    private Instant expiresAt;
    private String signalVersion; // Signal algorithm version
    
    /**
     * Core Signal Information
     */
    private SignalCore signalCore;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignalCore {
        private String signalType; // BUY, SELL, HOLD, STRONG_BUY, STRONG_SELL
        private String action; // OPEN_LONG, CLOSE_LONG, OPEN_SHORT, CLOSE_SHORT
        private BigDecimal strength; // Signal strength 0.0 to 1.0
        private BigDecimal confidence; // Signal confidence 0.0 to 1.0
        private String priority; // LOW, MEDIUM, HIGH, URGENT
        private String timeframe; // 1m, 5m, 15m, 1h, 4h, 1d, 1w
        private String signalSource; // TECHNICAL, FUNDAMENTAL, SENTIMENT, ML, HYBRID
        private Boolean isConfirmed; // Signal confirmation status
        private LocalDateTime confirmationTime; // When signal was confirmed
        private String confirmationMethod; // Multi-timeframe, cross-validation, etc.
    }
    
    /**
     * Market Context at Signal Generation
     */
    private MarketContext marketContext;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketContext {
        private BigDecimal currentPrice; // Price when signal generated
        private BigDecimal volume; // Current volume
        private BigDecimal volatility; // Current volatility measure
        private String marketTrend; // BULLISH, BEARISH, SIDEWAYS
        private String marketRegime; // TRENDING, MEAN_REVERTING, HIGH_VOLATILITY
        private BigDecimal bidAskSpread; // Current spread
        private String liquidityCondition; // HIGH, MEDIUM, LOW
        private BigDecimal dayChange; // Day price change
        private BigDecimal dayChangePercent; // Day change percentage
        private LocalDateTime marketOpen; // Market open time
        private LocalDateTime marketClose; // Market close time
        private Boolean isMarketHours; // Trading during market hours
        private Map<String, BigDecimal> sectorPerformance; // Sector context
    }
    
    /**
     * Technical Analysis Attribution
     */
    private TechnicalAnalysis technicalAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicalAnalysis {
        // Indicator Contributions
        private Map<String, BigDecimal> indicatorSignals; // RSI, MACD, SMA, etc.
        private Map<String, BigDecimal> indicatorWeights; // Weight of each indicator
        private BigDecimal technicalScore; // Composite technical score
        
        // Price Pattern Recognition
        private List<String> identifiedPatterns; // Head-shoulders, triangle, etc.
        private Map<String, BigDecimal> patternConfidence; // Pattern confidence scores
        private String primaryPattern; // Most significant pattern
        private String patternDirection; // BULLISH, BEARISH, NEUTRAL
        
        // Support and Resistance
        private List<BigDecimal> supportLevels; // Identified support levels
        private List<BigDecimal> resistanceLevels; // Identified resistance levels
        private BigDecimal nearestSupport; // Closest support level
        private BigDecimal nearestResistance; // Closest resistance level
        private String positionVsLevels; // ABOVE_RESISTANCE, BELOW_SUPPORT, etc.
        
        // Momentum Analysis
        private BigDecimal momentum; // Price momentum measure
        private String momentumTrend; // ACCELERATING, DECELERATING, STABLE
        private BigDecimal rsi; // Relative Strength Index
        private String rsiCondition; // OVERBOUGHT, OVERSOLD, NEUTRAL
        private BigDecimal macd; // MACD value
        private String macdSignal; // BULLISH_CROSSOVER, BEARISH_CROSSOVER, etc.
        
        // Volume Analysis
        private BigDecimal volumeRatio; // Current vs average volume
        private String volumeCondition; // HIGH, AVERAGE, LOW
        private String volumePrice; // CONFIRMING, DIVERGING
        private BigDecimal onBalanceVolume; // OBV indicator
        private String volumePattern; // ACCUMULATION, DISTRIBUTION, NEUTRAL
    }
    
    /**
     * Fundamental Analysis Attribution
     */
    private FundamentalAnalysis fundamentalAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundamentalAnalysis {
        private BigDecimal fundamentalScore; // Composite fundamental score
        private String valuationRating; // UNDERVALUED, FAIR_VALUE, OVERVALUED
        private BigDecimal priceToEarnings; // P/E ratio
        private BigDecimal priceToBook; // P/B ratio
        private String financialHealth; // STRONG, MODERATE, WEAK
        private String growthProspects; // HIGH, MEDIUM, LOW
        private String industryOutlook; // POSITIVE, NEUTRAL, NEGATIVE
        private Map<String, String> analystRatings; // Analyst recommendations
        private String earningsRevisionTrend; // UP, DOWN, STABLE
        private LocalDateTime nextEarningsDate; // Next earnings announcement
        private Boolean isEarningsSeason; // Currently in earnings season
        private List<String> catalysts; // Upcoming catalysts
        private String competitivePosition; // LEADER, CHALLENGER, FOLLOWER
    }
    
    /**
     * Sentiment Analysis
     */
    private SentimentAnalysis sentimentAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentAnalysis {
        private BigDecimal sentimentScore; // Overall sentiment score -1 to +1
        private String sentimentDirection; // BULLISH, BEARISH, NEUTRAL
        private String sentimentStrength; // STRONG, MODERATE, WEAK
        
        // News and Social Media
        private BigDecimal newsSentiment; // News sentiment score
        private Integer newsVelocity; // News article count last 24h
        private List<String> keyNewsTopics; // Main news themes
        private BigDecimal socialMediaSentiment; // Social media sentiment
        private Integer socialMediaMentions; // Mention count
        private String socialMediaTrend; // RISING, FALLING, STABLE
        
        // Market Sentiment Indicators
        private BigDecimal putCallRatio; // Options put/call ratio
        private String putCallSignal; // BULLISH, BEARISH, NEUTRAL
        private BigDecimal vixLevel; // VIX fear/greed indicator
        private String vixSignal; // FEAR, GREED, NEUTRAL
        private BigDecimal shortInterest; // Short interest ratio
        private String shortInterestTrend; // INCREASING, DECREASING, STABLE
        
        // Institutional Activity
        private String institutionalFlow; // INFLOW, OUTFLOW, NEUTRAL
        private BigDecimal institutionalOwnership; // Institutional ownership %
        private String insiderTrading; // BUYING, SELLING, NEUTRAL
        private String analystSentiment; // POSITIVE, NEGATIVE, NEUTRAL
    }
    
    /**
     * Machine Learning Predictions
     */
    private MLPredictions mlPredictions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MLPredictions {
        private BigDecimal mlSignalStrength; // ML model signal strength
        private BigDecimal predictionConfidence; // Model prediction confidence
        private String modelType; // REGRESSION, CLASSIFICATION, ENSEMBLE
        private String modelVersion; // Model version used
        private Instant modelLastTrained; // When model was last trained
        
        // Price Predictions
        private BigDecimal predictedReturn; // Predicted return %
        private BigDecimal returnProbability; // Probability of predicted return
        private Map<String, BigDecimal> returnScenarios; // Multiple return scenarios
        private BigDecimal predictedVolatility; // Predicted volatility
        private Integer predictionHorizon; // Prediction time horizon (hours)
        
        // Feature Importance
        private Map<String, BigDecimal> featureImportance; // Feature importance scores
        private List<String> topFeatures; // Most important features
        private String primaryDriver; // Main signal driver
        
        // Model Performance
        private BigDecimal modelAccuracy; // Model accuracy score
        private BigDecimal modelSharpe; // Model Sharpe ratio
        private Integer modelTradeCount; // Trades used in training
        private String modelValidation; // Validation method used
        
        // Uncertainty Quantification
        private BigDecimal predictionStdDev; // Prediction standard deviation
        private BigDecimal confidenceInterval; // 95% confidence interval
        private String uncertaintySource; // Model, data, market uncertainty
    }
    
    /**
     * Signal Execution Recommendations
     */
    private ExecutionRecommendation executionRecommendation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionRecommendation {
        // Entry Recommendations
        private BigDecimal recommendedPrice; // Recommended entry price
        private BigDecimal priceRange; // Acceptable price range
        private String orderType; // MARKET, LIMIT, STOP_LIMIT
        private BigDecimal limitPrice; // Limit order price
        private BigDecimal stopPrice; // Stop order price
        private String timeInForce; // GTC, DAY, IOC, FOK
        
        // Position Sizing
        private BigDecimal recommendedSize; // Recommended position size
        private String sizingMethod; // FIXED, RISK_BASED, KELLY, VOLATILITY
        private BigDecimal riskAmount; // Amount at risk
        private BigDecimal riskPercent; // Portfolio risk percentage
        private BigDecimal maxPositionSize; // Maximum allowed size
        
        // Risk Management
        private BigDecimal stopLoss; // Stop loss price
        private BigDecimal takeProfit; // Take profit price
        private BigDecimal riskReward; // Risk/reward ratio
        private Boolean enableTrailingStop; // Use trailing stop
        private BigDecimal trailingPercent; // Trailing stop percentage
        
        // Execution Timing
        private String executionUrgency; // IMMEDIATE, BEFORE_CLOSE, NEXT_OPEN
        private LocalDateTime recommendedTime; // Best execution time
        private String marketTiming; // MARKET_OPEN, MID_DAY, MARKET_CLOSE
        private Integer maxExecutionMinutes; // Maximum execution time
        
        // Venue Selection
        private List<String> recommendedVenues; // Preferred execution venues
        private String executionStrategy; // TWAP, VWAP, IS, AGGRESSIVE
        private BigDecimal participationRate; // VWAP participation rate
        private Boolean enableDarkPool; // Use dark pool liquidity
    }
    
    /**
     * Signal Quality and Validation
     */
    private SignalQuality signalQuality;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignalQuality {
        private BigDecimal qualityScore; // Overall signal quality score
        private String qualityRating; // A, B, C, D, F
        private List<String> qualityFlags; // Quality concerns
        
        // Data Quality
        private BigDecimal dataQuality; // Input data quality score
        private Boolean hasDataGaps; // Missing data detected
        private String dataFreshness; // REAL_TIME, DELAYED, STALE
        private Integer dataLatencyMs; // Data latency milliseconds
        
        // Signal Consistency
        private BigDecimal consistencyScore; // Signal consistency across timeframes
        private Boolean crossTimeframeConfirmed; // Multi-timeframe confirmation
        private List<String> conflictingSignals; // Conflicting indicator signals
        private String signalAlignment; // ALIGNED, MIXED, CONFLICTED
        
        // Historical Performance
        private BigDecimal historicalAccuracy; // Historical signal accuracy
        private BigDecimal historicalSharpe; // Historical Sharpe ratio
        private Integer historicalSampleSize; // Historical sample size
        private String performanceTrend; // IMPROVING, STABLE, DECLINING
        
        // Signal Validation
        private List<String> validationChecks; // Completed validation checks
        private Boolean passedValidation; // All validations passed
        private List<String> validationFailures; // Failed validations
        private String validationMethod; // Validation methodology
        private Instant lastValidated; // Last validation time
    }
    
    /**
     * Signal Lifecycle and Status
     */
    private SignalStatus signalStatus;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignalStatus {
        private String currentStatus; // GENERATED, VALIDATED, PENDING, EXECUTED, EXPIRED, CANCELLED
        private String executionStatus; // NOT_EXECUTED, PARTIALLY_EXECUTED, FULLY_EXECUTED, FAILED
        private LocalDateTime statusLastUpdated; // Last status update time
        private String statusReason; // Reason for current status
        
        // Execution Tracking
        private String orderId; // Associated order ID
        private BigDecimal executedSize; // Size executed
        private BigDecimal executedPrice; // Average execution price
        private BigDecimal executionSlippage; // Execution slippage
        private Long executionLatencyMs; // Signal to execution latency
        
        // Performance Tracking
        private BigDecimal currentPnL; // Current P&L since signal
        private BigDecimal maxPnL; // Maximum P&L achieved
        private BigDecimal minPnL; // Minimum P&L (maximum adverse move)
        private Boolean isActive; // Signal currently active
        private LocalDateTime closedAt; // When position was closed
        private String closeReason; // Reason for closing
        
        // Signal Feedback
        private BigDecimal outcomeScore; // Signal outcome score
        private String outcomeCategory; // WIN, LOSS, BREAKEVEN, PENDING
        private Boolean contributesToLearning; // Include in model training
        private Map<String, Object> executionMetrics; // Detailed execution metrics
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if signal is still valid (not expired)
     */
    public boolean isValid() {
        return expiresAt == null || Instant.now().isBefore(expiresAt);
    }
    
    /**
     * Check if signal is high confidence
     */
    public boolean isHighConfidence() {
        return signalCore != null && 
               signalCore.getConfidence() != null &&
               signalCore.getConfidence().compareTo(new BigDecimal("0.8")) >= 0;
    }
    
    /**
     * Check if signal is bullish
     */
    public boolean isBullish() {
        return signalCore != null && 
               ("BUY".equals(signalCore.getSignalType()) || 
                "STRONG_BUY".equals(signalCore.getSignalType()) ||
                "OPEN_LONG".equals(signalCore.getAction()));
    }
    
    /**
     * Check if signal is bearish
     */
    public boolean isBearish() {
        return signalCore != null && 
               ("SELL".equals(signalCore.getSignalType()) || 
                "STRONG_SELL".equals(signalCore.getSignalType()) ||
                "OPEN_SHORT".equals(signalCore.getAction()));
    }
    
    /**
     * Get composite signal score
     * Uses Optional patterns to eliminate if-statements and ternary operators
     */
    public BigDecimal getCompositeScore() {
        return Optional.ofNullable(signalCore)
            .map(core -> {
                BigDecimal baseScore = Optional.ofNullable(core.getStrength())
                    .orElse(BigDecimal.ZERO);
                BigDecimal confidence = Optional.ofNullable(core.getConfidence())
                    .orElse(BigDecimal.ZERO);
                return baseScore.multiply(confidence);
            })
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Check if signal has strong technical backing
     */
    public boolean hasStrongTechnicalBacking() {
        return technicalAnalysis != null && 
               technicalAnalysis.getTechnicalScore() != null &&
               technicalAnalysis.getTechnicalScore().compareTo(new BigDecimal("0.75")) >= 0;
    }
    
    /**
     * Check if signal is ML-driven
     */
    public boolean isMLDriven() {
        return mlPredictions != null && 
               mlPredictions.getPredictionConfidence() != null &&
               mlPredictions.getPredictionConfidence().compareTo(new BigDecimal("0.6")) >= 0;
    }
    
    /**
     * Get signal age in minutes
     * Uses Optional patterns to eliminate if-statements
     */
    public Long getAgeInMinutes() {
        return Optional.ofNullable(generatedAt)
            .map(generated -> java.time.Duration.between(generated, Instant.now()).toMinutes())
            .orElse(0L);
    }
    
    /**
     * Get minutes until expiration
     * Uses Optional patterns to eliminate if-statements
     */
    public Long getMinutesUntilExpiration() {
        return Optional.ofNullable(expiresAt)
            .map(expires -> java.time.Duration.between(Instant.now(), expires).toMinutes())
            .orElse(Long.MAX_VALUE);
    }
    
    /**
     * Get signal summary for alerts
     * Uses Optional patterns to eliminate if-statements and ternary operators
     */
    public String getSignalSummary() {
        return Optional.ofNullable(signalCore)
            .map(core -> {
                StringBuilder summary = new StringBuilder();

                // Build base summary
                summary.append(core.getSignalType()).append(" ")
                      .append(symbol).append(" @ ");

                // Add current price using Optional chain
                String price = Optional.ofNullable(marketContext)
                    .flatMap(mc -> Optional.ofNullable(mc.getCurrentPrice()))
                    .map(BigDecimal::toString)
                    .orElse("N/A");
                summary.append(price);

                // Add strength using Optional
                Optional.ofNullable(core.getStrength())
                    .ifPresent(strength -> summary.append(" (Strength: ").append(strength).append(")"));

                // Add confidence using Optional
                Optional.ofNullable(core.getConfidence())
                    .ifPresent(confidence -> summary.append(" (Confidence: ").append(confidence).append(")"));

                return summary.toString();
            })
            .orElse("");
    }
    
    /**
     * Static factory methods
     */
    
    public static TradingSignal buySignal(String strategyId, String symbol, BigDecimal price, 
                                        BigDecimal strength, BigDecimal confidence) {
        return TradingSignal.builder()
            .signalId("BUY_" + System.currentTimeMillis())
            .strategyId(strategyId)
            .symbol(symbol)
            .generatedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)) // 1 hour expiration
            .signalCore(SignalCore.builder()
                .signalType("BUY")
                .action("OPEN_LONG")
                .strength(strength)
                .confidence(confidence)
                .priority("MEDIUM")
                .timeframe("1h")
                .signalSource("TECHNICAL")
                .build())
            .marketContext(MarketContext.builder()
                .currentPrice(price)
                .isMarketHours(true)
                .build())
            .signalStatus(SignalStatus.builder()
                .currentStatus("GENERATED")
                .executionStatus("NOT_EXECUTED")
                .statusLastUpdated(LocalDateTime.now())
                .isActive(true)
                .build())
            .build();
    }
    
    public static TradingSignal sellSignal(String strategyId, String symbol, BigDecimal price, 
                                         BigDecimal strength, BigDecimal confidence) {
        return TradingSignal.builder()
            .signalId("SELL_" + System.currentTimeMillis())
            .strategyId(strategyId)
            .symbol(symbol)
            .generatedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)) // 1 hour expiration
            .signalCore(SignalCore.builder()
                .signalType("SELL")
                .action("OPEN_SHORT")
                .strength(strength)
                .confidence(confidence)
                .priority("MEDIUM")
                .timeframe("1h")
                .signalSource("TECHNICAL")
                .build())
            .marketContext(MarketContext.builder()
                .currentPrice(price)
                .isMarketHours(true)
                .build())
            .signalStatus(SignalStatus.builder()
                .currentStatus("GENERATED")
                .executionStatus("NOT_EXECUTED")
                .statusLastUpdated(LocalDateTime.now())
                .isActive(true)
                .build())
            .build();
    }
}