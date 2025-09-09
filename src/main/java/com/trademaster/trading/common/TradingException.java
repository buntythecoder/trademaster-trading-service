package com.trademaster.common.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Trading Exception
 * 
 * Base exception for all trading-related errors.
 * Provides structured error information for debugging and monitoring.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public class TradingException extends RuntimeException {
    
    private final String errorCode;
    private final String orderId;
    private final String symbol;
    private final Long userId;
    private final Instant timestamp;
    private final Map<String, Object> context;
    private final ErrorSeverity severity;
    
    public TradingException(String message) {
        this(message, null, null, null, null, null, ErrorSeverity.MEDIUM, null);
    }
    
    public TradingException(String message, Throwable cause) {
        this(message, cause, null, null, null, null, ErrorSeverity.MEDIUM, null);
    }
    
    public TradingException(String message, String errorCode, String orderId, String symbol, Long userId) {
        this(message, null, errorCode, orderId, symbol, userId, ErrorSeverity.MEDIUM, null);
    }
    
    public TradingException(String message, Throwable cause, String errorCode, String orderId, 
                          String symbol, Long userId, ErrorSeverity severity, Map<String, Object> context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.orderId = orderId;
        this.symbol = symbol;
        this.userId = userId;
        this.timestamp = Instant.now();
        this.context = context;
        this.severity = severity != null ? severity : ErrorSeverity.MEDIUM;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public ErrorSeverity getSeverity() {
        return severity;
    }
    
    public boolean isCritical() {
        return severity == ErrorSeverity.CRITICAL;
    }
    
    public boolean isRetryable() {
        return errorCode != null && (
            errorCode.startsWith("TEMP_") ||
            errorCode.contains("TIMEOUT") ||
            errorCode.contains("CONNECTION") ||
            errorCode.contains("NETWORK")
        );
    }
    
    /**
     * Get formatted error message with context
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder(getMessage());
        
        if (errorCode != null) {
            sb.append(" [").append(errorCode).append("]");
        }
        
        if (orderId != null) {
            sb.append(" Order: ").append(orderId);
        }
        
        if (symbol != null) {
            sb.append(" Symbol: ").append(symbol);
        }
        
        if (userId != null) {
            sb.append(" User: ").append(userId);
        }
        
        return sb.toString();
    }
    
    public enum ErrorSeverity {
        LOW(1),
        MEDIUM(2), 
        HIGH(3),
        CRITICAL(4);
        
        private final int level;
        
        ErrorSeverity(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
        
        public boolean isHigherThan(ErrorSeverity other) {
            return this.level > other.level;
        }
    }
    
    // Specific trading exception types
    
    public static class OrderValidationException extends TradingException {
        public OrderValidationException(String message, String orderId, String symbol, Long userId) {
            super(message, "ORDER_VALIDATION_FAILED", orderId, symbol, userId);
        }
    }
    
    public static class InsufficientFundsException extends TradingException {
        public InsufficientFundsException(String message, String orderId, Long userId) {
            super(message, "INSUFFICIENT_FUNDS", orderId, null, userId);
        }
    }
    
    public static class RiskLimitExceededException extends TradingException {
        public RiskLimitExceededException(String message, String orderId, String symbol, Long userId) {
            super(message, null, "RISK_LIMIT_EXCEEDED", orderId, symbol, userId, ErrorSeverity.HIGH, null);
        }
    }
    
    public static class OrderExecutionException extends TradingException {
        public OrderExecutionException(String message, Throwable cause, String orderId, String symbol) {
            super(message, cause, "ORDER_EXECUTION_FAILED", orderId, symbol, null, ErrorSeverity.HIGH, null);
        }
    }
    
    public static class MarketDataException extends TradingException {
        public MarketDataException(String message, String symbol) {
            super(message, "MARKET_DATA_ERROR", null, symbol, null);
        }
    }
    
    public static class BrokerIntegrationException extends TradingException {
        public BrokerIntegrationException(String message, Throwable cause, String orderId) {
            super(message, cause, "BROKER_INTEGRATION_ERROR", orderId, null, null, ErrorSeverity.HIGH, null);
        }
    }
    
    public static class PositionManagementException extends TradingException {
        public PositionManagementException(String message, String symbol, Long userId) {
            super(message, "POSITION_MANAGEMENT_ERROR", null, symbol, userId);
        }
    }
    
    public static class ComplianceException extends TradingException {
        public ComplianceException(String message, String orderId, Long userId) {
            super(message, null, "COMPLIANCE_VIOLATION", orderId, null, userId, ErrorSeverity.CRITICAL, null);
        }
    }
}