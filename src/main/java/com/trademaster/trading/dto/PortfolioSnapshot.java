package com.trademaster.trading.dto;

import com.trademaster.trading.entity.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Portfolio Snapshot DTO
 * 
 * Comprehensive real-time portfolio state with:
 * - Current positions and valuations
 * - P&L analysis with attribution
 * - Risk metrics and exposure analysis
 * - Performance benchmarking
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSnapshot {
    
    /**
     * Basic Portfolio Information
     */
    private Long userId;
    private Instant snapshotTime;
    private LocalDate snapshotDate;
    private String portfolioId;
    private String portfolioName;
    
    /**
     * Portfolio Valuation
     */
    private BigDecimal totalValue; // Total portfolio market value
    private BigDecimal cashBalance; // Available cash
    private BigDecimal investedValue; // Value of invested positions
    private BigDecimal marginUsed; // Margin currently used
    private BigDecimal buyingPower; // Available buying power
    private BigDecimal totalEquity; // Total account equity
    private BigDecimal netLiquidationValue; // Net liquidation value
    
    /**
     * P&L Analysis
     */
    private BigDecimal totalPnL; // Total realized + unrealized P&L
    private BigDecimal realizedPnL; // Realized P&L from closed positions
    private BigDecimal unrealizedPnL; // Unrealized P&L from open positions
    private BigDecimal intradayPnL; // Intraday P&L change
    private BigDecimal dayChange; // Change since previous close
    private BigDecimal dayChangePercent; // Day change percentage
    
    /**
     * Performance Metrics
     */
    private BigDecimal totalReturnPercent; // Total return percentage
    private BigDecimal annualizedReturn; // Annualized return
    private BigDecimal monthToDateReturn; // MTD return
    private BigDecimal yearToDateReturn; // YTD return
    
    /**
     * Risk Metrics
     */
    private BigDecimal portfolioVaR; // Value at Risk (95% confidence)
    private BigDecimal portfolioBeta; // Beta to market benchmark
    private BigDecimal portfolioVolatility; // Portfolio volatility
    private BigDecimal sharpeRatio; // Risk-adjusted return
    private BigDecimal maxDrawdown; // Maximum drawdown
    
    /**
     * Position Details
     */
    private Integer totalPositions; // Number of positions
    private Integer longPositions; // Number of long positions
    private Integer shortPositions; // Number of short positions
    private List<Position> positions; // All current positions
    
    /**
     * Portfolio Health Indicators
     */
    private BigDecimal healthScore; // Overall portfolio health (0-100)
    private List<String> recommendations; // Portfolio recommendations
    
    /**
     * Market Data Context
     */
    private String marketStatus; // OPEN, CLOSED, PRE_MARKET, AFTER_HOURS
    private Instant lastMarketDataUpdate; // Last market data timestamp
    
    /**
     * Calculated Properties and Helper Methods
     */
    
    /**
     * Calculate portfolio allocation percentage for a position
     */
    public BigDecimal getPositionWeight(Position position) {
        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) == 0 ||
            position.getMarketValue() == null) {
            return BigDecimal.ZERO;
        }
        return position.getMarketValue().divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Get cash allocation percentage
     */
    public BigDecimal getCashAllocationPercent() {
        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) == 0 ||
            cashBalance == null) {
            return BigDecimal.ZERO;
        }
        return cashBalance.divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Get invested allocation percentage
     */
    public BigDecimal getInvestedAllocationPercent() {
        if (totalValue == null || totalValue.compareTo(BigDecimal.ZERO) == 0 ||
            investedValue == null) {
            return BigDecimal.ZERO;
        }
        return investedValue.divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Get performance category based on returns
     */
    public String getPerformanceCategory() {
        if (totalReturnPercent == null) {
            return "UNKNOWN";
        }
        
        BigDecimal returnPct = totalReturnPercent;
        if (returnPct.compareTo(new BigDecimal("20")) > 0) {
            return "EXCELLENT";
        } else if (returnPct.compareTo(new BigDecimal("10")) > 0) {
            return "GOOD";
        } else if (returnPct.compareTo(new BigDecimal("0")) >= 0) {
            return "POSITIVE";
        } else if (returnPct.compareTo(new BigDecimal("-10")) > 0) {
            return "NEGATIVE";
        } else {
            return "POOR";
        }
    }
    
    /**
     * Get risk category based on risk metrics
     */
    public String getRiskCategory() {
        if (portfolioVaR == null || totalValue == null || totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return "UNKNOWN";
        }
        
        // VaR as percentage of portfolio value
        BigDecimal varPercent = portfolioVaR.divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
                                            .multiply(BigDecimal.valueOf(100));
        
        if (varPercent.compareTo(new BigDecimal("2")) <= 0) {
            return "LOW";
        } else if (varPercent.compareTo(new BigDecimal("5")) <= 0) {
            return "MODERATE";
        } else if (varPercent.compareTo(new BigDecimal("10")) <= 0) {
            return "HIGH";
        } else {
            return "VERY_HIGH";
        }
    }
    
    /**
     * Check if portfolio needs rebalancing
     */
    public boolean needsRebalancing() {
        // Simple rebalancing check based on concentration
        return totalPositions != null && totalPositions > 10;
    }
    
    /**
     * Get portfolio summary statistics
     */
    public Map<String, Object> getSummaryStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalValue", totalValue != null ? totalValue : BigDecimal.ZERO);
        stats.put("totalPnL", totalPnL != null ? totalPnL : BigDecimal.ZERO);
        stats.put("dayChange", dayChange != null ? dayChange : BigDecimal.ZERO);
        stats.put("dayChangePercent", dayChangePercent != null ? dayChangePercent : BigDecimal.ZERO);
        stats.put("totalPositions", totalPositions != null ? totalPositions : 0);
        stats.put("performanceCategory", getPerformanceCategory());
        stats.put("riskCategory", getRiskCategory());
        stats.put("healthScore", healthScore != null ? healthScore : BigDecimal.ZERO);
        stats.put("needsRebalancing", needsRebalancing());
        return java.util.Collections.unmodifiableMap(stats);
    }
    
    /**
     * Static factory methods
     */
    public static PortfolioSnapshot empty(Long userId) {
        return PortfolioSnapshot.builder()
            .userId(userId)
            .snapshotTime(Instant.now())
            .snapshotDate(LocalDate.now())
            .totalValue(BigDecimal.ZERO)
            .cashBalance(BigDecimal.ZERO)
            .totalPositions(0)
            .healthScore(new BigDecimal("50"))
            .build();
    }
    
    /**
     * Create snapshot with error state
     */
    public static PortfolioSnapshot error(Long userId, String errorMessage) {
        return PortfolioSnapshot.builder()
            .userId(userId)
            .snapshotTime(Instant.now())
            .snapshotDate(LocalDate.now())
            .totalValue(BigDecimal.ZERO)
            .healthScore(BigDecimal.ZERO)
            .recommendations(List.of(errorMessage))
            .build();
    }
}