package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Risk Analysis Parameters DTO
 */
public record RiskAnalysisParams(
    String portfolioId,
    Instant startDate,
    Instant endDate,
    BigDecimal confidenceLevel,
    Integer lookbackDays,
    String riskModel
) {}
