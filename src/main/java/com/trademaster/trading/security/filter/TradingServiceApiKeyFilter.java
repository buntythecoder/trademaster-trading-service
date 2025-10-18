package com.trademaster.trading.security.filter;

import com.trademaster.common.properties.CommonServiceProperties;
import com.trademaster.common.security.filter.AbstractServiceApiKeyFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Trading Service API Key Authentication Filter
 *
 * MANDATORY: Rule #3 - Functional Programming (no if-else, pattern matching)
 * MANDATORY: Rule #6 - Zero Trust Security Policy (Tiered Security)
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #14 - Pattern Matching Excellence
 * MANDATORY: Rule #19 - Access Control & Encapsulation
 *
 * Extends AbstractServiceApiKeyFilter from common library to provide
 * trading-service-specific API key authentication with Kong integration.
 *
 * Features:
 * - Kong consumer header validation (X-Consumer-ID, X-Consumer-Username)
 * - Dynamic API key authentication for internal service-to-service calls
 * - Trading-specific authority assignment (ROLE_TRADING_SERVICE)
 * - Request context logging with correlation IDs
 * - Functional authentication pipeline with pattern matching
 * - Audit logging for security compliance
 *
 * Security Architecture:
 * - External API Calls: SecurityFacade + SecurityMediator (full security stack)
 * - Internal Service-to-Service: This filter provides lightweight authentication
 * - Zero Trust: Validate all internal API requests, fail-secure by default
 *
 * Configuration in application.yml:
 * <pre>
 * trademaster:
 *   security:
 *     service:
 *       enabled: true
 *       api-key: ${SERVICE_API_KEY}
 *
 * trademaster-common:
 *   security-filter:
 *     enabled: true
 *     excluded-paths: /actuator/**,/api-docs/**,/swagger-ui/**
 * </pre>
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class TradingServiceApiKeyFilter extends AbstractServiceApiKeyFilter {

    /**
     * Constructor injection for CommonServiceProperties (Rule #2 SOLID - DIP)
     */
    public TradingServiceApiKeyFilter(CommonServiceProperties properties) {
        super(properties);
        log.info("✅ TradingServiceApiKeyFilter initialized with Zero Trust security policy");
        log.info("   Internal API paths protected: {}", properties.getSecurity().getInternalPaths());
        log.info("   Kong integration: {}", properties.getKong().getHeaders().getConsumerId());
    }

    /**
     * Override to add trading-service-specific request context
     * Rule #9: Immutable records for context
     */
    @Override
    protected RequestContext createRequestContext(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 jakarta.servlet.FilterChain chain) {
        RequestContext context = super.createRequestContext(request, response, chain);

        // Log trading-specific context (correlation ID for audit trail)
        log.debug("📊 Trading request context: path={}, method={}, remote={}",
                 context.requestPath(),
                 request.getMethod(),
                 request.getRemoteAddr());

        return context;
    }

    /**
     * Override to customize internal API request filtering for trading service
     * Rule #3: Functional predicate, no if-else statements
     */
    @Override
    protected boolean isInternalApiRequest(RequestContext context) {
        boolean isInternal = super.isInternalApiRequest(context);

        // Pattern matching for trading-specific internal paths
        boolean isTradingInternal = context.requestPath().startsWith("/api/internal/") ||
                                   context.requestPath().startsWith("/api/v1/internal/");

        return isInternal || isTradingInternal;
    }

    /**
     * Override to add trading-service-specific authorities
     * Rule #3: Functional composition with service-specific authorities
     */
    @Override
    protected void setServiceAuthentication(String serviceId, List<SimpleGrantedAuthority> authorities) {
        // Create immutable authority list with trading-specific roles
        List<SimpleGrantedAuthority> tradingAuthorities = createTradingServiceAuthorities(serviceId);

        super.setServiceAuthentication(serviceId, tradingAuthorities);

        log.info("🔐 Trading service authentication set: serviceId={}, authorities={}",
                serviceId, tradingAuthorities.stream()
                    .map(SimpleGrantedAuthority::getAuthority)
                    .toList());
    }

    /**
     * Create trading-service-specific authorities based on service ID
     * Rule #3: Functional authority creation, Rule #14: Pattern matching
     */
    private List<SimpleGrantedAuthority> createTradingServiceAuthorities(String serviceId) {
        return switch (serviceId) {
            // Kong-validated services get full trading access
            case String s when s.contains("portfolio-service") -> List.of(
                new SimpleGrantedAuthority("ROLE_SERVICE"),
                new SimpleGrantedAuthority("ROLE_INTERNAL"),
                new SimpleGrantedAuthority("ROLE_TRADING_SERVICE"),
                new SimpleGrantedAuthority("ROLE_PORTFOLIO_ACCESS")
            );

            // Broker auth service gets broker-specific access
            case String s when s.contains("broker-auth-service") -> List.of(
                new SimpleGrantedAuthority("ROLE_SERVICE"),
                new SimpleGrantedAuthority("ROLE_INTERNAL"),
                new SimpleGrantedAuthority("ROLE_TRADING_SERVICE"),
                new SimpleGrantedAuthority("ROLE_BROKER_ACCESS")
            );

            // Event bus service gets event publishing access
            case String s when s.contains("event-bus-service") -> List.of(
                new SimpleGrantedAuthority("ROLE_SERVICE"),
                new SimpleGrantedAuthority("ROLE_INTERNAL"),
                new SimpleGrantedAuthority("ROLE_TRADING_SERVICE"),
                new SimpleGrantedAuthority("ROLE_EVENT_PUBLISHER")
            );

            // Default service access with trading role
            default -> List.of(
                new SimpleGrantedAuthority("ROLE_SERVICE"),
                new SimpleGrantedAuthority("ROLE_INTERNAL"),
                new SimpleGrantedAuthority("ROLE_TRADING_SERVICE")
            );
        };
    }

    /**
     * Override to add trading-specific error response format
     * Rule #9: Immutable error response structure
     */
    @Override
    protected void sendUnauthorizedResponse(HttpServletResponse response, String message)
            throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        // Trading-specific error response format
        String errorJson = String.format(
            "{\"error\":\"TRADING_SERVICE_AUTHENTICATION_FAILED\"," +
            "\"message\":\"%s\"," +
            "\"timestamp\":%d," +
            "\"service\":\"trading-service\"," +
            "\"code\":\"AUTH_001\"}",
            message, System.currentTimeMillis());

        response.getWriter().write(errorJson);

        // Audit log for security compliance (Rule #15: Structured Logging)
        log.warn("🚨 Trading service authentication failed: message={}, timestamp={}",
                message, System.currentTimeMillis());
    }
}
