package com.trademaster.trading.dto.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Position Risk DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Data transfer object representing position risk assessment from portfolio service.
 * Used for risk calculations and position limit validation.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record PositionRisk(

    /**
     * User ID
     */
    Long userId,

    /**
     * Trading symbol
     */
    String symbol,

    /**
     * Current position size
     */
    BigDecimal currentPosition,

    /**
     * Maximum allowed position size
     */
    BigDecimal maxPositionSize,

    /**
     * Current position value
     */
    BigDecimal positionValue,

    /**
     * Available margin
     */
    BigDecimal availableMargin,

    /**
     * Used margin
     */
    BigDecimal usedMargin,

    /**
     * Risk score (0.0 - 1.0, higher = riskier)
     */
    BigDecimal riskScore,

    /**
     * Risk level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    RiskLevel riskLevel,

    /**
     * Whether position is within limits
     */
    boolean withinLimits,

    /**
     * Risk calculation timestamp
     */
    LocalDateTime calculatedAt,

    /**
     * Additional risk factors
     */
    String riskFactors
) {

    /**
     * Risk level enumeration
     */
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Check if position exceeds risk limits
     */
    public boolean exceedsLimits() {
        return !withinLimits;
    }

    /**
     * Check if risk is acceptable (not CRITICAL)
     */
    public boolean isAcceptableRisk() {
        return riskLevel != RiskLevel.CRITICAL;
    }

    /**
     * Calculate remaining position capacity
     */
    public BigDecimal remainingCapacity() {
        return maxPositionSize.subtract(currentPosition);
    }
}
