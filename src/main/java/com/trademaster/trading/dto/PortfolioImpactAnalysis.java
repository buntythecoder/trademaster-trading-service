package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Portfolio Impact Analysis DTO
 */
public record PortfolioImpactAnalysis(
    String analysisId,
    String portfolioId,
    BigDecimal expectedImpact,
    BigDecimal riskImpact,
    BigDecimal correlationImpact,
    Map<String, BigDecimal> sectorImpacts,
    Instant analysedAt
) {}
