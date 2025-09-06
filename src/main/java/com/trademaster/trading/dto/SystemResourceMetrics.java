package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Record representing system resource metrics for strategy monitoring.
 */
public record SystemResourceMetrics(
    String metricsId,
    String strategyId,
    BigDecimal cpuUsagePercent,
    long memoryUsageBytes,
    long maxMemoryBytes,
    int activeThreads,
    int virtualThreads,
    long networkBytesIn,
    long networkBytesOut,
    long diskReadBytes,
    long diskWriteBytes,
    BigDecimal diskUsagePercent,
    Map<String, Object> additionalMetrics,
    Instant timestamp,
    String correlationId
) {}