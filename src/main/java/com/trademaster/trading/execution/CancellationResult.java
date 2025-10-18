package com.trademaster.trading.execution;

import java.time.Instant;
import java.util.Optional;

/**
 * Cancellation Result Record
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Immutable record representing the result of order cancellation at broker API.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record CancellationResult(
        String orderId,
        String brokerOrderId,
        String brokerName,
        boolean cancelled,
        String message,
        Instant cancellationTime
) {

    public CancellationResult {
        // Eliminates if-statement using Optional.ofNullable().filter().orElseThrow()
        Optional.ofNullable(brokerName)
            .filter(name -> !name.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("Broker name cannot be null or blank"));

        // Eliminates if-statement using Optional.ofNullable().orElseThrow()
        Optional.ofNullable(cancellationTime)
            .orElseThrow(() -> new IllegalArgumentException("Cancellation time cannot be null"));
    }

    /**
     * Factory method for successful cancellation
     */
    public static CancellationResult success(
            String orderId,
            String brokerOrderId,
            String brokerName,
            Instant cancellationTime) {

        return new CancellationResult(
                orderId,
                brokerOrderId,
                brokerName,
                true,
                "Order cancelled successfully",
                cancellationTime
        );
    }

    /**
     * Factory method for failed cancellation
     */
    public static CancellationResult failure(
            String orderId,
            String brokerName,
            String failureReason,
            Instant cancellationTime) {

        return new CancellationResult(
                orderId,
                null,
                brokerName,
                false,
                failureReason,
                cancellationTime
        );
    }
}
