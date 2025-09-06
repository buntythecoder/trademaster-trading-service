package com.trademaster.trading.common;

/**
 * Domain-specific error types for functional error handling
 * 
 * Sealed hierarchy provides exhaustive pattern matching
 * and type-safe error handling without exceptions.
 */
public sealed interface TradeError permits 
    TradeError.ValidationError,
    TradeError.RiskError,
    TradeError.ExecutionError,
    TradeError.DataError,
    TradeError.SystemError {
    
    String getMessage();
    String getCode();
    
    // Static factory methods for convenience
    static ValidationError validationError(String message) {
        return new ValidationError.MissingRequiredField(message);
    }
    
    static RiskError riskError(String message) {
        return new RiskError.InsufficientFunds("unknown", java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
    }
    
    /**
     * Input validation errors
     */
    sealed interface ValidationError extends TradeError permits
        ValidationError.InvalidSymbol,
        ValidationError.InvalidQuantity,
        ValidationError.InvalidPrice,
        ValidationError.InvalidOrderType,
        ValidationError.MissingRequiredField {
        
        record InvalidSymbol(String symbol) implements ValidationError {
            @Override
            public String getMessage() { return "Invalid trading symbol: " + symbol; }
            @Override
            public String getCode() { return "INVALID_SYMBOL"; }
        }
        
        record InvalidQuantity(Integer quantity) implements ValidationError {
            @Override
            public String getMessage() { return "Invalid quantity: " + quantity; }
            @Override
            public String getCode() { return "INVALID_QUANTITY"; }
        }
        
        record InvalidPrice(String price) implements ValidationError {
            @Override
            public String getMessage() { return "Invalid price: " + price; }
            @Override
            public String getCode() { return "INVALID_PRICE"; }
        }
        
        record InvalidOrderType(String orderType) implements ValidationError {
            @Override
            public String getMessage() { return "Invalid order type: " + orderType; }
            @Override
            public String getCode() { return "INVALID_ORDER_TYPE"; }
        }
        
        record MissingRequiredField(String fieldName) implements ValidationError {
            @Override
            public String getMessage() { return "Missing required field: " + fieldName; }
            @Override
            public String getCode() { return "MISSING_FIELD"; }
        }
    }
    
    /**
     * Risk management errors
     */
    sealed interface RiskError extends TradeError permits
        RiskError.InsufficientFunds,
        RiskError.PositionLimitExceeded,
        RiskError.RiskLimitExceeded,
        RiskError.MarginRequirementNotMet {
        
        record InsufficientFunds(String userId, java.math.BigDecimal required, java.math.BigDecimal available) implements RiskError {
            @Override
            public String getMessage() { 
                return String.format("Insufficient funds for user %s: required %s, available %s", 
                    userId, required, available); 
            }
            @Override
            public String getCode() { return "INSUFFICIENT_FUNDS"; }
        }
        
        record PositionLimitExceeded(String symbol, Integer currentPosition, Integer limit) implements RiskError {
            @Override
            public String getMessage() {
                return String.format("Position limit exceeded for %s: current %d, limit %d", 
                    symbol, currentPosition, limit);
            }
            @Override
            public String getCode() { return "POSITION_LIMIT_EXCEEDED"; }
        }
        
        record RiskLimitExceeded(String riskType, java.math.BigDecimal currentRisk, java.math.BigDecimal limit) implements RiskError {
            @Override
            public String getMessage() {
                return String.format("%s risk limit exceeded: current %s, limit %s", 
                    riskType, currentRisk, limit);
            }
            @Override
            public String getCode() { return "RISK_LIMIT_EXCEEDED"; }
        }
        
        record MarginRequirementNotMet(java.math.BigDecimal required, java.math.BigDecimal available) implements RiskError {
            @Override
            public String getMessage() {
                return String.format("Margin requirement not met: required %s, available %s", 
                    required, available);
            }
            @Override
            public String getCode() { return "MARGIN_NOT_MET"; }
        }
    }
    
    /**
     * Order execution errors
     */
    sealed interface ExecutionError extends TradeError permits
        ExecutionError.MarketClosed,
        ExecutionError.InsufficientLiquidity,
        ExecutionError.OrderRejected,
        ExecutionError.TimeoutError,
        ExecutionError.VenueUnavailable {
        
        record MarketClosed(String market) implements ExecutionError {
            @Override
            public String getMessage() { return "Market is closed: " + market; }
            @Override
            public String getCode() { return "MARKET_CLOSED"; }
        }
        
        record InsufficientLiquidity(String symbol) implements ExecutionError {
            @Override
            public String getMessage() { return "Insufficient liquidity for: " + symbol; }
            @Override
            public String getCode() { return "INSUFFICIENT_LIQUIDITY"; }
        }
        
        record OrderRejected(String reason) implements ExecutionError {
            @Override
            public String getMessage() { return "Order rejected: " + reason; }
            @Override
            public String getCode() { return "ORDER_REJECTED"; }
        }
        
        record TimeoutError(long timeoutMs) implements ExecutionError {
            @Override
            public String getMessage() { return "Operation timed out after " + timeoutMs + "ms"; }
            @Override
            public String getCode() { return "TIMEOUT"; }
        }
        
        record VenueUnavailable(String venue) implements ExecutionError {
            @Override
            public String getMessage() { return "Trading venue unavailable: " + venue; }
            @Override
            public String getCode() { return "VENUE_UNAVAILABLE"; }
        }
    }
    
    /**
     * Data access errors
     */
    sealed interface DataError extends TradeError permits
        DataError.EntityNotFound,
        DataError.DuplicateEntity,
        DataError.DatabaseError,
        DataError.DataIntegrityViolation {
        
        record EntityNotFound(String entityType, String id) implements DataError {
            @Override
            public String getMessage() { return entityType + " not found: " + id; }
            @Override
            public String getCode() { return "ENTITY_NOT_FOUND"; }
        }
        
        record DuplicateEntity(String entityType, String id) implements DataError {
            @Override
            public String getMessage() { return entityType + " already exists: " + id; }
            @Override
            public String getCode() { return "DUPLICATE_ENTITY"; }
        }
        
        record DatabaseError(String operation, String details) implements DataError {
            @Override
            public String getMessage() { return "Database error in " + operation + ": " + details; }
            @Override
            public String getCode() { return "DATABASE_ERROR"; }
        }
        
        record DataIntegrityViolation(String constraint) implements DataError {
            @Override
            public String getMessage() { return "Data integrity violation: " + constraint; }
            @Override
            public String getCode() { return "DATA_INTEGRITY_VIOLATION"; }
        }
    }
    
    /**
     * System-level errors
     */
    sealed interface SystemError extends TradeError permits
        SystemError.ServiceUnavailable,
        SystemError.ConfigurationError,
        SystemError.ResourceExhausted,
        SystemError.UnexpectedError {
        
        record ServiceUnavailable(String serviceName) implements SystemError {
            @Override
            public String getMessage() { return "Service unavailable: " + serviceName; }
            @Override
            public String getCode() { return "SERVICE_UNAVAILABLE"; }
        }
        
        record ConfigurationError(String parameter, String issue) implements SystemError {
            @Override
            public String getMessage() { return "Configuration error in " + parameter + ": " + issue; }
            @Override
            public String getCode() { return "CONFIG_ERROR"; }
        }
        
        record ResourceExhausted(String resource) implements SystemError {
            @Override
            public String getMessage() { return "Resource exhausted: " + resource; }
            @Override
            public String getCode() { return "RESOURCE_EXHAUSTED"; }
        }
        
        record UnexpectedError(String details) implements SystemError {
            @Override
            public String getMessage() { return "Unexpected error: " + details; }
            @Override
            public String getCode() { return "UNEXPECTED_ERROR"; }
        }
    }
}