package com.trademaster.trading.dto;

import java.time.Instant;
import java.util.Set;

/**
 * Record representing parameters for strategy audit operations.
 */
public record AuditParameters(
    String strategyId,
    Instant startTime,
    Instant endTime,
    Set<String> auditTypes,
    boolean includeTransactions,
    boolean includePositions,
    boolean includeRiskMetrics,
    boolean includeCompliance,
    String requestedBy,
    String correlationId
) {}