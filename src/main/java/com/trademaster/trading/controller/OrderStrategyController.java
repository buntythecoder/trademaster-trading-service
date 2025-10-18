package com.trademaster.trading.controller;

import com.trademaster.trading.agentos.agents.OrderStrategyAgent;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.order.strategy.OrderStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Order Strategy REST API Controller
 *
 * Provides HTTP endpoints for advanced order execution strategies including Stop-Loss,
 * Trailing Stop, Bracket Orders, Iceberg Orders, TWAP, and VWAP. Delegates to
 * OrderStrategyAgent for order execution and lifecycle management.
 *
 * Endpoints:
 * - POST /api/v1/orders/advanced: Execute any advanced order type
 * - POST /api/v1/orders/stop-loss: Execute stop-loss order
 * - POST /api/v1/orders/trailing-stop: Execute trailing stop order
 * - POST /api/v1/orders/bracket: Execute bracket order
 * - POST /api/v1/orders/iceberg: Execute iceberg order
 * - POST /api/v1/orders/twap: Execute TWAP order
 * - POST /api/v1/orders/vwap: Execute VWAP order
 * - PUT /api/v1/orders/{orderId}/price: Update price for active order
 * - PUT /api/v1/orders/{orderId}: Modify order parameters
 * - DELETE /api/v1/orders/{orderId}: Cancel active order
 * - GET /api/v1/orders/statistics: Get order execution statistics
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Strategies", description = "Advanced order execution strategies APIs")
public class OrderStrategyController {

    private final OrderStrategyAgent orderStrategyAgent;

    /**
     * Execute advanced order with automatic strategy selection.
     *
     * @param request Order request with strategy-specific parameters
     * @return CompletableFuture with OrderResponse
     */
    @PostMapping("/advanced")
    @Operation(summary = "Execute advanced order",
            description = "Executes order using appropriate strategy based on order type")
    public CompletableFuture<ResponseEntity<OrderResponse>> executeAdvancedOrder(
            @RequestBody OrderRequest request) {

        log.info("REST API: Executing advanced order: type={}, symbol={}, quantity={}",
                request.orderType(), request.symbol(), request.quantity());

        return orderStrategyAgent.executeAdvancedOrder(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to execute advanced order", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Execute stop-loss order.
     *
     * @param request Order request with stop price
     * @return CompletableFuture with OrderResponse
     */
    @PostMapping("/stop-loss")
    @Operation(summary = "Execute stop-loss order",
            description = "Creates stop-loss order that triggers when price hits stop level")
    public CompletableFuture<ResponseEntity<OrderResponse>> executeStopLoss(
            @RequestBody OrderRequest request) {

        log.info("REST API: Executing stop-loss order: symbol={}, stopPrice={}",
                request.symbol(), request.stopPrice());

        return orderStrategyAgent.executeStopLoss(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to execute stop-loss order", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Execute trailing stop order.
     *
     * @param request Order request with trail amount or percentage
     * @return CompletableFuture with OrderResponse
     */
    @PostMapping("/trailing-stop")
    @Operation(summary = "Execute trailing stop order",
            description = "Creates trailing stop that adjusts automatically with favorable price movement")
    public CompletableFuture<ResponseEntity<OrderResponse>> executeTrailingStop(
            @RequestBody OrderRequest request) {

        log.info("REST API: Executing trailing stop order: symbol={}, trailAmount={}",
                request.symbol(), request.trailAmount());

        return orderStrategyAgent.executeTrailingStop(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to execute trailing stop order", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Execute bracket order.
     *
     * @param request Order request with entry, profit target, and stop-loss
     * @return CompletableFuture with OrderResponse
     */
    @PostMapping("/bracket")
    @Operation(summary = "Execute bracket order",
            description = "Creates bracket order with entry, profit target, and stop-loss (OCO)")
    public CompletableFuture<ResponseEntity<OrderResponse>> executeBracketOrder(
            @RequestBody OrderRequest request) {

        log.info("REST API: Executing bracket order: symbol={}, entry={}, profit={}, stop={}",
                request.symbol(), request.entryPrice(), request.profitTarget(), request.stopPrice());

        return orderStrategyAgent.executeBracketOrder(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to execute bracket order", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Execute iceberg order.
     *
     * @param request Order request with total quantity and display quantity
     * @return CompletableFuture with OrderResponse
     */
    @PostMapping("/iceberg")
    @Operation(summary = "Execute iceberg order",
            description = "Creates iceberg order showing only small portion of total quantity")
    public CompletableFuture<ResponseEntity<OrderResponse>> executeIcebergOrder(
            @RequestBody OrderRequest request) {

        log.info("REST API: Executing iceberg order: symbol={}, totalQty={}, displayQty={}",
                request.symbol(), request.quantity(), request.displayQuantity());

        return orderStrategyAgent.executeIcebergOrder(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to execute iceberg order", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Execute TWAP (Time-Weighted Average Price) order.
     *
     * @param request Order request with time window and slice interval
     * @return CompletableFuture with OrderResponse
     */
    @PostMapping("/twap")
    @Operation(summary = "Execute TWAP order",
            description = "Creates TWAP order distributing execution evenly over time period")
    public CompletableFuture<ResponseEntity<OrderResponse>> executeTWAP(
            @RequestBody OrderRequest request) {

        log.info("REST API: Executing TWAP order: symbol={}, qty={}, timeWindow={}m",
                request.symbol(), request.quantity(), request.timeWindowMinutes());

        return orderStrategyAgent.executeTWAP(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to execute TWAP order", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Execute VWAP (Volume-Weighted Average Price) order.
     *
     * @param request Order request with time window and participation rate
     * @return CompletableFuture with OrderResponse
     */
    @PostMapping("/vwap")
    @Operation(summary = "Execute VWAP order",
            description = "Creates VWAP order matching execution to historical volume patterns")
    public CompletableFuture<ResponseEntity<OrderResponse>> executeVWAP(
            @RequestBody OrderRequest request) {

        log.info("REST API: Executing VWAP order: symbol={}, qty={}, participationRate={}%",
                request.symbol(), request.quantity(), request.participationRate());

        return orderStrategyAgent.executeVWAP(request)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to execute VWAP order", throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Update market price for active orders.
     * Triggers price monitoring for all strategies.
     *
     * @param symbol Stock symbol
     * @param currentPrice Current market price
     * @return CompletableFuture with trigger results
     */
    @PutMapping("/{symbol}/price")
    @Operation(summary = "Update market price",
            description = "Updates market price and checks if any orders should be triggered")
    public CompletableFuture<ResponseEntity<Map<String, Boolean>>> updateMarketPrice(
            @PathVariable String symbol,
            @Parameter(description = "Current market price") @RequestParam BigDecimal currentPrice) {

        log.debug("REST API: Updating market price: symbol={}, price={}", symbol, currentPrice);

        return orderStrategyAgent.onPriceUpdate(symbol, currentPrice)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to update market price for symbol={}", symbol, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Modify an active order.
     *
     * @param orderId Order identifier
     * @param newParameters Updated order parameters
     * @return CompletableFuture with OrderResponse
     */
    @PutMapping("/{orderId}")
    @Operation(summary = "Modify order",
            description = "Modifies parameters of an active order")
    public CompletableFuture<ResponseEntity<OrderResponse>> modifyOrder(
            @PathVariable String orderId,
            @RequestBody OrderRequest newParameters) {

        log.info("REST API: Modifying order: orderId={}", orderId);

        return orderStrategyAgent.modifyOrder(orderId, newParameters)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to modify order: orderId={}", orderId, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Cancel an active order.
     *
     * @param orderId Order identifier
     * @return CompletableFuture with OrderResponse
     */
    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel order",
            description = "Cancels an active order")
    public CompletableFuture<ResponseEntity<OrderResponse>> cancelOrder(
            @PathVariable String orderId) {

        log.info("REST API: Cancelling order: orderId={}", orderId);

        return orderStrategyAgent.cancelOrder(orderId)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                log.error("Failed to cancel order: orderId={}", orderId, throwable);
                return ResponseEntity.internalServerError().build();
            });
    }

    /**
     * Get order execution statistics.
     *
     * @return Order statistics including active orders and strategy breakdown
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get order statistics",
            description = "Fetches order execution statistics and active order counts")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        log.debug("REST API: Fetching order statistics");

        try {
            int totalActiveOrders = orderStrategyAgent.getTotalActiveOrders();
            Map<OrderStrategy.StrategyType, Integer> ordersByStrategy =
                orderStrategyAgent.getActiveOrdersByStrategy();
            Double healthScore = orderStrategyAgent.getHealthScore();

            Map<String, Object> statistics = Map.of(
                "totalActiveOrders", totalActiveOrders,
                "ordersByStrategy", ordersByStrategy,
                "healthScore", healthScore,
                "capabilities", orderStrategyAgent.getCapabilities()
            );

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            log.error("Failed to fetch order statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
