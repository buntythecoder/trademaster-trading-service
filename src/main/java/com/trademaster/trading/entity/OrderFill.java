package com.trademaster.trading.entity;

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
import java.util.Optional;

/**
 * Order Fill Entity
 * 
 * Tracks partial fills and execution details for orders.
 * Maps exactly to the 'order_fills' table in V1__Create_trading_schema.sql
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Schema-Aligned)
 */
@Entity
@Table(name = "order_fills", indexes = {
    @Index(name = "idx_order_fills_order_id", columnList = "order_id"),
    @Index(name = "idx_order_fills_fill_time", columnList = "fill_time")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFill {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Associated order ID (foreign key to orders table)
     */
    @Column(name = "order_id", nullable = false)
    @NotNull
    private Long orderId;
    
    /**
     * Quantity filled in this execution
     */
    @Column(name = "fill_quantity", nullable = false)
    @NotNull
    @Positive
    private Integer fillQuantity;
    
    /**
     * Execution price for this fill
     */
    @Column(name = "fill_price", precision = 15, scale = 4, nullable = false)
    @NotNull
    @Positive
    private BigDecimal fillPrice;
    
    /**
     * Fill execution timestamp
     */
    @Column(name = "fill_time", nullable = false)
    @NotNull
    @Builder.Default
    private Instant fillTime = Instant.now();
    
    /**
     * Broker's internal fill ID
     */
    @Column(name = "broker_fill_id", length = 100)
    private String brokerFillId;
    
    /**
     * Commission for this fill
     */
    @Column(name = "commission", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal commission = BigDecimal.ZERO;
    
    /**
     * Taxes for this fill
     */
    @Column(name = "taxes", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal taxes = BigDecimal.ZERO;
    
    /**
     * Fill creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // Helper methods
    
    /**
     * Calculate total fill value (quantity * price)
     */
    public BigDecimal getFillValue() {
        // Eliminates if-statement using Optional.ofNullable().flatMap() chain
        return Optional.ofNullable(fillQuantity)
            .flatMap(qty -> Optional.ofNullable(fillPrice)
                .map(price -> price.multiply(new BigDecimal(qty))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Calculate total charges (commission + taxes)
     */
    public BigDecimal getTotalCharges() {
        // Eliminates ternary operators using Optional.ofNullable().orElse()
        BigDecimal comm = Optional.ofNullable(commission).orElse(BigDecimal.ZERO);
        BigDecimal tax = Optional.ofNullable(taxes).orElse(BigDecimal.ZERO);
        return comm.add(tax);
    }
    
    /**
     * Calculate net fill value after charges
     */
    public BigDecimal getNetFillValue() {
        return getFillValue().subtract(getTotalCharges());
    }
}