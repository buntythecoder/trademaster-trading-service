package com.trademaster.trading.execution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Execution Result Record
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Immutable record representing the result of order execution at broker API.
 * Contains comprehensive execution details including fills, status, and metrics.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record ExecutionResult(
        String brokerOrderId,
        String symbol,
        String brokerName,
        ExecutionStatus status,
        Integer requestedQuantity,
        Integer executedQuantity,
        Integer remainingQuantity,
        BigDecimal averagePrice,
        BigDecimal totalValue,
        List<FillDetail> fills,
        Instant executionTime,
        long latencyMillis,
        String message
) {

    /**
     * Fill detail record - eliminates if-statements with Optional validation
     */
    public record FillDetail(
            String fillId,
            Instant fillTime,
            Integer fillQuantity,
            BigDecimal fillPrice,
            BigDecimal fillValue,
            BigDecimal commission
    ) {
        public FillDetail {
            // Eliminate if-statements with Optional.filter().orElseThrow()
            Optional.ofNullable(fillQuantity)
                .filter(qty -> qty > 0)
                .orElseThrow(() -> new IllegalArgumentException("Fill quantity must be positive"));

            Optional.ofNullable(fillPrice)
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .orElseThrow(() -> new IllegalArgumentException("Fill price must be positive"));
        }
    }

    public ExecutionResult {
        // Eliminate if-statements with Optional.filter().orElseThrow()
        Optional.ofNullable(symbol)
            .filter(s -> !s.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("Symbol cannot be null or blank"));

        Optional.ofNullable(brokerName)
            .filter(b -> !b.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("Broker name cannot be null or blank"));

        Optional.ofNullable(status)
            .orElseThrow(() -> new IllegalArgumentException("Status cannot be null"));

        Optional.ofNullable(executionTime)
            .orElseThrow(() -> new IllegalArgumentException("Execution time cannot be null"));

        fills = Optional.ofNullable(fills).map(List::copyOf).orElse(List.of());
    }

    /**
     * Check if execution is complete
     */
    public boolean isComplete() {
        return status == ExecutionStatus.FILLED ||
               status == ExecutionStatus.CANCELLED ||
               status == ExecutionStatus.REJECTED;
    }

    /**
     * Check if execution was successful
     */
    public boolean isSuccessful() {
        return status == ExecutionStatus.FILLED ||
               status == ExecutionStatus.PARTIAL_FILL;
    }

    /**
     * Get fill rate percentage - eliminates if-statement with Optional
     */
    public BigDecimal getFillRate() {
        return Optional.ofNullable(requestedQuantity)
            .filter(qty -> qty != 0)
            .map(requested -> {
                Integer executed = Optional.ofNullable(executedQuantity).orElse(0);
                return BigDecimal.valueOf(executed)
                    .divide(BigDecimal.valueOf(requested), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            })
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Calculate total commission
     */
    public BigDecimal getTotalCommission() {
        return fills.stream()
                .map(FillDetail::commission)
                .filter(commission -> commission != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if execution had high latency
     */
    public boolean hasHighLatency() {
        return latencyMillis > 1000; // >1 second is high latency
    }

    /**
     * Factory method for successful execution
     */
    public static ExecutionResult success(
            String brokerOrderId,
            String symbol,
            String brokerName,
            Integer requestedQuantity,
            Integer executedQuantity,
            BigDecimal averagePrice,
            List<FillDetail> fills,
            Instant executionTime,
            long latencyMillis) {

        BigDecimal totalValue = Optional.ofNullable(averagePrice)
                .flatMap(price -> Optional.ofNullable(executedQuantity)
                    .map(qty -> price.multiply(BigDecimal.valueOf(qty))))
                .orElse(BigDecimal.ZERO);

        ExecutionStatus status = Optional.of(executedQuantity.equals(requestedQuantity))
                .filter(Boolean::booleanValue)
                .map(match -> ExecutionStatus.FILLED)
                .orElse(ExecutionStatus.PARTIAL_FILL);

        Integer remaining = requestedQuantity - executedQuantity;

        return new ExecutionResult(
                brokerOrderId,
                symbol,
                brokerName,
                status,
                requestedQuantity,
                executedQuantity,
                remaining,
                averagePrice,
                totalValue,
                fills,
                executionTime,
                latencyMillis,
                "Order executed successfully"
        );
    }

    /**
     * Factory method for pending execution
     */
    public static ExecutionResult pending(
            String brokerOrderId,
            String symbol,
            String brokerName,
            Integer requestedQuantity,
            Instant executionTime,
            long latencyMillis) {

        return new ExecutionResult(
                brokerOrderId,
                symbol,
                brokerName,
                ExecutionStatus.PENDING,
                requestedQuantity,
                0,
                requestedQuantity,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                executionTime,
                latencyMillis,
                "Order placed and pending execution"
        );
    }

    /**
     * Factory method for rejected execution
     */
    public static ExecutionResult rejected(
            String symbol,
            String brokerName,
            Integer requestedQuantity,
            String rejectionReason,
            Instant executionTime,
            long latencyMillis) {

        return new ExecutionResult(
                null,
                symbol,
                brokerName,
                ExecutionStatus.REJECTED,
                requestedQuantity,
                0,
                requestedQuantity,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                executionTime,
                latencyMillis,
                rejectionReason
        );
    }
}
