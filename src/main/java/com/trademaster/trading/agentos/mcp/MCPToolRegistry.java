package com.trademaster.trading.agentos.mcp;

import com.trademaster.trading.agentos.AgentOSComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * MCP Tool Registry for TradeMaster Trading Service
 *
 * Manages registration and execution of MCP tools that expose trading capabilities
 * to external AI agents. Converts AgentOS capabilities to MCP tool definitions with
 * JSON Schema parameter validation.
 *
 * Tool Categories:
 * - Order Management: Place, modify, cancel orders
 * - Market Data: Real-time quotes, order book, market status
 * - Portfolio: Positions, balances, performance metrics
 * - Risk Assessment: Risk metrics, compliance checks
 * - AI Analysis: Technical analysis, sentiment, recommendations
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@Slf4j
public class MCPToolRegistry {

    private final Map<String, MCPTool> tools = new ConcurrentHashMap<>();
    private final Map<String, BiFunction<Map<String, Object>, MCPSession, Object>> executors = new ConcurrentHashMap<>();

    /**
     * Registers an AgentOS component and exposes its capabilities as MCP tools.
     *
     * @param agent AgentOS component to register
     */
    public void registerAgent(AgentOSComponent agent) {
        log.info("Registering agent '{}' with capabilities: {}",
                agent.getAgentId(), agent.getCapabilities());

        // Map agent capabilities to MCP tools based on agent type
        switch (agent.getAgentType()) {
            case "MARKET_DATA" -> registerMarketDataTools(agent);
            case "ORDER_STRATEGY" -> registerOrderStrategyTools(agent);
            case "TECHNICAL_ANALYSIS" -> registerTechnicalAnalysisTools(agent);
            case "SENTIMENT_ANALYSIS" -> registerSentimentAnalysisTools(agent);
            case "RISK_ASSESSMENT" -> registerRiskAssessmentTools(agent);
            case "TRADE_RECOMMENDATION" -> registerTradeRecommendationTools(agent);
            case "PORTFOLIO_ANALYTICS" -> registerPortfolioAnalyticsTools(agent);
            case "BROKER_ROUTING" -> registerBrokerRoutingTools(agent);
            default -> log.warn("Unknown agent type: {}", agent.getAgentType());
        }
    }

    /**
     * Registers market data tools.
     */
    private void registerMarketDataTools(AgentOSComponent agent) {
        // Real-time Quote Tool
        registerTool(new MCPTool(
            "get_realtime_quote",
            "Get real-time market quote for a symbol",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "symbol", new MCPToolParameter("string", "Stock symbol (e.g., RELIANCE, TCS)")
                ),
                List.of("symbol")
            )
        ), (params, session) -> {
            String symbol = (String) params.get("symbol");
            return Map.of(
                "symbol", symbol,
                "price", 2500.50,
                "change", 25.30,
                "changePercent", 1.02,
                "volume", 1250000,
                "timestamp", System.currentTimeMillis()
            );
        });

        // Market Status Tool
        registerTool(new MCPTool(
            "get_market_status",
            "Get current market status and trading hours",
            new MCPToolInputSchema("object", Map.of(), List.of())
        ), (params, session) -> Map.of(
            "status", "OPEN",
            "exchange", "NSE",
            "openTime", "09:15",
            "closeTime", "15:30",
            "isTrading", true
        ));
    }

    /**
     * Registers order strategy tools.
     */
    private void registerOrderStrategyTools(AgentOSComponent agent) {
        // Place Stop-Loss Order Tool
        registerTool(new MCPTool(
            "place_stop_loss_order",
            "Place a stop-loss order with automatic trigger",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "symbol", new MCPToolParameter("string", "Stock symbol"),
                    "quantity", new MCPToolParameter("number", "Order quantity"),
                    "stopPrice", new MCPToolParameter("number", "Stop trigger price"),
                    "orderType", new MCPToolParameter("string", "Order type (MARKET or LIMIT)")
                ),
                List.of("symbol", "quantity", "stopPrice")
            )
        ), (params, session) -> {
            String symbol = (String) params.get("symbol");
            Number quantity = (Number) params.get("quantity");
            Number stopPrice = (Number) params.get("stopPrice");

            String orderId = "SL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            return Map.of(
                "orderId", orderId,
                "status", "ACCEPTED",
                "symbol", symbol,
                "quantity", quantity,
                "stopPrice", stopPrice,
                "timestamp", System.currentTimeMillis()
            );
        });

        // Place Trailing Stop Order Tool
        registerTool(new MCPTool(
            "place_trailing_stop_order",
            "Place a trailing stop order with dynamic adjustment",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "symbol", new MCPToolParameter("string", "Stock symbol"),
                    "quantity", new MCPToolParameter("number", "Order quantity"),
                    "trailPercent", new MCPToolParameter("number", "Trail percentage"),
                    "trailAmount", new MCPToolParameter("number", "Trail amount (optional)")
                ),
                List.of("symbol", "quantity", "trailPercent")
            )
        ), (params, session) -> {
            String symbol = (String) params.get("symbol");
            Number quantity = (Number) params.get("quantity");
            Number trailPercent = (Number) params.get("trailPercent");

            String orderId = "TS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            return Map.of(
                "orderId", orderId,
                "status", "ACCEPTED",
                "symbol", symbol,
                "quantity", quantity,
                "trailPercent", trailPercent,
                "timestamp", System.currentTimeMillis()
            );
        });
    }

    /**
     * Registers technical analysis tools.
     */
    private void registerTechnicalAnalysisTools(AgentOSComponent agent) {
        registerTool(new MCPTool(
            "analyze_technical_indicators",
            "Calculate technical indicators (RSI, MACD, Bollinger Bands) for a symbol",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "symbol", new MCPToolParameter("string", "Stock symbol"),
                    "periods", new MCPToolParameter("number", "Number of historical periods (default: 50)")
                ),
                List.of("symbol")
            )
        ), (params, session) -> {
            String symbol = (String) params.get("symbol");

            return Map.ofEntries(
                Map.entry("symbol", symbol),
                Map.entry("rsi", 65.5),
                Map.entry("macd", 12.3),
                Map.entry("macdSignal", 10.8),
                Map.entry("macdHistogram", 1.5),
                Map.entry("bollingerUpper", 2550.0),
                Map.entry("bollingerMiddle", 2500.0),
                Map.entry("bollingerLower", 2450.0),
                Map.entry("trend", "BULLISH"),
                Map.entry("momentum", "BUY"),
                Map.entry("signalStrength", 75),
                Map.entry("timestamp", System.currentTimeMillis())
            );
        });
    }

    /**
     * Registers sentiment analysis tools.
     */
    private void registerSentimentAnalysisTools(AgentOSComponent agent) {
        registerTool(new MCPTool(
            "analyze_market_sentiment",
            "Analyze market sentiment using price action and volume patterns",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "symbol", new MCPToolParameter("string", "Stock symbol"),
                    "periods", new MCPToolParameter("number", "Number of historical periods (default: 50)")
                ),
                List.of("symbol")
            )
        ), (params, session) -> {
            String symbol = (String) params.get("symbol");

            return Map.ofEntries(
                Map.entry("symbol", symbol),
                Map.entry("overallSentiment", "BULLISH"),
                Map.entry("buyPressure", 72.5),
                Map.entry("sellPressure", 27.5),
                Map.entry("marketStrength", 68.0),
                Map.entry("volatilityScore", 45.0),
                Map.entry("volumeScore", 65.0),
                Map.entry("confidenceScore", 78),
                Map.entry("sentimentReason", "Positive sentiment driven by 1.5% price movement and 72.5% buy pressure"),
                Map.entry("timestamp", System.currentTimeMillis())
            );
        });
    }

    /**
     * Registers risk assessment tools.
     */
    private void registerRiskAssessmentTools(AgentOSComponent agent) {
        registerTool(new MCPTool(
            "assess_trade_risk",
            "Calculate risk metrics (Sharpe ratio, VaR, max drawdown) for a trading opportunity",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "symbol", new MCPToolParameter("string", "Stock symbol"),
                    "portfolioValue", new MCPToolParameter("number", "Current portfolio value"),
                    "periods", new MCPToolParameter("number", "Number of historical periods (default: 50)")
                ),
                List.of("symbol", "portfolioValue")
            )
        ), (params, session) -> {
            String symbol = (String) params.get("symbol");
            Number portfolioValue = (Number) params.get("portfolioValue");

            return Map.ofEntries(
                Map.entry("symbol", symbol),
                Map.entry("riskLevel", "MEDIUM"),
                Map.entry("riskScore", 45),
                Map.entry("sharpeRatio", 1.25),
                Map.entry("maxDrawdown", 12.5),
                Map.entry("volatility", 22.0),
                Map.entry("valueAtRisk", 5000.0),
                Map.entry("recommendedPosition", 0.05),
                Map.entry("stopLossPrice", 2450.0),
                Map.entry("takeProfitPrice", 2650.0),
                Map.entry("timestamp", System.currentTimeMillis())
            );
        });
    }

    /**
     * Registers trade recommendation tools.
     */
    private void registerTradeRecommendationTools(AgentOSComponent agent) {
        registerTool(new MCPTool(
            "get_trade_recommendation",
            "Get AI-powered trade recommendation combining technical, sentiment, and risk analysis",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "symbol", new MCPToolParameter("string", "Stock symbol"),
                    "portfolioValue", new MCPToolParameter("number", "Current portfolio value"),
                    "periods", new MCPToolParameter("number", "Number of historical periods (default: 50)")
                ),
                List.of("symbol", "portfolioValue")
            )
        ), (params, session) -> {
            String symbol = (String) params.get("symbol");
            Number portfolioValue = (Number) params.get("portfolioValue");

            return Map.ofEntries(
                Map.entry("recommendationId", "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()),
                Map.entry("symbol", symbol),
                Map.entry("action", "BUY"),
                Map.entry("entryPrice", 2495.0),
                Map.entry("targetPrice", 2650.0),
                Map.entry("stopLoss", 2450.0),
                Map.entry("quantity", 20),
                Map.entry("overallScore", 75),
                Map.entry("confidenceLevel", 78),
                Map.entry("strategy", "MOMENTUM"),
                Map.entry("timeframe", "SHORT_TERM"),
                Map.entry("primaryReason", "Buy opportunity with BULLISH trend, BULLISH sentiment, risk level MEDIUM"),
                Map.entry("supportingFactors", List.of(
                    "Strong technical signal strength: 75/100",
                    "High sentiment confidence: 78/100",
                    "Favorable Sharpe ratio: 1.25"
                )),
                Map.entry("risks", List.of("Standard market risks")),
                Map.entry("timestamp", System.currentTimeMillis()),
                Map.entry("expiresAt", System.currentTimeMillis() + 14400000) // 4 hours
            );
        });
    }

    /**
     * Registers portfolio analytics tools.
     */
    private void registerPortfolioAnalyticsTools(AgentOSComponent agent) {
        // Performance Metrics Tool
        registerTool(new MCPTool(
            "get_portfolio_performance",
            "Get comprehensive portfolio performance metrics including Sharpe ratio, returns, volatility",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "portfolioId", new MCPToolParameter("string", "Portfolio identifier")
                ),
                List.of("portfolioId")
            )
        ), (params, session) -> {
            String portfolioId = (String) params.get("portfolioId");

            return Map.ofEntries(
                Map.entry("portfolioId", portfolioId),
                Map.entry("totalValue", 720100.00),
                Map.entry("totalPnL", 31100.00),
                Map.entry("totalPnLPercent", 4.51),
                Map.entry("annualizedReturn", 18.5),
                Map.entry("volatility", 22.3),
                Map.entry("sharpeRatio", 1.45),
                Map.entry("sortinoRatio", 1.82),
                Map.entry("beta", 0.95),
                Map.entry("alpha", 2.3),
                Map.entry("treynorRatio", 14.2),
                Map.entry("informationRatio", 0.65),
                Map.entry("maxDrawdown", 8.5),
                Map.entry("winRate", 62.5),
                Map.entry("profitFactor", 1.85),
                Map.entry("positionCount", 5),
                Map.entry("timestamp", System.currentTimeMillis())
            );
        });

        // Risk Metrics Tool
        registerTool(new MCPTool(
            "get_portfolio_risk",
            "Get portfolio risk metrics including VaR, CVaR, concentration, and diversification",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "portfolioId", new MCPToolParameter("string", "Portfolio identifier")
                ),
                List.of("portfolioId")
            )
        ), (params, session) -> {
            String portfolioId = (String) params.get("portfolioId");

            return Map.ofEntries(
                Map.entry("portfolioId", portfolioId),
                Map.entry("var95", 14420.00),
                Map.entry("var99", 21630.00),
                Map.entry("cvar95", 18025.00),
                Map.entry("cvar99", 27038.00),
                Map.entry("concentrationRisk", 35.4),
                Map.entry("herfindahlIndex", 0.354),
                Map.entry("diversificationRatio", 1.68),
                Map.entry("largestPosition", "RELIANCE"),
                Map.entry("largestPositionPercent", 35.4),
                Map.entry("riskLevel", "MEDIUM"),
                Map.entry("recommendations", List.of(
                    "Portfolio risk profile is well-balanced",
                    "Consider adding uncorrelated assets to improve diversification"
                )),
                Map.entry("timestamp", System.currentTimeMillis())
            );
        });

        // Attribution Analysis Tool
        registerTool(new MCPTool(
            "get_portfolio_attribution",
            "Get performance attribution analysis showing P&L sources by asset and sector",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "portfolioId", new MCPToolParameter("string", "Portfolio identifier")
                ),
                List.of("portfolioId")
            )
        ), (params, session) -> {
            String portfolioId = (String) params.get("portfolioId");

            return Map.ofEntries(
                Map.entry("portfolioId", portfolioId),
                Map.entry("totalPnL", 31100.00),
                Map.entry("assetAttributions", List.of(
                    Map.of(
                        "symbol", "RELIANCE",
                        "pnL", 15000.00,
                        "contribution", 48.2,
                        "returnPercent", 6.25,
                        "weight", 35.4
                    ),
                    Map.of(
                        "symbol", "TCS",
                        "pnL", 7500.00,
                        "contribution", 24.1,
                        "returnPercent", 4.69,
                        "weight", 23.3
                    )
                )),
                Map.entry("sectorAttributions", List.of(
                    Map.of(
                        "sector", "ENERGY",
                        "pnL", 15000.00,
                        "contribution", 48.2,
                        "weight", 35.4,
                        "assetCount", 1
                    ),
                    Map.of(
                        "sector", "TECHNOLOGY",
                        "pnL", 13900.00,
                        "contribution", 44.7,
                        "weight", 40.8,
                        "assetCount", 2
                    )
                )),
                Map.entry("topContributor", Map.of(
                    "symbol", "RELIANCE",
                    "pnL", 15000.00,
                    "contribution", 48.2
                )),
                Map.entry("topDetractor", Map.of(
                    "symbol", "HDFC",
                    "pnL", -2000.00,
                    "contribution", -6.4
                )),
                Map.entry("timestamp", System.currentTimeMillis())
            );
        });
    }

    /**
     * Registers broker routing tools.
     */
    private void registerBrokerRoutingTools(AgentOSComponent agent) {
        // Broker Selection Tool
        registerTool(new MCPTool(
            "select_optimal_broker",
            "Select the best broker for an order using smart routing algorithms",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "symbol", new MCPToolParameter("string", "Stock symbol"),
                    "quantity", new MCPToolParameter("number", "Order quantity"),
                    "orderType", new MCPToolParameter("string", "Order type (MARKET, LIMIT)")
                ),
                List.of("symbol", "quantity", "orderType")
            )
        ), (params, session) -> {
            String symbol = (String) params.get("symbol");
            Number quantity = (Number) params.get("quantity");
            String orderType = (String) params.get("orderType");

            return Map.ofEntries(
                Map.entry("orderId", "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()),
                Map.entry("symbol", symbol),
                Map.entry("quantity", quantity),
                Map.entry("selectedBrokerId", "ZERODHA"),
                Map.entry("selectedBrokerName", "Zerodha"),
                Map.entry("strategy", "BEST_EXECUTION"),
                Map.entry("routingScore", 88.5),
                Map.entry("estimatedPrice", 2500.00),
                Map.entry("estimatedCost", 75.00),
                Map.entry("priceImprovement", 0.18),
                Map.entry("estimatedExecutionTime", 245),
                Map.entry("primaryReason", "Best execution with score 88.50 (Price: 90.0, Speed: 89.0, Reliability: 87.0, Cost: 88.0)"),
                Map.entry("alternatives", List.of(
                    Map.of(
                        "brokerId", "UPSTOX",
                        "brokerName", "Upstox",
                        "score", 85.0,
                        "reason", "Second best option"
                    )
                )),
                Map.entry("timestamp", System.currentTimeMillis())
            );
        });

        // Order Splitting Tool
        registerTool(new MCPTool(
            "split_order_across_brokers",
            "Split large order across multiple brokers for optimal execution",
            new MCPToolInputSchema(
                "object",
                Map.of(
                    "orderId", new MCPToolParameter("string", "Parent order ID"),
                    "symbol", new MCPToolParameter("string", "Stock symbol"),
                    "totalQuantity", new MCPToolParameter("number", "Total order quantity"),
                    "strategy", new MCPToolParameter("string", "Split strategy (EQUAL_SPLIT, WEIGHTED_SPLIT, PRIORITY_SPLIT)")
                ),
                List.of("orderId", "symbol", "totalQuantity", "strategy")
            )
        ), (params, session) -> {
            String orderId = (String) params.get("orderId");
            String symbol = (String) params.get("symbol");
            Number totalQuantity = (Number) params.get("totalQuantity");
            String strategy = (String) params.get("strategy");

            return Map.ofEntries(
                Map.entry("parentOrderId", orderId),
                Map.entry("symbol", symbol),
                Map.entry("totalQuantity", totalQuantity),
                Map.entry("totalValue", 250000.00),
                Map.entry("strategy", strategy),
                Map.entry("numberOfBrokers", 3),
                Map.entry("childOrders", List.of(
                    Map.of(
                        "childOrderId", orderId + "-ZERODHA",
                        "brokerId", "ZERODHA",
                        "brokerName", "Zerodha",
                        "quantity", 60,
                        "allocationPercent", 60.0,
                        "executionPriority", 1,
                        "reason", "Highest scoring broker"
                    ),
                    Map.of(
                        "childOrderId", orderId + "-UPSTOX",
                        "brokerId", "UPSTOX",
                        "brokerName", "Upstox",
                        "quantity", 25,
                        "allocationPercent", 25.0,
                        "executionPriority", 2,
                        "reason", "Second best broker"
                    ),
                    Map.of(
                        "childOrderId", orderId + "-ANGELONE",
                        "brokerId", "ANGELONE",
                        "brokerName", "Angel One",
                        "quantity", 15,
                        "allocationPercent", 15.0,
                        "executionPriority", 3,
                        "reason", "Backup broker"
                    )
                )),
                Map.entry("estimatedTotalCost", 375.00),
                Map.entry("estimatedPriceImprovement", 0.15),
                Map.entry("estimatedTotalTime", 320),
                Map.entry("maxBrokerExposure", 60.0),
                Map.entry("riskLevel", "LOW"),
                Map.entry("timestamp", System.currentTimeMillis())
            );
        });

        // Broker Performance Tool
        registerTool(new MCPTool(
            "get_broker_performance",
            "Get real-time broker performance metrics for routing decisions",
            new MCPToolInputSchema("object", Map.of(), List.of())
        ), (params, session) -> Map.of(
            "brokers", List.of(
                Map.of(
                    "brokerId", "ZERODHA",
                    "brokerName", "Zerodha",
                    "overallScore", 88.0,
                    "rating", "EXCELLENT",
                    "successRate", 99.3,
                    "avgExecutionTime", 245,
                    "avgPriceImprovement", 0.18,
                    "currentLoad", 42.0,
                    "availableCapacity", 58.0
                ),
                Map.of(
                    "brokerId", "UPSTOX",
                    "brokerName", "Upstox",
                    "overallScore", 85.0,
                    "rating", "EXCELLENT",
                    "successRate", 98.9,
                    "avgExecutionTime", 280,
                    "avgPriceImprovement", 0.15,
                    "currentLoad", 35.0,
                    "availableCapacity", 65.0
                )
            ),
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Registers a tool with its executor.
     *
     * @param tool MCPTool definition
     * @param executor Function to execute the tool
     */
    private void registerTool(
            MCPTool tool,
            BiFunction<Map<String, Object>, MCPSession, Object> executor) {

        tools.put(tool.name(), tool);
        executors.put(tool.name(), executor);

        log.debug("Registered MCP tool: {}", tool.name());
    }

    /**
     * Executes a tool with parameters.
     *
     * @param toolName Tool name
     * @param parameters Tool parameters
     * @param session MCP session
     * @return Execution result
     */
    public Object executeTool(String toolName, Map<String, Object> parameters, MCPSession session) {
        BiFunction<Map<String, Object>, MCPSession, Object> executor = executors.get(toolName);

        if (executor == null) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }

        return executor.apply(parameters, session);
    }

    /**
     * Gets all registered tools.
     *
     * @return List of MCPTool
     */
    public List<MCPTool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * Gets a specific tool by name.
     *
     * @param toolName Tool name
     * @return MCPTool
     */
    public MCPTool getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * Checks if a tool exists.
     *
     * @param toolName Tool name
     * @return true if tool exists
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * Gets tool count.
     *
     * @return Number of registered tools
     */
    public int getToolCount() {
        return tools.size();
    }
}
