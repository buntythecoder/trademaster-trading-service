package com.trademaster.trading.dto;

import java.time.Instant;
import java.util.List;

/**
 * Comparison Parameters DTO
 */
public record ComparisonParameters(
    List<String> strategyIds,
    Instant startDate,
    Instant endDate,
    String benchmark,
    List<String> metrics,
    String comparisonType
) {}
