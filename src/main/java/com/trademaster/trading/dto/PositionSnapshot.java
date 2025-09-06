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
 * Position Snapshot DTO
 * 
 * Comprehensive real-time position snapshot with:
 * - Current position details and valuation
 * - Real-time P&L with detailed breakdown
 * - Cost basis and tax lot information
 * - Risk metrics and concentration analysis
 * - Performance attribution and analytics
 * - Corporate action adjustments tracking
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionSnapshot {
    
    /**
     * Position Identification
     */
    private Long userId;
    private String symbol;
    private String exchange;
    private String assetClass; // EQUITY, DERIVATIVE, COMMODITY, CURRENCY, BOND
    private String sector;
    private String industry;
    private String currency;
    private Instant snapshotTime;
    
    /**
     * Position Basics
     */
    private PositionDetails positionDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionDetails {
        private Integer totalQuantity; // Current position quantity
        private Integer longQuantity; // Long position quantity
        private Integer shortQuantity; // Short position quantity
        private Integer availableQuantity; // Available for trading
        private Integer pledgedQuantity; // Pledged as collateral
        private Integer blockedQuantity; // Blocked for orders
        private BigDecimal averagePrice; // Average cost price
        private BigDecimal marketPrice; // Current market price
        private BigDecimal marketValue; // Current market value
        private BigDecimal costValue; // Total cost value
        private String positionType; // LONG, SHORT, FLAT
        private LocalDate firstTradeDate; // First trade date
        private LocalDate lastTradeDate; // Last trade date
    }
    
    /**
     * Real-time P&L Analysis
     */
    private PnLBreakdown pnlBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PnLBreakdown {
        private BigDecimal unrealizedPnL; // Current unrealized P&L
        private BigDecimal realizedPnL; // Realized P&L to date
        private BigDecimal totalPnL; // Total P&L
        private BigDecimal intradayPnL; // Intraday P&L change
        private BigDecimal percentReturn; // Percentage return
        private BigDecimal absoluteReturn; // Absolute return
        private BigDecimal dayChange; // Day change amount
        private BigDecimal dayChangePercent; // Day change percentage
        private BigDecimal costBasis; // Cost basis amount
        private BigDecimal marketValue; // Current market value
        private String performanceCategory; // EXCELLENT, GOOD, POOR, LOSS
    }
    
    /**
     * Cost Basis Information
     */
    private CostBasisDetails costBasis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostBasisDetails {
        private String method; // FIFO, LIFO, AVERAGE, SPECIFIC
        private BigDecimal averageCostPrice; // Average cost per share
        private BigDecimal totalCostBasis; // Total cost basis
        private BigDecimal adjustedCostBasis; // Cost basis after adjustments
        private Integer taxLotsCount; // Number of tax lots
        private List<TaxLot> taxLots; // Individual tax lots
        private BigDecimal shortTermBasis; // Short-term cost basis
        private BigDecimal longTermBasis; // Long-term cost basis
        private BigDecimal washSaleAdjustment; // Wash sale adjustments
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxLot {
        private String lotId;
        private LocalDate purchaseDate;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal costBasis;
        private String term; // SHORT_TERM, LONG_TERM
        private Boolean washSale;
        private String costBasisMethod;
        private Instant createdAt;
    }
    
    /**
     * Risk Metrics
     */
    private RiskMetrics riskMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskMetrics {
        private BigDecimal positionVaR; // Position Value at Risk
        private BigDecimal beta; // Beta to market
        private BigDecimal volatility; // Position volatility
        private BigDecimal maxDrawdown; // Maximum drawdown
        private BigDecimal sharpeRatio; // Risk-adjusted return
        private BigDecimal concentrationRisk; // Concentration risk score
        private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private BigDecimal riskScore; // Overall risk score 0-100
        private BigDecimal correlationToMarket; // Correlation to market index
        private BigDecimal trackingError; // Tracking error
    }
    
    /**
     * Greeks (for derivatives)
     */
    private GreeksData greeks;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GreeksData {
        private BigDecimal delta; // Price sensitivity
        private BigDecimal gamma; // Delta sensitivity
        private BigDecimal theta; // Time decay
        private BigDecimal vega; // Volatility sensitivity
        private BigDecimal rho; // Interest rate sensitivity
        private BigDecimal impliedVolatility; // Implied volatility
        private BigDecimal timeToExpiry; // Days to expiry
        private String optionType; // CALL, PUT
        private BigDecimal strikePrice; // Strike price
        private LocalDate expiryDate; // Expiry date
    }
    
    /**
     * Performance Metrics
     */
    private PerformanceMetrics performance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private BigDecimal totalReturn; // Total return percentage
        private BigDecimal annualizedReturn; // Annualized return
        private BigDecimal monthToDateReturn; // MTD return
        private BigDecimal yearToDateReturn; // YTD return
        private BigDecimal oneMonthReturn; // 1-month return
        private BigDecimal threeMonthReturn; // 3-month return
        private BigDecimal oneYearReturn; // 1-year return
        private BigDecimal bestDay; // Best single day return
        private BigDecimal worstDay; // Worst single day return
        private Integer winningDays; // Number of winning days
        private Integer losingDays; // Number of losing days
        private BigDecimal winRate; // Win rate percentage
        private String performanceTrend; // IMPROVING, STABLE, DECLINING
    }
    
    /**
     * Margin and Collateral
     */
    private MarginDetails marginDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarginDetails {
        private BigDecimal marginRequirement; // Margin requirement
        private BigDecimal maintenanceMargin; // Maintenance margin
        private BigDecimal excessMargin; // Excess margin available
        private BigDecimal marginUtilization; // Margin utilization %
        private BigDecimal collateralValue; // Collateral value
        private BigDecimal haircut; // Haircut percentage
        private String marginStatus; // SUFFICIENT, WARNING, DEFICIENT
        private BigDecimal leverageRatio; // Leverage ratio
        private Boolean marginCall; // Margin call status
    }
    
    /**
     * Corporate Actions
     */
    private List<CorporateAction> corporateActions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorporateAction {
        private String actionId;
        private String actionType; // DIVIDEND, SPLIT, MERGER, SPINOFF
        private LocalDate announcementDate;
        private LocalDate exDate;
        private LocalDate recordDate;
        private LocalDate paymentDate;
        private BigDecimal rate; // Split ratio, dividend amount, etc.
        private String status; // PENDING, PROCESSED, CANCELLED
        private Integer quantityAffected;
        private BigDecimal valueImpact;
        private String description;
    }
    
    /**
     * Trading Activity
     */
    private TradingActivity tradingActivity;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradingActivity {
        private Integer totalTrades; // Total number of trades
        private Integer buyTrades; // Number of buy trades
        private Integer sellTrades; // Number of sell trades
        private Long totalVolume; // Total volume traded
        private BigDecimal averageTradeSize; // Average trade size
        private BigDecimal turnoverRate; // Position turnover rate
        private Integer daysHeld; // Average holding period
        private LocalDate lastTradeDate; // Last trade date
        private BigDecimal tradingCosts; // Total trading costs
        private BigDecimal costPerShare; // Average cost per share
    }
    
    /**
     * Market Context
     */
    private MarketContext marketContext;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketContext {
        private BigDecimal currentPrice; // Current market price
        private BigDecimal bidPrice; // Current bid price
        private BigDecimal askPrice; // Current ask price
        private BigDecimal spread; // Bid-ask spread
        private Long volume; // Current volume
        private BigDecimal dayHigh; // Day high price
        private BigDecimal dayLow; // Day low price
        private BigDecimal weekHigh; // Week high price
        private BigDecimal weekLow; // Week low price
        private String marketStatus; // OPEN, CLOSED, HALTED
        private Instant lastUpdate; // Last market data update
        private String liquidityCondition; // HIGH, MEDIUM, LOW
    }
    
    /**
     * Position Alerts and Notifications
     */
    private List<PositionAlert> activeAlerts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionAlert {
        private String alertId;
        private String alertType; // PRICE, PNL, RISK, MARGIN, CONCENTRATION
        private String condition; // ABOVE, BELOW, EQUAL
        private BigDecimal threshold; // Alert threshold
        private BigDecimal currentValue; // Current value
        private String severity; // INFO, WARNING, CRITICAL
        private String status; // ACTIVE, TRIGGERED, ACKNOWLEDGED
        private Instant createdAt;
        private Instant triggeredAt;
        private String message;
    }
    
    /**
     * Compliance and Regulatory
     */
    private ComplianceInfo complianceInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceInfo {
        private List<String> regulatoryFlags; // Regulatory flags
        private Boolean insiderPosition; // Insider trading position
        private BigDecimal positionLimit; // Position limit if applicable
        private BigDecimal utilizationPercent; // Limit utilization
        private List<String> restrictions; // Trading restrictions
        private String complianceStatus; // COMPLIANT, WARNING, VIOLATION
        private LocalDate lastComplianceCheck; // Last compliance check
        private List<String> pendingReports; // Pending regulatory reports
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if position is profitable
     */
    public boolean isProfitable() {
        return pnlBreakdown != null && 
               pnlBreakdown.getTotalPnL() != null &&
               pnlBreakdown.getTotalPnL().compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if position is at risk
     */
    public boolean isAtRisk() {
        return riskMetrics != null && 
               ("HIGH".equals(riskMetrics.getRiskLevel()) || 
                "CRITICAL".equals(riskMetrics.getRiskLevel()));
    }
    
    /**
     * Get position size as percentage of portfolio
     */
    public BigDecimal getPortfolioWeight(BigDecimal totalPortfolioValue) {
        if (positionDetails == null || positionDetails.getMarketValue() == null ||
            totalPortfolioValue == null || totalPortfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return positionDetails.getMarketValue()
               .divide(totalPortfolioValue, 4, java.math.RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Check if position has margin call
     */
    public boolean hasMarginCall() {
        return marginDetails != null && 
               Boolean.TRUE.equals(marginDetails.getMarginCall());
    }
    
    /**
     * Get holding period in days
     */
    public Integer getHoldingPeriodDays() {
        if (positionDetails == null || positionDetails.getFirstTradeDate() == null) {
            return 0;
        }
        LocalDate firstTrade = positionDetails.getFirstTradeDate();
        return (int) java.time.temporal.ChronoUnit.DAYS.between(firstTrade, LocalDate.now());
    }
    
    /**
     * Check if position qualifies for long-term capital gains
     */
    public boolean isLongTermPosition() {
        return getHoldingPeriodDays() > 365; // More than 1 year
    }
    
    /**
     * Get position concentration score
     */
    public BigDecimal getConcentrationScore() {
        if (riskMetrics != null && riskMetrics.getConcentrationRisk() != null) {
            return riskMetrics.getConcentrationRisk();
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Check if position has active alerts
     */
    public boolean hasActiveAlerts() {
        return activeAlerts != null && !activeAlerts.isEmpty() &&
               activeAlerts.stream().anyMatch(alert -> "ACTIVE".equals(alert.getStatus()));
    }
    
    /**
     * Get critical alerts only
     */
    public List<PositionAlert> getCriticalAlerts() {
        if (activeAlerts == null) return List.of();
        return activeAlerts.stream()
               .filter(alert -> "CRITICAL".equals(alert.getSeverity()))
               .toList();
    }
    
    /**
     * Calculate break-even price
     */
    public BigDecimal getBreakEvenPrice() {
        if (costBasis == null || costBasis.getAverageCostPrice() == null ||
            tradingActivity == null || tradingActivity.getTradingCosts() == null ||
            positionDetails == null || positionDetails.getTotalQuantity() == null ||
            positionDetails.getTotalQuantity() == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalCost = costBasis.getTotalCostBasis().add(tradingActivity.getTradingCosts());
        return totalCost.divide(BigDecimal.valueOf(positionDetails.getTotalQuantity()), 
                               4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Get position summary for reporting
     */
    public Map<String, Object> getPositionSummary() {
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("symbol", symbol != null ? symbol : "N/A");
        summary.put("quantity", positionDetails != null && positionDetails.getTotalQuantity() != null ? 
                   positionDetails.getTotalQuantity() : 0);
        summary.put("marketValue", positionDetails != null && positionDetails.getMarketValue() != null ? 
                  positionDetails.getMarketValue() : BigDecimal.ZERO);
        summary.put("unrealizedPnL", pnlBreakdown != null && pnlBreakdown.getUnrealizedPnL() != null ? 
                    pnlBreakdown.getUnrealizedPnL() : BigDecimal.ZERO);
        summary.put("percentReturn", pnlBreakdown != null && pnlBreakdown.getPercentReturn() != null ? 
                    pnlBreakdown.getPercentReturn() : BigDecimal.ZERO);
        summary.put("riskLevel", riskMetrics != null && riskMetrics.getRiskLevel() != null ? 
                riskMetrics.getRiskLevel() : "UNKNOWN");
        summary.put("isProfitable", isProfitable());
        summary.put("isAtRisk", isAtRisk());
        summary.put("hasMarginCall", hasMarginCall());
        summary.put("holdingDays", getHoldingPeriodDays());
        summary.put("snapshotTime", snapshotTime != null ? snapshotTime : Instant.EPOCH);
        return java.util.Collections.unmodifiableMap(summary);
    }
    
    /**
     * Static factory methods
     */
    public static PositionSnapshot empty(Long userId, String symbol) {
        return PositionSnapshot.builder()
            .userId(userId)
            .symbol(symbol)
            .snapshotTime(Instant.now())
            .positionDetails(PositionDetails.builder()
                .totalQuantity(0)
                .marketValue(BigDecimal.ZERO)
                .averagePrice(BigDecimal.ZERO)
                .positionType("FLAT")
                .build())
            .pnlBreakdown(PnLBreakdown.builder()
                .unrealizedPnL(BigDecimal.ZERO)
                .realizedPnL(BigDecimal.ZERO)
                .totalPnL(BigDecimal.ZERO)
                .performanceCategory("FLAT")
                .build())
            .riskMetrics(RiskMetrics.builder()
                .riskLevel("LOW")
                .riskScore(BigDecimal.ZERO)
                .build())
            .build();
    }
}