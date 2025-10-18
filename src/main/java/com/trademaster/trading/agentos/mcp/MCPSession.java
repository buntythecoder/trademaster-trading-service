package com.trademaster.trading.agentos.mcp;

import lombok.Getter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Session Management
 *
 * Manages session state and context for MCP tool executions.
 * Tracks tool calls, maintains conversation history, and stores session-specific data.
 *
 * Session Features:
 * - Unique session ID generation
 * - Tool call history tracking
 * - Context accumulation across tool calls
 * - Session metadata storage
 * - Automatic expiration handling
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Getter
public class MCPSession {

    private final String sessionId;
    private final Instant createdAt;
    private Instant lastAccessedAt;
    private final Map<String, Object> metadata;
    private final List<ToolCallRecord> toolCallHistory;
    private final Map<String, Object> context;

    /**
     * Creates a new MCP session.
     *
     * @param metadata Session metadata
     */
    public MCPSession(Map<String, Object> metadata) {
        this.sessionId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.lastAccessedAt = Instant.now();
        this.metadata = new ConcurrentHashMap<>(metadata);
        this.toolCallHistory = Collections.synchronizedList(new ArrayList<>());
        this.context = new ConcurrentHashMap<>();
    }

    /**
     * Records a tool call in session history.
     *
     * @param toolName Tool name
     * @param parameters Tool parameters
     * @param result Tool execution result
     */
    public void recordToolCall(String toolName, Map<String, Object> parameters, Object result) {
        this.lastAccessedAt = Instant.now();

        ToolCallRecord record = new ToolCallRecord(
            toolName,
            parameters,
            result,
            Instant.now()
        );

        toolCallHistory.add(record);

        // Update context with tool results
        context.put("lastToolCall", toolName);
        context.put("lastResult", result);
        context.put("toolCallCount", toolCallHistory.size());
    }

    /**
     * Gets session context value.
     *
     * @param key Context key
     * @return Context value
     */
    public Object getContextValue(String key) {
        return context.get(key);
    }

    /**
     * Sets session context value.
     *
     * @param key Context key
     * @param value Context value
     */
    public void setContextValue(String key, Object value) {
        this.lastAccessedAt = Instant.now();
        context.put(key, value);
    }

    /**
     * Gets session metadata value.
     *
     * @param key Metadata key
     * @return Metadata value
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Sets session metadata value.
     *
     * @param key Metadata key
     * @param value Metadata value
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Gets tool call history.
     *
     * @return List of tool call records
     */
    public List<ToolCallRecord> getToolCallHistory() {
        return Collections.unmodifiableList(toolCallHistory);
    }

    /**
     * Gets the last tool call.
     *
     * @return Last ToolCallRecord or null
     */
    public ToolCallRecord getLastToolCall() {
        return toolCallHistory.isEmpty() ? null : toolCallHistory.get(toolCallHistory.size() - 1);
    }

    /**
     * Checks if session has expired.
     *
     * @param timeoutMinutes Timeout in minutes
     * @return true if session expired
     */
    public boolean isExpired(long timeoutMinutes) {
        Instant expirationTime = lastAccessedAt.plusSeconds(timeoutMinutes * 60);
        return Instant.now().isAfter(expirationTime);
    }

    /**
     * Gets session duration in seconds.
     *
     * @return Duration in seconds
     */
    public long getDurationSeconds() {
        return Instant.now().getEpochSecond() - createdAt.getEpochSecond();
    }

    /**
     * Tool call record.
     */
    public record ToolCallRecord(
        String toolName,
        Map<String, Object> parameters,
        Object result,
        Instant timestamp
    ) {}
}
