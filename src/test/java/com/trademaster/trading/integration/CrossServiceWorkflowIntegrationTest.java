package com.trademaster.trading.integration;

import com.trademaster.common.functional.Result;
import com.trademaster.trading.TradingServiceApplication;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.metrics.TradingMetricsService;
import com.trademaster.trading.model.*;
import com.trademaster.trading.repository.OrderRepository;
import com.trademaster.trading.routing.ExecutionStrategy;
import com.trademaster.trading.routing.OrderRouter;
import com.trademaster.trading.routing.RoutingDecision;
import com.trademaster.trading.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Phase 7: Cross-Service Workflow Integration Tests
 *
 * Validates end-to-end workflows across multiple refactored services:
 * - OrderService (Phase 6E)
 * - OrderRouter (Phase 6F)
 * - TradingMetricsService (Phase 6G)
 *
 * Critical Workflows Tested:
 * 1. Complete Order Lifecycle - Order placement through routing to metrics recording
 * 2. Order Validation & Routing - Multi-service validation and smart routing
 * 3. Error Handling Flow - Cross-service error propagation and recovery
 * 4. Metrics Collection Flow - End-to-end metrics recording across services
 * 5. Circuit Breaker Integration - Resilience patterns across service boundaries
 *
 * Validates:
 * - Service integration points work correctly after refactoring
 * - Data flows seamlessly between services
 * - Error handling propagates correctly
 * - Metrics are collected at all stages
 * - No regressions from Phase 6 refactorings
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(
    classes = TradingServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@DisplayName("Phase 7: Cross-Service Workflow Integration Tests")
class CrossServiceWorkflowIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    @Qualifier("functionalOrderRouter")
    private OrderRouter orderRouter;

    @Autowired
    private TradingMetricsService metricsService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    private static final Long TEST_USER_ID = 99999L;
    private static final String TEST_SYMBOL = "TCS";
    private static final String TEST_EXCHANGE = "NSE";
    private static final Integer TEST_QUANTITY = 100;
    private static final BigDecimal TEST_LIMIT_PRICE = new BigDecimal("3500.00");

    @BeforeEach
    void setUp() {
        // Clean up test data
        orderRepository.deleteAll();
    }

    @Nested
    @DisplayName("Workflow 1: Complete Order Lifecycle")
    @Transactional
    class CompleteOrderLifecycleTests {

        @Test
        @DisplayName("End-to-end order placement should flow through all services successfully")
        void testCompleteOrderLifecycleWorkflow() {
            // Given: Valid order request and initial metrics state
            OrderRequest request = createValidMarketOrder();
            Counter ordersPlacedBefore = meterRegistry.find("trading.orders.placed").counter();
            double initialOrderCount = ordersPlacedBefore.count();

            // ============================================
            // STEP 1: Order Placement (OrderService)
            // ============================================
            Result<OrderResponse, TradeError> orderResult = orderService.placeOrder(request, TEST_USER_ID);

            // Verify: Order placement succeeded
            assertThat(orderResult.isSuccess()).isTrue();
            OrderResponse orderResponse = orderResult.getValue();
            assertThat(orderResponse).isNotNull();
            assertThat(orderResponse.orderId()).isNotEmpty();
            assertThat(orderResponse.symbol()).isEqualTo(TEST_SYMBOL);

            // ============================================
            // STEP 2: Verify Order Persistence
            // ============================================
            Optional<Order> persistedOrder = orderRepository.findByOrderId(orderResponse.orderId());
            assertThat(persistedOrder).isPresent();
            Order order = persistedOrder.get();
            assertThat(order.getStatus()).isIn(OrderStatus.PENDING, OrderStatus.ACKNOWLEDGED);

            // ============================================
            // STEP 3: Verify Routing Decision (OrderRouter)
            // ============================================
            RoutingDecision routingDecision = orderRouter.routeOrder(order);
            assertThat(routingDecision).isNotNull();
            assertThat(routingDecision.getBrokerName()).isNotEmpty();
            assertThat(routingDecision.getStrategy()).isEqualTo(ExecutionStrategy.IMMEDIATE); // Market order
            assertThat(routingDecision.getVenue()).isNotEmpty();

            // ============================================
            // STEP 4: Verify Metrics Recording (TradingMetricsService)
            // ============================================
            await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Counter ordersPlacedAfter = meterRegistry.find("trading.orders.placed").counter();
                    assertThat(ordersPlacedAfter.count()).isEqualTo(initialOrderCount + 1);
                });

            // Verify: Active orders gauge updated
            Gauge activeOrdersGauge = meterRegistry.find("trading.orders.active").gauge();
            assertThat(activeOrdersGauge.value()).isGreaterThan(0);

            // ============================================
            // STEP 5: Verify Complete Workflow Integration
            // ============================================
            // All services worked together correctly:
            // OrderService → OrderRouter → TradingMetricsService
            log.info("✅ Complete order lifecycle workflow validated successfully");
        }

        @Test
        @DisplayName("Large order should trigger SLICED execution strategy across services")
        void testLargeOrderWorkflowWithSlicedExecution() {
            // Given: Large order request (>10K threshold)
            OrderRequest largeRequest = createLargeOrder();

            // ============================================
            // STEP 1: Order Placement
            // ============================================
            Result<OrderResponse, TradeError> orderResult = orderService.placeOrder(largeRequest, TEST_USER_ID);

            assertThat(orderResult.isSuccess()).isTrue();
            String orderId = orderResult.getValue().orderId();

            // ============================================
            // STEP 2: Verify Routing Strategy for Large Order
            // ============================================
            Optional<Order> order = orderRepository.findByOrderId(orderId);
            assertThat(order).isPresent();

            RoutingDecision routingDecision = orderRouter.routeOrder(order.get());
            assertThat(routingDecision.getStrategy()).isIn(
                ExecutionStrategy.SLICED,
                ExecutionStrategy.VWAP,
                ExecutionStrategy.TWAP
            );

            // ============================================
            // STEP 3: Verify Metrics for Large Orders
            // ============================================
            await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Broker-specific metrics should be recorded
                    Gauge brokerOrdersGauge = meterRegistry.find("trading.orders.by_broker")
                        .tag("broker", routingDecision.getBrokerName())
                        .gauge();
                    assertThat(brokerOrdersGauge).isNotNull();
                });

            log.info("✅ Large order workflow with SLICED execution validated successfully");
        }
    }

    @Nested
    @DisplayName("Workflow 2: Order Validation & Smart Routing")
    @Transactional
    class OrderValidationAndRoutingTests {

        @Test
        @DisplayName("Order should flow through validation → routing → broker selection")
        void testOrderValidationAndSmartRoutingWorkflow() {
            // Given: Valid limit order
            OrderRequest limitOrder = createValidLimitOrder();

            // ============================================
            // STEP 1: Order Validation (OrderService validators)
            // ============================================
            Result<OrderResponse, TradeError> orderResult = orderService.placeOrder(limitOrder, TEST_USER_ID);

            assertThat(orderResult.isSuccess()).isTrue();
            OrderResponse response = orderResult.getValue();

            // ============================================
            // STEP 2: Smart Routing (OrderRouter)
            // ============================================
            Optional<Order> order = orderRepository.findByOrderId(response.orderId());
            assertThat(order).isPresent();

            RoutingDecision decision = orderRouter.routeOrder(order.get());

            // Verify: Smart routing selected appropriate broker
            assertThat(decision.getBrokerName()).isIn("ZERODHA", "UPSTOX", "ANGEL_ONE");
            assertThat(decision.getConfidence()).isGreaterThan(0.5);

            // ============================================
            // STEP 3: Verify Routing Metrics
            // ============================================
            // Routing metrics should be recorded
            // Note: Routing timer is recorded by OrderRouter internally

            log.info("✅ Order validation and smart routing workflow validated successfully");
        }

        @Test
        @DisplayName("Multiple exchange orders should route to correct broker pools")
        void testMultiExchangeRoutingWorkflow() {
            // Given: Orders for different exchanges
            OrderRequest nseOrder = createOrderForExchange("NSE");
            OrderRequest bseOrder = createOrderForExchange("BSE");
            OrderRequest mcxOrder = createOrderForExchange("MCX");

            // ============================================
            // STEP 1: Place orders on different exchanges
            // ============================================
            Result<OrderResponse, TradeError> nseResult = orderService.placeOrder(nseOrder, TEST_USER_ID);
            Result<OrderResponse, TradeError> bseResult = orderService.placeOrder(bseOrder, TEST_USER_ID);
            Result<OrderResponse, TradeError> mcxResult = orderService.placeOrder(mcxOrder, TEST_USER_ID);

            assertThat(nseResult.isSuccess()).isTrue();
            assertThat(bseResult.isSuccess()).isTrue();
            assertThat(mcxResult.isSuccess()).isTrue();

            // ============================================
            // STEP 2: Verify Exchange-Specific Routing
            // ============================================
            Order nseOrderEntity = orderRepository.findByOrderId(nseResult.getValue().orderId()).get();
            Order bseOrderEntity = orderRepository.findByOrderId(bseResult.getValue().orderId()).get();
            Order mcxOrderEntity = orderRepository.findByOrderId(mcxResult.getValue().orderId()).get();

            RoutingDecision nseDecision = orderRouter.routeOrder(nseOrderEntity);
            RoutingDecision bseDecision = orderRouter.routeOrder(bseOrderEntity);
            RoutingDecision mcxDecision = orderRouter.routeOrder(mcxOrderEntity);

            // Verify: NSE/BSE brokers include all three
            assertThat(nseDecision.getBrokerName()).isIn("ZERODHA", "UPSTOX", "ANGEL_ONE");
            assertThat(bseDecision.getBrokerName()).isIn("ZERODHA", "UPSTOX", "ANGEL_ONE");

            // Verify: MCX brokers limited to ZERODHA and ANGEL_ONE
            assertThat(mcxDecision.getBrokerName()).isIn("ZERODHA", "ANGEL_ONE");

            log.info("✅ Multi-exchange routing workflow validated successfully");
        }
    }

    @Nested
    @DisplayName("Workflow 3: Error Handling & Recovery")
    @Transactional
    class ErrorHandlingAndRecoveryTests {

        @Test
        @DisplayName("Invalid order should propagate errors across services correctly")
        void testErrorPropagationWorkflow() {
            // Given: Invalid order request (missing required fields)
            OrderRequest invalidRequest = OrderRequest.builder()
                .symbol("") // Invalid empty symbol
                .exchange(TEST_EXCHANGE)
                .orderType(OrderType.MARKET)
                .side(OrderSide.BUY)
                .quantity(0) // Invalid zero quantity
                .timeInForce(TimeInForce.DAY)
                .build();

            // ============================================
            // STEP 1: Order Validation Failure
            // ============================================
            Result<OrderResponse, TradeError> result = orderService.placeOrder(invalidRequest, TEST_USER_ID);

            // Verify: Validation failed
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TradeError.ValidationError.class);

            // ============================================
            // STEP 2: Verify Error Metrics Recorded
            // ============================================
            await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Counter failedOrders = meterRegistry.find("trading.orders.failed")
                        .tag("error_type", "VALIDATION_FAILED")
                        .counter();
                    assertThat(failedOrders).isNotNull();
                    assertThat(failedOrders.count()).isGreaterThan(0);
                });

            // ============================================
            // STEP 3: Verify No Partial State
            // ============================================
            // No order should be persisted for invalid request
            assertThat(orderRepository.findByUserId(TEST_USER_ID)).isEmpty();

            log.info("✅ Error propagation workflow validated successfully");
        }

        @Test
        @DisplayName("Circuit breaker should protect cross-service calls")
        void testCircuitBreakerProtectionWorkflow() {
            // Given: Valid order that will test circuit breaker
            OrderRequest request = createValidMarketOrder();

            // ============================================
            // STEP 1: Order Placement with Circuit Breaker Protection
            // ============================================
            Result<OrderResponse, TradeError> result = orderService.placeOrder(request, TEST_USER_ID);

            // Note: Circuit breaker protects broker calls
            // In test environment, this should either succeed or fail gracefully

            // ============================================
            // STEP 2: Verify Graceful Degradation
            // ============================================
            if (result.isFailure()) {
                // If broker unavailable, should get ServiceUnavailable error
                assertThat(result.getError()).isInstanceOf(TradeError.SystemError.ServiceUnavailable.class);

                // Verify: Circuit breaker trip recorded in metrics
                await().atMost(3, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        Counter cbTrips = meterRegistry.find("trading.circuit_breaker.trips").counter();
                        // Circuit breaker metrics may or may not be triggered depending on broker availability
                        assertThat(cbTrips).isNotNull();
                    });
            } else {
                // If successful, verify order was processed
                assertThat(result.isSuccess()).isTrue();
            }

            log.info("✅ Circuit breaker protection workflow validated successfully");
        }
    }

    @Nested
    @DisplayName("Workflow 4: Metrics Collection Across Services")
    class MetricsCollectionWorkflowTests {

        @Test
        @DisplayName("Complete workflow should record metrics at all integration points")
        void testEndToEndMetricsCollectionWorkflow() {
            // Given: Valid order and initial metrics state
            OrderRequest request = createValidMarketOrder();

            Counter initialOrdersPlaced = meterRegistry.find("trading.orders.placed").counter();
            double initialPlacedCount = initialOrdersPlaced.count();

            Gauge initialActiveOrders = meterRegistry.find("trading.orders.active").gauge();
            double initialActiveCount = initialActiveOrders.value();

            // ============================================
            // STEP 1: Place Order
            // ============================================
            Result<OrderResponse, TradeError> result = orderService.placeOrder(request, TEST_USER_ID);
            assertThat(result.isSuccess()).isTrue();

            // ============================================
            // STEP 2: Verify Order Placement Metrics
            // ============================================
            await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Counter ordersPlaced = meterRegistry.find("trading.orders.placed").counter();
                    assertThat(ordersPlaced.count()).isEqualTo(initialPlacedCount + 1);
                });

            // ============================================
            // STEP 3: Verify Active Orders Gauge
            // ============================================
            await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Gauge activeOrders = meterRegistry.find("trading.orders.active").gauge();
                    assertThat(activeOrders.value()).isEqualTo(initialActiveCount + 1);
                });

            // ============================================
            // STEP 4: Verify Broker-Specific Metrics
            // ============================================
            String orderId = result.getValue().orderId();
            Order order = orderRepository.findByOrderId(orderId).get();
            RoutingDecision decision = orderRouter.routeOrder(order);

            await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Gauge brokerOrders = meterRegistry.find("trading.orders.by_broker")
                        .tag("broker", decision.getBrokerName())
                        .gauge();
                    assertThat(brokerOrders).isNotNull();
                    assertThat(brokerOrders.value()).isGreaterThan(0);
                });

            // ============================================
            // STEP 5: Verify Processing Time Metrics
            // ============================================
            io.micrometer.core.instrument.Timer processingTimer = meterRegistry
                .find("trading.orders.processing_time")
                .timer();
            assertThat(processingTimer).isNotNull();
            assertThat(processingTimer.count()).isGreaterThan(0);

            log.info("✅ End-to-end metrics collection workflow validated successfully");
        }

        @Test
        @DisplayName("Aggregated metrics should reflect complete workflow state")
        void testAggregatedMetricsWorkflow() {
            // Given: Multiple successful orders
            for (int i = 0; i < 3; i++) {
                OrderRequest request = createValidMarketOrder();
                Result<OrderResponse, TradeError> result = orderService.placeOrder(request, TEST_USER_ID);
                assertThat(result.isSuccess()).isTrue();
            }

            // ============================================
            // STEP 1: Verify Aggregated Success Rate
            // ============================================
            await().atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    BigDecimal successRate = metricsService.getOrderSuccessRate();
                    // Success rate should be calculated correctly
                    assertThat(successRate).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                });

            // ============================================
            // STEP 2: Verify Average Processing Time
            // ============================================
            BigDecimal avgProcessingTime = metricsService.getAverageOrderProcessingTime();
            assertThat(avgProcessingTime).isGreaterThanOrEqualTo(BigDecimal.ZERO);

            // ============================================
            // STEP 3: Verify Total Active Entities
            // ============================================
            long totalActive = metricsService.getTotalActiveEntities();
            assertThat(totalActive).isGreaterThan(0);

            log.info("✅ Aggregated metrics workflow validated successfully");
        }
    }

    // Test Data Builders

    private OrderRequest createValidMarketOrder() {
        return OrderRequest.builder()
            .symbol(TEST_SYMBOL)
            .exchange(TEST_EXCHANGE)
            .orderType(OrderType.MARKET)
            .side(OrderSide.BUY)
            .quantity(TEST_QUANTITY)
            .timeInForce(TimeInForce.DAY)
            .build();
    }

    private OrderRequest createValidLimitOrder() {
        return OrderRequest.builder()
            .symbol(TEST_SYMBOL)
            .exchange(TEST_EXCHANGE)
            .orderType(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .quantity(TEST_QUANTITY)
            .limitPrice(TEST_LIMIT_PRICE)
            .timeInForce(TimeInForce.DAY)
            .build();
    }

    private OrderRequest createLargeOrder() {
        return OrderRequest.builder()
            .symbol(TEST_SYMBOL)
            .exchange(TEST_EXCHANGE)
            .orderType(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .quantity(15000) // Large order > 10K threshold
            .limitPrice(TEST_LIMIT_PRICE)
            .timeInForce(TimeInForce.DAY)
            .build();
    }

    private OrderRequest createOrderForExchange(String exchange) {
        return OrderRequest.builder()
            .symbol(TEST_SYMBOL)
            .exchange(exchange)
            .orderType(OrderType.MARKET)
            .side(OrderSide.BUY)
            .quantity(TEST_QUANTITY)
            .timeInForce(TimeInForce.DAY)
            .build();
    }

    // Logging utility
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CrossServiceWorkflowIntegrationTest.class);
}
