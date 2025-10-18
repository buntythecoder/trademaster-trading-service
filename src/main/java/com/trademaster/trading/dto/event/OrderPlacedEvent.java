package com.trademaster.trading.dto.event;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Placed Event DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Event published when an order is successfully placed to the broker.
 * Used for event-driven communication with portfolio-service and other services.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderPlacedEvent(

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
     * Symbol traded
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
     * Quantity
     */
    Integer quantity,

    /**
     * Order price (null for market orders)
     */
    BigDecimal price,

    /**
     * Stop loss price (null if not applicable)
     */
    BigDecimal stopLoss,

    /**
     * Take profit price (null if not applicable)
     */
    BigDecimal takeProfit,

    /**
     * Order value
     */
    BigDecimal orderValue,

    /**
     * Broker name
     */
    String brokerName,

    /**
     * Event timestamp
     */
    LocalDateTime timestamp,

    /**
     * Order placement source
     */
    String source
) {

    /**
     * Default event type constant
     */
    public static final String EVENT_TYPE = "ORDER_PLACED";

    /**
     * Create from OrderRequest
     */
    public static OrderPlacedEvent from(
            OrderRequest request,
            Long userId,
            String orderId,
            String brokerOrderId,
            String correlationId) {

        return new OrderPlacedEvent(
            correlationId,
            EVENT_TYPE,
            userId,
            orderId,
            brokerOrderId,
            request.symbol(),
            request.exchange(),
            request.side(),
            request.orderType(),
            request.quantity(),
            request.limitPrice(),  // Use limitPrice instead of price
            request.stopPrice(),   // Use stopPrice instead of stopLoss
            null,  // takeProfit not in OrderRequest
            request.getEstimatedOrderValue(),  // Use getEstimatedOrderValue
            request.brokerName(),  // Use brokerName instead of brokerPreference
            LocalDateTime.now(),
            "TRADING_SERVICE"
        );
    }

    /**
     * Check if this is a market order
     */
    public boolean isMarketOrder() {
        return orderType == OrderType.MARKET;
    }

    /**
     * Check if this is a stop loss order
     */
    public boolean hasStopLoss() {
        return stopLoss != null && stopLoss.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if this is a take profit order
     */
    public boolean hasTakeProfit() {
        return takeProfit != null && takeProfit.compareTo(BigDecimal.ZERO) > 0;
    }
}
