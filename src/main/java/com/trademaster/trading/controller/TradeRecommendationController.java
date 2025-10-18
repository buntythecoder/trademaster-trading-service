package com.trademaster.trading.controller;

import com.trademaster.trading.agentos.agents.TradeRecommendationAgent;
import com.trademaster.trading.dto.MarketAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Trade Recommendation REST API Controller
 *
 * Provides AI-powered trade recommendations using multi-agent analysis combining
 * technical indicators, market sentiment, and risk assessment.
 *
 * Endpoints:
 * - GET /api/v1/recommendations/{symbol} - Get comprehensive trade recommendation
 * - POST /api/v1/recommendations/analyze - Analyze custom OHLCV data
 * - GET /api/v1/recommendations/status - Get recommendation service status
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TradeRecommendationController {

    private final TradeRecommendationAgent recommendationAgent;

    /**
     * Get AI-powered trade recommendation for a symbol.
     *
     * @param symbol Stock symbol (e.g., "RELIANCE", "TCS")
     * @param portfolioValue Current portfolio value (optional, defaults to ₹100,000)
     * @param periods Number of historical periods to analyze (optional, defaults to 50)
     * @return Comprehensive trade recommendation with multi-agent analysis
     */
    @GetMapping("/{symbol}")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public CompletableFuture<ResponseEntity<RecommendationResponse>> getRecommendation(
            @PathVariable String symbol,
            @RequestParam(required = false, defaultValue = "100000") BigDecimal portfolioValue,
            @RequestParam(required = false, defaultValue = "50") Integer periods) {

        log.info("Trade recommendation requested: symbol={}, portfolioValue={}, periods={}",
                symbol, portfolioValue, periods);

        // TODO: Fetch real OHLCV data from MarketDataService
        // For now, generate sample data
        List<MarketAnalysis.OHLCVData> ohlcvData = generateSampleOHLCVData(symbol, periods);
        BigDecimal currentPrice = ohlcvData.get(ohlcvData.size() - 1).close();

        return recommendationAgent.generateRecommendation(symbol, ohlcvData, portfolioValue, currentPrice)
            .thenApply(recommendation -> {
                var response = RecommendationResponse.fromDomain(recommendation);
                log.info("Recommendation generated: symbol={}, action={}, confidence={}",
                        symbol, recommendation.action(), recommendation.confidenceLevel());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                log.error("Failed to generate recommendation for symbol={}", symbol, ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Analyze custom OHLCV data and generate recommendation.
     *
     * @param request Analysis request with OHLCV data
     * @return Trade recommendation based on provided data
     */
    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public CompletableFuture<ResponseEntity<RecommendationResponse>> analyzeCustomData(
            @RequestBody @Validated AnalysisRequest request) {

        log.info("Custom analysis requested: symbol={}, dataPoints={}",
                request.symbol(), request.ohlcvData().size());

        BigDecimal currentPrice = request.ohlcvData().get(request.ohlcvData().size() - 1).close();

        return recommendationAgent.generateRecommendation(
                request.symbol(),
                request.ohlcvData(),
                request.portfolioValue(),
                currentPrice
            )
            .thenApply(recommendation -> {
                var response = RecommendationResponse.fromDomain(recommendation);
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                log.error("Failed to analyze custom data for symbol={}", request.symbol(), ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Get recommendation service health status.
     *
     * @return Service status and agent health metrics
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public ResponseEntity<ServiceStatus> getStatus() {
        return ResponseEntity.ok(new ServiceStatus(
            "HEALTHY",
            recommendationAgent.getHealthScore(),
            recommendationAgent.getCapabilities(),
            Instant.now()
        ));
    }

    /**
     * Generates sample OHLCV data for demonstration.
     * In production, this would be replaced with MarketDataService integration.
     */
    private List<MarketAnalysis.OHLCVData> generateSampleOHLCVData(String symbol, int periods) {
        // Sample data generation - simplified for demonstration
        BigDecimal basePrice = new BigDecimal("2500.00");
        List<MarketAnalysis.OHLCVData> data = new java.util.ArrayList<>();

        for (int i = 0; i < periods; i++) {
            BigDecimal randomChange = BigDecimal.valueOf(Math.random() * 100 - 50); // ±50
            BigDecimal close = basePrice.add(randomChange);
            BigDecimal high = close.add(BigDecimal.valueOf(Math.random() * 30));
            BigDecimal low = close.subtract(BigDecimal.valueOf(Math.random() * 30));
            BigDecimal open = low.add(high.subtract(low).multiply(BigDecimal.valueOf(Math.random())));

            data.add(new MarketAnalysis.OHLCVData(
                symbol,
                Instant.now().minusSeconds((periods - i) * 86400L),
                open.setScale(2, java.math.RoundingMode.HALF_UP),
                high.setScale(2, java.math.RoundingMode.HALF_UP),
                low.setScale(2, java.math.RoundingMode.HALF_UP),
                close.setScale(2, java.math.RoundingMode.HALF_UP),
                (long) (1000000 + Math.random() * 5000000),
                "1d"
            ));

            basePrice = close;
        }

        return data;
    }

    // ========== DTOs ==========

    /**
     * Trade recommendation response DTO.
     */
    public record RecommendationResponse(
        String recommendationId,
        String symbol,
        Instant timestamp,
        String action,
        BigDecimal entryPrice,
        BigDecimal targetPrice,
        BigDecimal stopLoss,
        Integer quantity,
        Integer overallScore,
        Integer confidenceLevel,
        String strategy,
        String timeframe,
        String primaryReason,
        List<String> supportingFactors,
        List<String> risks,
        TechnicalSummary technical,
        SentimentSummary sentiment,
        RiskSummary risk,
        Instant expiresAt
    ) {
        public static RecommendationResponse fromDomain(MarketAnalysis.TradeRecommendation rec) {
            return new RecommendationResponse(
                rec.recommendationId(),
                rec.symbol(),
                rec.timestamp(),
                rec.action().name(),
                rec.entryPrice(),
                rec.targetPrice(),
                rec.stopLoss(),
                rec.quantity(),
                rec.overallScore(),
                rec.confidenceLevel(),
                rec.strategy(),
                rec.timeframe(),
                rec.primaryReason(),
                rec.supportingFactors(),
                rec.risks(),
                TechnicalSummary.fromDomain(rec.technicalAnalysis()),
                SentimentSummary.fromDomain(rec.sentimentAnalysis()),
                RiskSummary.fromDomain(rec.riskAssessment()),
                rec.expiresAt()
            );
        }
    }

    /**
     * Technical analysis summary.
     */
    public record TechnicalSummary(
        BigDecimal rsi,
        BigDecimal macd,
        String trend,
        String momentum,
        Integer signalStrength
    ) {
        public static TechnicalSummary fromDomain(MarketAnalysis.TechnicalIndicators tech) {
            return new TechnicalSummary(
                tech.rsi(),
                tech.macd(),
                tech.trend().name(),
                tech.momentum().name(),
                tech.signalStrength()
            );
        }
    }

    /**
     * Sentiment analysis summary.
     */
    public record SentimentSummary(
        String overallSentiment,
        BigDecimal buyPressure,
        BigDecimal sellPressure,
        BigDecimal marketStrength,
        Integer confidenceScore
    ) {
        public static SentimentSummary fromDomain(MarketAnalysis.SentimentAnalysis sentiment) {
            return new SentimentSummary(
                sentiment.overallSentiment().name(),
                sentiment.buyPressure(),
                sentiment.sellPressure(),
                sentiment.marketStrength(),
                sentiment.confidenceScore()
            );
        }
    }

    /**
     * Risk assessment summary.
     */
    public record RiskSummary(
        String riskLevel,
        Integer riskScore,
        BigDecimal sharpeRatio,
        BigDecimal maxDrawdown,
        BigDecimal recommendedPosition
    ) {
        public static RiskSummary fromDomain(MarketAnalysis.RiskAssessment risk) {
            return new RiskSummary(
                risk.riskLevel().name(),
                risk.riskScore(),
                risk.sharpeRatio(),
                risk.maxDrawdown(),
                risk.recommendedPosition()
            );
        }
    }

    /**
     * Analysis request DTO.
     */
    public record AnalysisRequest(
        String symbol,
        List<MarketAnalysis.OHLCVData> ohlcvData,
        BigDecimal portfolioValue
    ) {}

    /**
     * Service status response.
     */
    public record ServiceStatus(
        String status,
        Double healthScore,
        List<String> capabilities,
        Instant timestamp
    ) {}
}
