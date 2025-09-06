package com.trademaster.trading.repository;

import com.trademaster.trading.entity.OrderFill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Order Fill Repository Interface
 * 
 * Data access layer for OrderFill entities to track partial fills
 * and execution details for orders.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface OrderFillRepository extends JpaRepository<OrderFill, Long> {
    
    // Basic finders
    
    /**
     * Find all fills for an order
     */
    List<OrderFill> findByOrderIdOrderByFillTime(Long orderId);
    
    /**
     * Find fills by broker fill ID
     */
    Optional<OrderFill> findByBrokerFillId(String brokerFillId);
    
    /**
     * Find fills in time range
     */
    List<OrderFill> findByFillTimeBetweenOrderByFillTime(Instant startTime, Instant endTime);
    
    /**
     * Find latest fill for order
     */
    Optional<OrderFill> findFirstByOrderIdOrderByFillTimeDesc(Long orderId);
    
    /**
     * Check if order has any fills
     */
    boolean existsByOrderId(Long orderId);
    
    // Aggregation queries
    
    /**
     * Get total filled quantity for order
     */
    @Query("SELECT COALESCE(SUM(of.fillQuantity), 0) FROM OrderFill of WHERE of.orderId = :orderId")
    Integer getTotalFilledQuantity(@Param("orderId") Long orderId);
    
    /**
     * Calculate average fill price for order
     */
    @Query("SELECT CASE WHEN SUM(of.fillQuantity) = 0 THEN 0 " +
           "ELSE SUM(of.fillPrice * of.fillQuantity) / SUM(of.fillQuantity) END " +
           "FROM OrderFill of WHERE of.orderId = :orderId")
    BigDecimal getAverageFillPrice(@Param("orderId") Long orderId);
    
    /**
     * Get total fill value for order
     */
    @Query("SELECT COALESCE(SUM(of.fillPrice * of.fillQuantity), 0) FROM OrderFill of " +
           "WHERE of.orderId = :orderId")
    BigDecimal getTotalFillValue(@Param("orderId") Long orderId);
    
    /**
     * Get total commission for order
     */
    @Query("SELECT COALESCE(SUM(of.commission), 0) FROM OrderFill of WHERE of.orderId = :orderId")
    BigDecimal getTotalCommission(@Param("orderId") Long orderId);
    
    /**
     * Get total taxes for order
     */
    @Query("SELECT COALESCE(SUM(of.taxes), 0) FROM OrderFill of WHERE of.orderId = :orderId")
    BigDecimal getTotalTaxes(@Param("orderId") Long orderId);
    
    /**
     * Get fill count for order
     */
    @Query("SELECT COUNT(of) FROM OrderFill of WHERE of.orderId = :orderId")
    Long getFillCount(@Param("orderId") Long orderId);
    
    /**
     * Get first fill time for order
     */
    @Query("SELECT MIN(of.fillTime) FROM OrderFill of WHERE of.orderId = :orderId")
    Optional<Instant> getFirstFillTime(@Param("orderId") Long orderId);
    
    /**
     * Get last fill time for order
     */
    @Query("SELECT MAX(of.fillTime) FROM OrderFill of WHERE of.orderId = :orderId")
    Optional<Instant> getLastFillTime(@Param("orderId") Long orderId);
    
    /**
     * Get execution summary for order
     */
    @Query("SELECT NEW com.trademaster.trading.dto.ExecutionSummary(" +
           "COUNT(of), " +
           "SUM(of.fillQuantity), " +
           "CASE WHEN SUM(of.fillQuantity) = 0 THEN 0 " +
           "ELSE SUM(of.fillPrice * of.fillQuantity) / SUM(of.fillQuantity) END, " +
           "SUM(of.fillPrice * of.fillQuantity), " +
           "SUM(of.commission), " +
           "SUM(of.taxes), " +
           "MIN(of.fillTime), " +
           "MAX(of.fillTime)) " +
           "FROM OrderFill of WHERE of.orderId = :orderId")
    Optional<ExecutionSummary> getExecutionSummary(@Param("orderId") Long orderId);
    
    /**
     * DTO for execution summary
     */
    record ExecutionSummary(
        Long fillCount,
        Integer totalQuantity,
        BigDecimal averagePrice,
        BigDecimal totalValue,
        BigDecimal totalCommission,
        BigDecimal totalTaxes,
        Instant firstFillTime,
        Instant lastFillTime
    ) {}
}