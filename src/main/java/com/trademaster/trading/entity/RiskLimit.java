package com.trademaster.trading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Risk Limit Entity
 * 
 * Database entity for storing user risk limits and configurations.
 * Supports dynamic risk adjustments and regulatory compliance tracking.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Entity
@Table(name = "risk_limits", indexes = {
    @Index(name = "idx_risk_limits_user_id", columnList = "user_id"),
    @Index(name = "idx_risk_limits_active", columnList = "active"),
    @Index(name = "idx_risk_limits_profile_type", columnList = "profile_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLimit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "profile_type", length = 20)
    private String profileType; // CONSERVATIVE, MODERATE, AGGRESSIVE, CUSTOM
    
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    // Position Limits
    @Column(name = "max_single_position_value", precision = 15, scale = 2)
    private BigDecimal maxSinglePositionValue;
    
    @Column(name = "max_single_position_percent", precision = 5, scale = 2)
    private BigDecimal maxSinglePositionPercent;
    
    @Column(name = "max_sector_concentration", precision = 5, scale = 2)
    private BigDecimal maxSectorConcentration;
    
    @Column(name = "max_total_positions")
    private Integer maxTotalPositions;
    
    // Leverage Limits
    @Column(name = "max_leverage_ratio", precision = 5, scale = 2)
    private BigDecimal maxLeverageRatio;
    
    @Column(name = "max_margin_utilization", precision = 5, scale = 2)
    private BigDecimal maxMarginUtilization;
    
    @Column(name = "allow_margin_trading", nullable = false)
    @Builder.Default
    private Boolean allowMarginTrading = false;
    
    // Trading Velocity Limits
    @Column(name = "max_daily_trading_value", precision = 15, scale = 2)
    private BigDecimal maxDailyTradingValue;
    
    @Column(name = "max_daily_orders")
    private Integer maxDailyOrders;
    
    @Column(name = "max_orders_per_minute")
    private Integer maxOrdersPerMinute;
    
    // Portfolio Risk Limits
    @Column(name = "max_var", precision = 15, scale = 2)
    private BigDecimal maxVaR;
    
    @Column(name = "max_drawdown", precision = 5, scale = 2)
    private BigDecimal maxDrawdown;
    
    @Column(name = "max_volatility", precision = 5, scale = 4)
    private BigDecimal maxVolatility;
    
    // Dynamic Adjustments
    @Column(name = "enable_volatility_adjustment", nullable = false)
    @Builder.Default
    private Boolean enableVolatilityAdjustment = false;
    
    @Column(name = "volatility_multiplier", precision = 5, scale = 3)
    @Builder.Default
    private BigDecimal volatilityMultiplier = BigDecimal.ONE;
    
    @Column(name = "enable_market_regime_adjustment", nullable = false)
    @Builder.Default
    private Boolean enableMarketRegimeAdjustment = false;
    
    // Alert Configuration
    @Column(name = "warning_threshold_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal warningThresholdPercent = new BigDecimal("80.0");
    
    @Column(name = "critical_threshold_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal criticalThresholdPercent = new BigDecimal("95.0");
    
    @Column(name = "enable_real_time_alerts", nullable = false)
    @Builder.Default
    private Boolean enableRealTimeAlerts = true;
    
    // Audit Fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if position limit is breached
     */
    public boolean isPositionLimitBreached(BigDecimal positionValue) {
        return maxSinglePositionValue != null && 
               positionValue.compareTo(maxSinglePositionValue) > 0;
    }
    
    /**
     * Check if leverage limit is breached
     */
    public boolean isLeverageLimitBreached(BigDecimal currentLeverage) {
        return maxLeverageRatio != null && 
               currentLeverage.compareTo(maxLeverageRatio) > 0;
    }
    
    /**
     * Check if VaR limit is breached
     */
    public boolean isVaRLimitBreached(BigDecimal currentVaR) {
        return maxVaR != null && currentVaR.compareTo(maxVaR) > 0;
    }
    
    /**
     * Get effective limit considering dynamic adjustments
     */
    public BigDecimal getEffectiveLimit(BigDecimal baseLimit, String marketCondition) {
        if (!enableVolatilityAdjustment || volatilityMultiplier == null) {
            return baseLimit;
        }
        
        // Adjust limit based on market volatility
        if ("HIGH_VOLATILITY".equals(marketCondition)) {
            return baseLimit.multiply(volatilityMultiplier);
        }
        
        return baseLimit;
    }
    
    /**
     * Check if alert should be triggered
     */
    public boolean shouldTriggerWarningAlert(BigDecimal utilization) {
        return warningThresholdPercent != null && 
               utilization.compareTo(warningThresholdPercent) >= 0;
    }
    
    /**
     * Check if critical alert should be triggered
     */
    public boolean shouldTriggerCriticalAlert(BigDecimal utilization) {
        return criticalThresholdPercent != null && 
               utilization.compareTo(criticalThresholdPercent) >= 0;
    }
    
    /**
     * Pre-persist callback to set defaults
     */
    @PrePersist
    protected void onCreate() {
        if (active == null) {
            active = true;
        }
        if (allowMarginTrading == null) {
            allowMarginTrading = false;
        }
        if (enableVolatilityAdjustment == null) {
            enableVolatilityAdjustment = false;
        }
        if (enableRealTimeAlerts == null) {
            enableRealTimeAlerts = true;
        }
        if (volatilityMultiplier == null) {
            volatilityMultiplier = BigDecimal.ONE;
        }
        if (warningThresholdPercent == null) {
            warningThresholdPercent = new BigDecimal("80.0");
        }
        if (criticalThresholdPercent == null) {
            criticalThresholdPercent = new BigDecimal("95.0");
        }
    }
    
    /**
     * Pre-update callback
     */
    @PreUpdate
    protected void onUpdate() {
        // Additional validation or processing can be added here
    }
}