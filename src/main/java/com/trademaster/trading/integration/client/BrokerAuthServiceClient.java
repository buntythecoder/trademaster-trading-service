package com.trademaster.trading.integration.client;

import com.trademaster.common.functional.Result;
import com.trademaster.common.integration.client.AbstractInternalServiceClient;
import com.trademaster.common.properties.CommonServiceProperties;
import com.trademaster.trading.dto.integration.BrokerConnection;
import com.trademaster.trading.dto.integration.BrokerToken;
import com.trademaster.trading.dto.integration.TokenRefreshResult;
import com.trademaster.trading.error.ServiceError;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Broker Auth Service Client
 *
 * MANDATORY: Rule #3 - Functional Programming (Result monad, no if-else)
 * MANDATORY: Rule #6 - Zero Trust Security (Internal service-to-service)
 * MANDATORY: Rule #11 - Error Handling Patterns (Result monad)
 * MANDATORY: Rule #25 - Circuit Breaker Implementation
 *
 * Client for broker-auth-service integration with circuit breaker protection.
 * Handles broker connections, token validation, and token refresh operations.
 *
 * Features:
 * - Circuit breaker on all external calls
 * - Functional error handling with Result monad
 * - Token expiration handling with automatic refresh
 * - Graceful degradation with cached tokens
 * - Correlation ID propagation
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class BrokerAuthServiceClient extends AbstractInternalServiceClient {

    private static final String SERVICE_NAME = "broker-auth-service";
    private static final String CB_NAME = "broker-auth-service";

    public BrokerAuthServiceClient(RestTemplate restTemplate,
                                   CommonServiceProperties properties) {
        super(restTemplate, properties);
    }

    /**
     * Get broker connection for user
     * Rule #25: Circuit breaker protection for external service calls
     * Rule #11: Functional error handling with Result monad
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "getBrokerConnectionFallback")
    public Result<BrokerConnection, ServiceError> getBrokerConnection(Long userId, String brokerName) {
        log.info("Fetching broker connection for user {} broker {}", userId, brokerName);

        Map<String, Object> request = Map.of(
            "userId", userId,
            "brokerName", brokerName
        );

        return callService(SERVICE_NAME,
                "/api/internal/v1/broker/connection",
                HttpMethod.POST,
                request,
                BrokerConnection.class)
            .map(Result::<BrokerConnection, ServiceError>success)
            .orElseGet(() -> {
                String msg = String.format("Failed to fetch broker connection for user %d broker %s",
                                          userId, brokerName);
                log.error("Broker connection fetch failed: {}", msg);
                return Result.failure(new ServiceError.ServiceUnavailable(SERVICE_NAME, msg));
            });
    }

    /**
     * Fallback method for broker connection when circuit breaker is open
     * Rule #25: Graceful degradation with cached connection data
     */
    private Result<BrokerConnection, ServiceError> getBrokerConnectionFallback(
            Long userId, String brokerName, Exception ex) {

        log.warn("Broker auth service circuit breaker activated - using cached connection: {} {}",
                userId, brokerName, ex);

        // Return cached connection with expired token to trigger refresh
        BrokerConnection cachedConnection = new BrokerConnection(
            userId,
            brokerName,
            null,  // No client ID in cache
            BrokerConnection.ConnectionStatus.TOKEN_EXPIRED,
            null,  // No access token
            null,  // No refresh token
            LocalDateTime.now().minusHours(1),  // Expired timestamp
            false,  // Not active
            LocalDateTime.now().minusHours(1),
            Map.of("cached", "true", "circuit_breaker", "open")
        );

        return Result.success(cachedConnection);
    }

    /**
     * Validate broker token
     * Rule #25: Circuit breaker protection
     * Rule #11: Functional error handling with Result monad
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "validateBrokerTokenFallback")
    public Result<BrokerToken, ServiceError> validateBrokerToken(Long userId, String brokerName, String accessToken) {
        log.debug("Validating broker token for user {} broker {}", userId, brokerName);

        Map<String, Object> request = Map.of(
            "userId", userId,
            "brokerName", brokerName,
            "accessToken", accessToken
        );

        return callService(SERVICE_NAME,
                "/api/internal/v1/broker/token/validate",
                HttpMethod.POST,
                request,
                BrokerToken.class)
            .map(Result::<BrokerToken, ServiceError>success)
            .orElseGet(() -> {
                String msg = String.format("Failed to validate token for user %d broker %s",
                                          userId, brokerName);
                log.error("Token validation failed: {}", msg);
                return Result.failure(new ServiceError.ServiceUnavailable(SERVICE_NAME, msg));
            });
    }

    /**
     * Fallback method for token validation when circuit breaker is open
     * Rule #25: Conservative validation assuming token is expired
     */
    private Result<BrokerToken, ServiceError> validateBrokerTokenFallback(
            Long userId, String brokerName, String accessToken, Exception ex) {

        log.warn("Broker auth service circuit breaker activated - assuming token expired: {} {}",
                userId, brokerName, ex);

        // Return expired token to trigger refresh
        BrokerToken expiredToken = new BrokerToken(
            userId,
            brokerName,
            accessToken,
            BrokerToken.TokenStatus.EXPIRED,
            LocalDateTime.now().minusHours(1),
            false,  // Not valid
            LocalDateTime.now(),
            "Circuit breaker active - token validation unavailable"
        );

        return Result.success(expiredToken);
    }

    /**
     * Refresh broker token
     * Rule #25: Circuit breaker protection
     * Rule #11: Functional error handling with Result monad
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "refreshBrokerTokenFallback")
    public Result<TokenRefreshResult, ServiceError> refreshBrokerToken(
            Long userId, String brokerName, String refreshToken) {

        log.info("Refreshing broker token for user {} broker {}", userId, brokerName);

        Map<String, Object> request = Map.of(
            "userId", userId,
            "brokerName", brokerName,
            "refreshToken", refreshToken
        );

        return callService(SERVICE_NAME,
                "/api/internal/v1/broker/token/refresh",
                HttpMethod.POST,
                request,
                TokenRefreshResult.class)
            .map(Result::<TokenRefreshResult, ServiceError>success)
            .orElseGet(() -> {
                String msg = String.format("Failed to refresh token for user %d broker %s",
                                          userId, brokerName);
                log.error("Token refresh failed: {}", msg);
                return Result.failure(new ServiceError.ServiceUnavailable(SERVICE_NAME, msg));
            });
    }

    /**
     * Fallback method for token refresh when circuit breaker is open
     * Rule #25: Return failure to trigger re-authentication
     */
    private Result<TokenRefreshResult, ServiceError> refreshBrokerTokenFallback(
            Long userId, String brokerName, String refreshToken, Exception ex) {

        log.error("Broker auth service circuit breaker activated - token refresh failed: {} {}",
                userId, brokerName, ex);

        // Return failed refresh result requiring re-authentication
        TokenRefreshResult failedRefresh = new TokenRefreshResult(
            userId,
            brokerName,
            null,  // No new access token
            null,  // No new refresh token
            null,  // No expiry
            TokenRefreshResult.RefreshStatus.BROKER_ERROR,
            false,  // Not successful
            LocalDateTime.now(),
            "Circuit breaker active - broker auth service unavailable, re-authentication required"
        );

        return Result.success(failedRefresh);
    }

    /**
     * Submit order to broker via broker-auth-service
     * Rule #25: Circuit breaker protection
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "submitOrderToBrokerFallback")
    public Result<Map<String, Object>, ServiceError> submitOrderToBroker(
            String brokerName, Map<String, Object> orderData, String correlationId) {

        log.info("Submitting order to broker {} - correlationId: {}", brokerName, correlationId);

        Map<String, Object> request = Map.of(
            "brokerName", brokerName,
            "orderData", orderData,
            "correlationId", correlationId
        );

        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> responseType = (Class<Map<String, Object>>) (Class<?>) Map.class;

        return callService(SERVICE_NAME,
                "/api/internal/v1/broker/order/submit",
                HttpMethod.POST,
                request,
                responseType)
            .map(Result::<Map<String, Object>, ServiceError>success)
            .orElseGet(() -> {
                String msg = String.format("Failed to submit order to broker %s", brokerName);
                log.error("Order submission failed: {}", msg);
                return Result.failure(new ServiceError.ServiceUnavailable(SERVICE_NAME, msg));
            });
    }

    /**
     * Fallback method for order submission when circuit breaker is open
     * Rule #25: Return failure to prevent order placement
     */
    private Result<Map<String, Object>, ServiceError> submitOrderToBrokerFallback(
            String brokerName, Map<String, Object> orderData, String correlationId, Exception ex) {

        log.error("Broker auth service circuit breaker activated - order submission blocked: {}",
                correlationId, ex);

        return Result.failure(new ServiceError.CircuitBreakerOpen(
            SERVICE_NAME,
            "Broker auth service unavailable - order submission blocked for safety"
        ));
    }

    /**
     * Check broker auth service health
     */
    public boolean isHealthy() {
        return checkServiceHealth(SERVICE_NAME);
    }
}
