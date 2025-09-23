package com.trademaster.trading.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Internal Service Client
 * 
 * Example client for making internal service-to-service calls with API key authentication.
 * This demonstrates how other services should call the internal trading API.
 * 
 * Usage Example:
 * - broker-auth-service can call trading-service internal APIs
 * - portfolio-service can get position data
 * - risk-service can get user order history
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class InternalServiceClient {
    
    private final RestTemplate restTemplate;
    private final RestTemplate circuitBreakerRestTemplate;
    
    @Value("${trademaster.security.service.api-key}")
    private String serviceApiKey;
    
    @Value("${trademaster.service.name:trading-service}")
    private String serviceName;
    
    /**
     * ✅ CONSTRUCTOR INJECTION: Use configured RestTemplate with connection pooling
     * Primary RestTemplate has connection pooling configured via HttpClientConfiguration
     * Circuit breaker RestTemplate provides additional resilience for broker API calls
     */
    public InternalServiceClient(RestTemplate restTemplate, 
                               @Qualifier("circuitBreakerRestTemplate") RestTemplate circuitBreakerRestTemplate) {
        this.restTemplate = restTemplate;
        this.circuitBreakerRestTemplate = circuitBreakerRestTemplate;
        log.info("✅ Trading Service InternalServiceClient initialized with connection pooling");
    }
    
    /**
     * Example: Call another service's internal API
     * This shows how broker-auth-service would call trading-service
     */
    public Map<String, Object> callTradingServiceInternal(String endpoint, Object payload) {
        try {
            String url = "http://trading-service:8083/api/internal/v1/trading" + endpoint;
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
            
            log.info("Calling internal API: {} with headers: {}", url, headers.keySet());
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class);
            
            log.info("Internal API call successful: {}", response.getStatusCode());
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("Internal API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Internal service call failed", e);
        }
    }
    
    /**
     * Example: Get user positions from trading service
     */
    public Map<String, Object> getUserPositions(String userId) {
        try {
            String url = "http://trading-service:8083/api/internal/v1/trading/positions/" + userId;
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);
            
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("Failed to get user positions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user positions", e);
        }
    }
    
    /**
     * Create headers for service-to-service authentication
     */
    private HttpHeaders createServiceHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-API-Key", serviceApiKey);
        headers.set("X-Service-ID", serviceName);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        
        return headers;
    }
    
    /**
     * Health check for internal service connectivity
     */
    public boolean checkServiceHealth(String serviceUrl) {
        try {
            String url = serviceUrl + "/api/internal/v1/trading/health";
            
            HttpHeaders headers = createServiceHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class);
            
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.error("Service health check failed for: {}", serviceUrl, e);
            return false;
        }
    }
}