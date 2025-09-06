package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Walk Forward Result DTO
 */
public record WalkForwardResult(
    String analysisId,
    List<Map<String, Object>> periodResults,
    BigDecimal averageReturn,
    BigDecimal consistency,
    BigDecimal degradationFactor,
    Instant completedAt
) {}
