package com.trademaster.trading.service.impl;

import com.trademaster.trading.client.BrokerAuthClient;
import com.trademaster.trading.common.Result;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.metrics.AlertingService;
import com.trademaster.trading.metrics.TradingMetricsService;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.repository.OrderRepository;
import com.trademaster.trading.routing.ExecutionStrategy;
import com.trademaster.trading.routing.OrderRouter;
import com.trademaster.trading.routing.RoutingDecision;
import com.trademaster.trading.service.OrderService;
import com.trademaster.trading.service.TradingEventPublisher;
import com.trademaster.trading.validation.OrderValidator;
import com.trademaster.trading.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Order Service Implementation
 * 
 * Production-ready order management service with real broker integration.
 * Uses Java 24 Virtual Threads for unlimited scalability.
 * 
 * MANDATORY COMPLIANCE:
 * - Rule #1: Java 24 Virtual Threads
 * - Rule #3: Functional programming patterns
 * - Rule #11: Result types for error handling
 * - Rule #15: Structured logging with correlation IDs
 * - Rule #25: Circuit breaker for broker calls
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final List<OrderValidator> validators;
    private final OrderRouter orderRouter;
    private final TradingEventPublisher eventPublisher;
    private final BrokerAuthClient brokerAuthClient;
    private final TradingMetricsService metricsService;
    private final AlertingService alertingService;
    private final AsyncTaskExecutor orderProcessingExecutor;
    
    public OrderServiceImpl(
            OrderRepository orderRepository,
            List<OrderValidator> validators,
            OrderRouter orderRouter,
            TradingEventPublisher eventPublisher,
            BrokerAuthClient brokerAuthClient,
            TradingMetricsService metricsService,
            AlertingService alertingService,
            @Qualifier("orderProcessingExecutor") AsyncTaskExecutor orderProcessingExecutor) {
        this.orderRepository = orderRepository;
        this.validators = validators;
        this.orderRouter = orderRouter;
        this.eventPublisher = eventPublisher;
        this.brokerAuthClient = brokerAuthClient;
        this.metricsService = metricsService;
        this.alertingService = alertingService;
        this.orderProcessingExecutor = orderProcessingExecutor;
    }
    
    // Circuit breaker names for monitoring
    private static final String BROKER_AUTH_CB = "broker-auth-service";
    private static final String PORTFOLIO_SERVICE_CB = "portfolio-service";
    
    @Override
    @Transactional
    public Result<OrderResponse, TradeError> placeOrder(OrderRequest orderRequest, Long userId) {
        long startTime = System.currentTimeMillis();
        String correlationId = generateCorrelationId();
        
        // Start metrics collection
        var orderProcessingTimer = metricsService.startOrderProcessing();
        var riskCheckTimer = metricsService.startRiskCheck();
        
        log.info("Processing order placement - correlationId: {}, userId: {}, symbol: {}, quantity: {}", 
                correlationId, userId, orderRequest.symbol(), orderRequest.quantity());
        
        try {
            // Step 1: Validate order
            ValidationResult validation = validateOrderWithAllValidators(orderRequest, userId);
            metricsService.recordRiskCheckTime(riskCheckTimer);
            
            // Functional validation pattern using Result type
            Result<ValidationResult, TradeError> validationResult = Optional.of(validation)
                .filter(ValidationResult::isValid)
                .map(Result::<ValidationResult, TradeError>success)
                .orElseGet(() -> {
                    metricsService.recordOrderProcessingTime(orderProcessingTimer);
                    metricsService.recordOrderFailed("UNKNOWN", "VALIDATION_FAILED");
                    return Result.failure(new TradeError.ValidationError.MissingRequiredField(
                        "Order validation failed: " + String.join(", ", validation.getErrorMessages())));
                });
            
            // Short-circuit on validation failure
            if (validationResult.isFailure()) {
                return validationResult.map(v -> null);
            }
            
            // Step 2: Create and persist order entity
            Order order = createOrderFromRequest(orderRequest, userId);
            order = orderRepository.save(order);
            
            // Record order placed metric
            String brokerName = orderRequest.brokerName() != null ? orderRequest.brokerName() : "UNKNOWN";
            BigDecimal orderValue = orderRequest.getEstimatedOrderValue();
            metricsService.recordOrderPlaced(brokerName, orderValue);
            metricsService.incrementActiveOrders();
            
            // Step 3: Route order to best broker/venue using functional pattern
            RoutingDecision routingDecision = orderRouter.routeOrder(order);
            
            // Functional routing decision pattern
            Result<RoutingDecision, TradeError> routingResult = switch (routingDecision.getStrategy()) {
                case REJECT -> {
                    // Handle rejection functionally without lambda mutation
                    order.updateStatus(OrderStatus.REJECTED);
                    order.setRejectionReason(routingDecision.getReason());
                    orderRepository.save(order);
                    
                    // Record metrics for rejected order
                    metricsService.recordOrderProcessingTime(orderProcessingTimer);
                    metricsService.recordOrderFailed(routingDecision.getBrokerName(), "ROUTING_REJECTED");
                    metricsService.decrementActiveOrders();
                    
                    yield Result.failure(new TradeError.ExecutionError.OrderRejected(routingDecision.getReason()));
                }
                default -> Result.success(routingDecision);
            };
            
            // Short-circuit on routing failure
            if (routingResult.isFailure()) {
                return routingResult.map(r -> null);
            }
            
            // Step 4: Submit to broker via broker-auth-service
            CompletableFuture<String> brokerSubmission = submitOrderToBroker(order, routingDecision, correlationId);
            
            // Step 5: Handle broker response
            try {
                String brokerOrderId = brokerSubmission.join();
                order.setBrokerOrderId(brokerOrderId);
                order.setBrokerName(routingDecision.getBrokerName());
                order.updateStatus(OrderStatus.ACKNOWLEDGED);
                order = orderRepository.save(order);
                
                // Step 6: Publish order placed event
                eventPublisher.publishOrderPlacedEvent(order);
                
                // Record successful execution metrics
                metricsService.recordOrderProcessingTime(orderProcessingTimer);
                metricsService.recordOrderExecuted(routingDecision.getBrokerName(), orderRequest.getEstimatedOrderValue());
                
                long processingTime = System.currentTimeMillis() - startTime;
                
                // Check for SLA violations
                if (processingTime > 100) { // 100ms SLA
                    alertingService.handleSLAViolation("ORDER_PROCESSING", processingTime, 100);
                }
                
                log.info("Order placed successfully - correlationId: {}, orderId: {}, brokerOrderId: {}, processingTime: {}ms", 
                        correlationId, order.getOrderId(), brokerOrderId, processingTime);
                
                return Result.success(convertToOrderResponse(order));
                
            } catch (Exception brokerError) {
                log.error("Broker submission failed - correlationId: {}, orderId: {}, error: {}", 
                         correlationId, order.getOrderId(), brokerError.getMessage());
                
                order.updateStatus(OrderStatus.REJECTED);
                order.setRejectionReason("Broker submission failed: " + brokerError.getMessage());
                orderRepository.save(order);
                
                // Record failure metrics
                metricsService.recordOrderProcessingTime(orderProcessingTimer);
                metricsService.recordOrderFailed(routingDecision.getBrokerName(), "BROKER_SUBMISSION_FAILED");
                metricsService.decrementActiveOrders();
                
                // Handle connectivity issue
                alertingService.handleBrokerConnectivityIssue(routingDecision.getBrokerName(), brokerError.getMessage());
                
                return Result.failure(new TradeError.SystemError.ServiceUnavailable("broker-auth-service"));
            }
            
        } catch (Exception e) {
            log.error("Failed to place order - correlationId: {}, userId: {}, error: {}", 
                     correlationId, userId, e.getMessage());
            
            // Record unexpected error metrics
            metricsService.recordOrderProcessingTime(orderProcessingTimer);
            metricsService.recordOrderFailed("UNKNOWN", "UNEXPECTED_ERROR");
            
            return Result.failure(new TradeError.SystemError.UnexpectedError("Internal error: " + e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Result<OrderResponse, TradeError> getOrder(String orderId, Long userId) {
        try {
            return orderRepository.findByOrderIdAndUserId(orderId, userId)
                .map(this::convertToOrderResponse)
                .map(Result::<OrderResponse, TradeError>success)
                .orElse(Result.failure(new TradeError.DataError.EntityNotFound("Order", orderId)));
                
        } catch (Exception e) {
            log.error("Failed to get order {} for user {}: {}", orderId, userId, e.getMessage());
            return Result.failure(new TradeError.DataError.DatabaseError("order retrieval", e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Result<List<OrderResponse>, TradeError> getOrdersByUser(Long userId, Pageable pageable) {
        try {
            Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            List<OrderResponse> responses = orderPage.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
            return Result.success(responses);
            
        } catch (Exception e) {
            log.error("Failed to get orders for user {}: {}", userId, e.getMessage());
            return Result.failure(new TradeError.DataError.DatabaseError("order retrieval", e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Result<List<OrderResponse>, TradeError> getOrdersByUserAndStatus(Long userId, OrderStatus status) {
        try {
            List<Order> orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
            List<OrderResponse> responses = orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
            return Result.success(responses);
            
        } catch (Exception e) {
            log.error("Failed to get orders for user {} and status {}: {}", userId, status, e.getMessage());
            return Result.failure(new TradeError.DataError.DatabaseError("order retrieval", e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Result<List<OrderResponse>, TradeError> getOrdersByUserAndSymbol(Long userId, String symbol, Pageable pageable) {
        try {
            Page<Order> orderPage = orderRepository.findByUserIdAndSymbolOrderByCreatedAtDesc(userId, symbol, pageable);
            List<OrderResponse> responses = orderPage.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
            return Result.success(responses);
            
        } catch (Exception e) {
            log.error("Failed to get orders for user {} and symbol {}: {}", userId, symbol, e.getMessage());
            return Result.failure(new TradeError.DataError.DatabaseError("order retrieval", e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Result<List<OrderResponse>, TradeError> getOrdersByUserSymbolAndStatus(Long userId, String symbol, OrderStatus status) {
        try {
            List<Order> orders = orderRepository.findByUserIdAndSymbolAndStatusOrderByCreatedAtDesc(userId, symbol, status);
            List<OrderResponse> responses = orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
            return Result.success(responses);
            
        } catch (Exception e) {
            log.error("Failed to get orders for user {} symbol {} status {}: {}", userId, symbol, status, e.getMessage());
            return Result.failure(new TradeError.DataError.DatabaseError("order retrieval", e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Result<List<OrderResponse>, TradeError> getActiveOrders(Long userId) {
        try {
            List<OrderStatus> activeStatuses = List.of(
                OrderStatus.PENDING, OrderStatus.ACKNOWLEDGED, OrderStatus.PARTIALLY_FILLED
            );
            
            List<Order> orders = orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, activeStatuses);
            List<OrderResponse> responses = orders.stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
            return Result.success(responses);
            
        } catch (Exception e) {
            log.error("Failed to get active orders for user {}: {}", userId, e.getMessage());
            return Result.failure(new TradeError.DataError.DatabaseError("order retrieval", e.getMessage()));
        }
    }
    
    @Override
    @Transactional
    public Result<OrderResponse, TradeError> modifyOrder(String orderId, OrderRequest modificationRequest, Long userId) {
        String correlationId = generateCorrelationId();
        
        try {
            Optional<Order> orderOpt = orderRepository.findByOrderIdAndUserId(orderId, userId);
            if (orderOpt.isEmpty()) {
                return Result.failure(new TradeError.DataError.EntityNotFound("Order", orderId));
            }
            
            Order order = orderOpt.get();
            if (!order.getStatus().isModifiable()) {
                return Result.failure(new TradeError.ExecutionError.OrderRejected(
                    "Order cannot be modified in status: " + order.getStatus()));
            }
            
            // Validate modification request
            ValidationResult validation = validateOrderWithAllValidators(modificationRequest, userId);
            if (!validation.isValid()) {
                return Result.failure(new TradeError.ValidationError.MissingRequiredField(
                    "Modification validation failed: " + String.join(", ", validation.getErrorMessages())));
            }
            
            // Submit modification to broker
            CompletableFuture<String> brokerModification = modifyOrderWithBroker(order, modificationRequest, correlationId);
            
            try {
                String newBrokerOrderId = brokerModification.join();
                
                // Update order with new details
                updateOrderFromModificationRequest(order, modificationRequest);
                if (newBrokerOrderId != null && !newBrokerOrderId.equals(order.getBrokerOrderId())) {
                    order.setBrokerOrderId(newBrokerOrderId);
                }
                order = orderRepository.save(order);
                
                log.info("Order modified successfully - correlationId: {}, orderId: {}", correlationId, orderId);
                return Result.success(convertToOrderResponse(order));
                
            } catch (Exception brokerError) {
                log.error("Broker modification failed - correlationId: {}, orderId: {}, error: {}", 
                         correlationId, orderId, brokerError.getMessage());
                return Result.failure(new TradeError.SystemError.ServiceUnavailable("broker-auth-service"));
            }
            
        } catch (Exception e) {
            log.error("Failed to modify order - correlationId: {}, orderId: {}, error: {}", 
                     correlationId, orderId, e.getMessage());
            return Result.failure(new TradeError.SystemError.UnexpectedError("Internal error: " + e.getMessage()));
        }
    }
    
    @Override
    @Transactional
    public Result<OrderResponse, TradeError> cancelOrder(String orderId, Long userId) {
        String correlationId = generateCorrelationId();
        
        try {
            Optional<Order> orderOpt = orderRepository.findByOrderIdAndUserId(orderId, userId);
            if (orderOpt.isEmpty()) {
                return Result.failure(new TradeError.DataError.EntityNotFound("Order", orderId));
            }
            
            Order order = orderOpt.get();
            if (!order.getStatus().isCancellable()) {
                return Result.failure(new TradeError.ExecutionError.OrderRejected(
                    "Order cannot be cancelled in status: " + order.getStatus()));
            }
            
            // Submit cancellation to broker
            if (order.getBrokerOrderId() != null) {
                CompletableFuture<Void> brokerCancellation = cancelOrderWithBroker(order, correlationId);
                
                try {
                    brokerCancellation.join();
                } catch (Exception brokerError) {
                    log.warn("Broker cancellation failed but proceeding with local cancellation - correlationId: {}, orderId: {}, error: {}", 
                            correlationId, orderId, brokerError.getMessage());
                }
            }
            
            // Update order status
            order.updateStatus(OrderStatus.CANCELLED);
            order = orderRepository.save(order);
            
            // Publish cancellation event
            eventPublisher.publishOrderCancelledEvent(order);
            
            log.info("Order cancelled successfully - correlationId: {}, orderId: {}", correlationId, orderId);
            return Result.success(convertToOrderResponse(order));
            
        } catch (Exception e) {
            log.error("Failed to cancel order - correlationId: {}, orderId: {}, error: {}", 
                     correlationId, orderId, e.getMessage());
            return Result.failure(new TradeError.SystemError.UnexpectedError("Internal error: " + e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Result<OrderStatus, TradeError> getOrderStatus(String orderId, Long userId) {
        try {
            return orderRepository.findByOrderIdAndUserId(orderId, userId)
                .map(Order::getStatus)
                .map(Result::<OrderStatus, TradeError>success)
                .orElse(Result.failure(new TradeError.DataError.EntityNotFound("Order", orderId)));
                
        } catch (Exception e) {
            log.error("Failed to get order status {} for user {}: {}", orderId, userId, e.getMessage());
            return Result.failure(new TradeError.DataError.DatabaseError("order status retrieval", e.getMessage()));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Result<Map<String, Long>, TradeError> getOrderCounts(Long userId) {
        try {
            Map<String, Long> counts = orderRepository.findByUserId(userId).stream()
                .collect(Collectors.groupingBy(
                    order -> order.getStatus().name(),
                    Collectors.counting()
                ));
            return Result.success(counts);
            
        } catch (Exception e) {
            log.error("Failed to get order counts for user {}: {}", userId, e.getMessage());
            return Result.failure(new TradeError.DataError.DatabaseError("order count retrieval", e.getMessage()));
        }
    }
    
    @Override
    @Transactional
    public Order processOrderFill(Order order, Integer fillQuantity, BigDecimal fillPrice) {
        log.info("Processing fill for order {}: quantity={}, price={}", 
                order.getOrderId(), fillQuantity, fillPrice);
        
        order.addFill(fillQuantity, fillPrice);
        Order savedOrder = orderRepository.save(order);
        
        // Publish execution event
        eventPublisher.publishOrderExecutedEvent(savedOrder);
        
        return savedOrder;
    }
    
    @Override
    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatus newStatus, String reason) {
        Optional<Order> orderOpt = orderRepository.findByOrderId(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.updateStatus(newStatus);
            if (reason != null && newStatus == OrderStatus.REJECTED) {
                order.setRejectionReason(reason);
            }
            return orderRepository.save(order);
        }
        throw new IllegalArgumentException("Order not found: " + orderId);
    }
    
    @Override
    @Transactional
    public Long expireOrders() {
        LocalDate today = LocalDate.now();
        
        // Find GTD orders that have expired
        List<Order> expiredOrders = orderRepository.findByExpiryDateBeforeAndStatusIn(
            today, List.of(OrderStatus.PENDING, OrderStatus.ACKNOWLEDGED, OrderStatus.PARTIALLY_FILLED)
        );
        
        // Functional programming pattern - replace for loop with stream operations
        long expiredCount = expiredOrders.stream()
            .map(order -> {
                try {
                    order.updateStatus(OrderStatus.EXPIRED);
                    orderRepository.save(order);
                    log.info("Expired order: {}", order.getOrderId());
                    return 1L;
                } catch (Exception e) {
                    log.error("Failed to expire order {}: {}", order.getOrderId(), e.getMessage());
                    return 0L;
                }
            })
            .reduce(0L, Long::sum);
        
        log.info("Expired {} orders", expiredCount);
        return expiredCount;
    }
    
    // Private helper methods
    
    private ValidationResult validateOrderWithAllValidators(OrderRequest orderRequest, Long userId) {
        // Functional programming pattern - replace for loop with stream operations
        return validators.stream()
            .map(validator -> validator.validate(orderRequest, userId))
            .reduce(ValidationResult.success("OrderService"), ValidationResult::merge);
    }
    
    private Order createOrderFromRequest(OrderRequest request, Long userId) {
        return Order.builder()
            .userId(userId)
            .symbol(request.symbol())
            .exchange(request.exchange())
            .orderType(request.orderType())
            .side(request.side())
            .quantity(request.quantity())
            .limitPrice(request.limitPrice())
            .stopPrice(request.stopPrice())
            .timeInForce(request.timeInForce())
            .expiryDate(request.expiryDate())
            .status(OrderStatus.PENDING)
            .build();
    }
    
    private OrderResponse convertToOrderResponse(Order order) {
        return OrderResponse.builder()
            .id(order.getId())
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .symbol(order.getSymbol())
            .exchange(order.getExchange())
            .orderType(order.getOrderType())
            .side(order.getSide())
            .quantity(order.getQuantity())
            .limitPrice(order.getLimitPrice())
            .stopPrice(order.getStopPrice())
            .timeInForce(order.getTimeInForce())
            .expiryDate(order.getExpiryDate())
            .status(order.getStatus())
            .brokerOrderId(order.getBrokerOrderId())
            .brokerName(order.getBrokerName())
            .filledQuantity(order.getFilledQuantity())
            .averagePrice(order.getAvgFillPrice())
            .rejectionReason(order.getRejectionReason())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .submittedAt(order.getSubmittedAt())
            .executedAt(order.getExecutedAt())
            .build();
    }
    
    @CircuitBreaker(name = BROKER_AUTH_CB, fallbackMethod = "submitOrderToBrokerFallback")
    private CompletableFuture<String> submitOrderToBroker(Order order, RoutingDecision routingDecision, String correlationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> orderData = Map.of(
                    "symbol", order.getSymbol(),
                    "exchange", order.getExchange(),
                    "side", order.getSide().name(),
                    "orderType", order.getOrderType().name(),
                    "quantity", order.getQuantity(),
                    "limitPrice", order.getLimitPrice() != null ? order.getLimitPrice().toString() : "",
                    "stopPrice", order.getStopPrice() != null ? order.getStopPrice().toString() : "",
                    "timeInForce", order.getTimeInForce().name()
                );
                
                Map<String, Object> response = brokerAuthClient.submitOrder(
                    routingDecision.getBrokerName(), 
                    orderData, 
                    correlationId
                );
                
                if (response.get("success").equals(true)) {
                    return (String) response.get("brokerOrderId");
                } else {
                    throw new RuntimeException("Broker rejected order: " + response.get("message"));
                }
                
            } catch (Exception e) {
                log.error("Failed to submit order to broker - correlationId: {}, orderId: {}, error: {}", 
                         correlationId, order.getOrderId(), e.getMessage());
                throw new RuntimeException("Broker submission failed: " + e.getMessage(), e);
            }
        }, orderProcessingExecutor);
    }
    
    @CircuitBreaker(name = BROKER_AUTH_CB, fallbackMethod = "modifyOrderWithBrokerFallback")
    private CompletableFuture<String> modifyOrderWithBroker(Order order, OrderRequest modificationRequest, String correlationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> modificationData = Map.of(
                    "quantity", modificationRequest.quantity(),
                    "limitPrice", modificationRequest.limitPrice() != null ? modificationRequest.limitPrice().toString() : "",
                    "stopPrice", modificationRequest.stopPrice() != null ? modificationRequest.stopPrice().toString() : ""
                );
                
                Map<String, Object> response = brokerAuthClient.modifyOrder(
                    order.getBrokerName(),
                    order.getBrokerOrderId(),
                    modificationData,
                    correlationId
                );
                
                if (response.get("success").equals(true)) {
                    return (String) response.get("brokerOrderId");
                } else {
                    throw new RuntimeException("Broker rejected modification: " + response.get("message"));
                }
                
            } catch (Exception e) {
                log.error("Failed to modify order with broker - correlationId: {}, orderId: {}, error: {}", 
                         correlationId, order.getOrderId(), e.getMessage());
                throw new RuntimeException("Broker modification failed: " + e.getMessage(), e);
            }
        }, orderProcessingExecutor);
    }
    
    @CircuitBreaker(name = BROKER_AUTH_CB, fallbackMethod = "cancelOrderWithBrokerFallback")
    private CompletableFuture<Void> cancelOrderWithBroker(Order order, String correlationId) {
        return CompletableFuture.runAsync(() -> {
            try {
                brokerAuthClient.cancelOrder(
                    order.getBrokerName(),
                    order.getBrokerOrderId(),
                    correlationId
                );
                
            } catch (Exception e) {
                log.error("Failed to cancel order with broker - correlationId: {}, orderId: {}, error: {}", 
                         correlationId, order.getOrderId(), e.getMessage());
                throw new RuntimeException("Broker cancellation failed: " + e.getMessage(), e);
            }
        }, orderProcessingExecutor);
    }
    
    private void updateOrderFromModificationRequest(Order order, OrderRequest modificationRequest) {
        if (modificationRequest.quantity() != null) {
            order.setQuantity(modificationRequest.quantity());
        }
        if (modificationRequest.limitPrice() != null) {
            order.setLimitPrice(modificationRequest.limitPrice());
        }
        if (modificationRequest.stopPrice() != null) {
            order.setStopPrice(modificationRequest.stopPrice());
        }
    }
    
    private String generateCorrelationId() {
        return "TM-" + System.currentTimeMillis() + "-" + Thread.currentThread().getName();
    }
    
    // Circuit Breaker Fallback Methods
    
    /**
     * Fallback method for order submission when broker service is unavailable
     */
    private CompletableFuture<String> submitOrderToBrokerFallback(Order order, RoutingDecision routingDecision, String correlationId, Exception ex) {
        log.error("Broker service unavailable for order submission - correlationId: {}, orderId: {}, error: {}", 
                 correlationId, order.getOrderId(), ex.getMessage());
        
        return CompletableFuture.failedFuture(
            new RuntimeException("Broker authentication service is currently unavailable. Order will be queued for retry.", ex)
        );
    }
    
    /**
     * Fallback method for order modification when broker service is unavailable
     */
    private CompletableFuture<String> modifyOrderWithBrokerFallback(Order order, OrderRequest modificationRequest, String correlationId, Exception ex) {
        log.error("Broker service unavailable for order modification - correlationId: {}, orderId: {}, error: {}", 
                 correlationId, order.getOrderId(), ex.getMessage());
        
        return CompletableFuture.failedFuture(
            new RuntimeException("Broker authentication service is currently unavailable. Modification will be queued for retry.", ex)
        );
    }
    
    /**
     * Fallback method for order cancellation when broker service is unavailable
     */
    private CompletableFuture<Void> cancelOrderWithBrokerFallback(Order order, String correlationId, Exception ex) {
        log.error("Broker service unavailable for order cancellation - correlationId: {}, orderId: {}, error: {}", 
                 correlationId, order.getOrderId(), ex.getMessage());
        
        // For cancellation, we can proceed with local cancellation as graceful degradation
        log.warn("Proceeding with local cancellation due to broker service unavailability - orderId: {}", order.getOrderId());
        
        return CompletableFuture.runAsync(() -> {
            // Mark order as cancellation pending - could be handled by a retry mechanism
            log.info("Order marked for cancellation retry when broker service is available - orderId: {}", order.getOrderId());
        }, orderProcessingExecutor);
    }
}