package com.trademaster.trading.dto;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Order Response DTO
 * 
 * Data Transfer Object for order information responses.
 * Optimized for efficient serialization and minimal bandwidth usage.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderResponse(
    /**
     * Internal order ID
     */
    Long id,
    
    /**
     * External order identifier
     */
    String orderId,
    
    /**
     * User ID who placed the order
     */
    Long userId,
    
    /**
     * Trading symbol
     */
    String symbol,
    
    /**
     * Exchange
     */
    String exchange,
    
    /**
     * Order type
     */
    OrderType orderType,
    
    /**
     * Order side
     */
    OrderSide side,
    
    /**
     * Order quantity
     */
    Integer quantity,
    
    /**
     * Limit price (if applicable)
     */
    BigDecimal limitPrice,
    
    /**
     * Stop price (if applicable)
     */
    BigDecimal stopPrice,
    
    /**
     * Time in force
     */
    TimeInForce timeInForce,
    
    /**
     * Expiry date (for GTD orders)
     */
    LocalDate expiryDate,
    
    /**
     * Current order status
     */
    OrderStatus status,
    
    /**
     * Broker order ID
     */
    String brokerOrderId,
    
    /**
     * Broker name
     */
    String brokerName,
    
    /**
     * Quantity filled
     */
    Integer filledQuantity,
    
    /**
     * Remaining quantity to be filled
     */
    Integer remainingQuantity,
    
    /**
     * Average fill price
     */
    BigDecimal averagePrice,
    
    /**
     * Fill percentage
     */
    Double fillPercentage,
    
    /**
     * Total order value
     */
    BigDecimal orderValue,
    
    /**
     * Executed value
     */
    BigDecimal executedValue,
    
    /**
     * Rejection reason (if applicable)
     */
    String rejectionReason,
    
    /**
     * Order creation timestamp
     */
    Instant createdAt,
    
    /**
     * Last modification timestamp
     */
    Instant updatedAt,
    
    /**
     * Order submission timestamp
     */
    Instant submittedAt,
    
    /**
     * Order execution timestamp
     */
    Instant executedAt
) {
    
    /**
     * Order summary for quick display
     */
    public String getOrderSummary() {
        return String.format("%s %d %s @ %s", 
                           side.getDisplayName(),
                           quantity,
                           symbol,
                           orderType == OrderType.MARKET ? "MARKET" : 
                           (limitPrice != null ? limitPrice.toString() : "N/A"));
    }
    
    /**
     * Check if order is in terminal state
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }
    
    /**
     * Check if order is actively trading
     */
    public boolean isActive() {
        return status.isActive();
    }
    
    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return status.isCancellable();
    }
    
    /**
     * Check if order can be modified
     */
    public boolean isModifiable() {
        return status.isModifiable();
    }
    
    // Convenience methods for backward compatibility with tests
    public String getOrderId() {
        return orderId;
    }
    
    public BigDecimal getPrice() {
        return limitPrice;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public BigDecimal getAveragePrice() {
        return averagePrice;
    }
    
    /**
     * Builder pattern for easier construction
     */
    public static OrderResponseBuilder builder() {
        return new OrderResponseBuilder();
    }
    
    public static class OrderResponseBuilder {
        private Long id;
        private String orderId;
        private Long userId;
        private String symbol;
        private String exchange;
        private OrderType orderType;
        private OrderSide side;
        private Integer quantity;
        private BigDecimal limitPrice;
        private BigDecimal stopPrice;
        private TimeInForce timeInForce;
        private LocalDate expiryDate;
        private OrderStatus status;
        private String brokerOrderId;
        private String brokerName;
        private Integer filledQuantity;
        private Integer remainingQuantity;
        private BigDecimal averagePrice;
        private Double fillPercentage;
        private BigDecimal orderValue;
        private BigDecimal executedValue;
        private String rejectionReason;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant submittedAt;
        private Instant executedAt;
        
        public OrderResponseBuilder id(Long id) { this.id = id; return this; }
        public OrderResponseBuilder orderId(String orderId) { this.orderId = orderId; return this; }
        public OrderResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public OrderResponseBuilder symbol(String symbol) { this.symbol = symbol; return this; }
        public OrderResponseBuilder exchange(String exchange) { this.exchange = exchange; return this; }
        public OrderResponseBuilder orderType(OrderType orderType) { this.orderType = orderType; return this; }
        public OrderResponseBuilder side(OrderSide side) { this.side = side; return this; }
        public OrderResponseBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public OrderResponseBuilder limitPrice(BigDecimal limitPrice) { this.limitPrice = limitPrice; return this; }
        
        // Convenience method for backward compatibility
        public OrderResponseBuilder price(BigDecimal price) { this.limitPrice = price; return this; }
        public OrderResponseBuilder stopPrice(BigDecimal stopPrice) { this.stopPrice = stopPrice; return this; }
        public OrderResponseBuilder timeInForce(TimeInForce timeInForce) { this.timeInForce = timeInForce; return this; }
        public OrderResponseBuilder expiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; return this; }
        public OrderResponseBuilder status(OrderStatus status) { this.status = status; return this; }
        public OrderResponseBuilder brokerOrderId(String brokerOrderId) { this.brokerOrderId = brokerOrderId; return this; }
        public OrderResponseBuilder brokerName(String brokerName) { this.brokerName = brokerName; return this; }
        public OrderResponseBuilder filledQuantity(Integer filledQuantity) { this.filledQuantity = filledQuantity; return this; }
        public OrderResponseBuilder remainingQuantity(Integer remainingQuantity) { this.remainingQuantity = remainingQuantity; return this; }
        public OrderResponseBuilder averagePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; return this; }
        public OrderResponseBuilder fillPercentage(Double fillPercentage) { this.fillPercentage = fillPercentage; return this; }
        public OrderResponseBuilder orderValue(BigDecimal orderValue) { this.orderValue = orderValue; return this; }
        public OrderResponseBuilder executedValue(BigDecimal executedValue) { this.executedValue = executedValue; return this; }
        public OrderResponseBuilder rejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; return this; }
        public OrderResponseBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public OrderResponseBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public OrderResponseBuilder submittedAt(Instant submittedAt) { this.submittedAt = submittedAt; return this; }
        public OrderResponseBuilder executedAt(Instant executedAt) { this.executedAt = executedAt; return this; }
        
        public OrderResponse build() {
            return new OrderResponse(id, orderId, userId, symbol, exchange, orderType, side, quantity,
                                   limitPrice, stopPrice, timeInForce, expiryDate, status, brokerOrderId,
                                   brokerName, filledQuantity, remainingQuantity, averagePrice,
                                   fillPercentage, orderValue, executedValue, rejectionReason,
                                   createdAt, updatedAt, submittedAt, executedAt);
        }
    }
}