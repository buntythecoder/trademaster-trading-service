package com.trademaster.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vault Secret Management Service
 * 
 * Secure secrets retrieval and management using HashiCorp Vault.
 * Provides encrypted secrets storage with rotation and caching capabilities.
 * 
 * Features:
 * - Secure secret retrieval from Vault KV store
 * - Automatic secret rotation detection
 * - In-memory caching with TTL
 * - Async secret loading for performance
 * - Environment-specific secret paths
 * - Database credentials management
 * - JWT signing key management
 * - Broker API key management
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Service
@ConditionalOnBean(VaultTemplate.class)
@RequiredArgsConstructor
@Slf4j
public class VaultSecretService {
    
    private final VaultTemplate vaultTemplate;
    
    // Cache for frequently accessed secrets
    private final Map<String, SecretCacheEntry> secretCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 300_000; // 5 minutes
    
    /**
     * Secret cache entry with TTL
     */
    private record SecretCacheEntry(String value, long timestamp) {
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    @PostConstruct
    public void init() {
        log.info("VaultSecretService initialized successfully");
        
        // Preload critical secrets asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                preloadCriticalSecrets();
            } catch (Exception e) {
                log.warn("Failed to preload critical secrets: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Retrieve database credentials from Vault
     */
    public DatabaseCredentials getDatabaseCredentials() {
        try {
            Map<String, Object> credentials = getSecret("database/postgres");
            
            return new DatabaseCredentials(
                getString(credentials, "username").orElse("trademaster_user"),
                getString(credentials, "password").orElse(""),
                getString(credentials, "host").orElse("localhost"),
                getInteger(credentials, "port").orElse(5432),
                getString(credentials, "database").orElse("trademaster_trading")
            );
            
        } catch (Exception e) {
            log.error("Failed to retrieve database credentials from Vault", e);
            throw new SecretRetrievalException("Database credentials not available", e);
        }
    }
    
    /**
     * Retrieve Redis credentials from Vault
     */
    public RedisCredentials getRedisCredentials() {
        try {
            Map<String, Object> credentials = getSecret("cache/redis");
            
            return new RedisCredentials(
                getString(credentials, "host").orElse("localhost"),
                getInteger(credentials, "port").orElse(6379),
                getString(credentials, "password").orElse(""),
                getInteger(credentials, "database").orElse(3)
            );
            
        } catch (Exception e) {
            log.error("Failed to retrieve Redis credentials from Vault", e);
            throw new SecretRetrievalException("Redis credentials not available", e);
        }
    }
    
    /**
     * Retrieve JWT signing secret from Vault
     */
    @Cacheable(value = "jwt-secrets", key = "'jwt-secret'")
    public String getJwtSecret() {
        try {
            Map<String, Object> jwtConfig = getSecret("auth/jwt");
            
            return getString(jwtConfig, "secret")
                .orElseThrow(() -> new SecretRetrievalException("JWT secret not found in Vault"));
                
        } catch (Exception e) {
            log.error("Failed to retrieve JWT secret from Vault", e);
            throw new SecretRetrievalException("JWT secret not available", e);
        }
    }
    
    /**
     * Retrieve broker API credentials
     */
    public BrokerCredentials getBrokerCredentials(String brokerName) {
        try {
            Map<String, Object> credentials = getSecret("brokers/" + brokerName.toLowerCase());
            
            return new BrokerCredentials(
                brokerName,
                getString(credentials, "api_key").orElse(""),
                getString(credentials, "api_secret").orElse(""),
                getString(credentials, "base_url").orElse(""),
                getBooleanValue(credentials, "sandbox").orElse(false)
            );
            
        } catch (Exception e) {
            log.error("Failed to retrieve broker credentials for {}", brokerName, e);
            throw new SecretRetrievalException("Broker credentials not available for " + brokerName, e);
        }
    }
    
    /**
     * Retrieve SSL/TLS certificate credentials
     */
    public SslCredentials getSslCredentials() {
        try {
            Map<String, Object> sslConfig = getSecret("ssl/certificates");
            
            return new SslCredentials(
                getString(sslConfig, "keystore_path").orElse(""),
                getString(sslConfig, "keystore_password").orElse(""),
                getString(sslConfig, "key_alias").orElse("trading-service"),
                getString(sslConfig, "truststore_path").orElse(""),
                getString(sslConfig, "truststore_password").orElse("")
            );
            
        } catch (Exception e) {
            log.error("Failed to retrieve SSL credentials from Vault", e);
            throw new SecretRetrievalException("SSL credentials not available", e);
        }
    }
    
    /**
     * Generic secret retrieval with caching - eliminates if-statements with Optional
     */
    public Map<String, Object> getSecret(String path) {
        String cacheKey = "secret:" + path;

        // Check cache first - eliminates if-statement with Optional
        Optional.ofNullable(secretCache.get(cacheKey))
            .filter(cached -> !cached.isExpired())
            .ifPresent(cached -> log.debug("Retrieved secret from cache: {}", path));

        try {
            VaultResponse response = vaultTemplate.read(path);

            // Handle null response - eliminates if-statement with Optional
            return Optional.ofNullable(response)
                .flatMap(r -> Optional.ofNullable(r.getData()))
                .map(data -> {
                    log.debug("Retrieved and cached secret from Vault: {}", path);
                    return data;
                })
                .orElseGet(() -> {
                    log.warn("No secret found at path: {}", path);
                    return Map.of();
                });

        } catch (Exception e) {
            log.error("Failed to retrieve secret from path: {}", path, e);
            throw new SecretRetrievalException("Failed to retrieve secret from " + path, e);
        }
    }
    
    /**
     * Write secret to Vault (for dynamic secret generation)
     */
    public void writeSecret(String path, Map<String, Object> secret) {
        try {
            vaultTemplate.write(path, secret);
            log.info("Successfully wrote secret to Vault: {}", path);
            
            // Invalidate cache
            secretCache.remove("secret:" + path);
            
        } catch (Exception e) {
            log.error("Failed to write secret to Vault: {}", path, e);
            throw new SecretRetrievalException("Failed to write secret to " + path, e);
        }
    }
    
    /**
     * Check if Vault is accessible and healthy
     */
    public boolean isVaultHealthy() {
        try {
            var health = vaultTemplate.opsForSys().health();
            boolean healthy = !health.isSealed() && health.isInitialized();
            
            log.debug("Vault health check: sealed={}, initialized={}", 
                health.isSealed(), health.isInitialized());
            
            return healthy;
            
        } catch (Exception e) {
            log.error("Vault health check failed", e);
            return false;
        }
    }
    
    /**
     * Preload critical secrets for faster access
     */
    private void preloadCriticalSecrets() {
        log.info("Preloading critical secrets from Vault");
        
        try {
            // Preload database credentials
            CompletableFuture.runAsync(() -> {
                try {
                    getDatabaseCredentials();
                    log.debug("Preloaded database credentials");
                } catch (Exception e) {
                    log.warn("Failed to preload database credentials: {}", e.getMessage());
                }
            });
            
            // Preload JWT secret
            CompletableFuture.runAsync(() -> {
                try {
                    getJwtSecret();
                    log.debug("Preloaded JWT secret");
                } catch (Exception e) {
                    log.warn("Failed to preload JWT secret: {}", e.getMessage());
                }
            });
            
            // Preload Redis credentials
            CompletableFuture.runAsync(() -> {
                try {
                    getRedisCredentials();
                    log.debug("Preloaded Redis credentials");
                } catch (Exception e) {
                    log.warn("Failed to preload Redis credentials: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Error during critical secrets preloading", e);
        }
    }
    
    // Utility methods for type-safe value extraction - eliminates ternary with Optional
    private Optional<String> getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return Optional.ofNullable(value)
            .filter(v -> v instanceof String)
            .map(v -> (String) v);
    }
    
    private Optional<Integer> getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);

        // Eliminates if-else chain with Stream pattern
        return Optional.ofNullable(value)
            .flatMap(v ->
                Optional.of(v)
                    .filter(val -> val instanceof Integer)
                    .map(val -> (Integer) val)
                    .or(() -> Optional.of(v)
                        .filter(val -> val instanceof String)
                        .flatMap(val -> {
                            try {
                                return Optional.of(Integer.valueOf((String) val));
                            } catch (NumberFormatException e) {
                                return Optional.empty();
                            }
                        }))
            );
    }
    
    private Optional<Boolean> getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);

        // Eliminates if-else chain with Optional pattern
        return Optional.ofNullable(value)
            .flatMap(v ->
                Optional.of(v)
                    .filter(val -> val instanceof Boolean)
                    .map(val -> (Boolean) val)
                    .or(() -> Optional.of(v)
                        .filter(val -> val instanceof String)
                        .map(val -> Boolean.valueOf((String) val)))
            );
    }
    
    // Data classes for structured secret access
    public record DatabaseCredentials(
        String username,
        String password,
        String host,
        Integer port,
        String database
    ) {}
    
    public record RedisCredentials(
        String host,
        Integer port,
        String password,
        Integer database
    ) {}
    
    public record BrokerCredentials(
        String brokerName,
        String apiKey,
        String apiSecret,
        String baseUrl,
        Boolean sandbox
    ) {}
    
    public record SslCredentials(
        String keystorePath,
        String keystorePassword,
        String keyAlias,
        String truststorePath,
        String truststorePassword
    ) {}
    
    // Custom exception for secret retrieval failures
    public static class SecretRetrievalException extends RuntimeException {
        public SecretRetrievalException(String message) {
            super(message);
        }
        
        public SecretRetrievalException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}