package com.trademaster.trading.agentos.mcp;

/**
 * MCP Resource Definition
 *
 * Represents a resource available through the Model Context Protocol.
 * Resources are data sources that can be read by AI agents (market data, portfolio info, etc.).
 *
 * Resource URI Format: trademaster://[category]/[resource-id]
 * Examples:
 * - trademaster://portfolio/summary
 * - trademaster://market/quotes/RELIANCE
 * - trademaster://risk/assessment/portfolio
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
public record MCPResource(
    String uri,
    String name,
    String description,
    String mimeType
) {}
