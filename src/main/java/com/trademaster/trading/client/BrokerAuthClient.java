package com.trademaster.trading.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Broker Auth Service Client
 * 
 * Feign client for communication with the broker-auth-service.
 * Uses service discovery and circuit breaker patterns for resilience.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@FeignClient(
    name = "broker-auth-service",
    path = "/api/v1",
    fallback = BrokerAuthClientFallback.class
)
public interface BrokerAuthClient {
    
    /**
     * Submit order to broker via broker-auth-service
     */
    @PostMapping("/broker/{brokerName}/orders")
    Map<String, Object> submitOrder(
        @PathVariable("brokerName") String brokerName,
        @RequestBody Map<String, Object> orderData,
        @RequestHeader("Correlation-ID") String correlationId
    );
    
    /**
     * Modify existing order
     */
    @PutMapping("/broker/{brokerName}/orders/{orderId}/modify")
    Map<String, Object> modifyOrder(
        @PathVariable("brokerName") String brokerName,
        @PathVariable("orderId") String orderId,
        @RequestBody Map<String, Object> modificationData,
        @RequestHeader("Correlation-ID") String correlationId
    );
    
    /**
     * Cancel order
     */
    @DeleteMapping("/broker/{brokerName}/orders/{orderId}/cancel")
    void cancelOrder(
        @PathVariable("brokerName") String brokerName,
        @PathVariable("orderId") String orderId,
        @RequestHeader("Correlation-ID") String correlationId
    );
    
    /**
     * Get order status from broker
     */
    @GetMapping("/broker/{brokerName}/orders/{orderId}/status")
    Map<String, Object> getOrderStatus(
        @PathVariable("brokerName") String brokerName,
        @PathVariable("orderId") String orderId,
        @RequestHeader("Correlation-ID") String correlationId
    );
    
    /**
     * Health check for broker connection
     */
    @GetMapping("/broker/{brokerName}/health")
    Map<String, Object> getBrokerHealth(
        @PathVariable("brokerName") String brokerName
    );
}