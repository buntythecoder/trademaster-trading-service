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
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Iceberg Order Strategy Implementation
 *
 * Executes large orders by showing only a small visible portion (tip of iceberg) to the market
 * at a time, while hiding the total order quantity. When the visible portion fills, a new
 * slice is automatically placed until the entire order is executed.
 *
 * Strategy Logic:
 * - Total Quantity: Full order size (hidden from market)
 * - Display Quantity: Visible portion shown to market (iceberg tip)
 * - Automatic Replenishment: New slice placed after each fill
 * - Price Limit: Optional limit price for each slice
 * - Market Impact Reduction: Prevents price movement from large orders
 *
 * Execution Flow:
 * 1. Place initial slice (display quantity) on order book
 * 2. Wait for slice to fill completely
 * 3. Calculate remaining quantity
 * 4. Place next slice if remaining quantity exists
 * 5. Repeat until total quantity filled
 *
 * Use Cases:
 * - Institutional Orders: Hide large order sizes
 * - Market Impact Reduction: Minimize price slippage
 * - Stealth Trading: Conceal trading intentions
 * - Liquidity Management: Avoid overwhelming order book
 *
 * Performance:
 * - Slice placement: <100ms
 * - Fill detection: <50ms
 * - Replenishment latency: <200ms
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public final class IcebergOrderStrategy implements OrderStrategy {

    // Active iceberg orders: orderId -> IcebergOrder
    private final Map<String, IcebergOrder> activeOrders = new ConcurrentHashMap<>();

    @Override
    public StrategyType getStrategyType() {
        return StrategyType.ICEBERG;
    }

    @Override
    public ValidationResult validate(OrderRequest request) {
        log.debug("Validating iceberg order: symbol={}, totalQty={}, displayQty={}",
                request.symbol(), request.quantity(), request.displayQuantity());

        // Validate display quantity exists
        var displayQtyResult = Optional.ofNullable(request.displayQuantity())
            .filter(displayQty -> displayQty > 0)
            .map(displayQty -> ValidationResult.success())
            .orElse(ValidationResult.failure("Display quantity must be greater than zero"));

        if (!displayQtyResult.isValid()) {
            return displayQtyResult;
        }

        // Validate display quantity is less than total quantity
        return Optional.of(request)
            .filter(req -> req.displayQuantity() < req.quantity())
            .map(req -> ValidationResult.success())
            .orElse(ValidationResult.failure("Display quantity must be less than total quantity"));
    }

    @Override
    public CompletableFuture<OrderResponse> execute(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Executing iceberg order: symbol={}, totalQty={}, displayQty={}, price={}",
                    request.symbol(), request.quantity(), request.displayQuantity(), request.price());

            try {
                // Create iceberg order with initial slice
                String orderId = generateOrderId();
                var icebergOrder = new IcebergOrder(
                    orderId,
                    request.symbol(),
                    request.side(),
                    request.quantity(),
                    request.displayQuantity(),
                    new AtomicInteger(0),  // Filled quantity
                    request.price(),
                    1,  // Current slice number
                    IcebergState.ACTIVE,
                    OrderStatus.PENDING,
                    Instant.now()
                );

                // Store in active orders
                activeOrders.put(orderId, icebergOrder);

                // Place initial slice
                placeSlice(icebergOrder);

                log.info("Iceberg order created: orderId={}, totalQty={}, slices={}",
                        orderId, request.quantity(), calculateTotalSlices(request.quantity(), request.displayQuantity()));

                // Return pending order response
                return createOrderResponse(icebergOrder);

            } catch (Exception e) {
                log.error("Failed to execute iceberg order", e);
                throw new RuntimeException("Iceberg order execution failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> onPriceUpdate(String orderId, BigDecimal currentPrice) {
        return CompletableFuture.supplyAsync(() ->
            Optional.ofNullable(activeOrders.get(orderId))
                .map(order -> processIcebergOrder(orderId, order, currentPrice))
                .orElse(false)
        );
    }

    @Override
    public CompletableFuture<OrderResponse> cancel(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Cancelling iceberg order: orderId={}", orderId);

            return Optional.ofNullable(activeOrders.remove(orderId))
                .map(order -> {
                    var cancelledOrder = new IcebergOrder(
                        order.orderId(),
                        order.symbol(),
                        order.side(),
                        order.totalQuantity(),
                        order.displayQuantity(),
                        order.filledQuantity(),
                        order.price(),
                        order.currentSlice(),
                        IcebergState.CANCELLED,
                        OrderStatus.CANCELLED,
                        order.createdAt()
                    );

                    // In production: Cancel current slice on exchange
                    // cancelExchangeOrder(order.orderId(), order.currentSlice());

                    return createOrderResponse(cancelledOrder);
                })
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        });
    }

    @Override
    public CompletableFuture<OrderResponse> modify(String orderId, OrderRequest newParameters) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Modifying iceberg order: orderId={}", orderId);

            return Optional.ofNullable(activeOrders.get(orderId))
                .map(existingOrder -> {
                    // Calculate new display quantity for remaining slices
                    Integer newDisplayQty = Optional.ofNullable(newParameters.displayQuantity())
                        .orElse(existingOrder.displayQuantity());

                    // Validate new display quantity
                    int remaining = existingOrder.totalQuantity() - existingOrder.filledQuantity().get();
                    if (newDisplayQty >= remaining && remaining > 0) {
                        throw new IllegalArgumentException("Display quantity must be less than remaining quantity");
                    }

                    // Create modified order
                    var modifiedOrder = new IcebergOrder(
                        existingOrder.orderId(),
                        existingOrder.symbol(),
                        existingOrder.side(),
                        existingOrder.totalQuantity(),
                        newDisplayQty,
                        existingOrder.filledQuantity(),
                        newParameters.price() != null ? newParameters.price() : existingOrder.price(),
                        existingOrder.currentSlice(),
                        existingOrder.state(),
                        OrderStatus.PENDING,
                        existingOrder.createdAt()
                    );

                    // Update in active orders
                    activeOrders.put(orderId, modifiedOrder);

                    log.info("Iceberg order modified: orderId={}, displayQty={}",
                            orderId, newDisplayQty);

                    return createOrderResponse(modifiedOrder);
                })
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        });
    }

    /**
     * Processes iceberg order by detecting slice fills and placing new slices.
     */
    private boolean processIcebergOrder(String orderId, IcebergOrder order, BigDecimal currentPrice) {
        if (order.state() != IcebergState.ACTIVE) {
            return false;
        }

        // Simulate slice fill detection (in production, check actual exchange fills)
        boolean sliceFilled = checkSliceFilled(order, currentPrice);

        if (sliceFilled) {
            return handleSliceFill(orderId, order);
        }

        return false;
    }

    /**
     * Handles slice fill by updating filled quantity and placing next slice.
     */
    private boolean handleSliceFill(String orderId, IcebergOrder order) {
        // Update filled quantity
        int newFilled = order.filledQuantity().addAndGet(order.displayQuantity());
        int remaining = order.totalQuantity() - newFilled;

        log.info("Slice filled: orderId={}, slice={}, filled={}/{}, remaining={}",
                orderId, order.currentSlice(), newFilled, order.totalQuantity(), remaining);

        // Check if entire order filled
        if (remaining <= 0) {
            completeIcebergOrder(orderId, order);
            return true;
        }

        // Place next slice
        placeNextSlice(orderId, order, remaining);
        return false;
    }

    /**
     * Places initial slice on order book.
     */
    private void placeSlice(IcebergOrder order) {
        int sliceQty = Math.min(order.displayQuantity(),
                                order.totalQuantity() - order.filledQuantity().get());

        log.info("Placing slice: orderId={}, slice={}, qty={}, price={}",
                order.orderId(), order.currentSlice(), sliceQty, order.price());

        // In production: Place actual order on exchange
        // placeExchangeOrder(order.orderId(), order.symbol(), order.side(), sliceQty, order.price());
    }

    /**
     * Places next slice after previous slice filled.
     */
    private void placeNextSlice(String orderId, IcebergOrder order, int remainingQty) {
        int nextSliceQty = Math.min(order.displayQuantity(), remainingQty);
        int nextSliceNumber = order.currentSlice() + 1;

        var updatedOrder = new IcebergOrder(
            order.orderId(),
            order.symbol(),
            order.side(),
            order.totalQuantity(),
            order.displayQuantity(),
            order.filledQuantity(),
            order.price(),
            nextSliceNumber,
            IcebergState.ACTIVE,
            OrderStatus.PENDING,
            order.createdAt()
        );

        activeOrders.put(orderId, updatedOrder);

        log.info("Placing next slice: orderId={}, slice={}, qty={}, remaining={}",
                orderId, nextSliceNumber, nextSliceQty, remainingQty);

        // In production: Place next slice on exchange
        // placeExchangeOrder(orderId, order.symbol(), order.side(), nextSliceQty, order.price());
    }

    /**
     * Completes iceberg order when all slices filled.
     */
    private void completeIcebergOrder(String orderId, IcebergOrder order) {
        var completedOrder = new IcebergOrder(
            order.orderId(),
            order.symbol(),
            order.side(),
            order.totalQuantity(),
            order.displayQuantity(),
            order.filledQuantity(),
            order.price(),
            order.currentSlice(),
            IcebergState.COMPLETED,
            OrderStatus.FILLED,
            order.createdAt()
        );

        activeOrders.put(orderId, completedOrder);

        log.info("ICEBERG ORDER COMPLETED: orderId={}, totalFilled={}, slices={}",
                orderId, order.filledQuantity().get(), order.currentSlice());
    }

    /**
     * Checks if current slice has been filled.
     * In production, this would check actual exchange fills.
     */
    private boolean checkSliceFilled(IcebergOrder order, BigDecimal currentPrice) {
        // Mock fill detection: Check if price matches limit price
        if (order.price() == null) {
            return true;  // Market order slice fills immediately
        }

        return switch (order.side()) {
            case BUY -> currentPrice.compareTo(order.price()) <= 0;  // Buy at or below limit
            case SELL -> currentPrice.compareTo(order.price()) >= 0; // Sell at or above limit
        };
    }

    /**
     * Calculates total number of slices needed.
     */
    private int calculateTotalSlices(int totalQty, int displayQty) {
        return (int) Math.ceil((double) totalQty / displayQty);
    }

    /**
     * Generates unique order identifier.
     */
    private String generateOrderId() {
        return "ICE_" + System.currentTimeMillis() + "_" +
               Long.toHexString(System.nanoTime());
    }

    /**
     * Creates OrderResponse from IcebergOrder.
     */
    private OrderResponse createOrderResponse(IcebergOrder order) {
        return new OrderResponse(
            null,
            order.orderId(),
            null,
            order.symbol(),
            null,
            OrderType.LIMIT,
            order.side(),
            order.totalQuantity(),
            order.price(),
            null,
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
     * Gets total active iceberg orders count.
     */
    public int getActiveOrdersCount() {
        return activeOrders.size();
    }

    /**
     * Gets filled percentage for an order.
     */
    public double getFilledPercentage(String orderId) {
        return Optional.ofNullable(activeOrders.get(orderId))
            .map(order -> (order.filledQuantity().get() * 100.0) / order.totalQuantity())
            .orElse(0.0);
    }

    /**
     * Iceberg order state enumeration.
     */
    private enum IcebergState {
        ACTIVE,     // Slices being placed and filled
        COMPLETED,  // All slices filled
        CANCELLED   // Order cancelled
    }

    /**
     * Internal record for tracking iceberg orders.
     */
    private record IcebergOrder(
        String orderId,
        String symbol,
        OrderSide side,
        Integer totalQuantity,
        Integer displayQuantity,
        AtomicInteger filledQuantity,
        BigDecimal price,
        Integer currentSlice,
        IcebergState state,
        OrderStatus status,
        Instant createdAt
    ) {}
}
