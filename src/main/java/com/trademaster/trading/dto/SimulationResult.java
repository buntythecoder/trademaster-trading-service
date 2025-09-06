package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Simulation Result DTO
 */
public record SimulationResult(
    String simulationId,
    BigDecimal finalValue,
    BigDecimal totalReturn,
    BigDecimal maxDrawdown,
    BigDecimal sharpeRatio,
    List<Map<String, Object>> scenarioResults,
    Instant completedAt
) {}
