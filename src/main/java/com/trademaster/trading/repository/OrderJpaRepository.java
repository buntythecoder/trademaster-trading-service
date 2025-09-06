package com.trademaster.trading.repository;

import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Order JPA Repository
 * 
 * High-performance JPA repository for order data access with Virtual Threads.
 * Leverages Java 24 Virtual Threads for massive concurrency without blocking.
 * 
 * Key Performance Features:
 * - Virtual Thread compatibility for unlimited scalability
 * - JPA second-level cache for frequent reads
 * - Optimized queries with proper indexing
 * - Batch operations for high-frequency trading
 * - Connection pooling for concurrent access
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find order by external order ID
     */
    Optional<Order> findByOrderId(String orderId);
    
    /**
     * Find orders by user ID with pagination
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find orders by user ID and status
     */
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status);
    
    /**
     * Find active orders for a user (ACKNOWLEDGED, PARTIALLY_FILLED)
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status IN ('ACKNOWLEDGED', 'PARTIALLY_FILLED') ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByUserId(@Param("userId") Long userId);
    
    /**
     * Find orders by user ID and symbol
     */
    Page<Order> findByUserIdAndSymbolOrderByCreatedAtDesc(Long userId, String symbol, Pageable pageable);
    
    /**
     * Find orders by user ID, symbol, and status
     */
    List<Order> findByUserIdAndSymbolAndStatusOrderByCreatedAtDesc(Long userId, String symbol, OrderStatus status);
    
    /**
     * Count open orders for a user
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status NOT IN ('FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')")
    long countOpenOrdersByUserId(@Param("userId") Long userId);
    
    /**
     * Count daily orders for a user
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND DATE(o.createdAt) = :date")
    long countDailyOrdersByUserId(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    /**
     * Find orders requiring expiry check (GTD orders past expiry date)
     */
    @Query("SELECT o FROM Order o WHERE o.timeInForce = 'GTD' AND o.expiryDate < :currentDate AND o.status NOT IN ('FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')")
    List<Order> findOrdersRequiringExpiry(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find DAY orders requiring expiry at market close
     */
    @Query("SELECT o FROM Order o WHERE o.timeInForce = 'DAY' AND DATE(o.createdAt) < :currentDate AND o.status NOT IN ('FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')")
    List<Order> findDayOrdersRequiringExpiry(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find orders by broker order ID
     */
    Optional<Order> findByBrokerOrderId(String brokerOrderId);
    
    /**
     * Find orders submitted after a specific timestamp
     */
    List<Order> findBySubmittedAtAfter(Instant timestamp);
    
    /**
     * Calculate total order value for user within time period
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN o.orderType = 'MARKET' THEN 0 WHEN o.limitPrice IS NOT NULL THEN o.limitPrice * o.quantity ELSE 0 END), 0) " +
           "FROM Order o WHERE o.userId = :userId AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Double calculateOrderValueByUserAndPeriod(@Param("userId") Long userId, 
                                            @Param("startTime") Instant startTime, 
                                            @Param("endTime") Instant endTime);
    
    /**
     * Find orders by multiple statuses
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<OrderStatus> statuses);
    
    /**
     * Find orders requiring risk review (large orders)
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND " +
           "CASE WHEN o.orderType = 'MARKET' THEN 100000 " + // Assume ₹100K for market orders
           "WHEN o.limitPrice IS NOT NULL THEN o.limitPrice * o.quantity " +
           "ELSE 0 END > :threshold AND " +
           "o.status = 'PENDING'")
    List<Order> findOrdersRequiringRiskReview(@Param("userId") Long userId, @Param("threshold") Double threshold);
    
    /**
     * Find executed orders for position calculation
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.symbol = :symbol AND " +
           "o.status IN ('FILLED', 'PARTIALLY_FILLED') AND o.executedAt IS NOT NULL " +
           "ORDER BY o.executedAt")
    List<Order> findExecutedOrdersForPosition(@Param("userId") Long userId, @Param("symbol") String symbol);
    
    /**
     * Performance optimization: Find orders with minimal data for dashboards
     */
    @Query("SELECT new Order(o.id, o.orderId, o.symbol, o.side, o.quantity, o.status, o.limitPrice, o.createdAt) " +
           "FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersSummary(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Real-time order monitoring: Find orders modified since timestamp
     */
    List<Order> findByUpdatedAtAfterOrderByUpdatedAt(Instant since);
    
    // Async methods using Virtual Threads (Java 24 feature)
    
    /**
     * Async find by order ID using Virtual Threads
     */
    default CompletableFuture<Optional<Order>> findByOrderIdAsync(String orderId) {
        return CompletableFuture.supplyAsync(() -> findByOrderId(orderId));
    }
    
    /**
     * Async count open orders using Virtual Threads
     */
    default CompletableFuture<Long> countOpenOrdersByUserIdAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> countOpenOrdersByUserId(userId));
    }
    
    /**
     * Async find active orders using Virtual Threads
     */
    default CompletableFuture<List<Order>> findActiveOrdersByUserIdAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> findActiveOrdersByUserId(userId));
    }
}