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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Trade Recommendation Agent for AgentOS Framework
 *
 * Multi-agent orchestration system that combines technical analysis, sentiment analysis,
 * and risk assessment to generate comprehensive trade recommendations. Implements a
 * weighted ensemble approach with confidence scoring and dynamic strategy selection.
 *
 * Agent Capabilities:
 * - RECOMMENDATION_GENERATION: Generate BUY/SELL/HOLD recommendations
 * - MULTI_AGENT_COORDINATION: Orchestrate multiple analysis agents
 * - CONFIDENCE_SCORING: Calculate recommendation confidence levels
 * - STRATEGY_SELECTION: Identify optimal trading strategy
 * - RISK_REWARD_OPTIMIZATION: Balance risk and return objectives
 *
 * Ensemble Algorithms:
 * - Weighted Multi-Factor Scoring: 40% technical + 30% sentiment + 30% risk
 * - Dynamic Threshold Adjustment: Adaptive confidence requirements
 * - Signal Confluence: Align multi-agent signals for validation
 * - Kelly-Optimal Sizing: Risk-adjusted position recommendations
 * - Bayesian Confidence: Update beliefs based on signal strength
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TradeRecommendationAgent implements AgentOSComponent {

    private final TradingCapabilityRegistry capabilityRegistry;
    private final TechnicalAnalysisAgent technicalAgent;
    private final SentimentAnalysisAgent sentimentAgent;
    private final RiskAssessmentAgent riskAgent;

    // Multi-Agent Ensemble Weights
    private static final BigDecimal TECHNICAL_WEIGHT = new BigDecimal("0.40");  // 40%
    private static final BigDecimal SENTIMENT_WEIGHT = new BigDecimal("0.30");  // 30%
    private static final BigDecimal RISK_WEIGHT = new BigDecimal("0.30");      // 30%

    // Confidence Thresholds
    private static final int MIN_CONFIDENCE_STRONG = 75;  // Strong BUY/SELL
    private static final int MIN_CONFIDENCE_MODERATE = 60; // BUY/SELL
    private static final int MIN_CONFIDENCE_WEAK = 40;     // HOLD

    // Recommendation Expiration
    private static final long EXPIRATION_HOURS_INTRADAY = 4;   // 4 hours
    private static final long EXPIRATION_HOURS_SHORT_TERM = 24; // 1 day
    private static final long EXPIRATION_HOURS_LONG_TERM = 168; // 7 days

    /**
     * Generates comprehensive trade recommendation by orchestrating multiple AI agents.
     *
     * @param symbol Stock symbol
     * @param ohlcvData Historical OHLCV data
     * @param portfolioValue Current portfolio value
     * @param currentPrice Current market price
     * @return CompletableFuture with TradeRecommendation
     */
    @EventHandler(event = "TradeRecommendationRequest")
    @AgentCapability(
        name = "RECOMMENDATION_GENERATION",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<MarketAnalysis.TradeRecommendation> generateRecommendation(
            String symbol, List<MarketAnalysis.OHLCVData> ohlcvData,
            BigDecimal portfolioValue, BigDecimal currentPrice) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Generating trade recommendation: symbol={}, periods={}, price={}",
                    symbol, ohlcvData.size(), currentPrice);

            try {
                if (ohlcvData.size() < 20) {
                    throw new IllegalArgumentException("Insufficient data for recommendation (minimum 20 periods)");
                }

                // Step 1: Coordinate multi-agent analysis (parallel execution)
                CompletableFuture<MarketAnalysis.TechnicalIndicators> technicalFuture =
                    technicalAgent.calculateIndicators(symbol, ohlcvData);

                CompletableFuture<MarketAnalysis.SentimentAnalysis> sentimentFuture =
                    sentimentAgent.analyzeSentiment(symbol, ohlcvData);

                CompletableFuture<MarketAnalysis.RiskAssessment> riskFuture =
                    riskAgent.assessRisk(symbol, ohlcvData, portfolioValue);

                // Wait for all agents to complete
                MarketAnalysis.TechnicalIndicators technical = technicalFuture.join();
                MarketAnalysis.SentimentAnalysis sentiment = sentimentFuture.join();
                MarketAnalysis.RiskAssessment risk = riskFuture.join();

                // Step 2: Calculate weighted ensemble score
                Integer overallScore = calculateEnsembleScore(technical, sentiment, risk);

                // Step 3: Determine action (BUY/SELL/HOLD)
                MarketAnalysis.TradeRecommendation.Action action = determineAction(
                    technical, sentiment, risk, overallScore
                );

                // Step 4: Calculate confidence level
                Integer confidenceLevel = calculateConfidenceLevel(technical, sentiment, risk, action);

                // Step 5: Determine trading strategy
                String strategy = identifyStrategy(technical, sentiment, risk);
                String timeframe = determineTimeframe(strategy, technical);

                // Step 6: Calculate entry, target, and stop-loss prices
                BigDecimal entryPrice = calculateEntryPrice(currentPrice, action, technical);
                BigDecimal targetPrice = calculateTargetPrice(entryPrice, action, risk, technical);
                BigDecimal stopLoss = action == MarketAnalysis.TradeRecommendation.Action.BUY
                    ? risk.stopLossPrice()
                    : calculateSellStopLoss(entryPrice, risk);

                // Step 7: Calculate position quantity
                Integer quantity = calculateQuantity(portfolioValue, entryPrice, risk.recommendedPosition());

                // Step 8: Generate reasoning and risk factors
                String primaryReason = generatePrimaryReason(technical, sentiment, risk, action);
                List<String> supportingFactors = generateSupportingFactors(technical, sentiment, risk);
                List<String> risks = generateRiskFactors(risk, sentiment, technical);

                // Step 9: Calculate expiration time
                Instant expiresAt = calculateExpiration(timeframe);

                // Step 10: Create recommendation
                var recommendation = new MarketAnalysis.TradeRecommendation(
                    generateRecommendationId(symbol),
                    symbol,
                    Instant.now(),
                    action,
                    entryPrice,
                    targetPrice,
                    stopLoss,
                    quantity,
                    technical,
                    sentiment,
                    risk,
                    overallScore,
                    confidenceLevel,
                    strategy,
                    timeframe,
                    primaryReason,
                    supportingFactors,
                    risks,
                    getAgentId(),
                    expiresAt
                );

                capabilityRegistry.recordSuccessfulExecution("RECOMMENDATION_GENERATION");

                log.info("Recommendation generated: symbol={}, action={}, confidence={}, score={}, strategy={}",
                        symbol, action, confidenceLevel, overallScore, strategy);

                return recommendation;

            } catch (Exception e) {
                log.error("Failed to generate recommendation for symbol={}", symbol, e);
                capabilityRegistry.recordFailedExecution("RECOMMENDATION_GENERATION", e);
                throw new RuntimeException("Recommendation generation failed", e);
            }
        });
    }

    /**
     * Calculates weighted ensemble score combining all agent signals.
     * Score Formula: (Technical * 0.4) + (Sentiment * 0.3) + (Risk * 0.3)
     */
    private Integer calculateEnsembleScore(
            MarketAnalysis.TechnicalIndicators technical,
            MarketAnalysis.SentimentAnalysis sentiment,
            MarketAnalysis.RiskAssessment risk) {

        // Convert technical signal to score (0-100)
        int technicalScore = mapMomentumToScore(technical.momentum());
        int trendBonus = mapTrendToBonus(technical.trend());
        technicalScore = Math.min(100, technicalScore + trendBonus);

        // Convert sentiment to score (0-100)
        int sentimentScore = mapSentimentToScore(sentiment.overallSentiment());

        // Convert risk to score (0-100, inverted: lower risk = higher score)
        int riskScore = 100 - risk.riskScore();

        // Weighted ensemble
        BigDecimal weightedScore = BigDecimal.valueOf(technicalScore)
            .multiply(TECHNICAL_WEIGHT)
            .add(BigDecimal.valueOf(sentimentScore).multiply(SENTIMENT_WEIGHT))
            .add(BigDecimal.valueOf(riskScore).multiply(RISK_WEIGHT));

        return weightedScore.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * Maps momentum signal to numerical score (0-100).
     */
    private int mapMomentumToScore(MarketAnalysis.TechnicalIndicators.MomentumSignal momentum) {
        return switch (momentum) {
            case STRONG_BUY -> 95;
            case BUY -> 75;
            case NEUTRAL -> 50;
            case SELL -> 25;
            case STRONG_SELL -> 5;
        };
    }

    /**
     * Maps trend signal to bonus points (-10 to +10).
     */
    private int mapTrendToBonus(MarketAnalysis.TechnicalIndicators.TrendSignal trend) {
        return switch (trend) {
            case STRONG_BULLISH -> 10;
            case BULLISH -> 5;
            case NEUTRAL -> 0;
            case BEARISH -> -5;
            case STRONG_BEARISH -> -10;
        };
    }

    /**
     * Maps sentiment score to numerical score (0-100).
     */
    private int mapSentimentToScore(MarketAnalysis.SentimentAnalysis.SentimentScore sentiment) {
        return switch (sentiment) {
            case VERY_BULLISH -> 95;
            case BULLISH -> 75;
            case NEUTRAL -> 50;
            case BEARISH -> 25;
            case VERY_BEARISH -> 5;
        };
    }

    /**
     * Determines action based on ensemble score and multi-agent alignment.
     */
    private MarketAnalysis.TradeRecommendation.Action determineAction(
            MarketAnalysis.TechnicalIndicators technical,
            MarketAnalysis.SentimentAnalysis sentiment,
            MarketAnalysis.RiskAssessment risk,
            Integer overallScore) {

        // Check risk level first - don't recommend if risk is extreme
        if (risk.riskLevel() == MarketAnalysis.RiskAssessment.RiskLevel.EXTREME) {
            return MarketAnalysis.TradeRecommendation.Action.HOLD;
        }

        // Strong signals require alignment
        boolean bullishAlignment = isBullishAligned(technical, sentiment);
        boolean bearishAlignment = isBearishAligned(technical, sentiment);

        // Decision logic based on score and alignment
        if (overallScore >= 80 && bullishAlignment) {
            return MarketAnalysis.TradeRecommendation.Action.STRONG_BUY;
        }
        if (overallScore >= 65 && bullishAlignment) {
            return MarketAnalysis.TradeRecommendation.Action.BUY;
        }
        if (overallScore <= 20 && bearishAlignment) {
            return MarketAnalysis.TradeRecommendation.Action.STRONG_SELL;
        }
        if (overallScore <= 35 && bearishAlignment) {
            return MarketAnalysis.TradeRecommendation.Action.SELL;
        }

        return MarketAnalysis.TradeRecommendation.Action.HOLD;
    }

    /**
     * Checks if technical and sentiment signals are aligned bullish.
     */
    private boolean isBullishAligned(
            MarketAnalysis.TechnicalIndicators technical,
            MarketAnalysis.SentimentAnalysis sentiment) {

        boolean techBullish = technical.momentum() == MarketAnalysis.TechnicalIndicators.MomentumSignal.BUY ||
                              technical.momentum() == MarketAnalysis.TechnicalIndicators.MomentumSignal.STRONG_BUY;

        boolean sentimentBullish = sentiment.overallSentiment() == MarketAnalysis.SentimentAnalysis.SentimentScore.BULLISH ||
                                   sentiment.overallSentiment() == MarketAnalysis.SentimentAnalysis.SentimentScore.VERY_BULLISH;

        return techBullish && sentimentBullish;
    }

    /**
     * Checks if technical and sentiment signals are aligned bearish.
     */
    private boolean isBearishAligned(
            MarketAnalysis.TechnicalIndicators technical,
            MarketAnalysis.SentimentAnalysis sentiment) {

        boolean techBearish = technical.momentum() == MarketAnalysis.TechnicalIndicators.MomentumSignal.SELL ||
                              technical.momentum() == MarketAnalysis.TechnicalIndicators.MomentumSignal.STRONG_SELL;

        boolean sentimentBearish = sentiment.overallSentiment() == MarketAnalysis.SentimentAnalysis.SentimentScore.BEARISH ||
                                   sentiment.overallSentiment() == MarketAnalysis.SentimentAnalysis.SentimentScore.VERY_BEARISH;

        return techBearish && sentimentBearish;
    }

    /**
     * Calculates confidence level using Bayesian-inspired approach.
     */
    private Integer calculateConfidenceLevel(
            MarketAnalysis.TechnicalIndicators technical,
            MarketAnalysis.SentimentAnalysis sentiment,
            MarketAnalysis.RiskAssessment risk,
            MarketAnalysis.TradeRecommendation.Action action) {

        // Base confidence from signal strength
        int technicalConfidence = technical.signalStrength();
        int sentimentConfidence = sentiment.confidenceScore();

        // Risk penalty (higher risk = lower confidence)
        int riskPenalty = risk.riskScore() / 2;

        // Alignment bonus
        int alignmentBonus = isBullishAligned(technical, sentiment) ||
                             isBearishAligned(technical, sentiment) ? 10 : 0;

        // Action strength bonus
        int actionBonus = (action == MarketAnalysis.TradeRecommendation.Action.STRONG_BUY ||
                          action == MarketAnalysis.TradeRecommendation.Action.STRONG_SELL) ? 5 : 0;

        // Weighted confidence
        int confidence = (int) (technicalConfidence * 0.4 +
                               sentimentConfidence * 0.3 +
                               (100 - riskPenalty) * 0.3 +
                               alignmentBonus + actionBonus);

        return Math.max(0, Math.min(100, confidence));
    }

    /**
     * Identifies optimal trading strategy based on market conditions.
     */
    private String identifyStrategy(
            MarketAnalysis.TechnicalIndicators technical,
            MarketAnalysis.SentimentAnalysis sentiment,
            MarketAnalysis.RiskAssessment risk) {

        // Trend-following strategy
        if (technical.trend() == MarketAnalysis.TechnicalIndicators.TrendSignal.STRONG_BULLISH ||
            technical.trend() == MarketAnalysis.TechnicalIndicators.TrendSignal.STRONG_BEARISH) {
            return "TREND_FOLLOWING";
        }

        // Momentum strategy
        if (sentiment.buyPressure().compareTo(new BigDecimal("70")) > 0 ||
            sentiment.sellPressure().compareTo(new BigDecimal("70")) > 0) {
            return "MOMENTUM";
        }

        // Mean reversion strategy
        if (technical.rsi().compareTo(new BigDecimal("70")) > 0 ||
            technical.rsi().compareTo(new BigDecimal("30")) < 0) {
            return "MEAN_REVERSION";
        }

        // Breakout strategy
        BigDecimal currentPrice = technical.bollingerMiddle();
        if (technical.bollingerUpper().subtract(currentPrice).abs()
            .compareTo(new BigDecimal("2")) < 0) {
            return "BREAKOUT";
        }

        return "BALANCED";
    }

    /**
     * Determines timeframe based on strategy and technical signals.
     */
    private String determineTimeframe(String strategy, MarketAnalysis.TechnicalIndicators technical) {
        return switch (strategy) {
            case "MOMENTUM", "BREAKOUT" -> "SHORT_TERM";
            case "TREND_FOLLOWING" -> "MEDIUM_TERM";
            case "MEAN_REVERSION", "BALANCED" -> "LONG_TERM";
            default -> "MEDIUM_TERM";
        };
    }

    /**
     * Calculates optimal entry price with micro-adjustments.
     */
    private BigDecimal calculateEntryPrice(
            BigDecimal currentPrice,
            MarketAnalysis.TradeRecommendation.Action action,
            MarketAnalysis.TechnicalIndicators technical) {

        // For BUY: slightly below current for limit order
        if (action == MarketAnalysis.TradeRecommendation.Action.BUY ||
            action == MarketAnalysis.TradeRecommendation.Action.STRONG_BUY) {
            BigDecimal adjustment = currentPrice.multiply(new BigDecimal("0.002")); // 0.2% below
            return currentPrice.subtract(adjustment).setScale(2, RoundingMode.HALF_UP);
        }

        // For SELL: slightly above current for limit order
        if (action == MarketAnalysis.TradeRecommendation.Action.SELL ||
            action == MarketAnalysis.TradeRecommendation.Action.STRONG_SELL) {
            BigDecimal adjustment = currentPrice.multiply(new BigDecimal("0.002")); // 0.2% above
            return currentPrice.add(adjustment).setScale(2, RoundingMode.HALF_UP);
        }

        // HOLD: current price
        return currentPrice;
    }

    /**
     * Calculates target price using risk/reward optimization.
     */
    private BigDecimal calculateTargetPrice(
            BigDecimal entryPrice,
            MarketAnalysis.TradeRecommendation.Action action,
            MarketAnalysis.RiskAssessment risk,
            MarketAnalysis.TechnicalIndicators technical) {

        if (action == MarketAnalysis.TradeRecommendation.Action.HOLD) {
            return entryPrice;
        }

        // Calculate risk distance
        BigDecimal riskDistance = entryPrice.subtract(risk.stopLossPrice()).abs();

        // Risk/reward ratio based on strategy (1.5:1 to 3:1)
        BigDecimal riskRewardRatio = risk.expectedReturn()
            .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP)
            .max(new BigDecimal("1.5"))
            .min(new BigDecimal("3.0"));

        BigDecimal targetDistance = riskDistance.multiply(riskRewardRatio);

        // BUY: target above entry
        if (action == MarketAnalysis.TradeRecommendation.Action.BUY ||
            action == MarketAnalysis.TradeRecommendation.Action.STRONG_BUY) {
            return entryPrice.add(targetDistance).setScale(2, RoundingMode.HALF_UP);
        }

        // SELL: target below entry
        return entryPrice.subtract(targetDistance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates stop-loss for SELL positions.
     */
    private BigDecimal calculateSellStopLoss(BigDecimal entryPrice, MarketAnalysis.RiskAssessment risk) {
        BigDecimal riskDistance = entryPrice.multiply(new BigDecimal("0.02")); // 2% above
        return entryPrice.add(riskDistance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates position quantity using Kelly-optimal sizing.
     */
    private Integer calculateQuantity(
            BigDecimal portfolioValue,
            BigDecimal entryPrice,
            BigDecimal positionSize) {

        if (entryPrice.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        BigDecimal positionValue = portfolioValue.multiply(positionSize);
        BigDecimal quantity = positionValue.divide(entryPrice, 0, RoundingMode.DOWN);

        return quantity.max(BigDecimal.ONE).intValue();
    }

    /**
     * Generates primary reason for recommendation.
     */
    private String generatePrimaryReason(
            MarketAnalysis.TechnicalIndicators technical,
            MarketAnalysis.SentimentAnalysis sentiment,
            MarketAnalysis.RiskAssessment risk,
            MarketAnalysis.TradeRecommendation.Action action) {

        return switch (action) {
            case STRONG_BUY -> String.format(
                "Strong buy signal with RSI %.1f, positive sentiment (%.0f%% buy pressure), Sharpe ratio %.2f",
                technical.rsi(), sentiment.buyPressure(), risk.sharpeRatio()
            );
            case BUY -> String.format(
                "Buy opportunity with %s trend, %s sentiment, risk level %s",
                technical.trend(), sentiment.overallSentiment(), risk.riskLevel()
            );
            case STRONG_SELL -> String.format(
                "Strong sell signal with RSI %.1f, negative sentiment (%.0f%% sell pressure), max drawdown %.1f%%",
                technical.rsi(), sentiment.sellPressure(), risk.maxDrawdown()
            );
            case SELL -> String.format(
                "Sell recommendation with %s trend, %s sentiment, elevated risk %s",
                technical.trend(), sentiment.overallSentiment(), risk.riskLevel()
            );
            case HOLD -> String.format(
                "Hold position - mixed signals or high risk (%s)", risk.riskLevel()
            );
        };
    }

    /**
     * Generates supporting factors for recommendation.
     */
    private List<String> generateSupportingFactors(
            MarketAnalysis.TechnicalIndicators technical,
            MarketAnalysis.SentimentAnalysis sentiment,
            MarketAnalysis.RiskAssessment risk) {

        List<String> factors = new ArrayList<>();

        // Technical factors
        if (technical.signalStrength() > 70) {
            factors.add(String.format("Strong technical signal strength: %d/100", technical.signalStrength()));
        }
        if (technical.macd().compareTo(technical.macdSignal()) > 0) {
            factors.add("MACD bullish crossover signal");
        }

        // Sentiment factors
        if (sentiment.confidenceScore() > 70) {
            factors.add(String.format("High sentiment confidence: %d/100", sentiment.confidenceScore()));
        }
        if (sentiment.marketStrength().compareTo(new BigDecimal("70")) > 0) {
            factors.add(String.format("Strong market strength: %.0f/100", sentiment.marketStrength()));
        }

        // Risk factors
        if (risk.sharpeRatio().compareTo(BigDecimal.ONE) > 0) {
            factors.add(String.format("Favorable Sharpe ratio: %.2f", risk.sharpeRatio()));
        }
        if (risk.riskLevel() == MarketAnalysis.RiskAssessment.RiskLevel.LOW) {
            factors.add("Low risk profile with controlled volatility");
        }

        return factors.isEmpty() ? List.of("Standard market conditions") : factors;
    }

    /**
     * Generates risk factors for recommendation.
     */
    private List<String> generateRiskFactors(
            MarketAnalysis.RiskAssessment risk,
            MarketAnalysis.SentimentAnalysis sentiment,
            MarketAnalysis.TechnicalIndicators technical) {

        List<String> risks = new ArrayList<>();

        // Risk-based warnings
        if (risk.volatility().compareTo(new BigDecimal("30")) > 0) {
            risks.add(String.format("High volatility: %.1f%%", risk.volatility()));
        }
        if (risk.maxDrawdown().compareTo(new BigDecimal("20")) > 0) {
            risks.add(String.format("Large historical drawdown: %.1f%%", risk.maxDrawdown()));
        }
        if (risk.sharpeRatio().compareTo(BigDecimal.ZERO) < 0) {
            risks.add("Negative risk-adjusted returns");
        }

        // Sentiment warnings
        if (sentiment.volatilityScore().compareTo(new BigDecimal("70")) > 0) {
            risks.add("Elevated price volatility");
        }

        // Technical warnings
        if (technical.rsi().compareTo(new BigDecimal("70")) > 0) {
            risks.add("Overbought conditions (RSI > 70)");
        }
        if (technical.rsi().compareTo(new BigDecimal("30")) < 0) {
            risks.add("Oversold conditions (RSI < 30)");
        }

        return risks.isEmpty() ? List.of("Standard market risks") : risks;
    }

    /**
     * Calculates recommendation expiration time.
     */
    private Instant calculateExpiration(String timeframe) {
        long hours = switch (timeframe) {
            case "SHORT_TERM" -> EXPIRATION_HOURS_INTRADAY;
            case "MEDIUM_TERM" -> EXPIRATION_HOURS_SHORT_TERM;
            case "LONG_TERM" -> EXPIRATION_HOURS_LONG_TERM;
            default -> EXPIRATION_HOURS_SHORT_TERM;
        };

        return Instant.now().plusSeconds(hours * 3600);
    }

    /**
     * Generates unique recommendation ID.
     */
    private String generateRecommendationId(String symbol) {
        return String.format("REC-%s-%s", symbol, UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    // ========== AgentOSComponent Implementation ==========

    @Override
    public String getAgentId() {
        return "trade-recommendation-agent";
    }

    @Override
    public String getAgentType() {
        return "TRADE_RECOMMENDATION";
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
            "RECOMMENDATION_GENERATION",
            "MULTI_AGENT_COORDINATION",
            "CONFIDENCE_SCORING",
            "STRATEGY_SELECTION",
            "RISK_REWARD_OPTIMIZATION"
        );
    }

    @Override
    public Double getHealthScore() {
        return capabilityRegistry.calculateOverallHealthScore();
    }
}
