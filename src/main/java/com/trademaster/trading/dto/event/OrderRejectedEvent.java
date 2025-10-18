package com.trademaster.trading.dto.event;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Rejected Event DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Event published when an order is rejected by validation, risk checks, or broker.
 * Used for analytics, alerting, and user notification.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record OrderRejectedEvent(

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
     * Broker order ID (may be null if rejected before submission)
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
     * Quantity
     */
    Integer quantity,

    /**
     * Price
     */
    BigDecimal price,

    /**
     * Order value
     */
    BigDecimal orderValue,

    /**
     * Rejection reason
     */
    String reason,

    /**
     * Rejection stage
     */
    RejectionStage stage,

    /**
     * Error code
     */
    String errorCode,

    /**
     * Broker name (may be null if rejected before routing)
     */
    String brokerName,

    /**
     * Rejection timestamp
     */
    LocalDateTime rejectedAt,

    /**
     * Event timestamp
     */
    LocalDateTime timestamp
) {

    /**
     * Default event type constant
     */
    public static final String EVENT_TYPE = "ORDER_REJECTED";

    /**
     * Rejection stage enumeration
     */
    public enum RejectionStage {
        VALIDATION,
        RISK_CHECK,
        ROUTING,
        BROKER_SUBMISSION,
        BROKER_PROCESSING
    }

    /**
     * Create from OrderRequest
     */
    public static OrderRejectedEvent from(
            OrderRequest request,
            Long userId,
            String orderId,
            String reason,
            RejectionStage stage,
            String errorCode,
            String correlationId) {

        return new OrderRejectedEvent(
            correlationId,
            EVENT_TYPE,
            userId,
            orderId,
            null,  // No broker order ID for rejected orders
            request.symbol(),
            request.exchange(),
            request.side(),
            request.orderType(),
            request.quantity(),
            request.limitPrice(),  // Use limitPrice instead of price
            request.getEstimatedOrderValue(),  // Use getEstimatedOrderValue
            reason,
            stage,
            errorCode,
            request.brokerName(),  // Use brokerName instead of brokerPreference
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    /**
     * Check if rejected during validation
     */
    public boolean isValidationRejection() {
        return stage == RejectionStage.VALIDATION;
    }

    /**
     * Check if rejected during risk check
     */
    public boolean isRiskRejection() {
        return stage == RejectionStage.RISK_CHECK;
    }

    /**
     * Check if rejected by broker
     */
    public boolean isBrokerRejection() {
        return stage == RejectionStage.BROKER_SUBMISSION ||
               stage == RejectionStage.BROKER_PROCESSING;
    }

    /**
     * Check if rejected during routing
     */
    public boolean isRoutingRejection() {
        return stage == RejectionStage.ROUTING;
    }
}
