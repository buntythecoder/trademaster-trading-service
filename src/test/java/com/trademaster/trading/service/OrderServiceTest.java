package com.trademaster.trading.service;

import com.trademaster.trading.common.Result;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.model.TimeInForce;
import com.trademaster.trading.repository.OrderRepository;
import com.trademaster.trading.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for OrderService
 * 
 * Tests critical trading operations including:
 * - Order placement with risk management
 * - Order lifecycle management (modify, cancel)
 * - Order status tracking and reporting
 * - Error handling and edge cases
 * - Virtual Thread compatibility
 * 
 * MANDATORY: Java 24 Virtual Threads + Result<T,E> pattern compliance
 * MANDATORY: 80%+ unit test coverage per TradeMaster standards
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private RiskManagementService riskManagementService;
    
    @Mock
    private BrokerIntegrationService brokerIntegrationService;
    
    @Mock
    private PortfolioService portfolioService;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private OrderServiceImpl orderService;
    
    private Long testUserId;
    private String testOrderId;
    private String testSymbol;
    private OrderRequest testOrderRequest;
    private Order testOrder;
    private OrderResponse testOrderResponse;
    
    @BeforeEach
    void setUp() {
        testUserId = 12345L;
        testOrderId = "ORDER_12345_001";
        testSymbol = "RELIANCE";
        
        testOrderRequest = OrderRequest.builder()
            .symbol(testSymbol)
            .side(OrderSide.BUY)
            .orderType(OrderType.LIMIT)
            .quantity(100)
            .limitPrice(new BigDecimal("2450.75"))
            .timeInForce(TimeInForce.DAY)
            .build();
            
        testOrder = Order.builder()
            .orderId(testOrderId)
            .userId(testUserId)
            .symbol(testSymbol)
            .side(OrderSide.BUY)
            .orderType(OrderType.LIMIT)
            .quantity(100)
            .limitPrice(new BigDecimal("2450.75"))
            .status(OrderStatus.PENDING)
            .timeInForce(TimeInForce.DAY)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
            
        testOrderResponse = OrderResponse.builder()
            .orderId(testOrderId)
            .symbol(testSymbol)
            .side(OrderSide.BUY)
            .orderType(OrderType.LIMIT)
            .quantity(100)
            .limitPrice(new BigDecimal("2450.75"))
            .status(OrderStatus.PENDING)
            .build();
    }
    
    @Test
    void placeOrder_WithValidRequest_ShouldSucceed() {
        // Arrange
        when(riskManagementService.validateOrder(eq(testOrderRequest), eq(testUserId)))
            .thenReturn(Result.success(null));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(brokerIntegrationService.submitOrder(any(Order.class)))
            .thenReturn(Result.success(testOrder));
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.placeOrder(testOrderRequest, testUserId);
        
        // Assert
        assertTrue(result.isSuccess());
        
        OrderResponse response = result.getValue();
        assertNotNull(response);
        assertEquals(testOrderId, response.getOrderId());
        assertEquals(testSymbol, response.symbol());
        assertEquals(OrderSide.BUY, response.side());
        assertEquals(OrderType.LIMIT, response.orderType());
        assertEquals(100, response.quantity());
        assertEquals(new BigDecimal("2450.75"), response.getPrice());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        
        // Verify interactions
        verify(riskManagementService).validateOrder(eq(testOrderRequest), eq(testUserId));
        verify(orderRepository).save(any(Order.class));
        verify(brokerIntegrationService).submitOrder(any(Order.class));
        verify(notificationService).sendOrderNotification(eq(testUserId), any(OrderResponse.class));
    }
    
    @Test
    void placeOrder_WithRiskValidationFailure_ShouldReturnError() {
        // Arrange
        TradeError riskError = TradeError.riskViolation("Insufficient margin for order");
        when(riskManagementService.validateOrder(eq(testOrderRequest), eq(testUserId)))
            .thenReturn(Result.failure(riskError));
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.placeOrder(testOrderRequest, testUserId);
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(riskError, result.getError());
        
        // Verify no downstream calls were made
        verify(riskManagementService).validateOrder(eq(testOrderRequest), eq(testUserId));
        verify(orderRepository, never()).save(any());
        verify(brokerIntegrationService, never()).submitOrder(any());
        verify(notificationService, never()).sendOrderNotification(any(), any());
    }
    
    @Test
    void placeOrder_WithBrokerFailure_ShouldReturnError() {
        // Arrange
        TradeError brokerError = TradeError.brokerError("Broker connection failed");
        when(riskManagementService.validateOrder(eq(testOrderRequest), eq(testUserId)))
            .thenReturn(Result.success(null));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(brokerIntegrationService.submitOrder(any(Order.class)))
            .thenReturn(Result.failure(brokerError));
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.placeOrder(testOrderRequest, testUserId);
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals(brokerError, result.getError());
        
        // Verify order was saved but broker submission failed
        verify(orderRepository).save(any(Order.class));
        verify(brokerIntegrationService).submitOrder(any(Order.class));
        
        // Verify error notification was sent
        verify(notificationService).sendOrderErrorNotification(eq(testUserId), any(), eq(brokerError.getMessage()));
    }
    
    @Test
    void placeOrder_WithMarketOrder_ShouldNotRequirePrice() {
        // Arrange
        OrderRequest marketOrder = testOrderRequest.toBuilder()
            .orderType(OrderType.MARKET)
            .price(null)  // Market orders don't require price
            .build();
            
        Order marketOrderEntity = testOrder.toBuilder()
            .orderType(OrderType.MARKET)
            .price(null)
            .build();
            
        when(riskManagementService.validateOrder(eq(marketOrder), eq(testUserId)))
            .thenReturn(Result.success(null));
        when(orderRepository.save(any(Order.class))).thenReturn(marketOrderEntity);
        when(brokerIntegrationService.submitOrder(any(Order.class)))
            .thenReturn(Result.success(marketOrderEntity));
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.placeOrder(marketOrder, testUserId);
        
        // Assert
        assertTrue(result.isSuccess());
        
        OrderResponse response = result.getValue();
        assertEquals(OrderType.MARKET, response.orderType());
        assertNull(response.getPrice()); // Market orders have no price limit
    }
    
    @Test
    void getOrder_WithValidOrderId_ShouldReturnOrder() {
        // Arrange
        when(orderRepository.findByIdAndUserId(testOrderId, testUserId))
            .thenReturn(Optional.of(testOrder));
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.getOrder(testOrderId, testUserId);
        
        // Assert
        assertTrue(result.isSuccess());
        
        OrderResponse response = result.getValue();
        assertEquals(testOrderId, response.getOrderId());
        assertEquals(testUserId, testOrder.getUserId());
    }
    
    @Test
    void getOrder_WithInvalidOrderId_ShouldReturnError() {
        // Arrange
        String invalidOrderId = "INVALID_ORDER_123";
        when(orderRepository.findByIdAndUserId(invalidOrderId, testUserId))
            .thenReturn(Optional.empty());
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.getOrder(invalidOrderId, testUserId);
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals("ORDER_NOT_FOUND", result.getError().getCode());
        assertTrue(result.getError().getMessage().contains("not found"));
    }
    
    @Test
    void getOrdersByUser_WithValidUserId_ShouldReturnOrders() {
        // Arrange
        Order order2 = testOrder.toBuilder()
            .id("ORDER_12345_002")
            .symbol("TCS")
            .side(OrderSide.SELL)
            .build();
            
        List<Order> orders = Arrays.asList(testOrder, order2);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
            .thenReturn(orders);
        
        // Act
        Result<List<OrderResponse>, TradeError> result = orderService.getOrdersByUser(testUserId, pageable);
        
        // Assert
        assertTrue(result.isSuccess());
        
        List<OrderResponse> responses = result.getValue();
        assertEquals(2, responses.size());
        assertEquals(testOrderId, responses.get(0).getOrderId());
        assertEquals("ORDER_12345_002", responses.get(1).getOrderId());
    }
    
    @Test
    void getOrdersByUserAndStatus_WithActiveStatus_ShouldReturnActiveOrders() {
        // Arrange
        Order activeOrder = testOrder.toBuilder()
            .status(OrderStatus.PENDING)
            .build();
            
        List<Order> activeOrders = Arrays.asList(activeOrder);
        
        when(orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(testUserId, OrderStatus.PENDING))
            .thenReturn(activeOrders);
        
        // Act
        Result<List<OrderResponse>, TradeError> result = 
            orderService.getOrdersByUserAndStatus(testUserId, OrderStatus.PENDING);
        
        // Assert
        assertTrue(result.isSuccess());
        
        List<OrderResponse> responses = result.getValue();
        assertEquals(1, responses.size());
        assertEquals(OrderStatus.PENDING, responses.get(0).getStatus());
    }
    
    @Test
    void getActiveOrders_ShouldReturnOnlyActiveStatuses() {
        // Arrange
        Order pendingOrder = testOrder.toBuilder().status(OrderStatus.PENDING).build();
        Order partiallyFilledOrder = testOrder.toBuilder()
            .id("ORDER_12345_002")
            .status(OrderStatus.PARTIALLY_FILLED)
            .build();
            
        List<Order> activeOrders = Arrays.asList(pendingOrder, partiallyFilledOrder);
        
        when(orderRepository.findActiveOrdersByUserId(testUserId))
            .thenReturn(activeOrders);
        
        // Act
        Result<List<OrderResponse>, TradeError> result = orderService.getActiveOrders(testUserId);
        
        // Assert
        assertTrue(result.isSuccess());
        
        List<OrderResponse> responses = result.getValue();
        assertEquals(2, responses.size());
        
        // Verify only active statuses are returned
        responses.forEach(response -> 
            assertTrue(response.getStatus() == OrderStatus.PENDING || 
                      response.getStatus() == OrderStatus.PARTIALLY_FILLED));
    }
    
    @Test
    void cancelOrder_WithValidPendingOrder_ShouldSucceed() {
        // Arrange
        when(orderRepository.findByIdAndUserId(testOrderId, testUserId))
            .thenReturn(Optional.of(testOrder));
        when(brokerIntegrationService.cancelOrder(testOrder))
            .thenReturn(Result.success(testOrder.toBuilder().status(OrderStatus.CANCELLED).build()));
        when(orderRepository.save(any(Order.class)))
            .thenReturn(testOrder.toBuilder().status(OrderStatus.CANCELLED).build());
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.cancelOrder(testOrderId, testUserId);
        
        // Assert
        assertTrue(result.isSuccess());
        
        OrderResponse response = result.getValue();
        assertEquals(testOrderId, response.getOrderId());
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        
        // Verify interactions
        verify(brokerIntegrationService).cancelOrder(testOrder);
        verify(orderRepository).save(any(Order.class));
        verify(notificationService).sendOrderCancelledNotification(eq(testUserId), any(OrderResponse.class));
    }
    
    @Test
    void cancelOrder_WithAlreadyFilledOrder_ShouldReturnError() {
        // Arrange
        Order filledOrder = testOrder.toBuilder()
            .status(OrderStatus.FILLED)
            .build();
            
        when(orderRepository.findByIdAndUserId(testOrderId, testUserId))
            .thenReturn(Optional.of(filledOrder));
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.cancelOrder(testOrderId, testUserId);
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals("CANNOT_CANCEL_FILLED_ORDER", result.getError().getCode());
        
        // Verify no broker call was made
        verify(brokerIntegrationService, never()).cancelOrder(any());
    }
    
    @Test
    void modifyOrder_WithValidModification_ShouldSucceed() {
        // Arrange
        OrderRequest modificationRequest = testOrderRequest.toBuilder()
            .price(new BigDecimal("2500.00"))  // Modified price
            .quantity(150)  // Modified quantity
            .build();
            
        Order modifiedOrder = testOrder.toBuilder()
            .price(new BigDecimal("2500.00"))
            .quantity(150)
            .remainingQuantity(150)
            .updatedAt(Instant.now())
            .build();
            
        when(orderRepository.findByIdAndUserId(testOrderId, testUserId))
            .thenReturn(Optional.of(testOrder));
        when(riskManagementService.validateOrderModification(eq(testOrder), eq(modificationRequest), eq(testUserId)))
            .thenReturn(Result.success(null));
        when(brokerIntegrationService.modifyOrder(any(Order.class), eq(modificationRequest)))
            .thenReturn(Result.success(modifiedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(modifiedOrder);
        
        // Act
        Result<OrderResponse, TradeError> result = 
            orderService.modifyOrder(testOrderId, modificationRequest, testUserId);
        
        // Assert
        assertTrue(result.isSuccess());
        
        OrderResponse response = result.getValue();
        assertEquals(new BigDecimal("2500.00"), response.getPrice());
        assertEquals(150, response.quantity());
        
        // Verify interactions
        verify(riskManagementService).validateOrderModification(eq(testOrder), eq(modificationRequest), eq(testUserId));
        verify(brokerIntegrationService).modifyOrder(any(Order.class), eq(modificationRequest));
        verify(orderRepository).save(any(Order.class));
    }
    
    @Test
    void processOrderFill_WithPartialFill_ShouldUpdateQuantities() {
        // Arrange
        Integer fillQuantity = 50;  // Partial fill
        BigDecimal fillPrice = new BigDecimal("2455.00");
        
        // Act
        Order result = orderService.processOrderFill(testOrder, fillQuantity, fillPrice);
        
        // Assert
        assertEquals(50, result.getFilledQuantity());
        assertEquals(50, result.getRemainingQuantity());  // 100 - 50
        assertEquals(OrderStatus.PARTIALLY_FILLED, result.getStatus());
        assertEquals(fillPrice, result.getAveragePrice());
        
        // Verify order was saved
        verify(orderRepository).save(result);
        
        // Verify position update
        verify(portfolioService).updatePosition(eq(testUserId), eq(testSymbol), eq(fillQuantity), eq(fillPrice));
    }
    
    @Test
    void processOrderFill_WithFullFill_ShouldCompleteOrder() {
        // Arrange
        Integer fillQuantity = 100;  // Full fill
        BigDecimal fillPrice = new BigDecimal("2455.00");
        
        // Act
        Order result = orderService.processOrderFill(testOrder, fillQuantity, fillPrice);
        
        // Assert
        assertEquals(100, result.getFilledQuantity());
        assertEquals(0, result.getRemainingQuantity());
        assertEquals(OrderStatus.FILLED, result.getStatus());
        assertEquals(fillPrice, result.getAveragePrice());
    }
    
    @Test
    void getOrderCounts_ShouldReturnStatusCounts() {
        // Arrange
        Map<String, Long> expectedCounts = Map.of(
            "PENDING", 5L,
            "PARTIALLY_FILLED", 2L,
            "FILLED", 10L,
            "CANCELLED", 3L
        );
        
        when(orderRepository.getOrderCountsByStatus(testUserId))
            .thenReturn(expectedCounts);
        
        // Act
        Result<Map<String, Long>, TradeError> result = orderService.getOrderCounts(testUserId);
        
        // Assert
        assertTrue(result.isSuccess());
        
        Map<String, Long> counts = result.getValue();
        assertEquals(expectedCounts, counts);
    }
    
    @Test
    void updateOrderStatus_WithValidTransition_ShouldUpdateStatus() {
        // Arrange
        String reason = "Order filled by broker";
        OrderStatus newStatus = OrderStatus.FILLED;
        
        when(orderRepository.findById(testOrderId))
            .thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class)))
            .thenReturn(testOrder.toBuilder().status(newStatus).build());
        
        // Act
        Order result = orderService.updateOrderStatus(testOrderId, newStatus, reason);
        
        // Assert
        assertEquals(newStatus, result.getStatus());
        
        // Verify save was called with updated order
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        
        Order savedOrder = orderCaptor.getValue();
        assertEquals(newStatus, savedOrder.getStatus());
    }
    
    @Test
    void expireOrders_ShouldExpireTimedOutOrders() {
        // Arrange
        List<Order> expiredOrders = Arrays.asList(
            testOrder.toBuilder().id("EXPIRED_001").build(),
            testOrder.toBuilder().id("EXPIRED_002").build()
        );
        
        when(orderRepository.findExpiredOrders())
            .thenReturn(expiredOrders);
        when(orderRepository.saveAll(anyList()))
            .thenReturn(expiredOrders);
        
        // Act
        Long expiredCount = orderService.expireOrders();
        
        // Assert
        assertEquals(2L, expiredCount);
        
        // Verify all expired orders were updated
        ArgumentCaptor<List<Order>> ordersCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).saveAll(ordersCaptor.capture());
        
        List<Order> savedOrders = ordersCaptor.getValue();
        assertEquals(2, savedOrders.size());
        savedOrders.forEach(order -> assertEquals(OrderStatus.EXPIRED, order.getStatus()));
    }
    
    @Test
    void placeOrder_WithInvalidSymbol_ShouldReturnValidationError() {
        // Arrange
        OrderRequest invalidRequest = testOrderRequest.toBuilder()
            .symbol("")  // Invalid empty symbol
            .build();
            
        TradeError validationError = TradeError.validationError("Invalid symbol");
        when(riskManagementService.validateOrder(eq(invalidRequest), eq(testUserId)))
            .thenReturn(Result.failure(validationError));
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.placeOrder(invalidRequest, testUserId);
        
        // Assert
        assertFalse(result.isSuccess());
        assertEquals("VALIDATION_ERROR", result.getError().getCode());
        assertTrue(result.getError().getMessage().contains("Invalid symbol"));
    }
    
    @Test
    void placeOrder_WithZeroQuantity_ShouldReturnValidationError() {
        // Arrange
        OrderRequest zeroQuantityRequest = testOrderRequest.toBuilder()
            .quantity(0)  // Invalid zero quantity
            .build();
            
        TradeError validationError = TradeError.validationError("Quantity must be greater than zero");
        when(riskManagementService.validateOrder(eq(zeroQuantityRequest), eq(testUserId)))
            .thenReturn(Result.failure(validationError));
        
        // Act
        Result<OrderResponse, TradeError> result = orderService.placeOrder(zeroQuantityRequest, testUserId);
        
        // Assert
        assertFalse(result.isSuccess());
        assertTrue(result.getError().getMessage().contains("Quantity must be greater than zero"));
    }
}