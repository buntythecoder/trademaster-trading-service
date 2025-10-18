package com.trademaster.trading.health;

import com.trademaster.trading.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Trading Readiness Health Indicator
 * 
 * Kubernetes readiness probe implementation for trading service.
 * Checks external dependencies that affect the service's ability to handle traffic.
 * 
 * Readiness Checks:
 * - Database connectivity and query performance
 * - Redis cache connectivity
 * - Repository layer functionality
 * - External service dependencies
 * 
 * This indicator determines if the service should receive traffic from load balancers.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component("tradingReadinessIndicator")
@Slf4j
public class TradingReadinessIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    private final OrderRepository orderRepository;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String READINESS_CHECK_KEY = "trading-service:readiness:check";
    private static final int READINESS_TIMEOUT_MS = 3000;
    
    public TradingReadinessIndicator(DataSource dataSource, OrderRepository orderRepository) {
        this.dataSource = dataSource;
        this.orderRepository = orderRepository;
    }
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        long startTime = System.currentTimeMillis();
        
        try {
            // Check external dependencies that affect readiness
            CompletableFuture<Boolean> databaseReady = checkDatabaseReadiness();
            CompletableFuture<Boolean> redisReady = checkRedisReadiness();
            CompletableFuture<Boolean> repositoryReady = checkRepositoryReadiness();
            
            // Wait for all checks with timeout
            CompletableFuture<Void> allChecks = CompletableFuture.allOf(
                databaseReady, redisReady, repositoryReady
            );
            
            allChecks.get(READINESS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
            boolean dbHealthy = databaseReady.join();
            boolean cacheHealthy = redisReady.join();
            boolean repoHealthy = repositoryReady.join();
            
            long responseTime = System.currentTimeMillis() - startTime;

            // Eliminates if-else using Optional.of().filter().ifPresentOrElse()
            // Service is ready if all critical dependencies are available
            Optional.of(dbHealthy && cacheHealthy && repoHealthy)
                .filter(allReady -> allReady)
                .ifPresentOrElse(
                    ready -> builder.up(),
                    () -> builder.down()
                );

            // Eliminates ternary operators using Optional.of().filter().map().orElse()
            builder.withDetail("database",
                    Optional.of(dbHealthy)
                        .filter(healthy -> healthy)
                        .map(h -> "READY")
                        .orElse("NOT_READY"))
                   .withDetail("redis",
                    Optional.of(cacheHealthy)
                        .filter(healthy -> healthy)
                        .map(h -> "READY")
                        .orElse("NOT_READY"))
                   .withDetail("repository",
                    Optional.of(repoHealthy)
                        .filter(healthy -> healthy)
                        .map(h -> "READY")
                        .orElse("NOT_READY"))
                   .withDetail("responseTimeMs", responseTime)
                   .withDetail("checkType", "READINESS")
                   .withDetail("timestamp", Instant.now().toString());
                   
        } catch (Exception e) {
            log.error("Readiness check failed", e);
            builder.down()
                   .withDetail("error", e.getMessage())
                   .withDetail("checkType", "READINESS")
                   .withDetail("timestamp", Instant.now().toString());
        }
        
        return builder.build();
    }
    
    private CompletableFuture<Boolean> checkDatabaseReadiness() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                // Verify database is ready with a quick validation query
                return connection.isValid(2);
            } catch (Exception e) {
                log.debug("Database readiness check failed: {}", e.getMessage());
                return false;
            }
        });
    }
    
    private CompletableFuture<Boolean> checkRedisReadiness() {
        return CompletableFuture.supplyAsync(() ->
            // Eliminates if-statement using Optional.ofNullable().map().orElseGet()
            Optional.ofNullable(redisTemplate)
                .map(redis -> {
                    try {
                        String testValue = "readiness-" + System.currentTimeMillis();
                        redis.opsForValue().set(READINESS_CHECK_KEY, testValue, 30, TimeUnit.SECONDS);
                        String retrieved = (String) redis.opsForValue().get(READINESS_CHECK_KEY);
                        redis.delete(READINESS_CHECK_KEY);
                        return testValue.equals(retrieved);
                    } catch (Exception e) {
                        log.debug("Redis readiness check failed: {}", e.getMessage());
                        return false;
                    }
                })
                .orElseGet(() -> {
                    log.debug("Redis not configured, skipping Redis readiness check");
                    return true; // Consider Redis check as passed if not configured
                })
        );
    }
    
    private CompletableFuture<Boolean> checkRepositoryReadiness() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verify repository layer is functional
                orderRepository.count();
                return true;
            } catch (Exception e) {
                log.debug("Repository readiness check failed: {}", e.getMessage());
                return false;
            }
        });
    }
}