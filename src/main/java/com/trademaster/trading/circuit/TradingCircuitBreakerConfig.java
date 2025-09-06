package com.trademaster.trading.circuit;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Circuit Breaker Configuration for Trading Service
 * 
 * Implements circuit breaker pattern using Resilience4j for fault tolerance
 * and system stability. Provides automatic failure detection, fast failure
 * response, and automatic recovery for critical trading operations.
 * 
 * Circuit Breaker States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Failure threshold exceeded, requests fail fast
 * - HALF_OPEN: Testing recovery, limited requests allowed
 * 
 * Key Features:
 * - Configurable failure thresholds and timeouts
 * - Automatic recovery and health monitoring
 * - Custom failure predicates for domain-specific errors
 * - Metrics and monitoring integration
 * - Fallback mechanisms for graceful degradation
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Java 24 + Resilience4j)
 */
@Configuration
@Slf4j
public class TradingCircuitBreakerConfig {
    
    // Circuit breaker configuration properties
    @Value("${trading.circuit-breaker.failure-rate-threshold:50.0}")
    private Float failureRateThreshold;
    
    @Value("${trading.circuit-breaker.slow-call-rate-threshold:80.0}")
    private Float slowCallRateThreshold;
    
    @Value("${trading.circuit-breaker.slow-call-duration-threshold:5000}")
    private Long slowCallDurationThresholdMs;
    
    @Value("${trading.circuit-breaker.permitted-calls-in-half-open:5}")
    private Integer permittedCallsInHalfOpen;
    
    @Value("${trading.circuit-breaker.sliding-window-size:10}")
    private Integer slidingWindowSize;
    
    @Value("${trading.circuit-breaker.minimum-calls:5}")
    private Integer minimumNumberOfCalls;
    
    @Value("${trading.circuit-breaker.wait-duration:30000}")
    private Long waitDurationMs;
    
    @Value("${trading.circuit-breaker.automatic-transition:true}")
    private Boolean automaticTransitionFromOpenToHalfOpenEnabled;
    
    /**
     * Circuit breaker registry for managing multiple circuit breakers
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
    
    /**
     * Order processing circuit breaker
     * 
     * Protects order placement, modification, and cancellation operations.
     * Configured with aggressive thresholds for financial operations.
     */
    @Bean
    public CircuitBreaker orderProcessingCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .slowCallRateThreshold(slowCallRateThreshold)
            .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationThresholdMs))
            .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpen)
            .slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(minimumNumberOfCalls)
            .waitDurationInOpenState(Duration.ofMillis(waitDurationMs))
            .automaticTransitionFromOpenToHalfOpenEnabled(automaticTransitionFromOpenToHalfOpenEnabled)
            .recordExceptions(
                RuntimeException.class,
                IllegalStateException.class,
                IllegalArgumentException.class
            )
            .ignoreExceptions(
                SecurityException.class,
                IllegalAccessException.class
            )
            .recordExceptions(Exception.class)
            .build();
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker("orderProcessing", config);
        
        // Add event listeners for monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Order processing circuit breaker state transition: {} -> {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onCallNotPermitted(event ->
                log.warn("Order processing call not permitted due to circuit breaker"))
            .onError(event ->
                log.error("Order processing circuit breaker recorded error: {}", 
                    event.getThrowable().getMessage()));
        
        return circuitBreaker;
    }
    
    /**
     * Risk management circuit breaker
     * 
     * Protects risk validation, VaR calculations, and portfolio analytics.
     * More lenient configuration to prevent blocking valid trades.
     */
    @Bean
    public CircuitBreaker riskManagementCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(70.0f) // Higher threshold for risk operations
            .slowCallRateThreshold(90.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(10))
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowSize(15)
            .minimumNumberOfCalls(8)
            .waitDurationInOpenState(Duration.ofSeconds(45))
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                RuntimeException.class,
                ArithmeticException.class,
                IllegalStateException.class
            )
            .ignoreExceptions(
                SecurityException.class,
                IllegalArgumentException.class // Validation errors should not trip breaker
            )
            .recordExceptions(Exception.class)
            .build();
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker("riskManagement", config);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Risk management circuit breaker state transition: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onCallNotPermitted(event ->
                log.warn("Risk management call not permitted due to circuit breaker"))
            .onError(event ->
                log.error("Risk management circuit breaker recorded error: {}",
                    event.getThrowable().getMessage()));
        
        return circuitBreaker;
    }
    
    /**
     * Broker integration circuit breaker
     * 
     * Protects external broker API calls and order routing.
     * Conservative configuration to handle network and external service failures.
     */
    @Bean
    public CircuitBreaker brokerIntegrationCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(60.0f)
            .slowCallRateThreshold(85.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(8))
            .permittedNumberOfCallsInHalfOpenState(2)
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .waitDurationInOpenState(Duration.ofMinutes(2))
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                RuntimeException.class,
                java.net.ConnectException.class,
                java.net.SocketTimeoutException.class,
                java.io.IOException.class
            )
            .ignoreExceptions(
                SecurityException.class,
                IllegalArgumentException.class
            )
            .recordExceptions(Exception.class)
            .build();
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker("brokerIntegration", config);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Broker integration circuit breaker state transition: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onCallNotPermitted(event ->
                log.warn("Broker integration call not permitted due to circuit breaker"))
            .onError(event ->
                log.error("Broker integration circuit breaker recorded error: {}",
                    event.getThrowable().getMessage()));
        
        return circuitBreaker;
    }
    
    /**
     * Portfolio service circuit breaker
     * 
     * Protects portfolio calculations, position updates, and performance analytics.
     * Balanced configuration for non-critical operations.
     */
    @Bean
    public CircuitBreaker portfolioServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(65.0f)
            .slowCallRateThreshold(80.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(7))
            .permittedNumberOfCallsInHalfOpenState(4)
            .slidingWindowSize(12)
            .minimumNumberOfCalls(6)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                RuntimeException.class,
                ArithmeticException.class
            )
            .ignoreExceptions(
                SecurityException.class,
                IllegalArgumentException.class
            )
            .recordExceptions(Exception.class)
            .build();
        
        CircuitBreaker circuitBreaker = registry.circuitBreaker("portfolioService", config);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Portfolio service circuit breaker state transition: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onCallNotPermitted(event ->
                log.warn("Portfolio service call not permitted due to circuit breaker"))
            .onError(event ->
                log.error("Portfolio service circuit breaker recorded error: {}",
                    event.getThrowable().getMessage()));
        
        return circuitBreaker;
    }
    
    // Custom failure predicates for domain-specific error classification
    
    private Predicate<Throwable> createOrderFailurePredicate() {
        return throwable -> {
            // Consider these as failures that should trip the circuit breaker
            return throwable instanceof RuntimeException ||
                   throwable instanceof IllegalStateException ||
                   (throwable instanceof Exception && 
                    throwable.getMessage() != null &&
                    (throwable.getMessage().contains("timeout") ||
                     throwable.getMessage().contains("connection") ||
                     throwable.getMessage().contains("unavailable")));
        };
    }
    
    private Predicate<Throwable> createRiskFailurePredicate() {
        return throwable -> {
            // Risk calculation failures should trip breaker, validation errors should not
            return throwable instanceof RuntimeException &&
                   !(throwable instanceof IllegalArgumentException) &&
                   throwable.getMessage() != null &&
                   !throwable.getMessage().toLowerCase().contains("validation");
        };
    }
    
    private Predicate<Throwable> createBrokerFailurePredicate() {
        return throwable -> {
            // Network and external service errors should trip breaker
            return throwable instanceof java.net.ConnectException ||
                   throwable instanceof java.net.SocketTimeoutException ||
                   throwable instanceof java.io.IOException ||
                   (throwable instanceof RuntimeException &&
                    throwable.getMessage() != null &&
                    (throwable.getMessage().contains("broker") ||
                     throwable.getMessage().contains("network") ||
                     throwable.getMessage().contains("timeout")));
        };
    }
    
    private Predicate<Throwable> createPortfolioFailurePredicate() {
        return throwable -> {
            // Portfolio calculation errors should trip breaker
            return throwable instanceof ArithmeticException ||
                   (throwable instanceof RuntimeException &&
                    throwable.getMessage() != null &&
                    (throwable.getMessage().contains("calculation") ||
                     throwable.getMessage().contains("portfolio") ||
                     throwable.getMessage().contains("position")));
        };
    }
}