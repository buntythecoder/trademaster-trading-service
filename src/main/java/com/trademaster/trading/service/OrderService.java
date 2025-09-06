package com.trademaster.trading.service;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.common.Result;
import com.trademaster.trading.common.TradeError;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Order Service Interface
 * 
 * Core business logic interface for order management operations using Java 24 Virtual Threads.
 * Defines the contract for order placement, tracking, and lifecycle management.
 * 
 * Uses standard blocking I/O with Virtual Threads for unlimited scalability and simplified code.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface OrderService {
    
    /**
     * Place a new order with full validation and risk checks
     * 
     * @param orderRequest The order request
     * @param userId The user placing the order
     * @return Result containing either OrderResponse on success or TradeError on failure
     */
    Result<OrderResponse, TradeError> placeOrder(OrderRequest orderRequest, Long userId);
    
    /**
     * Get order details by order ID
     * 
     * @param orderId The order ID
     * @param userId The user ID for authorization
     * @return Result containing either OrderResponse on success or TradeError on failure
     */
    Result<OrderResponse, TradeError> getOrder(String orderId, Long userId);
    
    /**
     * Get orders for a user with pagination
     * 
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Result containing either List<OrderResponse> on success or TradeError on failure
     */
    Result<List<OrderResponse>, TradeError> getOrdersByUser(Long userId, Pageable pageable);
    
    /**
     * Get orders by user and status
     * 
     * @param userId The user ID
     * @param status The order status
     * @return Result containing either List<OrderResponse> on success or TradeError on failure
     */
    Result<List<OrderResponse>, TradeError> getOrdersByUserAndStatus(Long userId, OrderStatus status);
    
    /**
     * Get orders by user and symbol
     * 
     * @param userId The user ID
     * @param symbol The trading symbol
     * @param pageable Pagination parameters
     * @return Result containing either List<OrderResponse> on success or TradeError on failure
     */
    Result<List<OrderResponse>, TradeError> getOrdersByUserAndSymbol(Long userId, String symbol, Pageable pageable);
    
    /**
     * Get orders by user, symbol, and status
     * 
     * @param userId The user ID
     * @param symbol The trading symbol
     * @param status The order status
     * @return Result containing either List<OrderResponse> on success or TradeError on failure
     */
    Result<List<OrderResponse>, TradeError> getOrdersByUserSymbolAndStatus(Long userId, String symbol, OrderStatus status);
    
    /**
     * Get active orders for a user
     * 
     * @param userId The user ID
     * @return Result containing either List<OrderResponse> on success or TradeError on failure
     */
    Result<List<OrderResponse>, TradeError> getActiveOrders(Long userId);
    
    /**
     * Modify an existing order
     * 
     * @param orderId The order ID
     * @param modificationRequest The modification request
     * @param userId The user ID for authorization
     * @return Result containing either OrderResponse on success or TradeError on failure
     */
    Result<OrderResponse, TradeError> modifyOrder(String orderId, OrderRequest modificationRequest, Long userId);
    
    /**
     * Cancel an order
     * 
     * @param orderId The order ID
     * @param userId The user ID for authorization
     * @return Result containing either OrderResponse on success or TradeError on failure
     */
    Result<OrderResponse, TradeError> cancelOrder(String orderId, Long userId);
    
    /**
     * Get order status (lightweight operation)
     * 
     * @param orderId The order ID
     * @param userId The user ID for authorization
     * @return Result containing either OrderStatus on success or TradeError on failure
     */
    Result<OrderStatus, TradeError> getOrderStatus(String orderId, Long userId);
    
    /**
     * Get order counts by status for dashboard
     * 
     * @param userId The user ID
     * @return Result containing either Map<String, Long> on success or TradeError on failure
     */
    Result<Map<String, Long>, TradeError> getOrderCounts(Long userId);
    
    /**
     * Process order fill notification from broker
     * 
     * @param order The order entity
     * @param fillQuantity The fill quantity
     * @param fillPrice The fill price
     * @return Order containing updated order
     */
    Order processOrderFill(Order order, Integer fillQuantity, BigDecimal fillPrice);
    
    /**
     * Update order status
     * 
     * @param orderId The order ID
     * @param newStatus The new status
     * @param reason Optional reason for status change
     * @return Order containing updated order
     */
    Order updateOrderStatus(String orderId, OrderStatus newStatus, String reason);
    
    /**
     * Expire orders based on time in force rules
     * 
     * @return Long containing count of expired orders
     */
    Long expireOrders();
}