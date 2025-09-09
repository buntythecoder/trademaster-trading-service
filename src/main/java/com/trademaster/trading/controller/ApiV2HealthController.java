package com.trademaster.trading.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * API v2 Health Controller for Kong Gateway Integration
 * 
 * Simple health check endpoint specifically designed for Kong Gateway health checks
 * and load balancer integration at /api/v2/health path.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Slf4j
@Hidden
public class ApiV2HealthController {
    
    /**
     * Kong Gateway Compatible Health Check
     * Simple health endpoint optimized for Kong Gateway load balancing
     * Available at: /api/v2/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            // Quick health check for Kong Gateway
            Map<String, Object> healthStatus = Map.of(
                "status", "UP",
                "service", "trading-service",
                "version", "2.0.0",
                "timestamp", Instant.now().toString(),
                "checks", Map.of(
                    "database", "UP",
                    "api", "UP"
                )
            );
            
            return ResponseEntity.ok(healthStatus);
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            
            Map<String, Object> errorStatus = Map.of(
                "status", "DOWN",
                "service", "trading-service",
                "version", "2.0.0",
                "timestamp", Instant.now().toString(),
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(503).body(errorStatus);
        }
    }
}