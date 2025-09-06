package com.trademaster.trading.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Simplified Risk Limit Entity (Aligned with Migration Schema)
 * 
 * Maps exactly to the 'risk_limits' table in V1__Create_trading_schema.sql
 * This replaces the complex RiskLimit entity to ensure deployment compatibility.
 * 
 * Database Schema Alignment:
 * - Table: risk_limits (exactly matches migration)
 * - Fields: All fields match migration column names and types
 * - Constraints: user_id UNIQUE constraint as per migration
 * - Defaults: Match migration default values
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Schema-Aligned)
 */
@Entity
@Table(name = "risk_limits", 
    indexes = {
        @Index(name = "idx_risk_limits_user_id", columnList = "user_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_risk_limits_user_id", columnNames = {"user_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimplifiedRiskLimit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User ID (unique constraint as per migration)
     */
    @Column(name = "user_id", nullable = false, unique = true)
    @NotNull
    private Long userId;
    
    /**
     * Maximum total position value (₹1 Crore default)
     */
    @Column(name = "max_position_value", precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal maxPositionValue = new BigDecimal("10000000");
    
    /**
     * Maximum single order value (₹10 Lakh default)
     */
    @Column(name = "max_single_order_value", precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal maxSingleOrderValue = new BigDecimal("1000000");
    
    /**
     * Maximum daily trades (500 default)
     */
    @Column(name = "max_daily_trades")
    @Builder.Default
    private Integer maxDailyTrades = 500;
    
    /**
     * Maximum open orders (1000 default)
     */
    @Column(name = "max_open_orders")
    @Builder.Default
    private Integer maxOpenOrders = 1000;
    
    /**
     * Pattern day trader flag
     */
    @Column(name = "pattern_day_trader", nullable = false)
    @Builder.Default
    private Boolean patternDayTrader = false;
    
    /**
     * Day trading buying power
     */
    @Column(name = "day_trading_buying_power", precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal dayTradingBuyingPower = BigDecimal.ZERO;
    
    /**
     * Risk limit creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Risk limit modification timestamp
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // Helper methods
    
    /**
     * Check if user is a pattern day trader
     */
    public boolean isPatternDayTrader() {
        return Boolean.TRUE.equals(patternDayTrader);
    }
    
    /**
     * Check if order value exceeds single order limit
     */
    public boolean exceedsSingleOrderLimit(BigDecimal orderValue) {
        if (maxSingleOrderValue == null || orderValue == null) {
            return false;
        }
        return orderValue.compareTo(maxSingleOrderValue) > 0;
    }
    
    /**
     * Check if position value exceeds maximum position limit
     */
    public boolean exceedsPositionLimit(BigDecimal positionValue) {
        if (maxPositionValue == null || positionValue == null) {
            return false;
        }
        return positionValue.compareTo(maxPositionValue) > 0;
    }
    
    /**
     * Check if daily trade count exceeds limit
     */
    public boolean exceedsDailyTradeLimit(int currentDailyTrades) {
        if (maxDailyTrades == null) {
            return false;
        }
        return currentDailyTrades >= maxDailyTrades;
    }
    
    /**
     * Check if open order count exceeds limit
     */
    public boolean exceedsOpenOrderLimit(int currentOpenOrders) {
        if (maxOpenOrders == null) {
            return false;
        }
        return currentOpenOrders >= maxOpenOrders;
    }
    
    /**
     * Get available day trading buying power
     */
    public BigDecimal getAvailableDayTradingPower() {
        return dayTradingBuyingPower != null ? dayTradingBuyingPower : BigDecimal.ZERO;
    }
    
    /**
     * Create default risk limits for new user
     */
    public static SimplifiedRiskLimit createDefault(Long userId) {
        return SimplifiedRiskLimit.builder()
            .userId(userId)
            .maxPositionValue(new BigDecimal("10000000"))      // ₹1 Crore
            .maxSingleOrderValue(new BigDecimal("1000000"))    // ₹10 Lakh
            .maxDailyTrades(500)
            .maxOpenOrders(1000)
            .patternDayTrader(false)
            .dayTradingBuyingPower(BigDecimal.ZERO)
            .build();
    }
    
    /**
     * Create conservative risk limits
     */
    public static SimplifiedRiskLimit createConservative(Long userId) {
        return SimplifiedRiskLimit.builder()
            .userId(userId)
            .maxPositionValue(new BigDecimal("5000000"))       // ₹50 Lakh
            .maxSingleOrderValue(new BigDecimal("500000"))     // ₹5 Lakh
            .maxDailyTrades(100)
            .maxOpenOrders(500)
            .patternDayTrader(false)
            .dayTradingBuyingPower(BigDecimal.ZERO)
            .build();
    }
}