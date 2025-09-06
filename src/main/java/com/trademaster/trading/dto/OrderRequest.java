package com.trademaster.trading.dto;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Order Request DTO
 * 
 * Data Transfer Object for order placement requests with comprehensive validation.
 * Designed for high-performance reactive processing with minimal serialization overhead.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderRequest(
    
    /**
     * Trading symbol (e.g., RELIANCE, TCS, INFY)
     */
    @NotBlank(message = "Symbol is required")
    @Size(min = 1, max = 20, message = "Symbol must be between 1 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Symbol must contain only uppercase letters, numbers, and underscores")
    String symbol,
    
    /**
     * Exchange where security is listed (NSE, BSE)
     */
    @NotBlank(message = "Exchange is required")
    @Pattern(regexp = "^(NSE|BSE|MCX)$", message = "Exchange must be NSE, BSE, or MCX")
    String exchange,
    
    /**
     * Order type (MARKET, LIMIT, STOP_LOSS, STOP_LIMIT)
     */
    @NotNull(message = "Order type is required")
    OrderType orderType,
    
    /**
     * Order side (BUY, SELL)
     */
    @NotNull(message = "Order side is required")
    OrderSide side,
    
    /**
     * Order quantity in shares
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 1000000, message = "Quantity cannot exceed 1,000,000 shares")
    Integer quantity,
    
    /**
     * Limit price (required for LIMIT and STOP_LIMIT orders)
     */
    @DecimalMin(value = "0.01", message = "Limit price must be at least 0.01")
    @DecimalMax(value = "100000.00", message = "Limit price cannot exceed 100,000")
    @Digits(integer = 6, fraction = 4, message = "Limit price format: max 6 digits before decimal, 4 after")
    BigDecimal limitPrice,
    
    /**
     * Stop price (required for STOP_LOSS and STOP_LIMIT orders)
     */
    @DecimalMin(value = "0.01", message = "Stop price must be at least 0.01")
    @DecimalMax(value = "100000.00", message = "Stop price cannot exceed 100,000")
    @Digits(integer = 6, fraction = 4, message = "Stop price format: max 6 digits before decimal, 4 after")
    BigDecimal stopPrice,
    
    /**
     * Time in force (DAY, GTC, IOC, FOK, GTD)
     */
    @NotNull(message = "Time in force is required")
    TimeInForce timeInForce,
    
    /**
     * Order expiry date (required for GTD orders)
     */
    @Future(message = "Expiry date must be in the future")
    LocalDate expiryDate,
    
    /**
     * Broker preference (optional)
     */
    @Size(max = 50, message = "Broker name cannot exceed 50 characters")
    String brokerName,
    
    /**
     * Client-side order reference (optional)
     */
    @Size(max = 100, message = "Client order reference cannot exceed 100 characters")
    String clientOrderRef
) {
    
    /**
     * Compact constructor for validation
     */
    public OrderRequest {
        // Set default TimeInForce if null
        if (timeInForce == null) {
            timeInForce = TimeInForce.DAY;
        }
        
        // Validate order business rules
        validate();
    }
    
    /**
     * Validate order request for business rule compliance
     */
    private void validate() {
        validatePriceRequirements();
        validateExpiryDateRequirement();
        validateOrderValue();
    }
    
    /**
     * Validate that required prices are provided based on order type
     */
    private void validatePriceRequirements() {
        if (orderType.requiresLimitPrice() && limitPrice == null) {
            throw new IllegalArgumentException(
                String.format("Limit price is required for %s orders", orderType));
        }
        
        if (orderType.requiresStopPrice() && stopPrice == null) {
            throw new IllegalArgumentException(
                String.format("Stop price is required for %s orders", orderType));
        }
        
        // Validate stop-limit order price relationship
        if (orderType == OrderType.STOP_LIMIT && limitPrice != null && stopPrice != null) {
            if (side == OrderSide.BUY && stopPrice.compareTo(limitPrice) < 0) {
                throw new IllegalArgumentException(
                    "For buy stop-limit orders, stop price must be >= limit price");
            }
            if (side == OrderSide.SELL && stopPrice.compareTo(limitPrice) > 0) {
                throw new IllegalArgumentException(
                    "For sell stop-limit orders, stop price must be <= limit price");
            }
        }
    }
    
    /**
     * Validate expiry date requirement for GTD orders
     */
    private void validateExpiryDateRequirement() {
        if (timeInForce == TimeInForce.GTD && expiryDate == null) {
            throw new IllegalArgumentException("Expiry date is required for Good Till Date orders");
        }
        
        if (timeInForce != TimeInForce.GTD && expiryDate != null) {
            throw new IllegalArgumentException("Expiry date should only be set for Good Till Date orders");
        }
    }
    
    /**
     * Validate order value limits
     */
    private void validateOrderValue() {
        BigDecimal price = getEffectivePrice();
        if (price != null) {
            BigDecimal orderValue = price.multiply(BigDecimal.valueOf(quantity));
            BigDecimal maxOrderValue = BigDecimal.valueOf(10_000_000); // ₹1 Crore
            
            if (orderValue.compareTo(maxOrderValue) > 0) {
                throw new IllegalArgumentException(
                    String.format("Order value ₹%.2f exceeds maximum allowed ₹%.2f", 
                                 orderValue, maxOrderValue));
            }
        }
    }
    
    /**
     * Get effective price for order value calculation
     */
    public BigDecimal getEffectivePrice() {
        return switch (orderType) {
            case MARKET -> null; // Market orders don't have a predetermined price
            case LIMIT -> limitPrice;
            case STOP_LOSS -> stopPrice;
            case STOP_LIMIT -> limitPrice; // Use limit price for value calculation
        };
    }
    
    /**
     * Calculate estimated order value
     */
    public BigDecimal getEstimatedOrderValue() {
        BigDecimal price = getEffectivePrice();
        return price != null ? price.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
    }
    
    /**
     * Check if this is a day trading order (buy/sell same symbol same day)
     */
    public boolean isDayTradingOrder(boolean hasOpenPosition) {
        return (side == OrderSide.SELL && !hasOpenPosition) || 
               (side == OrderSide.BUY && hasOpenPosition);
    }
    
    /**
     * Builder pattern for easier construction
     */
    public static OrderRequestBuilder builder() {
        return new OrderRequestBuilder();
    }
    
    /**
     * Create builder from existing record
     */
    public OrderRequestBuilder toBuilder() {
        return new OrderRequestBuilder()
            .symbol(this.symbol)
            .exchange(this.exchange)
            .orderType(this.orderType)
            .side(this.side)
            .quantity(this.quantity)
            .limitPrice(this.limitPrice)
            .stopPrice(this.stopPrice)
            .timeInForce(this.timeInForce)
            .expiryDate(this.expiryDate)
            .brokerName(this.brokerName)
            .clientOrderRef(this.clientOrderRef);
    }
    
    public static class OrderRequestBuilder {
        private String symbol;
        private String exchange;
        private OrderType orderType;
        private OrderSide side;
        private Integer quantity;
        private BigDecimal limitPrice;
        private BigDecimal stopPrice;
        private TimeInForce timeInForce = TimeInForce.DAY;
        private LocalDate expiryDate;
        private String brokerName;
        private String clientOrderRef;
        
        public OrderRequestBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        
        public OrderRequestBuilder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }
        
        public OrderRequestBuilder orderType(OrderType orderType) {
            this.orderType = orderType;
            return this;
        }
        
        public OrderRequestBuilder side(OrderSide side) {
            this.side = side;
            return this;
        }
        
        public OrderRequestBuilder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }
        
        public OrderRequestBuilder limitPrice(BigDecimal limitPrice) {
            this.limitPrice = limitPrice;
            return this;
        }
        
        // Convenience method for backward compatibility
        public OrderRequestBuilder price(BigDecimal price) {
            this.limitPrice = price;
            return this;
        }
        
        public OrderRequestBuilder stopPrice(BigDecimal stopPrice) {
            this.stopPrice = stopPrice;
            return this;
        }
        
        public OrderRequestBuilder timeInForce(TimeInForce timeInForce) {
            this.timeInForce = timeInForce;
            return this;
        }
        
        public OrderRequestBuilder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }
        
        public OrderRequestBuilder brokerName(String brokerName) {
            this.brokerName = brokerName;
            return this;
        }
        
        public OrderRequestBuilder clientOrderRef(String clientOrderRef) {
            this.clientOrderRef = clientOrderRef;
            return this;
        }
        
        public OrderRequest build() {
            return new OrderRequest(symbol, exchange, orderType, side, quantity,
                                  limitPrice, stopPrice, timeInForce, expiryDate,
                                  brokerName, clientOrderRef);
        }
    }
}