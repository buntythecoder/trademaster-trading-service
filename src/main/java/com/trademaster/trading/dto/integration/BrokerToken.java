package com.trademaster.trading.dto.integration;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Broker Token DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Data transfer object representing a validated broker token.
 * Used for token validation responses from broker-auth-service.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record BrokerToken(

    /**
     * User ID
     */
    Long userId,

    /**
     * Broker name
     */
    String brokerName,

    /**
     * Access token
     */
    String accessToken,

    /**
     * Token validation status
     */
    TokenStatus status,

    /**
     * Token expiry timestamp
     */
    LocalDateTime expiresAt,

    /**
     * Whether token is valid
     */
    boolean valid,

    /**
     * Token validation timestamp
     */
    LocalDateTime validatedAt,

    /**
     * Validation message/reason
     */
    String message
) {

    /**
     * Token status enumeration
     */
    public enum TokenStatus {
        VALID,
        EXPIRED,
        INVALID,
        REVOKED,
        NOT_FOUND
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return status == TokenStatus.EXPIRED ||
               (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
    }

    /**
     * Check if token needs refresh
     */
    public boolean needsRefresh() {
        return isExpired() || !valid;
    }

    /**
     * Get time until expiry (in seconds) - eliminates ternary with Optional
     */
    public long secondsUntilExpiry() {
        return Optional.ofNullable(expiresAt)
            .map(expiry -> java.time.Duration.between(LocalDateTime.now(), expiry).getSeconds())
            .orElse(0L);
    }

    /**
     * Check if token is usable
     */
    public boolean isUsable() {
        return valid && status == TokenStatus.VALID && !isExpired();
    }
}
