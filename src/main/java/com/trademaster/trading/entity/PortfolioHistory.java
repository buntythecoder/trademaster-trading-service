package com.trademaster.trading.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Portfolio History Entity
 * 
 * Tracks portfolio changes over time for historical performance analysis.
 * Maps exactly to the 'portfolio_history' table in V1__Create_trading_schema.sql
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Schema-Aligned)
 */
@Entity
@Table(name = "portfolio_history", 
    indexes = {
        @Index(name = "idx_portfolio_history_user_date", columnList = "user_id, snapshot_date"),
        @Index(name = "idx_portfolio_history_symbol_date", columnList = "symbol, snapshot_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_portfolio_history_user_symbol_exchange_date", 
                         columnNames = {"user_id", "symbol", "exchange", "snapshot_date"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User who owns this historical position
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
     * Exchange where security was traded (NSE, BSE)
     */
    @Column(name = "exchange", nullable = false, length = 10)
    @NotNull
    private String exchange;
    
    /**
     * Position quantity at this snapshot date
     */
    @Column(name = "quantity", nullable = false)
    @NotNull
    private Integer quantity;
    
    /**
     * Average price per share at this snapshot date
     */
    @Column(name = "avg_price", precision = 15, scale = 4, nullable = false)
    @NotNull
    private BigDecimal avgPrice;
    
    /**
     * Market value of position at this snapshot date
     */
    @Column(name = "market_value", precision = 20, scale = 4)
    private BigDecimal marketValue;
    
    /**
     * Unrealized profit/loss at this snapshot date
     */
    @Column(name = "unrealized_pnl", precision = 20, scale = 4)
    private BigDecimal unrealizedPnl;
    
    /**
     * Date of this portfolio snapshot
     */
    @Column(name = "snapshot_date", nullable = false)
    @NotNull
    private LocalDate snapshotDate;
    
    /**
     * Snapshot creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Helper methods
    
    /**
     * Check if position was long on this date
     */
    public boolean isLong() {
        return quantity != null && quantity > 0;
    }
    
    /**
     * Check if position was short on this date
     */
    public boolean isShort() {
        return quantity != null && quantity < 0;
    }
    
    /**
     * Check if position was flat on this date
     */
    public boolean isFlat() {
        return quantity == null || quantity == 0;
    }
    
    /**
     * Calculate cost basis for this historical position
     * Uses Optional to eliminate if-statement
     */
    public BigDecimal getCostBasis() {
        return Optional.ofNullable(quantity)
            .flatMap(qty -> Optional.ofNullable(avgPrice)
                .map(price -> price.multiply(new BigDecimal(Math.abs(qty)))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Calculate P&L percentage for this historical position
     * Uses Optional to eliminate if-statements
     */
    public BigDecimal getPnlPercent() {
        return Optional.ofNullable(unrealizedPnl)
            .flatMap(pnl -> Optional.ofNullable(marketValue)
                .filter(mv -> !mv.equals(BigDecimal.ZERO))
                .map(mv -> getCostBasis())
                .filter(costBasis -> !costBasis.equals(BigDecimal.ZERO))
                .map(costBasis -> pnl.divide(costBasis, 4, java.math.RoundingMode.HALF_UP)
                                     .multiply(new BigDecimal("100"))))
            .orElse(BigDecimal.ZERO);
    }
}