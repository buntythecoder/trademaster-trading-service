package com.trademaster.trading.repository;

import com.trademaster.trading.entity.Position;
import com.trademaster.trading.model.PositionSide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Position Repository
 * 
 * Data access layer for position management and portfolio tracking.
 * Provides optimized queries for position analytics and reporting.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    /**
     * Find all positions for a user
     */
    List<Position> findByUserIdOrderBySymbolAsc(Long userId);
    
    /**
     * Find position by user and symbol
     */
    Optional<Position> findByUserIdAndSymbol(Long userId, String symbol);
    
    /**
     * Find all open positions for a user
     */
    @Query("SELECT p FROM Position p WHERE p.userId = :userId AND p.quantity != 0")
    List<Position> findOpenPositionsByUserId(@Param("userId") Long userId);
    
    /**
     * Find positions by symbol
     */
    List<Position> findBySymbolOrderByUserIdAsc(String symbol);
    
    /**
     * Find positions by side
     */
    List<Position> findByUserIdAndSideOrderBySymbolAsc(Long userId, PositionSide side);
    
    /**
     * Get total position value for a user
     */
    @Query("SELECT SUM(p.marketValue) FROM Position p WHERE p.userId = :userId AND p.quantity != 0")
    Optional<BigDecimal> getTotalPositionValue(@Param("userId") Long userId);
    
    /**
     * Get total unrealized P&L for a user
     */
    @Query("SELECT SUM(p.unrealizedPnL) FROM Position p WHERE p.userId = :userId AND p.quantity != 0")
    Optional<BigDecimal> getTotalUnrealizedPnL(@Param("userId") Long userId);
    
    /**
     * Find positions updated after timestamp
     */
    @Query("SELECT p FROM Position p WHERE p.userId = :userId AND p.updatedAt > :since")
    List<Position> findPositionsUpdatedSince(@Param("userId") Long userId, @Param("since") Instant since);
    
    /**
     * Get position count for user
     */
    @Query("SELECT COUNT(p) FROM Position p WHERE p.userId = :userId AND p.quantity != 0")
    Long countOpenPositionsByUserId(@Param("userId") Long userId);
    
    /**
     * Find positions with P&L above threshold
     */
    @Query("SELECT p FROM Position p WHERE p.userId = :userId AND p.unrealizedPnL > :threshold")
    List<Position> findPositionsWithPnLAbove(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);
    
    /**
     * Find positions with P&L below threshold
     */
    @Query("SELECT p FROM Position p WHERE p.userId = :userId AND p.unrealizedPnL < :threshold")
    List<Position> findPositionsWithPnLBelow(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);
    
    /**
     * Find positions by market value range
     */
    @Query("SELECT p FROM Position p WHERE p.userId = :userId AND p.marketValue BETWEEN :minValue AND :maxValue")
    List<Position> findPositionsByValueRange(@Param("userId") Long userId, 
                                           @Param("minValue") BigDecimal minValue,
                                           @Param("maxValue") BigDecimal maxValue);
    
    /**
     * Get positions grouped by sector (assuming symbol mapping)
     */
    @Query("SELECT p.symbol, SUM(p.marketValue) FROM Position p WHERE p.userId = :userId AND p.quantity != 0 GROUP BY p.symbol")
    List<Object[]> getPositionsBySymbol(@Param("userId") Long userId);
    
    /**
     * Find largest positions by market value
     */
    @Query("SELECT p FROM Position p WHERE p.userId = :userId AND p.quantity != 0 ORDER BY p.marketValue DESC")
    List<Position> findLargestPositions(@Param("userId") Long userId);
    
    /**
     * Find positions needing rebalancing (high allocation percentage)
     */
    @Query("SELECT p FROM Position p WHERE p.userId = :userId AND p.portfolioWeight > :threshold")
    List<Position> findPositionsNeedingRebalancing(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);
    
    /**
     * Get average entry price for symbol across all users
     */
    @Query("SELECT AVG(p.averageCost) FROM Position p WHERE p.symbol = :symbol AND p.quantity != 0")
    Optional<BigDecimal> getAverageEntryPriceForSymbol(@Param("symbol") String symbol);
    
    /**
     * Find positions opened within date range
     */
    @Query("SELECT p FROM Position p WHERE p.userId = :userId AND p.createdAt BETWEEN :startDate AND :endDate")
    List<Position> findPositionsOpenedBetween(@Param("userId") Long userId,
                                            @Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate);
    
    /**
     * Delete closed positions (zero quantity) older than specified date
     */
    @Query("DELETE FROM Position p WHERE p.quantity = 0 AND p.updatedAt < :cutoffDate")
    void deleteClosedPositionsOlderThan(@Param("cutoffDate") Instant cutoffDate);
}