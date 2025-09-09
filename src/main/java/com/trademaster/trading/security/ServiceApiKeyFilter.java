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
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * Service API Key Authentication Filter
 * 
 * Provides authentication for service-to-service communication using API keys.
 * This filter runs before JWT authentication and handles internal service calls.
 * 
 * Security Features:
 * - API key validation for internal services
 * - Request path filtering (only /api/internal/*)
 * - Audit logging for service authentication
 * - Fail-safe authentication bypass for health checks
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Order(1) // Run before JWT filter
@Slf4j
public class ServiceApiKeyFilter implements Filter {
    
    private static final String API_KEY_HEADER = "X-Service-API-Key";
    private static final String SERVICE_ID_HEADER = "X-Service-ID";
    private static final String INTERNAL_API_PATH = "/api/internal/";
    
    @Value("${trademaster.security.service.api-key:}")
    private String masterServiceApiKey;
    
    @Value("${trademaster.security.service.enabled:true}")
    private boolean serviceAuthEnabled;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getRequestURI();
        
        // Only process internal API requests
        if (!requestPath.startsWith(INTERNAL_API_PATH)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Skip authentication if disabled (for local development)
        if (!serviceAuthEnabled) {
            log.warn("Service authentication is DISABLED - allowing internal API access");
            setServiceAuthentication("development-service");
            chain.doFilter(request, response);
            return;
        }
        
        String apiKey = httpRequest.getHeader(API_KEY_HEADER);
        String serviceId = httpRequest.getHeader(SERVICE_ID_HEADER);
        
        if (!StringUtils.hasText(apiKey)) {
            log.error("Missing API key for internal service request: {} from {}", 
                     requestPath, httpRequest.getRemoteAddr());
            sendUnauthorizedResponse(httpResponse, "Missing service API key");
            return;
        }
        
        if (!StringUtils.hasText(serviceId)) {
            log.error("Missing service ID for internal service request: {} from {}", 
                     requestPath, httpRequest.getRemoteAddr());
            sendUnauthorizedResponse(httpResponse, "Missing service ID");
            return;
        }
        
        // Validate API key
        if (!isValidServiceApiKey(apiKey, serviceId)) {
            log.error("Invalid API key for service '{}' requesting: {} from {}", 
                     serviceId, requestPath, httpRequest.getRemoteAddr());
            sendUnauthorizedResponse(httpResponse, "Invalid service credentials");
            return;
        }
        
        // Set service authentication in security context
        setServiceAuthentication(serviceId);
        
        log.info("Service authentication successful: {} accessing {}", serviceId, requestPath);
        
        chain.doFilter(request, response);
    }
    
    /**
     * Validate service API key
     */
    private boolean isValidServiceApiKey(String apiKey, String serviceId) {
        // For now, use master API key for all services
        // In production, you might have service-specific keys stored in database/vault
        if (!StringUtils.hasText(masterServiceApiKey)) {
            log.error("Master service API key not configured");
            return false;
        }
        
        // Simple validation - in production, use more sophisticated validation
        boolean isValid = masterServiceApiKey.equals(apiKey) && isKnownService(serviceId);
        
        if (!isValid) {
            log.error("API key validation failed for service: {}", serviceId);
        }
        
        return isValid;
    }
    
    /**
     * Check if service ID is in our known services list
     */
    private boolean isKnownService(String serviceId) {
        return List.of(
            "event-bus-service",
            "broker-auth-service", 
            "portfolio-service",
            "notification-service",
            "risk-service"
        ).contains(serviceId);
    }
    
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