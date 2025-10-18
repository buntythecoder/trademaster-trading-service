package com.trademaster.trading.controller;

import com.trademaster.trading.agentos.mcp.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Model Context Protocol (MCP) REST API Controller
 *
 * Exposes TradeMaster trading capabilities through the Model Context Protocol.
 * Implements MCP specification for tools, resources, and server information.
 *
 * MCP Endpoints:
 * - POST /mcp/initialize - Get server information and capabilities
 * - GET /mcp/tools/list - List all available tools
 * - POST /mcp/tools/call - Execute a tool
 * - GET /mcp/resources/list - List all available resources
 * - POST /mcp/resources/read - Read a resource
 * - GET /mcp/health - Get server and agent health status
 *
 * Protocol Compliance:
 * - JSON-RPC 2.0 message format
 * - Standard MCP method names
 * - Structured parameter validation
 * - Error codes per MCP specification
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Validated
@Slf4j
public class MCPController {

    private final MCPServer mcpServer;

    /**
     * Initialize MCP server and get capabilities.
     * Implements: initialize method from MCP specification.
     *
     * @return Server information with capabilities
     */
    @PostMapping("/initialize")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'AGENT')")
    public CompletableFuture<ResponseEntity<MCPServer.MCPServerInfo>> initialize() {
        log.info("MCP initialize request received");

        return mcpServer.getServerInfo()
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> {
                log.error("MCP initialize failed", ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * List all available MCP tools.
     * Implements: tools/list method from MCP specification.
     *
     * @return List of available tools with schemas
     */
    @GetMapping("/tools/list")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'AGENT')")
    public CompletableFuture<ResponseEntity<MCPServer.MCPToolsListResponse>> listTools() {
        log.info("MCP tools/list request received");

        return mcpServer.listTools()
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> {
                log.error("MCP tools/list failed", ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Execute an MCP tool.
     * Implements: tools/call method from MCP specification.
     *
     * @param request Tool call request with tool name and parameters
     * @return Tool execution result
     */
    @PostMapping("/tools/call")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'AGENT')")
    public CompletableFuture<ResponseEntity<MCPServer.MCPToolCallResponse>> callTool(
            @RequestBody @Validated ToolCallRequest request) {

        log.info("MCP tools/call request: tool={}, session={}",
                request.toolName(), request.sessionId());

        return mcpServer.callTool(request.toolName(), request.parameters(), request.sessionId())
            .thenApply(response -> {
                if (response.success()) {
                    return ResponseEntity.ok(response);
                } else {
                    // Return 200 with error in body (MCP convention)
                    return ResponseEntity.ok(response);
                }
            })
            .exceptionally(ex -> {
                log.error("MCP tools/call failed: tool={}", request.toolName(), ex);
                return ResponseEntity.ok(
                    MCPServer.MCPToolCallResponse.error(
                        -32603,
                        "Internal error: " + ex.getMessage(),
                        Map.of("error", ex.getClass().getSimpleName())
                    )
                );
            });
    }

    /**
     * List all available MCP resources.
     * Implements: resources/list method from MCP specification.
     *
     * @return List of available resources
     */
    @GetMapping("/resources/list")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'AGENT')")
    public CompletableFuture<ResponseEntity<MCPServer.MCPResourcesListResponse>> listResources() {
        log.info("MCP resources/list request received");

        return mcpServer.listResources()
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> {
                log.error("MCP resources/list failed", ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Read an MCP resource.
     * Implements: resources/read method from MCP specification.
     *
     * @param request Resource read request with URI
     * @return Resource contents
     */
    @PostMapping("/resources/read")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'AGENT')")
    public CompletableFuture<ResponseEntity<MCPServer.MCPResourceReadResponse>> readResource(
            @RequestBody @Validated ResourceReadRequest request) {

        log.info("MCP resources/read request: uri={}, session={}",
                request.uri(), request.sessionId());

        return mcpServer.readResource(request.uri(), request.sessionId())
            .thenApply(response -> {
                if (response.success()) {
                    return ResponseEntity.ok(response);
                } else {
                    // Return 200 with error in body (MCP convention)
                    return ResponseEntity.ok(response);
                }
            })
            .exceptionally(ex -> {
                log.error("MCP resources/read failed: uri={}", request.uri(), ex);
                return ResponseEntity.ok(
                    MCPServer.MCPResourceReadResponse.error(
                        -32603,
                        "Internal error: " + ex.getMessage(),
                        Map.of("error", ex.getClass().getSimpleName())
                    )
                );
            });
    }

    /**
     * Get MCP server health status.
     *
     * @return Health status with agent metrics
     */
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'AGENT')")
    public CompletableFuture<ResponseEntity<MCPServer.MCPHealthStatus>> getHealth() {
        log.debug("MCP health check request received");

        return mcpServer.getHealth()
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> {
                log.error("MCP health check failed", ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Create a new MCP session.
     *
     * @param request Session creation request with metadata
     * @return New session with session ID
     */
    @PostMapping("/sessions/create")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'AGENT')")
    public ResponseEntity<SessionResponse> createSession(
            @RequestBody(required = false) SessionCreateRequest request) {

        log.info("MCP session creation request: metadata={}",
                request != null ? request.metadata().keySet() : Map.of());

        Map<String, Object> metadata = request != null ? request.metadata() : Map.of();
        MCPSession session = mcpServer.createSession(metadata);

        return ResponseEntity.ok(new SessionResponse(
            session.getSessionId(),
            session.getCreatedAt().toString(),
            metadata
        ));
    }

    // ========== DTOs ==========

    /**
     * Tool call request DTO.
     */
    public record ToolCallRequest(
        String toolName,
        Map<String, Object> parameters,
        String sessionId
    ) {}

    /**
     * Resource read request DTO.
     */
    public record ResourceReadRequest(
        String uri,
        String sessionId
    ) {}

    /**
     * Session creation request DTO.
     */
    public record SessionCreateRequest(
        Map<String, Object> metadata
    ) {}

    /**
     * Session response DTO.
     */
    public record SessionResponse(
        String sessionId,
        String createdAt,
        Map<String, Object> metadata
    ) {}
}
