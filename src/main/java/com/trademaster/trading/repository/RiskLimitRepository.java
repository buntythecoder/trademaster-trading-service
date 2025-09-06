package com.trademaster.trading.repository;

import com.trademaster.trading.entity.RiskLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Risk Limit Repository
 * 
 * Repository interface for risk limit database operations with optimized queries.
 * Includes custom queries for risk monitoring and limit validation.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface RiskLimitRepository extends JpaRepository<RiskLimit, Long> {
    
    /**
     * Find active risk limits for a user
     */
    @Query("SELECT rl FROM RiskLimit rl WHERE rl.userId = :userId AND rl.active = true")
    Optional<RiskLimit> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find all active risk limits for multiple users
     */
    @Query("SELECT rl FROM RiskLimit rl WHERE rl.userId IN :userIds AND rl.active = true")
    List<RiskLimit> findByUserIds(@Param("userIds") List<Long> userIds);
    
    /**
     * Find risk limits by profile type
     */
    @Query("SELECT rl FROM RiskLimit rl WHERE rl.profileType = :profileType AND rl.active = true")
    List<RiskLimit> findByProfileType(@Param("profileType") String profileType);
    
    /**
     * Find users with aggressive risk profiles
     */
    @Query("SELECT rl.userId FROM RiskLimit rl WHERE rl.profileType = 'AGGRESSIVE' AND rl.active = true")
    List<Long> findAggressiveRiskUsers();
    
    /**
     * Find users with margin trading enabled
     */
    @Query("SELECT rl.userId FROM RiskLimit rl WHERE rl.allowMarginTrading = true AND rl.active = true")
    List<Long> findMarginTradingUsers();
    
    /**
     * Find risk limits with high leverage ratios
     */
    @Query("SELECT rl FROM RiskLimit rl WHERE rl.maxLeverageRatio > :threshold AND rl.active = true")
    List<RiskLimit> findHighLeverageRiskLimits(@Param("threshold") java.math.BigDecimal threshold);
    
    /**
     * Find risk limits updated recently
     */
    @Query("SELECT rl FROM RiskLimit rl WHERE rl.updatedAt > :since AND rl.active = true")
    List<RiskLimit> findRecentlyUpdated(@Param("since") Instant since);
    
    /**
     * Find users with real-time alerts enabled
     */
    @Query("SELECT rl.userId FROM RiskLimit rl WHERE rl.enableRealTimeAlerts = true AND rl.active = true")
    List<Long> findRealTimeAlertUsers();
    
    /**
     * Find users with dynamic adjustments enabled
     */
    @Query("SELECT rl FROM RiskLimit rl WHERE rl.enableVolatilityAdjustment = true OR rl.enableMarketRegimeAdjustment = true")
    List<RiskLimit> findDynamicAdjustmentUsers();
    
    /**
     * Count active risk limits by profile type
     */
    @Query("SELECT rl.profileType, COUNT(rl) FROM RiskLimit rl WHERE rl.active = true GROUP BY rl.profileType")
    List<Object[]> countByProfileType();
    
    /**
     * Find users with custom risk profiles (non-standard configurations)
     */
    @Query("SELECT rl FROM RiskLimit rl WHERE rl.profileType = 'CUSTOM' AND rl.active = true")
    List<RiskLimit> findCustomRiskProfiles();
    
    /**
     * Find risk limits that need review (old versions)
     */
    @Query("SELECT rl FROM RiskLimit rl WHERE rl.updatedAt < :threshold AND rl.active = true")
    List<RiskLimit> findStaleRiskLimits(@Param("threshold") Instant threshold);
    
    /**
     * Check if user has any active risk limits
     */
    @Query("SELECT COUNT(rl) > 0 FROM RiskLimit rl WHERE rl.userId = :userId AND rl.active = true")
    boolean hasActiveRiskLimits(@Param("userId") Long userId);
    
    /**
     * Get maximum leverage ratio for a user
     */
    @Query("SELECT rl.maxLeverageRatio FROM RiskLimit rl WHERE rl.userId = :userId AND rl.active = true")
    Optional<java.math.BigDecimal> getMaxLeverageRatio(@Param("userId") Long userId);
    
    /**
     * Get maximum VaR limit for a user
     */
    @Query("SELECT rl.maxVaR FROM RiskLimit rl WHERE rl.userId = :userId AND rl.active = true")
    Optional<java.math.BigDecimal> getMaxVaRLimit(@Param("userId") Long userId);
    
    /**
     * Find users exceeding position concentration limits
     */
    @Query("SELECT rl.userId FROM RiskLimit rl WHERE rl.maxSectorConcentration < :currentConcentration AND rl.active = true")
    List<Long> findUsersExceedingConcentration(@Param("currentConcentration") java.math.BigDecimal currentConcentration);
    
    /**
     * Get warning threshold for a user
     */
    @Query("SELECT rl.warningThresholdPercent FROM RiskLimit rl WHERE rl.userId = :userId AND rl.active = true")
    Optional<java.math.BigDecimal> getWarningThreshold(@Param("userId") Long userId);
    
    /**
     * Get critical threshold for a user
     */
    @Query("SELECT rl.criticalThresholdPercent FROM RiskLimit rl WHERE rl.userId = :userId AND rl.active = true")
    Optional<java.math.BigDecimal> getCriticalThreshold(@Param("userId") Long userId);
    
    /**
     * Find all users with limits that need volatility adjustment
     */
    @Query("SELECT rl.userId, rl.volatilityMultiplier FROM RiskLimit rl WHERE rl.enableVolatilityAdjustment = true AND rl.active = true")
    List<Object[]> findVolatilityAdjustmentUsers();
    
    /**
     * Deactivate risk limits for a user
     */
    @Query("UPDATE RiskLimit rl SET rl.active = false, rl.updatedAt = CURRENT_TIMESTAMP WHERE rl.userId = :userId")
    void deactivateRiskLimits(@Param("userId") Long userId);
    
    /**
     * Update volatility multiplier for dynamic adjustment
     */
    @Query("UPDATE RiskLimit rl SET rl.volatilityMultiplier = :multiplier, rl.updatedAt = CURRENT_TIMESTAMP WHERE rl.userId = :userId AND rl.active = true")
    void updateVolatilityMultiplier(@Param("userId") Long userId, @Param("multiplier") java.math.BigDecimal multiplier);
    
    /**
     * Find users with position limits below threshold
     */
    @Query("SELECT rl FROM RiskLimit rl WHERE rl.maxSinglePositionValue < :threshold AND rl.active = true")
    List<RiskLimit> findLowPositionLimitUsers(@Param("threshold") java.math.BigDecimal threshold);
    
    /**
     * Get risk limit statistics
     */
    @Query("SELECT " +
           "COUNT(rl) as total, " +
           "AVG(rl.maxLeverageRatio) as avgLeverage, " +
           "AVG(rl.maxSectorConcentration) as avgConcentration " +
           "FROM RiskLimit rl WHERE rl.active = true")
    Object[] getRiskLimitStatistics();
    
    /**
     * Find users with specific order velocity limits
     */
    @Query("SELECT rl.userId FROM RiskLimit rl WHERE rl.maxOrdersPerMinute >= :minLimit AND rl.active = true")
    List<Long> findHighFrequencyTradingUsers(@Param("minLimit") Integer minLimit);
    
    /**
     * Check if user allows margin trading
     */
    @Query("SELECT rl.allowMarginTrading FROM RiskLimit rl WHERE rl.userId = :userId AND rl.active = true")
    Optional<Boolean> isMarginTradingAllowed(@Param("userId") Long userId);
}