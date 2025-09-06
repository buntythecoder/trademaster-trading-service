package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Signal Execution Parameters DTO
 */
public record SignalExecutionParams(
    String signalId,
    String executionMode,
    BigDecimal maxSlippage,
    Integer timeoutSeconds,
    boolean partialFillAllowed,
    Map<String, Object> customParams
) {}
