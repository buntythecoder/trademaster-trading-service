package com.trademaster.trading.repository;

import com.trademaster.trading.entity.SimplifiedPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Simplified Position Repository Interface
 * 
 * Data access layer for SimplifiedPosition entities (portfolios table).
 * Provides optimized queries for portfolio management and position tracking.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Schema-Aligned)
 */
@Repository
public interface SimplifiedPositionRepository extends JpaRepository<SimplifiedPosition, Long> {
    
    // Basic finders
    
    /**
     * Find position by user, symbol, and exchange
     */
    Optional<SimplifiedPosition> findByUserIdAndSymbolAndExchange(Long userId, String symbol, String exchange);
    
    /**
     * Find all positions for user
     */
    List<SimplifiedPosition> findByUserIdOrderBySymbol(Long userId);
    
    /**
     * Find positions by symbol
     */
    List<SimplifiedPosition> findBySymbolOrderByUserId(String symbol);
    
    /**
     * Find active positions (non-zero quantity)
     */
    List<SimplifiedPosition> findByUserIdAndQuantityNotOrderByMarketValueDesc(Long userId, Integer quantity);
    
    /**
     * Find long positions
     */
    @Query("SELECT p FROM SimplifiedPosition p WHERE p.userId = :userId AND p.quantity > 0")
    List<SimplifiedPosition> findLongPositions(@Param("userId") Long userId);
    
    /**
     * Find short positions
     */
    @Query("SELECT p FROM SimplifiedPosition p WHERE p.userId = :userId AND p.quantity < 0")
    List<SimplifiedPosition> findShortPositions(@Param("userId") Long userId);
    
    // Portfolio analytics
    
    /**
     * Get total portfolio value for user
     */
    @Query("SELECT COALESCE(SUM(p.marketValue), 0) FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0")
    BigDecimal getTotalPortfolioValue(@Param("userId") Long userId);
    
    /**
     * Get total unrealized P&L for user
     */
    @Query("SELECT COALESCE(SUM(p.unrealizedPnl), 0) FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0")
    BigDecimal getTotalUnrealizedPnL(@Param("userId") Long userId);
    
    /**
     * Get total realized P&L for user
     */
    @Query("SELECT COALESCE(SUM(p.realizedPnl), 0) FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId")
    BigDecimal getTotalRealizedPnL(@Param("userId") Long userId);
    
    /**
     * Get position count for user
     */
    @Query("SELECT COUNT(p) FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0")
    Long getActivePositionCount(@Param("userId") Long userId);
    
    /**
     * Get top positions by market value
     */
    @Query("SELECT p FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0 " +
           "ORDER BY p.marketValue DESC")
    List<SimplifiedPosition> getTopPositionsByValue(@Param("userId") Long userId);
    
    /**
     * Get top gainers
     */
    @Query("SELECT p FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0 AND p.unrealizedPnl > 0 " +
           "ORDER BY p.unrealizedPnl DESC")
    List<SimplifiedPosition> getTopGainers(@Param("userId") Long userId);
    
    /**
     * Get top losers
     */
    @Query("SELECT p FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0 AND p.unrealizedPnl < 0 " +
           "ORDER BY p.unrealizedPnl ASC")
    List<SimplifiedPosition> getTopLosers(@Param("userId") Long userId);
    
    /**
     * Calculate portfolio P&L percentage
     */
    @Query("SELECT CASE WHEN SUM(p.marketValue - p.unrealizedPnl) = 0 THEN 0 " +
           "ELSE (SUM(p.unrealizedPnl) / SUM(p.marketValue - p.unrealizedPnl)) * 100 END " +
           "FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0")
    BigDecimal getPortfolioPnLPercent(@Param("userId") Long userId);
    
    /**
     * Get positions requiring margin calls
     */
    @Query("SELECT p FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId " +
           "AND p.quantity < 0 " +  // Short positions
           "AND p.unrealizedPnl < :marginThreshold")
    List<SimplifiedPosition> getMarginCallPositions(@Param("userId") Long userId, 
                                                   @Param("marginThreshold") BigDecimal marginThreshold);
    
    /**
     * Get concentration risk (largest position percentage)
     */
    @Query("SELECT MAX(p.marketValue) / " +
           "(SELECT SUM(p2.marketValue) FROM SimplifiedPosition p2 " +
           " WHERE p2.userId = :userId AND p2.quantity != 0) * 100 " +
           "FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0")
    BigDecimal getConcentrationRisk(@Param("userId") Long userId);
    
    /**
     * Check if user has position in symbol
     */
    boolean existsByUserIdAndSymbolAndQuantityNot(Long userId, String symbol, Integer quantity);
    
    /**
     * Get symbols with positions for user
     */
    @Query("SELECT DISTINCT p.symbol FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0")
    List<String> getSymbolsWithPositions(@Param("userId") Long userId);
    
    /**
     * Get portfolio summary
     */
    @Query("SELECT " +
           "COUNT(p) as positionCount, " +
           "SUM(CASE WHEN p.quantity > 0 THEN 1 ELSE 0 END) as longCount, " +
           "SUM(CASE WHEN p.quantity < 0 THEN 1 ELSE 0 END) as shortCount, " +
           "SUM(p.marketValue) as totalValue, " +
           "SUM(p.unrealizedPnl) as totalUnrealizedPnl, " +
           "SUM(p.realizedPnl) as totalRealizedPnl " +
           "FROM SimplifiedPosition p " +
           "WHERE p.userId = :userId AND p.quantity != 0")
    Object[] getPortfolioSummary(@Param("userId") Long userId);
}