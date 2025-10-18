package com.trademaster.trading.execution;

/**
 * Execution Status Enum
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #14 - Pattern Matching Excellence
 *
 * Enumeration of order execution statuses for pattern matching.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum ExecutionStatus {
    /**
     * Order placed but not yet executed
     */
    PENDING("Pending", "Order placed and awaiting execution"),

    /**
     * Order partially filled
     */
    PARTIAL_FILL("Partial Fill", "Order partially executed"),

    /**
     * Order completely filled
     */
    FILLED("Filled", "Order completely executed"),

    /**
     * Order cancelled before execution
     */
    CANCELLED("Cancelled", "Order cancelled"),

    /**
     * Order rejected by broker
     */
    REJECTED("Rejected", "Order rejected by broker"),

    /**
     * Order expired without execution
     */
    EXPIRED("Expired", "Order expired"),

    /**
     * Execution failed due to error
     */
    FAILED("Failed", "Execution failed");

    private final String displayName;
    private final String description;

    ExecutionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if status is terminal (no further updates expected)
     */
    public boolean isTerminal() {
        return switch (this) {
            case FILLED, CANCELLED, REJECTED, EXPIRED, FAILED -> true;
            case PENDING, PARTIAL_FILL -> false;
        };
    }

    /**
     * Check if status indicates success
     */
    public boolean isSuccessful() {
        return switch (this) {
            case FILLED, PARTIAL_FILL -> true;
            case PENDING, CANCELLED, REJECTED, EXPIRED, FAILED -> false;
        };
    }

    /**
     * Check if status requires action
     */
    public boolean requiresAction() {
        return switch (this) {
            case PENDING, PARTIAL_FILL -> true;
            case FILLED, CANCELLED, REJECTED, EXPIRED, FAILED -> false;
        };
    }

    /**
     * Get status from string (case-insensitive)
     */
    public static ExecutionStatus fromString(String status) {
        return switch (status.toUpperCase()) {
            case "PENDING", "OPEN", "NEW" -> PENDING;
            case "PARTIAL_FILL", "PARTIAL", "PARTIALLY_FILLED" -> PARTIAL_FILL;
            case "FILLED", "COMPLETE", "EXECUTED" -> FILLED;
            case "CANCELLED", "CANCELED" -> CANCELLED;
            case "REJECTED", "REJECT" -> REJECTED;
            case "EXPIRED", "EXPIRE" -> EXPIRED;
            case "FAILED", "FAIL", "ERROR" -> FAILED;
            default -> throw new IllegalArgumentException("Unknown execution status: " + status);
        };
    }
}
