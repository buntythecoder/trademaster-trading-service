package com.trademaster.trading.routing;

/**
 * Routing Error Record
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Sealed interface for routing errors using pattern matching.
 * All routing errors are immutable Records.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface RoutingError permits
    RoutingError.NoBrokerAvailableError,
    RoutingError.UnsupportedExchangeError,
    RoutingError.OrderTooLargeError,
    RoutingError.BrokerConnectivityError,
    RoutingError.ConfigurationError {

    /**
     * Get error message for display
     */
    String message();

    /**
     * Get error code for logging and metrics
     */
    String code();

    /**
     * No broker available error
     */
    record NoBrokerAvailableError(
            String exchange,
            String reason,
            String message
    ) implements RoutingError {
        @Override
        public String code() {
            return "NO_BROKER_AVAILABLE";
        }
    }

    /**
     * Unsupported exchange error
     */
    record UnsupportedExchangeError(
            String exchange,
            String message
    ) implements RoutingError {
        @Override
        public String code() {
            return "UNSUPPORTED_EXCHANGE";
        }
    }

    /**
     * Order too large error
     */
    record OrderTooLargeError(
            Integer quantity,
            Integer maxQuantity,
            String message
    ) implements RoutingError {
        @Override
        public String code() {
            return "ORDER_TOO_LARGE";
        }
    }

    /**
     * Broker connectivity error
     */
    record BrokerConnectivityError(
            String brokerName,
            String message
    ) implements RoutingError {
        @Override
        public String code() {
            return "BROKER_CONNECTIVITY_ERROR";
        }
    }

    /**
     * Configuration error
     */
    record ConfigurationError(
            String configKey,
            String message
    ) implements RoutingError {
        @Override
        public String code() {
            return "CONFIGURATION_ERROR";
        }
    }

    // Factory methods for common errors
    static NoBrokerAvailableError noBrokerAvailable(String exchange, String reason) {
        return new NoBrokerAvailableError(
                exchange,
                reason,
                "No broker available for exchange " + exchange + ": " + reason
        );
    }

    static UnsupportedExchangeError unsupportedExchange(String exchange) {
        return new UnsupportedExchangeError(
                exchange,
                "Exchange " + exchange + " is not supported for routing"
        );
    }

    static OrderTooLargeError orderTooLarge(Integer quantity, Integer maxQuantity) {
        return new OrderTooLargeError(
                quantity,
                maxQuantity,
                "Order quantity " + quantity + " exceeds maximum " + maxQuantity
        );
    }

    static BrokerConnectivityError brokerConnectivity(String brokerName) {
        return new BrokerConnectivityError(
                brokerName,
                "Broker " + brokerName + " is not connected or unavailable"
        );
    }

    static ConfigurationError configurationError(String configKey) {
        return new ConfigurationError(
                configKey,
                "Configuration error: " + configKey + " is not properly configured"
        );
    }
}
