package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Signal Execution Result DTO
 */
public record SignalExecutionResult(
    String signalId,
    String executionId,
    boolean executed,
    BigDecimal executedPrice,
    Integer executedQuantity,
    BigDecimal slippage,
    String status,
    String message,
    Instant executedAt
) {}
