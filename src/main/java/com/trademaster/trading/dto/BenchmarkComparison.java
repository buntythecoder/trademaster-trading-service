package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Benchmark Comparison DTO
 */
public record BenchmarkComparison(
    String comparisonId,
    String strategyId,
    String benchmarkId,
    BigDecimal outperformance,
    BigDecimal trackingError,
    BigDecimal informationRatio,
    Map<String, BigDecimal> periodReturns,
    Instant generatedAt
) {}
