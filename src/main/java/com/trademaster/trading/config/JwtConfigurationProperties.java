package com.trademaster.trading.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT Configuration Properties
 * 
 * Type-safe configuration properties for JWT authentication.
 * Eliminates @Value usage and provides validation and IDE support.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtConfigurationProperties(
    /**
     * JWT signing secret
     */
    @NotBlank(message = "JWT secret is required")
    String secret,
    
    /**
     * JWT expiration time in minutes
     */
    @Positive(message = "JWT expiration must be positive")
    long expirationMinutes,
    
    /**
     * JWT issuer
     */
    @NotBlank(message = "JWT issuer is required")
    String issuer,
    
    /**
     * JWT audience
     */
    String audience,
    
    /**
     * JWT refresh token expiration time in days
     */
    @Positive(message = "Refresh token expiration must be positive")
    long refreshExpirationDays
) {
    
    /**
     * Constructor with default values
     */
    public JwtConfigurationProperties {
        if (expirationMinutes == 0) {
            expirationMinutes = 15; // Default 15 minutes
        }
        if (refreshExpirationDays == 0) {
            refreshExpirationDays = 7; // Default 7 days
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "trademaster";
        }
    }
    
    /**
     * Get expiration time in milliseconds
     */
    public long getExpirationMillis() {
        return expirationMinutes * 60 * 1000;
    }
    
    /**
     * Get refresh expiration time in milliseconds
     */
    public long getRefreshExpirationMillis() {
        return refreshExpirationDays * 24 * 60 * 60 * 1000;
    }
}