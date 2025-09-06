package com.trademaster.trading.routing;

/**
 * Execution Strategy Enumeration
 * 
 * Defines different strategies for order execution based on
 * market conditions, order characteristics, and user preferences.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum ExecutionStrategy {
    /**
     * IMMEDIATE - Execute order immediately at market
     */
    IMMEDIATE("Execute immediately"),
    
    /**
     * SCHEDULED - Execute at specified time
     */
    SCHEDULED("Execute at scheduled time"),
    
    /**
     * SLICED - Break large order into smaller pieces
     */
    SLICED("Break order into smaller slices"),
    
    /**
     * ICEBERG - Hide order size (show only small portion)
     */
    ICEBERG("Hide order size with iceberg strategy"),
    
    /**
     * VWAP - Volume Weighted Average Price execution
     */
    VWAP("Execute using VWAP strategy"),
    
    /**
     * TWAP - Time Weighted Average Price execution
     */
    TWAP("Execute using TWAP strategy"),
    
    /**
     * DARK_POOL - Route to dark pool
     */
    DARK_POOL("Route to dark pool"),
    
    /**
     * SMART - Intelligent routing based on market conditions
     */
    SMART("Smart order routing"),
    
    /**
     * REJECT - Reject order execution
     */
    REJECT("Reject order");
    
    private final String description;
    
    ExecutionStrategy(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if strategy requires immediate execution
     */
    public boolean isImmediate() {
        return this == IMMEDIATE;
    }
    
    /**
     * Check if strategy is algorithmic
     */
    public boolean isAlgorithmic() {
        return this == VWAP || this == TWAP || this == ICEBERG || this == SLICED;
    }
    
    /**
     * Check if strategy is suitable for large orders
     */
    public boolean isSuitableForLargeOrders() {
        return this == SLICED || this == ICEBERG || this == VWAP || this == TWAP || this == DARK_POOL;
    }
}