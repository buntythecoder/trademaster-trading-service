package com.trademaster.trading.dto.integration;

import com.trademaster.trading.model.OrderSide;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Position Update DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Data transfer object for updating positions in portfolio service.
 * Sent after order execution to maintain position consistency.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record PositionUpdate(

    /**
     * User ID
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
     * Order side (BUY or SELL)
     */
    OrderSide side,

    /**
     * Quantity executed
     */
    Integer quantity,

    /**
     * Execution price
     */
    BigDecimal executionPrice,

    /**
     * Order ID
     */
    String orderId,

    /**
     * Broker order ID
     */
    String brokerOrderId,

    /**
     * Execution timestamp
     */
    LocalDateTime executedAt,

    /**
     * Order value (quantity * execution price)
     */
    BigDecimal orderValue,

    /**
     * Transaction fees
     */
    BigDecimal fees,

    /**
     * Correlation ID for tracing
     */
    String correlationId
) {

    /**
     * Calculate net order value (including fees)
     */
    public BigDecimal netOrderValue() {
        return switch (side) {
            case BUY -> orderValue.add(fees);
            case SELL -> orderValue.subtract(fees);
        };
    }

    /**
     * Check if this is a position-opening trade
     */
    public boolean isOpeningTrade() {
        return side == OrderSide.BUY;
    }

    /**
     * Check if this is a position-closing trade
     */
    public boolean isClosingTrade() {
        return side == OrderSide.SELL;
    }
}
