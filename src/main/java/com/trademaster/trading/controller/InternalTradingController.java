package com.trademaster.trading.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Internal Trading API Controller - Kong Dynamic API Key Testing
 * 
 * Provides internal endpoints for service-to-service communication.
 * These endpoints use Kong dynamic API key authentication via ServiceApiKeyFilter.
 * 
 * Security:
 * - Kong API key authentication required
 * - Role-based access control (ROLE_SERVICE)
 * - Internal network access only
 * - Audit logging for all operations
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Kong Integration)
 */
@RestController
@RequestMapping("/api/internal")
@Slf4j
public class InternalTradingController {
    
    /**
     * Internal greeting endpoint for testing Kong API key authentication
     * Requires SERVICE role authentication through Kong dynamic keys
     */
    @GetMapping("/greeting")
    @PreAuthorize("hasRole('SERVICE')")
    public Map<String, Object> getGreeting() {
        log.info("Internal greeting endpoint accessed");
        
        return Map.of(
            "message", "Hello from Trading Service Internal API!",
            "timestamp", LocalDateTime.now(),
            "service", "trading-service",
            "authenticated", true,
            "role", "SERVICE"
        );
    }
    
    /**
     * Internal status endpoint for service health validation
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('SERVICE')")
    public Map<String, Object> getStatus() {
        log.info("Internal status endpoint accessed");
        
        return Map.of(
            "status", "UP",
            "service", "trading-service",
            "timestamp", LocalDateTime.now(),
            "authenticated", true,
            "message", "Trading service is running and authenticated"
        );
    }
    
    /**
     * Health check for internal services (public endpoint for testing)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "service", "trading-service",
            "status", "UP",
            "internal_api", "available",
            "timestamp", LocalDateTime.now()
        ));
    }
}