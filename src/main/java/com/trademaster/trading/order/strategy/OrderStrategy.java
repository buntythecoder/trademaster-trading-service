package com.trademaster.trading.order.strategy;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Order Strategy Interface for Advanced Order Types
 *
 * Defines the contract for implementing sophisticated order execution strategies
 * including Stop-Loss, Trailing Stop, Bracket Orders, and algorithmic execution.
 *
 * Strategy Pattern:
 * - Encapsulates order execution logic in separate strategy classes
 * - Allows runtime strategy selection based on order type
 * - Enables addition of new strategies without modifying existing code
 *
 * All strategies must:
 * - Be immutable and thread-safe
 * - Support Virtual Thread execution
 * - Provide validation and risk checks
 * - Handle market data updates
 * - Support cancellation and modification
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
public sealed interface OrderStrategy
    permits StopLossStrategy,
            TrailingStopStrategy,
            BracketOrderStrategy,
            IcebergOrderStrategy,
            TWAPStrategy,
            VWAPStrategy {

    /**
     * Gets the strategy type identifier.
     *
     * @return Strategy type (STOP_LOSS, TRAILING_STOP, BRACKET, ICEBERG, TWAP, VWAP)
     */
    StrategyType getStrategyType();

    /**
     * Validates order parameters for this strategy.
     *
     * @param request Order request to validate
     * @return Validation result with error messages if invalid
     */
    ValidationResult validate(OrderRequest request);

    /**
     * Executes the order using this strategy.
     *
     * @param request Order request to execute
     * @return CompletableFuture with order response
     */
    CompletableFuture<OrderResponse> execute(OrderRequest request);

    /**
     * Handles market price updates for active orders.
     *
     * @param orderId Active order identifier
     * @param currentPrice Current market price
     * @return CompletableFuture with true if order should be triggered
     */
    CompletableFuture<Boolean> onPriceUpdate(String orderId, BigDecimal currentPrice);

    /**
     * Cancels an active order managed by this strategy.
     *
     * @param orderId Order identifier to cancel
     * @return CompletableFuture with cancellation result
     */
    CompletableFuture<OrderResponse> cancel(String orderId);

    /**
     * Modifies an active order's parameters.
     *
     * @param orderId Order identifier to modify
     * @param newParameters Updated order parameters
     * @return CompletableFuture with modification result
     */
    CompletableFuture<OrderResponse> modify(String orderId, OrderRequest newParameters);

    /**
     * Strategy type enumeration for all supported order strategies.
     */
    enum StrategyType {
        STOP_LOSS("Stop-Loss Order"),
        TRAILING_STOP("Trailing Stop Order"),
        BRACKET("Bracket Order"),
        ICEBERG("Iceberg Order"),
        TWAP("Time-Weighted Average Price"),
        VWAP("Volume-Weighted Average Price");

        private final String description;

        StrategyType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Validation result record for order validation.
     *
     * @param isValid True if order is valid
     * @param errorMessage Error message if invalid
     */
    record ValidationResult(
        boolean isValid,
        String errorMessage
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}
