package com.trademaster.trading.execution;

import com.trademaster.common.functional.Result;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.routing.RoutingDecision;

import java.util.concurrent.CompletableFuture;

/**
 * Order Executor Interface
 *
 * MANDATORY: Rule #3 - Functional Programming (Result monad, CompletableFuture)
 * MANDATORY: Rule #12 - Virtual Threads & Concurrency
 * MANDATORY: Rule #25 - Circuit Breaker Implementation
 *
 * Interface for executing orders at broker APIs with functional error handling,
 * asynchronous processing, and circuit breaker protection.
 *
 * All implementations must:
 * - Use Result monad for error handling
 * - Execute operations asynchronously with CompletableFuture
 * - Implement circuit breakers for broker API calls
 * - Handle partial fills, rejections, and timeouts
 * - Ensure idempotency (prevent duplicate orders)
 * - Track execution metrics
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public interface OrderExecutor {

    /**
     * Execute order at broker API
     *
     * MANDATORY: Rule #11 - Result monad for error handling
     * MANDATORY: Rule #12 - CompletableFuture with virtual threads
     * MANDATORY: Rule #25 - Circuit breaker protected
     *
     * @param order Order to execute
     * @param routingDecision Routing decision with broker and strategy
     * @return CompletableFuture with execution result
     */
    CompletableFuture<Result<ExecutionResult, ExecutionError>> executeOrder(
            Order order,
            RoutingDecision routingDecision
    );

    /**
     * Check order execution status at broker
     *
     * @param orderId Order ID
     * @param brokerName Broker name
     * @return CompletableFuture with execution status
     */
    CompletableFuture<Result<ExecutionStatus, ExecutionError>> checkOrderStatus(
            String orderId,
            String brokerName
    );

    /**
     * Cancel order execution at broker
     *
     * @param orderId Order ID
     * @param brokerName Broker name
     * @return CompletableFuture with cancellation result
     */
    CompletableFuture<Result<CancellationResult, ExecutionError>> cancelOrder(
            String orderId,
            String brokerName
    );

    /**
     * Get executor name
     */
    String getExecutorName();

    /**
     * Get supported brokers
     */
    java.util.List<String> getSupportedBrokers();

    /**
     * Check if broker is supported
     */
    boolean supportsBroker(String brokerName);
}
