package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Portfolio Context DTO
 */
public record PortfolioContext(
    String portfolioId,
    BigDecimal totalValue,
    BigDecimal cashBalance,
    Map<String, BigDecimal> positions,
    BigDecimal riskCapacity,
    String riskProfile,
    Map<String, Object> constraints
) {}
