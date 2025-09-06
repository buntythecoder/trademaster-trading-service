package com.trademaster.trading.entity;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Order Entity
 * 
 * Represents a trading order with all necessary fields for order management,
 * risk checking, and broker integration. Designed for high-performance 
 * database operations with JPA and Virtual Threads.
 * 
 * Performance Considerations:
 * - Indexed fields for fast lookup (user_id, symbol, status, created_at)
 * - Optimized for concurrent access with Virtual Threads
 * - Immutable order ID for audit trail integrity
 * - JPA second-level cache enabled for frequent reads
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_orders_symbol", columnList = "symbol"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_created_at", columnList = "created_at"),
    @Index(name = "idx_orders_user_symbol_status", columnList = "user_id, symbol, status"),
    @Index(name = "idx_orders_broker_order_id", columnList = "broker_order_id"),
    @Index(name = "idx_orders_active", columnList = "user_id, status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique order identifier for external references
     * Format: TM-{timestamp}-{random}
     */
    @Column(name = "order_id", unique = true, nullable = false, length = 50)
    @Builder.Default
    private String orderId = generateOrderId();
    
    /**
     * User who placed the order
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
     * Exchange where security is listed (NSE, BSE)
     */
    @Column(name = "exchange", nullable = false, length = 10)
    @NotNull
    private String exchange;
    
    /**
     * Order type (MARKET, LIMIT, STOP_LOSS, STOP_LIMIT)
     */
    @Column(name = "order_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull
    private OrderType orderType;
    
    /**
     * Order side (BUY, SELL)
     */
    @Column(name = "side", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @NotNull
    private OrderSide side;
    
    /**
     * Order quantity in shares
     */
    @Column(name = "quantity", nullable = false)
    @NotNull
    @Positive
    private Integer quantity;
    
    /**
     * Limit price (required for LIMIT and STOP_LIMIT orders)
     */
    @Column(name = "limit_price", precision = 15, scale = 4)
    private BigDecimal limitPrice;
    
    /**
     * Stop price (required for STOP_LOSS and STOP_LIMIT orders)
     */
    @Column(name = "stop_price", precision = 15, scale = 4)
    private BigDecimal stopPrice;
    
    /**
     * Time in force (DAY, GTC, IOC, FOK)
     */
    @Column(name = "time_in_force", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TimeInForce timeInForce = TimeInForce.DAY;
    
    /**
     * Order expiry date (for GTD orders)
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    /**
     * Current order status
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    /**
     * Broker order ID for tracking external execution
     */
    @Column(name = "broker_order_id", length = 100)
    private String brokerOrderId;
    
    /**
     * Broker name handling this order
     */
    @Column(name = "broker_name", length = 50)
    private String brokerName;
    
    /**
     * Quantity filled so far
     */
    @Column(name = "filled_quantity")
    @Builder.Default
    private Integer filledQuantity = 0;
    
    /**
     * Average fill price for executed quantity
     */
    @Column(name = "avg_fill_price", precision = 15, scale = 4)
    private BigDecimal avgFillPrice;
    
    /**
     * Total value of filled quantity
     */
    @Column(name = "total_filled_value", precision = 18, scale = 4)
    private BigDecimal totalFilledValue;
    
    /**
     * Remaining quantity to be filled
     */
    public Integer getRemainingQuantity() {
        return quantity - filledQuantity;
    }
    
    /**
     * Rejection reason if order was rejected
     */
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    /**
     * Additional order metadata as JSON
     */
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;
    
    /**
     * Order creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Last modification timestamp
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Order submission timestamp (when sent to broker)
     */
    @Column(name = "submitted_at")
    private Instant submittedAt;
    
    /**
     * Order execution timestamp (when first fill received)
     */
    @Column(name = "executed_at")
    private Instant executedAt;
    
    /**
     * Generate unique order ID
     */
    private static String generateOrderId() {
        return "TM-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Check if order is completely filled
     */
    public boolean isCompletelyFilled() {
        return filledQuantity != null && filledQuantity.equals(quantity);
    }
    
    /**
     * Check if order is partially filled
     */
    public boolean isPartiallyFilled() {
        return filledQuantity != null && filledQuantity > 0 && !isCompletelyFilled();
    }
    
    /**
     * Calculate fill percentage
     */
    public double getFillPercentage() {
        if (quantity == null || quantity == 0 || filledQuantity == null) {
            return 0.0;
        }
        return (filledQuantity.doubleValue() / quantity.doubleValue()) * 100.0;
    }
    
    /**
     * Get average execution price
     */
    public BigDecimal getAveragePrice() {
        if (filledQuantity == null || filledQuantity == 0 || totalFilledValue == null) {
            return BigDecimal.ZERO;
        }
        return totalFilledValue.divide(BigDecimal.valueOf(filledQuantity), 4, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return status != null && switch (status) {
            case ACKNOWLEDGED, PARTIALLY_FILLED -> true;
            default -> false;
        };
    }
    
    /**
     * Check if order can be modified
     */
    public boolean isModifiable() {
        return status != null && switch (status) {
            case ACKNOWLEDGED -> true;
            default -> false;
        };
    }
    
    
    /**
     * Check if order is active
     */
    public boolean isActive() {
        return status != null && switch (status) {
            case ACKNOWLEDGED, PARTIALLY_FILLED -> true;
            default -> false;
        };
    }
    
    /**
     * Check if order is final
     */
    public boolean isFinal() {
        return status != null && switch (status) {
            case FILLED, CANCELLED, REJECTED, EXPIRED -> true;
            default -> false;
        };
    }
    
    /**
     * Builder convenience method for creating test orders
     */
    public static OrderBuilder testOrder() {
        return Order.builder()
            .orderId(generateOrderId())
            .symbol("RELIANCE")
            .exchange("NSE")
            .orderType(OrderType.MARKET)
            .side(OrderSide.BUY)
            .quantity(100)
            .status(OrderStatus.ACKNOWLEDGED)
            .timeInForce(TimeInForce.DAY)
            .createdAt(Instant.now())
            .updatedAt(Instant.now());
    }
    
    /**
     * Calculate total order value
     */
    public BigDecimal getOrderValue() {
        BigDecimal price = orderType.requiresLimitPrice() ? limitPrice : null;
        if (price == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }
    
    /**
     * Calculate executed value (filled quantity * average fill price)
     */
    public BigDecimal getExecutedValue() {
        if (avgFillPrice == null || filledQuantity == null || filledQuantity == 0) {
            return BigDecimal.ZERO;
        }
        return avgFillPrice.multiply(BigDecimal.valueOf(filledQuantity));
    }
    
    /**
     * Update order status with validation
     */
    public void updateStatus(OrderStatus newStatus) {
        if (status.canTransitionTo(newStatus)) {
            this.status = newStatus;
            this.updatedAt = Instant.now();
            
            // Set timestamps for specific status transitions
            if (newStatus == OrderStatus.SUBMITTED && submittedAt == null) {
                this.submittedAt = Instant.now();
            } else if ((newStatus == OrderStatus.PARTIALLY_FILLED || newStatus == OrderStatus.FILLED) 
                       && executedAt == null) {
                this.executedAt = Instant.now();
            }
        } else {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s for order %s", 
                             status, newStatus, orderId));
        }
    }
    
    /**
     * Add fill to the order
     */
    public void addFill(Integer fillQuantity, BigDecimal fillPrice) {
        if (fillQuantity == null || fillQuantity <= 0) {
            throw new IllegalArgumentException("Fill quantity must be positive");
        }
        
        if (fillPrice == null || fillPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fill price must be positive");
        }
        
        int newFilledQuantity = (filledQuantity != null ? filledQuantity : 0) + fillQuantity;
        
        if (newFilledQuantity > quantity) {
            throw new IllegalArgumentException("Fill quantity exceeds order quantity");
        }
        
        // Calculate new average fill price and total filled value
        BigDecimal fillValue = fillPrice.multiply(BigDecimal.valueOf(fillQuantity));
        
        if (totalFilledValue == null) {
            totalFilledValue = fillValue;
            avgFillPrice = fillPrice;
        } else {
            totalFilledValue = totalFilledValue.add(fillValue);
            avgFillPrice = totalFilledValue.divide(BigDecimal.valueOf(newFilledQuantity), 
                                                 4, java.math.RoundingMode.HALF_UP);
        }
        
        filledQuantity = newFilledQuantity;
        
        // Update status based on fill completion
        if (isCompletelyFilled()) {
            updateStatus(OrderStatus.FILLED);
        } else {
            updateStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }
    
    /**
     * Convenience methods for record-style access (backward compatibility with tests)
     */
    public String symbol() {
        return symbol;
    }
    
    public OrderType orderType() {
        return orderType;
    }
    
    public Integer quantity() {
        return quantity;
    }
    
    public OrderSide side() {
        return side;
    }
    
    public BigDecimal limitPrice() {
        return limitPrice;
    }
    
    public String exchange() {
        return exchange;
    }
    
    /**
     * Validate order for basic business rules
     */
    public void validate() {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        if (orderType.requiresLimitPrice() && limitPrice == null) {
            throw new IllegalArgumentException("Limit price is required for " + orderType);
        }
        
        if (orderType.requiresStopPrice() && stopPrice == null) {
            throw new IllegalArgumentException("Stop price is required for " + orderType);
        }
        
        if (limitPrice != null && limitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Limit price must be positive");
        }
        
        if (stopPrice != null && stopPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stop price must be positive");
        }
        
        if (timeInForce == TimeInForce.GTD && expiryDate == null) {
            throw new IllegalArgumentException("Expiry date is required for GTD orders");
        }
    }
}