package com.trademaster.trading.controller;

import com.trademaster.trading.common.Result;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.ErrorResponse;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.security.TradingUserPrincipal;
import com.trademaster.trading.service.OrderService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Trading Controller
 * 
 * REST API endpoints for order placement, management, and tracking.
 * Uses Java 24 Virtual Threads for unlimited scalability with simple blocking I/O.
 * 
 * Performance Targets (Java 24 + Virtual Threads):
 * - Order placement: <50ms response time
 * - Order queries: <25ms response time  
 * - Concurrent support: 10,000+ users (unlimited Virtual Threads)
 * - Memory usage: ~8KB per thread (vs 2MB platform threads)
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
@RequestMapping("/api/v2/orders")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Trading API", description = "Order placement and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TradingController {
    
    private final OrderService orderService;
    
    /**
     * Place a new order (Blocking I/O with Virtual Threads)
     */
    @PostMapping
    @Operation(
        summary = "Place new order",
        description = "Submit a new trading order with comprehensive validation, risk checks, and position management",
        tags = {"Orders"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Order details including symbol, side, quantity, price, and order type",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Market Buy Order",
                        description = "Simple market buy order for 100 shares",
                        value = "{\"symbol\":\"AAPL\",\"side\":\"BUY\",\"quantity\":100,\"orderType\":\"MARKET\"}"
                    ),
                    @ExampleObject(
                        name = "Limit Sell Order",
                        description = "Limit sell order with specific price",
                        value = "{\"symbol\":\"TSLA\",\"side\":\"SELL\",\"quantity\":50,\"orderType\":\"LIMIT\",\"limitPrice\":250.00}"
                    )
                }
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Order placed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class),
                examples = @ExampleObject(
                    name = "Successful Order Response",
                    value = "{\"orderId\":\"ORD-12345\",\"status\":\"ACKNOWLEDGED\",\"symbol\":\"AAPL\",\"side\":\"BUY\",\"quantity\":100,\"filledQuantity\":0,\"avgPrice\":null,\"timestamp\":\"2024-01-15T10:30:00Z\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid order request - validation failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required - invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions or risk check failed"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded - too many requests")
    })
    public ResponseEntity<?> placeOrder(
            @Parameter(hidden = true) @Valid @RequestBody OrderRequest orderRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal,
            @Parameter(hidden = true) HttpServletRequest request) {
        
        Long userId = principal.getUserId();
        
        log.info("Placing order for user {}: {} {} {} @ {}", 
                userId, orderRequest.side(), orderRequest.quantity(), 
                orderRequest.symbol(), orderRequest.limitPrice());
        
        String correlationId = generateCorrelationId();
        
        try {
            // Simple blocking call - Virtual Thread handles concurrency
            Result<OrderResponse, TradeError> result = orderService.placeOrder(orderRequest, userId);
            
            return switch (result) {
                case Result.Success<OrderResponse, TradeError> success -> {
                    log.info("Order placed successfully - correlationId: {}, orderId: {}", 
                            correlationId, success.value().orderId());
                    yield ResponseEntity.status(HttpStatus.CREATED).body(success.value());
                }
                case Result.Failure<OrderResponse, TradeError> failure -> {
                    log.warn("Order placement failed - correlationId: {}, userId: {}, error: {}", 
                            correlationId, userId, failure.error().getMessage());
                    
                    ErrorResponse errorResponse = ErrorResponse.fromTradeError(
                        failure.error(), 
                        request.getRequestURI(), 
                        HttpStatus.BAD_REQUEST.value(), 
                        correlationId
                    );
                    yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }
            };
            
        } catch (Exception e) {
            log.error("Failed to place order - correlationId: {}, userId: {}, error: {}", 
                     correlationId, userId, e.getMessage());
            
            ErrorResponse errorResponse = ErrorResponse.genericError(
                "Internal server error during order placement",
                request.getRequestURI(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                correlationId
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get order by ID (High-performance lookup)
     */
    @GetMapping("/{orderId}")
    @Operation(
        summary = "Get order details",
        description = "Retrieve comprehensive information about a specific order including status, fills, and timestamps",
        tags = {"Orders"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Order found and returned successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Order not found or not accessible to user"),
        @ApiResponse(responseCode = "401", description = "Authentication required - invalid JWT token"),
        @ApiResponse(responseCode = "403", description = "Access denied - order belongs to another user")
    })
    public ResponseEntity<?> getOrder(
            @Parameter(
                name = "orderId",
                description = "Unique order identifier",
                required = true,
                example = "ORD-12345678",
                in = ParameterIn.PATH
            ) @PathVariable String orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal,
            @Parameter(hidden = true) HttpServletRequest request) {
        
        Long userId = principal.getUserId();
        
        String correlationId = generateCorrelationId();
        
        // Blocking call with Virtual Thread - no performance penalty
        Result<OrderResponse, TradeError> result = orderService.getOrder(orderId, userId);
        
        return switch (result) {
            case Result.Success<OrderResponse, TradeError> success -> ResponseEntity.ok(success.value());
            case Result.Failure<OrderResponse, TradeError> failure -> {
                log.warn("Order retrieval failed - correlationId: {}, orderId: {}, userId: {}, error: {}", 
                        correlationId, orderId, userId, failure.error().getMessage());
                
                int status = failure.error().getCode().equals("ENTITY_NOT_FOUND") ? 
                    HttpStatus.NOT_FOUND.value() : HttpStatus.BAD_REQUEST.value();
                    
                ErrorResponse errorResponse = ErrorResponse.fromTradeError(
                    failure.error(), 
                    request.getRequestURI(), 
                    status, 
                    correlationId
                );
                yield ResponseEntity.status(status).body(errorResponse);
            }
        };
    }
    
    /**
     * Get user's order history (Paginated with Virtual Threads)
     */
    @GetMapping
    @Operation(
        summary = "Get order history",
        description = "Retrieve paginated list of user's historical and current orders with optional filtering",
        tags = {"Orders"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Orders retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "array", implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required - invalid JWT token")
    })
    public List<OrderResponse> getOrderHistory(
            @Parameter(
                name = "page",
                description = "Page number for pagination (0-based)",
                example = "0",
                in = ParameterIn.QUERY
            ) @RequestParam(defaultValue = "0") int page,
            @Parameter(
                name = "size",
                description = "Number of orders per page (maximum 100)",
                example = "20",
                in = ParameterIn.QUERY
            ) @RequestParam(defaultValue = "20") int size,
            @Parameter(
                name = "status",
                description = "Filter orders by status",
                example = "FILLED",
                schema = @Schema(implementation = OrderStatus.class),
                in = ParameterIn.QUERY
            ) @RequestParam(required = false) OrderStatus status,
            @Parameter(
                name = "symbol",
                description = "Filter orders by trading symbol",
                example = "AAPL",
                in = ParameterIn.QUERY
            ) @RequestParam(required = false) String symbol,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        // Functional page size validation
        int validatedSize = Optional.of(size)
            .filter(s -> s <= 100)
            .orElse(100);
        
        Pageable pageable = PageRequest.of(page, validatedSize);
        
        // Functional strategy-based order retrieval
        return getOrderRetrievalStrategy(status, symbol)
            .apply(new OrderFilterParams(userId, status, symbol, pageable));
    }
    
    /**
     * Get active orders for user (Optimized query)
     */
    @GetMapping("/active")
    @Operation(
        summary = "Get active orders",
        description = "Retrieve all orders with active status (ACKNOWLEDGED, PARTIALLY_FILLED) for the authenticated user",
        tags = {"Orders"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Active orders retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(type = "array", implementation = OrderResponse.class)
        )
    )
    public ResponseEntity<List<OrderResponse>> getActiveOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        // Fast blocking query with Virtual Thread
        Result<List<OrderResponse>, TradeError> result = orderService.getActiveOrders(userId);
        
        return switch (result) {
            case Result.Success<List<OrderResponse>, TradeError> success -> ResponseEntity.ok(success.value());
            case Result.Failure<List<OrderResponse>, TradeError> failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        };
    }
    
    /**
     * Modify an existing order (Async with Virtual Threads)
     */
    @PutMapping("/{orderId}")
    @Operation(
        summary = "Modify order",
        description = "Modify quantity, price, or order type of an existing order that is not yet filled",
        tags = {"Orders"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Order modified successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid modification request - validation failed"),
        @ApiResponse(responseCode = "404", description = "Order not found or not accessible"),
        @ApiResponse(responseCode = "409", description = "Order cannot be modified in current status (e.g., already filled)")
    })
    public CompletableFuture<ResponseEntity<OrderResponse>> modifyOrder(
            @Parameter(
                name = "orderId",
                description = "Unique order identifier to modify",
                required = true,
                example = "ORD-12345678",
                in = ParameterIn.PATH
            ) @PathVariable String orderId,
            @Parameter(hidden = true) @Valid @RequestBody OrderRequest modificationRequest,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        log.info("Modifying order {} for user {}", orderId, userId);
        
        // Async processing with Virtual Threads for high concurrency
        return CompletableFuture.supplyAsync(() -> {
            try {
                Result<OrderResponse, TradeError> result = orderService.modifyOrder(orderId, modificationRequest, userId);
                return switch (result) {
                    case Result.Success<OrderResponse, TradeError> success -> {
                        log.info("Order modified successfully: {}", orderId);
                        yield ResponseEntity.ok(success.value());
                    }
                    case Result.Failure<OrderResponse, TradeError> failure -> {
                        log.warn("Failed to modify order {}: {}", orderId, failure.error().getMessage());
                        yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                    }
                };
                
            } catch (IllegalArgumentException | IllegalStateException e) {
                log.warn("Failed to modify order {}: {}", orderId, e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Unexpected error modifying order {}: {}", orderId, e.getMessage());
                throw new RuntimeException("Order modification failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Cancel an order (Fast blocking operation)
     */
    @DeleteMapping("/{orderId}")
    @Operation(
        summary = "Cancel order",
        description = "Cancel an existing order that has not been filled yet",
        tags = {"Orders"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Order cancelled successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Order not found or not accessible to user"),
        @ApiResponse(responseCode = "409", description = "Order cannot be cancelled in current status (e.g., already filled or cancelled)")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(
                name = "orderId",
                description = "Unique order identifier to cancel",
                required = true,
                example = "ORD-12345678",
                in = ParameterIn.PATH
            ) @PathVariable String orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        log.info("Cancelling order {} for user {}", orderId, userId);
        
        try {
            Result<OrderResponse, TradeError> result = orderService.cancelOrder(orderId, userId);
            return switch (result) {
                case Result.Success<OrderResponse, TradeError> success -> {
                    log.info("Order cancelled successfully: {}", orderId);
                    yield ResponseEntity.ok(success.value());
                }
                case Result.Failure<OrderResponse, TradeError> failure -> ResponseEntity.notFound().build();
            };
            
        } catch (IllegalStateException e) {
            log.warn("Cannot cancel order {}: {}", orderId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to cancel order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Order cancellation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get order status (Ultra-fast lightweight endpoint)
     */
    @GetMapping("/{orderId}/status")
    @Operation(
        summary = "Get order status",
        description = "Get current status of an order (lightweight endpoint for status polling)",
        tags = {"Orders"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Order status retrieved successfully",
        content = @Content(
            mediaType = "text/plain",
            schema = @Schema(type = "string", example = "FILLED")
        )
    )
    public ResponseEntity<String> getOrderStatus(
            @Parameter(
                name = "orderId",
                description = "Unique order identifier",
                required = true,
                example = "ORD-12345678",
                in = ParameterIn.PATH
            ) @PathVariable String orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        // Ultra-fast status lookup with functional pattern
        Result<OrderStatus, TradeError> result = orderService.getOrderStatus(orderId, userId);
        return switch (result) {
            case Result.Success<OrderStatus, TradeError> success -> ResponseEntity.ok(success.value().name());
            case Result.Failure<OrderStatus, TradeError> failure -> ResponseEntity.notFound().build();
        };
    }
    
    /**
     * Get order count for user (Dashboard widget optimization)
     */
    @GetMapping("/count")
    @Operation(
        summary = "Get order counts",
        description = "Get aggregated count of orders by status for user dashboard widgets",
        tags = {"Orders", "Dashboard"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Order counts retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(type = "object"),
            examples = @ExampleObject(
                name = "Order Counts",
                value = "{\"FILLED\":25,\"ACKNOWLEDGED\":3,\"CANCELLED\":2,\"REJECTED\":1}"
            )
        )
    )
    public ResponseEntity<Map<String, Long>> getOrderCounts(
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        // Fast aggregation query
        Result<Map<String, Long>, TradeError> result = orderService.getOrderCounts(userId);
        return switch (result) {
            case Result.Success<Map<String, Long>, TradeError> success -> ResponseEntity.ok(success.value());
            case Result.Failure<Map<String, Long>, TradeError> failure -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        };
    }
    
    /**
     * Bulk order operations (Virtual Thread optimization)
     */
    @PostMapping("/bulk")
    @Operation(
        summary = "Place multiple orders",
        description = "Submit multiple orders for efficient batch processing using Virtual Threads",
        tags = {"Orders", "Batch"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Bulk orders processed (partial success possible)",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(type = "array", implementation = OrderResponse.class)
        )
    )
    public CompletableFuture<ResponseEntity<List<OrderResponse>>> placeBulkOrders(
            @Parameter(hidden = true) @Valid @RequestBody List<OrderRequest> orderRequests,
            @Parameter(hidden = true) @AuthenticationPrincipal TradingUserPrincipal principal) {
        
        Long userId = principal.getUserId();
        
        log.info("Processing {} bulk orders for user {}", orderRequests.size(), userId);
        
        // Parallel processing with Virtual Threads - unlimited scalability
        return CompletableFuture.supplyAsync(() -> {
            List<OrderResponse> responses = orderRequests.parallelStream()
                .map(request -> {
                    try {
                        Result<OrderResponse, TradeError> result = orderService.placeOrder(request, userId);
                        return switch (result) {
                            case Result.Success<OrderResponse, TradeError> success -> success.value();
                            case Result.Failure<OrderResponse, TradeError> failure -> {
                                log.error("Failed to process bulk order: {}", failure.error().getMessage());
                                yield null;
                            }
                        };
                    } catch (Exception e) {
                        log.error("Failed to process bulk order: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
                
            log.info("Processed {}/{} bulk orders successfully for user {}", 
                    responses.size(), orderRequests.size(), userId);
                    
            return ResponseEntity.ok(responses);
        });
    }
    
    // Functional strategy for order retrieval - replaces if-else chains
    private Function<OrderFilterParams, List<OrderResponse>> getOrderRetrievalStrategy(OrderStatus status, String symbol) {
        return Optional.ofNullable(status)
            .map(s -> Optional.ofNullable(symbol)
                .map(sym -> (Function<OrderFilterParams, List<OrderResponse>>) 
                    params -> orderService.getOrdersByUserSymbolAndStatus(params.userId(), params.symbol(), params.status()).getOrElse(List.of()))
                .orElse(params -> orderService.getOrdersByUserAndStatus(params.userId(), params.status()).getOrElse(List.of())))
            .orElse(Optional.ofNullable(symbol)
                .map(sym -> (Function<OrderFilterParams, List<OrderResponse>>) 
                    params -> orderService.getOrdersByUserAndSymbol(params.userId(), params.symbol(), params.pageable()).getOrElse(List.of()))
                .orElse(params -> orderService.getOrdersByUser(params.userId(), params.pageable()).getOrElse(List.of())));
    }
    
    /**
     * Generate correlation ID for request tracing
     */
    private String generateCorrelationId() {
        return "TC-" + System.currentTimeMillis() + "-" + Thread.currentThread().getName().hashCode();
    }
    
    // Note: Health endpoints are now handled by ApiV2HealthController and GatewayController
}

// Functional record for order filter parameters
record OrderFilterParams(Long userId, OrderStatus status, String symbol, Pageable pageable) {}