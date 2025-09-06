package com.trademaster.trading.dto;

import com.trademaster.trading.model.PositionSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Position Summary DTO
 * 
 * Aggregated view of positions for portfolio management and reporting.
 * Provides summarized position data grouped by various criteria.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionSummary {
    
    /**
     * Group identifier (symbol, sector, strategy, etc.)
     */
    private String groupKey;
    
    /**
     * Group description
     */
    private String groupDescription;
    
    /**
     * Total number of positions in this group
     */
    private Integer positionCount;
    
    /**
     * Net position quantity (long - short)
     */
    private Integer netQuantity;
    
    /**
     * Long position quantity
     */
    private Integer longQuantity;
    
    /**
     * Short position quantity
     */
    private Integer shortQuantity;
    
    /**
     * Net position side based on net quantity
     */
    private PositionSide netSide;
    
    /**
     * Total market value of positions
     */
    private BigDecimal totalMarketValue;
    
    /**
     * Average entry price (weighted by quantity)
     */
    private BigDecimal averageEntryPrice;
    
    /**
     * Current weighted average price
     */
    private BigDecimal currentPrice;
    
    /**
     * Total unrealized P&L
     */
    private BigDecimal unrealizedPnL;
    
    /**
     * Unrealized P&L percentage
     */
    private BigDecimal unrealizedPnLPercent;
    
    /**
     * Total realized P&L (from closed positions)
     */
    private BigDecimal realizedPnL;
    
    /**
     * Total P&L (realized + unrealized)
     */
    private BigDecimal totalPnL;
    
    /**
     * Total P&L percentage
     */
    private BigDecimal totalPnLPercent;
    
    /**
     * Daily P&L change
     */
    private BigDecimal dailyPnL;
    
    /**
     * Daily P&L change percentage
     */
    private BigDecimal dailyPnLPercent;
    
    /**
     * Position allocation as percentage of portfolio
     */
    private BigDecimal allocationPercent;
    
    /**
     * Maximum position value achieved
     */
    private BigDecimal maxPositionValue;
    
    /**
     * Minimum position value achieved
     */
    private BigDecimal minPositionValue;
    
    /**
     * Position-weighted beta (for risk calculations)
     */
    private BigDecimal weightedBeta;
    
    /**
     * Value at Risk (VaR) for these positions
     */
    private BigDecimal valueAtRisk;
    
    /**
     * Position volatility
     */
    private BigDecimal volatility;
    
    /**
     * Last update timestamp
     */
    private Instant lastUpdated;
    
    /**
     * Individual positions in this group
     */
    private List<PositionDetail> positions;
    
    /**
     * Position detail for individual holdings
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionDetail {
        private String symbol;
        private PositionSide side;
        private Integer quantity;
        private BigDecimal entryPrice;
        private BigDecimal currentPrice;
        private BigDecimal marketValue;
        private BigDecimal unrealizedPnL;
        private BigDecimal unrealizedPnLPercent;
        private BigDecimal allocationPercent;
        private Instant openedAt;
        private Instant lastUpdated;
    }
    
    /**
     * Calculate position metrics
     */
    public void calculateMetrics() {
        if (longQuantity != null && shortQuantity != null) {
            netQuantity = longQuantity - shortQuantity;
            netSide = netQuantity >= 0 ? PositionSide.LONG : PositionSide.SHORT;
        }
        
        if (unrealizedPnL != null && totalMarketValue != null && totalMarketValue.compareTo(BigDecimal.ZERO) > 0) {
            unrealizedPnLPercent = unrealizedPnL.divide(totalMarketValue, 4, java.math.RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100));
        }
        
        if (totalPnL != null && totalMarketValue != null && totalMarketValue.compareTo(BigDecimal.ZERO) > 0) {
            totalPnLPercent = totalPnL.divide(totalMarketValue, 4, java.math.RoundingMode.HALF_UP)
                                     .multiply(BigDecimal.valueOf(100));
        }
        
        if (dailyPnL != null && totalMarketValue != null && totalMarketValue.compareTo(BigDecimal.ZERO) > 0) {
            dailyPnLPercent = dailyPnL.divide(totalMarketValue, 4, java.math.RoundingMode.HALF_UP)
                                     .multiply(BigDecimal.valueOf(100));
        }
    }
    
    /**
     * Check if positions are profitable
     */
    public boolean isProfitable() {
        return totalPnL != null && totalPnL.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if positions are at risk (large unrealized loss)
     */
    public boolean isAtRisk() {
        return unrealizedPnLPercent != null && 
               unrealizedPnLPercent.compareTo(new BigDecimal("-10.0")) < 0;
    }
    
    /**
     * Get position concentration risk level
     */
    public String getConcentrationRisk() {
        if (allocationPercent == null) return "UNKNOWN";
        
        if (allocationPercent.compareTo(new BigDecimal("20.0")) > 0) {
            return "HIGH";
        } else if (allocationPercent.compareTo(new BigDecimal("10.0")) > 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Get position status based on P&L and risk metrics
     */
    public String getPositionStatus() {
        if (isAtRisk()) {
            return "AT_RISK";
        } else if (isProfitable()) {
            return "PROFITABLE";
        } else if (totalPnL != null && totalPnL.compareTo(BigDecimal.ZERO) < 0) {
            return "LOSING";
        } else {
            return "NEUTRAL";
        }
    }
}