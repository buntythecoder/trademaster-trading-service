package com.trademaster.trading.controller;

import com.trademaster.trading.agentos.agents.PortfolioAnalyticsAgent;
import com.trademaster.trading.dto.PortfolioAnalytics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Portfolio Analytics REST API Controller
 *
 * Provides comprehensive portfolio analytics including performance metrics, risk assessment,
 * attribution analysis, and optimization recommendations using the PortfolioAnalyticsAgent.
 *
 * Endpoints:
 * - GET /api/v1/portfolio/{portfolioId}/performance - Performance metrics
 * - GET /api/v1/portfolio/{portfolioId}/risk - Risk metrics
 * - GET /api/v1/portfolio/{portfolioId}/attribution - Attribution analysis
 *
 * Real Financial Algorithms:
 * - Sharpe Ratio: (Return - RFR) / Volatility
 * - Sortino Ratio: (Return - RFR) / Downside Deviation
 * - Information Ratio: (Return - Benchmark) / Tracking Error
 * - Beta: Covariance(Portfolio, Market) / Variance(Market)
 * - Alpha: Return - (RFR + Beta × Market Risk Premium)
 * - VaR/CVaR: Value at Risk and Conditional Value at Risk
 * - Herfindahl Index: Sum of squared portfolio weights
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PortfolioAnalyticsController {

    private final PortfolioAnalyticsAgent portfolioAnalyticsAgent;

    /**
     * Get comprehensive performance metrics for a portfolio.
     *
     * Calculates 20+ performance metrics including:
     * - Return metrics (daily, annualized, cumulative)
     * - Risk metrics (volatility, max drawdown)
     * - Risk-adjusted metrics (Sharpe, Sortino, Treynor)
     * - Benchmark comparison (beta, alpha, tracking error, information ratio)
     * - Trading metrics (win rate, profit factor)
     *
     * @param portfolioId Portfolio identifier
     * @return CompletableFuture with PerformanceMetricsResponse
     */
    @GetMapping("/{portfolioId}/performance")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public CompletableFuture<ResponseEntity<PerformanceMetricsResponse>> getPerformanceMetrics(
            @PathVariable String portfolioId) {

        log.info("Performance metrics request: portfolioId={}", portfolioId);

        // Generate sample portfolio positions
        List<PortfolioAnalytics.Position> positions = generateSamplePositions(portfolioId);

        // Generate sample historical returns
        List<BigDecimal> historicalReturns = generateSampleReturns(50, new BigDecimal("0.001"), new BigDecimal("0.02"));
        List<BigDecimal> benchmarkReturns = generateSampleReturns(50, new BigDecimal("0.0008"), new BigDecimal("0.015"));

        return portfolioAnalyticsAgent.analyzePerformance(portfolioId, positions, historicalReturns, benchmarkReturns)
            .thenApply(metrics -> {
                var response = PerformanceMetricsResponse.fromDomain(metrics);
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                log.error("Failed to get performance metrics for portfolioId={}", portfolioId, ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Get comprehensive risk metrics for a portfolio.
     *
     * Calculates risk metrics including:
     * - Value at Risk (VaR) at 95% and 99% confidence
     * - Conditional VaR (CVaR / Expected Shortfall)
     * - Concentration risk and Herfindahl Index
     * - Diversification ratio
     * - Overall risk level classification
     *
     * @param portfolioId Portfolio identifier
     * @return CompletableFuture with RiskMetricsResponse
     */
    @GetMapping("/{portfolioId}/risk")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public CompletableFuture<ResponseEntity<RiskMetricsResponse>> getRiskMetrics(
            @PathVariable String portfolioId) {

        log.info("Risk metrics request: portfolioId={}", portfolioId);

        // Generate sample portfolio positions
        List<PortfolioAnalytics.Position> positions = generateSamplePositions(portfolioId);

        // Generate sample historical returns
        List<BigDecimal> historicalReturns = generateSampleReturns(50, new BigDecimal("0.001"), new BigDecimal("0.02"));

        // Generate sample correlation matrix
        Map<String, Map<String, BigDecimal>> correlationMatrix = generateSampleCorrelationMatrix(positions);

        return portfolioAnalyticsAgent.analyzeRisk(portfolioId, positions, historicalReturns, correlationMatrix)
            .thenApply(metrics -> {
                var response = RiskMetricsResponse.fromDomain(metrics);
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                log.error("Failed to get risk metrics for portfolioId={}", portfolioId, ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Get attribution analysis showing sources of portfolio returns.
     *
     * Provides:
     * - Asset-level attribution (P&L contribution by symbol)
     * - Sector-level attribution (P&L contribution by sector)
     * - Top contributors and detractors
     * - Contribution percentages and portfolio weights
     *
     * @param portfolioId Portfolio identifier
     * @return CompletableFuture with AttributionAnalysisResponse
     */
    @GetMapping("/{portfolioId}/attribution")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public CompletableFuture<ResponseEntity<AttributionAnalysisResponse>> getAttributionAnalysis(
            @PathVariable String portfolioId) {

        log.info("Attribution analysis request: portfolioId={}", portfolioId);

        // Generate sample portfolio positions
        List<PortfolioAnalytics.Position> positions = generateSamplePositions(portfolioId);

        // Generate sample sector classification
        Map<String, String> sectorClassification = Map.of(
            "RELIANCE", "ENERGY",
            "TCS", "TECHNOLOGY",
            "INFY", "TECHNOLOGY",
            "HDFC", "FINANCIALS",
            "ICICI", "FINANCIALS"
        );

        return portfolioAnalyticsAgent.analyzeAttribution(portfolioId, positions, sectorClassification)
            .thenApply(attribution -> {
                var response = AttributionAnalysisResponse.fromDomain(attribution);
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                log.error("Failed to get attribution analysis for portfolioId={}", portfolioId, ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    // ========== Helper Methods ==========

    /**
     * Generates sample portfolio positions for demonstration.
     */
    private List<PortfolioAnalytics.Position> generateSamplePositions(String portfolioId) {
        return List.of(
            new PortfolioAnalytics.Position(
                "RELIANCE",
                100,
                new BigDecimal("2400.00"),
                new BigDecimal("2550.00"),
                new BigDecimal("255000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("6.25"),
                Instant.now().minusSeconds(86400 * 60)
            ),
            new PortfolioAnalytics.Position(
                "TCS",
                50,
                new BigDecimal("3200.00"),
                new BigDecimal("3350.00"),
                new BigDecimal("167500.00"),
                new BigDecimal("7500.00"),
                new BigDecimal("4.69"),
                Instant.now().minusSeconds(86400 * 45)
            ),
            new PortfolioAnalytics.Position(
                "INFY",
                80,
                new BigDecimal("1500.00"),
                new BigDecimal("1580.00"),
                new BigDecimal("126400.00"),
                new BigDecimal("6400.00"),
                new BigDecimal("5.33"),
                Instant.now().minusSeconds(86400 * 30)
            ),
            new PortfolioAnalytics.Position(
                "HDFC",
                40,
                new BigDecimal("2800.00"),
                new BigDecimal("2750.00"),
                new BigDecimal("110000.00"),
                new BigDecimal("-2000.00"),
                new BigDecimal("-1.79"),
                Instant.now().minusSeconds(86400 * 20)
            ),
            new PortfolioAnalytics.Position(
                "ICICI",
                60,
                new BigDecimal("950.00"),
                new BigDecimal("1020.00"),
                new BigDecimal("61200.00"),
                new BigDecimal("4200.00"),
                new BigDecimal("7.37"),
                Instant.now().minusSeconds(86400 * 15)
            )
        );
    }

    /**
     * Generates sample daily returns using random walk with drift.
     */
    private List<BigDecimal> generateSampleReturns(int periods, BigDecimal drift, BigDecimal volatility) {
        return java.util.stream.IntStream.range(0, periods)
            .mapToObj(i -> {
                double random = Math.random() - 0.5;
                return drift.add(volatility.multiply(BigDecimal.valueOf(random)));
            })
            .collect(Collectors.toList());
    }

    /**
     * Generates sample correlation matrix for positions.
     */
    private Map<String, Map<String, BigDecimal>> generateSampleCorrelationMatrix(
            List<PortfolioAnalytics.Position> positions) {

        return positions.stream()
            .collect(Collectors.toMap(
                PortfolioAnalytics.Position::symbol,
                pos1 -> positions.stream()
                    .collect(Collectors.toMap(
                        PortfolioAnalytics.Position::symbol,
                        pos2 -> pos1.symbol().equals(pos2.symbol())
                            ? BigDecimal.ONE
                            : new BigDecimal("0.6")
                    ))
            ));
    }

    // ========== Response DTOs ==========

    /**
     * Performance metrics response DTO.
     */
    public record PerformanceMetricsResponse(
        String portfolioId,
        String timestamp,
        BigDecimal totalValue,
        BigDecimal totalCost,
        BigDecimal totalPnL,
        BigDecimal totalPnLPercent,
        BigDecimal avgDailyReturn,
        BigDecimal annualizedReturn,
        BigDecimal cumulativeReturn,
        BigDecimal volatility,
        BigDecimal downsideDeviation,
        BigDecimal maxDrawdown,
        BigDecimal sharpeRatio,
        BigDecimal sortinoRatio,
        BigDecimal beta,
        BigDecimal alpha,
        BigDecimal treynorRatio,
        BigDecimal trackingError,
        BigDecimal informationRatio,
        BigDecimal winRate,
        BigDecimal profitFactor,
        Integer positionCount
    ) {
        public static PerformanceMetricsResponse fromDomain(PortfolioAnalytics.PerformanceMetrics metrics) {
            return new PerformanceMetricsResponse(
                metrics.portfolioId(),
                metrics.timestamp().toString(),
                metrics.totalValue(),
                metrics.totalCost(),
                metrics.totalPnL(),
                metrics.totalPnLPercent(),
                metrics.avgDailyReturn(),
                metrics.annualizedReturn(),
                metrics.cumulativeReturn(),
                metrics.volatility(),
                metrics.downsideDeviation(),
                metrics.maxDrawdown(),
                metrics.sharpeRatio(),
                metrics.sortinoRatio(),
                metrics.beta(),
                metrics.alpha(),
                metrics.treynorRatio(),
                metrics.trackingError(),
                metrics.informationRatio(),
                metrics.winRate(),
                metrics.profitFactor(),
                metrics.positionCount()
            );
        }
    }

    /**
     * Risk metrics response DTO.
     */
    public record RiskMetricsResponse(
        String portfolioId,
        String timestamp,
        BigDecimal var95,
        BigDecimal var99,
        BigDecimal cvar95,
        BigDecimal cvar99,
        BigDecimal concentrationRisk,
        BigDecimal herfindahlIndex,
        BigDecimal diversificationRatio,
        String largestPosition,
        BigDecimal largestPositionPercent,
        String riskLevel,
        List<String> recommendations
    ) {
        public static RiskMetricsResponse fromDomain(PortfolioAnalytics.RiskMetrics metrics) {
            return new RiskMetricsResponse(
                metrics.portfolioId(),
                metrics.timestamp().toString(),
                metrics.var95(),
                metrics.var99(),
                metrics.cvar95(),
                metrics.cvar99(),
                metrics.concentrationRisk(),
                metrics.herfindahlIndex(),
                metrics.diversificationRatio(),
                metrics.largestPosition(),
                metrics.largestPositionPercent(),
                metrics.riskLevel().name(),
                metrics.recommendations()
            );
        }
    }

    /**
     * Attribution analysis response DTO.
     */
    public record AttributionAnalysisResponse(
        String portfolioId,
        String timestamp,
        BigDecimal totalPnL,
        List<AssetAttributionResponse> assetAttributions,
        List<SectorAttributionResponse> sectorAttributions,
        AssetAttributionResponse topContributor,
        AssetAttributionResponse topDetractor
    ) {
        public static AttributionAnalysisResponse fromDomain(PortfolioAnalytics.AttributionAnalysis attribution) {
            return new AttributionAnalysisResponse(
                attribution.portfolioId(),
                attribution.timestamp().toString(),
                attribution.totalPnL(),
                attribution.assetAttributions().stream()
                    .map(AssetAttributionResponse::fromDomain)
                    .collect(Collectors.toList()),
                attribution.sectorAttributions().stream()
                    .map(SectorAttributionResponse::fromDomain)
                    .collect(Collectors.toList()),
                attribution.topContributor() != null
                    ? AssetAttributionResponse.fromDomain(attribution.topContributor())
                    : null,
                attribution.topDetractor() != null
                    ? AssetAttributionResponse.fromDomain(attribution.topDetractor())
                    : null
            );
        }
    }

    /**
     * Asset attribution response DTO.
     */
    public record AssetAttributionResponse(
        String symbol,
        BigDecimal pnL,
        BigDecimal contribution,
        BigDecimal returnPercent,
        BigDecimal weight
    ) {
        public static AssetAttributionResponse fromDomain(
                PortfolioAnalytics.AttributionAnalysis.AssetAttribution attr) {
            return new AssetAttributionResponse(
                attr.symbol(),
                attr.pnL(),
                attr.contribution(),
                attr.returnPercent(),
                attr.weight()
            );
        }
    }

    /**
     * Sector attribution response DTO.
     */
    public record SectorAttributionResponse(
        String sector,
        BigDecimal pnL,
        BigDecimal contribution,
        BigDecimal weight,
        Integer assetCount
    ) {
        public static SectorAttributionResponse fromDomain(
                PortfolioAnalytics.AttributionAnalysis.SectorAttribution attr) {
            return new SectorAttributionResponse(
                attr.sector(),
                attr.pnL(),
                attr.contribution(),
                attr.weight(),
                attr.assetCount()
            );
        }
    }
}
