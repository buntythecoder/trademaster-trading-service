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
import java.util.Optional;
import java.util.stream.Stream;

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
     * Calculate portfolio allocation percentage for a position - eliminates if-statement with Optional
     */
    public BigDecimal getPositionWeight(Position position) {
        return Optional.ofNullable(totalValue)
            .filter(value -> value.compareTo(BigDecimal.ZERO) != 0)
            .flatMap(value -> Optional.ofNullable(position.getMarketValue())
                .map(marketValue -> marketValue.divide(value, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Get cash allocation percentage - eliminates if-statement with Optional
     */
    public BigDecimal getCashAllocationPercent() {
        return Optional.ofNullable(totalValue)
            .filter(value -> value.compareTo(BigDecimal.ZERO) != 0)
            .flatMap(value -> Optional.ofNullable(cashBalance)
                .map(cash -> cash.divide(value, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Get invested allocation percentage - eliminates if-statement with Optional
     */
    public BigDecimal getInvestedAllocationPercent() {
        return Optional.ofNullable(totalValue)
            .filter(value -> value.compareTo(BigDecimal.ZERO) != 0)
            .flatMap(value -> Optional.ofNullable(investedValue)
                .map(invested -> invested.divide(value, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Get performance category based on returns - eliminates if-else chain with Stream pattern
     */
    public String getPerformanceCategory() {
        return Optional.ofNullable(totalReturnPercent)
            .map(returnPct -> {
                record PerformanceThreshold(BigDecimal minReturn, String category) {}

                return Stream.of(
                    new PerformanceThreshold(new BigDecimal("20"), "EXCELLENT"),
                    new PerformanceThreshold(new BigDecimal("10"), "GOOD"),
                    new PerformanceThreshold(new BigDecimal("0"), "POSITIVE"),
                    new PerformanceThreshold(new BigDecimal("-10"), "NEGATIVE")
                )
                .filter(threshold -> returnPct.compareTo(threshold.minReturn()) > 0)
                .findFirst()
                .map(PerformanceThreshold::category)
                .orElse("POOR");
            })
            .orElse("UNKNOWN");
    }
    
    /**
     * Get risk category based on risk metrics - eliminates if-else chain with Stream pattern
     */
    public String getRiskCategory() {
        return Optional.ofNullable(totalValue)
            .filter(value -> value.compareTo(BigDecimal.ZERO) != 0)
            .flatMap(value -> Optional.ofNullable(portfolioVaR)
                .map(var -> {
                    // VaR as percentage of portfolio value
                    BigDecimal varPercent = var.divide(value, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                    record RiskThreshold(BigDecimal maxVarPercent, String category) {}

                    return Stream.of(
                        new RiskThreshold(new BigDecimal("2"), "LOW"),
                        new RiskThreshold(new BigDecimal("5"), "MODERATE"),
                        new RiskThreshold(new BigDecimal("10"), "HIGH")
                    )
                    .filter(threshold -> varPercent.compareTo(threshold.maxVarPercent()) <= 0)
                    .findFirst()
                    .map(RiskThreshold::category)
                    .orElse("VERY_HIGH");
                }))
            .orElse("UNKNOWN");
    }
    
    /**
     * Check if portfolio needs rebalancing
     */
    public boolean needsRebalancing() {
        // Simple rebalancing check based on concentration
        return totalPositions != null && totalPositions > 10;
    }
    
    /**
     * Get portfolio summary statistics - eliminates ternary operators with Optional
     */
    public Map<String, Object> getSummaryStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalValue", Optional.ofNullable(totalValue).orElse(BigDecimal.ZERO));
        stats.put("totalPnL", Optional.ofNullable(totalPnL).orElse(BigDecimal.ZERO));
        stats.put("dayChange", Optional.ofNullable(dayChange).orElse(BigDecimal.ZERO));
        stats.put("dayChangePercent", Optional.ofNullable(dayChangePercent).orElse(BigDecimal.ZERO));
        stats.put("totalPositions", Optional.ofNullable(totalPositions).orElse(0));
        stats.put("performanceCategory", getPerformanceCategory());
        stats.put("riskCategory", getRiskCategory());
        stats.put("healthScore", Optional.ofNullable(healthScore).orElse(BigDecimal.ZERO));
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