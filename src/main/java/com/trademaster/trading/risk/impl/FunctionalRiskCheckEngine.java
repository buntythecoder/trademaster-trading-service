package com.trademaster.trading.risk.impl;

import com.trademaster.common.functional.Result;
import com.trademaster.common.functional.Validation;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.integration.PortfolioImpact;
import com.trademaster.trading.dto.integration.PositionRisk;
import com.trademaster.trading.error.ServiceError;
import com.trademaster.trading.integration.client.PortfolioServiceClient;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.risk.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Functional Risk Check Engine
 *
 * MANDATORY: Rule #3 - Functional Programming (no if-else, no loops)
 * MANDATORY: Rule #5 - Cognitive Complexity Control (max 7 per method)
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #11 - Error Handling Patterns (Railway programming)
 * MANDATORY: Rule #12 - Virtual Threads & Concurrency
 * MANDATORY: Rule #13 - Stream API Mastery
 * MANDATORY: Rule #14 - Pattern Matching Excellence
 * MANDATORY: Rule #15 - Structured Logging & Monitoring
 * MANDATORY: Rule #25 - Circuit Breaker Implementation
 *
 * Comprehensive functional risk checking system with zero if-else statements.
 * Uses Validation monad for error accumulation and pattern matching throughout.
 *
 * Risk Checks Implemented:
 * - Buying power validation
 * - Position limit validation
 * - Order value limit validation
 * - Daily trade limit validation
 * - Margin requirement validation
 * - Concentration risk validation
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionalRiskCheckEngine implements RiskCheckEngine {

    private final PortfolioServiceClient portfolioClient;
    private final MeterRegistry meterRegistry;

    // Daily trade counters (userId -> count)
    private final Map<Long, AtomicInteger> dailyTradeCounters = new ConcurrentHashMap<>();
    private final Map<Long, LocalDate> lastResetDates = new ConcurrentHashMap<>();

    // Configuration constants (Rule #16: Dynamic Configuration)
    @Value("${trading.risk.max-order-value:10000000}")
    private BigDecimal maxOrderValue;

    @Value("${trading.risk.max-daily-trades:100}")
    private Integer maxDailyTrades;

    @Value("${trading.risk.max-position-concentration:30.0}")
    private BigDecimal maxConcentrationPercent;

    @Value("${trading.risk.min-buying-power-buffer:0.1}")
    private BigDecimal minBuyingPowerBuffer;

    @Value("${trading.risk.max-margin-usage:0.8}")
    private BigDecimal maxMarginUsage;

    // Metrics constants (Rule #15)
    private static final String RISK_CHECK_METRIC = "trading.risk.check";
    private static final String RISK_VIOLATION_METRIC = "trading.risk.violations";

    /**
     * Perform comprehensive risk check on order request
     * Rule #11: Functional error handling with Result monad
     */
    @Override
    public RiskCheckResult performRiskCheck(OrderRequest orderRequest, Long userId) {
        long startTime = System.nanoTime();

        log.info("Performing risk check for user {} symbol {} - order value: {}",
                userId, orderRequest.symbol(), orderRequest.getEstimatedOrderValue());

        // Functional risk check pipeline (Rule #3, #11)
        RiskCheckResult result = performRiskCheckFunctionally(orderRequest, userId);

        // Record metrics (Rule #15)
        recordRiskCheckMetrics(result, System.nanoTime() - startTime);

        return result;
    }

    /**
     * Perform risk check on order modification
     * Rule #11: Functional error handling
     */
    @Override
    public RiskCheckResult performModificationRiskCheck(
            Order existingOrder,
            OrderRequest modificationRequest,
            Long userId) {

        log.info("Performing modification risk check for order {}", existingOrder.getId());

        // For modifications, perform same checks as new orders
        return performRiskCheck(modificationRequest, userId);
    }

    /**
     * Get risk metrics for user
     * Rule #12: Virtual Threads with CompletableFuture
     */
    @Override
    public CompletableFuture<RiskMetrics> getRiskMetrics(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            int dailyTrades = getDailyTradeCount(userId);

            return RiskMetrics.builder()
                .userId(userId)
                .dailyTradeCount(dailyTrades)
                .maxOrderValue(maxOrderValue)
                .currentRiskScore(0.0)
                .riskUtilization(0.0)
                .calculatedAt(java.time.Instant.now())
                .build();
        });
    }

    /**
     * Check if user is approaching risk limits
     * Rule #12: Virtual Threads with CompletableFuture
     */
    @Override
    public CompletableFuture<Boolean> isApproachingRiskLimits(Long userId) {
        return getRiskMetrics(userId)
            .thenApply(metrics -> {
                double usagePercent = (double) metrics.getDailyTradeCount() / maxDailyTrades;
                return usagePercent >= 0.8;  // 80% threshold
            });
    }

    /**
     * Functional risk check pipeline
     * Rule #3: Zero if-else, functional composition
     * Rule #13: Stream API for functional processing
     */
    private RiskCheckResult performRiskCheckFunctionally(OrderRequest request, Long userId) {
        BigDecimal orderValue = request.getEstimatedOrderValue();

        // Compose all risk checks using functional pipeline
        List<Function<OrderRequest, Validation<OrderRequest, RiskError>>> riskChecks = List.of(
            r -> checkOrderValueLimit(r, orderValue),
            r -> checkDailyTradeLimit(userId),
            r -> checkBuyingPower(userId, orderValue),
            r -> checkPositionLimits(userId, r),
            r -> checkConcentrationRisk(userId, r, orderValue),
            r -> checkMarginRequirements(userId, orderValue)
        );

        // Execute all checks and accumulate errors
        Validation<OrderRequest, RiskError> result = Validation.validateWith(request, riskChecks);

        // Convert to RiskCheckResult
        return adaptToRiskCheckResult(result, userId);
    }

    /**
     * Check order value limit
     * Rule #14: Pattern matching with switch expressions
     */
    private Validation<OrderRequest, RiskError> checkOrderValueLimit(
            OrderRequest request,
            BigDecimal orderValue) {

        return Optional.ofNullable(orderValue)
            .filter(value -> value.compareTo(maxOrderValue) > 0)
            .map(value -> Validation.<OrderRequest, RiskError>invalid(
                RiskError.orderValueLimitExceeded(value, maxOrderValue)))
            .orElseGet(() -> Validation.valid(request));
    }

    /**
     * Check daily trade limit
     * Rule #3: Functional programming with Optional
     */
    private Validation<OrderRequest, RiskError> checkDailyTradeLimit(Long userId) {
        int currentTrades = getDailyTradeCount(userId);

        return Optional.of(currentTrades)
            .filter(count -> count >= maxDailyTrades)
            .map(count -> Validation.<OrderRequest, RiskError>invalid(
                RiskError.dailyTradeLimitExceeded(count, maxDailyTrades)))
            .orElseGet(() -> Validation.valid(null));
    }

    /**
     * Check buying power
     * Rule #25: Circuit breaker via PortfolioServiceClient
     * Rule #11: Result monad error handling
     */
    private Validation<OrderRequest, RiskError> checkBuyingPower(
            Long userId,
            BigDecimal orderValue) {

        // Get portfolio impact from portfolio service (with circuit breaker)
        Result<PortfolioImpact, ServiceError> impactResult =
            portfolioClient.calculateImpact(userId, "PORTFOLIO", 0, orderValue);

        return switch (impactResult) {
            case Result.Success<PortfolioImpact, ServiceError> success -> {
                PortfolioImpact impact = success.value();

                // Calculate required buying power with buffer
                BigDecimal requiredWithBuffer = orderValue.multiply(
                    BigDecimal.ONE.add(minBuyingPowerBuffer));

                // Check if order value is acceptable - eliminates ternary using Optional
                yield Optional.of(impact.acceptable() && impact.marginImpactAcceptable())
                    .filter(Boolean::booleanValue)
                    .map(acceptable -> Validation.<OrderRequest, RiskError>valid(null))
                    .orElseGet(() -> Validation.invalid(RiskError.insufficientBuyingPower(
                        requiredWithBuffer, impact.newPortfolioValue())));
            }

            case Result.Failure<PortfolioImpact, ServiceError> failure ->
                Validation.invalid(RiskError.systemError(
                    "Failed to check buying power",
                    failure.error().toString()));
        };
    }

    /**
     * Check position limits
     * Rule #25: Circuit breaker via PortfolioServiceClient
     * Rule #14: Pattern matching with Result
     */
    private Validation<OrderRequest, RiskError> checkPositionLimits(
            Long userId,
            OrderRequest request) {

        Result<PositionRisk, ServiceError> riskResult =
            portfolioClient.getPositionRisk(userId, request.symbol());

        return switch (riskResult) {
            case Result.Success<PositionRisk, ServiceError> success -> {
                PositionRisk risk = success.value();

                // Calculate new position size
                BigDecimal newPosition = risk.currentPosition()
                    .add(BigDecimal.valueOf(request.quantity()));

                // Check if within limits - eliminates ternary using Optional
                yield Optional.of(newPosition.compareTo(risk.maxPositionSize()) <= 0)
                    .filter(Boolean::booleanValue)
                    .map(withinLimit -> Validation.<OrderRequest, RiskError>valid(request))
                    .orElseGet(() -> Validation.invalid(RiskError.positionLimitExceeded(
                        request.symbol(),
                        newPosition,
                        risk.maxPositionSize())));
            }

            case Result.Failure<PositionRisk, ServiceError> failure ->
                Validation.invalid(RiskError.systemError(
                    "Failed to check position limits",
                    failure.error().toString()));
        };
    }

    /**
     * Check concentration risk
     * Rule #25: Circuit breaker via PortfolioServiceClient
     * Rule #14: Pattern matching
     */
    private Validation<OrderRequest, RiskError> checkConcentrationRisk(
            Long userId,
            OrderRequest request,
            BigDecimal orderValue) {

        Result<PortfolioImpact, ServiceError> impactResult =
            portfolioClient.calculateImpact(
                userId,
                request.symbol(),
                request.quantity(),
                orderValue);

        return switch (impactResult) {
            case Result.Success<PortfolioImpact, ServiceError> success -> {
                PortfolioImpact impact = success.value();

                // Check concentration percentage - eliminates ternary using Optional
                yield Optional.of(impact.newConcentration().compareTo(maxConcentrationPercent) <= 0)
                    .filter(Boolean::booleanValue)
                    .map(acceptable -> Validation.<OrderRequest, RiskError>valid(request))
                    .orElseGet(() -> Validation.invalid(RiskError.concentrationRiskExceeded(
                        request.symbol(),
                        impact.newConcentration(),
                        maxConcentrationPercent)));
            }

            case Result.Failure<PortfolioImpact, ServiceError> failure ->
                Validation.invalid(RiskError.systemError(
                    "Failed to check concentration risk",
                    failure.error().toString()));
        };
    }

    /**
     * Check margin requirements
     * Rule #25: Circuit breaker via PortfolioServiceClient
     * Rule #14: Pattern matching
     */
    private Validation<OrderRequest, RiskError> checkMarginRequirements(
            Long userId,
            BigDecimal orderValue) {

        Result<PortfolioImpact, ServiceError> impactResult =
            portfolioClient.calculateImpact(userId, "MARGIN", 0, orderValue);

        return switch (impactResult) {
            case Result.Success<PortfolioImpact, ServiceError> success -> {
                PortfolioImpact impact = success.value();

                // Check margin usage - eliminates ternary using Optional
                yield Optional.of(impact.marginImpact().compareTo(maxMarginUsage) <= 0)
                    .filter(Boolean::booleanValue)
                    .map(acceptable -> Validation.<OrderRequest, RiskError>valid(null))
                    .orElseGet(() -> Validation.invalid(RiskError.marginRequirementNotMet(
                        orderValue,
                        impact.newPortfolioValue())));
            }

            case Result.Failure<PortfolioImpact, ServiceError> failure ->
                Validation.invalid(RiskError.systemError(
                    "Failed to check margin requirements",
                    failure.error().toString()));
        };
    }

    /**
     * Get daily trade count for user
     * Rule #3: Functional programming with Optional
     * Rule #12: Thread-safe with AtomicInteger
     */
    private int getDailyTradeCount(Long userId) {
        // Reset counter if new day
        resetCounterIfNewDay(userId);

        // Return current count
        return Optional.ofNullable(dailyTradeCounters.get(userId))
            .map(AtomicInteger::get)
            .orElse(0);
    }

    /**
     * Increment daily trade count
     * Rule #12: Thread-safe with AtomicInteger
     */
    private void incrementDailyTradeCount(Long userId) {
        resetCounterIfNewDay(userId);
        dailyTradeCounters
            .computeIfAbsent(userId, k -> new AtomicInteger(0))
            .incrementAndGet();
    }

    /**
     * Reset counter if new day
     * Rule #3: Functional programming with Optional
     */
    private void resetCounterIfNewDay(Long userId) {
        LocalDate today = LocalDate.now();

        Optional.ofNullable(lastResetDates.get(userId))
            .filter(lastReset -> lastReset.isBefore(today))
            .ifPresent(lastReset -> {
                dailyTradeCounters.put(userId, new AtomicInteger(0));
                lastResetDates.put(userId, today);
            });

        // Initialize if not exists
        lastResetDates.putIfAbsent(userId, today);
    }

    /**
     * Adapt Validation result to RiskCheckResult
     * Rule #14: Pattern matching with switch expression
     * Rule #13: Stream API for functional processing
     */
    private RiskCheckResult adaptToRiskCheckResult(
            Validation<OrderRequest, RiskError> validation,
            Long userId) {

        return switch (validation) {
            case Validation.Valid<OrderRequest, RiskError> v -> {
                // All checks passed
                incrementDailyTradeCount(userId);

                yield RiskCheckResult.builder()
                    .passed(true)
                    .riskScore(0.0)
                    .violations(List.of())
                    .warnings(List.of())
                    .engineName(getEngineName())
                    .build();
            }

            case Validation.Invalid<OrderRequest, RiskError> i -> {
                // Convert errors to violations
                List<RiskViolation> violations = i.errors().stream()
                    .map(this::convertToViolation)
                    .toList();

                // Calculate risk score based on severity
                double riskScore = calculateRiskScore(violations);

                yield RiskCheckResult.builder()
                    .passed(false)
                    .riskScore(riskScore)
                    .violations(violations)
                    .warnings(List.of())
                    .engineName(getEngineName())
                    .build();
            }
        };
    }

    /**
     * Convert RiskError to RiskViolation
     * Rule #14: Pattern matching with switch expression
     */
    private RiskViolation convertToViolation(RiskError error) {
        return switch (error) {
            case RiskError.InsufficientBuyingPowerError e ->
                RiskViolation.builder()
                    .type(RiskViolationType.INSUFFICIENT_BUYING_POWER)
                    .message(e.message())
                    .severity(convertSeverity(e.severity()))
                    .currentValue(e.available().toString())
                    .limitValue(e.required().toString())
                    .ruleCode(e.code())
                    .build();

            case RiskError.PositionLimitError e ->
                RiskViolation.builder()
                    .type(RiskViolationType.POSITION_LIMIT_EXCEEDED)
                    .message(e.message())
                    .severity(convertSeverity(e.severity()))
                    .currentValue(e.currentPosition().toString())
                    .limitValue(e.maxPosition().toString())
                    .ruleCode(e.code())
                    .build();

            case RiskError.OrderValueLimitError e ->
                RiskViolation.builder()
                    .type(RiskViolationType.ORDER_VALUE_EXCEEDED)
                    .message(e.message())
                    .severity(convertSeverity(e.severity()))
                    .currentValue(e.orderValue().toString())
                    .limitValue(e.maxOrderValue().toString())
                    .ruleCode(e.code())
                    .build();

            case RiskError.DailyTradeLimitError e ->
                RiskViolation.builder()
                    .type(RiskViolationType.DAILY_TRADING_LIMIT_EXCEEDED)
                    .message(e.message())
                    .severity(convertSeverity(e.severity()))
                    .currentValue(e.currentTrades().toString())
                    .limitValue(e.maxTrades().toString())
                    .ruleCode(e.code())
                    .build();

            case RiskError.MarginRequirementError e ->
                RiskViolation.builder()
                    .type(RiskViolationType.MARGIN_REQUIREMENT)
                    .message(e.message())
                    .severity(convertSeverity(e.severity()))
                    .currentValue(e.availableMargin().toString())
                    .limitValue(e.requiredMargin().toString())
                    .ruleCode(e.code())
                    .build();

            case RiskError.ConcentrationRiskError e ->
                RiskViolation.builder()
                    .type(RiskViolationType.CONCENTRATION_RISK_EXCEEDED)
                    .message(e.message())
                    .severity(convertSeverity(e.severity()))
                    .currentValue(e.positionPercentage().toString())
                    .limitValue(e.maxPercentage().toString())
                    .ruleCode(e.code())
                    .build();

            case RiskError.SystemError e ->
                RiskViolation.builder()
                    .type(RiskViolationType.SYSTEM_ERROR)
                    .message(e.message())
                    .severity(RiskSeverity.CRITICAL)
                    .ruleCode(e.code())
                    .build();
        };
    }

    /**
     * Convert RiskError.RiskSeverity to RiskViolation.RiskSeverity
     * Rule #14: Pattern matching
     */
    private RiskSeverity convertSeverity(RiskError.RiskSeverity severity) {
        return switch (severity) {
            case LOW -> RiskSeverity.LOW;
            case MEDIUM -> RiskSeverity.MEDIUM;
            case HIGH -> RiskSeverity.HIGH;
            case CRITICAL -> RiskSeverity.CRITICAL;
        };
    }

    /**
     * Calculate risk score from violations
     * Rule #13: Stream API for functional processing
     */
    private double calculateRiskScore(List<RiskViolation> violations) {
        return violations.stream()
            .mapToDouble(v -> switch (v.getSeverity()) {
                case LOW -> 0.25;
                case MEDIUM -> 0.5;
                case HIGH -> 0.75;
                case CRITICAL -> 1.0;
            })
            .max()
            .orElse(0.0);
    }

    /**
     * Record risk check metrics
     * Rule #15: Structured logging and monitoring
     */
    private void recordRiskCheckMetrics(RiskCheckResult result, long durationNanos) {
        Timer.builder(RISK_CHECK_METRIC)
            .tag("engine", getEngineName())
            .tag("passed", String.valueOf(result.isPassed()))
            .description("Risk check processing time")
            .register(meterRegistry)
            .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        // Record violations by type
        result.getViolations().forEach(violation ->
            meterRegistry.counter(RISK_VIOLATION_METRIC,
                "engine", getEngineName(),
                "type", violation.getType().name(),
                "severity", violation.getSeverity().name()
            ).increment()
        );
    }

    /**
     * Get engine name
     */
    private String getEngineName() {
        return "FunctionalRiskCheckEngine";
    }
}
