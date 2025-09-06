package com.trademaster.trading.repository;

import com.trademaster.trading.entity.Trade;
import com.trademaster.trading.model.OrderSide;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Trade Repository Interface
 * 
 * Data access layer for Trade entities with optimized queries
 * for trade reporting, performance analysis, and compliance.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    
    // Basic finders
    
    /**
     * Find trade by trade ID
     */
    Optional<Trade> findByTradeId(String tradeId);
    
    /**
     * Find trades by user ID
     */
    Page<Trade> findByUserIdOrderByTradeTimeDesc(Long userId, Pageable pageable);
    
    /**
     * Find trades by user and symbol
     */
    Page<Trade> findByUserIdAndSymbolOrderByTradeTimeDesc(Long userId, String symbol, Pageable pageable);
    
    /**
     * Find trades by user and side
     */
    List<Trade> findByUserIdAndSideOrderByTradeTimeDesc(Long userId, OrderSide side);
    
    /**
     * Find trades by order ID
     */
    List<Trade> findByOrderIdOrderByTradeTime(Long orderId);
    
    /**
     * Find trades by date range
     */
    List<Trade> findByUserIdAndTradeTimeBetweenOrderByTradeTime(
        Long userId, Instant startTime, Instant endTime);
    
    /**
     * Find trades by settlement date
     */
    List<Trade> findByUserIdAndSettlementDateOrderByTradeTime(Long userId, LocalDate settlementDate);
    
    // Analytical queries
    
    /**
     * Get total trade value for user in date range
     */
    @Query("SELECT COALESCE(SUM(t.tradeValue), 0) FROM Trade t " +
           "WHERE t.userId = :userId " +
           "AND t.tradeTime BETWEEN :startTime AND :endTime")
    BigDecimal getTotalTradeValue(@Param("userId") Long userId, 
                                 @Param("startTime") Instant startTime, 
                                 @Param("endTime") Instant endTime);
    
    /**
     * Get total trade count for user in date range
     */
    @Query("SELECT COUNT(t) FROM Trade t " +
           "WHERE t.userId = :userId " +
           "AND t.tradeTime BETWEEN :startTime AND :endTime")
    Long getTotalTradeCount(@Param("userId") Long userId, 
                           @Param("startTime") Instant startTime, 
                           @Param("endTime") Instant endTime);
    
    /**
     * Get total commission paid by user in date range
     */
    @Query("SELECT COALESCE(SUM(t.commission), 0) FROM Trade t " +
           "WHERE t.userId = :userId " +
           "AND t.tradeTime BETWEEN :startTime AND :endTime")
    BigDecimal getTotalCommission(@Param("userId") Long userId, 
                                 @Param("startTime") Instant startTime, 
                                 @Param("endTime") Instant endTime);
    
    /**
     * Get daily trade count for user
     */
    @Query("SELECT COUNT(t) FROM Trade t " +
           "WHERE t.userId = :userId " +
           "AND DATE(t.tradeTime) = :tradeDate")
    Long getDailyTradeCount(@Param("userId") Long userId, @Param("tradeDate") LocalDate tradeDate);
    
    /**
     * Get average trade size for user and symbol
     */
    @Query("SELECT AVG(t.quantity) FROM Trade t " +
           "WHERE t.userId = :userId AND t.symbol = :symbol")
    Double getAverageTradeSize(@Param("userId") Long userId, @Param("symbol") String symbol);
    
    /**
     * Get top traded symbols for user
     */
    @Query("SELECT t.symbol, COUNT(t), SUM(t.tradeValue) FROM Trade t " +
           "WHERE t.userId = :userId " +
           "AND t.tradeTime BETWEEN :startTime AND :endTime " +
           "GROUP BY t.symbol " +
           "ORDER BY SUM(t.tradeValue) DESC")
    List<Object[]> getTopTradedSymbols(@Param("userId") Long userId, 
                                      @Param("startTime") Instant startTime, 
                                      @Param("endTime") Instant endTime);
    
    /**
     * Get buy vs sell breakdown for user
     */
    @Query("SELECT t.side, COUNT(t), SUM(t.tradeValue) FROM Trade t " +
           "WHERE t.userId = :userId " +
           "AND t.tradeTime BETWEEN :startTime AND :endTime " +
           "GROUP BY t.side")
    List<Object[]> getBuySellBreakdown(@Param("userId") Long userId, 
                                      @Param("startTime") Instant startTime, 
                                      @Param("endTime") Instant endTime);
    
    /**
     * Check if user has traded symbol recently
     */
    boolean existsByUserIdAndSymbolAndTradeTimeAfter(Long userId, String symbol, Instant since);
    
    /**
     * Get latest trade for user and symbol
     */
    Optional<Trade> findFirstByUserIdAndSymbolOrderByTradeTimeDesc(Long userId, String symbol);
    
    /**
     * Get trades for P&L calculation
     */
    @Query("SELECT t FROM Trade t " +
           "WHERE t.userId = :userId " +
           "AND t.symbol = :symbol " +
           "ORDER BY t.tradeTime ASC")
    List<Trade> getTradesForPnLCalculation(@Param("userId") Long userId, @Param("symbol") String symbol);
}