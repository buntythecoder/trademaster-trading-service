package com.trademaster.trading.circuit;

import com.trademaster.trading.common.Result;
import com.trademaster.trading.common.TradeError;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Circuit Breaker Service for Trading Operations
 * 
 * Provides functional circuit breaker protection for all trading service operations.
 * Integrates with Result<T, TradeError> pattern for consistent error handling
 * and railway programming patterns.
 * 
 * Key Features:
 * - Functional composition with Result types
 * - Domain-specific circuit breakers for different operation types
 * - Automatic fallback mechanisms and graceful degradation
 * - Async operation support with CompletableFuture
 * - Comprehensive error mapping and monitoring
 * 
 * Circuit Breaker Types:
 * - Order Processing: Critical path operations (place, modify, cancel orders)
 * - Risk Management: Risk validation and portfolio analytics
 * - Broker Integration: External broker API calls
 * - Portfolio Service: Portfolio calculations and updates
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0 (Java 24 + Resilience4j)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerService {
    
    private final CircuitBreaker orderProcessingCircuitBreaker;
    private final CircuitBreaker riskManagementCircuitBreaker;
    private final CircuitBreaker brokerIntegrationCircuitBreaker;
    private final CircuitBreaker portfolioServiceCircuitBreaker;
    
    /**
     * Execute order processing operation with circuit breaker protection
     * 
     * @param operation The operation to execute
     * @param fallback Fallback operation if circuit breaker is open
     * @return Result containing operation result or circuit breaker error
     */
    public <T> Result<T, TradeError> executeOrderOperation(
            Supplier<Result<T, TradeError>> operation,
            Supplier<Result<T, TradeError>> fallback) {
        
        return executeWithCircuitBreaker(
            orderProcessingCircuitBreaker,
            operation,
            fallback,
            "Order processing"
        );
    }
    
    /**
     * Execute order processing operation with circuit breaker (no fallback)
     * 
     * @param operation The operation to execute
     * @return Result containing operation result or circuit breaker error
     */
    public <T> Result<T, TradeError> executeOrderOperation(
            Supplier<Result<T, TradeError>> operation) {
        
        return executeOrderOperation(
            operation,
            () -> Result.failure(new TradeError.SystemError.ServiceUnavailable("Order processing"))
        );
    }
    
    /**
     * Execute risk management operation with circuit breaker protection
     * 
     * @param operation The operation to execute
     * @param fallback Fallback operation if circuit breaker is open
     * @return Result containing operation result or circuit breaker error
     */
    public <T> Result<T, TradeError> executeRiskOperation(
            Supplier<Result<T, TradeError>> operation,
            Supplier<Result<T, TradeError>> fallback) {
        
        return executeWithCircuitBreaker(
            riskManagementCircuitBreaker,
            operation,
            fallback,
            "Risk management"
        );
    }
    
    /**
     * Execute risk management operation with circuit breaker (no fallback)
     * 
     * @param operation The operation to execute
     * @return Result containing operation result or circuit breaker error
     */
    public <T> Result<T, TradeError> executeRiskOperation(
            Supplier<Result<T, TradeError>> operation) {
        
        return executeRiskOperation(
            operation,
            () -> Result.failure(new TradeError.SystemError.ServiceUnavailable("Risk management"))
        );
    }
    
    /**
     * Execute broker integration operation with circuit breaker protection
     * 
     * @param operation The operation to execute
     * @param fallback Fallback operation if circuit breaker is open
     * @return Result containing operation result or circuit breaker error
     */
    public <T> Result<T, TradeError> executeBrokerOperation(
            Supplier<Result<T, TradeError>> operation,
            Supplier<Result<T, TradeError>> fallback) {
        
        return executeWithCircuitBreaker(
            brokerIntegrationCircuitBreaker,
            operation,
            fallback,
            "Broker integration"
        );
    }
    
    /**
     * Execute broker integration operation with circuit breaker (no fallback)
     * 
     * @param operation The operation to execute
     * @return Result containing operation result or circuit breaker error
     */
    public <T> Result<T, TradeError> executeBrokerOperation(
            Supplier<Result<T, TradeError>> operation) {
        
        return executeBrokerOperation(
            operation,
            () -> Result.failure(new TradeError.ExecutionError.VenueUnavailable("Broker service"))
        );
    }
    
    /**
     * Execute portfolio service operation with circuit breaker protection
     * 
     * @param operation The operation to execute
     * @param fallback Fallback operation if circuit breaker is open
     * @return Result containing operation result or circuit breaker error
     */
    public <T> Result<T, TradeError> executePortfolioOperation(
            Supplier<Result<T, TradeError>> operation,
            Supplier<Result<T, TradeError>> fallback) {
        
        return executeWithCircuitBreaker(
            portfolioServiceCircuitBreaker,
            operation,
            fallback,
            "Portfolio service"
        );
    }
    
    /**
     * Execute portfolio service operation with circuit breaker (no fallback)
     * 
     * @param operation The operation to execute
     * @return Result containing operation result or circuit breaker error
     */
    public <T> Result<T, TradeError> executePortfolioOperation(
            Supplier<Result<T, TradeError>> operation) {
        
        return executePortfolioOperation(
            operation,
            () -> Result.failure(new TradeError.SystemError.ServiceUnavailable("Portfolio service"))
        );
    }
    
    /**
     * Execute async order processing operation with circuit breaker protection
     * 
     * @param operation The async operation to execute
     * @return CompletableFuture containing Result with operation result or circuit breaker error
     */
    public <T> CompletableFuture<Result<T, TradeError>> executeOrderOperationAsync(
            Supplier<CompletableFuture<Result<T, TradeError>>> operation) {
        
        return executeAsyncWithCircuitBreaker(
            orderProcessingCircuitBreaker,
            operation,
            "Order processing async"
        );
    }
    
    /**
     * Execute async broker integration operation with circuit breaker protection
     * 
     * @param operation The async operation to execute
     * @return CompletableFuture containing Result with operation result or circuit breaker error
     */
    public <T> CompletableFuture<Result<T, TradeError>> executeBrokerOperationAsync(
            Supplier<CompletableFuture<Result<T, TradeError>>> operation) {
        
        return executeAsyncWithCircuitBreaker(
            brokerIntegrationCircuitBreaker,
            operation,
            "Broker integration async"
        );
    }
    
    /**
     * Get circuit breaker health status for monitoring
     * 
     * @return CircuitBreakerHealthStatus containing all circuit breaker states
     */
    public CircuitBreakerHealthStatus getHealthStatus() {
        return new CircuitBreakerHealthStatus(
            orderProcessingCircuitBreaker.getState(),
            riskManagementCircuitBreaker.getState(),
            brokerIntegrationCircuitBreaker.getState(),
            portfolioServiceCircuitBreaker.getState(),
            orderProcessingCircuitBreaker.getMetrics(),
            riskManagementCircuitBreaker.getMetrics(),
            brokerIntegrationCircuitBreaker.getMetrics(),
            portfolioServiceCircuitBreaker.getMetrics()
        );
    }
    
    // Private helper methods
    
    private <T> Result<T, TradeError> executeWithCircuitBreaker(
            CircuitBreaker circuitBreaker,
            Supplier<Result<T, TradeError>> operation,
            Supplier<Result<T, TradeError>> fallback,
            String operationName) {
        
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    Result<T, TradeError> result = operation.get();
                    
                    // If the result is a failure, throw an exception to trigger circuit breaker
                    if (result.isFailure()) {
                        TradeError error = result.getError().orElseThrow();
                        throw new CircuitBreakerException(error.getMessage(), new RuntimeException(error.getMessage()));
                    }
                    
                    return result;
                } catch (Exception e) {
                    log.error("{} operation failed: {}", operationName, e.getMessage());
                    throw new CircuitBreakerException(e.getMessage(), e);
                }
            });
            
        } catch (CallNotPermittedException e) {
            log.warn("{} circuit breaker is OPEN, executing fallback", operationName);
            return fallback.get();
            
        } catch (CircuitBreakerException e) {
            // Circuit breaker exception - wrap as system error
            return Result.failure(new TradeError.SystemError.UnexpectedError(
                operationName + " failed: " + e.getMessage()));
                
        } catch (Exception e) {
            log.error("Unexpected error in {} circuit breaker: {}", operationName, e.getMessage());
            return Result.failure(new TradeError.SystemError.UnexpectedError(
                operationName + " circuit breaker error: " + e.getMessage()));
        }
    }
    
    private <T> CompletableFuture<Result<T, TradeError>> executeAsyncWithCircuitBreaker(
            CircuitBreaker circuitBreaker,
            Supplier<CompletableFuture<Result<T, TradeError>>> operation,
            String operationName) {
        
        try {
            return circuitBreaker.executeCompletionStage(() -> 
                operation.get().thenApply(result -> {
                    // If the result is a failure, complete exceptionally to trigger circuit breaker
                    if (result.isFailure()) {
                        TradeError error = result.getError().orElseThrow();
                        CompletableFuture<Result<T, TradeError>> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(new CircuitBreakerException(error.getMessage(), new RuntimeException(error.getMessage())));
                        return failedFuture.join();
                    }
                    return result;
                })).toCompletableFuture();
                
        } catch (CallNotPermittedException e) {
            log.warn("{} circuit breaker is OPEN, returning service unavailable", operationName);
            return CompletableFuture.completedFuture(
                Result.failure(new TradeError.SystemError.ServiceUnavailable(operationName)));
                
        } catch (Exception e) {
            log.error("Unexpected error in {} async circuit breaker: {}", operationName, e.getMessage());
            return CompletableFuture.completedFuture(
                Result.failure(new TradeError.SystemError.UnexpectedError(
                    operationName + " async circuit breaker error: " + e.getMessage())));
        }
    }
    
    // Custom exception for internal circuit breaker logic
    private static class CircuitBreakerException extends RuntimeException {
        public CircuitBreakerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    // Health status record for monitoring
    public record CircuitBreakerHealthStatus(
        CircuitBreaker.State orderProcessingState,
        CircuitBreaker.State riskManagementState,
        CircuitBreaker.State brokerIntegrationState,
        CircuitBreaker.State portfolioServiceState,
        CircuitBreaker.Metrics orderProcessingMetrics,
        CircuitBreaker.Metrics riskManagementMetrics,
        CircuitBreaker.Metrics brokerIntegrationMetrics,
        CircuitBreaker.Metrics portfolioServiceMetrics
    ) {}
}