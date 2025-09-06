package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Risk Limits DTO
 * 
 * Comprehensive risk limit configuration with:
 * - Position and concentration limits
 * - Portfolio-level risk constraints
 * - Dynamic limit adjustments
 * - Regulatory compliance limits
 * - Time-based and volatility-adjusted limits
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLimits {
    
    /**
     * Limit Configuration Metadata
     */
    private String limitId;
    private Long userId;
    private String profileType; // CONSERVATIVE, MODERATE, AGGRESSIVE, CUSTOM
    private Instant createdAt;
    private Instant lastUpdated;
    private String updatedBy;
    private Boolean active;
    
    /**
     * Position Limits
     */
    private PositionLimits positionLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionLimits {
        private BigDecimal maxSinglePositionValue; // Maximum position value
        private BigDecimal maxSinglePositionPercent; // Max % of portfolio
        private BigDecimal maxSectorConcentration; // Max sector concentration %
        private BigDecimal maxIndustryConcentration; // Max industry concentration %
        private Integer maxPositionsPerStock; // Max positions per symbol
        private Integer maxTotalPositions; // Max total open positions
        private BigDecimal maxLongExposure; // Max long exposure value
        private BigDecimal maxShortExposure; // Max short exposure value
        private BigDecimal maxNetExposure; // Max net exposure
        private BigDecimal maxGrossExposure; // Max gross exposure
    }
    
    /**
     * Leverage and Margin Limits
     */
    private LeverageLimits leverageLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeverageLimits {
        private BigDecimal maxLeverageRatio; // Maximum leverage ratio
        private BigDecimal maxMarginUtilization; // Max margin utilization %
        private BigDecimal minMaintenanceMargin; // Minimum maintenance margin
        private BigDecimal maxIntradayLeverage; // Intraday leverage limit
        private BigDecimal maxOvernightLeverage; // Overnight leverage limit
        private BigDecimal marginCallThreshold; // Margin call trigger
        private BigDecimal liquidationThreshold; // Forced liquidation trigger
        private Boolean allowMarginTrading; // Allow margin trading
    }
    
    /**
     * Trading Velocity Limits
     */
    private VelocityLimits velocityLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VelocityLimits {
        private BigDecimal maxDailyTradingValue; // Max daily trading value
        private Integer maxDailyOrders; // Max orders per day
        private Integer maxOrdersPerMinute; // Rate limiting
        private Integer maxOrdersPerHour; // Hourly limit
        private BigDecimal maxHourlyTradingValue; // Hourly trading value limit
        private BigDecimal dayTradingBuyingPower; // Day trading buying power
        private Integer maxDayTrades; // Max day trades per period
        private Integer dayTradeResetDays; // Day trade count reset period
    }
    
    /**
     * Portfolio Risk Limits
     */
    private PortfolioRiskLimits portfolioLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioRiskLimits {
        private BigDecimal maxVaR; // Maximum Value at Risk
        private BigDecimal maxExpectedShortfall; // Max Expected Shortfall
        private BigDecimal maxDrawdown; // Maximum drawdown limit
        private BigDecimal maxVolatility; // Portfolio volatility limit
        private BigDecimal minSharpeRatio; // Minimum Sharpe ratio
        private BigDecimal maxBeta; // Maximum portfolio beta
        private BigDecimal maxCorrelation; // Max correlation to benchmark
        private BigDecimal stopLossPercent; // Portfolio stop loss %
        private BigDecimal profitTargetPercent; // Portfolio profit target %
    }
    
    /**
     * Sector and Asset Class Limits
     */
    private List<SectorLimit> sectorLimits;
    private List<AssetClassLimit> assetClassLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorLimit {
        private String sectorName;
        private BigDecimal maxAllocationPercent;
        private BigDecimal maxPositionValue;
        private Integer maxPositions;
        private BigDecimal currentAllocation;
        private BigDecimal utilizationPercent;
        private Boolean breached;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetClassLimit {
        private String assetClass; // EQUITY, DEBT, COMMODITY, CURRENCY
        private BigDecimal maxAllocationPercent;
        private BigDecimal minAllocationPercent;
        private BigDecimal maxPositionValue;
        private BigDecimal currentAllocation;
        private Boolean withinLimits;
    }
    
    /**
     * Regulatory Limits
     */
    private RegulatoryLimits regulatoryLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryLimits {
        private BigDecimal sebiPositionLimit; // SEBI position limits
        private BigDecimal fnoPositionLimit; // F&O position limits
        private BigDecimal intradayExposureLimit; // Intraday exposure limit
        private Boolean pdrCompliant; // Promoter/Director/Relative rules
        private BigDecimal insiderTradingLimit; // Insider trading limits
        private Map<String, BigDecimal> exchangeLimits; // Exchange-specific limits
        private List<String> restrictedSymbols; // Trading restricted symbols
    }
    
    /**
     * Dynamic Risk Adjustments
     */
    private DynamicAdjustments dynamicAdjustments;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicAdjustments {
        private Boolean enableVolatilityAdjustment; // Adjust for volatility
        private Boolean enableMarketRegimeAdjustment; // Adjust for market regime
        private Boolean enableLiquidityAdjustment; // Adjust for liquidity
        private BigDecimal volatilityMultiplier; // Volatility adjustment factor
        private Map<String, BigDecimal> marketRegimeMultipliers; // Regime multipliers
        private BigDecimal lowLiquidityMultiplier; // Low liquidity multiplier
        private Boolean enableTimeBasedLimits; // Time-based limit adjustments
    }
    
    /**
     * Time-Based Limits
     */
    private TimeBasedLimits timeBasedLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeBasedLimits {
        private LocalTime marketOpenReduction; // Reduce limits at market open
        private LocalTime marketCloseReduction; // Reduce limits near close
        private BigDecimal openingLimitMultiplier; // Opening limit multiplier
        private BigDecimal closingLimitMultiplier; // Closing limit multiplier
        private Map<String, BigDecimal> weekdayMultipliers; // Day-specific limits
        private Boolean enablePreMarketLimits; // Pre-market trading limits
        private Boolean enableAfterHoursLimits; // After-hours trading limits
    }
    
    /**
     * Alert and Notification Thresholds
     */
    private AlertThresholds alertThresholds;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertThresholds {
        private BigDecimal warningThresholdPercent; // Warning at % of limit
        private BigDecimal criticalThresholdPercent; // Critical alert threshold
        private Boolean enableRealTimeAlerts; // Real-time alert notifications
        private Boolean enableEmailAlerts; // Email notifications
        private Boolean enableSmsAlerts; // SMS notifications
        private List<String> alertContacts; // Alert contact list
        private Integer alertFrequencyMinutes; // Alert frequency limits
    }
    
    /**
     * Stress Test Limits
     */
    private StressTestLimits stressTestLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StressTestLimits {
        private BigDecimal maxStressTestLoss; // Max loss in stress scenarios
        private BigDecimal minStressTestCapital; // Min capital after stress
        private List<String> mandatoryScenarios; // Required stress scenarios
        private BigDecimal tailRiskLimit; // Tail risk exposure limit
        private BigDecimal blackSwanReserve; // Black swan event reserve
    }
    
    /**
     * Custom Limits
     */
    private List<CustomLimit> customLimits;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomLimit {
        private String limitName;
        private String limitType; // VALUE, PERCENTAGE, COUNT, RATIO
        private BigDecimal limitValue;
        private String metric; // What metric to limit
        private String condition; // LESS_THAN, GREATER_THAN, BETWEEN
        private Boolean active;
        private String description;
        private Instant createdAt;
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if any position limits are breached
     */
    public boolean hasPositionLimitBreach() {
        if (sectorLimits != null) {
            return sectorLimits.stream().anyMatch(limit -> 
                limit.getBreached() != null && limit.getBreached());
        }
        return false;
    }
    
    /**
     * Get limit utilization for a specific type
     */
    public BigDecimal getLimitUtilization(String limitType) {
        return switch (limitType) {
            case "POSITION_VALUE" -> calculatePositionUtilization();
            case "LEVERAGE" -> calculateLeverageUtilization();
            case "SECTOR" -> calculateSectorUtilization();
            case "VELOCITY" -> calculateVelocityUtilization();
            default -> BigDecimal.ZERO;
        };
    }
    
    private BigDecimal calculatePositionUtilization() {
        if (positionLimits == null || positionLimits.getMaxSinglePositionPercent() == null) {
            return BigDecimal.ZERO;
        }
        
        return new BigDecimal("75.0"); // Simulated current utilization
    }
    
    private BigDecimal calculateLeverageUtilization() {
        if (leverageLimits == null || leverageLimits.getMaxLeverageRatio() == null) {
            return BigDecimal.ZERO;
        }
        
        return new BigDecimal("60.0"); // Simulated current leverage utilization
    }
    
    private BigDecimal calculateSectorUtilization() {
        if (sectorLimits == null || sectorLimits.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return sectorLimits.stream()
            .filter(limit -> limit.getUtilizationPercent() != null)
            .map(SectorLimit::getUtilizationPercent)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }
    
    private BigDecimal calculateVelocityUtilization() {
        if (velocityLimits == null) {
            return BigDecimal.ZERO;
        }
        
        return new BigDecimal("45.0"); // Simulated velocity utilization
    }
    
    /**
     * Check if leverage is within limits
     */
    public boolean isLeverageWithinLimits(BigDecimal currentLeverage) {
        return leverageLimits != null && 
               leverageLimits.getMaxLeverageRatio() != null &&
               currentLeverage.compareTo(leverageLimits.getMaxLeverageRatio()) <= 0;
    }
    
    /**
     * Get effective limit considering dynamic adjustments
     */
    public BigDecimal getEffectiveLimit(String limitType, BigDecimal baseLimit) {
        if (dynamicAdjustments == null || !dynamicAdjustments.getEnableVolatilityAdjustment()) {
            return baseLimit;
        }
        
        BigDecimal multiplier = dynamicAdjustments.getVolatilityMultiplier();
        return multiplier != null ? baseLimit.multiply(multiplier) : baseLimit;
    }
    
    /**
     * Check if alerts should be triggered
     */
    public boolean shouldTriggerAlert(BigDecimal utilizationPercent) {
        if (alertThresholds == null) return false;
        
        BigDecimal warningThreshold = alertThresholds.getWarningThresholdPercent();
        return warningThreshold != null && 
               utilizationPercent.compareTo(warningThreshold) >= 0;
    }
    
    /**
     * Get active sector limits
     */
    public List<SectorLimit> getActiveSectorLimits() {
        if (sectorLimits == null) return List.of();
        return sectorLimits.stream()
            .filter(limit -> limit.getMaxAllocationPercent() != null)
            .toList();
    }
    
    /**
     * Get limits summary
     */
    public Map<String, Object> getLimitsSummary() {
        return Map.of(
            "profileType", profileType != null ? profileType : "UNKNOWN",
            "active", active != null ? active : false,
            "sectorLimitsCount", sectorLimits != null ? sectorLimits.size() : 0,
            "customLimitsCount", customLimits != null ? customLimits.size() : 0,
            "hasPositionLimitBreach", hasPositionLimitBreach(),
            "dynamicAdjustmentsEnabled", dynamicAdjustments != null && 
                                       dynamicAdjustments.getEnableVolatilityAdjustment()
        );
    }
    
    /**
     * Static factory methods
     */
    public static RiskLimits conservative(Long userId) {
        return RiskLimits.builder()
            .userId(userId)
            .profileType("CONSERVATIVE")
            .active(true)
            .createdAt(Instant.now())
            .positionLimits(PositionLimits.builder()
                .maxSinglePositionPercent(new BigDecimal("5.0"))
                .maxSectorConcentration(new BigDecimal("15.0"))
                .build())
            .leverageLimits(LeverageLimits.builder()
                .maxLeverageRatio(new BigDecimal("1.5"))
                .allowMarginTrading(false)
                .build())
            .build();
    }
    
    public static RiskLimits aggressive(Long userId) {
        return RiskLimits.builder()
            .userId(userId)
            .profileType("AGGRESSIVE")
            .active(true)
            .createdAt(Instant.now())
            .positionLimits(PositionLimits.builder()
                .maxSinglePositionPercent(new BigDecimal("15.0"))
                .maxSectorConcentration(new BigDecimal("40.0"))
                .build())
            .leverageLimits(LeverageLimits.builder()
                .maxLeverageRatio(new BigDecimal("4.0"))
                .allowMarginTrading(true)
                .build())
            .build();
    }
}