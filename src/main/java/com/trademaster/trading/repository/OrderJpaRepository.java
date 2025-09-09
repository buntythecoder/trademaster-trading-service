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

@Repository
public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderId(String orderId);
    
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, OrderStatus status);
    
    Page<Order> findByUserIdAndSymbolOrderByCreatedAtDesc(Long userId, String symbol, Pageable pageable);
    
    List<Order> findByUserIdAndSymbolAndStatusOrderByCreatedAtDesc(Long userId, String symbol, OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status IN ('ACKNOWLEDGED', 'PARTIALLY_FILLED') ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status NOT IN ('FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')")
    long countOpenOrdersByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND DATE(o.createdAt) = :date")
    long countDailyOrdersByUserId(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query("SELECT o FROM Order o WHERE o.timeInForce = 'GTD' AND o.expiryDate < :currentDate AND o.status NOT IN ('FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')")
    List<Order> findOrdersRequiringExpiry(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT o FROM Order o WHERE o.timeInForce = 'DAY' AND DATE(o.createdAt) < :currentDate AND o.status NOT IN ('FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')")
    List<Order> findDayOrdersRequiringExpiry(@Param("currentDate") LocalDate currentDate);
    
    Optional<Order> findByBrokerOrderId(String brokerOrderId);
    
    List<Order> findBySubmittedAtAfter(Instant timestamp);
    
    @Query("SELECT COALESCE(SUM(CASE WHEN o.orderType = 'MARKET' THEN 0 WHEN o.limitPrice IS NOT NULL THEN o.limitPrice * o.quantity ELSE 0 END), 0) " +
           "FROM Order o WHERE o.userId = :userId AND o.createdAt >= :startTime AND o.createdAt <= :endTime")
    Double calculateOrderValueByUserAndPeriod(@Param("userId") Long userId, 
                                            @Param("startTime") Instant startTime, 
                                            @Param("endTime") Instant endTime);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<OrderStatus> statuses);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND " +
           "CASE WHEN o.orderType = 'MARKET' THEN 100000 " +
           "WHEN o.limitPrice IS NOT NULL THEN o.limitPrice * o.quantity " +
           "ELSE 0 END > :threshold AND " +
           "o.status = 'PENDING'")
    List<Order> findOrdersRequiringRiskReview(@Param("userId") Long userId, @Param("threshold") Double threshold);
    
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.symbol = :symbol AND " +
           "o.status IN ('FILLED', 'PARTIALLY_FILLED') AND o.executedAt IS NOT NULL " +
           "ORDER BY o.executedAt")
    List<Order> findExecutedOrdersForPosition(@Param("userId") Long userId, @Param("symbol") String symbol);
    
    List<Order> findByUpdatedAtAfterOrderByUpdatedAt(Instant since);
    
    // Explicit query to avoid Spring Data auto-generation issues  
    @Query("SELECT o FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    Page<Order> findRecentOrdersSummary(@Param("userId") Long userId, Pageable pageable);
}