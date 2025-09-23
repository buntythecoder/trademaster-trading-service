package com.trademaster.trading.controller;

import com.trademaster.trading.health.BrokerConnectivityHealthIndicator;
import com.trademaster.trading.health.TradingLivenessIndicator;
import com.trademaster.trading.health.TradingReadinessIndicator;
import com.trademaster.trading.health.TradingServiceHealthIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * API Gateway Compatible Health Endpoints Controller
 * 
 * Provides standardized health endpoints for API Gateway integration.
 * These endpoints are optimized for load balancer health checks and monitoring systems.
 * 
 * Endpoints:
 * - /gateway/health - Comprehensive health status
 * - /gateway/ready - Readiness probe for Kubernetes/load balancers
 * - /gateway/alive - Liveness probe for Kubernetes
 * - /gateway/status - Detailed service status with metrics
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/gateway")
@Slf4j
public class GatewayController {

    private final TradingServiceHealthIndicator serviceHealthIndicator;
    private final TradingReadinessIndicator readinessIndicator;
    private final TradingLivenessIndicator livenessIndicator;

    @Autowired(required = false)
    private BrokerConnectivityHealthIndicator brokerHealthIndicator;

    private static final int HEALTH_CHECK_TIMEOUT_MS = 5000;

    public GatewayController(TradingServiceHealthIndicator serviceHealthIndicator,
                           TradingReadinessIndicator readinessIndicator,
                           TradingLivenessIndicator livenessIndicator) {
        this.serviceHealthIndicator = serviceHealthIndicator;
        this.readinessIndicator = readinessIndicator;
        this.livenessIndicator = livenessIndicator;
    }
    
    /**
     * Comprehensive health check for API Gateway
     * Returns 200 if service is healthy, 503 if unhealthy
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Run all health checks in parallel
            CompletableFuture<Health> serviceHealth = CompletableFuture
                .supplyAsync(serviceHealthIndicator::health);
            CompletableFuture<Health> brokerHealth = brokerHealthIndicator != null ?
                CompletableFuture.supplyAsync(brokerHealthIndicator::health) :
                CompletableFuture.completedFuture(Health.up()
                    .withDetail("status", "DISABLED")
                    .withDetail("message", "Broker connectivity health check is disabled")
                    .build());
            CompletableFuture<Health> readinessHealth = CompletableFuture
                .supplyAsync(readinessIndicator::health);
            CompletableFuture<Health> livenessHealth = CompletableFuture
                .supplyAsync(livenessIndicator::health);

            // Wait for all checks with timeout
            CompletableFuture.allOf(serviceHealth, brokerHealth, readinessHealth, livenessHealth)
                .get(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            Health service = serviceHealth.join();
            Health broker = brokerHealth.join();
            Health readiness = readinessHealth.join();
            Health liveness = livenessHealth.join();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Overall health is UP if critical components are healthy
            boolean isHealthy = isHealthUp(service) && isHealthUp(readiness) && isHealthUp(liveness);
            boolean brokerHealthy = isHealthUp(broker);
            
            HealthResponse response = HealthResponse.builder()
                .status(isHealthy ? "UP" : "DOWN")
                .service(getHealthStatus(service))
                .brokers(getHealthStatus(broker))
                .readiness(getHealthStatus(readiness))
                .liveness(getHealthStatus(liveness))
                .overallHealth(isHealthy)
                .brokerConnectivity(brokerHealthy)
                .responseTimeMs(responseTime)
                .timestamp(Instant.now().toString())
                .build();
            
            return isHealthy ? 
                ResponseEntity.ok(response) : 
                ResponseEntity.status(503).body(response);
                
        } catch (Exception e) {
            log.error("Gateway health check failed", e);
            
            HealthResponse errorResponse = HealthResponse.builder()
                .status("DOWN")
                .service("ERROR")
                .brokers("UNKNOWN")
                .readiness("UNKNOWN")
                .liveness("UNKNOWN")
                .overallHealth(false)
                .brokerConnectivity(false)
                .error(e.getMessage())
                .timestamp(Instant.now().toString())
                .build();
                
            return ResponseEntity.status(503).body(errorResponse);
        }
    }
    
    /**
     * Readiness probe endpoint for Kubernetes and load balancers
     * Returns 200 if service is ready to receive traffic, 503 if not ready
     */
    @GetMapping("/ready")
    public ResponseEntity<ReadinessResponse> ready() {
        try {
            Health readiness = readinessIndicator.health();
            boolean isReady = isHealthUp(readiness);
            
            ReadinessResponse response = ReadinessResponse.builder()
                .ready(isReady)
                .status(isReady ? "READY" : "NOT_READY")
                .details(readiness.getDetails())
                .timestamp(Instant.now().toString())
                .build();
            
            return isReady ? 
                ResponseEntity.ok(response) : 
                ResponseEntity.status(503).body(response);
                
        } catch (Exception e) {
            log.error("Readiness check failed", e);
            
            ReadinessResponse errorResponse = ReadinessResponse.builder()
                .ready(false)
                .status("ERROR")
                .error(e.getMessage())
                .timestamp(Instant.now().toString())
                .build();
                
            return ResponseEntity.status(503).body(errorResponse);
        }
    }
    
    /**
     * Liveness probe endpoint for Kubernetes
     * Returns 200 if service is alive, 503 if service needs restart
     */
    @GetMapping("/alive")
    public ResponseEntity<LivenessResponse> alive() {
        try {
            Health liveness = livenessIndicator.health();
            boolean isAlive = isHealthUp(liveness);
            
            LivenessResponse response = LivenessResponse.builder()
                .alive(isAlive)
                .status(isAlive ? "ALIVE" : "NEEDS_RESTART")
                .details(liveness.getDetails())
                .timestamp(Instant.now().toString())
                .build();
            
            return isAlive ? 
                ResponseEntity.ok(response) : 
                ResponseEntity.status(503).body(response);
                
        } catch (Exception e) {
            log.error("Liveness check failed", e);
            
            LivenessResponse errorResponse = LivenessResponse.builder()
                .alive(false)
                .status("ERROR")
                .error(e.getMessage())
                .timestamp(Instant.now().toString())
                .build();
                
            return ResponseEntity.status(503).body(errorResponse);
        }
    }
    
    /**
     * Detailed service status with metrics for monitoring systems
     */
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status() {
        try {
            Health service = serviceHealthIndicator.health();
            Health broker = brokerHealthIndicator != null ?
                brokerHealthIndicator.health() :
                Health.up()
                    .withDetail("status", "DISABLED")
                    .withDetail("message", "Broker connectivity health check is disabled")
                    .build();
            
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            StatusResponse response = StatusResponse.builder()
                .serviceName("trading-service")
                .version("2.0.0")
                .environment(System.getProperty("spring.profiles.active", "default"))
                .serviceHealth(getHealthStatus(service))
                .brokerHealth(getHealthStatus(broker))
                .memoryUsedMB(usedMemory / 1024 / 1024)
                .memoryTotalMB(totalMemory / 1024 / 1024)
                .memoryUsagePercent((double) usedMemory / totalMemory * 100)
                .availableProcessors(runtime.availableProcessors())
                .virtualThreadsEnabled(true)
                .uptime(System.currentTimeMillis())
                .timestamp(Instant.now().toString())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Status check failed", e);
            
            StatusResponse errorResponse = StatusResponse.builder()
                .serviceName("trading-service")
                .version("2.0.0")
                .serviceHealth("ERROR")
                .brokerHealth("UNKNOWN")
                .error(e.getMessage())
                .timestamp(Instant.now().toString())
                .build();
                
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    private boolean isHealthUp(Health health) {
        return "UP".equalsIgnoreCase(health.getStatus().getCode());
    }
    
    private String getHealthStatus(Health health) {
        return health.getStatus().getCode();
    }
    
    // Response DTOs
    public record HealthResponse(
        String status,
        String service,
        String brokers,
        String readiness,
        String liveness,
        boolean overallHealth,
        boolean brokerConnectivity,
        long responseTimeMs,
        String error,
        String timestamp
    ) {
        public static HealthResponseBuilder builder() {
            return new HealthResponseBuilder();
        }
        
        public static class HealthResponseBuilder {
            private String status, service, brokers, readiness, liveness, error, timestamp;
            private boolean overallHealth, brokerConnectivity;
            private long responseTimeMs;
            
            public HealthResponseBuilder status(String status) { this.status = status; return this; }
            public HealthResponseBuilder service(String service) { this.service = service; return this; }
            public HealthResponseBuilder brokers(String brokers) { this.brokers = brokers; return this; }
            public HealthResponseBuilder readiness(String readiness) { this.readiness = readiness; return this; }
            public HealthResponseBuilder liveness(String liveness) { this.liveness = liveness; return this; }
            public HealthResponseBuilder overallHealth(boolean overallHealth) { this.overallHealth = overallHealth; return this; }
            public HealthResponseBuilder brokerConnectivity(boolean brokerConnectivity) { this.brokerConnectivity = brokerConnectivity; return this; }
            public HealthResponseBuilder responseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; return this; }
            public HealthResponseBuilder error(String error) { this.error = error; return this; }
            public HealthResponseBuilder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
            
            public HealthResponse build() {
                return new HealthResponse(status, service, brokers, readiness, liveness, 
                    overallHealth, brokerConnectivity, responseTimeMs, error, timestamp);
            }
        }
    }
    
    public record ReadinessResponse(
        boolean ready,
        String status,
        Object details,
        String error,
        String timestamp
    ) {
        public static ReadinessResponseBuilder builder() {
            return new ReadinessResponseBuilder();
        }
        
        public static class ReadinessResponseBuilder {
            private boolean ready;
            private String status, error, timestamp;
            private Object details;
            
            public ReadinessResponseBuilder ready(boolean ready) { this.ready = ready; return this; }
            public ReadinessResponseBuilder status(String status) { this.status = status; return this; }
            public ReadinessResponseBuilder details(Object details) { this.details = details; return this; }
            public ReadinessResponseBuilder error(String error) { this.error = error; return this; }
            public ReadinessResponseBuilder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
            
            public ReadinessResponse build() {
                return new ReadinessResponse(ready, status, details, error, timestamp);
            }
        }
    }
    
    public record LivenessResponse(
        boolean alive,
        String status,
        Object details,
        String error,
        String timestamp
    ) {
        public static LivenessResponseBuilder builder() {
            return new LivenessResponseBuilder();
        }
        
        public static class LivenessResponseBuilder {
            private boolean alive;
            private String status, error, timestamp;
            private Object details;
            
            public LivenessResponseBuilder alive(boolean alive) { this.alive = alive; return this; }
            public LivenessResponseBuilder status(String status) { this.status = status; return this; }
            public LivenessResponseBuilder details(Object details) { this.details = details; return this; }
            public LivenessResponseBuilder error(String error) { this.error = error; return this; }
            public LivenessResponseBuilder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
            
            public LivenessResponse build() {
                return new LivenessResponse(alive, status, details, error, timestamp);
            }
        }
    }
    
    public record StatusResponse(
        String serviceName,
        String version,
        String environment,
        String serviceHealth,
        String brokerHealth,
        long memoryUsedMB,
        long memoryTotalMB,
        double memoryUsagePercent,
        int availableProcessors,
        boolean virtualThreadsEnabled,
        long uptime,
        String error,
        String timestamp
    ) {
        public static StatusResponseBuilder builder() {
            return new StatusResponseBuilder();
        }
        
        public static class StatusResponseBuilder {
            private String serviceName, version, environment, serviceHealth, brokerHealth, error, timestamp;
            private long memoryUsedMB, memoryTotalMB, uptime;
            private double memoryUsagePercent;
            private int availableProcessors;
            private boolean virtualThreadsEnabled;
            
            public StatusResponseBuilder serviceName(String serviceName) { this.serviceName = serviceName; return this; }
            public StatusResponseBuilder version(String version) { this.version = version; return this; }
            public StatusResponseBuilder environment(String environment) { this.environment = environment; return this; }
            public StatusResponseBuilder serviceHealth(String serviceHealth) { this.serviceHealth = serviceHealth; return this; }
            public StatusResponseBuilder brokerHealth(String brokerHealth) { this.brokerHealth = brokerHealth; return this; }
            public StatusResponseBuilder memoryUsedMB(long memoryUsedMB) { this.memoryUsedMB = memoryUsedMB; return this; }
            public StatusResponseBuilder memoryTotalMB(long memoryTotalMB) { this.memoryTotalMB = memoryTotalMB; return this; }
            public StatusResponseBuilder memoryUsagePercent(double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; return this; }
            public StatusResponseBuilder availableProcessors(int availableProcessors) { this.availableProcessors = availableProcessors; return this; }
            public StatusResponseBuilder virtualThreadsEnabled(boolean virtualThreadsEnabled) { this.virtualThreadsEnabled = virtualThreadsEnabled; return this; }
            public StatusResponseBuilder uptime(long uptime) { this.uptime = uptime; return this; }
            public StatusResponseBuilder error(String error) { this.error = error; return this; }
            public StatusResponseBuilder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
            
            public StatusResponse build() {
                return new StatusResponse(serviceName, version, environment, serviceHealth, brokerHealth, 
                    memoryUsedMB, memoryTotalMB, memoryUsagePercent, availableProcessors, 
                    virtualThreadsEnabled, uptime, error, timestamp);
            }
        }
    }
}