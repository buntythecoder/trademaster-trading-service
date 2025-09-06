package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Simulation Parameters DTO
 */
public record SimulationParameters(
    String simulationType,
    Instant startDate,
    Instant endDate,
    BigDecimal initialCapital,
    Integer iterations,
    Map<String, Object> scenarioParams,
    String randomSeed
) {}
