package com.trademaster.trading.entity;

import com.trademaster.trading.model.OrderSide;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Trade Entity
 * 
 * Represents completed trade records for reporting and settlement.
 * Maps exactly to the 'trades' table in V1__Create_trading_schema.sql
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Schema-Aligned)
 */
@Entity
@Table(name = "trades", indexes = {
    @Index(name = "idx_trades_user_id", columnList = "user_id"),
    @Index(name = "idx_trades_symbol", columnList = "symbol"),
    @Index(name = "idx_trades_trade_time", columnList = "trade_time"),
    @Index(name = "idx_trades_user_symbol_time", columnList = "user_id, symbol, trade_time")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique trade identifier (auto-generated in database)
     */
    @Column(name = "trade_id", unique = true, nullable = false, length = 50)
    private String tradeId;
    
    /**
     * User who executed the trade
     */
    @Column(name = "user_id", nullable = false)
    @NotNull
    private Long userId;
    
    /**
     * Associated order ID
     */
    @Column(name = "order_id", nullable = false)
    @NotNull
    private Long orderId;
    
    /**
     * Trading symbol (e.g., RELIANCE, TCS, INFY)
     */
    @Column(name = "symbol", nullable = false, length = 20)
    @NotNull
    private String symbol;
    
    /**
     * Exchange where trade was executed (NSE, BSE)
     */
    @Column(name = "exchange", nullable = false, length = 10)
    @NotNull
    private String exchange;
    
    /**
     * Trade side (BUY, SELL)
     */
    @Column(name = "side", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @NotNull
    private OrderSide side;
    
    /**
     * Trade quantity in shares
     */
    @Column(name = "quantity", nullable = false)
    @NotNull
    @Positive
    private Integer quantity;
    
    /**
     * Trade execution price per share
     */
    @Column(name = "price", precision = 15, scale = 4, nullable = false)
    @NotNull
    @Positive
    private BigDecimal price;
    
    /**
     * Total trade value (quantity * price)
     */
    @Column(name = "trade_value", precision = 20, scale = 4, nullable = false)
    @NotNull
    @Positive
    private BigDecimal tradeValue;
    
    /**
     * Commission charges
     */
    @Column(name = "commission", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal commission = BigDecimal.ZERO;
    
    /**
     * Tax charges (STT, etc.)
     */
    @Column(name = "taxes", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal taxes = BigDecimal.ZERO;
    
    /**
     * Net amount after charges
     */
    @Column(name = "net_amount", precision = 20, scale = 4, nullable = false)
    @NotNull
    private BigDecimal netAmount;
    
    /**
     * Trade execution timestamp
     */
    @Column(name = "trade_time", nullable = false)
    @NotNull
    @Builder.Default
    private Instant tradeTime = Instant.now();
    
    /**
     * Settlement date for this trade
     */
    @Column(name = "settlement_date")
    private LocalDate settlementDate;
    
    /**
     * Broker's internal trade ID
     */
    @Column(name = "broker_trade_id", length = 100)
    private String brokerTradeId;
    
    /**
     * Trade creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Helper methods
    
    /**
     * Calculate total charges (commission + taxes)
     */
    public BigDecimal getTotalCharges() {
        BigDecimal comm = commission != null ? commission : BigDecimal.ZERO;
        BigDecimal tax = taxes != null ? taxes : BigDecimal.ZERO;
        return comm.add(tax);
    }
    
    /**
     * Check if trade is a buy
     */
    public boolean isBuy() {
        return OrderSide.BUY.equals(side);
    }
    
    /**
     * Check if trade is a sell
     */
    public boolean isSell() {
        return OrderSide.SELL.equals(side);
    }
    
    /**
     * Calculate net amount (trade value - charges for sell, trade value + charges for buy)
     */
    public BigDecimal calculateNetAmount() {
        BigDecimal totalCharges = getTotalCharges();
        return isBuy() ? 
            tradeValue.add(totalCharges) : 
            tradeValue.subtract(totalCharges);
    }
}