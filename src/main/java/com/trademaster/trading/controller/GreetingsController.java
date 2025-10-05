package com.trademaster.trading.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Greetings Controller for testing API key configuration
 * Used to verify Kong Gateway authentication is working properly
 *
 * MANDATORY: Golden Specification - Kong API Gateway Integration
 * MANDATORY: Rule #6 - Zero Trust Security (Internal API endpoint)
 */
@RestController
@RequestMapping("/internal/v1/greetings")
@Slf4j
public class GreetingsController {

    /**
     * Simple greeting endpoint for testing internal API key authentication
     * This endpoint should be protected by Kong API key authentication
     */
    @GetMapping("/hello")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> hello() {
        log.debug("Greetings endpoint accessed successfully");

        Map<String, Object> response = Map.of(
            "message", "Hello from TradeMaster Trading Service!",
            "service", "trading-service",
            "timestamp", LocalDateTime.now(),
            "status", "API key authentication working",
            "version", "1.0.0"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for internal monitoring
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("Internal health check accessed");

        Map<String, Object> response = Map.of(
            "status", "UP",
            "service", "trading-service",
            "timestamp", LocalDateTime.now(),
            "authentication", "API key verified"
        );

        return ResponseEntity.ok(response);
    }
}
