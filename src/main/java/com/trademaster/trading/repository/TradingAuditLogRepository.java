package com.trademaster.trading.repository;

import com.trademaster.trading.entity.TradingAuditLog;
import com.trademaster.trading.entity.TradingAuditLog.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Trading Audit Log Repository Interface
 * 
 * Data access layer for TradingAuditLog entities to provide
 * complete audit trail for regulatory compliance.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Repository
public interface TradingAuditLogRepository extends JpaRepository<TradingAuditLog, Long> {
    
    // Basic finders
    
    /**
     * Find audit logs for specific user
     */
    Page<TradingAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find audit logs for specific entity
     */
    List<TradingAuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        EntityType entityType, Long entityId);
    
    /**
     * Find audit logs for specific order
     */
    List<TradingAuditLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    /**
     * Find audit logs for specific trade
     */
    List<TradingAuditLog> findByTradeIdOrderByCreatedAtDesc(Long tradeId);
    
    /**
     * Find audit logs by action type
     */
    List<TradingAuditLog> findByUserIdAndActionOrderByCreatedAtDesc(Long userId, String action);
    
    /**
     * Find audit logs by entity type
     */
    Page<TradingAuditLog> findByUserIdAndEntityTypeOrderByCreatedAtDesc(
        Long userId, EntityType entityType, Pageable pageable);
    
    /**
     * Find audit logs in time range
     */
    List<TradingAuditLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long userId, Instant startTime, Instant endTime);
    
    /**
     * Find audit logs by session ID
     */
    List<TradingAuditLog> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    
    /**
     * Find audit logs by IP address
     */
    List<TradingAuditLog> findByIpAddressOrderByCreatedAtDesc(InetAddress ipAddress);
    
    // Compliance and monitoring queries
    
    /**
     * Get user activity count in time range
     */
    @Query("SELECT COUNT(al) FROM TradingAuditLog al " +
           "WHERE al.userId = :userId " +
           "AND al.createdAt BETWEEN :startTime AND :endTime")
    Long getUserActivityCount(@Param("userId") Long userId,
                             @Param("startTime") Instant startTime,
                             @Param("endTime") Instant endTime);
    
    /**
     * Get action count by type for user in time range
     */
    @Query("SELECT al.action, COUNT(al) FROM TradingAuditLog al " +
           "WHERE al.userId = :userId " +
           "AND al.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY al.action " +
           "ORDER BY COUNT(al) DESC")
    List<Object[]> getActionCountByType(@Param("userId") Long userId,
                                       @Param("startTime") Instant startTime,
                                       @Param("endTime") Instant endTime);
    
    /**
     * Get entity modification count by type
     */
    @Query("SELECT al.entityType, COUNT(al) FROM TradingAuditLog al " +
           "WHERE al.userId = :userId " +
           "AND al.createdAt BETWEEN :startTime AND :endTime " +
           "AND al.action IN ('INSERT', 'UPDATE', 'DELETE') " +
           "GROUP BY al.entityType")
    List<Object[]> getEntityModificationCount(@Param("userId") Long userId,
                                             @Param("startTime") Instant startTime,
                                             @Param("endTime") Instant endTime);
    
    /**
     * Find suspicious activities (multiple IPs for same user)
     */
    @Query("SELECT al.userId, COUNT(DISTINCT al.ipAddress) as ipCount " +
           "FROM TradingAuditLog al " +
           "WHERE al.createdAt BETWEEN :startTime AND :endTime " +
           "AND al.ipAddress IS NOT NULL " +
           "GROUP BY al.userId " +
           "HAVING COUNT(DISTINCT al.ipAddress) > :threshold")
    List<Object[]> findUsersWithMultipleIPs(@Param("startTime") Instant startTime,
                                            @Param("endTime") Instant endTime,
                                            @Param("threshold") Long threshold);
    
    /**
     * Get session activity summary
     */
    @Query("SELECT al.sessionId, al.userId, COUNT(al), MIN(al.createdAt), MAX(al.createdAt) " +
           "FROM TradingAuditLog al " +
           "WHERE al.sessionId IS NOT NULL " +
           "AND al.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY al.sessionId, al.userId " +
           "ORDER BY COUNT(al) DESC")
    List<Object[]> getSessionActivitySummary(@Param("startTime") Instant startTime,
                                            @Param("endTime") Instant endTime);
    
    /**
     * Find high-frequency trading patterns
     */
    @Query("SELECT al.userId, COUNT(al) as actionCount " +
           "FROM TradingAuditLog al " +
           "WHERE al.entityType = 'ORDER' " +
           "AND al.action IN ('INSERT', 'UPDATE') " +
           "AND al.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY al.userId " +
           "HAVING COUNT(al) > :threshold " +
           "ORDER BY COUNT(al) DESC")
    List<Object[]> findHighFrequencyTradingUsers(@Param("startTime") Instant startTime,
                                                 @Param("endTime") Instant endTime,
                                                 @Param("threshold") Long threshold);
    
    /**
     * Get compliance report for user
     */
    @Query("SELECT " +
           "al.entityType, " +
           "al.action, " +
           "COUNT(al) as count, " +
           "MIN(al.createdAt) as firstActivity, " +
           "MAX(al.createdAt) as lastActivity " +
           "FROM TradingAuditLog al " +
           "WHERE al.userId = :userId " +
           "AND al.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY al.entityType, al.action " +
           "ORDER BY COUNT(al) DESC")
    List<Object[]> getComplianceReport(@Param("userId") Long userId,
                                      @Param("startTime") Instant startTime,
                                      @Param("endTime") Instant endTime);
    
    /**
     * Check for rapid consecutive actions (potential bot activity)
     */
    @Query("SELECT COUNT(al) FROM TradingAuditLog al " +
           "WHERE al.userId = :userId " +
           "AND al.entityType = 'ORDER' " +
           "AND al.createdAt BETWEEN :startTime AND :endTime")
    Long checkRapidActions(@Param("userId") Long userId,
                          @Param("startTime") Instant startTime,
                          @Param("endTime") Instant endTime);
    
    /**
     * Get audit trail for specific entity
     */
    @Query("SELECT al FROM TradingAuditLog al " +
           "WHERE al.entityType = :entityType " +
           "AND al.entityId = :entityId " +
           "ORDER BY al.createdAt ASC")
    List<TradingAuditLog> getAuditTrail(@Param("entityType") EntityType entityType,
                                       @Param("entityId") Long entityId);
    
    /**
     * Find most recent audit entry for entity
     */
    Optional<TradingAuditLog> findFirstByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        EntityType entityType, Long entityId);
    
    /**
     * Check if user performed action recently
     */
    boolean existsByUserIdAndActionAndCreatedAtAfter(Long userId, String action, Instant since);
}