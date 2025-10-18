package com.trademaster.trading.dto.event;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Filled Event DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Event published when an order is completely filled by the broker.
 * Triggers position updates in portfolio-service.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderFilledEvent(

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
     * Filled quantity
     */
    Integer filledQuantity,

    /**
     * Average execution price
     */
    BigDecimal avgExecutionPrice,

    /**
     * Total execution value
     */
    BigDecimal executionValue,

    /**
     * Broker fees
     */
    BigDecimal fees,

    /**
     * Net execution value (after fees)
     */
    BigDecimal netValue,

    /**
     * Broker name
     */
    String brokerName,

    /**
     * Execution timestamp
     */
    LocalDateTime executedAt,

    /**
     * Event timestamp
     */
    LocalDateTime timestamp
) {

    /**
     * Default event type constant
     */
    public static final String EVENT_TYPE = "ORDER_FILLED";

    /**
     * Calculate net profit/loss for sell orders
     */
    public BigDecimal calculateNetProfitLoss(BigDecimal costBasis) {
        return switch (side) {
            case SELL -> netValue.subtract(costBasis);
            case BUY -> BigDecimal.ZERO;
        };
    }

    /**
     * Check if this is a buy order
     */
    public boolean isBuyOrder() {
        return side == OrderSide.BUY;
    }

    /**
     * Check if this is a sell order
     */
    public boolean isSellOrder() {
        return side == OrderSide.SELL;
    }

    /**
     * Get fee percentage
     */
    public BigDecimal getFeePercentage() {
        return fees.divide(executionValue, 4, java.math.RoundingMode.HALF_UP)
                   .multiply(BigDecimal.valueOf(100));
    }
}
