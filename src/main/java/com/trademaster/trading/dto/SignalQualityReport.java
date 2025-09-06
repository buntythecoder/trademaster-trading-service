package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Signal Quality Report DTO
 */
public record SignalQualityReport(
    String signalId,
    BigDecimal accuracyScore,
    BigDecimal precisionScore,
    BigDecimal recallScore,
    Integer totalSignals,
    Integer correctSignals,
    Map<String, BigDecimal> qualityMetrics,
    Instant evaluatedAt
) {}
