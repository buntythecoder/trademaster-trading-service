package com.trademaster.trading.controller;

import com.trademaster.trading.entity.SimpleOrder;
import com.trademaster.trading.service.SimpleTradingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Simple Trading Controller
 * 
 * Basic REST endpoints for order management that actually work
 * without complex dependencies or Result patterns.
 */
@RestController
@RequestMapping("/api/v1/simple-trading")
@RequiredArgsConstructor
@Slf4j
public class SimpleTradingController {
    
    private final SimpleTradingService tradingService;
    
    /**
     * Place a new order
     */
    @PostMapping("/orders")
    public ResponseEntity<SimpleOrder> placeOrder(@RequestBody Map<String, Object> request) {
        
        Long userId = Long.valueOf(request.get("userId").toString());
        String symbol = (String) request.get("symbol");
        String side = (String) request.get("side");
        Integer quantity = Integer.valueOf(request.get("quantity").toString());
        BigDecimal price = new BigDecimal(request.get("price").toString());
        String orderType = (String) request.get("orderType");
        
        SimpleOrder order = tradingService.placeOrder(userId, symbol, side, quantity, price, orderType);
        
        return ResponseEntity.ok(order);
    }
    
    /**
     * Get order by ID
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<SimpleOrder> getOrder(@PathVariable String orderId) {
        SimpleOrder order = tradingService.getOrder(orderId);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }
    
    /**
     * Get orders for user
     */
    @GetMapping("/users/{userId}/orders")
    public ResponseEntity<List<SimpleOrder>> getOrdersForUser(@PathVariable Long userId) {
        List<SimpleOrder> orders = tradingService.getOrdersForUser(userId);
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Cancel order
     */
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<SimpleOrder> cancelOrder(@PathVariable String orderId) {
        SimpleOrder order = tradingService.cancelOrder(orderId);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "simple-trading-service"
        ));
    }
}