package com.trademaster.trading.agentos.mcp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * MCP Resource Provider for TradeMaster Trading Service
 *
 * Provides access to trading resources through the Model Context Protocol.
 * Resources are read-only data sources that AI agents can access for context and information.
 *
 * Resource Categories:
 * - Portfolio: Holdings, positions, balances
 * - Market: Quotes, order book, market status
 * - Risk: Risk metrics, compliance status
 * - Analytics: Performance metrics, P&L reports
 * - Orders: Order history, open orders
 *
 * Resource URI Format: trademaster://[category]/[resource-id]
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MCPResourceProvider {

    private final Map<String, MCPResource> resources = new ConcurrentHashMap<>();
    private final Map<String, BiFunction<String, MCPSession, Object>> readers = new ConcurrentHashMap<>();

    /**
     * Initializes MCP resource provider and registers all resources.
     */
    public void initialize() {
        log.info("Initializing MCP Resource Provider");

        registerPortfolioResources();
        registerMarketDataResources();
        registerRiskResources();
        registerAnalyticsResources();
        registerOrderResources();

        log.info("MCP Resource Provider initialized with {} resources", resources.size());
    }

    /**
     * Registers portfolio resources.
     */
    private void registerPortfolioResources() {
        // Portfolio Summary Resource
        registerResource(
            new MCPResource(
                "trademaster://portfolio/summary",
                "Portfolio Summary",
                "Complete portfolio summary with holdings, positions, and balances",
                "application/json"
            ),
            (uri, session) -> Map.of(
                "totalValue", 1250000.00,
                "cash", 250000.00,
                "holdings", 1000000.00,
                "todayPnL", 15000.00,
                "todayPnLPercent", 1.2,
                "totalPnL", 150000.00,
                "totalPnLPercent", 13.6,
                "positionCount", 8,
                "timestamp", Instant.now().toString()
            )
        );

        // Portfolio Positions Resource
        registerResource(
            new MCPResource(
                "trademaster://portfolio/positions",
                "Portfolio Positions",
                "All open positions with P&L and metrics",
                "application/json"
            ),
            (uri, session) -> List.of(
                Map.of(
                    "symbol", "RELIANCE",
                    "quantity", 100,
                    "avgPrice", 2450.00,
                    "currentPrice", 2500.00,
                    "pnl", 5000.00,
                    "pnlPercent", 2.04
                ),
                Map.of(
                    "symbol", "TCS",
                    "quantity", 50,
                    "avgPrice", 3200.00,
                    "currentPrice", 3350.00,
                    "pnl", 7500.00,
                    "pnlPercent", 4.69
                )
            )
        );
    }

    /**
     * Registers market data resources.
     */
    private void registerMarketDataResources() {
        // Market Status Resource
        registerResource(
            new MCPResource(
                "trademaster://market/status",
                "Market Status",
                "Current market status and trading hours",
                "application/json"
            ),
            (uri, session) -> Map.of(
                "status", "OPEN",
                "exchange", "NSE",
                "openTime", "09:15",
                "closeTime", "15:30",
                "isPreMarket", false,
                "isMarketHours", true,
                "isPostMarket", false,
                "nextOpen", "2024-01-02T09:15:00Z",
                "timestamp", Instant.now().toString()
            )
        );

        // Top Gainers Resource
        registerResource(
            new MCPResource(
                "trademaster://market/top-gainers",
                "Top Gainers",
                "Top performing stocks of the day",
                "application/json"
            ),
            (uri, session) -> List.of(
                Map.of("symbol", "INFY", "change", 5.2, "changePercent", 3.8),
                Map.of("symbol", "TCS", "change", 150.0, "changePercent", 4.7),
                Map.of("symbol", "WIPRO", "change", 18.5, "changePercent", 4.2)
            )
        );
    }

    /**
     * Registers risk resources.
     */
    private void registerRiskResources() {
        // Portfolio Risk Metrics Resource
        registerResource(
            new MCPResource(
                "trademaster://risk/portfolio-metrics",
                "Portfolio Risk Metrics",
                "Comprehensive risk metrics for entire portfolio",
                "application/json"
            ),
            (uri, session) -> Map.of(
                "sharpeRatio", 1.45,
                "maxDrawdown", 8.5,
                "volatility", 18.5,
                "beta", 1.05,
                "var95", 25000.00,
                "riskLevel", "MEDIUM",
                "riskScore", 42,
                "timestamp", Instant.now().toString()
            )
        );

        // Compliance Status Resource
        registerResource(
            new MCPResource(
                "trademaster://risk/compliance-status",
                "Compliance Status",
                "Current compliance and regulatory status",
                "application/json"
            ),
            (uri, session) -> Map.of(
                "status", "COMPLIANT",
                "patternDayTrader", false,
                "marginCall", false,
                "restrictedSymbols", List.of(),
                "pendingActions", List.of(),
                "lastAudit", "2024-01-01T10:00:00Z",
                "timestamp", Instant.now().toString()
            )
        );
    }

    /**
     * Registers analytics resources.
     */
    private void registerAnalyticsResources() {
        // Performance Metrics Resource
        registerResource(
            new MCPResource(
                "trademaster://analytics/performance",
                "Performance Metrics",
                "Portfolio performance analytics and metrics",
                "application/json"
            ),
            (uri, session) -> Map.of(
                "todayReturn", 1.2,
                "weekReturn", 3.5,
                "monthReturn", 8.2,
                "yearReturn", 13.6,
                "winRate", 65.5,
                "profitFactor", 1.85,
                "avgWin", 8500.00,
                "avgLoss", 4200.00,
                "totalTrades", 145,
                "timestamp", Instant.now().toString()
            )
        );
    }

    /**
     * Registers order resources.
     */
    private void registerOrderResources() {
        // Open Orders Resource
        registerResource(
            new MCPResource(
                "trademaster://orders/open",
                "Open Orders",
                "All currently open orders",
                "application/json"
            ),
            (uri, session) -> List.of(
                Map.of(
                    "orderId", "ORD-12345",
                    "symbol", "RELIANCE",
                    "type", "LIMIT",
                    "side", "BUY",
                    "quantity", 50,
                    "price", 2480.00,
                    "status", "PENDING",
                    "timestamp", "2024-01-01T10:30:00Z"
                )
            )
        );

        // Recent Orders Resource
        registerResource(
            new MCPResource(
                "trademaster://orders/recent",
                "Recent Orders",
                "Recently executed orders (last 10)",
                "application/json"
            ),
            (uri, session) -> List.of(
                Map.of(
                    "orderId", "ORD-12340",
                    "symbol", "TCS",
                    "type", "MARKET",
                    "side", "BUY",
                    "quantity", 20,
                    "price", 3350.00,
                    "status", "FILLED",
                    "timestamp", "2024-01-01T09:45:00Z"
                ),
                Map.of(
                    "orderId", "ORD-12339",
                    "symbol", "INFY",
                    "type", "LIMIT",
                    "side", "SELL",
                    "quantity", 30,
                    "price", 1425.00,
                    "status", "FILLED",
                    "timestamp", "2024-01-01T09:30:00Z"
                )
            )
        );
    }

    /**
     * Registers a resource with its reader function.
     *
     * @param resource MCPResource definition
     * @param reader Function to read resource content
     */
    private void registerResource(
            MCPResource resource,
            BiFunction<String, MCPSession, Object> reader) {

        resources.put(resource.uri(), resource);
        readers.put(resource.uri(), reader);

        log.debug("Registered MCP resource: {}", resource.uri());
    }

    /**
     * Reads a resource by URI.
     *
     * @param uri Resource URI
     * @param session MCP session
     * @return Resource content
     */
    public Object readResource(String uri, MCPSession session) {
        BiFunction<String, MCPSession, Object> reader = readers.get(uri);

        if (reader == null) {
            throw new IllegalArgumentException("Resource not found: " + uri);
        }

        log.debug("Reading MCP resource: uri={}, session={}", uri, session.getSessionId());

        return reader.apply(uri, session);
    }

    /**
     * Gets all registered resources.
     *
     * @return List of MCPResource
     */
    public List<MCPResource> getAllResources() {
        return new ArrayList<>(resources.values());
    }

    /**
     * Gets resource count.
     *
     * @return Number of registered resources
     */
    public int getResourceCount() {
        return resources.size();
    }

    /**
     * Checks if a resource exists.
     *
     * @param uri Resource URI
     * @return true if resource exists
     */
    public boolean hasResource(String uri) {
        return resources.containsKey(uri);
    }
}
