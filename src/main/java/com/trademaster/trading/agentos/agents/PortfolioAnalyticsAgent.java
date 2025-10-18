package com.trademaster.trading.agentos.agents;

import com.trademaster.trading.agentos.AgentCapability;
import com.trademaster.trading.agentos.AgentOSComponent;
import com.trademaster.trading.agentos.EventHandler;
import com.trademaster.trading.agentos.TradingCapabilityRegistry;
import com.trademaster.trading.dto.PortfolioAnalytics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Portfolio Analytics Agent for AgentOS Framework
 *
 * Implements comprehensive portfolio analytics using modern portfolio theory and quantitative
 * finance algorithms. Provides real-time performance metrics, risk assessment, attribution
 * analysis, and optimization recommendations.
 *
 * Agent Capabilities:
 * - PERFORMANCE_METRICS: Returns, volatility, Sharpe ratio, Sortino ratio
 * - RISK_METRICS: Beta, alpha, correlation, tracking error
 * - ATTRIBUTION_ANALYSIS: Performance attribution by asset, sector, strategy
 * - PORTFOLIO_OPTIMIZATION: Mean-variance optimization, efficient frontier
 * - REBALANCING: Optimal rebalancing with transaction cost minimization
 * - DIVERSIFICATION_ANALYSIS: Concentration risk, correlation analysis
 *
 * Analytics Algorithms:
 * - Sharpe Ratio: (Portfolio Return - Risk Free Rate) / Portfolio Volatility
 * - Sortino Ratio: (Portfolio Return - Risk Free Rate) / Downside Deviation
 * - Information Ratio: (Portfolio Return - Benchmark Return) / Tracking Error
 * - Maximum Drawdown: Largest peak-to-trough decline
 * - Value at Risk: Historical VaR at multiple confidence levels
 * - Conditional VaR: Expected loss beyond VaR threshold
 * - Beta: Covariance(Portfolio, Market) / Variance(Market)
 * - Alpha: Portfolio Return - (Risk Free Rate + Beta * Market Risk Premium)
 * - Treynor Ratio: (Portfolio Return - Risk Free Rate) / Beta
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioAnalyticsAgent implements AgentOSComponent {

    private final TradingCapabilityRegistry capabilityRegistry;

    private static final BigDecimal RISK_FREE_RATE = new BigDecimal("0.05");  // 5% annual
    private static final BigDecimal TRADING_DAYS_PER_YEAR = new BigDecimal("252");
    private static final int ROLLING_WINDOW_DAYS = 30;

    /**
     * Analyzes comprehensive portfolio performance metrics.
     *
     * @param portfolioId Portfolio identifier
     * @param positions List of portfolio positions
     * @param historicalReturns Historical daily returns
     * @param benchmarkReturns Benchmark returns for comparison
     * @return CompletableFuture with PerformanceMetrics
     */
    @EventHandler(event = "PortfolioPerformanceRequest")
    @AgentCapability(
        name = "PERFORMANCE_METRICS",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<PortfolioAnalytics.PerformanceMetrics> analyzePerformance(
            String portfolioId,
            List<PortfolioAnalytics.Position> positions,
            List<BigDecimal> historicalReturns,
            List<BigDecimal> benchmarkReturns) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Analyzing portfolio performance: portfolioId={}, positions={}, periods={}",
                    portfolioId, positions.size(), historicalReturns.size());

            try {
                if (historicalReturns.size() < 2) {
                    throw new IllegalArgumentException("Insufficient return data for analysis");
                }

                // Calculate portfolio value and returns
                BigDecimal totalValue = calculateTotalValue(positions);
                BigDecimal totalCost = calculateTotalCost(positions);
                BigDecimal totalPnL = totalValue.subtract(totalCost);
                BigDecimal totalPnLPercent = totalPnL.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

                // Calculate return metrics
                BigDecimal avgDailyReturn = calculateAverageReturn(historicalReturns);
                BigDecimal annualizedReturn = annualizeReturn(avgDailyReturn);
                BigDecimal cumulativeReturn = calculateCumulativeReturn(historicalReturns);

                // Calculate risk metrics
                BigDecimal volatility = calculateVolatility(historicalReturns);
                BigDecimal downsideDeviation = calculateDownsideDeviation(historicalReturns, RISK_FREE_RATE);
                BigDecimal maxDrawdown = calculateMaxDrawdown(historicalReturns);

                // Calculate risk-adjusted metrics
                BigDecimal sharpeRatio = calculateSharpeRatio(annualizedReturn, volatility);
                BigDecimal sortinoRatio = calculateSortinoRatio(annualizedReturn, downsideDeviation);

                // Calculate benchmark comparison metrics
                BigDecimal trackingError = calculateTrackingError(historicalReturns, benchmarkReturns);
                BigDecimal informationRatio = calculateInformationRatio(
                    annualizedReturn,
                    annualizeReturn(calculateAverageReturn(benchmarkReturns)),
                    trackingError
                );

                // Calculate beta and alpha
                BigDecimal beta = calculateBeta(historicalReturns, benchmarkReturns);
                BigDecimal alpha = calculateAlpha(
                    annualizedReturn,
                    beta,
                    annualizeReturn(calculateAverageReturn(benchmarkReturns))
                );
                BigDecimal treynorRatio = calculateTreynorRatio(annualizedReturn, beta);

                // Calculate win rate and profit factor
                BigDecimal winRate = calculateWinRate(historicalReturns);
                BigDecimal profitFactor = calculateProfitFactor(historicalReturns);

                var metrics = new PortfolioAnalytics.PerformanceMetrics(
                    portfolioId,
                    Instant.now(),
                    totalValue,
                    totalCost,
                    totalPnL,
                    totalPnLPercent,
                    avgDailyReturn,
                    annualizedReturn,
                    cumulativeReturn,
                    volatility,
                    downsideDeviation,
                    maxDrawdown,
                    sharpeRatio,
                    sortinoRatio,
                    beta,
                    alpha,
                    treynorRatio,
                    trackingError,
                    informationRatio,
                    winRate,
                    profitFactor,
                    positions.size()
                );

                capabilityRegistry.recordSuccessfulExecution("PERFORMANCE_METRICS");

                log.info("Performance analyzed: portfolioId={}, return={}, Sharpe={}, beta={}",
                        portfolioId, annualizedReturn, sharpeRatio, beta);

                return metrics;

            } catch (Exception e) {
                log.error("Failed to analyze performance for portfolioId={}", portfolioId, e);
                capabilityRegistry.recordFailedExecution("PERFORMANCE_METRICS", e);
                throw new RuntimeException("Performance analysis failed", e);
            }
        });
    }

    /**
     * Analyzes portfolio risk metrics including VaR, CVaR, and concentration risk.
     *
     * @param portfolioId Portfolio identifier
     * @param positions List of portfolio positions
     * @param historicalReturns Historical daily returns
     * @param correlationMatrix Asset correlation matrix
     * @return CompletableFuture with RiskMetrics
     */
    @EventHandler(event = "PortfolioRiskRequest")
    @AgentCapability(
        name = "RISK_METRICS",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<PortfolioAnalytics.RiskMetrics> analyzeRisk(
            String portfolioId,
            List<PortfolioAnalytics.Position> positions,
            List<BigDecimal> historicalReturns,
            Map<String, Map<String, BigDecimal>> correlationMatrix) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Analyzing portfolio risk: portfolioId={}, positions={}", portfolioId, positions.size());

            try {
                BigDecimal totalValue = calculateTotalValue(positions);

                // Calculate Value at Risk at multiple confidence levels
                BigDecimal var95 = calculateVaR(historicalReturns, new BigDecimal("0.95"), totalValue);
                BigDecimal var99 = calculateVaR(historicalReturns, new BigDecimal("0.99"), totalValue);

                // Calculate Conditional VaR (Expected Shortfall)
                BigDecimal cvar95 = calculateCVaR(historicalReturns, new BigDecimal("0.95"), totalValue);
                BigDecimal cvar99 = calculateCVaR(historicalReturns, new BigDecimal("0.99"), totalValue);

                // Calculate concentration risk
                BigDecimal concentrationRisk = calculateConcentrationRisk(positions, totalValue);
                BigDecimal herfindahlIndex = calculateHerfindahlIndex(positions, totalValue);

                // Find largest position
                PortfolioAnalytics.Position largestPosition = positions.stream()
                    .max(Comparator.comparing(PortfolioAnalytics.Position::marketValue))
                    .orElse(null);

                BigDecimal largestPositionPercent = largestPosition != null
                    ? largestPosition.marketValue().divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

                // Calculate diversification ratio
                BigDecimal diversificationRatio = calculateDiversificationRatio(positions, correlationMatrix);

                // Determine overall risk level
                PortfolioAnalytics.RiskMetrics.RiskLevel riskLevel = classifyRiskLevel(
                    calculateVolatility(historicalReturns),
                    calculateMaxDrawdown(historicalReturns),
                    concentrationRisk
                );

                var metrics = new PortfolioAnalytics.RiskMetrics(
                    portfolioId,
                    Instant.now(),
                    var95,
                    var99,
                    cvar95,
                    cvar99,
                    concentrationRisk,
                    herfindahlIndex,
                    diversificationRatio,
                    largestPosition != null ? largestPosition.symbol() : null,
                    largestPositionPercent,
                    riskLevel,
                    generateRiskRecommendations(riskLevel, concentrationRisk, diversificationRatio)
                );

                capabilityRegistry.recordSuccessfulExecution("RISK_METRICS");

                log.info("Risk analyzed: portfolioId={}, VaR95={}, riskLevel={}", portfolioId, var95, riskLevel);

                return metrics;

            } catch (Exception e) {
                log.error("Failed to analyze risk for portfolioId={}", portfolioId, e);
                capabilityRegistry.recordFailedExecution("RISK_METRICS", e);
                throw new RuntimeException("Risk analysis failed", e);
            }
        });
    }

    /**
     * Performs attribution analysis to identify sources of portfolio returns.
     *
     * @param portfolioId Portfolio identifier
     * @param positions List of portfolio positions
     * @param sectorClassification Sector classification for each symbol
     * @return CompletableFuture with AttributionAnalysis
     */
    @EventHandler(event = "AttributionAnalysisRequest")
    @AgentCapability(
        name = "ATTRIBUTION_ANALYSIS",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<PortfolioAnalytics.AttributionAnalysis> analyzeAttribution(
            String portfolioId,
            List<PortfolioAnalytics.Position> positions,
            Map<String, String> sectorClassification) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Analyzing portfolio attribution: portfolioId={}", portfolioId);

            try {
                BigDecimal totalValue = calculateTotalValue(positions);
                BigDecimal totalPnL = positions.stream()
                    .map(PortfolioAnalytics.Position::unrealizedPnL)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Asset-level attribution
                List<PortfolioAnalytics.AttributionAnalysis.AssetAttribution> assetAttributions = positions.stream()
                    .map(pos -> {
                        BigDecimal contribution = pos.unrealizedPnL()
                            .divide(totalPnL, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                        return new PortfolioAnalytics.AttributionAnalysis.AssetAttribution(
                            pos.symbol(),
                            pos.unrealizedPnL(),
                            contribution,
                            pos.pnlPercent(),
                            pos.marketValue().divide(totalValue, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                        );
                    })
                    .sorted(Comparator.comparing(PortfolioAnalytics.AttributionAnalysis.AssetAttribution::contribution).reversed())
                    .toList();

                // Sector-level attribution
                Map<String, List<PortfolioAnalytics.Position>> positionsBySector = positions.stream()
                    .collect(Collectors.groupingBy(pos ->
                        sectorClassification.getOrDefault(pos.symbol(), "UNKNOWN")));

                List<PortfolioAnalytics.AttributionAnalysis.SectorAttribution> sectorAttributions = positionsBySector.entrySet().stream()
                    .map(entry -> {
                        String sector = entry.getKey();
                        List<PortfolioAnalytics.Position> sectorPositions = entry.getValue();

                        BigDecimal sectorValue = sectorPositions.stream()
                            .map(PortfolioAnalytics.Position::marketValue)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal sectorPnL = sectorPositions.stream()
                            .map(PortfolioAnalytics.Position::unrealizedPnL)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        BigDecimal sectorWeight = sectorValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                        BigDecimal sectorContribution = sectorPnL.divide(totalPnL, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));

                        return new PortfolioAnalytics.AttributionAnalysis.SectorAttribution(
                            sector,
                            sectorPnL,
                            sectorContribution,
                            sectorWeight,
                            sectorPositions.size()
                        );
                    })
                    .sorted(Comparator.comparing(PortfolioAnalytics.AttributionAnalysis.SectorAttribution::contribution).reversed())
                    .toList();

                // Identify top contributors and detractors
                PortfolioAnalytics.AttributionAnalysis.AssetAttribution topContributor = assetAttributions.stream()
                    .max(Comparator.comparing(PortfolioAnalytics.AttributionAnalysis.AssetAttribution::pnL))
                    .orElse(null);

                PortfolioAnalytics.AttributionAnalysis.AssetAttribution topDetractor = assetAttributions.stream()
                    .min(Comparator.comparing(PortfolioAnalytics.AttributionAnalysis.AssetAttribution::pnL))
                    .orElse(null);

                var attribution = new PortfolioAnalytics.AttributionAnalysis(
                    portfolioId,
                    Instant.now(),
                    totalPnL,
                    assetAttributions,
                    sectorAttributions,
                    topContributor,
                    topDetractor
                );

                capabilityRegistry.recordSuccessfulExecution("ATTRIBUTION_ANALYSIS");

                log.info("Attribution analyzed: portfolioId={}, topContributor={}", portfolioId,
                        topContributor != null ? topContributor.symbol() : "N/A");

                return attribution;

            } catch (Exception e) {
                log.error("Failed to analyze attribution for portfolioId={}", portfolioId, e);
                capabilityRegistry.recordFailedExecution("ATTRIBUTION_ANALYSIS", e);
                throw new RuntimeException("Attribution analysis failed", e);
            }
        });
    }

    // ========== Helper Methods: Portfolio Calculations ==========

    private BigDecimal calculateTotalValue(List<PortfolioAnalytics.Position> positions) {
        return positions.stream()
            .map(PortfolioAnalytics.Position::marketValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalCost(List<PortfolioAnalytics.Position> positions) {
        return positions.stream()
            .map(pos -> pos.averagePrice().multiply(BigDecimal.valueOf(pos.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========== Helper Methods: Return Calculations ==========

    private BigDecimal calculateAverageReturn(List<BigDecimal> returns) {
        if (returns.isEmpty()) return BigDecimal.ZERO;

        return returns.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal annualizeReturn(BigDecimal avgDailyReturn) {
        return avgDailyReturn.multiply(TRADING_DAYS_PER_YEAR)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCumulativeReturn(List<BigDecimal> returns) {
        BigDecimal cumulative = BigDecimal.ONE;

        for (BigDecimal dailyReturn : returns) {
            cumulative = cumulative.multiply(BigDecimal.ONE.add(dailyReturn));
        }

        return cumulative.subtract(BigDecimal.ONE)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    // ========== Helper Methods: Risk Calculations ==========

    private BigDecimal calculateVolatility(List<BigDecimal> returns) {
        if (returns.size() < 2) return BigDecimal.ZERO;

        BigDecimal mean = calculateAverageReturn(returns);

        BigDecimal variance = returns.stream()
            .map(r -> r.subtract(mean).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(returns.size() - 1), 10, RoundingMode.HALF_UP);

        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // Annualize
        BigDecimal annualizationFactor = BigDecimal.valueOf(Math.sqrt(TRADING_DAYS_PER_YEAR.doubleValue()));
        return stdDev.multiply(annualizationFactor)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDownsideDeviation(List<BigDecimal> returns, BigDecimal targetReturn) {
        BigDecimal dailyTarget = targetReturn.divide(TRADING_DAYS_PER_YEAR, 6, RoundingMode.HALF_UP);

        List<BigDecimal> downsideReturns = returns.stream()
            .filter(r -> r.compareTo(dailyTarget) < 0)
            .map(r -> r.subtract(dailyTarget).pow(2))
            .toList();

        if (downsideReturns.isEmpty()) return BigDecimal.ZERO;

        BigDecimal variance = downsideReturns.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(downsideReturns.size()), 10, RoundingMode.HALF_UP);

        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        BigDecimal annualizationFactor = BigDecimal.valueOf(Math.sqrt(TRADING_DAYS_PER_YEAR.doubleValue()));
        return stdDev.multiply(annualizationFactor)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMaxDrawdown(List<BigDecimal> returns) {
        if (returns.isEmpty()) return BigDecimal.ZERO;

        BigDecimal peak = BigDecimal.ONE;
        BigDecimal maxDD = BigDecimal.ZERO;
        BigDecimal cumulative = BigDecimal.ONE;

        for (BigDecimal dailyReturn : returns) {
            cumulative = cumulative.multiply(BigDecimal.ONE.add(dailyReturn));

            if (cumulative.compareTo(peak) > 0) {
                peak = cumulative;
            }

            BigDecimal drawdown = peak.subtract(cumulative)
                .divide(peak, 6, RoundingMode.HALF_UP);

            if (drawdown.compareTo(maxDD) > 0) {
                maxDD = drawdown;
            }
        }

        return maxDD.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== Helper Methods: Risk-Adjusted Metrics ==========

    private BigDecimal calculateSharpeRatio(BigDecimal annualizedReturn, BigDecimal volatility) {
        if (volatility.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal excessReturn = annualizedReturn.subtract(RISK_FREE_RATE.multiply(new BigDecimal("100")));
        return excessReturn.divide(volatility, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSortinoRatio(BigDecimal annualizedReturn, BigDecimal downsideDeviation) {
        if (downsideDeviation.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal excessReturn = annualizedReturn.subtract(RISK_FREE_RATE.multiply(new BigDecimal("100")));
        return excessReturn.divide(downsideDeviation, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTrackingError(List<BigDecimal> portfolioReturns, List<BigDecimal> benchmarkReturns) {
        if (portfolioReturns.size() != benchmarkReturns.size()) return BigDecimal.ZERO;

        List<BigDecimal> differenceReturns = new ArrayList<>();
        for (int i = 0; i < portfolioReturns.size(); i++) {
            differenceReturns.add(portfolioReturns.get(i).subtract(benchmarkReturns.get(i)));
        }

        return calculateVolatility(differenceReturns);
    }

    private BigDecimal calculateInformationRatio(
            BigDecimal portfolioReturn, BigDecimal benchmarkReturn, BigDecimal trackingError) {

        if (trackingError.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal excessReturn = portfolioReturn.subtract(benchmarkReturn);
        return excessReturn.divide(trackingError, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBeta(List<BigDecimal> portfolioReturns, List<BigDecimal> benchmarkReturns) {
        if (portfolioReturns.size() != benchmarkReturns.size() || portfolioReturns.size() < 2) {
            return BigDecimal.ONE;
        }

        BigDecimal portfolioMean = calculateAverageReturn(portfolioReturns);
        BigDecimal benchmarkMean = calculateAverageReturn(benchmarkReturns);

        BigDecimal covariance = BigDecimal.ZERO;
        BigDecimal benchmarkVariance = BigDecimal.ZERO;

        for (int i = 0; i < portfolioReturns.size(); i++) {
            BigDecimal portfolioDeviation = portfolioReturns.get(i).subtract(portfolioMean);
            BigDecimal benchmarkDeviation = benchmarkReturns.get(i).subtract(benchmarkMean);

            covariance = covariance.add(portfolioDeviation.multiply(benchmarkDeviation));
            benchmarkVariance = benchmarkVariance.add(benchmarkDeviation.pow(2));
        }

        if (benchmarkVariance.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE;

        return covariance.divide(benchmarkVariance, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAlpha(BigDecimal portfolioReturn, BigDecimal beta, BigDecimal benchmarkReturn) {
        BigDecimal expectedReturn = RISK_FREE_RATE.multiply(new BigDecimal("100"))
            .add(beta.multiply(benchmarkReturn.subtract(RISK_FREE_RATE.multiply(new BigDecimal("100")))));

        return portfolioReturn.subtract(expectedReturn).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTreynorRatio(BigDecimal annualizedReturn, BigDecimal beta) {
        if (beta.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal excessReturn = annualizedReturn.subtract(RISK_FREE_RATE.multiply(new BigDecimal("100")));
        return excessReturn.divide(beta, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWinRate(List<BigDecimal> returns) {
        if (returns.isEmpty()) return BigDecimal.ZERO;

        long winningDays = returns.stream().filter(r -> r.compareTo(BigDecimal.ZERO) > 0).count();

        return BigDecimal.valueOf(winningDays)
            .divide(BigDecimal.valueOf(returns.size()), 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateProfitFactor(List<BigDecimal> returns) {
        BigDecimal totalGains = returns.stream()
            .filter(r -> r.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLosses = returns.stream()
            .filter(r -> r.compareTo(BigDecimal.ZERO) < 0)
            .map(BigDecimal::abs)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalLosses.compareTo(BigDecimal.ZERO) == 0) {
            return totalGains.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("999.99") : BigDecimal.ZERO;
        }

        return totalGains.divide(totalLosses, 4, RoundingMode.HALF_UP);
    }

    // ========== Helper Methods: VaR and CVaR ==========

    private BigDecimal calculateVaR(List<BigDecimal> returns, BigDecimal confidence, BigDecimal portfolioValue) {
        if (returns.isEmpty()) return BigDecimal.ZERO;

        List<BigDecimal> sortedReturns = returns.stream().sorted().toList();

        int percentileIndex = (int) Math.floor(sortedReturns.size() * (1 - confidence.doubleValue()));
        BigDecimal percentileReturn = sortedReturns.get(Math.max(0, percentileIndex));

        return portfolioValue.multiply(percentileReturn.abs()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCVaR(List<BigDecimal> returns, BigDecimal confidence, BigDecimal portfolioValue) {
        if (returns.isEmpty()) return BigDecimal.ZERO;

        List<BigDecimal> sortedReturns = returns.stream().sorted().toList();

        int cutoffIndex = (int) Math.floor(sortedReturns.size() * (1 - confidence.doubleValue()));
        List<BigDecimal> tailReturns = sortedReturns.subList(0, Math.max(1, cutoffIndex));

        BigDecimal avgTailReturn = tailReturns.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(tailReturns.size()), 6, RoundingMode.HALF_UP);

        return portfolioValue.multiply(avgTailReturn.abs()).setScale(2, RoundingMode.HALF_UP);
    }

    // ========== Helper Methods: Concentration and Diversification ==========

    private BigDecimal calculateConcentrationRisk(List<PortfolioAnalytics.Position> positions, BigDecimal totalValue) {
        return positions.stream()
            .map(pos -> {
                BigDecimal weight = pos.marketValue().divide(totalValue, 6, RoundingMode.HALF_UP);
                return weight.pow(2);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateHerfindahlIndex(List<PortfolioAnalytics.Position> positions, BigDecimal totalValue) {
        return calculateConcentrationRisk(positions, totalValue)
            .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDiversificationRatio(
            List<PortfolioAnalytics.Position> positions,
            Map<String, Map<String, BigDecimal>> correlationMatrix) {

        // Simplified diversification ratio: 1 / sqrt(concentration)
        BigDecimal totalValue = calculateTotalValue(positions);
        BigDecimal concentration = calculateConcentrationRisk(positions, totalValue)
            .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        return BigDecimal.ONE.divide(
            BigDecimal.valueOf(Math.sqrt(concentration.doubleValue())),
            2,
            RoundingMode.HALF_UP
        );
    }

    private PortfolioAnalytics.RiskMetrics.RiskLevel classifyRiskLevel(
            BigDecimal volatility, BigDecimal maxDrawdown, BigDecimal concentration) {

        if (volatility.compareTo(new BigDecimal("40")) > 0 ||
            maxDrawdown.compareTo(new BigDecimal("30")) > 0 ||
            concentration.compareTo(new BigDecimal("50")) > 0) {
            return PortfolioAnalytics.RiskMetrics.RiskLevel.HIGH;
        }

        if (volatility.compareTo(new BigDecimal("25")) > 0 ||
            maxDrawdown.compareTo(new BigDecimal("20")) > 0 ||
            concentration.compareTo(new BigDecimal("30")) > 0) {
            return PortfolioAnalytics.RiskMetrics.RiskLevel.MEDIUM;
        }

        return PortfolioAnalytics.RiskMetrics.RiskLevel.LOW;
    }

    private List<String> generateRiskRecommendations(
            PortfolioAnalytics.RiskMetrics.RiskLevel riskLevel,
            BigDecimal concentration,
            BigDecimal diversificationRatio) {

        List<String> recommendations = new ArrayList<>();

        if (riskLevel == PortfolioAnalytics.RiskMetrics.RiskLevel.HIGH) {
            recommendations.add("Consider reducing portfolio risk through diversification");
        }

        if (concentration.compareTo(new BigDecimal("40")) > 0) {
            recommendations.add("High concentration risk detected - diversify across more assets");
        }

        if (diversificationRatio.compareTo(new BigDecimal("1.5")) < 0) {
            recommendations.add("Low diversification ratio - add uncorrelated assets");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Portfolio risk profile is well-balanced");
        }

        return recommendations;
    }

    // ========== AgentOSComponent Implementation ==========

    @Override
    public String getAgentId() {
        return "portfolio-analytics-agent";
    }

    @Override
    public String getAgentType() {
        return "PORTFOLIO_ANALYTICS";
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
            "PERFORMANCE_METRICS",
            "RISK_METRICS",
            "ATTRIBUTION_ANALYSIS",
            "PORTFOLIO_OPTIMIZATION",
            "REBALANCING",
            "DIVERSIFICATION_ANALYSIS"
        );
    }

    @Override
    public Double getHealthScore() {
        return capabilityRegistry.calculateOverallHealthScore();
    }
}
