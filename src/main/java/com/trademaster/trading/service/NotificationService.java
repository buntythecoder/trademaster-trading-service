package com.trademaster.trading.service;

import com.trademaster.trading.entity.Order;

/**
 * Notification Service
 * 
 * Service interface for real-time user notifications using Java 24 Virtual Threads.
 * Manages multi-channel notifications for order events, market alerts, and system updates.
 * 
 * Key Features:
 * - Real-time order status notifications (WebSocket, push notifications)
 * - Email and SMS notifications for critical events
 * - In-app notifications with notification center
 * - Market alerts and price notifications
 * - Risk and compliance notifications
 * 
 * Performance Targets:
 * - WebSocket notifications: <100ms delivery
 * - Push notifications: <500ms delivery
 * - Notification processing: <10ms (async processing)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface NotificationService {
    
    /**
     * Notify user that order has been placed
     * 
     * @param userId The user ID
     * @param order The placed order
     */
    void notifyOrderPlaced(Long userId, Order order);
    
    /**
     * Notify user that order has been cancelled
     * 
     * @param userId The user ID
     * @param order The cancelled order
     */
    void notifyOrderCancelled(Long userId, Order order);
    
    /**
     * Notify user of order fill
     * 
     * @param userId The user ID
     * @param order The filled order
     */
    void notifyOrderFilled(Long userId, Order order);
    
    /**
     * Notify user of order rejection
     * 
     * @param userId The user ID
     * @param order The rejected order
     * @param reason The rejection reason
     */
    void notifyOrderRejected(Long userId, Order order, String reason);
}