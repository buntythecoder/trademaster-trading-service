package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Risk Limit Result DTO
 */
public record RiskLimitResult(
    String limitId,
    String limitType,
    BigDecimal currentValue,
    BigDecimal limitValue,
    BigDecimal utilization,
    boolean breached,
    String status,
    Instant checkedAt
) {}
