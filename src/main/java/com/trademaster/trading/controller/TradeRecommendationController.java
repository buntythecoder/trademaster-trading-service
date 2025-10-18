package com.trademaster.trading.controller;

import com.trademaster.trading.agentos.agents.TradeRecommendationAgent;
import com.trademaster.trading.client.MarketDataServiceClient;
import com.trademaster.trading.dto.MarketAnalysis;
import com.trademaster.trading.dto.marketdata.MarketDataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Trade Recommendation REST API Controller
 *
 * Provides AI-powered trade recommendations using multi-agent analysis combining
 * technical indicators, market sentiment, and risk assessment.
 *
 * Key Features:
 * - Multi-agent AI analysis (Technical + Sentiment + Risk)
 * - Real market data integration from market-data-service
 * - Customizable analysis periods and portfolio values
 * - Comprehensive recommendation with entry/exit prices
 *
 * Performance Targets:
 * - AI recommendation generation: <5s
 * - Market data retrieval: <500ms
 * - Custom analysis: <3s
 *
 * AgentOS Capabilities:
 * - technical-analysis: RSI, MACD, trend analysis
 * - sentiment-analysis: Market sentiment and news analysis
 * - trade-recommendation: ML-powered trade recommendations
 *
 * Endpoints:
 * - GET /api/v1/recommendations/{symbol} - Get comprehensive trade recommendation
 * - POST /api/v1/recommendations/analyze - Analyze custom OHLCV data
 * - GET /api/v1/recommendations/status - Get recommendation service status
 *
 * @author TradeMaster Team
 * @version 2.0.0 (Java 24 + Real Market Data)
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(
    name = "AI Trade Recommendations",
    description = "AI-powered trade recommendations using multi-agent analysis with real market data"
)
@SecurityRequirement(name = "bearer-jwt")
public class TradeRecommendationController {

    private final TradeRecommendationAgent recommendationAgent;
    private final MarketDataServiceClient marketDataClient;

    /**
     * Get AI-powered trade recommendation for a symbol.
     *
     * Analyzes historical market data using multi-agent AI system to provide:
     * - Technical analysis (RSI, MACD, trend indicators)
     * - Sentiment analysis (market sentiment, news analysis)
     * - Risk assessment (Sharpe ratio, max drawdown)
     * - Trade action (BUY, SELL, HOLD)
     * - Entry price, target price, and stop-loss levels
     * - Recommended position size based on portfolio value
     *
     * @param symbol Stock symbol (e.g., "RELIANCE", "TCS")
     * @param portfolioValue Current portfolio value (optional, defaults to ₹100,000)
     * @param periods Number of historical periods to analyze (optional, defaults to 50)
     * @return Comprehensive trade recommendation with multi-agent analysis
     */
    @GetMapping("/{symbol}")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @Operation(
        summary = "Get AI trade recommendation",
        description = """
            Generates comprehensive trade recommendation using multi-agent AI analysis.

            The system analyzes real market data from market-data-service and combines:
            - Technical indicators (RSI, MACD, moving averages)
            - Market sentiment from news and social media
            - Risk metrics (volatility, Sharpe ratio, drawdown)

            Returns actionable recommendation with:
            - Trade action (BUY/SELL/HOLD)
            - Confidence level (0-100)
            - Entry and exit prices
            - Recommended position size
            - Supporting factors and risk warnings
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Recommendation generated successfully",
            content = @Content(schema = @Schema(implementation = RecommendationResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - valid JWT token required"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - failed to generate recommendation"
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Service unavailable - market-data-service unavailable"
        )
    })
    public CompletableFuture<ResponseEntity<RecommendationResponse>> getRecommendation(
            @PathVariable
            @Parameter(description = "Stock symbol (NSE)", example = "RELIANCE")
            String symbol,

            @RequestParam(required = false, defaultValue = "100000")
            @Parameter(description = "Portfolio value in INR for position sizing", example = "100000")
            BigDecimal portfolioValue,

            @RequestParam(required = false, defaultValue = "50")
            @Parameter(description = "Number of historical days to analyze", example = "50")
            Integer periods) {

        log.info("Trade recommendation requested: symbol={}, portfolioValue={}, periods={}",
                symbol, portfolioValue, periods);

        // Calculate time range for historical data
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(Duration.ofDays(periods));

        // Fetch real OHLCV data from market-data-service
        return marketDataClient.getHistoricalData(symbol, "NSE", startTime, endTime, "1d")
            .thenCompose(marketDataResponse -> {
                // Convert market data response to MarketAnalysis.OHLCVData format
                List<MarketAnalysis.OHLCVData> ohlcvData = marketDataResponse.data().stream()
                    .map(dataPoint -> new MarketAnalysis.OHLCVData(
                        symbol,
                        dataPoint.timestamp(),
                        dataPoint.open(),
                        dataPoint.high(),
                        dataPoint.low(),
                        dataPoint.close(),
                        dataPoint.volume(),
                        "1d"
                    ))
                    .collect(Collectors.toList());

                // Get current price from latest data point
                BigDecimal currentPrice = ohlcvData.isEmpty()
                    ? BigDecimal.ZERO
                    : ohlcvData.get(ohlcvData.size() - 1).close();

                log.info("Fetched {} OHLCV data points for symbol={}, currentPrice={}",
                        ohlcvData.size(), symbol, currentPrice);

                // Generate AI-powered recommendation with real market data
                return recommendationAgent.generateRecommendation(symbol, ohlcvData, portfolioValue, currentPrice);
            })
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
     * Allows users to provide their own OHLCV (Open, High, Low, Close, Volume) data
     * for analysis instead of fetching from market-data-service. Useful for:
     * - Backtesting with historical data
     * - Analyzing custom data sources
     * - Testing AI recommendations with specific scenarios
     *
     * @param request Analysis request with OHLCV data
     * @return Trade recommendation based on provided data
     */
    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @Operation(
        summary = "Analyze custom OHLCV data",
        description = """
            Generates trade recommendation from user-provided OHLCV data.

            This endpoint allows backtesting and custom data analysis without
            relying on market-data-service. Useful for:
            - Historical backtesting
            - Custom data source integration
            - What-if scenario analysis

            The AI system performs the same multi-agent analysis as the
            standard recommendation endpoint.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Analysis completed successfully",
            content = @Content(schema = @Schema(implementation = RecommendationResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing or malformed OHLCV data"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - valid JWT token required"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - analysis failed"
        )
    })
    public CompletableFuture<ResponseEntity<RecommendationResponse>> analyzeCustomData(
            @RequestBody
            @Validated
            @Parameter(description = "Custom OHLCV data for analysis")
            AnalysisRequest request) {

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
     * Returns health metrics and capabilities of the AI recommendation system:
     * - Overall health score (0.0 to 1.0)
     * - Available AI agent capabilities
     * - Service status (HEALTHY, DEGRADED, DOWN)
     * - Current timestamp
     *
     * @return Service status and agent health metrics
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @Operation(
        summary = "Get service health status",
        description = """
            Returns health metrics and capabilities of the AI recommendation service.

            Health Score Interpretation:
            - 1.0: Fully operational, all agents healthy
            - 0.7-0.9: Degraded performance, some agents slow
            - 0.5-0.7: Limited functionality, some agents unavailable
            - <0.5: Critical issues, service unreliable

            Use this endpoint for:
            - Health monitoring and alerting
            - Service discovery and capability checks
            - Debugging AI recommendation issues
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Status retrieved successfully",
            content = @Content(schema = @Schema(implementation = ServiceStatus.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - valid JWT token required"
        )
    })
    public ResponseEntity<ServiceStatus> getStatus() {
        return ResponseEntity.ok(new ServiceStatus(
            "HEALTHY",
            recommendationAgent.getHealthScore(),
            recommendationAgent.getCapabilities(),
            Instant.now()
        ));
    }

    // ========== DTOs ==========

    /**
     * Trade recommendation response DTO.
     *
     * Comprehensive AI-generated trade recommendation with:
     * - Trade action and confidence level
     * - Entry, target, and stop-loss prices
     * - Technical, sentiment, and risk analysis
     * - Supporting factors and risk warnings
     */
    @Schema(description = "AI-generated trade recommendation with multi-agent analysis")
    public record RecommendationResponse(
        @Schema(description = "Unique recommendation identifier", example = "REC-2024-001")
        String recommendationId,

        @Schema(description = "Stock symbol", example = "RELIANCE")
        String symbol,

        @Schema(description = "Recommendation generation timestamp")
        Instant timestamp,

        @Schema(description = "Recommended trade action", example = "BUY", allowableValues = {"BUY", "SELL", "HOLD"})
        String action,

        @Schema(description = "Recommended entry price in INR", example = "2450.50")
        BigDecimal entryPrice,

        @Schema(description = "Target profit price in INR", example = "2550.00")
        BigDecimal targetPrice,

        @Schema(description = "Stop-loss price in INR", example = "2400.00")
        BigDecimal stopLoss,

        @Schema(description = "Recommended quantity to trade", example = "10")
        Integer quantity,

        @Schema(description = "Overall recommendation score (0-100)", example = "85", minimum = "0", maximum = "100")
        Integer overallScore,

        @Schema(description = "AI confidence level (0-100)", example = "92", minimum = "0", maximum = "100")
        Integer confidenceLevel,

        @Schema(description = "Trading strategy", example = "Momentum")
        String strategy,

        @Schema(description = "Recommended timeframe", example = "INTRADAY")
        String timeframe,

        @Schema(description = "Primary reason for recommendation", example = "Strong bullish momentum with RSI confirmation")
        String primaryReason,

        @Schema(description = "Supporting factors for the recommendation")
        List<String> supportingFactors,

        @Schema(description = "Risk warnings and concerns")
        List<String> risks,

        @Schema(description = "Technical analysis summary")
        TechnicalSummary technical,

        @Schema(description = "Sentiment analysis summary")
        SentimentSummary sentiment,

        @Schema(description = "Risk assessment summary")
        RiskSummary risk,

        @Schema(description = "Recommendation expiration timestamp")
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
    @Schema(description = "Technical indicators and trend analysis")
    public record TechnicalSummary(
        @Schema(description = "Relative Strength Index (0-100)", example = "65.5", minimum = "0", maximum = "100")
        BigDecimal rsi,

        @Schema(description = "MACD indicator value", example = "12.3")
        BigDecimal macd,

        @Schema(description = "Market trend", example = "BULLISH", allowableValues = {"BULLISH", "BEARISH", "NEUTRAL"})
        String trend,

        @Schema(description = "Price momentum", example = "STRONG", allowableValues = {"STRONG", "MODERATE", "WEAK"})
        String momentum,

        @Schema(description = "Technical signal strength (0-100)", example = "80", minimum = "0", maximum = "100")
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
    @Schema(description = "Market sentiment and news analysis")
    public record SentimentSummary(
        @Schema(description = "Overall market sentiment", example = "POSITIVE", allowableValues = {"POSITIVE", "NEGATIVE", "NEUTRAL"})
        String overallSentiment,

        @Schema(description = "Buying pressure (0.0-1.0)", example = "0.75", minimum = "0", maximum = "1")
        BigDecimal buyPressure,

        @Schema(description = "Selling pressure (0.0-1.0)", example = "0.25", minimum = "0", maximum = "1")
        BigDecimal sellPressure,

        @Schema(description = "Market strength indicator (0.0-1.0)", example = "0.85", minimum = "0", maximum = "1")
        BigDecimal marketStrength,

        @Schema(description = "Sentiment confidence score (0-100)", example = "88", minimum = "0", maximum = "100")
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
    @Schema(description = "Risk metrics and position sizing")
    public record RiskSummary(
        @Schema(description = "Overall risk level", example = "MEDIUM", allowableValues = {"LOW", "MEDIUM", "HIGH"})
        String riskLevel,

        @Schema(description = "Risk score (0-100)", example = "45", minimum = "0", maximum = "100")
        Integer riskScore,

        @Schema(description = "Sharpe ratio (risk-adjusted returns)", example = "1.85")
        BigDecimal sharpeRatio,

        @Schema(description = "Maximum drawdown percentage", example = "12.5")
        BigDecimal maxDrawdown,

        @Schema(description = "Recommended position size as % of portfolio", example = "5.0")
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
    @Schema(description = "Request for custom OHLCV data analysis")
    public record AnalysisRequest(
        @Schema(description = "Stock symbol", example = "RELIANCE", required = true)
        String symbol,

        @Schema(description = "OHLCV data points for analysis (minimum 20 points recommended)", required = true)
        List<MarketAnalysis.OHLCVData> ohlcvData,

        @Schema(description = "Portfolio value in INR for position sizing", example = "100000", required = true)
        BigDecimal portfolioValue
    ) {}

    /**
     * Service status response.
     */
    @Schema(description = "AI recommendation service health status")
    public record ServiceStatus(
        @Schema(description = "Service status", example = "HEALTHY", allowableValues = {"HEALTHY", "DEGRADED", "DOWN"})
        String status,

        @Schema(description = "Health score (0.0-1.0)", example = "0.95", minimum = "0", maximum = "1")
        Double healthScore,

        @Schema(description = "Available AI agent capabilities", example = "[\"technical-analysis\", \"sentiment-analysis\", \"trade-recommendation\"]")
        List<String> capabilities,

        @Schema(description = "Status check timestamp")
        Instant timestamp
    ) {}
}
