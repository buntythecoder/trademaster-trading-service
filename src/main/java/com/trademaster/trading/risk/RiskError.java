package com.trademaster.trading.risk;

import java.math.BigDecimal;

/**
 * Risk Error Record
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Sealed interface for risk check errors using pattern matching.
 * All risk errors are immutable Records.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public sealed interface RiskError permits
    RiskError.InsufficientBuyingPowerError,
    RiskError.PositionLimitError,
    RiskError.OrderValueLimitError,
    RiskError.DailyTradeLimitError,
    RiskError.MarginRequirementError,
    RiskError.ConcentrationRiskError,
    RiskError.SystemError {

    /**
     * Get error message for display
     */
    String message();

    /**
     * Get error code for logging and metrics
     */
    String code();

    /**
     * Get risk severity
     */
    RiskSeverity severity();

    /**
     * Risk severity levels
     */
    enum RiskSeverity {
        LOW,      // Warning only
        MEDIUM,   // Caution required
        HIGH,     // Blocks order execution
        CRITICAL  // Immediate attention required
    }

    // Insufficient buying power error
    record InsufficientBuyingPowerError(
            BigDecimal required,
            BigDecimal available,
            String message
    ) implements RiskError {
        @Override
        public String code() {
            return "INSUFFICIENT_BUYING_POWER";
        }

        @Override
        public RiskSeverity severity() {
            return RiskSeverity.HIGH;
        }
    }

    // Position limit exceeded error
    record PositionLimitError(
            String symbol,
            BigDecimal currentPosition,
            BigDecimal maxPosition,
            String message
    ) implements RiskError {
        @Override
        public String code() {
            return "POSITION_LIMIT_EXCEEDED";
        }

        @Override
        public RiskSeverity severity() {
            return RiskSeverity.HIGH;
        }
    }

    // Order value limit exceeded error
    record OrderValueLimitError(
            BigDecimal orderValue,
            BigDecimal maxOrderValue,
            String message
    ) implements RiskError {
        @Override
        public String code() {
            return "ORDER_VALUE_LIMIT_EXCEEDED";
        }

        @Override
        public RiskSeverity severity() {
            return RiskSeverity.HIGH;
        }
    }

    // Daily trade limit exceeded error
    record DailyTradeLimitError(
            Integer currentTrades,
            Integer maxTrades,
            String message
    ) implements RiskError {
        @Override
        public String code() {
            return "DAILY_TRADE_LIMIT_EXCEEDED";
        }

        @Override
        public RiskSeverity severity() {
            return RiskSeverity.MEDIUM;
        }
    }

    // Margin requirement error
    record MarginRequirementError(
            BigDecimal requiredMargin,
            BigDecimal availableMargin,
            String message
    ) implements RiskError {
        @Override
        public String code() {
            return "MARGIN_REQUIREMENT_NOT_MET";
        }

        @Override
        public RiskSeverity severity() {
            return RiskSeverity.HIGH;
        }
    }

    // Concentration risk error
    record ConcentrationRiskError(
            String symbol,
            BigDecimal positionPercentage,
            BigDecimal maxPercentage,
            String message
    ) implements RiskError {
        @Override
        public String code() {
            return "CONCENTRATION_RISK_EXCEEDED";
        }

        @Override
        public RiskSeverity severity() {
            return RiskSeverity.MEDIUM;
        }
    }

    // System error
    record SystemError(
            String message,
            String cause
    ) implements RiskError {
        @Override
        public String code() {
            return "SYSTEM_ERROR";
        }

        @Override
        public RiskSeverity severity() {
            return RiskSeverity.CRITICAL;
        }
    }

    // Factory methods for common errors
    static InsufficientBuyingPowerError insufficientBuyingPower(
            BigDecimal required,
            BigDecimal available) {
        return new InsufficientBuyingPowerError(
                required,
                available,
                "Insufficient buying power: required " + required + ", available " + available
        );
    }

    static PositionLimitError positionLimitExceeded(
            String symbol,
            BigDecimal currentPosition,
            BigDecimal maxPosition) {
        return new PositionLimitError(
                symbol,
                currentPosition,
                maxPosition,
                "Position limit exceeded for " + symbol + ": current " + currentPosition + ", max " + maxPosition
        );
    }

    static OrderValueLimitError orderValueLimitExceeded(
            BigDecimal orderValue,
            BigDecimal maxOrderValue) {
        return new OrderValueLimitError(
                orderValue,
                maxOrderValue,
                "Order value limit exceeded: " + orderValue + " exceeds max " + maxOrderValue
        );
    }

    static DailyTradeLimitError dailyTradeLimitExceeded(
            Integer currentTrades,
            Integer maxTrades) {
        return new DailyTradeLimitError(
                currentTrades,
                maxTrades,
                "Daily trade limit exceeded: " + currentTrades + " trades, max " + maxTrades
        );
    }

    static MarginRequirementError marginRequirementNotMet(
            BigDecimal requiredMargin,
            BigDecimal availableMargin) {
        return new MarginRequirementError(
                requiredMargin,
                availableMargin,
                "Margin requirement not met: required " + requiredMargin + ", available " + availableMargin
        );
    }

    static ConcentrationRiskError concentrationRiskExceeded(
            String symbol,
            BigDecimal positionPercentage,
            BigDecimal maxPercentage) {
        return new ConcentrationRiskError(
                symbol,
                positionPercentage,
                maxPercentage,
                "Concentration risk for " + symbol + ": " + positionPercentage + "% exceeds max " + maxPercentage + "%"
        );
    }

    static SystemError systemError(String message, String cause) {
        return new SystemError(message, cause);
    }
}
