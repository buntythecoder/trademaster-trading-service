package com.trademaster.trading.dto;

import com.trademaster.trading.model.PositionSide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
     * Calculate position metrics - eliminates if-statements with Optional
     */
    public void calculateMetrics() {
        // Calculate net quantity and side - eliminates if-statement and ternary with Optional
        Optional.ofNullable(longQuantity)
            .flatMap(longQty -> Optional.ofNullable(shortQuantity)
                .map(shortQty -> {
                    netQuantity = longQty - shortQty;
                    netSide = Optional.of(netQuantity >= 0)
                        .filter(Boolean::booleanValue)
                        .map(isPositive -> PositionSide.LONG)
                        .orElse(PositionSide.SHORT);
                    return true;
                }));

        // Calculate unrealized P&L percentage - eliminates if-statement
        Optional.ofNullable(unrealizedPnL)
            .flatMap(pnl -> Optional.ofNullable(totalMarketValue)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .map(value -> {
                    unrealizedPnLPercent = pnl.divide(value, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    return true;
                }));

        // Calculate total P&L percentage - eliminates if-statement
        Optional.ofNullable(totalPnL)
            .flatMap(pnl -> Optional.ofNullable(totalMarketValue)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .map(value -> {
                    totalPnLPercent = pnl.divide(value, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    return true;
                }));

        // Calculate daily P&L percentage - eliminates if-statement
        Optional.ofNullable(dailyPnL)
            .flatMap(pnl -> Optional.ofNullable(totalMarketValue)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .map(value -> {
                    dailyPnLPercent = pnl.divide(value, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    return true;
                }));
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
     * Get position concentration risk level - eliminates if-else chain with Stream pattern
     */
    public String getConcentrationRisk() {
        return Optional.ofNullable(allocationPercent)
            .map(allocation -> {
                record ConcentrationThreshold(BigDecimal minAllocation, String riskLevel) {}

                return Stream.of(
                    new ConcentrationThreshold(new BigDecimal("20.0"), "HIGH"),
                    new ConcentrationThreshold(new BigDecimal("10.0"), "MEDIUM")
                )
                .filter(threshold -> allocation.compareTo(threshold.minAllocation()) > 0)
                .findFirst()
                .map(ConcentrationThreshold::riskLevel)
                .orElse("LOW");
            })
            .orElse("UNKNOWN");
    }
    
    /**
     * Get position status based on P&L and risk metrics - eliminates if-else chain with Stream pattern
     */
    public String getPositionStatus() {
        record StatusCondition(java.util.function.Supplier<Boolean> condition, String status) {}

        return Stream.of(
            new StatusCondition(this::isAtRisk, "AT_RISK"),
            new StatusCondition(this::isProfitable, "PROFITABLE"),
            new StatusCondition(() -> totalPnL != null && totalPnL.compareTo(BigDecimal.ZERO) < 0, "LOSING")
        )
        .filter(statusCondition -> statusCondition.condition().get())
        .findFirst()
        .map(StatusCondition::status)
        .orElse("NEUTRAL");
    }
}