package com.trademaster.trading.controller;

import com.trademaster.common.functional.Result;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.ErrorResponse;
import com.trademaster.trading.dto.PositionSnapshot;
import com.trademaster.trading.entity.Position;
import com.trademaster.trading.security.TradingUserPrincipal;
import com.trademaster.trading.service.PositionManagementService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Position Management Controller
 *
 * REST API endpoints for position tracking, P&L calculation, and position analytics.
 * Uses Java 24 Virtual Threads for unlimited scalability with simple blocking I/O.
 *
 * MANDATORY: Rule #2 - SOLID Principles (SRP, DIP)
 * MANDATORY: Rule #3 - Functional Programming (no if-else, pattern matching)
 * MANDATORY: Rule #11 - Error Handling Patterns (Result monad, railway programming)
 * MANDATORY: Rule #12 - Virtual Threads & Concurrency (CompletableFuture with Virtual Threads)
 * MANDATORY: Rule #15 - Structured Logging (correlation IDs, @Slf4j)
 *
 * Performance Targets (Java 24 + Virtual Threads):
 * - Position retrieval: <25ms response time
 * - P&L calculation: <50ms response time
 * - Position updates: <10ms response time
 * - Concurrent support: 10,000+ users (unlimited Virtual Threads)
 *
 * Security:
 * - JWT authentication required for all endpoints
 * - User-specific data isolation
 * - Rate limiting and input validation
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@RestController
@RequestMapping("/api/v1/trading/positions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Position API", description = "Position tracking and P&L calculation endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PositionController {

    private final PositionManagementService positionManagementService;

    /**
     * Get all positions for authenticated user
     * Rule #11: Functional error handling with Result monad
     * Rule #12: Async with CompletableFuture + Virtual Threads
     * Redis cache: 30-second TTL for real-time position data
     */
    @GetMapping
    @Cacheable(value = "positions", key = "#principal.userId")
    @Operation(
        summary = "Get all positions",
        description = "Retrieve all positions for the authenticated user with real-time P&L calculations",
        tags = {"Positions"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Positions retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "array", implementation = Position.class),
                examples = @ExampleObject(
                    name = "Position List",
                    value = "[{\"symbol\":\"AAPL\",\"quantity\":100,\"avgPrice\":150.00,\"currentPrice\":155.00,\"unrealizedPnL\":500.00}]"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required - invalid JWT token"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<?>> getAllPositions(
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal,
            @Parameter(hidden = true) HttpServletRequest request) {

        Long userId = principal.getUserId();
        String correlationId = generateCorrelationId();

        log.info("Retrieving all positions - userId: {}, correlationId: {}", userId, correlationId);

        // Async processing with Virtual Threads - no blocking
        // Functional error handling with pattern matching (Rule #3, #11)
        return positionManagementService.getAllPositions(userId)
            .handle((positions, ex) ->
                Optional.ofNullable(ex)
                    .map(error -> {
                        log.error("Failed to retrieve positions - userId: {}, correlationId: {}, error: {}",
                                 userId, correlationId, error.getMessage());

                        ErrorResponse errorResponse = ErrorResponse.genericError(
                            "Failed to retrieve positions: " + error.getMessage(),
                            request.getRequestURI(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            correlationId
                        );
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) errorResponse);
                    })
                    .orElseGet(() -> {
                        log.info("Retrieved {} positions - userId: {}, correlationId: {}",
                                positions.size(), userId, correlationId);
                        return ResponseEntity.ok((Object) positions);
                    })
            );
    }

    /**
     * Get specific position by symbol
     * Rule #11: Functional error handling
     * Rule #14: Pattern matching for error codes
     * Redis cache: 30-second TTL for position data
     */
    @GetMapping("/{symbol}")
    @Cacheable(value = "positions", key = "#principal.userId + '-' + #symbol")
    @Operation(
        summary = "Get position by symbol",
        description = "Retrieve detailed position information for a specific symbol with real-time P&L",
        tags = {"Positions"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Position retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Position.class)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Position not found for this symbol"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public CompletableFuture<ResponseEntity<?>> getPositionBySymbol(
            @Parameter(
                name = "symbol",
                description = "Trading symbol",
                required = true,
                example = "AAPL"
            ) @PathVariable String symbol,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal,
            @Parameter(hidden = true) HttpServletRequest request) {

        Long userId = principal.getUserId();
        String correlationId = generateCorrelationId();

        log.info("Retrieving position - userId: {}, symbol: {}, correlationId: {}",
                userId, symbol, correlationId);

        // Functional error handling with pattern matching (Rule #3, #11, #14)
        return positionManagementService.getPosition(userId, symbol)
            .handle((position, ex) ->
                Optional.ofNullable(ex)
                    .map(error -> {
                        log.warn("Position not found - userId: {}, symbol: {}, correlationId: {}, error: {}",
                                userId, symbol, correlationId, error.getMessage());

                        // Pattern matching for error status (Rule #14)
                        int status = switch (error.getMessage()) {
                            case String msg when msg.contains("not found") -> HttpStatus.NOT_FOUND.value();
                            case String msg when msg.contains("access denied") -> HttpStatus.FORBIDDEN.value();
                            default -> HttpStatus.INTERNAL_SERVER_ERROR.value();
                        };

                        ErrorResponse errorResponse = ErrorResponse.genericError(
                            "Failed to retrieve position: " + error.getMessage(),
                            request.getRequestURI(),
                            status,
                            correlationId
                        );
                        return ResponseEntity.status(status).body((Object) errorResponse);
                    })
                    .orElseGet(() -> {
                        log.info("Position retrieved - userId: {}, symbol: {}, correlationId: {}",
                                userId, symbol, correlationId);
                        return ResponseEntity.ok((Object) position);
                    })
            );
    }

    /**
     * Get position snapshot with comprehensive analytics
     * Rule #12: Async operations with Virtual Threads
     * Redis cache: 30-second TTL for snapshots
     */
    @GetMapping("/{symbol}/snapshot")
    @Cacheable(value = "position-snapshots", key = "#principal.userId + '-' + #symbol")
    @Operation(
        summary = "Get position snapshot with analytics",
        description = "Retrieve comprehensive position analytics including P&L, risk metrics, and performance data",
        tags = {"Positions", "Analytics"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Position snapshot retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PositionSnapshot.class)
        )
    )
    public CompletableFuture<ResponseEntity<?>> getPositionSnapshot(
            @Parameter(
                name = "symbol",
                description = "Trading symbol",
                required = true,
                example = "AAPL"
            ) @PathVariable String symbol,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal,
            @Parameter(hidden = true) HttpServletRequest request) {

        Long userId = principal.getUserId();
        String correlationId = generateCorrelationId();

        log.info("Retrieving position snapshot - userId: {}, symbol: {}, correlationId: {}",
                userId, symbol, correlationId);

        // Functional error handling (Rule #3, #11)
        return positionManagementService.getPositionSnapshot(userId, symbol)
            .handle((snapshot, ex) ->
                Optional.ofNullable(ex)
                    .map(error -> {
                        log.error("Failed to retrieve position snapshot - userId: {}, symbol: {}, correlationId: {}, error: {}",
                                 userId, symbol, correlationId, error.getMessage());

                        ErrorResponse errorResponse = ErrorResponse.genericError(
                            "Failed to retrieve position snapshot: " + error.getMessage(),
                            request.getRequestURI(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            correlationId
                        );
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) errorResponse);
                    })
                    .orElseGet(() -> {
                        log.info("Position snapshot retrieved - userId: {}, symbol: {}, correlationId: {}",
                                userId, symbol, correlationId);
                        return ResponseEntity.ok((Object) snapshot);
                    })
            );
    }

    /**
     * Calculate P&L for specific position
     * Rule #3: Functional programming with CompletableFuture chains
     * Redis cache: 30-second TTL for P&L data
     */
    @GetMapping("/{symbol}/pnl")
    @Cacheable(value = "position-pnl", key = "#principal.userId + '-' + #symbol + '-' + #currentPrice")
    @Operation(
        summary = "Calculate position P&L",
        description = "Calculate real-time profit and loss for a specific position",
        tags = {"Positions", "Analytics"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "P&L calculated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(type = "object"),
            examples = @ExampleObject(
                name = "P&L Breakdown",
                value = "{\"unrealizedPnL\":500.00,\"realizedPnL\":200.00,\"totalPnL\":700.00,\"pnlPercent\":4.67}"
            )
        )
    )
    public CompletableFuture<ResponseEntity<?>> calculatePositionPnL(
            @Parameter(
                name = "symbol",
                description = "Trading symbol",
                required = true,
                example = "AAPL"
            ) @PathVariable String symbol,
            @Parameter(
                name = "currentPrice",
                description = "Current market price for P&L calculation",
                required = true,
                example = "155.00"
            ) @RequestParam BigDecimal currentPrice,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal,
            @Parameter(hidden = true) HttpServletRequest request) {

        Long userId = principal.getUserId();
        String correlationId = generateCorrelationId();

        log.info("Calculating P&L - userId: {}, symbol: {}, price: {}, correlationId: {}",
                userId, symbol, currentPrice, correlationId);

        // Functional error handling (Rule #3, #11)
        return positionManagementService.calculatePositionPnL(userId, symbol, currentPrice)
            .handle((pnl, ex) ->
                Optional.ofNullable(ex)
                    .map(error -> {
                        log.error("Failed to calculate P&L - userId: {}, symbol: {}, correlationId: {}, error: {}",
                                 userId, symbol, correlationId, error.getMessage());

                        ErrorResponse errorResponse = ErrorResponse.genericError(
                            "Failed to calculate P&L: " + error.getMessage(),
                            request.getRequestURI(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            correlationId
                        );
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) errorResponse);
                    })
                    .orElseGet(() -> {
                        log.info("P&L calculated - userId: {}, symbol: {}, correlationId: {}",
                                userId, symbol, correlationId);
                        return ResponseEntity.ok((Object) pnl);
                    })
            );
    }

    /**
     * Calculate P&L for all positions
     * Rule #12: Parallel processing with Virtual Threads
     */
    @GetMapping("/pnl/all")
    @Operation(
        summary = "Calculate P&L for all positions",
        description = "Calculate real-time profit and loss for all user positions",
        tags = {"Positions", "Analytics"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "P&L calculated for all positions",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(type = "object")
        )
    )
    public CompletableFuture<ResponseEntity<?>> calculateAllPositionsPnL(
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal,
            @Parameter(hidden = true) HttpServletRequest request) {

        Long userId = principal.getUserId();
        String correlationId = generateCorrelationId();

        log.info("Calculating all positions P&L - userId: {}, correlationId: {}",
                userId, correlationId);

        // Functional error handling (Rule #3, #11)
        return positionManagementService.calculateAllPositionsPnL(userId)
            .handle((pnlMap, ex) ->
                Optional.ofNullable(ex)
                    .map(error -> {
                        log.error("Failed to calculate all positions P&L - userId: {}, correlationId: {}, error: {}",
                                 userId, correlationId, error.getMessage());

                        ErrorResponse errorResponse = ErrorResponse.genericError(
                            "Failed to calculate all positions P&L: " + error.getMessage(),
                            request.getRequestURI(),
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            correlationId
                        );
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) errorResponse);
                    })
                    .orElseGet(() -> {
                        log.info("All positions P&L calculated - userId: {}, correlationId: {}",
                                userId, correlationId);
                        return ResponseEntity.ok((Object) pnlMap);
                    })
            );
    }

    /**
     * Get positions by asset class
     * Rule #3: Functional filtering with Stream API
     */
    @GetMapping("/filter/asset-class/{assetClass}")
    @Operation(
        summary = "Get positions by asset class",
        description = "Filter positions by asset class (EQUITY, DERIVATIVE, COMMODITY, etc.)",
        tags = {"Positions", "Filter"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Filtered positions retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(type = "array", implementation = Position.class)
        )
    )
    public CompletableFuture<ResponseEntity<?>> getPositionsByAssetClass(
            @Parameter(
                name = "assetClass",
                description = "Asset class filter",
                required = true,
                example = "EQUITY"
            ) @PathVariable String assetClass,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal) {

        Long userId = principal.getUserId();
        String correlationId = generateCorrelationId();

        log.info("Retrieving positions by asset class - userId: {}, assetClass: {}, correlationId: {}",
                userId, assetClass, correlationId);

        return positionManagementService.getPositionsByAssetClass(userId, assetClass)
            .thenApply(positions -> {
                log.info("Retrieved {} positions for asset class {} - userId: {}, correlationId: {}",
                        positions.size(), assetClass, userId, correlationId);
                return ResponseEntity.ok(positions);
            });
    }

    /**
     * Generate correlation ID for request tracing
     * Rule #15: Structured logging with correlation IDs
     */
    private String generateCorrelationId() {
        return "PC-" + System.currentTimeMillis() + "-" + Thread.currentThread().threadId();
    }
}
