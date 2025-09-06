package com.trademaster.trading.dto;

import java.time.Instant;

/**
 * Walk Forward Parameters DTO
 */
public record WalkForwardParameters(
    String strategyId,
    Instant startDate,
    Instant endDate,
    Integer trainPeriodDays,
    Integer testPeriodDays,
    Integer stepSizeDays,
    Integer minObservations
) {}
