package com.trademaster.trading.dto.event;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Cancelled Event DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Event published when an order is cancelled by user or system.
 * Used for cleanup and notification purposes.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderCancelledEvent(

    /**
     * Event correlation ID for distributed tracing
     */
    String correlationId,

    /**
     * Event type identifier
     */
    String eventType,

    /**
     * User ID who placed the order
     */
    Long userId,

    /**
     * Trading order ID
     */
    String orderId,

    /**
     * Broker order ID
     */
    String brokerOrderId,

    /**
     * Symbol
     */
    String symbol,

    /**
     * Exchange
     */
    String exchange,

    /**
     * Order side (BUY/SELL)
     */
    OrderSide side,

    /**
     * Order type (MARKET/LIMIT/STOP_LOSS)
     */
    OrderType orderType,

    /**
     * Original quantity
     */
    Integer originalQuantity,

    /**
     * Filled quantity before cancellation
     */
    Integer filledQuantity,

    /**
     * Remaining quantity cancelled
     */
    Integer remainingQuantity,

    /**
     * Original order price
     */
    BigDecimal price,

    /**
     * Cancellation reason
     */
    String reason,

    /**
     * Whether cancelled by user or system
     */
    CancellationSource source,

    /**
     * Broker name
     */
    String brokerName,

    /**
     * Cancellation timestamp
     */
    LocalDateTime cancelledAt,

    /**
     * Event timestamp
     */
    LocalDateTime timestamp
) {

    /**
     * Default event type constant
     */
    public static final String EVENT_TYPE = "ORDER_CANCELLED";

    /**
     * Cancellation source enumeration
     */
    public enum CancellationSource {
        USER_REQUEST,
        SYSTEM_AUTO,
        RISK_MANAGEMENT,
        TIMEOUT,
        BROKER_REJECTION
    }

    /**
     * Check if order was partially filled
     */
    public boolean wasPartiallyFilled() {
        return filledQuantity > 0;
    }

    /**
     * Check if order was never filled
     */
    public boolean wasNeverFilled() {
        return filledQuantity == 0;
    }

    /**
     * Check if cancelled by user
     */
    public boolean wasCancelledByUser() {
        return source == CancellationSource.USER_REQUEST;
    }

    /**
     * Get fill percentage
     */
    public BigDecimal getFillPercentage() {
        return BigDecimal.valueOf(filledQuantity)
                        .divide(BigDecimal.valueOf(originalQuantity), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
    }
}
