package com.trademaster.trading.risk;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Risk Metrics
 * 
 * Comprehensive risk metrics for a user's trading activity
 * and current position exposure.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class RiskMetrics {
    
    /**
     * User ID
     */
    private Long userId;
    
    /**
     * Total portfolio value
     */
    private BigDecimal portfolioValue;
    
    /**
     * Available buying power
     */
    private BigDecimal buyingPower;
    
    /**
     * Used margin
     */
    private BigDecimal usedMargin;
    
    /**
     * Available margin
     */
    private BigDecimal availableMargin;
    
    /**
     * Total position value
     */
    private BigDecimal totalPositionValue;
    
    /**
     * Largest position value
     */
    private BigDecimal largestPositionValue;
    
    /**
     * Largest position symbol
     */
    private String largestPositionSymbol;
    
    /**
     * Number of open positions
     */
    private int openPositions;
    
    /**
     * Number of open orders
     */
    private int openOrders;
    
    /**
     * Daily trade count
     */
    private int dailyTradeCount;
    
    /**
     * Daily trading volume
     */
    private BigDecimal dailyVolume;
    
    /**
     * Maximum single order value allowed
     */
    private BigDecimal maxOrderValue;
    
    /**
     * Maximum position value allowed
     */
    private BigDecimal maxPositionValue;
    
    /**
     * Pattern day trader status
     */
    private boolean patternDayTrader;
    
    /**
     * Day trading buying power
     */
    private BigDecimal dayTradingBuyingPower;
    
    /**
     * Current risk score (0.0 to 1.0)
     */
    private double currentRiskScore;
    
    /**
     * Risk utilization percentage
     */
    private double riskUtilization;
    
    /**
     * Metrics calculation timestamp
     */
    private Instant calculatedAt;
    
    /**
     * Calculate position concentration risk
     */
    public double getConcentrationRisk() {
        // Eliminates if-statement using Optional.ofNullable().flatMap().filter() chain
        return Optional.ofNullable(portfolioValue)
            .filter(pv -> pv.compareTo(BigDecimal.ZERO) > 0)
            .flatMap(pv -> Optional.ofNullable(largestPositionValue)
                .map(lpv -> lpv.divide(pv, 4, BigDecimal.ROUND_HALF_UP).doubleValue()))
            .orElse(0.0);
    }
    
    /**
     * Check if approaching position limits
     */
    public boolean isApproachingPositionLimits() {
        return riskUtilization > 80.0;
    }
    
    /**
     * Check if day trading limits are approaching
     */
    public boolean isApproachingDayTradingLimits() {
        return dailyTradeCount > 20; // Approaching 25 trade limit
    }
    
    /**
     * Get available order capacity
     */
    public BigDecimal getAvailableOrderCapacity() {
        // Eliminates if-statement using Optional.ofNullable().flatMap() chain
        return Optional.ofNullable(maxOrderValue)
            .flatMap(maxOrder -> Optional.ofNullable(dailyVolume)
                .map(volume -> maxOrder.subtract(volume).max(BigDecimal.ZERO)))
            .orElse(BigDecimal.ZERO);
    }
}