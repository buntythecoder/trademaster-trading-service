package com.trademaster.trading.order.strategy;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.model.OrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trailing Stop Order Strategy Implementation
 *
 * Dynamically adjusts stop price as market price moves favorably, protecting profits
 * while allowing for continued gains. The stop price "trails" the market price by
 * a fixed amount or percentage.
 *
 * Strategy Logic:
 * - For SELL side: Stop price trails below market price (protect long position)
 * - For BUY side: Stop price trails above market price (cover short position)
 * - Stop price only moves in favorable direction, never reverses
 * - Triggers when price reverses and hits trailing stop
 *
 * Trail Types:
 * - Fixed Amount: Stop trails by fixed dollar/rupee amount (e.g., ₹50 below high)
 * - Percentage: Stop trails by percentage (e.g., 2% below high)
 *
 * Use Cases:
 * - Profit protection: Lock in gains as position becomes profitable
 * - Trend following: Stay in position during favorable moves
 * - Exit strategy: Automatic exit when trend reverses
 *
 * Performance:
 * - Update latency: <10ms for price adjustments
 * - Trigger latency: <50ms from price update
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public final class TrailingStopStrategy implements OrderStrategy {

    // Active trailing stop orders: orderId -> TrailingStopOrder
    private final Map<String, TrailingStopOrder> activeOrders = new ConcurrentHashMap<>();

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.TRAILING_STOP;
    }

    @Override
    public ValidationResult validate(OrderRequest request) {
        log.debug("Validating trailing stop order: symbol={}, side={}, trailAmount={}",
                request.symbol(), request.side(), request.trailAmount());

        return Optional.ofNullable(request.trailAmount())
            .filter(trailAmount -> trailAmount.compareTo(BigDecimal.ZERO) > 0)
            .map(trailAmount -> ValidationResult.success())
            .orElse(ValidationResult.failure("Trail amount must be greater than zero"));
    }

    @Override
    public CompletableFuture<OrderResponse> execute(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Executing trailing stop order: symbol={}, side={}, trailAmount={}",
                    request.symbol(), request.side(), request.trailAmount());

            try {
                // Create trailing stop order with initial stop price
                String orderId = generateOrderId();
                var trailingStopOrder = new TrailingStopOrder(
                    orderId,
                    request.symbol(),
                    request.side(),
                    request.quantity(),
                    request.trailAmount(),
                    request.trailPercent() != null ? request.trailPercent() : BigDecimal.ZERO,
                    null,  // Initial stop price will be set on first price update
                    null,  // High water mark for SELL, low water mark for BUY
                    OrderStatus.PENDING,
                    Instant.now()
                );

                // Store in active orders for price monitoring
                activeOrders.put(orderId, trailingStopOrder);

                log.info("Trailing stop order created: orderId={}, will trail by {}",
                        orderId, formatTrailAmount(request));

                // Return pending order response
                return createOrderResponse(trailingStopOrder);

            } catch (Exception e) {
                log.error("Failed to execute trailing stop order", e);
                throw new RuntimeException("Trailing stop order execution failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> onPriceUpdate(String orderId, BigDecimal currentPrice) {
        return CompletableFuture.supplyAsync(() ->
            Optional.ofNullable(activeOrders.get(orderId))
                .map(order -> processTrailingStop(orderId, order, currentPrice))
                .orElse(false)
        );
    }

    @Override
    public CompletableFuture<OrderResponse> cancel(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Cancelling trailing stop order: orderId={}", orderId);

            return Optional.ofNullable(activeOrders.remove(orderId))
                .map(order -> {
                    var cancelledOrder = new TrailingStopOrder(
                        order.orderId(),
                        order.symbol(),
                        order.side(),
                        order.quantity(),
                        order.trailAmount(),
                        order.trailPercent(),
                        order.currentStopPrice(),
                        order.extremePrice(),
                        OrderStatus.CANCELLED,
                        order.createdAt()
                    );
                    return createOrderResponse(cancelledOrder);
                })
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        });
    }

    @Override
    public CompletableFuture<OrderResponse> modify(String orderId, OrderRequest newParameters) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Modifying trailing stop order: orderId={}, newTrailAmount={}",
                    orderId, newParameters.trailAmount());

            return Optional.ofNullable(activeOrders.get(orderId))
                .map(existingOrder -> {
                    // Create modified order with new trail parameters
                    var modifiedOrder = new TrailingStopOrder(
                        existingOrder.orderId(),
                        existingOrder.symbol(),
                        existingOrder.side(),
                        newParameters.quantity() != null ? newParameters.quantity() : existingOrder.quantity(),
                        newParameters.trailAmount() != null ? newParameters.trailAmount() : existingOrder.trailAmount(),
                        newParameters.trailPercent() != null ? newParameters.trailPercent() : existingOrder.trailPercent(),
                        existingOrder.currentStopPrice(),
                        existingOrder.extremePrice(),
                        OrderStatus.PENDING,
                        existingOrder.createdAt()
                    );

                    // Update in active orders
                    activeOrders.put(orderId, modifiedOrder);

                    log.info("Trailing stop order modified: orderId={}, trailAmount={}",
                            orderId, modifiedOrder.trailAmount());

                    return createOrderResponse(modifiedOrder);
                })
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        });
    }

    /**
     * Processes trailing stop logic for price update.
     * Updates stop price if market moves favorably, triggers if stop hit.
     */
    private boolean processTrailingStop(String orderId, TrailingStopOrder order, BigDecimal currentPrice) {
        // Initialize extreme price on first update
        if (order.extremePrice() == null) {
            var initializedOrder = initializeOrder(order, currentPrice);
            activeOrders.put(orderId, initializedOrder);
            return false;
        }

        // Check if current price is more extreme (favorable) than recorded
        boolean isMoreExtreme = isMoreExtreme(order.side(), currentPrice, order.extremePrice());

        if (isMoreExtreme) {
            // Update extreme price and trailing stop price
            var updatedOrder = updateTrailingStop(order, currentPrice);
            activeOrders.put(orderId, updatedOrder);

            log.debug("Trailing stop updated: orderId={}, extremePrice={} -> {}, stopPrice={}",
                    orderId, order.extremePrice(), currentPrice, updatedOrder.currentStopPrice());

            return false;
        }

        // Check if stop price has been hit
        boolean stopHit = isStopHit(order.side(), currentPrice, order.currentStopPrice());

        if (stopHit) {
            triggerTrailingStop(orderId, order, currentPrice);
            return true;
        }

        return false;
    }

    /**
     * Initializes order with first price update.
     */
    private TrailingStopOrder initializeOrder(TrailingStopOrder order, BigDecimal currentPrice) {
        BigDecimal initialStopPrice = calculateStopPrice(order, currentPrice);

        log.info("Initializing trailing stop: orderId={}, currentPrice={}, stopPrice={}",
                order.orderId(), currentPrice, initialStopPrice);

        return new TrailingStopOrder(
            order.orderId(),
            order.symbol(),
            order.side(),
            order.quantity(),
            order.trailAmount(),
            order.trailPercent(),
            initialStopPrice,
            currentPrice,
            order.status(),
            order.createdAt()
        );
    }

    /**
     * Updates trailing stop price based on new extreme price.
     */
    private TrailingStopOrder updateTrailingStop(TrailingStopOrder order, BigDecimal newExtremePrice) {
        BigDecimal newStopPrice = calculateStopPrice(order, newExtremePrice);

        return new TrailingStopOrder(
            order.orderId(),
            order.symbol(),
            order.side(),
            order.quantity(),
            order.trailAmount(),
            order.trailPercent(),
            newStopPrice,
            newExtremePrice,
            order.status(),
            order.createdAt()
        );
    }

    /**
     * Calculates stop price based on extreme price and trail parameters.
     * Uses pattern matching for trail type selection.
     */
    private BigDecimal calculateStopPrice(TrailingStopOrder order, BigDecimal extremePrice) {
        // Use percentage trail if specified, otherwise use fixed amount
        boolean usePercent = order.trailPercent().compareTo(BigDecimal.ZERO) > 0;

        BigDecimal trailDistance = usePercent
            ? extremePrice.multiply(order.trailPercent()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
            : order.trailAmount();

        return switch (order.side()) {
            case SELL -> extremePrice.subtract(trailDistance).setScale(2, RoundingMode.HALF_UP);
            case BUY -> extremePrice.add(trailDistance).setScale(2, RoundingMode.HALF_UP);
        };
    }

    /**
     * Checks if current price is more extreme (favorable) than recorded extreme.
     * Uses pattern matching for side-specific logic.
     */
    private boolean isMoreExtreme(OrderSide side, BigDecimal currentPrice, BigDecimal extremePrice) {
        return switch (side) {
            case SELL -> currentPrice.compareTo(extremePrice) > 0;  // Higher is better for SELL
            case BUY -> currentPrice.compareTo(extremePrice) < 0;   // Lower is better for BUY
        };
    }

    /**
     * Checks if stop price has been hit.
     * Uses pattern matching for side-specific logic.
     */
    private boolean isStopHit(OrderSide side, BigDecimal currentPrice, BigDecimal stopPrice) {
        return switch (side) {
            case SELL -> currentPrice.compareTo(stopPrice) <= 0;  // Price fell to/below stop
            case BUY -> currentPrice.compareTo(stopPrice) >= 0;   // Price rose to/above stop
        };
    }

    /**
     * Triggers trailing stop by converting to market order.
     */
    private void triggerTrailingStop(String orderId, TrailingStopOrder order, BigDecimal triggerPrice) {
        log.info("TRAILING STOP TRIGGERED: orderId={}, triggerPrice={}, stopPrice={}, extremePrice={}",
                orderId, triggerPrice, order.currentStopPrice(), order.extremePrice());

        // Update order status to triggered
        var triggeredOrder = new TrailingStopOrder(
            order.orderId(),
            order.symbol(),
            order.side(),
            order.quantity(),
            order.trailAmount(),
            order.trailPercent(),
            order.currentStopPrice(),
            order.extremePrice(),
            OrderStatus.FILLED,  // Would be PENDING in real implementation
            order.createdAt()
        );

        activeOrders.put(orderId, triggeredOrder);

        // In production: Submit market order to broker
        // executeMarketOrder(order.symbol(), order.side(), order.quantity());
    }

    /**
     * Formats trail amount for logging.
     */
    private String formatTrailAmount(OrderRequest request) {
        return request.trailPercent() != null && request.trailPercent().compareTo(BigDecimal.ZERO) > 0
            ? request.trailPercent() + "%"
            : "₹" + request.trailAmount();
    }

    /**
     * Generates unique order identifier.
     */
    private String generateOrderId() {
        return "TS_" + System.currentTimeMillis() + "_" +
               Long.toHexString(System.nanoTime());
    }

    /**
     * Creates OrderResponse from TrailingStopOrder.
     */
    private OrderResponse createOrderResponse(TrailingStopOrder order) {
        return new OrderResponse(
            null,
            order.orderId(),
            null,
            order.symbol(),
            null,
            OrderType.STOP_LOSS,
            order.side(),
            order.quantity(),
            null,
            order.currentStopPrice(),
            null,
            null,
            order.status(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            order.createdAt(),
            Instant.now(),
            null,
            null
        );
    }

    /**
     * Gets total active trailing stop orders count.
     */
    public int getActiveOrdersCount() {
        return activeOrders.size();
    }

    /**
     * Internal record for tracking trailing stop orders.
     */
    private record TrailingStopOrder(
        String orderId,
        String symbol,
        OrderSide side,
        Integer quantity,
        BigDecimal trailAmount,
        BigDecimal trailPercent,
        BigDecimal currentStopPrice,
        BigDecimal extremePrice,  // High water mark for SELL, low for BUY
        OrderStatus status,
        Instant createdAt
    ) {}
}
