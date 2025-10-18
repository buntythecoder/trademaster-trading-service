package com.trademaster.trading.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;

/**
 * Trading Audit Log Entity
 * 
 * Complete audit trail for all trading operations to ensure regulatory compliance.
 * Maps exactly to the 'trading_audit_log' table in V1__Create_trading_schema.sql
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Schema-Aligned)
 */
@Entity
@Table(name = "trading_audit_log", indexes = {
    @Index(name = "idx_audit_log_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_log_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_log_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User who performed the action
     */
    @Column(name = "user_id", nullable = false)
    @NotNull
    private Long userId;
    
    /**
     * Associated order ID (if applicable)
     */
    @Column(name = "order_id")
    private Long orderId;
    
    /**
     * Associated trade ID (if applicable)
     */
    @Column(name = "trade_id")
    private Long tradeId;
    
    /**
     * Action performed (INSERT, UPDATE, DELETE)
     */
    @Column(name = "action", nullable = false, length = 50)
    @NotNull
    private String action;
    
    /**
     * Entity type (ORDER, TRADE, PORTFOLIO, RISK)
     */
    @Column(name = "entity_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @NotNull
    private EntityType entityType;
    
    /**
     * ID of the affected entity
     */
    @Column(name = "entity_id", nullable = false)
    @NotNull
    private Long entityId;
    
    /**
     * Previous values (JSON format)
     */
    @Column(name = "old_values", columnDefinition = "JSONB")
    private String oldValues;
    
    /**
     * New values (JSON format)
     */
    @Column(name = "new_values", columnDefinition = "JSONB")
    private String newValues;
    
    /**
     * Client IP address
     */
    @Column(name = "ip_address", columnDefinition = "INET")
    private InetAddress ipAddress;
    
    /**
     * Client user agent
     */
    @Column(name = "user_agent")
    private String userAgent;
    
    /**
     * Session ID for correlation
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    /**
     * Audit log creation timestamp
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * Entity types for audit logging
     */
    public enum EntityType {
        ORDER,
        TRADE,
        PORTFOLIO,
        RISK
    }
    
    // Helper methods
    
    /**
     * Check if this is an insert operation
     */
    public boolean isInsert() {
        return "INSERT".equalsIgnoreCase(action);
    }
    
    /**
     * Check if this is an update operation
     */
    public boolean isUpdate() {
        return "UPDATE".equalsIgnoreCase(action);
    }
    
    /**
     * Check if this is a delete operation
     */
    public boolean isDelete() {
        return "DELETE".equalsIgnoreCase(action);
    }
    
    /**
     * Get user agent summary (first 100 characters)
     */
    public String getUserAgentSummary() {
        // Eliminates if-statement and ternary using Optional.ofNullable().map() chain
        return Optional.ofNullable(userAgent)
            .map(ua -> Optional.of(ua.length() > 100)
                .filter(tooLong -> tooLong)
                .map(tooLong -> ua.substring(0, 100) + "...")
                .orElse(ua))
            .orElse(null);
    }
    
    /**
     * Check if audit entry has change data
     */
    public boolean hasChangeData() {
        return (oldValues != null && !oldValues.trim().isEmpty()) ||
               (newValues != null && !newValues.trim().isEmpty());
    }
}