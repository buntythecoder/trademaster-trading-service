package com.trademaster.trading.controller;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.dto.PositionResponse;
import com.trademaster.trading.service.OrderService;
import com.trademaster.trading.service.PositionService;
import com.trademaster.trading.common.TradingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Internal Trading API Controller
 * 
 * Provides internal endpoints for service-to-service communication.
 * These endpoints bypass JWT authentication and use service API key authentication.
 * 
 * Security:
 * - Service API key authentication required
 * - Role-based access control (ROLE_SERVICE, ROLE_INTERNAL)
 * - Internal network access only
 * - Audit logging for all operations
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/internal/v1/trading")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('SERVICE') and hasRole('INTERNAL')")
public class InternalTradingController {
    
    private final OrderService orderService;
    private final PositionService positionService;
    
    /**
     * Get user positions (internal service call)
     * Used by portfolio-service, risk-service, etc.
     */
    @GetMapping("/positions/{userId}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<List<PositionResponse>> getUserPositions(@PathVariable String userId) {
        try {
            log.info("Internal API: Getting positions for user: {}", userId);
            List<PositionResponse> positions = positionService.getUserPositions(userId);
            return ResponseEntity.ok(positions);
        } catch (Exception e) {
            log.error("Internal API: Failed to get positions for user: {}", userId, e);
            throw new TradingException("Failed to retrieve user positions", e);
        }
    }
    
    /**
     * Get order details (internal service call)
     * Used by notification-service, audit-service, etc.
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable String orderId) {
        try {
            log.info("Internal API: Getting order details for order: {}", orderId);
            OrderResponse order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Internal API: Failed to get order details for order: {}", orderId, e);
            throw new TradingException("Failed to retrieve order details", e);
        }
    }
    
    /**
     * Get user orders (internal service call)
     * Used by portfolio-service, risk-service for calculations
     */
    @GetMapping("/orders/user/{userId}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @PathVariable String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        try {
            log.info("Internal API: Getting orders for user: {}, status: {}, limit: {}", 
                    userId, status, limit);
            List<OrderResponse> orders = orderService.getUserOrders(userId, status, limit);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Internal API: Failed to get orders for user: {}", userId, e);
            throw new TradingException("Failed to retrieve user orders", e);
        }
    }
    
    /**
     * Place order (internal service call)
     * Used by risk-service after risk validation, algorithmic trading services
     */
    @PostMapping("/orders")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<OrderResponse> placeOrderInternal(@Valid @RequestBody OrderRequest request) {
        try {
            log.info("Internal API: Placing order for user: {}, symbol: {}, quantity: {}", 
                    request.getUserId(), request.getSymbol(), request.getQuantity());
            
            OrderResponse response = orderService.placeOrder(request);
            
            log.info("Internal API: Order placed successfully: {}", response.getOrderId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to place order: {}", request, e);
            throw new TradingException("Failed to place order", e);
        }
    }
    
    /**
     * Update order status (internal service call)
     * Used by broker-auth-service to update order status from broker
     */
    @PutMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> statusUpdate) {
        try {
            log.info("Internal API: Updating order status for order: {}, update: {}", 
                    orderId, statusUpdate);
            
            orderService.updateOrderStatus(orderId, statusUpdate);
            
            return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", "updated",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("Internal API: Failed to update order status for order: {}", orderId, e);
            throw new TradingException("Failed to update order status", e);
        }
    }
    
    /**
     * Get portfolio summary (internal service call)
     * Used by risk-service for position calculations
     */
    @GetMapping("/portfolio/{userId}/summary")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary(@PathVariable String userId) {
        try {
            log.info("Internal API: Getting portfolio summary for user: {}", userId);
            
            // Get positions and calculate summary
            List<PositionResponse> positions = positionService.getUserPositions(userId);
            
            double totalValue = positions.stream()
                .mapToDouble(pos -> pos.getCurrentValue())
                .sum();
            
            double totalPnL = positions.stream()
                .mapToDouble(pos -> pos.getUnrealizedPnL())
                .sum();
            
            Map<String, Object> summary = Map.of(
                "userId", userId,
                "totalPositions", positions.size(),
                "totalValue", totalValue,
                "totalPnL", totalPnL,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("Internal API: Failed to get portfolio summary for user: {}", userId, e);
            throw new TradingException("Failed to retrieve portfolio summary", e);
        }
    }
    
    /**
     * Health check for internal services
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "service", "trading-service",
            "status", "UP",
            "internal_api", "available",
            "timestamp", System.currentTimeMillis()
        ));
    }
}