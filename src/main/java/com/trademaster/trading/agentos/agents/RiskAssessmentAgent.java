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
 * Risk Assessment Agent for AgentOS Framework
 *
 * Implements quantitative risk analysis using modern portfolio theory metrics including
 * Sharpe Ratio, Maximum Drawdown, Value at Risk (VaR), and Kelly Criterion for position sizing.
 *
 * Agent Capabilities:
 * - RISK_METRICS: Calculate Sharpe ratio, volatility, max drawdown
 * - POSITION_SIZING: Kelly Criterion and risk-based position sizing
 * - VALUE_AT_RISK: VaR calculation at multiple confidence levels
 * - RISK_CLASSIFICATION: Categorize risk levels
 * - STOP_LOSS_CALCULATION: Optimal stop-loss placement
 *
 * Risk Algorithms:
 * - Sharpe Ratio: (Return - Risk Free Rate) / Standard Deviation
 * - Maximum Drawdown: Largest peak-to-trough decline
 * - Value at Risk: Historical VaR at 95% confidence
 * - Kelly Criterion: f* = (bp - q) / b for position sizing
 * - ATR-based Stops: Dynamic stop-loss using volatility
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentAgent implements AgentOSComponent {

    private final TradingCapabilityRegistry capabilityRegistry;

    private static final BigDecimal RISK_FREE_RATE = new BigDecimal("0.05");  // 5% annual
    private static final BigDecimal TRADING_DAYS_PER_YEAR = new BigDecimal("252");
    private static final BigDecimal MAX_POSITION_SIZE = new BigDecimal("0.10");  // 10% max
    private static final BigDecimal VAR_CONFIDENCE = new BigDecimal("0.95");  // 95% confidence
    private static final int ATR_MULTIPLIER = 2;  // 2x ATR for stops

    /**
     * Performs comprehensive risk assessment for a trading opportunity.
     *
     * @param symbol Stock symbol
     * @param ohlcvData Historical OHLCV data
     * @param portfolioValue Current portfolio value
     * @return CompletableFuture with RiskAssessment
     */
    @EventHandler(event = "RiskAssessmentRequest")
    @AgentCapability(
        name = "RISK_METRICS",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<MarketAnalysis.RiskAssessment> assessRisk(
            String symbol, List<MarketAnalysis.OHLCVData> ohlcvData, BigDecimal portfolioValue) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Assessing risk for symbol={}, periods={}, portfolio={}",
                    symbol, ohlcvData.size(), portfolioValue);

            try {
                if (ohlcvData.size() < 2) {
                    throw new IllegalArgumentException("Insufficient data for risk assessment");
                }

                // Extract price data
                List<BigDecimal> closePrices = ohlcvData.stream()
                    .map(MarketAnalysis.OHLCVData::close)
                    .toList();
                BigDecimal currentPrice = closePrices.get(closePrices.size() - 1);

                // Calculate returns
                List<BigDecimal> returns = calculateReturns(closePrices);

                // Calculate risk metrics
                BigDecimal expectedReturn = calculateExpectedReturn(returns);
                BigDecimal volatility = calculateVolatility(returns);
                BigDecimal sharpeRatio = calculateSharpeRatio(expectedReturn, volatility);
                BigDecimal maxDrawdown = calculateMaxDrawdown(closePrices);
                BigDecimal valueAtRisk = calculateVaR(returns, portfolioValue, currentPrice);

                // Calculate position sizing
                BigDecimal recommendedPosition = calculatePositionSize(
                    expectedReturn, volatility, maxDrawdown, portfolioValue
                );

                // Calculate stop-loss and take-profit levels
                BigDecimal atr = calculateATR(ohlcvData);
                BigDecimal stopLossPrice = calculateStopLoss(currentPrice, atr);
                BigDecimal takeProfitPrice = calculateTakeProfit(currentPrice, atr, expectedReturn);

                // Determine risk classification
                var riskLevel = classifyRiskLevel(volatility, maxDrawdown, sharpeRatio);
                Integer riskScore = calculateRiskScore(volatility, maxDrawdown, sharpeRatio);
                String riskFactors = identifyRiskFactors(volatility, maxDrawdown, sharpeRatio, valueAtRisk);

                var assessment = new MarketAnalysis.RiskAssessment(
                    symbol,
                    Instant.now(),
                    expectedReturn,
                    volatility,
                    sharpeRatio,
                    maxDrawdown,
                    valueAtRisk,
                    recommendedPosition,
                    stopLossPrice,
                    takeProfitPrice,
                    riskLevel,
                    riskScore,
                    riskFactors
                );

                capabilityRegistry.recordSuccessfulExecution("RISK_METRICS");

                log.info("Risk assessed: symbol={}, Sharpe={}, volatility={}, maxDD={}, riskLevel={}",
                        symbol, sharpeRatio, volatility, maxDrawdown, riskLevel);

                return assessment;

            } catch (Exception e) {
                log.error("Failed to assess risk for symbol={}", symbol, e);
                capabilityRegistry.recordFailedExecution("RISK_METRICS", e);
                throw new RuntimeException("Risk assessment failed", e);
            }
        });
    }

    /**
     * Calculates daily returns from price series.
     */
    private List<BigDecimal> calculateReturns(List<BigDecimal> prices) {
        return java.util.stream.IntStream.range(1, prices.size())
            .mapToObj(i -> {
                BigDecimal currentPrice = prices.get(i);
                BigDecimal previousPrice = prices.get(i - 1);

                if (previousPrice.compareTo(BigDecimal.ZERO) == 0) {
                    return BigDecimal.ZERO;
                }

                return currentPrice.subtract(previousPrice)
                    .divide(previousPrice, 6, RoundingMode.HALF_UP);
            })
            .toList();
    }

    /**
     * Calculates expected return (annualized).
     * Expected Return = Average Daily Return * Trading Days
     */
    private BigDecimal calculateExpectedReturn(List<BigDecimal> returns) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal avgDailyReturn = returns.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

        // Annualize
        return avgDailyReturn.multiply(TRADING_DAYS_PER_YEAR)
            .multiply(new BigDecimal("100"))  // Convert to percentage
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates annualized volatility (standard deviation of returns).
     * Volatility = StdDev(Daily Returns) * sqrt(252)
     */
    private BigDecimal calculateVolatility(List<BigDecimal> returns) {
        if (returns.size() < 2) {
            return BigDecimal.ZERO;
        }

        // Calculate mean
        BigDecimal mean = returns.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        // Calculate variance
        BigDecimal variance = returns.stream()
            .map(r -> r.subtract(mean).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size() - 1), 10, RoundingMode.HALF_UP);

        // Calculate standard deviation
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // Annualize: StdDev * sqrt(252)
        BigDecimal annualizationFactor = BigDecimal.valueOf(Math.sqrt(TRADING_DAYS_PER_YEAR.doubleValue()));
        return stdDev.multiply(annualizationFactor)
            .multiply(new BigDecimal("100"))  // Convert to percentage
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Sharpe Ratio (risk-adjusted return).
     * Sharpe = (Return - Risk Free Rate) / Volatility
     */
    private BigDecimal calculateSharpeRatio(BigDecimal expectedReturn, BigDecimal volatility) {
        if (volatility.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal excessReturn = expectedReturn.subtract(
            RISK_FREE_RATE.multiply(new BigDecimal("100"))
        );

        return excessReturn.divide(volatility, 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Maximum Drawdown (largest peak-to-trough decline).
     * Max DD = (Trough Value - Peak Value) / Peak Value
     */
    private BigDecimal calculateMaxDrawdown(List<BigDecimal> prices) {
        if (prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = prices.get(0);

        for (BigDecimal price : prices) {
            // Update peak if new high
            if (price.compareTo(peak) > 0) {
                peak = price;
            }

            // Calculate drawdown from peak
            BigDecimal drawdown = peak.subtract(price)
                .divide(peak, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

            // Update max drawdown if larger
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Value at Risk (VaR) using historical method.
     * VaR = Portfolio Value * (Current Price / Portfolio Value) * (5th percentile return)
     */
    private BigDecimal calculateVaR(List<BigDecimal> returns, BigDecimal portfolioValue, BigDecimal currentPrice) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Sort returns to find percentile
        List<BigDecimal> sortedReturns = returns.stream()
            .sorted()
            .toList();

        // Get 5th percentile (95% confidence VaR)
        int percentileIndex = (int) Math.floor(sortedReturns.size() * (1 - VAR_CONFIDENCE.doubleValue()));
        BigDecimal percentileReturn = sortedReturns.get(Math.max(0, percentileIndex));

        // Calculate potential loss
        BigDecimal potentialLoss = currentPrice.multiply(percentileReturn.abs());

        return potentialLoss.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates recommended position size using Kelly Criterion and risk limits.
     * Kelly: f* = (bp - q) / b
     * Where: b = odds, p = win probability, q = loss probability
     *
     * Simplified: Position = min(Kelly * 0.5, Max Position, Risk-based limit)
     */
    private BigDecimal calculatePositionSize(
            BigDecimal expectedReturn, BigDecimal volatility,
            BigDecimal maxDrawdown, BigDecimal portfolioValue) {

        // Estimate win probability from expected return and volatility
        BigDecimal winProb = estimateWinProbability(expectedReturn, volatility);
        BigDecimal lossProb = BigDecimal.ONE.subtract(winProb);

        // Estimate odds (reward/risk ratio)
        BigDecimal odds = calculateOdds(expectedReturn, maxDrawdown);

        // Kelly Criterion
        BigDecimal kelly = BigDecimal.ZERO;
        if (odds.compareTo(BigDecimal.ZERO) > 0) {
            kelly = winProb.multiply(odds).subtract(lossProb)
                .divide(odds, 4, RoundingMode.HALF_UP);
        }

        // Use half-Kelly for safety (conservative)
        BigDecimal halfKelly = kelly.multiply(new BigDecimal("0.5"));

        // Apply risk-based adjustment
        BigDecimal riskAdjustment = calculateRiskAdjustment(volatility, maxDrawdown);
        BigDecimal adjustedPosition = halfKelly.multiply(riskAdjustment);

        // Cap at maximum position size
        adjustedPosition = adjustedPosition.min(MAX_POSITION_SIZE);

        // Ensure positive and reasonable
        return adjustedPosition.max(BigDecimal.ZERO)
            .min(MAX_POSITION_SIZE)
            .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Estimates win probability from expected return and volatility.
     */
    private BigDecimal estimateWinProbability(BigDecimal expectedReturn, BigDecimal volatility) {
        if (volatility.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("0.5");
        }

        // Z-score approximation
        BigDecimal zScore = expectedReturn.divide(volatility, 4, RoundingMode.HALF_UP);

        // Convert z-score to probability (simplified normal CDF approximation)
        BigDecimal prob = new BigDecimal("0.5")
            .add(zScore.multiply(new BigDecimal("0.2")));

        // Clamp between 0.3 and 0.7
        return prob.max(new BigDecimal("0.3"))
            .min(new BigDecimal("0.7"));
    }

    /**
     * Calculates odds (reward/risk ratio).
     */
    private BigDecimal calculateOdds(BigDecimal expectedReturn, BigDecimal maxDrawdown) {
        if (maxDrawdown.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE;
        }

        BigDecimal odds = expectedReturn.divide(maxDrawdown, 4, RoundingMode.HALF_UP);
        return odds.max(new BigDecimal("0.5")).min(new BigDecimal("3.0"));  // Reasonable bounds
    }

    /**
     * Calculates risk adjustment factor (0-1) based on volatility and drawdown.
     */
    private BigDecimal calculateRiskAdjustment(BigDecimal volatility, BigDecimal maxDrawdown) {
        // Lower adjustment for higher risk
        BigDecimal volAdjustment = BigDecimal.ONE
            .subtract(volatility.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
            .max(new BigDecimal("0.3"));

        BigDecimal ddAdjustment = BigDecimal.ONE
            .subtract(maxDrawdown.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
            .max(new BigDecimal("0.3"));

        // Average of adjustments
        return volAdjustment.add(ddAdjustment)
            .divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
    }

    /**
     * Calculates ATR (Average True Range) for dynamic stops.
     */
    private BigDecimal calculateATR(List<MarketAnalysis.OHLCVData> ohlcvData) {
        int periods = Math.min(14, ohlcvData.size() - 1);
        if (periods < 1) {
            return BigDecimal.ZERO;
        }

        // Calculate true ranges
        List<BigDecimal> trueRanges = java.util.stream.IntStream.range(1, Math.min(periods + 1, ohlcvData.size()))
            .mapToObj(i -> {
                MarketAnalysis.OHLCVData current = ohlcvData.get(i);
                MarketAnalysis.OHLCVData previous = ohlcvData.get(i - 1);

                BigDecimal tr1 = current.high().subtract(current.low());
                BigDecimal tr2 = current.high().subtract(previous.close()).abs();
                BigDecimal tr3 = current.low().subtract(previous.close()).abs();

                return tr1.max(tr2).max(tr3);
            })
            .toList();

        // Calculate average
        return trueRanges.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(trueRanges.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates stop-loss price using ATR.
     * Stop Loss = Current Price - (ATR * Multiplier)
     */
    private BigDecimal calculateStopLoss(BigDecimal currentPrice, BigDecimal atr) {
        BigDecimal stopDistance = atr.multiply(BigDecimal.valueOf(ATR_MULTIPLIER));
        return currentPrice.subtract(stopDistance)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates take-profit price using risk/reward ratio.
     * Take Profit = Current Price + (Stop Distance * Risk/Reward Ratio)
     */
    private BigDecimal calculateTakeProfit(BigDecimal currentPrice, BigDecimal atr, BigDecimal expectedReturn) {
        BigDecimal stopDistance = atr.multiply(BigDecimal.valueOf(ATR_MULTIPLIER));

        // Risk/reward ratio based on expected return (1.5:1 to 3:1)
        BigDecimal riskRewardRatio = expectedReturn.divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP)
            .max(new BigDecimal("1.5"))
            .min(new BigDecimal("3.0"));

        BigDecimal targetDistance = stopDistance.multiply(riskRewardRatio);

        return currentPrice.add(targetDistance)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Classifies risk level based on metrics.
     */
    private MarketAnalysis.RiskAssessment.RiskLevel classifyRiskLevel(
            BigDecimal volatility, BigDecimal maxDrawdown, BigDecimal sharpeRatio) {

        // High risk: high volatility, large drawdown, negative Sharpe
        if (volatility.compareTo(new BigDecimal("40")) > 0 ||
            maxDrawdown.compareTo(new BigDecimal("30")) > 0 ||
            sharpeRatio.compareTo(BigDecimal.ZERO) < 0) {
            return MarketAnalysis.RiskAssessment.RiskLevel.EXTREME;
        }

        // Moderate-high risk
        if (volatility.compareTo(new BigDecimal("25")) > 0 ||
            maxDrawdown.compareTo(new BigDecimal("20")) > 0 ||
            sharpeRatio.compareTo(new BigDecimal("0.5")) < 0) {
            return MarketAnalysis.RiskAssessment.RiskLevel.HIGH;
        }

        // Moderate risk
        if (volatility.compareTo(new BigDecimal("15")) > 0 ||
            maxDrawdown.compareTo(new BigDecimal("10")) > 0 ||
            sharpeRatio.compareTo(new BigDecimal("1.0")) < 0) {
            return MarketAnalysis.RiskAssessment.RiskLevel.MEDIUM;
        }

        return MarketAnalysis.RiskAssessment.RiskLevel.LOW;
    }

    /**
     * Calculates numerical risk score (0-100, higher = riskier).
     */
    private Integer calculateRiskScore(
            BigDecimal volatility, BigDecimal maxDrawdown, BigDecimal sharpeRatio) {

        int score = 0;

        // Volatility contribution (0-40 points)
        score += volatility.min(new BigDecimal("40")).intValue();

        // Max drawdown contribution (0-30 points)
        score += maxDrawdown.min(new BigDecimal("30")).intValue();

        // Sharpe ratio contribution (-10 to +30 points, inverted)
        int sharpePoints = 15 - sharpeRatio.multiply(new BigDecimal("10")).intValue();
        score += Math.max(-10, Math.min(30, sharpePoints));

        return Math.max(0, Math.min(100, score));
    }

    /**
     * Identifies key risk factors.
     */
    private String identifyRiskFactors(
            BigDecimal volatility, BigDecimal maxDrawdown,
            BigDecimal sharpeRatio, BigDecimal valueAtRisk) {

        StringBuilder factors = new StringBuilder();

        if (volatility.compareTo(new BigDecimal("30")) > 0) {
            factors.append("High volatility (").append(volatility).append("%); ");
        }
        if (maxDrawdown.compareTo(new BigDecimal("20")) > 0) {
            factors.append("Large drawdown risk (").append(maxDrawdown).append("%); ");
        }
        if (sharpeRatio.compareTo(BigDecimal.ZERO) < 0) {
            factors.append("Negative risk-adjusted returns (Sharpe: ").append(sharpeRatio).append("); ");
        }
        if (valueAtRisk.compareTo(new BigDecimal("100")) > 0) {
            factors.append("High VaR (₹").append(valueAtRisk).append("); ");
        }

        return factors.length() > 0 ? factors.toString().trim() : "Moderate risk profile";
    }

    // ========== AgentOSComponent Implementation ==========

    @Override
    public String getAgentId() {
        return "risk-assessment-agent";
    }

    @Override
    public String getAgentType() {
        return "RISK_ASSESSMENT";
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
            "RISK_METRICS",
            "POSITION_SIZING",
            "VALUE_AT_RISK",
            "RISK_CLASSIFICATION",
            "STOP_LOSS_CALCULATION"
        );
    }

    @Override
    public Double getHealthScore() {
        return capabilityRegistry.calculateOverallHealthScore();
    }
}
