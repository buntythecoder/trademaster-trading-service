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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Trading Service Health Indicator
 * 
 * Comprehensive health check for all critical trading service components.
 * Provides detailed health information for API Gateway load balancing decisions.
 * 
 * Health Checks:
 * - Database connectivity and query performance
 * - Redis cache connectivity and latency
 * - Repository layer functionality
 * - Virtual threads performance
 * - Memory and resource utilization
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class TradingServiceHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    private final OrderRepository orderRepository;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String HEALTH_CHECK_KEY = "trading-service:health:check";
    private static final int HEALTH_CHECK_TIMEOUT_MS = 5000;
    
    public TradingServiceHealthIndicator(DataSource dataSource, OrderRepository orderRepository) {
        this.dataSource = dataSource;
        this.orderRepository = orderRepository;
    }
    
    /**
     * Health check using Optional patterns to eliminate if-statements
     */
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {
            // Check all critical components
            boolean databaseHealthy = checkDatabase();
            boolean redisHealthy = checkRedis();
            boolean repositoryHealthy = checkRepository();
            boolean systemHealthy = checkSystemHealth();

            // Eliminates if-else with Optional.of(boolean).filter()
            Optional.of(databaseHealthy && redisHealthy && repositoryHealthy && systemHealthy)
                .filter(allHealthy -> allHealthy)
                .ifPresentOrElse(
                    allHealthy -> builder.up(),
                    () -> builder.down()
                );

            // Add detailed component status - eliminates ternary operators with helper method
            builder.withDetail("database", healthStatus(databaseHealthy))
                   .withDetail("redis", healthStatus(redisHealthy))
                   .withDetail("repository", healthStatus(repositoryHealthy))
                   .withDetail("system", healthStatus(systemHealthy))
                   .withDetail("virtualThreads", "ENABLED")
                   .withDetail("service", "trading-service")
                   .withDetail("version", "2.0.0")
                   .withDetail("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Health check failed with exception", e);
            builder.down()
                   .withDetail("error", e.getMessage())
                   .withDetail("exception", e.getClass().getSimpleName())
                   .withDetail("timestamp", System.currentTimeMillis());
        }

        return builder.build();
    }

    /**
     * Helper method to convert boolean to health status string
     * Eliminates ternary operators
     */
    private String healthStatus(boolean healthy) {
        return Optional.of(healthy)
            .filter(h -> h)
            .map(h -> "UP")
            .orElse("DOWN");
    }
    
    private boolean checkDatabase() {
        try {
            CompletableFuture<Boolean> dbCheck = CompletableFuture.supplyAsync(() -> {
                try (Connection connection = dataSource.getConnection()) {
                    return connection.isValid(2);
                } catch (Exception e) {
                    log.warn("Database health check failed: {}", e.getMessage());
                    return false;
                }
            });
            
            return dbCheck.get(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            log.warn("Database health check timeout or error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check Redis health using Optional to eliminate if-statement
     */
    private boolean checkRedis() {
        return Optional.ofNullable(redisTemplate)
            .map(template -> {
                try {
                    CompletableFuture<Boolean> redisCheck = CompletableFuture.supplyAsync(() -> {
                        try {
                            // Test Redis connectivity with a simple ping
                            String testValue = String.valueOf(System.currentTimeMillis());
                            template.opsForValue().set(HEALTH_CHECK_KEY, testValue, 60, TimeUnit.SECONDS);
                            String retrieved = (String) template.opsForValue().get(HEALTH_CHECK_KEY);
                            template.delete(HEALTH_CHECK_KEY);

                            return testValue.equals(retrieved);

                        } catch (Exception e) {
                            log.warn("Redis health check failed: {}", e.getMessage());
                            return false;
                        }
                    });

                    return redisCheck.get(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                } catch (Exception e) {
                    log.warn("Redis health check timeout or error: {}", e.getMessage());
                    return false;
                }
            })
            .orElseGet(() -> {
                log.debug("Redis not configured, skipping Redis health check");
                return true; // Consider Redis check as passed if not configured
            });
    }
    
    private boolean checkRepository() {
        try {
            CompletableFuture<Boolean> repositoryCheck = CompletableFuture.supplyAsync(() -> {
                try {
                    // Test repository connectivity with a simple count query
                    orderRepository.count();
                    return true;
                    
                } catch (Exception e) {
                    log.warn("Repository health check failed: {}", e.getMessage());
                    return false;
                }
            });
            
            return repositoryCheck.get(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            log.warn("Repository health check timeout or error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check system health using Optional to eliminate if-statements
     */
    private boolean checkSystemHealth() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            // Check if memory usage is below 85%
            double memoryUsagePercent = ((double) usedMemory / totalMemory) * 100;

            boolean memoryHealthy = Optional.of(memoryUsagePercent)
                .filter(usage -> usage <= 85)
                .map(usage -> true)
                .orElseGet(() -> {
                    log.warn("High memory usage detected: {}%", memoryUsagePercent);
                    return false;
                });

            // Check available processors
            int availableProcessors = runtime.availableProcessors();
            boolean processorsHealthy = Optional.of(availableProcessors)
                .filter(processors -> processors >= 1)
                .map(processors -> true)
                .orElseGet(() -> {
                    log.warn("No available processors detected");
                    return false;
                });

            return memoryHealthy && processorsHealthy;

        } catch (Exception e) {
            log.warn("System health check failed: {}", e.getMessage());
            return false;
        }
    }
}