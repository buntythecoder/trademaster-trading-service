package com.trademaster.trading.model;

/**
 * Order Side Enumeration
 * 
 * Defines whether an order is buying or selling securities.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum OrderSide {
    /**
     * Buy Order - Purchase securities
     */
    BUY("Buy"),
    
    /**
     * Sell Order - Dispose of securities
     */
    SELL("Sell");
    
    private final String displayName;
    
    OrderSide(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the opposite side for hedging operations
     */
    public OrderSide opposite() {
        return this == BUY ? SELL : BUY;
    }
}