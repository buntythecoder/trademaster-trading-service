package com.trademaster.trading.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Broker Auth Client Fallback
 * 
 * Fallback implementation for when broker-auth-service is unavailable.
 * Provides graceful degradation and error responses.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class BrokerAuthClientFallback implements BrokerAuthClient {
    
    @Override
    public Map<String, Object> submitOrder(String brokerName, Map<String, Object> orderData, String correlationId) {
        log.error("Broker auth service unavailable for order submission - correlationId: {}, broker: {}", 
                 correlationId, brokerName);
        return Map.of(
            "success", false,
            "error", "BROKER_SERVICE_UNAVAILABLE",
            "message", "Broker authentication service is currently unavailable",
            "correlationId", correlationId
        );
    }
    
    @Override
    public Map<String, Object> modifyOrder(String brokerName, String orderId, Map<String, Object> modificationData, String correlationId) {
        log.error("Broker auth service unavailable for order modification - correlationId: {}, broker: {}, orderId: {}", 
                 correlationId, brokerName, orderId);
        return Map.of(
            "success", false,
            "error", "BROKER_SERVICE_UNAVAILABLE",
            "message", "Broker authentication service is currently unavailable",
            "correlationId", correlationId
        );
    }
    
    @Override
    public void cancelOrder(String brokerName, String orderId, String correlationId) {
        log.error("Broker auth service unavailable for order cancellation - correlationId: {}, broker: {}, orderId: {}", 
                 correlationId, brokerName, orderId);
        // Fallback: mark order as cancellation pending
        throw new RuntimeException("Broker service unavailable - order marked for cancellation retry");
    }
    
    @Override
    public Map<String, Object> getOrderStatus(String brokerName, String orderId, String correlationId) {
        log.error("Broker auth service unavailable for order status check - correlationId: {}, broker: {}, orderId: {}", 
                 correlationId, brokerName, orderId);
        return Map.of(
            "success", false,
            "error", "BROKER_SERVICE_UNAVAILABLE",
            "message", "Cannot retrieve order status - broker service unavailable",
            "correlationId", correlationId
        );
    }
    
    @Override
    public Map<String, Object> getBrokerHealth(String brokerName) {
        log.error("Broker auth service unavailable for health check - broker: {}", brokerName);
        return Map.of(
            "status", "UNAVAILABLE",
            "broker", brokerName,
            "message", "Broker authentication service is down",
            "timestamp", System.currentTimeMillis()
        );
    }
}