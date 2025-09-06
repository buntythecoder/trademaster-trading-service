package com.trademaster.trading.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Enhanced Security Hardening Configuration
 * 
 * Implements advanced security measures for production-grade trading service:
 * - Request/response filtering and validation
 * - Security headers enforcement
 * - Audit logging for security events
 * - HTTP firewall configuration
 * - Rate limiting and DOS protection
 * - Input sanitization and validation
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@Configuration
@Slf4j
public class SecurityHardeningConfig {
    
    /**
     * Strong password encoder for any password hashing needs
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strong cost factor for production
    }
    
    /**
     * HTTP Firewall - Enhanced security filtering
     */
    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        
        // Configure strict HTTP firewall rules
        firewall.setAllowUrlEncodedSlash(false);
        firewall.setAllowUrlEncodedPercent(false);
        firewall.setAllowUrlEncodedPeriod(false);
        firewall.setAllowBackSlash(false);
        firewall.setAllowNull(false);
        firewall.setAllowSemicolon(false);
        
        // Allowed HTTP methods (restrict to only what we need)
        firewall.setAllowedHttpMethods(Set.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"
        ));
        
        log.info("Configured strict HTTP firewall with enhanced security rules");
        return firewall;
    }
    
    /**
     * Security audit event repository
     */
    @Bean
    public AuditEventRepository auditEventRepository() {
        // In production, this should be a persistent repository
        return new InMemoryAuditEventRepository(1000);
    }
    
    /**
     * Request logging filter for security audit trails
     */
    @Bean
    public FilterRegistrationBean<CommonsRequestLoggingFilter> logFilter() {
        FilterRegistrationBean<CommonsRequestLoggingFilter> registrationBean = 
            new FilterRegistrationBean<>();
        
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeHeaders(false); // Don't log sensitive headers
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(false); // Don't log request body (may contain sensitive data)
        filter.setMaxPayloadLength(1000);
        filter.setIncludeClientInfo(true);
        filter.setBeforeMessagePrefix("REQUEST START: ");
        filter.setAfterMessagePrefix("REQUEST END: ");
        
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        
        return registrationBean;
    }
    
    /**
     * Security headers enforcement filter
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = 
            new FilterRegistrationBean<>();
        
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        
        return registrationBean;
    }
    
    /**
     * Input validation and sanitization filter
     */
    @Bean
    public FilterRegistrationBean<InputValidationFilter> inputValidationFilter() {
        FilterRegistrationBean<InputValidationFilter> registrationBean = 
            new FilterRegistrationBean<>();
        
        registrationBean.setFilter(new InputValidationFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(2);
        
        return registrationBean;
    }
    
    /**
     * Custom security headers filter implementation
     */
    public static class SecurityHeadersFilter extends OncePerRequestFilter {
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      FilterChain filterChain) 
                throws ServletException, IOException {
            
            // Add comprehensive security headers
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Permissions-Policy", 
                "geolocation=(), microphone=(), camera=(), payment=(), usb=()");
            
            // Content Security Policy for API responses
            if (request.getRequestURI().startsWith("/api/")) {
                response.setHeader("Content-Security-Policy", 
                    "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
            }
            
            // Cache control for sensitive endpoints
            if (request.getRequestURI().contains("/orders") || 
                request.getRequestURI().contains("/portfolio")) {
                response.setHeader("Cache-Control", 
                    "no-cache, no-store, must-revalidate, private");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
            
            // Server header obfuscation
            response.setHeader("Server", "TradeMaster/2.0");
            
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * Input validation and sanitization filter
     */
    public static class InputValidationFilter extends OncePerRequestFilter {
        
        // Dangerous patterns to detect and block
        private static final String[] DANGEROUS_PATTERNS = {
            "<script", "javascript:", "data:text/html", "vbscript:",
            "onload=", "onerror=", "onclick=", "onmouseover=",
            "eval(", "alert(", "confirm(", "prompt(",
            "document.cookie", "document.write", "innerHTML",
            "../", "..\\", "/etc/passwd", "/proc/", "cmd.exe",
            "powershell", "bash", "sh", "/bin/",
            "DROP TABLE", "DELETE FROM", "INSERT INTO", "UPDATE SET",
            "UNION SELECT", "' OR '", "' AND '", "/*", "*/"
        };
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      FilterChain filterChain) 
                throws ServletException, IOException {
            
            // Validate request parameters
            if (containsSuspiciousContent(request)) {
                log.warn("Suspicious request blocked from IP: {} - URI: {} - User-Agent: {}", 
                    getClientIP(request), request.getRequestURI(), 
                    request.getHeader("User-Agent"));
                
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"INVALID_REQUEST\",\"message\":\"Request contains invalid content\"}");
                return;
            }
            
            // Validate Content-Type for POST/PUT requests
            if (("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) &&
                request.getRequestURI().startsWith("/api/")) {
                
                String contentType = request.getContentType();
                if (contentType == null || !contentType.startsWith("application/json")) {
                    log.warn("Invalid content-type from IP: {} - Content-Type: {}", 
                        getClientIP(request), contentType);
                    
                    response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                    response.getWriter().write("{\"error\":\"INVALID_CONTENT_TYPE\",\"message\":\"Only application/json is supported\"}");
                    return;
                }
            }
            
            // Check request size limits
            if (request.getContentLengthLong() > 1024 * 1024) { // 1MB limit
                log.warn("Request too large from IP: {} - Size: {} bytes", 
                    getClientIP(request), request.getContentLengthLong());
                
                response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                response.getWriter().write("{\"error\":\"REQUEST_TOO_LARGE\",\"message\":\"Request exceeds maximum size limit\"}");
                return;
            }
            
            filterChain.doFilter(request, response);
        }
        
        private boolean containsSuspiciousContent(HttpServletRequest request) {
            // Check query parameters
            if (request.getQueryString() != null) {
                String query = request.getQueryString().toLowerCase();
                for (String pattern : DANGEROUS_PATTERNS) {
                    if (query.contains(pattern.toLowerCase())) {
                        return true;
                    }
                }
            }
            
            // Check headers
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                String lowerUserAgent = userAgent.toLowerCase();
                for (String pattern : DANGEROUS_PATTERNS) {
                    if (lowerUserAgent.contains(pattern.toLowerCase())) {
                        return true;
                    }
                }
            }
            
            // Check for SQL injection patterns in parameters
            request.getParameterNames().asIterator().forEachRemaining(paramName -> {
                String paramValue = request.getParameter(paramName);
                if (paramValue != null) {
                    String lowerValue = paramValue.toLowerCase();
                    for (String pattern : DANGEROUS_PATTERNS) {
                        if (lowerValue.contains(pattern.toLowerCase())) {
                            log.warn("Suspicious parameter detected: {} = {}", paramName, paramValue);
                        }
                    }
                }
            });
            
            return false;
        }
        
        private String getClientIP(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIP = request.getHeader("X-Real-IP");
            if (xRealIP != null && !xRealIP.isEmpty()) {
                return xRealIP;
            }
            
            return request.getRemoteAddr();
        }
    }
    
    /**
     * Rate limiting configuration (in production, use Redis-based implementation)
     */
    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter() {
        FilterRegistrationBean<RateLimitingFilter> registrationBean = 
            new FilterRegistrationBean<>();
        
        registrationBean.setFilter(new RateLimitingFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(0); // Highest priority
        
        return registrationBean;
    }
    
    /**
     * Simple in-memory rate limiting filter (use Redis for production)
     */
    public static class RateLimitingFilter extends OncePerRequestFilter {
        
        // Simple in-memory rate limiting (replace with Redis for production)
        private final java.util.Map<String, java.util.concurrent.atomic.AtomicInteger> requestCounts = 
            new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.Map<String, Long> lastResetTime = 
            new java.util.concurrent.ConcurrentHashMap<>();
        
        private static final int MAX_REQUESTS_PER_MINUTE = 1000; // Per IP
        private static final long WINDOW_SIZE_MS = 60_000; // 1 minute
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      FilterChain filterChain) 
                throws ServletException, IOException {
            
            String clientIP = getClientIP(request);
            long currentTime = System.currentTimeMillis();
            
            // Reset counters if window has expired
            Long lastReset = lastResetTime.get(clientIP);
            if (lastReset == null || (currentTime - lastReset) > WINDOW_SIZE_MS) {
                requestCounts.put(clientIP, new java.util.concurrent.atomic.AtomicInteger(0));
                lastResetTime.put(clientIP, currentTime);
            }
            
            // Check rate limit
            java.util.concurrent.atomic.AtomicInteger count = requestCounts.get(clientIP);
            if (count != null && count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {} - Count: {}", clientIP, count.get());
                
                response.setStatus(429); // Too Many Requests
                response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
                response.setHeader("X-RateLimit-Remaining", "0");
                response.setHeader("X-RateLimit-Reset", String.valueOf((lastResetTime.get(clientIP) + WINDOW_SIZE_MS) / 1000));
                response.getWriter().write("{\"error\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}");
                return;
            }
            
            // Add rate limit headers
            if (count != null) {
                response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
                response.setHeader("X-RateLimit-Remaining", 
                    String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - count.get())));
                response.setHeader("X-RateLimit-Reset", 
                    String.valueOf((lastResetTime.get(clientIP) + WINDOW_SIZE_MS) / 1000));
            }
            
            filterChain.doFilter(request, response);
        }
        
        private String getClientIP(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
    }
}