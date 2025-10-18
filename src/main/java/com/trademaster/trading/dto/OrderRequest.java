package com.trademaster.trading.dto;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

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
    String clientOrderRef,

    /**
     * Trailing stop amount (for trailing stop orders)
     */
    @DecimalMin(value = "0.01", message = "Trail amount must be at least 0.01")
    BigDecimal trailAmount,

    /**
     * Trailing stop percentage (for trailing stop orders)
     */
    @DecimalMin(value = "0.01", message = "Trail percent must be at least 0.01")
    @DecimalMax(value = "100.00", message = "Trail percent cannot exceed 100%")
    BigDecimal trailPercent,

    /**
     * Entry price (for bracket orders)
     */
    @DecimalMin(value = "0.01", message = "Entry price must be at least 0.01")
    BigDecimal entryPrice,

    /**
     * Profit target price (for bracket orders)
     */
    @DecimalMin(value = "0.01", message = "Profit target must be at least 0.01")
    BigDecimal profitTarget,

    /**
     * Display quantity (for iceberg orders)
     */
    @Min(value = 1, message = "Display quantity must be at least 1")
    Integer displayQuantity,

    /**
     * Time window in minutes (for TWAP/VWAP orders)
     */
    @Min(value = 1, message = "Time window must be at least 1 minute")
    Integer timeWindowMinutes,

    /**
     * Slice interval in seconds (for TWAP orders)
     */
    @Min(value = 1, message = "Slice interval must be at least 1 second")
    Integer sliceIntervalSeconds,

    /**
     * Participation rate percentage (for VWAP orders)
     */
    @DecimalMin(value = "0.01", message = "Participation rate must be at least 0.01%")
    @DecimalMax(value = "100.00", message = "Participation rate cannot exceed 100%")
    BigDecimal participationRate
) {
    
    /**
     * Compact constructor for validation
     * Rule #3: Functional programming, no if-else
     */
    public OrderRequest {
        // Set default TimeInForce if null using functional pattern
        timeInForce = Optional.ofNullable(timeInForce)
            .orElse(TimeInForce.DAY);

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
     * Rule #3: Functional programming with pattern matching, no if-else
     */
    private void validatePriceRequirements() {
        // Validate limit price requirement using Optional pattern
        Optional.of(orderType)
            .filter(type -> type.requiresLimitPrice() && limitPrice == null)
            .ifPresent(type -> {
                throw new IllegalArgumentException(
                    String.format("Limit price is required for %s orders", type));
            });

        // Validate stop price requirement using Optional pattern
        Optional.of(orderType)
            .filter(type -> type.requiresStopPrice() && stopPrice == null)
            .ifPresent(type -> {
                throw new IllegalArgumentException(
                    String.format("Stop price is required for %s orders", type));
            });

        // Validate stop-limit order price relationship using pattern matching
        Optional.of(orderType)
            .filter(type -> type == OrderType.STOP_LIMIT && limitPrice != null && stopPrice != null)
            .ifPresent(type -> validateStopLimitPriceRelationship());
    }

    /**
     * Validate stop-limit price relationship based on order side
     * Rule #14: Pattern matching excellence
     * Rule #3: Functional programming with Optional pattern
     */
    private void validateStopLimitPriceRelationship() {
        // BUY orders: stop price must be >= limit price
        Optional.of(side)
            .filter(s -> s == OrderSide.BUY && stopPrice.compareTo(limitPrice) < 0)
            .ifPresent(s -> {
                throw new IllegalArgumentException(
                    "For buy stop-limit orders, stop price must be >= limit price");
            });

        // SELL orders: stop price must be <= limit price
        Optional.of(side)
            .filter(s -> s == OrderSide.SELL && stopPrice.compareTo(limitPrice) > 0)
            .ifPresent(s -> {
                throw new IllegalArgumentException(
                    "For sell stop-limit orders, stop price must be <= limit price");
            });
    }
    
    /**
     * Validate expiry date requirement for GTD orders
     * Rule #3: Functional programming with pattern matching, no if-else
     */
    private void validateExpiryDateRequirement() {
        // Validate GTD requires expiry date using Optional pattern
        Optional.of(timeInForce)
            .filter(tif -> tif == TimeInForce.GTD && expiryDate == null)
            .ifPresent(tif -> {
                throw new IllegalArgumentException("Expiry date is required for Good Till Date orders");
            });

        // Validate only GTD should have expiry date using Optional pattern
        Optional.of(timeInForce)
            .filter(tif -> tif != TimeInForce.GTD && expiryDate != null)
            .ifPresent(tif -> {
                throw new IllegalArgumentException("Expiry date should only be set for Good Till Date orders");
            });
    }
    
    /**
     * Validate order value limits
     * Rule #3: Functional programming with Optional pattern, no if-else
     */
    private void validateOrderValue() {
        BigDecimal maxOrderValue = BigDecimal.valueOf(10_000_000); // ₹1 Crore

        // Functional validation using Optional chain
        Optional.ofNullable(getEffectivePrice())
            .map(price -> price.multiply(BigDecimal.valueOf(quantity)))
            .filter(orderValue -> orderValue.compareTo(maxOrderValue) > 0)
            .ifPresent(orderValue -> {
                throw new IllegalArgumentException(
                    String.format("Order value ₹%.2f exceeds maximum allowed ₹%.2f",
                                 orderValue, maxOrderValue));
            });
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
     * Calculate estimated order value - eliminates ternary with Optional
     */
    public BigDecimal getEstimatedOrderValue() {
        BigDecimal price = getEffectivePrice();
        return Optional.ofNullable(price)
            .map(p -> p.multiply(BigDecimal.valueOf(quantity)))
            .orElse(BigDecimal.ZERO);
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
            .clientOrderRef(this.clientOrderRef)
            .trailAmount(this.trailAmount)
            .trailPercent(this.trailPercent)
            .entryPrice(this.entryPrice)
            .profitTarget(this.profitTarget)
            .displayQuantity(this.displayQuantity)
            .timeWindowMinutes(this.timeWindowMinutes)
            .sliceIntervalSeconds(this.sliceIntervalSeconds)
            .participationRate(this.participationRate);
    }

    /**
     * Convenience method for backward compatibility - maps to limitPrice
     */
    public BigDecimal price() {
        return this.limitPrice;
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
        private BigDecimal trailAmount;
        private BigDecimal trailPercent;
        private BigDecimal entryPrice;
        private BigDecimal profitTarget;
        private Integer displayQuantity;
        private Integer timeWindowMinutes;
        private Integer sliceIntervalSeconds;
        private BigDecimal participationRate;
        
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

        public OrderRequestBuilder trailAmount(BigDecimal trailAmount) {
            this.trailAmount = trailAmount;
            return this;
        }

        public OrderRequestBuilder trailPercent(BigDecimal trailPercent) {
            this.trailPercent = trailPercent;
            return this;
        }

        public OrderRequestBuilder entryPrice(BigDecimal entryPrice) {
            this.entryPrice = entryPrice;
            return this;
        }

        public OrderRequestBuilder profitTarget(BigDecimal profitTarget) {
            this.profitTarget = profitTarget;
            return this;
        }

        public OrderRequestBuilder displayQuantity(Integer displayQuantity) {
            this.displayQuantity = displayQuantity;
            return this;
        }

        public OrderRequestBuilder timeWindowMinutes(Integer timeWindowMinutes) {
            this.timeWindowMinutes = timeWindowMinutes;
            return this;
        }

        public OrderRequestBuilder sliceIntervalSeconds(Integer sliceIntervalSeconds) {
            this.sliceIntervalSeconds = sliceIntervalSeconds;
            return this;
        }

        public OrderRequestBuilder participationRate(BigDecimal participationRate) {
            this.participationRate = participationRate;
            return this;
        }

        public OrderRequest build() {
            return new OrderRequest(symbol, exchange, orderType, side, quantity,
                                  limitPrice, stopPrice, timeInForce, expiryDate,
                                  brokerName, clientOrderRef, trailAmount, trailPercent,
                                  entryPrice, profitTarget, displayQuantity,
                                  timeWindowMinutes, sliceIntervalSeconds, participationRate);
        }
    }
}