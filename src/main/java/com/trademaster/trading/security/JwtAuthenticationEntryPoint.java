package com.trademaster.trading.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * JWT Authentication Entry Point
 * 
 * Handles authentication failures for JWT-based requests.
 * Returns proper error responses for unauthorized access attempts.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        log.warn("Unauthorized access attempt to: {} - {}", request.getRequestURI(), authException.getMessage());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        Map<String, Object> errorResponse = Map.of(
            "timestamp", Instant.now().toString(),
            "status", 401,
            "error", "Unauthorized",
            "message", "Authentication required to access this resource",
            "path", request.getRequestURI()
        );
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}