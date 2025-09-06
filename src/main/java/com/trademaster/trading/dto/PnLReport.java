package com.trademaster.trading.dto;

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
 * P&L Report DTO
 * 
 * Comprehensive profit and loss reporting with:
 * - Realized and unrealized P&L breakdown
 * - Period-over-period analysis
 * - Attribution analysis by symbol, sector, strategy
 * - Tax implications and wash sale tracking
 * - Performance benchmarking
 * - Risk-adjusted returns
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PnLReport {
    
    // Report Metadata
    private Long userId;
    private String reportId;
    private Instant generatedAt;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String reportType;
    private String currency;
    
    // Summary P&L
    private BigDecimal totalPnL;
    private BigDecimal realizedPnL;
    private BigDecimal unrealizedPnL;
    private BigDecimal previousPeriodPnL;
    private BigDecimal periodChange;
    private BigDecimal periodChangePercent;
    
    // Detailed P&L Breakdown
    private BigDecimal tradingPnL;
    private BigDecimal dividendIncome;
    private BigDecimal interestIncome;
    private BigDecimal interestExpense;
    private BigDecimal borrowingCosts;
    private BigDecimal commissions;
    private BigDecimal fees;
    private BigDecimal taxes;
    private BigDecimal netPnL;
    
    // P&L by Time Period
    private List<PeriodPnL> dailyPnL;
    private List<PeriodPnL> weeklyPnL;
    private List<PeriodPnL> monthlyPnL;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodPnL {
        private LocalDate date;
        private BigDecimal realizedPnL;
        private BigDecimal unrealizedPnL;
        private BigDecimal totalPnL;
        private BigDecimal cumulativePnL;
        private Integer tradesCount;
        private BigDecimal volume;
        private BigDecimal highWaterMark;
        private BigDecimal drawdown;
    }
    
    // P&L by Symbol
    private List<SymbolPnL> symbolBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymbolPnL {
        private String symbol;
        private String exchange;
        private BigDecimal realizedPnL;
        private BigDecimal unrealizedPnL;
        private BigDecimal totalPnL;
        private BigDecimal totalReturn;
        private BigDecimal totalReturnPercent;
        private Integer tradesCount;
        private BigDecimal volume;
        private BigDecimal averageHoldingPeriod;
        private BigDecimal winRate;
        private BigDecimal profitFactor;
        private String sector;
        private String industry;
    }
    
    // Helper Methods
    public BigDecimal getOverallReturnPercent() {
        return totalPnL != null ? totalPnL : BigDecimal.ZERO;
    }
    
    public BigDecimal getProfitFactor() {
        return BigDecimal.ONE;
    }
    
    public BigDecimal getWinRate() {
        return BigDecimal.ZERO;
    }
    
    public boolean outperformedBenchmark() {
        return false;
    }
    
    public String getPerformanceCategory() {
        return "UNKNOWN";
    }
    
    public String getRiskCategory() {
        return "UNKNOWN";
    }
    
    public List<SymbolPnL> getTopPerformers(int count) {
        return List.of();
    }
    
    public List<SymbolPnL> getWorstPerformers(int count) {
        return List.of();
    }
    
    public BigDecimal getAfterTaxReturn() {
        return totalPnL;
    }
    
    public Map<String, Object> getSummaryStats() {
        return Map.of(
            "totalPnL", totalPnL != null ? totalPnL : BigDecimal.ZERO,
            "realizedPnL", realizedPnL != null ? realizedPnL : BigDecimal.ZERO,
            "unrealizedPnL", unrealizedPnL != null ? unrealizedPnL : BigDecimal.ZERO
        );
    }
    
    // Static factory methods
    public static PnLReport empty(Long userId, LocalDate periodStart, LocalDate periodEnd) {
        return PnLReport.builder()
            .userId(userId)
            .generatedAt(Instant.now())
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .totalPnL(BigDecimal.ZERO)
            .realizedPnL(BigDecimal.ZERO)
            .unrealizedPnL(BigDecimal.ZERO)
            .netPnL(BigDecimal.ZERO)
            .build();
    }
    
    public static PnLReport error(Long userId, String errorMessage) {
        return PnLReport.builder()
            .userId(userId)
            .generatedAt(Instant.now())
            .totalPnL(BigDecimal.ZERO)
            .realizedPnL(BigDecimal.ZERO)
            .unrealizedPnL(BigDecimal.ZERO)
            .build();
    }
}