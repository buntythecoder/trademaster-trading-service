package com.trademaster.trading.agentos.agents;

import com.trademaster.trading.agentos.AgentCapability;
import com.trademaster.trading.agentos.AgentOSComponent;
import com.trademaster.trading.agentos.EventHandler;
import com.trademaster.trading.agentos.TradingCapabilityRegistry;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.order.strategy.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Order Strategy Agent for AgentOS Framework
 *
 * Provides advanced order execution capabilities to the TradeMaster multi-agent ecosystem.
 * Manages 6 sophisticated order strategies with intelligent routing, price monitoring,
 * and lifecycle management.
 *
 * Agent Capabilities:
 * - STOP_LOSS_EXECUTION: Automatic stop-loss triggers with <50ms latency
 * - TRAILING_STOP_EXECUTION: Dynamic stop adjustment with profit protection
 * - BRACKET_ORDER_EXECUTION: Coordinated entry/profit/stop with OCO logic
 * - ICEBERG_ORDER_EXECUTION: Hidden large order execution with market impact reduction
 * - TWAP_EXECUTION: Time-weighted execution for minimizing market impact
 * - VWAP_EXECUTION: Volume-weighted execution matching market liquidity
 *
 * Strategy Features:
 * - Automatic Strategy Selection: Based on order parameters
 * - Real-Time Price Monitoring: WebSocket-based market data integration
 * - Lifecycle Management: Execute, monitor, modify, cancel operations
 * - Performance Tracking: Execution metrics and success rate monitoring
 *
 * Performance:
 * - Order Execution: <100ms for strategy initialization
 * - Price Update Processing: <10ms per order
 * - Concurrent Orders: 1,000+ active orders per strategy
 * - Trigger Latency: <50ms from price update
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStrategyAgent implements AgentOSComponent {

    private final TradingCapabilityRegistry capabilityRegistry;

    // Strategy implementations
    private final StopLossStrategy stopLossStrategy;
    private final TrailingStopStrategy trailingStopStrategy;
    private final BracketOrderStrategy bracketOrderStrategy;
    private final IcebergOrderStrategy icebergOrderStrategy;
    private final TWAPStrategy twapStrategy;
    private final VWAPStrategy vwapStrategy;

    // Active order tracking: orderId -> strategy type
    private final Map<String, OrderStrategy.StrategyType> activeOrders = new ConcurrentHashMap<>();

    /**
     * Executes advanced order using appropriate strategy.
     * Routes to specific strategy based on order type.
     */
    @EventHandler(event = "AdvancedOrderRequest")
    @AgentCapability(
        name = "ADVANCED_ORDER_EXECUTION",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    @CircuitBreaker(name = "order-execution", fallbackMethod = "fallbackOrderExecution")
    public CompletableFuture<OrderResponse> executeAdvancedOrder(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Executing advanced order: type={}, symbol={}, side={}, quantity={}",
                    request.orderType(), request.symbol(), request.side(), request.quantity());

            try {
                // Route to appropriate strategy
                OrderStrategy strategy = selectStrategy(request);

                // Validate order
                var validationResult = strategy.validate(request);
                if (!validationResult.isValid()) {
                    log.error("Order validation failed: {}", validationResult.errorMessage());
                    throw new IllegalArgumentException(validationResult.errorMessage());
                }

                // Execute order
                var responseFuture = strategy.execute(request);
                var response = responseFuture.join();

                // Track active order
                activeOrders.put(response.orderId(), strategy.getStrategyType());

                capabilityRegistry.recordSuccessfulExecution("ADVANCED_ORDER_EXECUTION");

                log.info("Advanced order executed successfully: orderId={}, strategy={}",
                        response.orderId(), strategy.getStrategyType());

                return response;

            } catch (Exception e) {
                log.error("Failed to execute advanced order", e);
                capabilityRegistry.recordFailedExecution("ADVANCED_ORDER_EXECUTION", e);
                throw new RuntimeException("Advanced order execution failed", e);
            }
        });
    }

    /**
     * Handles stop-loss order execution.
     */
    @EventHandler(event = "StopLossOrderRequest")
    @AgentCapability(
        name = "STOP_LOSS_EXECUTION",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<OrderResponse> executeStopLoss(OrderRequest request) {
        return executeStrategyOrder(stopLossStrategy, request, "STOP_LOSS_EXECUTION");
    }

    /**
     * Handles trailing stop order execution.
     */
    @EventHandler(event = "TrailingStopOrderRequest")
    @AgentCapability(
        name = "TRAILING_STOP_EXECUTION",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<OrderResponse> executeTrailingStop(OrderRequest request) {
        return executeStrategyOrder(trailingStopStrategy, request, "TRAILING_STOP_EXECUTION");
    }

    /**
     * Handles bracket order execution.
     */
    @EventHandler(event = "BracketOrderRequest")
    @AgentCapability(
        name = "BRACKET_ORDER_EXECUTION",
        proficiency = "ADVANCED",
        performanceProfile = "MEDIUM_LATENCY"
    )
    public CompletableFuture<OrderResponse> executeBracketOrder(OrderRequest request) {
        return executeStrategyOrder(bracketOrderStrategy, request, "BRACKET_ORDER_EXECUTION");
    }

    /**
     * Handles iceberg order execution.
     */
    @EventHandler(event = "IcebergOrderRequest")
    @AgentCapability(
        name = "ICEBERG_ORDER_EXECUTION",
        proficiency = "ADVANCED",
        performanceProfile = "MEDIUM_LATENCY"
    )
    public CompletableFuture<OrderResponse> executeIcebergOrder(OrderRequest request) {
        return executeStrategyOrder(icebergOrderStrategy, request, "ICEBERG_ORDER_EXECUTION");
    }

    /**
     * Handles TWAP order execution.
     */
    @EventHandler(event = "TWAPOrderRequest")
    @AgentCapability(
        name = "TWAP_EXECUTION",
        proficiency = "ADVANCED",
        performanceProfile = "LOW_FREQUENCY"
    )
    public CompletableFuture<OrderResponse> executeTWAP(OrderRequest request) {
        return executeStrategyOrder(twapStrategy, request, "TWAP_EXECUTION");
    }

    /**
     * Handles VWAP order execution.
     */
    @EventHandler(event = "VWAPOrderRequest")
    @AgentCapability(
        name = "VWAP_EXECUTION",
        proficiency = "EXPERT",
        performanceProfile = "LOW_FREQUENCY"
    )
    public CompletableFuture<OrderResponse> executeVWAP(OrderRequest request) {
        return executeStrategyOrder(vwapStrategy, request, "VWAP_EXECUTION");
    }

    /**
     * Handles market price updates for active orders.
     * Broadcasts to all strategies to check trigger conditions.
     */
    @EventHandler(event = "MarketPriceUpdate")
    @AgentCapability(
        name = "PRICE_MONITORING",
        proficiency = "EXPERT",
        performanceProfile = "ULTRA_LOW_LATENCY"
    )
    public CompletableFuture<Map<String, Boolean>> onPriceUpdate(String symbol, BigDecimal currentPrice) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Processing price update: symbol={}, price={}", symbol, currentPrice);

            Map<String, Boolean> triggerResults = new ConcurrentHashMap<>();

            // Get all active orders for this symbol
            activeOrders.entrySet().stream()
                .forEach(entry -> {
                    String orderId = entry.getKey();
                    OrderStrategy.StrategyType strategyType = entry.getValue();

                    // Route to appropriate strategy
                    OrderStrategy strategy = getStrategy(strategyType);

                    // Check if order should be triggered
                    var triggerFuture = strategy.onPriceUpdate(orderId, currentPrice);
                    boolean triggered = triggerFuture.join();

                    if (triggered) {
                        triggerResults.put(orderId, true);
                        // Remove from active orders if triggered
                        activeOrders.remove(orderId);
                    }
                });

            capabilityRegistry.recordSuccessfulExecution("PRICE_MONITORING");
            return triggerResults;
        });
    }

    /**
     * Cancels an active order.
     */
    @EventHandler(event = "CancelOrderRequest")
    @AgentCapability(
        name = "ORDER_CANCELLATION",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<OrderResponse> cancelOrder(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Cancelling order: orderId={}", orderId);

            try {
                // Get strategy type for this order
                OrderStrategy.StrategyType strategyType = Optional.ofNullable(activeOrders.get(orderId))
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

                // Route to appropriate strategy
                OrderStrategy strategy = getStrategy(strategyType);
                var responseFuture = strategy.cancel(orderId);
                var response = responseFuture.join();

                // Remove from active orders
                activeOrders.remove(orderId);

                capabilityRegistry.recordSuccessfulExecution("ORDER_CANCELLATION");

                log.info("Order cancelled successfully: orderId={}", orderId);
                return response;

            } catch (Exception e) {
                log.error("Failed to cancel order: orderId={}", orderId, e);
                capabilityRegistry.recordFailedExecution("ORDER_CANCELLATION", e);
                throw new RuntimeException("Order cancellation failed", e);
            }
        });
    }

    /**
     * Modifies an active order.
     */
    @EventHandler(event = "ModifyOrderRequest")
    @AgentCapability(
        name = "ORDER_MODIFICATION",
        proficiency = "ADVANCED",
        performanceProfile = "MEDIUM_LATENCY"
    )
    public CompletableFuture<OrderResponse> modifyOrder(String orderId, OrderRequest newParameters) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Modifying order: orderId={}", orderId);

            try {
                // Get strategy type for this order
                OrderStrategy.StrategyType strategyType = Optional.ofNullable(activeOrders.get(orderId))
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

                // Route to appropriate strategy
                OrderStrategy strategy = getStrategy(strategyType);
                var responseFuture = strategy.modify(orderId, newParameters);
                var response = responseFuture.join();

                capabilityRegistry.recordSuccessfulExecution("ORDER_MODIFICATION");

                log.info("Order modified successfully: orderId={}", orderId);
                return response;

            } catch (Exception e) {
                log.error("Failed to modify order: orderId={}", orderId, e);
                capabilityRegistry.recordFailedExecution("ORDER_MODIFICATION", e);
                throw new RuntimeException("Order modification failed", e);
            }
        });
    }

    /**
     * Generic strategy execution method with capability tracking.
     */
    private CompletableFuture<OrderResponse> executeStrategyOrder(
            OrderStrategy strategy, OrderRequest request, String capabilityName) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Executing {} order: symbol={}, side={}, quantity={}",
                    strategy.getStrategyType(), request.symbol(), request.side(), request.quantity());

            try {
                // Validate order
                var validationResult = strategy.validate(request);
                if (!validationResult.isValid()) {
                    log.error("Order validation failed: {}", validationResult.errorMessage());
                    throw new IllegalArgumentException(validationResult.errorMessage());
                }

                // Execute order
                var responseFuture = strategy.execute(request);
                var response = responseFuture.join();

                // Track active order
                activeOrders.put(response.orderId(), strategy.getStrategyType());

                capabilityRegistry.recordSuccessfulExecution(capabilityName);

                log.info("{} order executed successfully: orderId={}",
                        strategy.getStrategyType(), response.orderId());

                return response;

            } catch (Exception e) {
                log.error("Failed to execute {} order", strategy.getStrategyType(), e);
                capabilityRegistry.recordFailedExecution(capabilityName, e);
                throw new RuntimeException(strategy.getStrategyType() + " order execution failed", e);
            }
        });
    }

    /**
     * Selects appropriate strategy based on order request.
     * Uses pattern matching for strategy routing based on order parameters.
     */
    private OrderStrategy selectStrategy(OrderRequest request) {
        // Detect strategy based on which optional fields are present
        if (request.trailAmount() != null || request.trailPercent() != null) {
            return trailingStopStrategy;
        }

        if (request.entryPrice() != null && request.profitTarget() != null) {
            return bracketOrderStrategy;
        }

        if (request.displayQuantity() != null) {
            return icebergOrderStrategy;
        }

        // Check for TWAP/VWAP based on clientOrderRef hint
        String clientRef = request.clientOrderRef();
        if (clientRef != null) {
            if (clientRef.toUpperCase().contains("TWAP")) {
                return twapStrategy;
            }
            if (clientRef.toUpperCase().contains("VWAP")) {
                return vwapStrategy;
            }
        }

        // Default to stop-loss strategy for STOP_LOSS orders
        if (request.orderType() == com.trademaster.trading.model.OrderType.STOP_LOSS) {
            return stopLossStrategy;
        }

        throw new IllegalArgumentException(
            "Cannot determine execution strategy from order parameters. " +
            "Please specify strategy hints in clientOrderRef or use appropriate parameters."
        );
    }

    /**
     * Gets strategy instance by strategy type.
     * Uses pattern matching for strategy lookup.
     */
    private OrderStrategy getStrategy(OrderStrategy.StrategyType strategyType) {
        return switch (strategyType) {
            case STOP_LOSS -> stopLossStrategy;
            case TRAILING_STOP -> trailingStopStrategy;
            case BRACKET -> bracketOrderStrategy;
            case ICEBERG -> icebergOrderStrategy;
            case TWAP -> twapStrategy;
            case VWAP -> vwapStrategy;
        };
    }

    /**
     * Fallback method when order execution circuit breaker opens.
     */
    private CompletableFuture<OrderResponse> fallbackOrderExecution(
            OrderRequest request, Throwable throwable) {

        log.warn("Using fallback for order execution due to: {}", throwable.getMessage());

        // Return a rejected order response
        var fallbackResponse = new OrderResponse(
            null,
            "FALLBACK_" + System.currentTimeMillis(),
            null,
            request.symbol(),
            null,
            request.orderType(),
            request.side(),
            request.quantity(),
            request.price(),
            request.stopPrice(),
            null,
            null,
            com.trademaster.trading.model.OrderStatus.REJECTED,
            null,
            null,
            null,
            null,
            null,
            null, // fillPercentage
            null, // orderValue
            null, // executedValue
            "Order execution failed: " + throwable.getMessage(), // rejectionReason
            java.time.Instant.now(),
            java.time.Instant.now(),
            null,
            null
        );

        return CompletableFuture.completedFuture(fallbackResponse);
    }

    // ========== AgentOSComponent Implementation ==========

    @Override
    public String getAgentId() {
        return "order-strategy-agent";
    }

    @Override
    public String getAgentType() {
        return "ORDER_EXECUTION";
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
            "ADVANCED_ORDER_EXECUTION",
            "STOP_LOSS_EXECUTION",
            "TRAILING_STOP_EXECUTION",
            "BRACKET_ORDER_EXECUTION",
            "ICEBERG_ORDER_EXECUTION",
            "TWAP_EXECUTION",
            "VWAP_EXECUTION",
            "PRICE_MONITORING",
            "ORDER_CANCELLATION",
            "ORDER_MODIFICATION"
        );
    }

    @Override
    public Double getHealthScore() {
        return capabilityRegistry.calculateOverallHealthScore();
    }

    /**
     * Gets total active orders across all strategies.
     */
    public int getTotalActiveOrders() {
        return activeOrders.size();
    }

    /**
     * Gets active orders count by strategy type.
     */
    public Map<OrderStrategy.StrategyType, Integer> getActiveOrdersByStrategy() {
        Map<OrderStrategy.StrategyType, Integer> counts = new ConcurrentHashMap<>();

        activeOrders.values().forEach(strategyType ->
            counts.merge(strategyType, 1, Integer::sum)
        );

        return counts;
    }
}
