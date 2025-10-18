package com.trademaster.trading.dto.integration;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Broker Connection DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Data transfer object representing a broker connection from broker-auth-service.
 * Contains broker credentials, connection status, and authentication tokens.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record BrokerConnection(

    /**
     * User ID
     */
    Long userId,

    /**
     * Broker name (ZERODHA, UPSTOX, etc.)
     */
    String brokerName,

    /**
     * Broker client ID/API key
     */
    String clientId,

    /**
     * Connection status
     */
    ConnectionStatus status,

    /**
     * Access token for API calls
     */
    String accessToken,

    /**
     * Refresh token for token renewal
     */
    String refreshToken,

    /**
     * Token expiry timestamp
     */
    LocalDateTime tokenExpiry,

    /**
     * Whether connection is active
     */
    boolean active,

    /**
     * Last successful connection timestamp
     */
    LocalDateTime lastConnectedAt,

    /**
     * Connection metadata (broker-specific settings)
     */
    Map<String, String> metadata
) {

    /**
     * Connection status enumeration
     */
    public enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        TOKEN_EXPIRED,
        AUTHENTICATION_FAILED,
        BROKER_UNAVAILABLE
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired() {
        return tokenExpiry != null && LocalDateTime.now().isAfter(tokenExpiry);
    }

    /**
     * Check if connection needs refresh
     */
    public boolean needsRefresh() {
        return isTokenExpired() || status == ConnectionStatus.TOKEN_EXPIRED;
    }

    /**
     * Check if connection is usable
     */
    public boolean isUsable() {
        return active && status == ConnectionStatus.CONNECTED && !isTokenExpired();
    }

    /**
     * Get time until token expires (in seconds) - eliminates ternary with Optional
     */
    public long secondsUntilExpiry() {
        return Optional.ofNullable(tokenExpiry)
            .map(expiry -> java.time.Duration.between(LocalDateTime.now(), expiry).getSeconds())
            .orElse(0L);
    }
}
