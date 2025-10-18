package com.trademaster.trading.integration;

import com.trademaster.trading.TradingServiceApplication;
import com.trademaster.trading.metrics.TradingMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Phase 6G Refactoring Integration Tests
 *
 * Validates that Phase 6G refactorings work correctly in integration:
 * - Pattern 2 (Layered Extraction) helper methods for metrics initialization
 * - Metrics registration by business domain
 * - Counter, Timer, and Gauge functionality
 * - Thread-safe metric updates with AtomicReference/AtomicLong
 * - Prometheus metrics integration
 * - No regressions from refactoring
 *
 * Test Categories:
 * 1. Pattern 2: Metrics Initialization Helper Methods (2 tests)
 * 2. Pattern 2: Gauge Registration Helper Methods (2 tests)
 * 3. Order Processing Metrics Integration (2 tests)
 * 4. Risk Management Metrics Integration (2 tests)
 * 5. Financial & Performance Metrics Integration (2 tests)
 * 6. Regression Validation Tests (2 tests)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@SpringBootTest(
    classes = TradingServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@DisplayName("Phase 6G: TradingMetricsService Refactoring Integration Tests")
class TradingMetricsServiceRefactoringIntegrationTest {

    @Autowired
    private TradingMetricsService metricsService;

    @Autowired
    private MeterRegistry meterRegistry;

    private static final String TEST_BROKER = "ZERODHA";
    private static final BigDecimal TEST_ORDER_VALUE = new BigDecimal("10000.00");

    @BeforeEach
    void setUp() {
        // Metrics are initialized via @PostConstruct
        // No additional setup needed
    }

    @Nested
    @DisplayName("Pattern 2: Metrics Initialization Helper Methods")
    class MetricsInitializationHelperTests {

        @Test
        @DisplayName("initializeOrderMetrics() should register order processing metrics")
        void testInitializeOrderMetricsHelper() {
            // When: Metrics service initialized (via @PostConstruct)
            // Then: Order metrics should be registered

            // Verify: ordersPlaced counter exists
            Counter ordersPlaced = meterRegistry.find("trading.orders.placed").counter();
            assertThat(ordersPlaced).isNotNull();
            assertThat(ordersPlaced.getId().getDescription()).isEqualTo("Total number of orders placed");

            // Verify: ordersExecuted counter exists
            Counter ordersExecuted = meterRegistry.find("trading.orders.executed").counter();
            assertThat(ordersExecuted).isNotNull();

            // Verify: orderProcessingTime timer exists
            Timer orderProcessingTime = meterRegistry.find("trading.orders.processing_time").timer();
            assertThat(orderProcessingTime).isNotNull();
        }

        @Test
        @DisplayName("initializeRiskMetrics() should register risk management metrics")
        void testInitializeRiskMetricsHelper() {
            // When: Metrics service initialized (via @PostConstruct)
            // Then: Risk metrics should be registered

            // Verify: riskCheckTime timer exists
            Timer riskCheckTime = meterRegistry.find("trading.risk.check_time").timer();
            assertThat(riskCheckTime).isNotNull();
            assertThat(riskCheckTime.getId().getDescription()).isEqualTo("Risk assessment processing time");

            // Verify: riskViolations counter exists
            Counter riskViolations = meterRegistry.find("trading.risk.violations").counter();
            assertThat(riskViolations).isNotNull();

            // Verify: riskAlerts counter exists
            Counter riskAlerts = meterRegistry.find("trading.risk.alerts").counter();
            assertThat(riskAlerts).isNotNull();
        }
    }

    @Nested
    @DisplayName("Pattern 2: Gauge Registration Helper Methods")
    class GaugeRegistrationHelperTests {

        @Test
        @DisplayName("registerRiskGauges() should register risk exposure and loss gauges")
        void testRegisterRiskGaugesHelper() {
            // When: Metrics service initialized (via @PostConstruct)
            // Then: Risk gauges should be registered

            // Verify: totalExposure gauge exists
            Gauge totalExposure = meterRegistry.find("trading.risk.total_exposure").gauge();
            assertThat(totalExposure).isNotNull();
            assertThat(totalExposure.getId().getDescription()).isEqualTo("Current total market exposure");

            // Verify: maxDailyLoss gauge exists
            Gauge maxDailyLoss = meterRegistry.find("trading.risk.max_daily_loss").gauge();
            assertThat(maxDailyLoss).isNotNull();
            assertThat(maxDailyLoss.getId().getDescription()).isEqualTo("Maximum daily loss threshold");
        }

        @Test
        @DisplayName("registerFinancialGauges() should register PnL and fees gauges")
        void testRegisterFinancialGaugesHelper() {
            // When: Metrics service initialized (via @PostConstruct)
            // Then: Financial gauges should be registered

            // Verify: totalPnL gauge exists
            Gauge totalPnL = meterRegistry.find("trading.pnl.total").gauge();
            assertThat(totalPnL).isNotNull();
            assertThat(totalPnL.getId().getDescription()).isEqualTo("Total profit and loss");

            // Verify: dailyPnL gauge exists
            Gauge dailyPnL = meterRegistry.find("trading.pnl.daily").gauge();
            assertThat(dailyPnL).isNotNull();

            // Verify: totalFees gauge exists
            Gauge totalFees = meterRegistry.find("trading.fees.total").gauge();
            assertThat(totalFees).isNotNull();
        }
    }

    @Nested
    @DisplayName("Order Processing Metrics Integration")
    class OrderProcessingMetricsTests {

        @Test
        @DisplayName("recordOrderPlaced() should increment counter and record broker metrics")
        void testRecordOrderPlacedIntegration() {
            // Given: Initial counter value
            Counter ordersPlaced = meterRegistry.find("trading.orders.placed").counter();
            double initialCount = ordersPlaced.count();

            // When: Record order placed
            metricsService.recordOrderPlaced(TEST_BROKER, TEST_ORDER_VALUE);

            // Then: Counter should increment
            assertThat(ordersPlaced.count()).isEqualTo(initialCount + 1);

            // Verify: Broker metrics also recorded
            await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Gauge brokerOrdersGauge = meterRegistry.find("trading.orders.by_broker")
                        .tag("broker", TEST_BROKER)
                        .gauge();
                    assertThat(brokerOrdersGauge).isNotNull();
                });
        }

        @Test
        @DisplayName("startOrderProcessing() and recordOrderProcessingTime() should work together")
        void testOrderProcessingTimerIntegration() {
            // Given: Start order processing timer
            Timer.Sample sample = metricsService.startOrderProcessing();
            assertThat(sample).isNotNull();

            // When: Simulate processing time
            try {
                Thread.sleep(10); // 10ms processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When: Stop timer
            metricsService.recordOrderProcessingTime(sample);

            // Then: Timer should have recorded measurement
            Timer orderProcessingTime = meterRegistry.find("trading.orders.processing_time").timer();
            assertThat(orderProcessingTime.count()).isGreaterThan(0);
            assertThat(orderProcessingTime.mean(TimeUnit.MILLISECONDS)).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Risk Management Metrics Integration")
    class RiskManagementMetricsTests {

        @Test
        @DisplayName("recordRiskViolation() should record violation with tags")
        void testRecordRiskViolationIntegration() {
            // When: Record risk violation
            metricsService.recordRiskViolation("POSITION_LIMIT", "HIGH");

            // Then: Violation counter should increment with tags
            await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Counter violations = meterRegistry.find("trading.risk.violations")
                        .tag("violation_type", "POSITION_LIMIT")
                        .tag("severity", "HIGH")
                        .counter();
                    assertThat(violations).isNotNull();
                    assertThat(violations.count()).isGreaterThan(0);
                });
        }

        @Test
        @DisplayName("updateTotalExposure() should update gauge value")
        void testUpdateTotalExposureIntegration() {
            // Given: New exposure value
            BigDecimal newExposure = new BigDecimal("50000.00");

            // When: Update total exposure
            metricsService.updateTotalExposure(newExposure);

            // Then: Gauge should reflect new value
            await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Gauge exposureGauge = meterRegistry.find("trading.risk.total_exposure").gauge();
                    assertThat(exposureGauge).isNotNull();
                    assertThat(exposureGauge.value()).isEqualTo(newExposure.doubleValue());
                });
        }
    }

    @Nested
    @DisplayName("Financial & Performance Metrics Integration")
    class FinancialPerformanceMetricsTests {

        @Test
        @DisplayName("updateTotalPnL() and updateDailyPnL() should update financial gauges")
        void testFinancialMetricsIntegration() {
            // Given: PnL values
            BigDecimal totalPnL = new BigDecimal("15000.00");
            BigDecimal dailyPnL = new BigDecimal("2500.00");

            // When: Update PnL metrics
            metricsService.updateTotalPnL(totalPnL);
            metricsService.updateDailyPnL(dailyPnL);

            // Then: Gauges should reflect new values
            await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Gauge totalPnLGauge = meterRegistry.find("trading.pnl.total").gauge();
                    Gauge dailyPnLGauge = meterRegistry.find("trading.pnl.daily").gauge();

                    assertThat(totalPnLGauge).isNotNull();
                    assertThat(totalPnLGauge.value()).isEqualTo(totalPnL.doubleValue());

                    assertThat(dailyPnLGauge).isNotNull();
                    assertThat(dailyPnLGauge.value()).isEqualTo(dailyPnL.doubleValue());
                });
        }

        @Test
        @DisplayName("incrementActiveOrders() and decrementActiveOrders() should manage counter")
        void testPerformanceMetricsIntegration() {
            // Given: Initial active orders count
            Gauge activeOrdersGauge = meterRegistry.find("trading.orders.active").gauge();
            double initialCount = activeOrdersGauge.value();

            // When: Increment and then decrement
            metricsService.incrementActiveOrders();
            metricsService.incrementActiveOrders();
            metricsService.decrementActiveOrders();

            // Then: Count should be initial + 1
            await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(activeOrdersGauge.value()).isEqualTo(initialCount + 1);
                });
        }
    }

    @Nested
    @DisplayName("Regression Tests - No Functional Changes")
    class RegressionValidationTests {

        @Test
        @DisplayName("All metrics categories should be properly initialized")
        void testMetricsCategoriesRegressionValidation() {
            // Then: All metric categories should exist

            // Order Processing Metrics
            assertThat(meterRegistry.find("trading.orders.placed").counter()).isNotNull();
            assertThat(meterRegistry.find("trading.orders.executed").counter()).isNotNull();
            assertThat(meterRegistry.find("trading.orders.processing_time").timer()).isNotNull();

            // Risk Management Metrics
            assertThat(meterRegistry.find("trading.risk.check_time").timer()).isNotNull();
            assertThat(meterRegistry.find("trading.risk.violations").counter()).isNotNull();
            assertThat(meterRegistry.find("trading.risk.total_exposure").gauge()).isNotNull();

            // Financial Metrics
            assertThat(meterRegistry.find("trading.pnl.total").gauge()).isNotNull();
            assertThat(meterRegistry.find("trading.pnl.daily").gauge()).isNotNull();
            assertThat(meterRegistry.find("trading.fees.total").gauge()).isNotNull();

            // Performance Metrics
            assertThat(meterRegistry.find("trading.orders.active").gauge()).isNotNull();
            assertThat(meterRegistry.find("trading.positions.active").gauge()).isNotNull();
            assertThat(meterRegistry.find("trading.users.connected").gauge()).isNotNull();

            // Circuit Breaker Metrics
            assertThat(meterRegistry.find("trading.circuit_breaker.trips").counter()).isNotNull();
        }

        @Test
        @DisplayName("Aggregated metrics calculations should work correctly")
        void testAggregatedMetricsRegressionValidation() {
            // Given: Record some orders
            metricsService.recordOrderPlaced(TEST_BROKER, TEST_ORDER_VALUE);
            metricsService.recordOrderExecuted(TEST_BROKER, TEST_ORDER_VALUE);

            // When: Calculate aggregated metrics
            BigDecimal successRate = metricsService.getOrderSuccessRate();
            BigDecimal avgProcessingTime = metricsService.getAverageOrderProcessingTime();
            long totalActiveEntities = metricsService.getTotalActiveEntities();

            // Then: Calculations should work correctly
            assertThat(successRate).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(avgProcessingTime).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(totalActiveEntities).isGreaterThanOrEqualTo(0);
        }
    }
}
