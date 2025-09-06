package com.trademaster.trading.service;

import com.trademaster.trading.entity.Order;

/**
 * Broker Integration Service
 * 
 * Service interface for broker connectivity and order routing using Java 24 Virtual Threads.
 * Manages connections to multiple brokers, order routing, and execution reporting.
 * 
 * Key Features:
 * - Multi-broker connectivity (Zerodha, Angel One, ICICI Direct, etc.)
 * - Smart order routing (best execution algorithms)
 * - Real-time execution reporting and fill notifications
 * - Broker-specific protocol handling (FIX, REST, WebSocket)
 * - Connection health monitoring and failover
 * 
 * Performance Targets:
 * - Order submission: <20ms (broker latency dependent)
 * - Connection establishment: <100ms
 * - Fill notification processing: <5ms
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface BrokerIntegrationService {
    
    /**
     * Submit order to broker for execution
     * 
     * @param order The order to submit
     * @return String broker order ID
     * @throws BrokerIntegrationException if submission fails
     */
    String submitOrder(Order order);
    
    /**
     * Cancel order with broker
     * 
     * @param brokerOrderId The broker order ID to cancel
     * @throws BrokerIntegrationException if cancellation fails
     */
    void cancelOrder(String brokerOrderId);
    
    /**
     * Modify order with broker
     * 
     * @param brokerOrderId The broker order ID to modify
     * @param modifiedOrder The modified order details
     * @return String new broker order ID (if broker assigns new ID)
     * @throws BrokerIntegrationException if modification fails
     */
    String modifyOrder(String brokerOrderId, Order modifiedOrder);
}