package com.trademaster.trading.agentos.mcp;

import com.trademaster.trading.agentos.AgentOSComponent;
import com.trademaster.trading.agentos.TradingCapabilityRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Model Context Protocol (MCP) Server for TradeMaster Trading Service
 *
 * Implements the Model Context Protocol specification to expose trading capabilities
 * to external AI agents (like Claude). Provides standard MCP endpoints for tools,
 * resources, and prompts following the official MCP specification.
 *
 * MCP Protocol Features:
 * - Tool Discovery: List available trading tools with schemas
 * - Tool Execution: Execute trading operations with structured parameters
 * - Resource Management: Access trading data and market information
 * - Capability Registry: Expose agent capabilities and health metrics
 * - Real-time Updates: Stream trading events and market data
 *
 * Protocol Compliance:
 * - JSON-RPC 2.0 message format
 * - Standard MCP method names (tools/list, tools/call, resources/list, etc.)
 * - Structured parameter schemas with JSON Schema validation
 * - Error handling with MCP error codes
 * - Async execution with CompletableFuture
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MCPServer {

    private final TradingCapabilityRegistry capabilityRegistry;
    private final MCPToolRegistry toolRegistry;
    private final MCPResourceProvider resourceProvider;
    private final List<AgentOSComponent> agents;

    private final Map<String, MCPSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Initializes MCP server and registers all available tools.
     */
    public void initialize() {
        log.info("Initializing MCP Server for TradeMaster Trading Service");

        // Register all agent capabilities as MCP tools
        agents.forEach(agent -> {
            log.info("Registering agent: {} with capabilities: {}",
                    agent.getAgentId(), agent.getCapabilities());
            toolRegistry.registerAgent(agent);
        });

        log.info("MCP Server initialized with {} tools and {} resources",
                toolRegistry.getToolCount(), resourceProvider.getResourceCount());
    }

    /**
     * Lists all available MCP tools with their schemas.
     * Implements: tools/list method from MCP specification.
     *
     * @return MCPToolsListResponse with all available tools
     */
    public CompletableFuture<MCPToolsListResponse> listTools() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("MCP tools/list request received");

            List<MCPTool> tools = toolRegistry.getAllTools();

            log.debug("Returning {} MCP tools", tools.size());
            return new MCPToolsListResponse(tools, toolRegistry.getToolCount());
        });
    }

    /**
     * Executes an MCP tool with provided parameters.
     * Implements: tools/call method from MCP specification.
     *
     * @param toolName Name of the tool to execute
     * @param parameters Tool parameters as key-value map
     * @param sessionId Optional session ID for context tracking
     * @return MCPToolCallResponse with execution result
     */
    public CompletableFuture<MCPToolCallResponse> callTool(
            String toolName, Map<String, Object> parameters, String sessionId) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("MCP tools/call: tool={}, session={}, params={}",
                    toolName, sessionId, parameters.keySet());

            try {
                // Get or create session
                MCPSession session = getOrCreateSession(sessionId);

                // Validate tool exists
                if (!toolRegistry.hasTool(toolName)) {
                    return MCPToolCallResponse.error(
                        -32601,
                        "Tool not found: " + toolName,
                        Map.of("tool", toolName)
                    );
                }

                // Validate parameters
                MCPTool tool = toolRegistry.getTool(toolName);
                var validationResult = validateParameters(tool, parameters);
                if (!validationResult.valid()) {
                    return MCPToolCallResponse.error(
                        -32602,
                        "Invalid parameters: " + validationResult.error(),
                        Map.of("errors", validationResult.errors())
                    );
                }

                // Execute tool
                Object result = toolRegistry.executeTool(toolName, parameters, session);

                // Update session context
                session.recordToolCall(toolName, parameters, result);

                log.info("Tool executed successfully: tool={}, session={}", toolName, sessionId);
                return MCPToolCallResponse.success(result);

            } catch (Exception e) {
                log.error("Tool execution failed: tool={}, session={}", toolName, sessionId, e);
                return MCPToolCallResponse.error(
                    -32000,
                    "Tool execution failed: " + e.getMessage(),
                    Map.of("error", e.getClass().getSimpleName())
                );
            }
        });
    }

    /**
     * Lists all available MCP resources.
     * Implements: resources/list method from MCP specification.
     *
     * @return MCPResourcesListResponse with all available resources
     */
    public CompletableFuture<MCPResourcesListResponse> listResources() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("MCP resources/list request received");

            List<MCPResource> resources = resourceProvider.getAllResources();

            log.debug("Returning {} MCP resources", resources.size());
            return new MCPResourcesListResponse(resources, resources.size());
        });
    }

    /**
     * Reads a specific MCP resource by URI.
     * Implements: resources/read method from MCP specification.
     *
     * @param uri Resource URI (e.g., "trademaster://portfolio/summary")
     * @param sessionId Optional session ID for context tracking
     * @return MCPResourceReadResponse with resource contents
     */
    public CompletableFuture<MCPResourceReadResponse> readResource(String uri, String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("MCP resources/read: uri={}, session={}", uri, sessionId);

            try {
                MCPSession session = getOrCreateSession(sessionId);

                Object content = resourceProvider.readResource(uri, session);

                log.debug("Resource read successfully: uri={}", uri);
                return MCPResourceReadResponse.success(uri, content);

            } catch (IllegalArgumentException e) {
                log.warn("Resource not found: uri={}", uri);
                return MCPResourceReadResponse.error(
                    -32601,
                    "Resource not found: " + uri,
                    Map.of("uri", uri)
                );
            } catch (Exception e) {
                log.error("Resource read failed: uri={}", uri, e);
                return MCPResourceReadResponse.error(
                    -32000,
                    "Resource read failed: " + e.getMessage(),
                    Map.of("error", e.getClass().getSimpleName())
                );
            }
        });
    }

    /**
     * Gets server information and capabilities.
     * Implements: initialize method from MCP specification.
     *
     * @return MCPServerInfo with server details
     */
    public CompletableFuture<MCPServerInfo> getServerInfo() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("MCP initialize request received");

            return new MCPServerInfo(
                "TradeMaster Trading Service MCP Server",
                "2.0.0",
                Map.of(
                    "tools", true,
                    "resources", true,
                    "prompts", false,
                    "logging", true
                ),
                Map.of(
                    "vendor", "TradeMaster",
                    "environment", "production",
                    "agentCount", agents.size(),
                    "toolCount", toolRegistry.getToolCount(),
                    "resourceCount", resourceProvider.getResourceCount()
                )
            );
        });
    }

    /**
     * Gets health status of MCP server and all agents.
     *
     * @return MCPHealthStatus with health metrics
     */
    public CompletableFuture<MCPHealthStatus> getHealth() {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("MCP health check request received");

            double overallHealth = capabilityRegistry.calculateOverallHealthScore();

            Map<String, AgentHealth> agentHealthMap = agents.stream()
                .collect(Collectors.toMap(
                    AgentOSComponent::getAgentId,
                    agent -> new AgentHealth(
                        agent.getAgentType(),
                        agent.getHealthScore(),
                        agent.getCapabilities(),
                        "HEALTHY"
                    )
                ));

            return new MCPHealthStatus(
                overallHealth >= 0.7 ? "HEALTHY" : "DEGRADED",
                overallHealth,
                agentHealthMap,
                activeSessions.size(),
                Instant.now()
            );
        });
    }

    /**
     * Creates a new MCP session.
     *
     * @param metadata Session metadata
     * @return New MCPSession
     */
    public MCPSession createSession(Map<String, Object> metadata) {
        MCPSession session = new MCPSession(metadata);
        activeSessions.put(session.getSessionId(), session);

        log.info("MCP session created: sessionId={}, metadata={}",
                session.getSessionId(), metadata.keySet());

        return session;
    }

    /**
     * Gets an existing session or creates a new one.
     *
     * @param sessionId Session ID (null creates new session)
     * @return MCPSession
     */
    private MCPSession getOrCreateSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return createSession(Map.of());
        }

        return activeSessions.computeIfAbsent(sessionId,
            id -> new MCPSession(Map.of("sessionId", id)));
    }

    /**
     * Validates tool parameters against schema.
     *
     * @param tool MCPTool with schema
     * @param parameters Parameters to validate
     * @return ValidationResult
     */
    private ValidationResult validateParameters(MCPTool tool, Map<String, Object> parameters) {
        List<String> errors = new java.util.ArrayList<>();

        // Check required parameters
        tool.inputSchema().required().forEach(param -> {
            if (!parameters.containsKey(param)) {
                errors.add("Missing required parameter: " + param);
            }
        });

        // Validate parameter types
        tool.inputSchema().properties().forEach((name, schema) -> {
            if (parameters.containsKey(name)) {
                Object value = parameters.get(name);
                if (!isValidType(value, schema.type())) {
                    errors.add("Invalid type for parameter '" + name +
                              "': expected " + schema.type() +
                              ", got " + value.getClass().getSimpleName());
                }
            }
        });

        return errors.isEmpty()
            ? new ValidationResult(true, null, List.of())
            : new ValidationResult(false, String.join(", ", errors), errors);
    }

    /**
     * Validates parameter type.
     */
    private boolean isValidType(Object value, String expectedType) {
        return switch (expectedType.toLowerCase()) {
            case "string" -> value instanceof String;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List || value.getClass().isArray();
            case "object" -> value instanceof Map;
            default -> true; // Unknown type, allow
        };
    }

    // ========== DTOs ==========

    public record MCPToolsListResponse(List<MCPTool> tools, int count) {}
    public record MCPResourcesListResponse(List<MCPResource> resources, int count) {}

    public record MCPToolCallResponse(
        boolean success,
        Object result,
        MCPError error
    ) {
        public static MCPToolCallResponse success(Object result) {
            return new MCPToolCallResponse(true, result, null);
        }

        public static MCPToolCallResponse error(int code, String message, Map<String, Object> data) {
            return new MCPToolCallResponse(false, null, new MCPError(code, message, data));
        }
    }

    public record MCPResourceReadResponse(
        boolean success,
        String uri,
        Object content,
        MCPError error
    ) {
        public static MCPResourceReadResponse success(String uri, Object content) {
            return new MCPResourceReadResponse(true, uri, content, null);
        }

        public static MCPResourceReadResponse error(int code, String message, Map<String, Object> data) {
            return new MCPResourceReadResponse(false, null, null, new MCPError(code, message, data));
        }
    }

    public record MCPServerInfo(
        String name,
        String version,
        Map<String, Boolean> capabilities,
        Map<String, Object> metadata
    ) {}

    public record MCPHealthStatus(
        String status,
        double overallHealth,
        Map<String, AgentHealth> agents,
        int activeSessions,
        Instant timestamp
    ) {}

    public record AgentHealth(
        String type,
        double healthScore,
        List<String> capabilities,
        String status
    ) {}

    public record MCPError(int code, String message, Map<String, Object> data) {}

    public record ValidationResult(boolean valid, String error, List<String> errors) {}
}
