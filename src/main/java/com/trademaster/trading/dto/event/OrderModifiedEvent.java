package com.trademaster.trading.dto.event;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Modified Event DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Event published when an order is modified (quantity, price, type change).
 * Used for audit trail and position recalculation.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderModifiedEvent(

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
     * Old order type
     */
    OrderType oldOrderType,

    /**
     * New order type
     */
    OrderType newOrderType,

    /**
     * Old quantity
     */
    Integer oldQuantity,

    /**
     * New quantity
     */
    Integer newQuantity,

    /**
     * Old price
     */
    BigDecimal oldPrice,

    /**
     * New price
     */
    BigDecimal newPrice,

    /**
     * Old stop loss
     */
    BigDecimal oldStopLoss,

    /**
     * New stop loss
     */
    BigDecimal newStopLoss,

    /**
     * Old take profit
     */
    BigDecimal oldTakeProfit,

    /**
     * New take profit
     */
    BigDecimal newTakeProfit,

    /**
     * Modification reason
     */
    String reason,

    /**
     * Broker name
     */
    String brokerName,

    /**
     * Modification timestamp
     */
    LocalDateTime modifiedAt,

    /**
     * Event timestamp
     */
    LocalDateTime timestamp
) {

    /**
     * Default event type constant
     */
    public static final String EVENT_TYPE = "ORDER_MODIFIED";

    /**
     * Check if quantity changed
     */
    public boolean isQuantityChanged() {
        return !oldQuantity.equals(newQuantity);
    }

    /**
     * Check if price changed
     */
    public boolean isPriceChanged() {
        return (oldPrice != null && newPrice != null && oldPrice.compareTo(newPrice) != 0) ||
               (oldPrice == null && newPrice != null) ||
               (oldPrice != null && newPrice == null);
    }

    /**
     * Check if order type changed
     */
    public boolean isOrderTypeChanged() {
        return oldOrderType != newOrderType;
    }

    /**
     * Check if stop loss changed
     */
    public boolean isStopLossChanged() {
        return (oldStopLoss != null && newStopLoss != null && oldStopLoss.compareTo(newStopLoss) != 0) ||
               (oldStopLoss == null && newStopLoss != null) ||
               (oldStopLoss != null && newStopLoss == null);
    }

    /**
     * Check if take profit changed
     */
    public boolean isTakeProfitChanged() {
        return (oldTakeProfit != null && newTakeProfit != null && oldTakeProfit.compareTo(newTakeProfit) != 0) ||
               (oldTakeProfit == null && newTakeProfit != null) ||
               (oldTakeProfit != null && newTakeProfit == null);
    }

    /**
     * Get quantity change
     */
    public Integer getQuantityChange() {
        return newQuantity - oldQuantity;
    }
}
