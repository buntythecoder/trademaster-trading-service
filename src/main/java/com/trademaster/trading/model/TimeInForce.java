package com.trademaster.trading.model;

import java.time.LocalTime;

/**
 * Time in Force Enumeration
 * 
 * Defines how long an order remains active in the market.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum TimeInForce {
    /**
     * DAY - Order valid until end of trading day
     * Most common time in force
     */
    DAY("Day Order"),
    
    /**
     * GTC - Good Till Cancelled
     * Order remains active until filled or cancelled
     */
    GTC("Good Till Cancelled"),
    
    /**
     * IOC - Immediate or Cancel
     * Execute immediately, cancel unfilled portion
     */
    IOC("Immediate or Cancel"),
    
    /**
     * FOK - Fill or Kill
     * Execute completely immediately or cancel entire order
     */
    FOK("Fill or Kill"),
    
    /**
     * GTD - Good Till Date
     * Order valid until specified date
     */
    GTD("Good Till Date");
    
    private final String displayName;
    
    TimeInForce(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if order expires at end of trading day
     */
    public boolean expiresEndOfDay() {
        return this == DAY;
    }
    
    /**
     * Check if order requires immediate execution
     */
    public boolean requiresImmediateExecution() {
        return this == IOC || this == FOK;
    }
    
    /**
     * Check if partial fills are allowed
     */
    public boolean allowsPartialFills() {
        return this != FOK;
    }
    
    /**
     * Get default market close time for DAY orders
     */
    public static LocalTime getMarketCloseTime() {
        return LocalTime.of(15, 30); // NSE/BSE market closes at 3:30 PM
    }
}