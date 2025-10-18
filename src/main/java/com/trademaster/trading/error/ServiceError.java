package com.trademaster.trading.error;

/**
 * Service Error
 *
 * MANDATORY: Rule #3 - Functional Programming (sealed interface for pattern matching)
 * MANDATORY: Rule #11 - Error Handling Patterns (Result monad with errors)
 * MANDATORY: Rule #14 - Pattern Matching Excellence
 *
 * Sealed interface for service integration errors.
 * Use with Result<T, ServiceError> for functional error handling.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface ServiceError
    permits ServiceError.ServiceUnavailable,
            ServiceError.CircuitBreakerOpen,
            ServiceError.TimeoutError,
            ServiceError.InvalidResponse,
            ServiceError.NotConfigured {

    /**
     * Service unavailable error (service is down or unreachable)
     */
    record ServiceUnavailable(String serviceName, String message) implements ServiceError {}

    /**
     * Circuit breaker is open (too many failures)
     */
    record CircuitBreakerOpen(String serviceName, String message) implements ServiceError {}

    /**
     * Service call timeout
     */
    record TimeoutError(String serviceName, long timeoutMs, String message) implements ServiceError {}

    /**
     * Invalid response from service (parse error, validation error)
     */
    record InvalidResponse(String serviceName, String message, String response) implements ServiceError {}

    /**
     * Service not configured in properties
     */
    record NotConfigured(String serviceName, String message) implements ServiceError {}

    /**
     * Get service name from error
     */
    default String getServiceName() {
        return switch (this) {
            case ServiceUnavailable e -> e.serviceName();
            case CircuitBreakerOpen e -> e.serviceName();
            case TimeoutError e -> e.serviceName();
            case InvalidResponse e -> e.serviceName();
            case NotConfigured e -> e.serviceName();
        };
    }

    /**
     * Get error message
     */
    default String getMessage() {
        return switch (this) {
            case ServiceUnavailable e -> e.message();
            case CircuitBreakerOpen e -> e.message();
            case TimeoutError e -> e.message();
            case InvalidResponse e -> e.message();
            case NotConfigured e -> e.message();
        };
    }

    /**
     * Check if error is recoverable (retry might succeed)
     */
    default boolean isRecoverable() {
        return switch (this) {
            case ServiceUnavailable _, TimeoutError _ -> true;
            case CircuitBreakerOpen _, InvalidResponse _, NotConfigured _ -> false;
        };
    }
}
