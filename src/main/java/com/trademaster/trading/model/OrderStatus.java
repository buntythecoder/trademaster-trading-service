package com.trademaster.trading.model;

/**
 * Order Status Enumeration
 * 
 * Represents the lifecycle states of an order from placement to completion.
 * Implements a state machine pattern for order processing.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum OrderStatus {
    /**
     * PENDING - Order created but not yet validated
     * Next states: VALIDATED, REJECTED
     */
    PENDING("Pending"),
    
    /**
     * VALIDATED - Order passed validation and risk checks
     * Next states: SUBMITTED, REJECTED
     */
    VALIDATED("Validated"),
    
    /**
     * SUBMITTED - Order sent to broker/exchange
     * Next states: ACKNOWLEDGED, REJECTED
     */
    SUBMITTED("Submitted"),
    
    /**
     * ACKNOWLEDGED - Order acknowledged by broker/exchange
     * Next states: PARTIALLY_FILLED, FILLED, CANCELLED
     */
    ACKNOWLEDGED("Acknowledged"),
    
    /**
     * PARTIALLY_FILLED - Order partially executed
     * Next states: FILLED, CANCELLED
     */
    PARTIALLY_FILLED("Partially Filled"),
    
    /**
     * FILLED - Order completely executed
     * Terminal state
     */
    FILLED("Filled"),
    
    /**
     * CANCELLED - Order cancelled by user or system
     * Terminal state
     */
    CANCELLED("Cancelled"),
    
    /**
     * REJECTED - Order rejected due to validation or broker issues
     * Terminal state
     */
    REJECTED("Rejected"),
    
    /**
     * EXPIRED - Order expired due to time constraints
     * Terminal state
     */
    EXPIRED("Expired");
    
    private final String displayName;
    
    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if order is in a terminal state (cannot be modified)
     */
    public boolean isTerminal() {
        return this == FILLED || this == CANCELLED || this == REJECTED || this == EXPIRED;
    }
    
    /**
     * Check if order is actively trading
     */
    public boolean isActive() {
        return this == ACKNOWLEDGED || this == PARTIALLY_FILLED;
    }
    
    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return !isTerminal() && this != PENDING && this != VALIDATED;
    }
    
    /**
     * Check if order can be modified
     */
    public boolean isModifiable() {
        return this == ACKNOWLEDGED && !isTerminal();
    }
    
    /**
     * Check if status transition is valid
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == VALIDATED || newStatus == REJECTED;
            case VALIDATED -> newStatus == SUBMITTED || newStatus == REJECTED;
            case SUBMITTED -> newStatus == ACKNOWLEDGED || newStatus == REJECTED;
            case ACKNOWLEDGED -> newStatus == PARTIALLY_FILLED || newStatus == FILLED || 
                               newStatus == CANCELLED || newStatus == EXPIRED;
            case PARTIALLY_FILLED -> newStatus == FILLED || newStatus == CANCELLED || 
                                   newStatus == EXPIRED;
            default -> false; // Terminal states cannot transition
        };
    }
}