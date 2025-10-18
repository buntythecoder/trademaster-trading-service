package com.trademaster.trading.agentos.mcp;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool Definition
 *
 * Represents a tool available through the Model Context Protocol.
 * Tools are executable functions with structured parameters and JSON Schema validation.
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
public record MCPTool(
    String name,
    String description,
    MCPToolInputSchema inputSchema
) {}

/**
 * MCP Tool Input Schema
 *
 * JSON Schema definition for tool parameters with type validation.
 */
record MCPToolInputSchema(
    String type,
    Map<String, MCPToolParameter> properties,
    List<String> required
) {}

/**
 * MCP Tool Parameter
 *
 * Individual parameter definition with type and description.
 */
record MCPToolParameter(
    String type,
    String description
) {}
