package com.trademaster.trading.dto.integration;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Token Refresh Result DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Data transfer object representing the result of a token refresh operation.
 * Returned by broker-auth-service after refreshing broker tokens.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record TokenRefreshResult(

    /**
     * User ID
     */
    Long userId,

    /**
     * Broker name
     */
    String brokerName,

    /**
     * New access token
     */
    String accessToken,

    /**
     * New refresh token (if rotated)
     */
    String refreshToken,

    /**
     * Token expiry timestamp
     */
    LocalDateTime expiresAt,

    /**
     * Refresh status
     */
    RefreshStatus status,

    /**
     * Whether refresh was successful
     */
    boolean success,

    /**
     * Refresh timestamp
     */
    LocalDateTime refreshedAt,

    /**
     * Status message
     */
    String message
) {

    /**
     * Refresh status enumeration
     */
    public enum RefreshStatus {
        SUCCESS,
        REFRESH_TOKEN_EXPIRED,
        REFRESH_TOKEN_INVALID,
        BROKER_ERROR,
        AUTHENTICATION_FAILED
    }

    /**
     * Check if refresh was successful
     */
    public boolean isSuccessful() {
        return success && status == RefreshStatus.SUCCESS;
    }

    /**
     * Check if refresh token needs re-authentication
     */
    public boolean needsReAuthentication() {
        return status == RefreshStatus.REFRESH_TOKEN_EXPIRED ||
               status == RefreshStatus.REFRESH_TOKEN_INVALID ||
               status == RefreshStatus.AUTHENTICATION_FAILED;
    }

    /**
     * Get time until new token expires (in seconds) - eliminates ternary with Optional
     */
    public long secondsUntilExpiry() {
        return Optional.ofNullable(expiresAt)
            .map(expiry -> java.time.Duration.between(LocalDateTime.now(), expiry).getSeconds())
            .orElse(0L);
    }

    /**
     * Check if new token is usable
     */
    public boolean hasUsableToken() {
        return success && accessToken != null && !accessToken.isBlank();
    }
}
