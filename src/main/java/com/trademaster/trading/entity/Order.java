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
import java.util.Optional;
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
     * Calculate fill percentage - eliminates if-statement with Optional pattern
     */
    public double getFillPercentage() {
        return Optional.ofNullable(quantity)
            .filter(q -> q > 0)
            .flatMap(q -> Optional.ofNullable(filledQuantity)
                .map(f -> (f.doubleValue() / q.doubleValue()) * 100.0))
            .orElse(0.0);
    }
    
    /**
     * Get average execution price - eliminates if-statement with Optional pattern
     */
    public BigDecimal getAveragePrice() {
        return Optional.ofNullable(filledQuantity)
            .filter(f -> f > 0)
            .flatMap(f -> Optional.ofNullable(totalFilledValue)
                .map(v -> v.divide(BigDecimal.valueOf(f), 4, java.math.RoundingMode.HALF_UP)))
            .orElse(BigDecimal.ZERO);
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
     * Calculate total order value - eliminates if-statements and ternary with Optional pattern
     */
    public BigDecimal getOrderValue() {
        BigDecimal price = Optional.of(orderType)
            .filter(OrderType::requiresLimitPrice)
            .map(type -> limitPrice)
            .orElse(null);

        return Optional.ofNullable(price)
            .flatMap(p -> Optional.ofNullable(quantity)
                .map(q -> p.multiply(BigDecimal.valueOf(q))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Calculate executed value (filled quantity * average fill price) - eliminates if-statement
     */
    public BigDecimal getExecutedValue() {
        return Optional.ofNullable(avgFillPrice)
            .flatMap(price -> Optional.ofNullable(filledQuantity)
                .filter(f -> f > 0)
                .map(f -> price.multiply(BigDecimal.valueOf(f))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Update order status with validation - eliminates if-statements with functional patterns
     */
    public void updateStatus(OrderStatus newStatus) {
        // Validate transition using Optional - eliminates if-else
        Optional.of(status)
            .filter(s -> s.canTransitionTo(newStatus))
            .ifPresentOrElse(
                s -> performStatusUpdate(newStatus),
                () -> {
                    throw new IllegalStateException(
                        String.format("Invalid status transition from %s to %s for order %s",
                                     status, newStatus, orderId));
                }
            );
    }

    /**
     * Perform status update with timestamp management - eliminates if-statements
     */
    private void performStatusUpdate(OrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();

        // Set submitted timestamp using pattern matching - eliminates if-statement
        Optional.of(newStatus)
            .filter(s -> s == OrderStatus.SUBMITTED)
            .filter(s -> submittedAt == null)
            .ifPresent(s -> this.submittedAt = Instant.now());

        // Set executed timestamp using pattern matching - eliminates if-else-if
        Optional.of(newStatus)
            .filter(s -> s == OrderStatus.PARTIALLY_FILLED || s == OrderStatus.FILLED)
            .filter(s -> executedAt == null)
            .ifPresent(s -> this.executedAt = Instant.now());
    }
    
    /**
     * Add fill to the order - eliminates if-statements with functional validation
     */
    public void addFill(Integer fillQuantity, BigDecimal fillPrice) {
        // Validate fill quantity using Optional - eliminates if-statement
        Optional.ofNullable(fillQuantity)
            .filter(q -> q > 0)
            .orElseThrow(() -> new IllegalArgumentException("Fill quantity must be positive"));

        // Validate fill price using Optional - eliminates if-statement
        Optional.ofNullable(fillPrice)
            .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
            .orElseThrow(() -> new IllegalArgumentException("Fill price must be positive"));

        int newFilledQuantity = Optional.ofNullable(filledQuantity).orElse(0) + fillQuantity;

        // Validate new quantity doesn't exceed order quantity - eliminates if-statement
        Optional.of(newFilledQuantity)
            .filter(q -> q <= quantity)
            .orElseThrow(() -> new IllegalArgumentException("Fill quantity exceeds order quantity"));

        // Calculate and update fill values - eliminates if-else with Optional
        BigDecimal fillValue = fillPrice.multiply(BigDecimal.valueOf(fillQuantity));

        Optional.ofNullable(totalFilledValue)
            .ifPresentOrElse(
                existing -> {
                    totalFilledValue = existing.add(fillValue);
                    avgFillPrice = totalFilledValue.divide(BigDecimal.valueOf(newFilledQuantity),
                                                         4, java.math.RoundingMode.HALF_UP);
                },
                () -> {
                    totalFilledValue = fillValue;
                    avgFillPrice = fillPrice;
                }
            );

        filledQuantity = newFilledQuantity;

        // Update status based on fill completion - eliminates if-else and ternary with Optional pattern
        OrderStatus newStatus = Optional.of(isCompletelyFilled())
            .filter(Boolean::booleanValue)
            .map(filled -> OrderStatus.FILLED)
            .orElse(OrderStatus.PARTIALLY_FILLED);
        updateStatus(newStatus);
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
     * Validate order for basic business rules - eliminates if-statements with functional validation
     */
    public void validate() {
        // Validate symbol using Optional - eliminates if-statement
        Optional.ofNullable(symbol)
            .filter(s -> !s.trim().isEmpty())
            .orElseThrow(() -> new IllegalArgumentException("Symbol is required"));

        // Validate quantity using Optional - eliminates if-statement
        Optional.ofNullable(quantity)
            .filter(q -> q > 0)
            .orElseThrow(() -> new IllegalArgumentException("Quantity must be positive"));

        // Validate limit price requirement using functional pattern - eliminates if-statement
        Optional.of(orderType)
            .filter(OrderType::requiresLimitPrice)
            .ifPresent(type -> Optional.ofNullable(limitPrice)
                .orElseThrow(() -> new IllegalArgumentException("Limit price is required for " + type)));

        // Validate stop price requirement using functional pattern - eliminates if-statement
        Optional.of(orderType)
            .filter(OrderType::requiresStopPrice)
            .ifPresent(type -> Optional.ofNullable(stopPrice)
                .orElseThrow(() -> new IllegalArgumentException("Stop price is required for " + type)));

        // Validate limit price is positive using Optional - eliminates if-statement
        Optional.ofNullable(limitPrice)
            .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
            .or(() -> Optional.ofNullable(limitPrice)
                .map(p -> {
                    throw new IllegalArgumentException("Limit price must be positive");
                })
            );

        // Validate stop price is positive using Optional - eliminates if-statement
        Optional.ofNullable(stopPrice)
            .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
            .or(() -> Optional.ofNullable(stopPrice)
                .map(p -> {
                    throw new IllegalArgumentException("Stop price must be positive");
                })
            );

        // Validate GTD expiry date using pattern matching - eliminates if-statement
        Optional.of(timeInForce)
            .filter(tif -> tif == TimeInForce.GTD)
            .ifPresent(tif -> Optional.ofNullable(expiryDate)
                .orElseThrow(() -> new IllegalArgumentException("Expiry date is required for GTD orders")));
    }
}