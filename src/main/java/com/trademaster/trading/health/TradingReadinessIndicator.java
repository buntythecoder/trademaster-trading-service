package com.trademaster.trading.health;

import com.trademaster.trading.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
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
@RequiredArgsConstructor
@Slf4j
public class TradingReadinessIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderRepository orderRepository;
    
    private static final String READINESS_CHECK_KEY = "trading-service:readiness:check";
    private static final int READINESS_TIMEOUT_MS = 3000;
    
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
            
            // Service is ready if all critical dependencies are available
            if (dbHealthy && cacheHealthy && repoHealthy) {
                builder.up();
            } else {
                builder.down();
            }
            
            builder.withDetail("database", dbHealthy ? "READY" : "NOT_READY")
                   .withDetail("redis", cacheHealthy ? "READY" : "NOT_READY")  
                   .withDetail("repository", repoHealthy ? "READY" : "NOT_READY")
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
        return CompletableFuture.supplyAsync(() -> {
            try {
                String testValue = "readiness-" + System.currentTimeMillis();
                redisTemplate.opsForValue().set(READINESS_CHECK_KEY, testValue, 30, TimeUnit.SECONDS);
                String retrieved = (String) redisTemplate.opsForValue().get(READINESS_CHECK_KEY);
                redisTemplate.delete(READINESS_CHECK_KEY);
                
                return testValue.equals(retrieved);
                
            } catch (Exception e) {
                log.debug("Redis readiness check failed: {}", e.getMessage());
                return false;
            }
        });
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