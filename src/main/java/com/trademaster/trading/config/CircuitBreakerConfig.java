package com.trademaster.trading.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Circuit Breaker Configuration
 * 
 * Configures circuit breakers for external service calls with proper
 * fallback mechanisms and monitoring integration.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class CircuitBreakerConfig {
    
    /**
     * Circuit breaker registry with custom configurations
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
    
    /**
     * Broker Auth Service Circuit Breaker
     */
    @Bean
    public CircuitBreaker brokerAuthCircuitBreaker(CircuitBreakerRegistry registry) {
        var config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();
            
        CircuitBreaker circuitBreaker = registry.circuitBreaker("broker-auth-service", config);
        
        // Add event listeners for monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Circuit breaker state transition: {} -> {} for {}", 
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState(),
                        event.getCircuitBreakerName()))
            .onFailureRateExceeded(event -> 
                log.warn("Circuit breaker failure rate exceeded: {}% for {}", 
                        event.getFailureRate(), 
                        event.getCircuitBreakerName()))
            .onCallNotPermitted(event -> 
                log.warn("Circuit breaker call not permitted for {}", 
                        event.getCircuitBreakerName()));
                        
        return circuitBreaker;
    }
    
    /**
     * Portfolio Service Circuit Breaker
     */
    @Bean
    public CircuitBreaker portfolioServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        var config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();
            
        return registry.circuitBreaker("portfolio-service", config);
    }
    
    /**
     * Time limiter for circuit breaker timeouts
     */
    @Bean
    public TimeLimiter brokerAuthTimeLimiter() {
        var config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10))
            .cancelRunningFuture(true)
            .build();
            
        return TimeLimiter.of("broker-auth-service", config);
    }
    
    /**
     * Portfolio service time limiter
     */
    @Bean
    public TimeLimiter portfolioServiceTimeLimiter() {
        var config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .cancelRunningFuture(true)
            .build();
            
        return TimeLimiter.of("portfolio-service", config);
    }
    
    /**
     * Database Operations Circuit Breaker
     * For critical database operations with high latency risk
     */
    @Bean
    public CircuitBreaker databaseOperationsCircuitBreaker(CircuitBreakerRegistry registry) {
        var config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowType(SlidingWindowType.TIME_BASED)
            .slidingWindowSize(60) // 60 seconds window
            .minimumNumberOfCalls(10)
            .failureRateThreshold(60.0f)
            .waitDurationInOpenState(Duration.ofSeconds(45))
            .permittedNumberOfCallsInHalfOpenState(5)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class, IllegalStateException.class)
            .build();
            
        CircuitBreaker circuitBreaker = registry.circuitBreaker("database-operations", config);
        
        // Add event listeners for monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("Database circuit breaker state transition: {} -> {} for {}", 
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState(),
                        event.getCircuitBreakerName()))
            .onFailureRateExceeded(event -> 
                log.error("Database circuit breaker failure rate exceeded: {}% for {}", 
                        event.getFailureRate(), 
                        event.getCircuitBreakerName()))
            .onCallNotPermitted(event -> 
                log.error("Database circuit breaker call not permitted for {}", 
                        event.getCircuitBreakerName()));
                        
        return circuitBreaker;
    }
    
    /**
     * Message Queue Operations Circuit Breaker
     * For message publishing/consuming operations
     */
    @Bean
    public CircuitBreaker messageQueueCircuitBreaker(CircuitBreakerRegistry registry) {
        var config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(20)
            .minimumNumberOfCalls(8)
            .failureRateThreshold(40.0f)
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .permittedNumberOfCallsInHalfOpenState(4)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();
            
        CircuitBreaker circuitBreaker = registry.circuitBreaker("message-queue", config);
        
        // Add event listeners for monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("Message queue circuit breaker state transition: {} -> {} for {}", 
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState(),
                        event.getCircuitBreakerName()))
            .onFailureRateExceeded(event -> 
                log.error("Message queue circuit breaker failure rate exceeded: {}% for {}", 
                        event.getFailureRate(), 
                        event.getCircuitBreakerName()))
            .onCallNotPermitted(event -> 
                log.error("Message queue circuit breaker call not permitted for {}", 
                        event.getCircuitBreakerName()));
                        
        return circuitBreaker;
    }
    
    /**
     * External File I/O Operations Circuit Breaker
     * For file system operations that may fail
     */
    @Bean
    public CircuitBreaker fileOperationsCircuitBreaker(CircuitBreakerRegistry registry) {
        var config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(15)
            .minimumNumberOfCalls(6)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class, SecurityException.class)
            .build();
            
        return registry.circuitBreaker("file-operations", config);
    }
    
    /**
     * Network Operations Circuit Breaker
     * For all network-dependent operations
     */
    @Bean
    public CircuitBreaker networkOperationsCircuitBreaker(CircuitBreakerRegistry registry) {
        var config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(12)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(45.0f)
            .waitDurationInOpenState(Duration.ofSeconds(25))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(Exception.class, TimeoutException.class)
            .ignoreExceptions(IllegalArgumentException.class, SecurityException.class)
            .build();
            
        CircuitBreaker circuitBreaker = registry.circuitBreaker("network-operations", config);
        
        // Add event listeners for monitoring
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.info("Network circuit breaker state transition: {} -> {} for {}", 
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState(),
                        event.getCircuitBreakerName()))
            .onFailureRateExceeded(event -> 
                log.warn("Network circuit breaker failure rate exceeded: {}% for {}", 
                        event.getFailureRate(), 
                        event.getCircuitBreakerName()))
            .onCallNotPermitted(event -> 
                log.warn("Network circuit breaker call not permitted for {}", 
                        event.getCircuitBreakerName()));
                        
        return circuitBreaker;
    }
}