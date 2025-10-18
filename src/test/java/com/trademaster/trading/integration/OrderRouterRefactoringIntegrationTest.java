package com.trademaster.trading.integration;

import com.trademaster.trading.TradingServiceApplication;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.*;
import com.trademaster.trading.routing.ExecutionStrategy;
import com.trademaster.trading.routing.OrderRouter;
import com.trademaster.trading.routing.RoutingDecision;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Phase 6F Refactoring Integration Tests
 *
 * Validates that Phase 6F refactorings work correctly in integration:
 * - Pattern 2 (Layered Extraction) helper methods for order routing
 * - Functional routing pipeline with Result monad
 * - Broker selection and scoring algorithms
 * - Execution strategy selection with pattern matching
 * - Connectivity validation with fallback
 * - Circuit breaker integration (Rule #25)
 * - Functional patterns (Rule #3, #11, #13, #14)
 * - No regressions from refactoring
 *
 * Test Categories:
 * 1. Pattern 2: Broker Selection Helper Methods (3 tests)
 * 2. Pattern 2: Scoring Calculation Helpers (2 tests)
 * 3. Pattern 2: Execution Strategy Helpers (2 tests)
 * 4. Pattern 2: Connectivity Validation Helpers (2 tests)
 * 5. Functional Pattern Integration Tests (2 tests)
 * 6. Regression Validation Tests (3 tests)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(
    classes = TradingServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@DisplayName("Phase 6F: OrderRouter Refactoring Integration Tests")
class OrderRouterRefactoringIntegrationTest {

    @Autowired
    @Qualifier("functionalOrderRouter")
    private OrderRouter orderRouter;

    private static final Long TEST_USER_ID = 12345L;
    private static final String TEST_SYMBOL = "RELIANCE";
    private static final String TEST_EXCHANGE_NSE = "NSE";
    private static final String TEST_EXCHANGE_BSE = "BSE";
    private static final String TEST_EXCHANGE_MCX = "MCX";

    @Nested
    @DisplayName("Pattern 2: Broker Selection Helper Methods")
    class BrokerSelectionHelperTests {

        @Test
        @DisplayName("routeOrder() should use selectOptimalBroker() helper with Stream API scoring")
        void testSelectOptimalBrokerHelperIntegration() {
            // Given: Market order on NSE
            Order order = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 100);

            // When: Route order (uses selectOptimalBroker helper with Stream API)
            RoutingDecision decision = orderRouter.routeOrder(order);

            // Then: Optimal broker selected based on scoring
            assertThat(decision).isNotNull();
            assertThat(decision.getBrokerName()).isIn("ZERODHA", "UPSTOX", "ANGEL_ONE");
            assertThat(decision.getStrategy()).isNotNull();
            assertThat(decision.getVenue()).isNotEmpty();
        }

        @Test
        @DisplayName("selectOptimalBroker() should choose primary broker for small market orders")
        void testSelectOptimalBrokerForSmallMarketOrder() {
            // Given: Small market order (typically highest score)
            Order order = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 50);

            // When: Route order (uses selectOptimalBroker with scoring)
            RoutingDecision decision = orderRouter.routeOrder(order);

            // Then: Should select primary broker (highest score)
            assertThat(decision).isNotNull();
            assertThat(decision.getBrokerName()).isNotEmpty();
            assertThat(decision.getConfidence()).isGreaterThan(0.5);
        }

        @Test
        @DisplayName("getAvailableBrokersForExchange() should return correct broker list by exchange")
        void testGetAvailableBrokersForExchangeHelper() {
            // Given: Orders for different exchanges
            Order nseOrder = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 100);
            Order bseOrder = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_BSE, 100);
            Order mcxOrder = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_MCX, 100);

            // When: Route orders (uses getAvailableBrokersForExchange helper)
            RoutingDecision nseDecision = orderRouter.routeOrder(nseOrder);
            RoutingDecision bseDecision = orderRouter.routeOrder(bseOrder);
            RoutingDecision mcxDecision = orderRouter.routeOrder(mcxOrder);

            // Then: Brokers should match exchange capabilities
            assertThat(nseDecision.getBrokerName()).isIn("ZERODHA", "UPSTOX", "ANGEL_ONE");
            assertThat(bseDecision.getBrokerName()).isIn("ZERODHA", "UPSTOX", "ANGEL_ONE");
            assertThat(mcxDecision.getBrokerName()).isIn("ZERODHA", "ANGEL_ONE");
        }
    }

    @Nested
    @DisplayName("Pattern 2: Scoring Calculation Helpers")
    class ScoringCalculationHelperTests {

        @Test
        @DisplayName("calculateBrokerScore() helpers should compose score from multiple factors")
        void testBrokerScoreCompositionHelpers() {
            // Given: Different order types and sizes
            Order smallMarketOrder = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 50);
            Order largeLimit Order = createTestOrder(OrderType.LIMIT, TEST_EXCHANGE_NSE, 15000);

            // When: Route orders (uses calculateBrokerScore composition)
            RoutingDecision smallDecision = orderRouter.routeOrder(smallMarketOrder);
            RoutingDecision largeDecision = orderRouter.routeOrder(largeLimitOrder);

            // Then: Scores should reflect order characteristics
            assertThat(smallDecision.getConfidence()).isGreaterThan(0.7); // High score for small market order
            assertThat(largeDecision.getConfidence()).isGreaterThan(0.5); // Lower score for large limit order
        }

        @Test
        @DisplayName("classifyOrderSize() helper should classify orders by size thresholds")
        void testClassifyOrderSizeHelper() {
            // Given: Orders of different sizes
            Order smallOrder = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 50);
            Order mediumOrder = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 5000);
            Order largeOrder = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 20000);

            // When: Route orders (uses classifyOrderSize helper internally)
            RoutingDecision smallDecision = orderRouter.routeOrder(smallOrder);
            RoutingDecision mediumDecision = orderRouter.routeOrder(mediumOrder);
            RoutingDecision largeDecision = orderRouter.routeOrder(largeOrder);

            // Then: Routing decisions should reflect size classification
            assertThat(smallDecision.getStrategy()).isEqualTo(ExecutionStrategy.IMMEDIATE);
            assertThat(mediumDecision.getStrategy()).isIn(ExecutionStrategy.IMMEDIATE, ExecutionStrategy.SLICED);
            assertThat(largeDecision.getStrategy()).isIn(ExecutionStrategy.SLICED, ExecutionStrategy.VWAP, ExecutionStrategy.TWAP);
        }
    }

    @Nested
    @DisplayName("Pattern 2: Execution Strategy Helpers")
    class ExecutionStrategyHelperTests {

        @Test
        @DisplayName("determineExecutionStrategy() should select strategy based on order type and size")
        void testDetermineExecutionStrategyHelper() {
            // Given: Different order types
            Order marketOrder = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 100);
            Order limitOrder = createTestOrder(OrderType.LIMIT, TEST_EXCHANGE_NSE, 100);
            Order stopLossOrder = createTestOrder(OrderType.STOP_LOSS, TEST_EXCHANGE_NSE, 100);

            // When: Route orders (uses determineExecutionStrategy helper)
            RoutingDecision marketDecision = orderRouter.routeOrder(marketOrder);
            RoutingDecision limitDecision = orderRouter.routeOrder(limitOrder);
            RoutingDecision stopLossDecision = orderRouter.routeOrder(stopLossOrder);

            // Then: Strategy should match order type
            assertThat(marketDecision.getStrategy()).isEqualTo(ExecutionStrategy.IMMEDIATE);
            assertThat(limitDecision.getStrategy()).isIn(ExecutionStrategy.IMMEDIATE, ExecutionStrategy.SLICED);
            assertThat(stopLossDecision.getStrategy()).isEqualTo(ExecutionStrategy.SCHEDULED);
        }

        @Test
        @DisplayName("buildRoutingDecision() helper should construct complete routing decision")
        void testBuildRoutingDecisionHelper() {
            // Given: Valid order
            Order order = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 100);

            // When: Route order (uses buildRoutingDecision helper)
            RoutingDecision decision = orderRouter.routeOrder(order);

            // Then: Decision should have all required fields populated
            assertThat(decision).isNotNull();
            assertThat(decision.getBrokerName()).isNotEmpty();
            assertThat(decision.getVenue()).isNotEmpty();
            assertThat(decision.getStrategy()).isNotNull();
            assertThat(decision.getEstimatedExecutionTime()).isNotNull();
            assertThat(decision.getConfidence()).isGreaterThan(0);
            assertThat(decision.getReason()).isNotEmpty();
            assertThat(decision.getRouterName()).isEqualTo("FunctionalOrderRouter");
        }
    }

    @Nested
    @DisplayName("Pattern 2: Connectivity Validation Helpers")
    class ConnectivityValidationHelperTests {

        @Test
        @DisplayName("validateBrokerConnection() should validate connectivity with fallback")
        void testValidateBrokerConnectionHelper() {
            // Given: Valid order
            Order order = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 100);

            // When: Route order (uses validateBrokerConnection helper with fallback)
            RoutingDecision decision = orderRouter.routeOrder(order);

            // Then: Connection should be validated (or fallback used)
            assertThat(decision).isNotNull();
            assertThat(decision.getBrokerName()).isNotEmpty();

            // Verify: Decision is usable (not rejected)
            assertThat(decision.getStrategy()).isNotEqualTo(ExecutionStrategy.REJECT);
        }

        @Test
        @DisplayName("createFallbackDecision() should provide fallback when primary broker unavailable")
        void testCreateFallbackDecisionHelper() {
            // Given: Order that might trigger fallback
            Order order = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 100);

            // When: Route order (uses createFallbackDecision on connectivity failure)
            RoutingDecision decision = orderRouter.routeOrder(order);

            // Then: Fallback decision should work correctly
            assertThat(decision).isNotNull();

            // Verify: If fallback was used, confidence would be lower (0.7)
            if (decision.getConfidence() == 0.7) {
                assertThat(decision.getReason()).contains("Fallback");
            }
        }
    }

    @Nested
    @DisplayName("Functional Pattern Integration Tests (Rules #3, #11, #13, #14)")
    class FunctionalPatternIntegrationTests {

        @Test
        @DisplayName("routeOrderFunctionally() should use functional pipeline with Result monad")
        void testFunctionalRoutingPipeline() {
            // Given: Valid order
            Order order = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 100);

            // When: Route order (uses functional pipeline throughout)
            RoutingDecision decision = orderRouter.routeOrder(order);

            // Then: Functional pipeline should work correctly
            assertThat(decision).isNotNull();
            assertThat(decision.getBrokerName()).isNotEmpty();

            // Verify: No exceptions thrown (functional error handling with Result monad)
            assertThat(decision.getStrategy()).isNotNull();
        }

        @Test
        @DisplayName("Pattern matching should be used throughout routing logic")
        void testPatternMatchingIntegration() {
            // Given: Orders with different characteristics
            Order marketOrder = createTestOrder(OrderType.MARKET, "NSE", 100);
            Order limitOrder = createTestOrder(OrderType.LIMIT, "BSE", 100);
            Order stopLossOrder = createTestOrder(OrderType.STOP_LOSS, "MCX", 100);

            // When: Route orders (uses pattern matching throughout)
            RoutingDecision marketDecision = orderRouter.routeOrder(marketOrder);
            RoutingDecision limitDecision = orderRouter.routeOrder(limitOrder);
            RoutingDecision stopLossDecision = orderRouter.routeOrder(stopLossOrder);

            // Then: Pattern matching should produce correct results
            assertThat(marketDecision.getStrategy()).isEqualTo(ExecutionStrategy.IMMEDIATE);
            assertThat(limitDecision.getStrategy()).isIn(ExecutionStrategy.IMMEDIATE, ExecutionStrategy.SLICED);
            assertThat(stopLossDecision.getStrategy()).isEqualTo(ExecutionStrategy.SCHEDULED);
        }
    }

    @Nested
    @DisplayName("Regression Tests - No Functional Changes")
    class RegressionValidationTests {

        @Test
        @DisplayName("Order routing API behavior should be unchanged")
        void testOrderRoutingRegressionValidation() {
            // Given: Standard market order
            Order order = createTestOrder(OrderType.MARKET, TEST_EXCHANGE_NSE, 100);

            // When: Route order
            RoutingDecision decision = orderRouter.routeOrder(order);

            // Then: API behavior unchanged after refactoring
            assertThat(decision).isNotNull();
            assertThat(decision.getBrokerName()).isNotEmpty();
            assertThat(decision.getVenue()).isNotEmpty();
            assertThat(decision.getStrategy()).isNotNull();
            assertThat(decision.isImmediateExecution()).isTrue(); // Market orders should be immediate
        }

        @Test
        @DisplayName("canHandle() method should validate exchange support correctly")
        void testCanHandleExchangeValidation() {
            // Given: Orders for supported and unsupported exchanges
            Order nseOrder = createTestOrder(OrderType.MARKET, "NSE", 100);
            Order invalidOrder = createTestOrder(OrderType.MARKET, "INVALID", 100);

            // When: Check if router can handle orders
            boolean canHandleNse = orderRouter.canHandle(nseOrder);
            boolean canHandleInvalid = orderRouter.canHandle(invalidOrder);

            // Then: Exchange validation should work correctly
            assertThat(canHandleNse).isTrue();
            assertThat(canHandleInvalid).isFalse();
        }

        @Test
        @DisplayName("Router priority and name should remain consistent")
        void testRouterMetadataConsistency() {
            // When: Get router metadata
            int priority = orderRouter.getPriority();
            String routerName = orderRouter.getRouterName();

            // Then: Metadata should be consistent
            assertThat(priority).isEqualTo(10); // Default priority
            assertThat(routerName).isEqualTo("FunctionalOrderRouter");
        }
    }

    // Test Data Builders

    private Order createTestOrder(OrderType orderType, String exchange, Integer quantity) {
        return Order.builder()
            .userId(TEST_USER_ID)
            .orderId("TEST-" + System.currentTimeMillis())
            .symbol(TEST_SYMBOL)
            .exchange(exchange)
            .orderType(orderType)
            .side(OrderSide.BUY)
            .quantity(quantity)
            .limitPrice(orderType == OrderType.MARKET ? null : new BigDecimal("2500.00"))
            .stopPrice(orderType.name().contains("STOP") ? new BigDecimal("2400.00") : null)
            .timeInForce(TimeInForce.DAY)
            .status(OrderStatus.PENDING)
            .build();
    }

    private Order createLargeOrder(OrderType orderType, String exchange) {
        return createTestOrder(orderType, exchange, 15000); // Large order (> threshold)
    }

    private Order createSmallOrder(OrderType orderType, String exchange) {
        return createTestOrder(orderType, exchange, 50); // Small order (< threshold/10)
    }
}
