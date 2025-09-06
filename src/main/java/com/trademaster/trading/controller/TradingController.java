package com.trademaster.trading.controller;

import com.trademaster.trading.common.Result;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.ErrorResponse;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.security.TradingUserPrincipal;
import com.trademaster.trading.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/orders")
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
    @Operation(summary = "Place new order", 
               description = "Submit a new trading order with validation and risk checks")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order placed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid order request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions or risk check failed"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<?> placeOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            @AuthenticationPrincipal TradingUserPrincipal principal,
            HttpServletRequest request) {
        
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
    @Operation(summary = "Get order details", 
               description = "Retrieve detailed information about a specific order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<?> getOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal TradingUserPrincipal principal,
            HttpServletRequest request) {
        
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
    @Operation(summary = "Get order history", 
               description = "Retrieve paginated list of user's orders")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public List<OrderResponse> getOrderHistory(
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") 
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by order status") 
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Filter by symbol") 
            @RequestParam(required = false) String symbol,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
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
    @Operation(summary = "Get active orders", 
               description = "Retrieve all active orders (ACKNOWLEDGED, PARTIALLY_FILLED)")
    @ApiResponse(responseCode = "200", description = "Active orders retrieved successfully")
    public ResponseEntity<List<OrderResponse>> getActiveOrders(
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
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
    @Operation(summary = "Modify order", 
               description = "Modify quantity or price of an existing order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order modified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid modification request"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "409", description = "Order cannot be modified in current status")
    })
    public CompletableFuture<ResponseEntity<OrderResponse>> modifyOrder(
            @PathVariable String orderId,
            @Valid @RequestBody OrderRequest modificationRequest,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
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
    @Operation(summary = "Cancel order", 
               description = "Cancel an existing order")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Order not found"),
        @ApiResponse(responseCode = "409", description = "Order cannot be cancelled in current status")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
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
    @Operation(summary = "Get order status", 
               description = "Get current status of an order (lightweight)")
    @ApiResponse(responseCode = "200", description = "Order status retrieved")
    public ResponseEntity<String> getOrderStatus(
            @PathVariable String orderId,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
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
    @Operation(summary = "Get order counts", 
               description = "Get count of orders by status for user dashboard")
    @ApiResponse(responseCode = "200", description = "Order counts retrieved")
    public ResponseEntity<Map<String, Long>> getOrderCounts(
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
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
    @Operation(summary = "Place multiple orders", 
               description = "Submit multiple orders for batch processing")
    @ApiResponse(responseCode = "200", description = "Bulk orders processed")
    public CompletableFuture<ResponseEntity<List<OrderResponse>>> placeBulkOrders(
            @Valid @RequestBody List<OrderRequest> orderRequests,
            @AuthenticationPrincipal TradingUserPrincipal principal) {
        
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
                .filter(response -> response != null)
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
}

// Functional record for order filter parameters
record OrderFilterParams(Long userId, OrderStatus status, String symbol, Pageable pageable) {}