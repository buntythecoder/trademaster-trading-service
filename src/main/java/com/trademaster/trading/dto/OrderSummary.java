package com.trademaster.trading.dto;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order Summary DTO for JPA projection queries
 * 
 * Lightweight data transfer object for order summary information.
 * Used in JPA constructor expressions for efficient data retrieval.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public class OrderSummary {
    private final Long id;
    private final String orderId;
    private final String symbol;
    private final OrderSide side;
    private final Integer quantity;
    private final OrderStatus status;
    private final BigDecimal limitPrice;
    private final Instant createdAt;
    
    /**
     * Constructor for JPA projection queries
     * Parameter order must match the SELECT new constructor expression
     */
    public OrderSummary(Long id, String orderId, String symbol, OrderSide side, 
                       Integer quantity, OrderStatus status, BigDecimal limitPrice, Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.status = status;
        this.limitPrice = limitPrice;
        this.createdAt = createdAt;
    }
    
    // Getters
    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public OrderSide getSide() { return side; }
    public Integer getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public Instant getCreatedAt() { return createdAt; }
}