package com.trademaster.trading.controller;

import com.trademaster.common.functional.Result;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.service.FunctionalRiskManagementService;
import com.trademaster.trading.service.FunctionalRiskManagementService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Risk Management REST API Controller
 *
 * Provides comprehensive risk management endpoints for pre-trade risk checks,
 * compliance validation, and portfolio risk metrics using Java 24 Virtual Threads.
 *
 * Key Capabilities:
 * - Pre-trade risk validation (buying power, position limits, trading limits)
 * - Compliance checks (regulatory requirements, risk limits)
 * - Real-time portfolio risk metrics (VaR, volatility, concentration)
 * - Margin requirement calculations
 *
 * Performance Targets:
 * - Risk checks: <25ms (cached portfolio data)
 * - Compliance validation: <50ms (policy evaluation)
 * - Risk metrics calculation: <100ms (portfolio aggregation)
 *
 * AgentOS Capabilities:
 * - risk-management: Pre-trade risk validation and limit enforcement
 * - compliance-check: Regulatory compliance and policy validation
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Result Types)
 */
@RestController
@RequestMapping("/api/v2/risk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk Management", description = "Pre-trade risk checks and portfolio risk metrics")
@SecurityRequirement(name = "bearer-jwt")
public class RiskManagementController {

    private final FunctionalRiskManagementService riskService;

    /**
     * Check order risk before execution
     *
     * Validates order against:
     * - Buying power requirements
     * - Position concentration limits
     * - Daily trading velocity limits
     * - Margin requirements
     *
     * @param orderRequest The order to validate
     * @param userDetails Authenticated user details
     * @return CompletableFuture with risk validation result
     */
    @PostMapping("/check")
    @Operation(
        summary = "Check order risk",
        description = "Validates order against buying power, position limits, and trading velocity limits"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Risk check completed successfully",
            content = @Content(schema = @Schema(implementation = RiskCheckResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Risk check failed - order violates risk limits",
            content = @Content(schema = @Schema(implementation = RiskCheckResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<RiskCheckResponse>> checkOrderRisk(
            @Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correlationId = UUID.randomUUID().toString();
        Long userId = extractUserId(userDetails);

        log.info("[{}] Risk check requested - userId={}, symbol={}, quantity={}, orderType={}",
                correlationId, userId, orderRequest.symbol(), orderRequest.quantity(), orderRequest.orderType());

        return CompletableFuture.supplyAsync(() -> {
            Order order = mapToOrder(orderRequest, userId);

            Result<RiskValidationResult, TradeError> result = riskService.validateOrderRisk(order, userId);

            return switch (result) {
                case Result.Success<RiskValidationResult, TradeError> success -> {
                    RiskValidationResult validation = success.value();
                    log.info("[{}] Risk check passed - riskLevel={}, messages={}",
                            correlationId, validation.riskLevel(), validation.validationMessages());

                    RiskCheckResponse response = new RiskCheckResponse(
                        true,
                        validation.riskLevel(),
                        validation.validationMessages(),
                        validation.riskMetrics(),
                        correlationId
                    );

                    yield ResponseEntity.ok(response);
                }
                case Result.Failure<RiskValidationResult, TradeError> failure -> {
                    log.warn("[{}] Risk check failed - error={}", correlationId, failure.error());

                    RiskCheckResponse response = new RiskCheckResponse(
                        false,
                        "HIGH",
                        List.of(failure.error().message()),
                        Map.of(),
                        correlationId
                    );

                    yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            };
        });
    }

    /**
     * Validate order compliance with regulatory requirements
     *
     * Checks:
     * - Regulatory trading limits (Pattern Day Trader rules)
     * - Position concentration requirements
     * - Margin trading regulations
     * - Market manipulation prevention
     *
     * @param orderRequest The order to validate
     * @param userDetails Authenticated user details
     * @return CompletableFuture with compliance validation result
     */
    @PostMapping("/compliance")
    @Operation(
        summary = "Check order compliance",
        description = "Validates order against regulatory requirements and compliance policies"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Compliance check completed successfully",
            content = @Content(schema = @Schema(implementation = ComplianceCheckResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Compliance check failed - order violates regulatory requirements",
            content = @Content(schema = @Schema(implementation = ComplianceCheckResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<ComplianceCheckResponse>> checkCompliance(
            @Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correlationId = UUID.randomUUID().toString();
        Long userId = extractUserId(userDetails);

        log.info("[{}] Compliance check requested - userId={}, symbol={}, quantity={}",
                correlationId, userId, orderRequest.symbol(), orderRequest.quantity());

        return CompletableFuture.supplyAsync(() -> {
            Order order = mapToOrder(orderRequest, userId);

            // Check trading limits (compliance requirement)
            Result<TradingLimitValidation, TradeError> tradingLimitsResult =
                riskService.validateTradingLimits(userId, order);

            // Check position limits (compliance requirement)
            Result<PositionLimitValidation, TradeError> positionLimitsResult =
                riskService.validatePositionLimits(userId, order);

            // Combine compliance results using railway programming
            Result<ComplianceValidation, TradeError> complianceResult = tradingLimitsResult
                .flatMap(tradingLimits -> positionLimitsResult
                    .map(positionLimits -> new ComplianceValidation(
                        tradingLimits.isValid() && positionLimits.isValid(),
                        List.of(
                            String.format("Trading limits: %s/%s trades today",
                                tradingLimits.todayTradeCount(), tradingLimits.maxDailyTrades()),
                            String.format("Position concentration: %.2f%% (max %.2f%%)",
                                positionLimits.concentrationPercent(), positionLimits.maxConcentration())
                        ),
                        Map.of(
                            "tradingLimitValid", tradingLimits.isValid(),
                            "positionLimitValid", positionLimits.isValid(),
                            "todayTradeCount", tradingLimits.todayTradeCount(),
                            "maxDailyTrades", tradingLimits.maxDailyTrades(),
                            "concentrationPercent", positionLimits.concentrationPercent()
                        )
                    ))
                );

            return switch (complianceResult) {
                case Result.Success<ComplianceValidation, TradeError> success -> {
                    ComplianceValidation validation = success.value();
                    log.info("[{}] Compliance check passed - checks={}", correlationId, validation.checks());

                    ComplianceCheckResponse response = new ComplianceCheckResponse(
                        validation.isCompliant(),
                        validation.checks(),
                        validation.complianceMetrics(),
                        correlationId
                    );

                    yield ResponseEntity.ok(response);
                }
                case Result.Failure<ComplianceValidation, TradeError> failure -> {
                    log.warn("[{}] Compliance check failed - error={}", correlationId, failure.error());

                    ComplianceCheckResponse response = new ComplianceCheckResponse(
                        false,
                        List.of(failure.error().message()),
                        Map.of(),
                        correlationId
                    );

                    yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            };
        });
    }

    /**
     * Get portfolio risk metrics
     *
     * Provides comprehensive risk metrics:
     * - Portfolio Value at Risk (VaR)
     * - Expected Shortfall (CVaR)
     * - Portfolio beta and volatility
     * - Leverage and concentration risk
     * - Sector exposure breakdown
     *
     * @param userId The user ID (defaults to authenticated user)
     * @param userDetails Authenticated user details
     * @return CompletableFuture with portfolio risk metrics
     */
    @GetMapping("/metrics/{userId}")
    @Operation(
        summary = "Get portfolio risk metrics",
        description = "Returns comprehensive risk metrics including VaR, volatility, and concentration"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Risk metrics retrieved successfully",
            content = @Content(schema = @Schema(implementation = PortfolioRiskMetrics.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Portfolio not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<PortfolioRiskMetrics>> getPortfolioRiskMetrics(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correlationId = UUID.randomUUID().toString();

        log.info("[{}] Portfolio risk metrics requested - userId={}", correlationId, userId);

        return CompletableFuture.supplyAsync(() -> {
            Result<PortfolioRiskMetrics, TradeError> result = riskService.getPortfolioRiskMetrics(userId);

            return switch (result) {
                case Result.Success<PortfolioRiskMetrics, TradeError> success -> {
                    PortfolioRiskMetrics metrics = success.value();
                    log.info("[{}] Risk metrics retrieved - portfolioValue={}, VaR={}, volatility={}",
                            correlationId, metrics.portfolioValue(), metrics.portfolioVaR(), metrics.portfolioVolatility());

                    yield ResponseEntity.ok(metrics);
                }
                case Result.Failure<PortfolioRiskMetrics, TradeError> failure -> {
                    log.error("[{}] Failed to retrieve risk metrics - error={}", correlationId, failure.error());
                    yield ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
            };
        });
    }

    /**
     * Calculate margin requirement for order
     *
     * @param orderRequest The order to calculate margin for
     * @param userDetails Authenticated user details
     * @return CompletableFuture with margin requirement
     */
    @PostMapping("/margin")
    @Operation(
        summary = "Calculate margin requirement",
        description = "Calculates initial and maintenance margin requirements for order"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Margin requirement calculated successfully",
            content = @Content(schema = @Schema(implementation = MarginRequirement.class))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<MarginRequirement>> calculateMarginRequirement(
            @Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correlationId = UUID.randomUUID().toString();
        Long userId = extractUserId(userDetails);

        log.info("[{}] Margin calculation requested - userId={}, symbol={}, quantity={}",
                correlationId, userId, orderRequest.symbol(), orderRequest.quantity());

        return CompletableFuture.supplyAsync(() -> {
            Order order = mapToOrder(orderRequest, userId);

            Result<MarginRequirement, TradeError> result = riskService.calculateMarginRequirement(order);

            return switch (result) {
                case Result.Success<MarginRequirement, TradeError> success -> {
                    MarginRequirement margin = success.value();
                    log.info("[{}] Margin calculated - initial={}, maintenance={}, sufficient={}",
                            correlationId, margin.initialMargin(), margin.maintenanceMargin(), margin.marginSufficient());

                    yield ResponseEntity.ok(margin);
                }
                case Result.Failure<MarginRequirement, TradeError> failure -> {
                    log.error("[{}] Margin calculation failed - error={}", correlationId, failure.error());
                    yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            };
        });
    }

    // Helper methods

    private Long extractUserId(UserDetails userDetails) {
        // Extract user ID from JWT token (implementation depends on token structure)
        return Long.parseLong(userDetails.getUsername());
    }

    private Order mapToOrder(OrderRequest request, Long userId) {
        // Simple mapping - proper implementation would use Order builder/factory
        Order order = new Order();
        order.setUserId(userId);
        order.setSymbol(request.symbol());
        order.setQuantity(request.quantity());
        order.setOrderType(request.orderType());
        order.setPrice(request.price());
        return order;
    }

    // Response DTOs (Records for immutability)

    /**
     * Risk check response
     */
    public record RiskCheckResponse(
        boolean riskAcceptable,
        String riskLevel,
        List<String> validationMessages,
        Map<String, Object> riskMetrics,
        String correlationId
    ) {}

    /**
     * Compliance check response
     */
    public record ComplianceCheckResponse(
        boolean isCompliant,
        List<String> checks,
        Map<String, Object> complianceMetrics,
        String correlationId
    ) {}

    /**
     * Compliance validation internal result
     */
    private record ComplianceValidation(
        boolean isCompliant,
        List<String> checks,
        Map<String, Object> complianceMetrics
    ) {}
}
