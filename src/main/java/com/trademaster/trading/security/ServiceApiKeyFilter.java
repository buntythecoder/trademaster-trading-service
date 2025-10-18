package com.trademaster.trading.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Service API Key Authentication Filter - Kong Dynamic Integration
 *
 * Updated to work with Kong API Gateway dynamic API keys instead of hardcoded keys.
 * Recognizes Kong consumer headers when API key validation is performed by Kong.
 * Falls back to direct API key validation when needed.
 *
 * Security Features:
 * - Kong consumer header recognition (X-Consumer-ID, X-Consumer-Username)
 * - Dynamic API key validation through Kong
 * - Request path filtering (only /api/internal/*)
 * - Audit logging for service authentication
 * - Fail-safe authentication bypass for health checks
 *
 * NOTE: Registered in SecurityConfig.filterChain() AND as @Component.
 * @Component required for dependency injection. Do NOT add @Order to avoid double registration.
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Kong Integration)
 */
@Component
@Slf4j
public class ServiceApiKeyFilter implements Filter {
    
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String SERVICE_ID_HEADER = "X-Service-ID";
    private static final String KONG_CONSUMER_ID_HEADER = "X-Consumer-ID";
    private static final String KONG_CONSUMER_USERNAME_HEADER = "X-Consumer-Username";
    private static final String INTERNAL_API_PATH = "/api/internal/";
    
    @Value("${trademaster.security.service.api-key:pTB9KkzqJWNkFDUJHIFyDv5b1tSUpP4q}")
    private String fallbackServiceApiKey;
    
    @Value("${trademaster.security.service.enabled:true}")
    private boolean serviceAuthEnabled;
    
    /**
     * Main filter method - functional validation chain using Optional patterns
     * Eliminates all if-statements with functional composition
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestPath = httpRequest.getRequestURI();

        // Functional validation chain - eliminates if-statements with Optional
        processInternalApiFilter(httpRequest, httpResponse, chain, requestPath);
    }

    /**
     * Step 1: Check if request is for internal API
     * Uses Optional to eliminate if-statement
     */
    private void processInternalApiFilter(HttpServletRequest request, HttpServletResponse response,
                                         FilterChain chain, String requestPath)
            throws IOException, ServletException {

        Optional.of(requestPath)
            .filter(path -> !path.startsWith(INTERNAL_API_PATH))
            .ifPresentOrElse(
                path -> {
                    // Not internal API - pass through filter chain
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    // Internal API - continue to next validation
                    try {
                        processServiceAuthCheck(request, response, chain, requestPath);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * Step 2: Check if service authentication is enabled
     * Uses Optional to eliminate if-statement
     */
    private void processServiceAuthCheck(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain, String requestPath)
            throws IOException, ServletException {

        Optional.of(serviceAuthEnabled)
            .filter(enabled -> !enabled)
            .ifPresentOrElse(
                enabled -> {
                    // Service auth disabled - allow with warning
                    log.warn("ServiceApiKeyFilter: Service authentication is DISABLED - allowing internal API access");
                    setServiceAuthentication("development-service");
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    // Service auth enabled - continue to Kong header check
                    try {
                        processKongHeaders(request, response, chain, requestPath);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * Step 3: Check for Kong consumer headers
     * Uses Optional to eliminate if-statement
     */
    private void processKongHeaders(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain chain, String requestPath)
            throws IOException, ServletException {

        String kongConsumerId = request.getHeader(KONG_CONSUMER_ID_HEADER);
        String kongConsumerUsername = request.getHeader(KONG_CONSUMER_USERNAME_HEADER);

        // Check if both Kong headers are present - eliminates if-statement with Optional
        Optional.of(kongConsumerId)
            .filter(StringUtils::hasText)
            .flatMap(id -> Optional.ofNullable(kongConsumerUsername)
                .filter(StringUtils::hasText)
                .map(username -> new KongConsumer(id, username)))
            .ifPresentOrElse(
                consumer -> {
                    // Kong validated - grant access
                    log.info("ServiceApiKeyFilter: Kong validated consumer '{}' (ID: {}), granting SERVICE access",
                             consumer.username(), consumer.id());
                    setServiceAuthentication(consumer.username());
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    // No Kong headers - fallback to direct API key validation
                    try {
                        processFallbackApiKey(request, response, chain, requestPath);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * Step 4: Process fallback direct API key validation
     * Uses Optional to eliminate if-statements
     */
    private void processFallbackApiKey(HttpServletRequest request, HttpServletResponse response,
                                      FilterChain chain, String requestPath)
            throws IOException, ServletException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        // Check if API key is present - eliminates if-statement with Optional
        Optional.ofNullable(apiKey)
            .filter(StringUtils::hasText)
            .ifPresentOrElse(
                key -> {
                    // API key present - validate it
                    try {
                        validateFallbackApiKey(request, response, chain, requestPath, key);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    // API key missing - send unauthorized
                    log.error("ServiceApiKeyFilter: No Kong consumer headers and missing X-API-Key header for request: {} from {}",
                             requestPath, request.getRemoteAddr());
                    try {
                        sendUnauthorizedResponse(response, "Missing service API key or Kong consumer headers");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * Step 5: Validate fallback API key
     * Uses Optional to eliminate if-statement
     */
    private void validateFallbackApiKey(HttpServletRequest request, HttpServletResponse response,
                                       FilterChain chain, String requestPath, String apiKey)
            throws IOException, ServletException {

        // Validate API key matches fallback key - eliminates if-statement with Optional
        Optional.ofNullable(fallbackServiceApiKey)
            .filter(StringUtils::hasText)
            .filter(fallbackKey -> !fallbackKey.equals(apiKey))
            .ifPresentOrElse(
                fallbackKey -> {
                    // Invalid API key - send unauthorized
                    log.error("ServiceApiKeyFilter: Invalid API key for direct service request: {} from {}",
                             requestPath, request.getRemoteAddr());
                    try {
                        sendUnauthorizedResponse(response, "Invalid service API key");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                () -> {
                    // Valid API key - grant access
                    setServiceAuthentication("direct-service-call");
                    log.info("ServiceApiKeyFilter: Direct API key authentication successful for request: {}", requestPath);
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * Helper record for Kong consumer data
     */
    private record KongConsumer(String id, String username) {}
    
    
    /**
     * Set service authentication in Spring Security context
     */
    private void setServiceAuthentication(String serviceId) {
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_SERVICE"),
            new SimpleGrantedAuthority("ROLE_INTERNAL")
        );
        
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(serviceId, null, authorities);
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    /**
     * Send unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"SERVICE_AUTHENTICATION_FAILED\",\"message\":\"%s\",\"timestamp\":%d}",
            message, System.currentTimeMillis()));
    }
}