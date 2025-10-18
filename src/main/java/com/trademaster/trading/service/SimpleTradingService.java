package com.trademaster.trading.service;

import com.trademaster.trading.entity.SimpleOrder;
import com.trademaster.trading.repository.SimpleOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple Trading Service
 * 
 * Core trading functionality that integrates with the working
 * broker-auth-service without complex dependencies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleTradingService {
    
    private final SimpleOrderRepository orderRepository;
    private final RestTemplate restTemplate;
    
    @Value("${trademaster.broker.auth.service.url:http://localhost:8087/api/v1}")
    private String brokerAuthServiceUrl;
    
    /**
     * Place a new order
     */
    public SimpleOrder placeOrder(Long userId, String symbol, String side, 
                                 Integer quantity, BigDecimal price, String orderType) {
        
        String orderId = "TM-" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("Placing order {} for user {}: {} {} {} @ {}", 
                orderId, userId, side, quantity, symbol, price);
        
        // Create and save order
        SimpleOrder order = SimpleOrder.builder()
                .orderId(orderId)
                .userId(userId)
                .symbol(symbol)
                .side(side)
                .quantity(quantity)
                .price(price)
                .orderType(orderType)
                .status("PENDING")
                .build();
        
        order = orderRepository.save(order);
        
        // Submit to broker (async)
        try {
            String brokerOrderId = submitToBroker(order);
            order.setBrokerOrderId(brokerOrderId);
            order.setStatus("SUBMITTED");
            order = orderRepository.save(order);
            log.info("Order {} submitted to broker with ID: {}", orderId, brokerOrderId);
        } catch (Exception e) {
            log.error("Failed to submit order {} to broker: {}", orderId, e.getMessage());
            order.setStatus("FAILED");
            orderRepository.save(order);
        }
        
        return order;
    }
    
    /**
     * Get order by ID
     */
    public SimpleOrder getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId).orElse(null);
    }
    
    /**
     * Get orders for user
     */
    public List<SimpleOrder> getOrdersForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Cancel order
     */
    public SimpleOrder cancelOrder(String orderId) {
        SimpleOrder order = orderRepository.findByOrderId(orderId).orElse(null);

        // Eliminates if-statement using Optional.ofNullable().filter().ifPresent()
        Optional.ofNullable(order)
            .filter(o -> "PENDING".equals(o.getStatus()))
            .ifPresent(o -> {
                o.setStatus("CANCELLED");
                orderRepository.save(o);
                log.info("Order {} cancelled", orderId);
            });

        return order;
    }
    
    /**
     * Submit order to broker-auth-service
     */
    private String submitToBroker(SimpleOrder order) {
        try {
            // Eliminates ternary operator using Optional.ofNullable().orElse()
            Map<String, Object> brokerRequest = Map.of(
                "symbol", order.getSymbol(),
                "quantity", order.getQuantity(),
                "price", Optional.ofNullable(order.getPrice()).orElse(BigDecimal.ZERO),
                "orderType", order.getOrderType(),
                "orderSide", order.getSide(),
                "userId", order.getUserId().toString(),
                "brokerType", "ZERODHA",
                "clientOrderId", order.getOrderId()
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-Correlation-ID", order.getOrderId());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(brokerRequest, headers);
            String url = brokerAuthServiceUrl + "/broker-auth/orders/submit";
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            // Eliminates if-else using Optional.of().filter().flatMap() chain
            return Optional.of(response)
                .filter(r -> r.getStatusCode().is2xxSuccessful())
                .flatMap(r -> Optional.ofNullable(r.getBody())
                    .map(body -> (String) body.get("brokerOrderId")))
                .orElseThrow(() -> new RuntimeException("Broker submission failed: " + response.getStatusCode()));
            
        } catch (Exception e) {
            log.error("Failed to submit to broker: {}", e.getMessage());
            throw new RuntimeException("Broker integration failed", e);
        }
    }
}