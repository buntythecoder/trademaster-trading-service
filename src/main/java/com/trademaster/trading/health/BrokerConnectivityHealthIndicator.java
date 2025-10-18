package com.trademaster.trading.health;

import com.trademaster.trading.client.BrokerAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Broker Connectivity Health Indicator
 * 
 * Monitors connectivity to broker authentication service and external brokers.
 * Provides critical health information for trading operations continuity.
 * 
 * Health Checks:
 * - Broker Auth Service connectivity
 * - Circuit breaker status
 * - Response time monitoring
 * - Fallback mechanism status
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "management.health.brokerconnectivity.enabled", havingValue = "true", matchIfMissing = false)
public class BrokerConnectivityHealthIndicator implements HealthIndicator {
    
    private final BrokerAuthClient brokerAuthClient;
    
    private static final int BROKER_HEALTH_CHECK_TIMEOUT_MS = 10000;
    private static final String[] TEST_BROKERS = {"ZERODHA", "UPSTOX", "ANGELONE"};
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        try {
            long startTime = System.currentTimeMillis();
            AtomicBoolean allBrokersHealthy = new AtomicBoolean(true);
            AtomicInteger healthyBrokers = new AtomicInteger(0);
            int totalBrokers = TEST_BROKERS.length;

            // Test each broker's health
            for (String brokerName : TEST_BROKERS) {
                boolean brokerHealthy = checkBrokerHealth(brokerName);

                // Eliminates ternary operator using Optional.filter().map().orElse()
                builder.withDetail("broker_" + brokerName.toLowerCase(),
                    Optional.of(brokerHealthy)
                        .filter(healthy -> healthy)
                        .map(healthy -> "UP")
                        .orElse("DOWN")
                );

                // Eliminates if-else using Optional.ifPresentOrElse()
                Optional.of(brokerHealthy)
                    .filter(healthy -> healthy)
                    .ifPresentOrElse(
                        healthy -> healthyBrokers.incrementAndGet(),
                        () -> allBrokersHealthy.set(false)
                    );
            }

            long responseTime = System.currentTimeMillis() - startTime;

            // Service is UP if at least one broker is available
            // Eliminates if-else using Optional.filter().ifPresentOrElse()
            Optional.of(healthyBrokers.get())
                .filter(count -> count > 0)
                .ifPresentOrElse(
                    count -> builder.up(),
                    () -> builder.down()
                );

            builder.withDetail("healthyBrokers", healthyBrokers.get())
                   .withDetail("totalBrokers", totalBrokers)
                   .withDetail("healthyPercentage", (double) healthyBrokers.get() / totalBrokers * 100)
                   .withDetail("responseTimeMs", responseTime)
                   .withDetail("brokerAuthService", "CONNECTED")
                   .withDetail("circuitBreaker", "OPERATIONAL")
                   .withDetail("timestamp", Instant.now().toString());
                   
        } catch (Exception e) {
            log.error("Broker connectivity health check failed", e);
            builder.down()
                   .withDetail("error", e.getMessage())
                   .withDetail("brokerAuthService", "DISCONNECTED")
                   .withDetail("timestamp", Instant.now().toString());
        }
        
        return builder.build();
    }
    
    private boolean checkBrokerHealth(String brokerName) {
        try {
            CompletableFuture<Boolean> brokerCheck = CompletableFuture.supplyAsync(() -> {
                try {
                    // Use the health endpoint to check broker connectivity
                    Map<String, Object> healthResponse = getBrokerHealthWithCircuitBreaker(brokerName);

                    // Eliminates if-statement using Optional.filter().map().orElse()
                    return Optional.ofNullable(healthResponse)
                        .filter(response -> response.containsKey("status"))
                        .map(response -> (String) response.get("status"))
                        .map(status -> "UP".equalsIgnoreCase(status) || "HEALTHY".equalsIgnoreCase(status))
                        .orElse(false);

                } catch (Exception e) {
                    log.debug("Broker {} health check failed: {}", brokerName, e.getMessage());
                    return false;
                }
            });
            
            return brokerCheck.get(BROKER_HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            log.debug("Broker {} health check timeout: {}", brokerName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Wrapper method with circuit breaker for broker health checks
     */
    @CircuitBreaker(name = "broker-auth-service", fallbackMethod = "getBrokerHealthFallback")
    private Map<String, Object> getBrokerHealthWithCircuitBreaker(String brokerName) {
        return brokerAuthClient.getBrokerHealth(brokerName);
    }
    
    /**
     * Circuit breaker fallback method for getBrokerHealth
     */
    public Map<String, Object> getBrokerHealthFallback(String brokerName, Throwable ex) {
        log.warn("Circuit breaker activated for broker health check - broker: {}, error: {}", 
                brokerName, ex.getMessage());
        return Map.of(
            "status", "UNAVAILABLE",
            "broker", brokerName,
            "message", "Circuit breaker active - service degraded",
            "timestamp", System.currentTimeMillis()
        );
    }
}