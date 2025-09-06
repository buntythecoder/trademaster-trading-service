package com.trademaster.trading.dto;

import java.time.Instant;
import java.util.List;

/**
 * Performance Report Parameters DTO
 */
public record PerformanceReportParams(
    String strategyId,
    Instant startDate,
    Instant endDate,
    List<String> metrics,
    String reportType,
    String benchmark
) {}
