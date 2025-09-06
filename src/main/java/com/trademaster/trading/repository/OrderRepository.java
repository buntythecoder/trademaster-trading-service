package com.trademaster.trading.repository;

import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Order Repository
 * 
 * Extends OrderJpaRepository with additional methods needed by OrderService.
 * Provides comprehensive order data access with Virtual Thread compatibility.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Repository
public interface OrderRepository extends OrderJpaRepository {
    
    
    /**
     * Find all orders by user ID
     */
    List<Order> findByUserId(Long userId);
    
    /**
     * Find orders with expiry date before specified date and specific statuses
     */
    @Query("SELECT o FROM Order o WHERE o.expiryDate < :date AND o.status IN :statuses")
    List<Order> findByExpiryDateBeforeAndStatusIn(@Param("date") LocalDate date, @Param("statuses") List<OrderStatus> statuses);
    
    /**
     * Find orders by user ID and status
     */
    List<Order> findByUserIdAndStatusIn(Long userId, List<OrderStatus> statuses);
    
    /**
     * Find active orders by user ID ordered by creation date
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusInOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("statuses") List<OrderStatus> statuses);
    
    /**
     * Find order by ID and user ID for security
     */
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.userId = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    /**
     * Find order by order ID and user ID for security  
     */
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.userId = :userId")
    Optional<Order> findByOrderIdAndUserId(@Param("orderId") String orderId, @Param("userId") Long userId);
    
    /**
     * Find orders by user and symbol
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.symbol = :symbol")
    List<Order> findByUserIdAndSymbol(@Param("userId") Long userId, @Param("symbol") String symbol);
    
    /**
     * Find orders by user, symbol and status
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.symbol = :symbol AND o.status = :status")
    List<Order> findByUserIdAndSymbolAndStatus(@Param("userId") Long userId, @Param("symbol") String symbol, @Param("status") OrderStatus status);
    
    /**
     * Find expired orders that should be cancelled
     */
    @Query("SELECT o FROM Order o WHERE o.expiryDate < CURRENT_DATE AND o.status IN ('ACKNOWLEDGED', 'PARTIALLY_FILLED')")
    List<Order> findExpiredOrders();
    
    /**
     * Count orders by user and status
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);
}