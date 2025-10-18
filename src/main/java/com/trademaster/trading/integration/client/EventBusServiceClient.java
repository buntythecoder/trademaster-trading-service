package com.trademaster.trading.integration.client;

import com.trademaster.common.functional.Result;
import com.trademaster.common.integration.client.AbstractInternalServiceClient;
import com.trademaster.common.properties.CommonServiceProperties;
import com.trademaster.trading.error.ServiceError;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Event Bus Service Client
 *
 * MANDATORY: Rule #3 - Functional Programming (Result monad, no if-else)
 * MANDATORY: Rule #6 - Zero Trust Security (Internal service-to-service)
 * MANDATORY: Rule #11 - Error Handling Patterns (Result monad)
 * MANDATORY: Rule #25 - Circuit Breaker Implementation
 *
 * Client for event-bus-service integration with circuit breaker protection.
 * Implements fire-and-forget event publishing pattern (non-blocking).
 *
 * Features:
 * - Circuit breaker on all event publishing calls
 * - Functional error handling with Result monad
 * - Fire-and-forget pattern (don't block order flow)
 * - Async publishing with CompletableFuture
 * - Graceful degradation (log failures, don't throw)
 * - Correlation ID propagation
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class EventBusServiceClient extends AbstractInternalServiceClient {

    private static final String SERVICE_NAME = "event-bus-service";
    private static final String CB_NAME = "event-bus-service";

    public EventBusServiceClient(RestTemplate restTemplate,
                                 CommonServiceProperties properties) {
        super(restTemplate, properties);
    }

    /**
     * Publish event to event bus (fire-and-forget pattern)
     * Rule #25: Circuit breaker protection for event publishing
     * Rule #11: Functional error handling with Result monad
     * Rule #12: Async with CompletableFuture using Virtual Threads
     *
     * @param event The event object to publish
     * @param correlationId Correlation ID for distributed tracing
     * @return CompletableFuture with Result indicating success or failure
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "publishEventFallback")
    public CompletableFuture<Result<Void, ServiceError>> publishEventAsync(
            Object event, String correlationId) {

        log.debug("Publishing event {} - correlationId: {}",
                event.getClass().getSimpleName(), correlationId);

        return CompletableFuture.supplyAsync(() ->
            callService(SERVICE_NAME,
                    "/api/internal/v1/events/publish",
                    HttpMethod.POST,
                    event,
                    Map.class)
                .map(response -> {
                    log.info("Event published successfully: {} - correlationId: {}",
                            event.getClass().getSimpleName(), correlationId);
                    return Result.<Void, ServiceError>success(null);
                })
                .orElseGet(() -> {
                    String msg = String.format("Failed to publish event %s",
                            event.getClass().getSimpleName());
                    log.warn("Event publishing failed: {} - correlationId: {}",
                            msg, correlationId);
                    return Result.failure(new ServiceError.ServiceUnavailable(SERVICE_NAME, msg));
                })
        );
    }

    /**
     * Fallback method for event publishing when circuit breaker is open
     * Rule #25: Graceful degradation - log failure but don't block order flow
     *
     * Fire-and-forget pattern: Log the failure and return success to prevent
     * blocking the main order flow. Events can be reprocessed from audit logs.
     */
    private CompletableFuture<Result<Void, ServiceError>> publishEventFallback(
            Object event, String correlationId, Exception ex) {

        log.warn("Event bus circuit breaker activated - event not published: {} - correlationId: {} - error: {}",
                event.getClass().getSimpleName(), correlationId, ex.getMessage());

        // Fire-and-forget: Return success to prevent blocking order flow
        // Events can be reprocessed from database audit trail later
        return CompletableFuture.completedFuture(
            Result.success(null)
        );
    }

    /**
     * Synchronous event publishing (blocking)
     * Use this only when you need to wait for event publishing confirmation
     *
     * @param event The event object to publish
     * @param correlationId Correlation ID for distributed tracing
     * @return Result indicating success or failure
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "publishEventSyncFallback")
    public Result<Void, ServiceError> publishEventSync(Object event, String correlationId) {

        log.debug("Publishing event synchronously {} - correlationId: {}",
                event.getClass().getSimpleName(), correlationId);

        return callService(SERVICE_NAME,
                "/api/internal/v1/events/publish",
                HttpMethod.POST,
                event,
                Map.class)
            .map(response -> {
                log.info("Event published successfully: {} - correlationId: {}",
                        event.getClass().getSimpleName(), correlationId);
                return Result.<Void, ServiceError>success(null);
            })
            .orElseGet(() -> {
                String msg = String.format("Failed to publish event %s",
                        event.getClass().getSimpleName());
                log.error("Event publishing failed: {} - correlationId: {}",
                        msg, correlationId);
                return Result.failure(new ServiceError.ServiceUnavailable(SERVICE_NAME, msg));
            });
    }

    /**
     * Fallback method for synchronous event publishing
     */
    private Result<Void, ServiceError> publishEventSyncFallback(
            Object event, String correlationId, Exception ex) {

        log.error("Event bus circuit breaker activated - synchronous event not published: {} - correlationId: {}",
                event.getClass().getSimpleName(), correlationId, ex);

        return Result.failure(new ServiceError.CircuitBreakerOpen(
            SERVICE_NAME,
            "Event bus unavailable - event not published: " + event.getClass().getSimpleName()
        ));
    }

    /**
     * Publish batch of events (fire-and-forget pattern)
     * Rule #25: Circuit breaker protection
     * Rule #12: Async batch processing with Virtual Threads
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "publishBatchFallback")
    public CompletableFuture<Result<Integer, ServiceError>> publishBatchAsync(
            java.util.List<Object> events, String correlationId) {

        log.debug("Publishing batch of {} events - correlationId: {}",
                events.size(), correlationId);

        Map<String, Object> batchRequest = Map.of(
            "events", events,
            "correlationId", correlationId,
            "timestamp", LocalDateTime.now().toString()
        );

        return CompletableFuture.supplyAsync(() ->
            callService(SERVICE_NAME,
                    "/api/internal/v1/events/publish/batch",
                    HttpMethod.POST,
                    batchRequest,
                    Map.class)
                .map(response -> {
                    log.info("Batch published successfully: {} events - correlationId: {}",
                            events.size(), correlationId);
                    return Result.<Integer, ServiceError>success(events.size());
                })
                .orElseGet(() -> {
                    String msg = String.format("Failed to publish batch of %d events",
                            events.size());
                    log.warn("Batch publishing failed: {} - correlationId: {}",
                            msg, correlationId);
                    return Result.failure(new ServiceError.ServiceUnavailable(SERVICE_NAME, msg));
                })
        );
    }

    /**
     * Fallback method for batch publishing when circuit breaker is open
     */
    private CompletableFuture<Result<Integer, ServiceError>> publishBatchFallback(
            java.util.List<Object> events, String correlationId, Exception ex) {

        log.warn("Event bus circuit breaker activated - batch not published: {} events - correlationId: {}",
                events.size(), correlationId, ex);

        // Fire-and-forget: Return success to prevent blocking order flow
        return CompletableFuture.completedFuture(
            Result.success(0)  // 0 events published
        );
    }

    /**
     * Check event bus service health
     */
    public boolean isHealthy() {
        return checkServiceHealth(SERVICE_NAME);
    }
}
