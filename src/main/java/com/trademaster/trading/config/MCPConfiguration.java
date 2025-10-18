package com.trademaster.trading.config;

import com.trademaster.trading.agentos.mcp.MCPResourceProvider;
import com.trademaster.trading.agentos.mcp.MCPServer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * MCP (Model Context Protocol) Configuration
 *
 * Initializes the MCP server and resource provider on application startup.
 * Registers all AgentOS components as MCP tools and exposes trading resources.
 *
 * Initialization Flow:
 * 1. Initialize MCPResourceProvider - Register all trading resources
 * 2. Initialize MCPServer - Register all agent tools and capabilities
 * 3. Log initialization summary with counts
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MCPConfiguration {

    private final MCPServer mcpServer;
    private final MCPResourceProvider resourceProvider;

    /**
     * Initializes MCP server and resource provider on application startup.
     */
    @PostConstruct
    public void initializeMCP() {
        log.info("==========================================================");
        log.info("Initializing Model Context Protocol (MCP) Server");
        log.info("==========================================================");

        try {
            // Step 1: Initialize resource provider
            log.info("Step 1: Initializing MCP Resource Provider...");
            resourceProvider.initialize();
            log.info("✅ Resource Provider initialized with {} resources",
                    resourceProvider.getResourceCount());

            // Step 2: Initialize MCP server
            log.info("Step 2: Initializing MCP Server...");
            mcpServer.initialize();
            log.info("✅ MCP Server initialized");

            // Log summary
            log.info("==========================================================");
            log.info("MCP Server Initialization Complete");
            log.info("==========================================================");
            log.info("Server Name: TradeMaster Trading Service MCP Server");
            log.info("Server Version: 2.0.0");
            log.info("Resources Available: {}", resourceProvider.getResourceCount());
            log.info("Tools Available: Check /mcp/tools/list for complete list");
            log.info("==========================================================");
            log.info("MCP Endpoints:");
            log.info("  POST /mcp/initialize       - Get server info");
            log.info("  GET  /mcp/tools/list       - List available tools");
            log.info("  POST /mcp/tools/call       - Execute a tool");
            log.info("  GET  /mcp/resources/list   - List available resources");
            log.info("  POST /mcp/resources/read   - Read a resource");
            log.info("  GET  /mcp/health           - Health status");
            log.info("  POST /mcp/sessions/create  - Create session");
            log.info("==========================================================");

        } catch (Exception e) {
            log.error("❌ Failed to initialize MCP Server", e);
            throw new RuntimeException("MCP Server initialization failed", e);
        }
    }
}
