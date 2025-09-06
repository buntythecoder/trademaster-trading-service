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
 * Simplified Position Entity (Aligned with Migration Schema)
 * 
 * Maps to the 'portfolios' table in the migration schema with exact field alignment.
 * This replaces the complex Position entity to ensure deployment compatibility.
 * 
 * Database Schema Alignment:
 * - Table: portfolios (exactly matches V1__Create_trading_schema.sql)
 * - Fields: All fields match migration column names and types
 * - Indexes: Matches migration index definitions
 * - Constraints: Unique constraint on (user_id, symbol, exchange)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Schema-Aligned)
 */
@Entity
@Table(name = "portfolios", 
    indexes = {
        @Index(name = "idx_portfolios_user_id", columnList = "user_id"),
        @Index(name = "idx_portfolios_symbol", columnList = "symbol"),
        @Index(name = "idx_portfolios_last_updated", columnList = "last_updated")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_portfolios_user_symbol_exchange", 
                         columnNames = {"user_id", "symbol", "exchange"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimplifiedPosition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User who owns this position
     */
    @Column(name = "user_id", nullable = false)
    @NotNull
    private Long userId;
    
    /**
     * Trading symbol (e.g., RELIANCE, TCS, INFY)
     */
    @Column(name = "symbol", nullable = false, length = 20)
    @NotNull
    private String symbol;
    
    /**
     * Exchange where security is traded (NSE, BSE)
     */
    @Column(name = "exchange", nullable = false, length = 10)
    @NotNull
    private String exchange;
    
    /**
     * Current position quantity (can be negative for short positions)
     */
    @Column(name = "quantity", nullable = false)
    @NotNull
    private Integer quantity;
    
    /**
     * Average price per share (must be positive as per CHECK constraint)
     */
    @Column(name = "avg_price", precision = 15, scale = 4, nullable = false)
    @NotNull
    private BigDecimal avgPrice;
    
    /**
     * Current market value of position
     */
    @Column(name = "market_value", precision = 20, scale = 4)
    private BigDecimal marketValue;
    
    /**
     * Unrealized profit/loss (mark-to-market)
     */
    @Column(name = "unrealized_pnl", precision = 20, scale = 4)
    private BigDecimal unrealizedPnl;
    
    /**
     * Realized profit/loss from closed positions
     */
    @Column(name = "realized_pnl", precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;
    
    /**
     * Last market price
     */
    @Column(name = "last_price", precision = 15, scale = 4)
    private BigDecimal lastPrice;
    
    /**
     * Last update timestamp (matches migration column name)
     */
    @LastModifiedDate
    @Column(name = "last_updated")
    private Instant lastUpdated;
    
    /**
     * Position creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Calculated methods
    
    /**
     * Check if position is long (positive quantity)
     */
    public boolean isLong() {
        return quantity != null && quantity > 0;
    }
    
    /**
     * Check if position is short (negative quantity)
     */
    public boolean isShort() {
        return quantity != null && quantity < 0;
    }
    
    /**
     * Check if position is flat (zero quantity)
     */
    public boolean isFlat() {
        return quantity == null || quantity == 0;
    }
    
    /**
     * Calculate current profit/loss
     */
    public BigDecimal calculateCurrentPnL() {
        if (unrealizedPnl == null && realizedPnl == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal unrealized = unrealizedPnl != null ? unrealizedPnl : BigDecimal.ZERO;
        BigDecimal realized = realizedPnl != null ? realizedPnl : BigDecimal.ZERO;
        
        return unrealized.add(realized);
    }
    
    /**
     * Calculate position value based on current price
     */
    public BigDecimal calculatePositionValue() {
        if (quantity == null || lastPrice == null) {
            return marketValue != null ? marketValue : BigDecimal.ZERO;
        }
        
        return lastPrice.multiply(new BigDecimal(Math.abs(quantity)));
    }
    
    /**
     * Update market value and unrealized P&L based on current price
     */
    public void updateMarketValue(BigDecimal currentPrice) {
        if (currentPrice == null || quantity == null || avgPrice == null) {
            return;
        }
        
        this.lastPrice = currentPrice;
        this.marketValue = currentPrice.multiply(new BigDecimal(Math.abs(quantity)));
        
        // Calculate unrealized P&L
        BigDecimal costBasis = avgPrice.multiply(new BigDecimal(Math.abs(quantity)));
        this.unrealizedPnl = isLong() ? 
            marketValue.subtract(costBasis) : 
            costBasis.subtract(marketValue);
        
        this.lastUpdated = Instant.now();
    }
}