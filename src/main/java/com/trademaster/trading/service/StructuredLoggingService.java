package com.trademaster.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Structured Logging Service
 * 
 * Centralized service for structured logging across the trading platform.
 * Provides consistent log formatting, correlation tracking, and compliance-ready audit trails.
 * 
 * Features:
 * - Correlation ID tracking across service boundaries
 * - Structured audit logging for regulatory compliance
 * - Performance metrics logging with timing
 * - Security event logging with context preservation
 * - Error logging with full context and stack traces
 * - Financial transaction logging with audit trails
 * 
 * Compliance:
 * - SOX compliance for financial transaction logging
 * - GDPR compliance for data processing logs
 * - Regulatory audit trail requirements
 * - Real-time security monitoring integration
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StructuredLoggingService {
    
    // Specialized loggers for different log types
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("com.trademaster.trading.audit");
    private static final Logger PERFORMANCE_LOGGER = LoggerFactory.getLogger("com.trademaster.trading.performance");
    private static final Logger ERROR_LOGGER = LoggerFactory.getLogger("com.trademaster.trading.error");
    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger("com.trademaster.trading.security");
    
    // MDC Keys for structured logging
    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_ID = "userId";
    private static final String OPERATION = "operation";
    private static final String METRIC = "metric";
    private static final String VALUE = "value";
    private static final String DURATION = "duration";
    private static final String ERROR_CODE = "errorCode";
    private static final String SECURITY_EVENT = "securityEvent";
    private static final String SOURCE_IP = "sourceIp";
    private static final String USER_AGENT = "userAgent";
    private static final String ORDER_ID = "orderId";
    private static final String SYMBOL = "symbol";
    private static final String AMOUNT = "amount";
    private static final String BROKER = "broker";
    
    /**
     * Log audit event for regulatory compliance
     */
    public void logAuditEvent(String correlationId, Long userId, String operation, String message) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            USER_ID, String.valueOf(userId),
            OPERATION, operation
        ), () -> AUDIT_LOGGER.info(message));
    }
    
    /**
     * Log financial transaction for audit trail
     */
    public void logFinancialTransaction(String correlationId, Long userId, String orderId, 
                                      String symbol, BigDecimal amount, String broker, String message) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            USER_ID, String.valueOf(userId),
            ORDER_ID, orderId,
            SYMBOL, symbol,
            AMOUNT, amount.toString(),
            BROKER, broker,
            OPERATION, "FINANCIAL_TRANSACTION"
        ), () -> AUDIT_LOGGER.info(message));
    }
    
    /**
     * Log order lifecycle event
     */
    public void logOrderEvent(String correlationId, Long userId, String orderId, 
                            String symbol, String orderStatus, String broker, String message) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            USER_ID, String.valueOf(userId),
            ORDER_ID, orderId,
            SYMBOL, symbol,
            BROKER, broker,
            OPERATION, "ORDER_" + orderStatus
        ), () -> AUDIT_LOGGER.info(message));
    }
    
    /**
     * Log performance metric
     */
    public void logPerformanceMetric(String correlationId, String metricName, 
                                   Object value, Duration duration, String message) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            METRIC, metricName,
            VALUE, String.valueOf(value),
            DURATION, String.valueOf(duration.toMillis())
        ), () -> PERFORMANCE_LOGGER.info(message));
    }
    
    /**
     * Log performance timing
     */
    public void logTiming(String correlationId, String operation, long startTime, String message) {
        long duration = System.currentTimeMillis() - startTime;
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            OPERATION, operation,
            DURATION, String.valueOf(duration)
        ), () -> PERFORMANCE_LOGGER.debug(message + " - Duration: {}ms", duration));
    }
    
    /**
     * Log security event
     * Uses Optional to eliminate ternary operators
     */
    public void logSecurityEvent(String correlationId, Long userId, String securityEvent,
                               String sourceIp, String userAgent, String message) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            USER_ID, Optional.ofNullable(userId).map(String::valueOf).orElse("anonymous"),
            SECURITY_EVENT, securityEvent,
            SOURCE_IP, Optional.ofNullable(sourceIp).orElse("unknown"),
            USER_AGENT, Optional.ofNullable(userAgent).orElse("unknown")
        ), () -> SECURITY_LOGGER.warn(message));
    }
    
    /**
     * Log authentication event
     * Uses Optional to eliminate if-statement and ternary operators
     */
    public void logAuthenticationEvent(String correlationId, Long userId, String authEvent,
                                     String sourceIp, boolean success, String message) {
        String securityEvent = Optional.of(success)
            .filter(s -> s)
            .map(s -> "AUTH_SUCCESS")
            .orElse("AUTH_FAILURE");
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            USER_ID, Optional.ofNullable(userId).map(String::valueOf).orElse("unknown"),
            SECURITY_EVENT, securityEvent,
            SOURCE_IP, Optional.ofNullable(sourceIp).orElse("unknown"),
            OPERATION, authEvent
        ), () -> Optional.of(success)
            .filter(s -> s)
            .ifPresentOrElse(
                s -> SECURITY_LOGGER.info(message),
                () -> SECURITY_LOGGER.warn(message)
            )
        );
    }
    
    /**
     * Log error with full context
     * Uses Optional to eliminate ternary operator
     */
    public void logError(String correlationId, Long userId, String errorCode,
                        String operation, String message, Throwable throwable) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            USER_ID, Optional.ofNullable(userId).map(String::valueOf).orElse("system"),
            ERROR_CODE, errorCode,
            OPERATION, operation
        ), () -> ERROR_LOGGER.error(message, throwable));
    }
    
    /**
     * Log business rule violation
     */
    public void logBusinessRuleViolation(String correlationId, Long userId, String ruleType, 
                                       String violationDetails, String message) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            USER_ID, String.valueOf(userId),
            OPERATION, "BUSINESS_RULE_VIOLATION",
            ERROR_CODE, ruleType
        ), () -> AUDIT_LOGGER.warn(message + " - Violation: {}", violationDetails));
    }
    
    /**
     * Log risk management event
     */
    public void logRiskEvent(String correlationId, Long userId, String riskType, 
                           String riskLevel, BigDecimal riskValue, String message) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            USER_ID, String.valueOf(userId),
            OPERATION, "RISK_ASSESSMENT",
            METRIC, riskType,
            VALUE, riskValue.toString()
        ), () -> AUDIT_LOGGER.info(message + " - Risk Level: {}", riskLevel));
    }
    
    /**
     * Log circuit breaker event
     */
    public void logCircuitBreakerEvent(String correlationId, String serviceName, 
                                     String state, String reason, String message) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            OPERATION, "CIRCUIT_BREAKER",
            METRIC, serviceName,
            VALUE, state
        ), () -> PERFORMANCE_LOGGER.warn(message + " - Reason: {}", reason));
    }
    
    /**
     * Start performance timing context
     */
    public PerformanceTimer startPerformanceTimer(String correlationId, String operation) {
        return new PerformanceTimer(correlationId, operation, System.currentTimeMillis());
    }
    
    /**
     * Execute operation with correlation context
     * Uses Optional to eliminate if-statement
     */
    public CompletableFuture<Void> executeWithCorrelation(String correlationId, Runnable operation) {
        return CompletableFuture.runAsync(() -> {
            String previousCorrelationId = MDC.get(CORRELATION_ID);
            try {
                MDC.put(CORRELATION_ID, correlationId);
                operation.run();
            } finally {
                Optional.ofNullable(previousCorrelationId)
                    .ifPresentOrElse(
                        prevId -> MDC.put(CORRELATION_ID, prevId),
                        () -> MDC.remove(CORRELATION_ID)
                    );
            }
        });
    }
    
    /**
     * Execute operation with full context
     */
    public void executeWithContext(String correlationId, Long userId, String operation, Runnable task) {
        withMDC(Map.of(
            CORRELATION_ID, correlationId,
            USER_ID, String.valueOf(userId),
            OPERATION, operation
        ), task);
    }
    
    /**
     * Helper method to execute code with MDC context
     * Uses Optional to eliminate if-statement
     */
    private void withMDC(Map<String, String> contextMap, Runnable operation) {
        // Store current MDC state
        Map<String, String> previousContext = MDC.getCopyOfContextMap();

        try {
            // Set new context
            contextMap.forEach(MDC::put);
            operation.run();
        } finally {
            // Restore previous context
            MDC.clear();
            Optional.ofNullable(previousContext)
                .ifPresent(MDC::setContextMap);
        }
    }
    
    /**
     * Performance timer for measuring operation duration
     */
    public class PerformanceTimer implements AutoCloseable {
        private final String correlationId;
        private final String operation;
        private final long startTime;
        private final Instant startInstant;
        
        public PerformanceTimer(String correlationId, String operation, long startTime) {
            this.correlationId = correlationId;
            this.operation = operation;
            this.startTime = startTime;
            this.startInstant = Instant.now();
        }
        
        /**
         * Stop timer and log performance metric
         */
        public void stop(String message) {
            Duration duration = Duration.between(startInstant, Instant.now());
            logPerformanceMetric(correlationId, operation, "completed", duration, message);
        }
        
        /**
         * Stop timer with custom result value
         */
        public void stop(String message, Object resultValue) {
            Duration duration = Duration.between(startInstant, Instant.now());
            logPerformanceMetric(correlationId, operation, resultValue, duration, message);
        }
        
        /**
         * AutoCloseable implementation
         */
        @Override
        public void close() {
            stop("Operation completed");
        }
        
        /**
         * Get elapsed time in milliseconds
         */
        public long getElapsedMillis() {
            return System.currentTimeMillis() - startTime;
        }
        
        /**
         * Get elapsed duration
         */
        public Duration getElapsedDuration() {
            return Duration.between(startInstant, Instant.now());
        }
    }
    
    /**
     * Generate correlation ID if not present
     * Uses Optional to eliminate if-statement
     */
    public String ensureCorrelationId(String existingCorrelationId) {
        return Optional.ofNullable(existingCorrelationId)
            .filter(id -> !id.trim().isEmpty())
            .orElseGet(() -> {
                String correlationId = "TM-" + System.currentTimeMillis() + "-" +
                                      Integer.toHexString((int) (Math.random() * 65536));
                MDC.put(CORRELATION_ID, correlationId);
                return correlationId;
            });
    }
    
    /**
     * Clear all MDC context
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * Get current correlation ID from MDC
     */
    public String getCurrentCorrelationId() {
        return MDC.get(CORRELATION_ID);
    }
}