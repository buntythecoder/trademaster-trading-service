package com.trademaster.trading.model;

/**
 * Order Type Enumeration
 * 
 * Defines the different types of orders supported by the trading system:
 * - MARKET: Execute immediately at current market price
 * - LIMIT: Execute only at specified price or better
 * - STOP_LOSS: Convert to market order when stop price is reached
 * - STOP_LIMIT: Convert to limit order when stop price is reached
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum OrderType {
    /**
     * Market Order - Execute immediately at best available price
     * - Provides immediate execution
     * - Price not guaranteed
     * - Used for urgent trades
     */
    MARKET("Market Order"),
    
    /**
     * Limit Order - Execute only at specified price or better
     * - Price protection for trader
     * - May not execute if price not reached
     * - Most common order type
     */
    LIMIT("Limit Order"),
    
    /**
     * Stop Loss Order - Market order triggered at stop price
     * - Risk management tool
     * - Converts to market order when triggered
     * - Used to limit losses
     */
    STOP_LOSS("Stop Loss Order"),
    
    /**
     * Stop Limit Order - Limit order triggered at stop price
     * - Combines stop loss with price protection
     * - More complex execution logic
     * - Used for sophisticated strategies
     */
    STOP_LIMIT("Stop Limit Order");
    
    private final String displayName;
    
    OrderType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if order type requires a limit price
     */
    public boolean requiresLimitPrice() {
        return this == LIMIT || this == STOP_LIMIT;
    }
    
    /**
     * Check if order type requires a stop price
     */
    public boolean requiresStopPrice() {
        return this == STOP_LOSS || this == STOP_LIMIT;
    }
    
    /**
     * Check if order type executes immediately
     */
    public boolean isImmediateExecution() {
        return this == MARKET;
    }
}