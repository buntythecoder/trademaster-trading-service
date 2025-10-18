package com.trademaster.trading.execution;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Execution Error Record
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Sealed interface for order execution errors using pattern matching.
 * All execution errors are immutable Records.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface ExecutionError permits
    ExecutionError.BrokerApiError,
    ExecutionError.OrderRejectedError,
    ExecutionError.TimeoutError,
    ExecutionError.PartialFillError,
    ExecutionError.InsufficientLiquidityError,
    ExecutionError.IdempotencyViolationError,
    ExecutionError.SystemError {

    /**
     * Get error message for display
     */
    String message();

    /**
     * Get error code for logging and metrics
     */
    String code();

    /**
     * Get error severity
     */
    ErrorSeverity severity();

    /**
     * Get retry eligibility
     */
    boolean retryable();

    /**
     * Error severity levels
     */
    enum ErrorSeverity {
        LOW,      // Warning only, execution may continue
        MEDIUM,   // Caution required, retry recommended
        HIGH,     // Execution failed, immediate attention
        CRITICAL  // System error, urgent intervention required
    }

    /**
     * Broker API error
     */
    record BrokerApiError(
            String brokerName,
            int httpStatusCode,
            String apiErrorCode,
            String apiErrorMessage,
            String message
    ) implements ExecutionError {
        @Override
        public String code() {
            return "BROKER_API_ERROR";
        }

        @Override
        public ErrorSeverity severity() {
            return Optional.of(httpStatusCode >= 500)
                .filter(Boolean::booleanValue)
                .map(isServerError -> ErrorSeverity.HIGH)
                .orElse(ErrorSeverity.MEDIUM);
        }

        @Override
        public boolean retryable() {
            return httpStatusCode >= 500 || httpStatusCode == 429; // Retry on 5xx or rate limit
        }
    }

    /**
     * Order rejected error
     */
    record OrderRejectedError(
            String brokerName,
            String rejectionReason,
            String rejectionCode,
            String message
    ) implements ExecutionError {
        @Override
        public String code() {
            return "ORDER_REJECTED";
        }

        @Override
        public ErrorSeverity severity() {
            return ErrorSeverity.HIGH;
        }

        @Override
        public boolean retryable() {
            return false; // Business rejections are not retryable
        }
    }

    /**
     * Timeout error
     */
    record TimeoutError(
            String brokerName,
            long timeoutMillis,
            String operation,
            String message
    ) implements ExecutionError {
        @Override
        public String code() {
            return "EXECUTION_TIMEOUT";
        }

        @Override
        public ErrorSeverity severity() {
            return ErrorSeverity.HIGH;
        }

        @Override
        public boolean retryable() {
            return true; // Timeouts can be retried
        }
    }

    /**
     * Partial fill error
     */
    record PartialFillError(
            String brokerName,
            Integer requestedQuantity,
            Integer filledQuantity,
            String message
    ) implements ExecutionError {
        @Override
        public String code() {
            return "PARTIAL_FILL";
        }

        @Override
        public ErrorSeverity severity() {
            return ErrorSeverity.MEDIUM;
        }

        @Override
        public boolean retryable() {
            return true; // Can retry for remaining quantity
        }
    }

    /**
     * Insufficient liquidity error
     */
    record InsufficientLiquidityError(
            String symbol,
            String exchange,
            Integer requestedQuantity,
            BigDecimal availableLiquidity,
            String message
    ) implements ExecutionError {
        @Override
        public String code() {
            return "INSUFFICIENT_LIQUIDITY";
        }

        @Override
        public ErrorSeverity severity() {
            return ErrorSeverity.HIGH;
        }

        @Override
        public boolean retryable() {
            return false; // Market condition, not retryable immediately
        }
    }

    /**
     * Idempotency violation error
     */
    record IdempotencyViolationError(
            String orderId,
            String duplicateExecutionId,
            String message
    ) implements ExecutionError {
        @Override
        public String code() {
            return "IDEMPOTENCY_VIOLATION";
        }

        @Override
        public ErrorSeverity severity() {
            return ErrorSeverity.CRITICAL;
        }

        @Override
        public boolean retryable() {
            return false; // Duplicate execution, must not retry
        }
    }

    /**
     * System error
     */
    record SystemError(
            String message,
            String cause
    ) implements ExecutionError {
        @Override
        public String code() {
            return "SYSTEM_ERROR";
        }

        @Override
        public ErrorSeverity severity() {
            return ErrorSeverity.CRITICAL;
        }

        @Override
        public boolean retryable() {
            return true; // System errors can be retried after investigation
        }
    }

    // Factory methods for common errors
    static BrokerApiError brokerApiError(
            String brokerName,
            int httpStatusCode,
            String apiErrorCode,
            String apiErrorMessage) {
        return new BrokerApiError(
                brokerName,
                httpStatusCode,
                apiErrorCode,
                apiErrorMessage,
                "Broker API error from " + brokerName + ": " + apiErrorMessage + " (HTTP " + httpStatusCode + ")"
        );
    }

    static OrderRejectedError orderRejected(
            String brokerName,
            String rejectionReason,
            String rejectionCode) {
        return new OrderRejectedError(
                brokerName,
                rejectionReason,
                rejectionCode,
                "Order rejected by " + brokerName + ": " + rejectionReason + " (Code: " + rejectionCode + ")"
        );
    }

    static TimeoutError timeout(
            String brokerName,
            long timeoutMillis,
            String operation) {
        return new TimeoutError(
                brokerName,
                timeoutMillis,
                operation,
                "Timeout executing " + operation + " with " + brokerName + " after " + timeoutMillis + "ms"
        );
    }

    static PartialFillError partialFill(
            String brokerName,
            Integer requestedQuantity,
            Integer filledQuantity) {
        return new PartialFillError(
                brokerName,
                requestedQuantity,
                filledQuantity,
                "Partial fill: requested " + requestedQuantity + ", filled " + filledQuantity
        );
    }

    static InsufficientLiquidityError insufficientLiquidity(
            String symbol,
            String exchange,
            Integer requestedQuantity,
            BigDecimal availableLiquidity) {
        return new InsufficientLiquidityError(
                symbol,
                exchange,
                requestedQuantity,
                availableLiquidity,
                "Insufficient liquidity for " + symbol + " on " + exchange +
                ": requested " + requestedQuantity + ", available " + availableLiquidity
        );
    }

    static IdempotencyViolationError idempotencyViolation(
            String orderId,
            String duplicateExecutionId) {
        return new IdempotencyViolationError(
                orderId,
                duplicateExecutionId,
                "Idempotency violation: order " + orderId + " already executed with ID " + duplicateExecutionId
        );
    }

    static SystemError systemError(String message, String cause) {
        return new SystemError(message, cause);
    }
}
