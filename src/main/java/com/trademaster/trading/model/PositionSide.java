package com.trademaster.trading.model;

/**
 * Position Side Enum
 * 
 * Represents the side/direction of a trading position.
 * Used for portfolio management and risk calculations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum PositionSide {
    
    /**
     * Long position - owning the asset
     * Profits when price increases
     */
    LONG("LONG", "Long Position", 1),
    
    /**
     * Short position - borrowing and selling the asset
     * Profits when price decreases
     */
    SHORT("SHORT", "Short Position", -1);
    
    private final String code;
    private final String description;
    private final int multiplier;
    
    PositionSide(String code, String description, int multiplier) {
        this.code = code;
        this.description = description;
        this.multiplier = multiplier;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get multiplier for P&L calculations
     * Long: +1, Short: -1
     */
    public int getMultiplier() {
        return multiplier;
    }
    
    /**
     * Check if position is long
     */
    public boolean isLong() {
        return this == LONG;
    }
    
    /**
     * Check if position is short
     */
    public boolean isShort() {
        return this == SHORT;
    }
    
    /**
     * Get opposite position side
     */
    public PositionSide getOpposite() {
        return switch (this) {
            case LONG -> SHORT;
            case SHORT -> LONG;
        };
    }
    
    /**
     * Calculate P&L based on entry and current price
     */
    public double calculatePnL(double entryPrice, double currentPrice, int quantity) {
        return (currentPrice - entryPrice) * multiplier * quantity;
    }
    
    @Override
    public String toString() {
        return code;
    }
}