package com.trademaster.trading.integration;

import com.trademaster.trading.TradingServiceApplication;
import com.trademaster.common.functional.Result;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.model.TimeInForce;
import com.trademaster.trading.repository.OrderRepository;
import com.trademaster.trading.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TradeMaster Trading Service
 * 
 * Tests complete order lifecycle with real databases and messaging:
 * - PostgreSQL for order persistence and transactions
 * - Redis for caching and session management  
 * - Kafka for order events and notifications
 * - Concurrent order processing with Virtual Threads
 * 
 * MANDATORY: TestContainers for enterprise-grade integration testing
 * MANDATORY: 80%+ coverage with realistic financial workflows
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(classes = TradingServiceApplication.class)
@Testcontainers
@ActiveProfiles("integration-test")
@Transactional
public class TradingServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("trademaster_trading_test")
            .withUsername("test_user") 
            .withPassword("test_password")
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofMinutes(1));

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        
        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
        
        // JPA configuration for testing
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
        
        // Virtual Threads configuration
        registry.add("spring.threads.virtual.enabled", () -> "true");
        
        // Disable external service calls for integration tests
        registry.add("trademaster.brokers.mock-mode", () -> "true");
        registry.add("trademaster.risk.bypass-external-checks", () -> "true");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    private Long testUserId;
    private String testSymbol;

    @BeforeEach
    void setUp() {
        testUserId = 12345L;
        testSymbol = "RELIANCE";
        
        // Clear any existing test data
        orderRepository.deleteAll();
    }

    @Test
    void placeOrder_WithValidLimitOrder_ShouldPersistAndSucceed() {
        // Arrange
        OrderRequest orderRequest = OrderRequest.builder()
                .symbol(testSymbol)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(100)
                .price(new BigDecimal("2450.75"))
                .timeInForce(TimeInForce.DAY)
                .build();

        // Act
        Result<OrderResponse, TradeError> result = orderService.placeOrder(orderRequest, testUserId);

        // Assert
        assertTrue(result.isSuccess(), "Order placement should succeed");
        
        OrderResponse response = result.getValue().orElseThrow();
        assertNotNull(response.getOrderId());
        assertEquals(testSymbol, response.symbol());
        assertEquals(OrderSide.BUY, response.side());
        assertEquals(OrderType.LIMIT, response.orderType());
        assertEquals(100, response.quantity());
        assertEquals(new BigDecimal("2450.75"), response.getPrice());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        
        // Verify persistence in database
        List<Order> persistedOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId, Pageable.unpaged()).getContent();
        assertEquals(1, persistedOrders.size());
        
        Order persistedOrder = persistedOrders.get(0);
        assertEquals(response.getOrderId(), persistedOrder.getId());
        assertEquals(testUserId, persistedOrder.getUserId());
        assertEquals(testSymbol, persistedOrder.symbol());
        assertEquals(OrderStatus.PENDING, persistedOrder.getStatus());
    }

    @Test
    void placeOrder_WithMarketOrder_ShouldSucceedWithoutPrice() {
        // Arrange
        OrderRequest marketOrder = OrderRequest.builder()
                .symbol(testSymbol)
                .side(OrderSide.SELL)
                .orderType(OrderType.MARKET)
                .quantity(50)
                .timeInForce(TimeInForce.IOC)
                .build();

        // Act
        Result<OrderResponse, TradeError> result = orderService.placeOrder(marketOrder, testUserId);

        // Assert
        assertTrue(result.isSuccess());
        
        OrderResponse response = result.getValue().orElseThrow();
        assertEquals(OrderType.MARKET, response.orderType());
        assertNull(response.getPrice());
        assertEquals(50, response.quantity());
        
        // Verify in database
        Order persistedOrder = orderRepository.findByOrderIdAndUserId(response.getOrderId(), testUserId)
                .orElseThrow(() -> new AssertionError("Order not found in database"));
        assertEquals(OrderType.MARKET, persistedOrder.orderType());
        assertNull(persistedOrder.getLimitPrice());
    }

    @Test
    void orderLifecycle_PlaceModifyCancel_ShouldWorkEndToEnd() {
        // Step 1: Place initial order
        OrderRequest initialOrder = OrderRequest.builder()
                .symbol(testSymbol)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(100)
                .price(new BigDecimal("2400.00"))
                .timeInForce(TimeInForce.DAY)
                .build();

        Result<OrderResponse, TradeError> placeResult = orderService.placeOrder(initialOrder, testUserId);
        assertTrue(placeResult.isSuccess());
        String orderId = placeResult.getValue().orElseThrow().getOrderId();

        // Step 2: Modify order price and quantity
        OrderRequest modification = OrderRequest.builder()
                .symbol(testSymbol)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(150)
                .limitPrice(new BigDecimal("2450.00"))
                .timeInForce(TimeInForce.DAY)
                .build();

        Result<OrderResponse, TradeError> modifyResult = orderService.modifyOrder(orderId, modification, testUserId);
        assertTrue(modifyResult.isSuccess());
        
        OrderResponse modifiedOrder = modifyResult.getValue().orElseThrow();
        assertEquals(150, modifiedOrder.quantity());
        assertEquals(new BigDecimal("2450.00"), modifiedOrder.getPrice());

        // Step 3: Cancel the order
        Result<OrderResponse, TradeError> cancelResult = orderService.cancelOrder(orderId, testUserId);
        assertTrue(cancelResult.isSuccess());
        assertEquals(OrderStatus.CANCELLED, cancelResult.getValue().orElseThrow().getStatus());

        // Verify final state in database
        Order finalOrder = orderRepository.findByOrderIdAndUserId(orderId, testUserId)
                .orElseThrow(() -> new AssertionError("Order not found"));
        assertEquals(OrderStatus.CANCELLED, finalOrder.getStatus());
        assertEquals(150, finalOrder.quantity());
        assertEquals(new BigDecimal("2450.00"), finalOrder.getLimitPrice());
    }

    @Test
    void concurrentOrderPlacement_With100Orders_ShouldHandleAllSuccessfully() throws InterruptedException {
        // Arrange
        int numberOfOrders = 100;
        CountDownLatch latch = new CountDownLatch(numberOfOrders);
        
        // Act - Submit 100 orders concurrently using Virtual Threads
        List<CompletableFuture<Result<OrderResponse, TradeError>>> futures = IntStream.range(0, numberOfOrders)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        OrderRequest order = OrderRequest.builder()
                                .symbol("STOCK_" + i)
                                .side(i % 2 == 0 ? OrderSide.BUY : OrderSide.SELL)
                                .orderType(OrderType.LIMIT)
                                .quantity(100 + i)
                                .price(new BigDecimal("2000.00").add(new BigDecimal(i)))
                                .timeInForce(TimeInForce.DAY)
                                .build();
                        
                        Result<OrderResponse, TradeError> result = orderService.placeOrder(order, testUserId + i);
                        return result;
                    } finally {
                        latch.countDown();
                    }
                }))
                .toList();

        // Wait for all orders to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All orders should complete within 30 seconds");

        // Assert - Verify all orders succeeded
        List<Result<OrderResponse, TradeError>> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        long successCount = results.stream()
                .mapToLong(result -> result.isSuccess() ? 1 : 0)
                .sum();
        
        assertEquals(numberOfOrders, successCount, "All concurrent orders should succeed");

        // Verify all orders are persisted
        long totalOrdersInDb = orderRepository.count();
        assertEquals(numberOfOrders, totalOrdersInDb, "All orders should be persisted in database");
    }

    @Test
    void orderRetrieval_WithMultipleOrdersAndStatuses_ShouldFilterCorrectly() {
        // Arrange - Create orders with different statuses
        createTestOrder("ORDER_001", OrderStatus.PENDING);
        createTestOrder("ORDER_002", OrderStatus.FILLED);
        createTestOrder("ORDER_003", OrderStatus.CANCELLED);
        createTestOrder("ORDER_004", OrderStatus.PARTIALLY_FILLED);
        createTestOrder("ORDER_005", OrderStatus.PENDING);

        // Act & Assert - Test different retrieval methods
        
        // Get all orders for user
        Result<List<OrderResponse>, TradeError> allOrders = orderService.getOrdersByUser(testUserId, null);
        assertTrue(allOrders.isSuccess());
        assertEquals(5, allOrders.getValue().orElseThrow().size());

        // Get only pending orders
        Result<List<OrderResponse>, TradeError> pendingOrders = 
                orderService.getOrdersByUserAndStatus(testUserId, OrderStatus.PENDING);
        assertTrue(pendingOrders.isSuccess());
        assertEquals(2, pendingOrders.getValue().orElseThrow().size());
        
        pendingOrders.getValue().orElseThrow().forEach(order -> 
                assertEquals(OrderStatus.PENDING, order.getStatus()));

        // Get active orders (pending + partially filled)
        Result<List<OrderResponse>, TradeError> activeOrders = orderService.getActiveOrders(testUserId);
        assertTrue(activeOrders.isSuccess());
        assertEquals(3, activeOrders.getValue().orElseThrow().size()); // PENDING (2) + PARTIALLY_FILLED (1)
        
        activeOrders.getValue().orElseThrow().forEach(order -> 
                assertTrue(order.getStatus() == OrderStatus.PENDING || 
                          order.getStatus() == OrderStatus.PARTIALLY_FILLED));
    }

    @Test
    void orderFillProcessing_WithPartialAndFullFills_ShouldUpdateCorrectly() {
        // Arrange - Place initial order
        OrderRequest orderRequest = OrderRequest.builder()
                .symbol(testSymbol)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(100)
                .limitPrice(new BigDecimal("2450.00"))
                .timeInForce(TimeInForce.DAY)
                .build();

        Result<OrderResponse, TradeError> placeResult = orderService.placeOrder(orderRequest, testUserId);
        assertTrue(placeResult.isSuccess());
        String orderId = placeResult.getValue().orElseThrow().getOrderId();

        Order initialOrder = orderRepository.findByOrderIdAndUserId(orderId, testUserId)
                .orElseThrow(() -> new AssertionError("Order not found"));

        // Act 1 - Process partial fill (50 out of 100 shares)
        Order partiallyFilledOrder = orderService.processOrderFill(
                initialOrder, 50, new BigDecimal("2455.00"));

        // Assert partial fill
        assertEquals(50, partiallyFilledOrder.getFilledQuantity());
        assertEquals(50, partiallyFilledOrder.getRemainingQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, partiallyFilledOrder.getStatus());
        assertEquals(new BigDecimal("2455.00"), partiallyFilledOrder.getAveragePrice());

        // Act 2 - Process remaining fill (50 remaining shares)  
        Order fullyFilledOrder = orderService.processOrderFill(
                partiallyFilledOrder, 50, new BigDecimal("2460.00"));

        // Assert full fill
        assertEquals(100, fullyFilledOrder.getFilledQuantity());
        assertEquals(0, fullyFilledOrder.getRemainingQuantity());
        assertEquals(OrderStatus.FILLED, fullyFilledOrder.getStatus());
        
        // Average price should be weighted average: (50*2455 + 50*2460) / 100 = 2457.50
        assertEquals(new BigDecimal("2457.50"), fullyFilledOrder.getAveragePrice());

        // Verify final state persisted in database
        Order persistedOrder = orderRepository.findByOrderIdAndUserId(orderId, testUserId)
                .orElseThrow(() -> new AssertionError("Order not found"));
        assertEquals(OrderStatus.FILLED, persistedOrder.getStatus());
        assertEquals(100, persistedOrder.getFilledQuantity());
        assertEquals(0, persistedOrder.getRemainingQuantity());
    }

    @Test
    void orderValidation_WithInvalidData_ShouldReturnErrors() {
        // Test 1: Invalid symbol (empty)
        OrderRequest invalidSymbol = OrderRequest.builder()
                .symbol("")
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(100)
                .limitPrice(new BigDecimal("2450.00"))
                .build();

        Result<OrderResponse, TradeError> result1 = orderService.placeOrder(invalidSymbol, testUserId);
        assertFalse(result1.isSuccess());

        // Test 2: Zero quantity
        OrderRequest zeroQuantity = OrderRequest.builder()
                .symbol(testSymbol)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(0)
                .limitPrice(new BigDecimal("2450.00"))
                .build();

        Result<OrderResponse, TradeError> result2 = orderService.placeOrder(zeroQuantity, testUserId);
        assertFalse(result2.isSuccess());

        // Test 3: Negative price
        OrderRequest negativePrice = OrderRequest.builder()
                .symbol(testSymbol)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(100)
                .price(new BigDecimal("-100.00"))
                .build();

        Result<OrderResponse, TradeError> result3 = orderService.placeOrder(negativePrice, testUserId);
        assertFalse(result3.isSuccess());

        // Verify no invalid orders were persisted
        List<Order> persistedOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(testUserId, Pageable.unpaged()).getContent();
        assertEquals(0, persistedOrders.size(), "No invalid orders should be persisted");
    }

    @Test
    void orderExpiration_WithExpiredOrders_ShouldUpdateStatusCorrectly() {
        // Arrange - Create orders with different time-in-force settings
        Order dayOrder = createTestOrder("DAY_ORDER_001", OrderStatus.PENDING);
        Order gooTillCancelOrder = createTestOrder("GTC_ORDER_001", OrderStatus.PENDING);
        
        // Simulate passage of time by updating created timestamp to past
        dayOrder.setCreatedAt(dayOrder.getCreatedAt().minus(Duration.ofHours(25))); // Day expired
        orderRepository.save(dayOrder);

        // Act - Expire old orders
        Long expiredCount = orderService.expireOrders();

        // Assert
        assertTrue(expiredCount > 0, "Some orders should have expired");

        // Verify expired order status updated
        Order expiredOrder = orderRepository.findById(dayOrder.getId())
                .orElseThrow(() -> new AssertionError("Expired order not found"));
        assertEquals(OrderStatus.EXPIRED, expiredOrder.getStatus());

        // Verify GTC order not affected
        Order gtcOrder = orderRepository.findById(gooTillCancelOrder.getId())
                .orElseThrow(() -> new AssertionError("GTC order not found"));
        assertEquals(OrderStatus.PENDING, gtcOrder.getStatus());
    }

    @Test
    void orderStatistics_ShouldReturnAccurateCounts() {
        // Arrange - Create orders with various statuses
        createTestOrder("PENDING_001", OrderStatus.PENDING);
        createTestOrder("PENDING_002", OrderStatus.PENDING);  
        createTestOrder("FILLED_001", OrderStatus.FILLED);
        createTestOrder("CANCELLED_001", OrderStatus.CANCELLED);
        createTestOrder("PARTIAL_001", OrderStatus.PARTIALLY_FILLED);

        // Act
        Result<java.util.Map<String, Long>, TradeError> result = orderService.getOrderCounts(testUserId);

        // Assert
        assertTrue(result.isSuccess());
        
        java.util.Map<String, Long> counts = result.getValue().orElseThrow();
        assertEquals(2L, counts.get("PENDING"));
        assertEquals(1L, counts.get("FILLED"));
        assertEquals(1L, counts.get("CANCELLED"));
        assertEquals(1L, counts.get("PARTIALLY_FILLED"));
    }

    /**
     * Helper method to create test orders with specific status
     */
    private Order createTestOrder(String orderId, OrderStatus status) {
        Order order = Order.builder()
                .orderId(orderId)
                .userId(testUserId)
                .symbol(testSymbol)
                .side(OrderSide.BUY)
                .orderType(OrderType.LIMIT)
                .quantity(100)
                .filledQuantity(status == OrderStatus.FILLED ? 100 : 0)
                .limitPrice(new BigDecimal("2450.00"))
                .status(status)
                .timeInForce(TimeInForce.DAY)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        
        return orderRepository.save(order);
    }
}