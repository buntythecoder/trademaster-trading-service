package com.trademaster.trading.repository;

import com.trademaster.trading.entity.SimplifiedRiskLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Simplified Risk Limit Repository Interface
 * 
 * Data access layer for SimplifiedRiskLimit entities (risk_limits table).
 * Provides queries for risk management and compliance validation.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Schema-Aligned)
 */
@Repository
public interface SimplifiedRiskLimitRepository extends JpaRepository<SimplifiedRiskLimit, Long> {
    
    // Basic finders
    
    /**
     * Find risk limits by user ID (unique constraint)
     */
    Optional<SimplifiedRiskLimit> findByUserId(Long userId);
    
    /**
     * Check if user has risk limits configured
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Find all pattern day traders
     */
    List<SimplifiedRiskLimit> findByPatternDayTrader(Boolean patternDayTrader);
    
    /**
     * Find users with day trading buying power
     */
    @Query("SELECT rl FROM SimplifiedRiskLimit rl WHERE rl.dayTradingBuyingPower > 0")
    List<SimplifiedRiskLimit> findUsersWithDayTradingPower();
    
    // Risk validation queries
    
    /**
     * Get maximum single order value for user
     */
    @Query("SELECT rl.maxSingleOrderValue FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<BigDecimal> getMaxSingleOrderValue(@Param("userId") Long userId);
    
    /**
     * Get maximum position value for user
     */
    @Query("SELECT rl.maxPositionValue FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<BigDecimal> getMaxPositionValue(@Param("userId") Long userId);
    
    /**
     * Get maximum daily trades for user
     */
    @Query("SELECT rl.maxDailyTrades FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<Integer> getMaxDailyTrades(@Param("userId") Long userId);
    
    /**
     * Get maximum open orders for user
     */
    @Query("SELECT rl.maxOpenOrders FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<Integer> getMaxOpenOrders(@Param("userId") Long userId);
    
    /**
     * Check if user is pattern day trader
     */
    @Query("SELECT rl.patternDayTrader FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<Boolean> isPatternDayTrader(@Param("userId") Long userId);
    
    /**
     * Get day trading buying power for user
     */
    @Query("SELECT rl.dayTradingBuyingPower FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<BigDecimal> getDayTradingBuyingPower(@Param("userId") Long userId);
    
    // Risk analysis queries
    
    /**
     * Find users with high position limits
     */
    @Query("SELECT rl FROM SimplifiedRiskLimit rl WHERE rl.maxPositionValue > :threshold")
    List<SimplifiedRiskLimit> findUsersWithHighPositionLimits(@Param("threshold") BigDecimal threshold);
    
    /**
     * Find users with high daily trading limits
     */
    @Query("SELECT rl FROM SimplifiedRiskLimit rl WHERE rl.maxDailyTrades > :threshold")
    List<SimplifiedRiskLimit> findUsersWithHighDailyLimits(@Param("threshold") Integer threshold);
    
    /**
     * Get risk limit statistics
     */
    @Query("SELECT " +
           "AVG(rl.maxPositionValue) as avgPositionLimit, " +
           "AVG(rl.maxSingleOrderValue) as avgOrderLimit, " +
           "AVG(rl.maxDailyTrades) as avgDailyTrades, " +
           "COUNT(CASE WHEN rl.patternDayTrader = true THEN 1 END) as pdtCount, " +
           "COUNT(rl) as totalUsers " +
           "FROM SimplifiedRiskLimit rl")
    Object[] getRiskLimitStatistics();
    
    /**
     * Find users needing risk limit review (old configurations)
     */
    @Query("SELECT rl FROM SimplifiedRiskLimit rl " +
           "WHERE rl.updatedAt < :thresholdDate " +
           "ORDER BY rl.updatedAt ASC")
    List<SimplifiedRiskLimit> findUsersNeedingReview(@Param("thresholdDate") java.time.Instant thresholdDate);
    
    /**
     * Get risk limit distribution by ranges
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN rl.maxPositionValue < 1000000 THEN 'LOW' " +
           "  WHEN rl.maxPositionValue < 10000000 THEN 'MEDIUM' " +
           "  ELSE 'HIGH' " +
           "END as riskCategory, " +
           "COUNT(rl) as userCount " +
           "FROM SimplifiedRiskLimit rl " +
           "GROUP BY " +
           "CASE " +
           "  WHEN rl.maxPositionValue < 1000000 THEN 'LOW' " +
           "  WHEN rl.maxPositionValue < 10000000 THEN 'MEDIUM' " +
           "  ELSE 'HIGH' " +
           "END")
    List<Object[]> getRiskLimitDistribution();
    
    // Compliance queries
    
    /**
     * Validate if order value is within limits
     */
    @Query("SELECT CASE WHEN :orderValue <= rl.maxSingleOrderValue THEN true ELSE false END " +
           "FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<Boolean> validateOrderValue(@Param("userId") Long userId, @Param("orderValue") BigDecimal orderValue);
    
    /**
     * Validate if position value is within limits
     */
    @Query("SELECT CASE WHEN :positionValue <= rl.maxPositionValue THEN true ELSE false END " +
           "FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<Boolean> validatePositionValue(@Param("userId") Long userId, @Param("positionValue") BigDecimal positionValue);
    
    /**
     * Validate if daily trades are within limits
     */
    @Query("SELECT CASE WHEN :currentTrades < rl.maxDailyTrades THEN true ELSE false END " +
           "FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<Boolean> validateDailyTrades(@Param("userId") Long userId, @Param("currentTrades") Integer currentTrades);
    
    /**
     * Validate if open orders are within limits
     */
    @Query("SELECT CASE WHEN :currentOrders < rl.maxOpenOrders THEN true ELSE false END " +
           "FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<Boolean> validateOpenOrders(@Param("userId") Long userId, @Param("currentOrders") Integer currentOrders);
    
    /**
     * Get complete risk profile for user
     */
    @Query("SELECT rl FROM SimplifiedRiskLimit rl WHERE rl.userId = :userId")
    Optional<SimplifiedRiskLimit> getRiskProfile(@Param("userId") Long userId);
}