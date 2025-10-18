package com.trademaster.trading.validation;

/**
 * Validation Error Record
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Sealed interface for validation errors using pattern matching.
 * All validation errors are immutable Records.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface ValidationError permits
    ValidationError.SymbolError,
    ValidationError.QuantityError,
    ValidationError.PriceError,
    ValidationError.OrderTypeError,
    ValidationError.TimeInForceError,
    ValidationError.PermissionError,
    ValidationError.BusinessRuleError {

    /**
     * Get error message for display
     */
    String message();

    /**
     * Get error code for logging and metrics
     */
    String code();

    /**
     * Get error field (optional)
     */
    default String field() {
        return null;
    }

    // Symbol validation errors
    record SymbolError(String symbol, String message) implements ValidationError {
        @Override
        public String code() {
            return "SYMBOL_ERROR";
        }

        @Override
        public String field() {
            return "symbol";
        }
    }

    // Quantity validation errors
    record QuantityError(Integer quantity, String message) implements ValidationError {
        @Override
        public String code() {
            return "QUANTITY_ERROR";
        }

        @Override
        public String field() {
            return "quantity";
        }
    }

    // Price validation errors
    record PriceError(String priceField, String message) implements ValidationError {
        @Override
        public String code() {
            return "PRICE_ERROR";
        }

        @Override
        public String field() {
            return priceField;
        }
    }

    // Order type validation errors
    record OrderTypeError(String orderType, String message) implements ValidationError {
        @Override
        public String code() {
            return "ORDER_TYPE_ERROR";
        }

        @Override
        public String field() {
            return "orderType";
        }
    }

    // Time in force validation errors
    record TimeInForceError(String timeInForce, String message) implements ValidationError {
        @Override
        public String code() {
            return "TIME_IN_FORCE_ERROR";
        }

        @Override
        public String field() {
            return "timeInForce";
        }
    }

    // User permission validation errors
    record PermissionError(String permission, String message) implements ValidationError {
        @Override
        public String code() {
            return "PERMISSION_ERROR";
        }

        @Override
        public String field() {
            return "permissions";
        }
    }

    // Business rule validation errors
    record BusinessRuleError(String rule, String message) implements ValidationError {
        @Override
        public String code() {
            return "BUSINESS_RULE_ERROR";
        }
    }

    // Factory methods for common errors
    static SymbolError symbolNotFound(String symbol) {
        return new SymbolError(symbol, "Symbol '" + symbol + "' not found or not tradeable");
    }

    static SymbolError symbolSuspended(String symbol) {
        return new SymbolError(symbol, "Symbol '" + symbol + "' is suspended from trading");
    }

    static SymbolError invalidSymbolFormat(String symbol) {
        return new SymbolError(symbol, "Symbol '" + symbol + "' has invalid format");
    }

    static QuantityError quantityBelowMinimum(Integer quantity, Integer minimum) {
        return new QuantityError(quantity, "Quantity " + quantity + " is below minimum " + minimum);
    }

    static QuantityError quantityAboveMaximum(Integer quantity, Integer maximum) {
        return new QuantityError(quantity, "Quantity " + quantity + " exceeds maximum " + maximum);
    }

    static QuantityError invalidLotSize(Integer quantity, Integer lotSize) {
        return new QuantityError(quantity, "Quantity " + quantity + " is not a multiple of lot size " + lotSize);
    }

    static PriceError priceRequired(String field) {
        return new PriceError(field, field + " is required for this order type");
    }

    static PriceError invalidTickSize(String field, String price, String tickSize) {
        return new PriceError(field, price + " does not conform to tick size " + tickSize);
    }

    static PriceError priceOutsideCircuitLimits(String field, String price, String lowerLimit, String upperLimit) {
        return new PriceError(field, price + " outside circuit limits [" + lowerLimit + ", " + upperLimit + "]");
    }

    static OrderTypeError incompatibleOrderType(String orderType, String reason) {
        return new OrderTypeError(orderType, "Order type " + orderType + " incompatible: " + reason);
    }

    static TimeInForceError expiryDateRequired(String timeInForce) {
        return new TimeInForceError(timeInForce, "Expiry date required for " + timeInForce);
    }

    static TimeInForceError invalidExpiryDate(String timeInForce, String reason) {
        return new TimeInForceError(timeInForce, "Invalid expiry date: " + reason);
    }

    static PermissionError insufficientPermissions(String requiredPermission) {
        return new PermissionError(requiredPermission, "User lacks permission: " + requiredPermission);
    }

    static BusinessRuleError businessRuleViolation(String rule, String reason) {
        return new BusinessRuleError(rule, "Business rule '" + rule + "' violated: " + reason);
    }
}
