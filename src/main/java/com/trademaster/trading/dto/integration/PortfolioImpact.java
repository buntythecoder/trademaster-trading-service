package com.trademaster.trading.dto.integration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Portfolio Impact DTO
 *
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #3 - Functional Programming (immutable data)
 *
 * Data transfer object representing the impact of a potential trade on the portfolio.
 * Used for pre-trade impact analysis and risk assessment.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public record PortfolioImpact(

    /**
     * User ID
     */
    Long userId,

    /**
     * Trading symbol
     */
    String symbol,

    /**
     * Order quantity
     */
    Integer quantity,

    /**
     * Order value
     */
    BigDecimal orderValue,

    /**
     * New portfolio value after trade
     */
    BigDecimal newPortfolioValue,

    /**
     * Portfolio value change
     */
    BigDecimal valueChange,

    /**
     * New portfolio risk score
     */
    BigDecimal newRiskScore,

    /**
     * Risk score change
     */
    BigDecimal riskChange,

    /**
     * New portfolio concentration (%)
     */
    BigDecimal newConcentration,

    /**
     * Concentration change (%)
     */
    BigDecimal concentrationChange,

    /**
     * Impact on margin usage (%)
     */
    BigDecimal marginImpact,

    /**
     * Impact on diversification score
     */
    BigDecimal diversificationImpact,

    /**
     * Whether impact is acceptable
     */
    boolean acceptable,

    /**
     * Impact warnings/recommendations
     */
    List<String> warnings,

    /**
     * Impact recommendations
     */
    List<String> recommendations
) {

    /**
     * Check if impact increases risk significantly
     */
    public boolean increasesRiskSignificantly() {
        return riskChange.compareTo(BigDecimal.valueOf(0.1)) > 0; // 10% increase
    }

    /**
     * Check if impact increases concentration significantly
     */
    public boolean increasesConcentration() {
        return concentrationChange.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if margin impact is acceptable
     */
    public boolean marginImpactAcceptable() {
        return marginImpact.compareTo(BigDecimal.valueOf(0.8)) < 0; // <80% margin usage
    }

    /**
     * Check if there are any critical warnings
     */
    public boolean hasCriticalWarnings() {
        return warnings.stream()
            .anyMatch(w -> w.toLowerCase().contains("critical") ||
                          w.toLowerCase().contains("limit"));
    }

    /**
     * Get impact severity level
     */
    public ImpactSeverity getSeverity() {
        return switch (hasCriticalWarnings()) {
            case true -> ImpactSeverity.HIGH;
            case false -> switch (warnings.isEmpty()) {
                case true -> ImpactSeverity.LOW;
                case false -> ImpactSeverity.MEDIUM;
            };
        };
    }

    /**
     * Impact severity enumeration
     */
    public enum ImpactSeverity {
        LOW,
        MEDIUM,
        HIGH
    }
}
