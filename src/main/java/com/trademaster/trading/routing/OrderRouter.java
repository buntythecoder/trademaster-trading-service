package com.trademaster.trading.routing;

import com.trademaster.trading.entity.Order;
/**
 * Order Router Interface
 * 
 * Defines the contract for routing orders to appropriate execution venues using Java 24 Virtual Threads.
 * Routes orders based on order type, market conditions, and broker capabilities.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface OrderRouter {
    
    /**
     * Route order to appropriate execution venue
     * 
     * @param order The order to route
     * @return RoutingDecision containing routing information
     */
    RoutingDecision routeOrder(Order order);
    
    /**
     * Get router priority (lower number = higher priority)
     */
    int getPriority();
    
    /**
     * Check if this router can handle the given order
     */
    boolean canHandle(Order order);
    
    /**
     * Get router name for logging and debugging
     */
    String getRouterName();
}