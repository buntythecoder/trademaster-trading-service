package com.trademaster.trading.repository;

import com.trademaster.trading.entity.PortfolioHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Portfolio History Repository Interface
 * 
 * Data access layer for PortfolioHistory entities to track
 * historical portfolio performance and analytics.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface PortfolioHistoryRepository extends JpaRepository<PortfolioHistory, Long> {
    
    // Basic finders
    
    /**
     * Find portfolio history for user on specific date
     */
    List<PortfolioHistory> findByUserIdAndSnapshotDateOrderBySymbol(Long userId, LocalDate snapshotDate);
    
    /**
     * Find portfolio history for user and symbol
     */
    List<PortfolioHistory> findByUserIdAndSymbolOrderBySnapshotDateDesc(Long userId, String symbol);
    
    /**
     * Find specific portfolio history record
     */
    Optional<PortfolioHistory> findByUserIdAndSymbolAndExchangeAndSnapshotDate(
        Long userId, String symbol, String exchange, LocalDate snapshotDate);
    
    /**
     * Find portfolio history in date range
     */
    List<PortfolioHistory> findByUserIdAndSnapshotDateBetweenOrderBySnapshotDate(
        Long userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * Find latest portfolio snapshot for user
     */
    List<PortfolioHistory> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);
    
    /**
     * Get latest snapshot date for user
     */
    @Query("SELECT MAX(ph.snapshotDate) FROM PortfolioHistory ph WHERE ph.userId = :userId")
    Optional<LocalDate> getLatestSnapshotDate(@Param("userId") Long userId);
    
    // Performance analytics
    
    /**
     * Get portfolio value on specific date
     */
    @Query("SELECT COALESCE(SUM(ph.marketValue), 0) FROM PortfolioHistory ph " +
           "WHERE ph.userId = :userId AND ph.snapshotDate = :snapshotDate")
    BigDecimal getPortfolioValueOnDate(@Param("userId") Long userId, @Param("snapshotDate") LocalDate snapshotDate);
    
    /**
     * Get total unrealized P&L on specific date
     */
    @Query("SELECT COALESCE(SUM(ph.unrealizedPnl), 0) FROM PortfolioHistory ph " +
           "WHERE ph.userId = :userId AND ph.snapshotDate = :snapshotDate")
    BigDecimal getUnrealizedPnLOnDate(@Param("userId") Long userId, @Param("snapshotDate") LocalDate snapshotDate);
    
    /**
     * Get portfolio performance between dates
     */
    @Query("SELECT " +
           "(SELECT COALESCE(SUM(ph1.marketValue), 0) FROM PortfolioHistory ph1 " +
           " WHERE ph1.userId = :userId AND ph1.snapshotDate = :endDate) - " +
           "(SELECT COALESCE(SUM(ph2.marketValue), 0) FROM PortfolioHistory ph2 " +
           " WHERE ph2.userId = :userId AND ph2.snapshotDate = :startDate)")
    BigDecimal getPortfolioPerformance(@Param("userId") Long userId, 
                                      @Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);
    
    /**
     * Get top performing symbols in date range
     */
    @Query("SELECT ph.symbol, " +
           "MAX(ph.marketValue) - MIN(ph.marketValue) as performance " +
           "FROM PortfolioHistory ph " +
           "WHERE ph.userId = :userId " +
           "AND ph.snapshotDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ph.symbol " +
           "ORDER BY performance DESC")
    List<Object[]> getTopPerformingSymbols(@Param("userId") Long userId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
    
    /**
     * Get position count on specific date
     */
    @Query("SELECT COUNT(ph) FROM PortfolioHistory ph " +
           "WHERE ph.userId = :userId AND ph.snapshotDate = :snapshotDate " +
           "AND ph.quantity != 0")
    Long getPositionCountOnDate(@Param("userId") Long userId, @Param("snapshotDate") LocalDate snapshotDate);
    
    /**
     * Get daily portfolio snapshots for chart data
     */
    @Query("SELECT ph.snapshotDate, SUM(ph.marketValue), SUM(ph.unrealizedPnl) " +
           "FROM PortfolioHistory ph " +
           "WHERE ph.userId = :userId " +
           "AND ph.snapshotDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ph.snapshotDate " +
           "ORDER BY ph.snapshotDate")
    List<Object[]> getDailyPortfolioSnapshots(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
    
    /**
     * Get symbol allocation history
     */
    @Query("SELECT ph.symbol, ph.snapshotDate, ph.marketValue, ph.unrealizedPnl " +
           "FROM PortfolioHistory ph " +
           "WHERE ph.userId = :userId " +
           "AND ph.snapshotDate BETWEEN :startDate AND :endDate " +
           "AND ph.quantity != 0 " +
           "ORDER BY ph.snapshotDate, ph.marketValue DESC")
    List<Object[]> getSymbolAllocationHistory(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
    
    /**
     * Check if snapshot exists for date
     */
    boolean existsByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);
    
    /**
     * Get available snapshot dates for user
     */
    @Query("SELECT DISTINCT ph.snapshotDate FROM PortfolioHistory ph " +
           "WHERE ph.userId = :userId " +
           "ORDER BY ph.snapshotDate DESC")
    List<LocalDate> getAvailableSnapshotDates(@Param("userId") Long userId);
    
    /**
     * Calculate portfolio return between two dates
     */
    default BigDecimal calculatePortfolioReturn(Long userId, LocalDate startDate, LocalDate endDate) {
        BigDecimal startValue = getPortfolioValueOnDate(userId, startDate);
        BigDecimal endValue = getPortfolioValueOnDate(userId, endDate);
        
        if (startValue.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        return endValue.subtract(startValue).divide(startValue, 4, java.math.RoundingMode.HALF_UP);
    }
}