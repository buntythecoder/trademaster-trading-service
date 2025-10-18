package com.trademaster.trading.routing.impl;

import com.trademaster.common.functional.Result;
import com.trademaster.trading.dto.integration.BrokerConnection;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.integration.client.BrokerAuthServiceClient;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.routing.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Functional Order Router
 *
 * MANDATORY: Rule #3 - Functional Programming (no if-else, no loops)
 * MANDATORY: Rule #5 - Cognitive Complexity Control (max 7 per method)
 * MANDATORY: Rule #9 - Immutability & Records Usage
 * MANDATORY: Rule #11 - Error Handling Patterns (Result monad)
 * MANDATORY: Rule #13 - Stream API Mastery
 * MANDATORY: Rule #14 - Pattern Matching Excellence
 * MANDATORY: Rule #15 - Structured Logging & Monitoring
 * MANDATORY: Rule #25 - Circuit Breaker Implementation
 *
 * Intelligent order routing with broker selection based on multiple factors:
 * - Broker connectivity status
 * - Order size and characteristics
 * - Exchange compatibility
 * - Fee structure optimization
 * - Execution speed requirements
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionalOrderRouter implements OrderRouter {

    private final BrokerAuthServiceClient brokerAuthClient;
    private final MeterRegistry meterRegistry;

    // Configuration constants (Rule #16: Dynamic Configuration)
    @Value("${trading.routing.primary-broker:ZERODHA}")
    private String primaryBroker;

    @Value("${trading.routing.fallback-broker:UPSTOX}")
    private String fallbackBroker;

    @Value("${trading.routing.large-order-threshold:10000}")
    private Integer largeOrderThreshold;

    @Value("${trading.routing.max-single-order-quantity:100000}")
    private Integer maxSingleOrderQuantity;

    // Metrics constants (Rule #15)
    private static final String ROUTING_METRIC = "trading.routing";
    private static final String ROUTING_DECISION_METRIC = "trading.routing.decisions";

    /**
     * Route order to appropriate broker and execution venue
     * Rule #11: Functional error handling with Result monad
     * Rule #14: Pattern matching throughout
     */
    @Override
    public RoutingDecision routeOrder(Order order) {
        long startTime = System.nanoTime();

        log.info("Routing order {} for symbol {} exchange {} - quantity: {}",
                order.getOrderId(), order.getSymbol(), order.getExchange(), order.getQuantity());

        // Functional routing pipeline (Rule #3, #11)
        RoutingDecision decision = routeOrderFunctionally(order);

        // Record metrics (Rule #15)
        recordRoutingMetrics(decision, System.nanoTime() - startTime);

        return decision;
    }

    /**
     * Get router priority
     */
    @Override
    public int getPriority() {
        return 10;  // Default priority
    }

    /**
     * Check if router can handle order
     * Rule #14: Pattern matching with switch expression
     */
    @Override
    public boolean canHandle(Order order) {
        return switch (order.getExchange()) {
            case "NSE", "BSE", "MCX" -> true;
            default -> false;
        };
    }

    /**
     * Get router name
     */
    @Override
    public String getRouterName() {
        return "FunctionalOrderRouter";
    }

    /**
     * Functional routing pipeline
     * Rule #3: Zero if-else, functional composition
     * Rule #13: Stream API for functional processing
     */
    private RoutingDecision routeOrderFunctionally(Order order) {
        // Compose routing decision using functional pipeline
        return validateOrderSize(order)
            .flatMap(this::selectBroker)
            .flatMap(broker -> selectExecutionStrategy(order, broker))
            .flatMap(decision -> validateBrokerConnection(decision))
            .map(decision -> enrichDecisionMetadata(decision, order))
            .fold(
                decision -> decision,
                error -> createRejectionDecision(error)
            );
    }

    /**
     * Validate order size
     * Rule #14: Pattern matching with Optional
     */
    private Result<Order, RoutingError> validateOrderSize(Order order) {
        return Optional.of(order.getQuantity())
            .filter(qty -> qty <= maxSingleOrderQuantity)
            .map(qty -> Result.<Order, RoutingError>success(order))
            .orElseGet(() -> Result.failure(
                RoutingError.orderTooLarge(order.getQuantity(), maxSingleOrderQuantity)));
    }

    /**
     * Select broker based on order characteristics
     * Rule #14: Pattern matching with switch expressions
     * Rule #13: Stream API for broker filtering
     */
    private Result<BrokerSelection, RoutingError> selectBroker(Order order) {
        // Get available brokers for exchange
        List<String> availableBrokers = getAvailableBrokersForExchange(order.getExchange());

        return Optional.of(availableBrokers)
            .filter(brokers -> !brokers.isEmpty())
            .map(brokers -> selectOptimalBroker(order, brokers))
            .map(Result::<BrokerSelection, RoutingError>success)
            .orElseGet(() -> Result.failure(
                RoutingError.noBrokerAvailable(order.getExchange(), "No configured brokers")));
    }

    /**
     * Select optimal broker based on order characteristics
     * Rule #14: Pattern matching for order type
     * Rule #13: Stream API for scoring
     */
    private BrokerSelection selectOptimalBroker(Order order, List<String> brokers) {
        return brokers.stream()
            .map(broker -> new BrokerScore(
                broker,
                calculateBrokerScore(broker, order)
            ))
            .max(Comparator.comparingDouble(BrokerScore::score))
            .map(scored -> new BrokerSelection(
                scored.brokerName(),
                calculateBrokerFees(scored.brokerName(), order),
                scored.score()
            ))
            .orElseGet(() -> new BrokerSelection(primaryBroker, BigDecimal.ZERO, 0.5));
    }

    /**
     * Calculate broker score based on order characteristics
     * Pattern 2: Layered Extraction - score composition
     * Rule #14: Pattern matching for scoring factors
     * Rule #5: 9 lines, complexity ≤7
     */
    private double calculateBrokerScore(String broker, Order order) {
        // Eliminates ternary using Optional.of().filter() for base score selection
        double baseScore = Optional.of(broker)
            .filter(primaryBroker::equals)
            .map(b -> 1.0)
            .orElse(0.8);

        // Compose final score from individual scoring factors
        return baseScore
            * calculateSizeScore(order)
            * calculateTypeScore(order)
            * calculateExchangeScore(order);
    }

    /**
     * Calculate score based on order size
     * Pattern 2: Score calculation extraction
     * Rule #14: Pattern matching for size scoring
     * Rule #5: 6 lines, complexity ≤7
     */
    private double calculateSizeScore(Order order) {
        return switch (classifyOrderSize(order.getQuantity())) {
            case SMALL -> 1.0;
            case MEDIUM -> 0.9;
            case LARGE -> 0.7;
        };
    }

    /**
     * Calculate score based on order type
     * Pattern 2: Score calculation extraction
     * Rule #14: Pattern matching for type scoring
     * Rule #5: 6 lines, complexity ≤7
     */
    private double calculateTypeScore(Order order) {
        return switch (order.getOrderType()) {
            case MARKET -> 1.0;
            case LIMIT -> 0.95;
            case STOP_LOSS, STOP_LIMIT -> 0.9;
        };
    }

    /**
     * Calculate score based on exchange
     * Pattern 2: Score calculation extraction
     * Rule #14: Pattern matching for exchange scoring
     * Rule #5: 7 lines, complexity ≤7
     */
    private double calculateExchangeScore(Order order) {
        return switch (order.getExchange()) {
            case "NSE" -> 1.0;
            case "BSE" -> 0.95;
            case "MCX" -> 0.9;
            default -> 0.5;
        };
    }

    /**
     * Classify order size
     * Rule #14: Pattern matching for size classification
     */
    private OrderSize classifyOrderSize(Integer quantity) {
        return switch (Integer.compare(quantity, largeOrderThreshold)) {
            case 1 -> OrderSize.LARGE;  // quantity > threshold
            case 0 -> OrderSize.MEDIUM;  // quantity == threshold
            case -1 -> switch (Integer.compare(quantity, largeOrderThreshold / 10)) {
                case 1, 0 -> OrderSize.MEDIUM;  // quantity >= threshold/10
                case -1 -> OrderSize.SMALL;  // quantity < threshold/10
                default -> OrderSize.SMALL;
            };
            default -> OrderSize.MEDIUM;
        };
    }

    /**
     * Calculate broker fees
     * Rule #14: Pattern matching for fee calculation
     */
    private BigDecimal calculateBrokerFees(String broker, Order order) {
        BigDecimal orderValue = order.getOrderValue();

        return switch (broker) {
            case "ZERODHA" -> orderValue.multiply(BigDecimal.valueOf(0.0003));  // 0.03%
            case "UPSTOX" -> orderValue.multiply(BigDecimal.valueOf(0.0002));   // 0.02%
            case "ANGEL_ONE" -> orderValue.multiply(BigDecimal.valueOf(0.0025)); // 0.025%
            default -> orderValue.multiply(BigDecimal.valueOf(0.0005));  // 0.05% default
        };
    }

    /**
     * Select execution strategy based on order characteristics
     * Pattern 2: Layered Extraction - strategy selection and decision building
     * Rule #14: Pattern matching for strategy selection
     * Rule #5: 10 lines, complexity ≤7
     */
    private Result<RoutingDecision, RoutingError> selectExecutionStrategy(
            Order order,
            BrokerSelection broker) {

        ExecutionStrategy strategy = determineExecutionStrategy(order);
        String venue = determineExecutionVenue(order, strategy);

        // Build routing decision with determined strategy and venue
        return Result.success(buildRoutingDecision(order, broker, strategy, venue));
    }

    /**
     * Build routing decision with all required parameters
     * Pattern 2: Decision builder extraction
     * Rule #9: Builder pattern with immutable records
     * Rule #5: 14 lines, complexity ≤7
     */
    private RoutingDecision buildRoutingDecision(
            Order order,
            BrokerSelection broker,
            ExecutionStrategy strategy,
            String venue) {

        return RoutingDecision.builder()
            .brokerName(broker.brokerName())
            .venue(venue)
            .strategy(strategy)
            .immediateExecution(strategy.isImmediate())
            .estimatedExecutionTime(Instant.now())
            .confidence(broker.confidenceScore())
            .reason(formatRoutingReason(order, broker, strategy))
            .routerName(getRouterName())
            .build();
    }

    /**
     * Determine execution strategy
     * Rule #14: Pattern matching with nested switches
     */
    private ExecutionStrategy determineExecutionStrategy(Order order) {
        return switch (order.getOrderType()) {
            case MARKET -> ExecutionStrategy.IMMEDIATE;
            case LIMIT -> switch (classifyOrderSize(order.getQuantity())) {
                case SMALL, MEDIUM -> ExecutionStrategy.IMMEDIATE;
                case LARGE -> ExecutionStrategy.SLICED;
            };
            case STOP_LOSS, STOP_LIMIT -> ExecutionStrategy.SCHEDULED;
        };
    }

    /**
     * Determine execution venue
     * Rule #14: Pattern matching for venue selection
     */
    private String determineExecutionVenue(Order order, ExecutionStrategy strategy) {
        return switch (strategy) {
            case DARK_POOL -> "DARK_POOL";
            case VWAP, TWAP, ICEBERG, SLICED -> "ALGORITHMIC";
            case SMART -> switch (order.getExchange()) {
                case "NSE" -> "NSE_SMART";
                case "BSE" -> "BSE_SMART";
                default -> order.getExchange();
            };
            default -> order.getExchange();
        };
    }

    /**
     * Format routing reason
     * Rule #13: Stream API for string composition
     */
    private String formatRoutingReason(
            Order order,
            BrokerSelection broker,
            ExecutionStrategy strategy) {

        return String.format(
            "Routed to %s via %s - Size: %s, Type: %s, Confidence: %.2f",
            broker.brokerName(),
            strategy.getDescription(),
            classifyOrderSize(order.getQuantity()),
            order.getOrderType(),
            broker.confidenceScore()
        );
    }

    /**
     * Validate broker connection
     * Pattern 2: Layered Extraction - connectivity validation
     * Rule #25: Circuit breaker via BrokerAuthServiceClient
     * Rule #11: Result monad error handling
     * Rule #5: 15 lines, complexity ≤7
     */
    private Result<RoutingDecision, RoutingError> validateBrokerConnection(
            RoutingDecision decision) {

        // Transform error type and get connection result
        Result<BrokerConnection, RoutingError> connectionResult = brokerAuthClient
            .getBrokerConnection(1L, decision.getBrokerName())
            .mapError(serviceError -> RoutingError.brokerConnectivity(decision.getBrokerName()));

        // Validate connection usability - eliminates ternary using Optional.of().filter()
        Result<RoutingDecision, RoutingError> validatedResult = connectionResult
            .flatMap(connection -> Optional.of(connection.isUsable())
                .filter(Boolean::booleanValue)
                .map(usable -> Result.<RoutingDecision, RoutingError>success(decision))
                .orElseGet(() -> Result.failure(RoutingError.brokerConnectivity(decision.getBrokerName()))));

        // Recover with fallback on error
        return validatedResult.recover(error -> Result.success(createFallbackDecision(decision)));
    }

    /**
     * Create fallback routing decision
     * Pattern 2: Fallback decision extraction
     * Rule #5: 12 lines, complexity ≤7
     */
    private RoutingDecision createFallbackDecision(RoutingDecision originalDecision) {
        log.warn("Broker {} unavailable, using fallback: {}",
            originalDecision.getBrokerName(), fallbackBroker);

        return RoutingDecision.builder()
            .brokerName(fallbackBroker)
            .venue(originalDecision.getVenue())
            .strategy(originalDecision.getStrategy())
            .immediateExecution(originalDecision.isImmediateExecution())
            .estimatedExecutionTime(originalDecision.getEstimatedExecutionTime())
            .confidence(0.7)  // Lower confidence for fallback
            .reason("Fallback broker due to connectivity issue")
            .routerName(getRouterName())
            .build();
    }

    /**
     * Enrich decision with metadata
     * Rule #14: Pattern matching for enrichment
     */
    private RoutingDecision enrichDecisionMetadata(RoutingDecision decision, Order order) {
        decision.setProcessingTimeMs(System.currentTimeMillis() - order.getCreatedAt().toEpochMilli());
        return decision;
    }

    /**
     * Create rejection decision from error
     * Rule #14: Pattern matching with switch expression
     */
    private RoutingDecision createRejectionDecision(RoutingError error) {
        String reason = switch (error) {
            case RoutingError.NoBrokerAvailableError e -> e.message();
            case RoutingError.UnsupportedExchangeError e -> e.message();
            case RoutingError.OrderTooLargeError e -> e.message();
            case RoutingError.BrokerConnectivityError e -> e.message();
            case RoutingError.ConfigurationError e -> e.message();
        };

        log.error("Order routing rejected: {} - {}", error.code(), reason);

        return RoutingDecision.reject(reason, getRouterName());
    }

    /**
     * Get available brokers for exchange
     * Rule #14: Pattern matching for broker mapping
     */
    private List<String> getAvailableBrokersForExchange(String exchange) {
        return switch (exchange) {
            case "NSE", "BSE" -> List.of("ZERODHA", "UPSTOX", "ANGEL_ONE");
            case "MCX" -> List.of("ZERODHA", "ANGEL_ONE");
            default -> List.of();
        };
    }

    /**
     * Record routing metrics
     * Pattern 2: Layered Extraction - metrics recording
     * Rule #15: Structured logging and monitoring
     * Rule #5: 7 lines, complexity ≤7
     */
    private void recordRoutingMetrics(RoutingDecision decision, long durationNanos) {
        // Record timing metrics
        createRoutingTimer(decision)
            .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        // Record decision counter
        createRoutingCounter(decision).increment();
    }

    /**
     * Create routing timer with consistent tags
     * Pattern 2: Timer builder extraction
     * Rule #15: Structured metrics with tags
     * Rule #5: 9 lines, complexity ≤7
     */
    private Timer createRoutingTimer(RoutingDecision decision) {
        return Timer.builder(ROUTING_METRIC)
            .tag("router", getRouterName())
            // Eliminates ternary using Optional.ofNullable().orElse()
            .tag("broker", Optional.ofNullable(decision.getBrokerName()).orElse("NONE"))
            .tag("strategy", Optional.ofNullable(decision.getStrategy())
                .map(ExecutionStrategy::name)
                .orElse("UNKNOWN"))
            .description("Order routing processing time")
            .register(meterRegistry);
    }

    /**
     * Create routing counter with consistent tags
     * Pattern 2: Counter builder extraction
     * Rule #15: Structured metrics with tags
     * Rule #5: 8 lines, complexity ≤7
     */
    private io.micrometer.core.instrument.Counter createRoutingCounter(RoutingDecision decision) {
        return meterRegistry.counter(ROUTING_DECISION_METRIC,
            "router", getRouterName(),
            // Eliminates ternary using Optional.ofNullable().orElse()
            "broker", Optional.ofNullable(decision.getBrokerName()).orElse("NONE"),
            "strategy", Optional.ofNullable(decision.getStrategy())
                .map(ExecutionStrategy::name)
                .orElse("UNKNOWN"),
            "immediate", String.valueOf(decision.isImmediateExecution())
        );
    }

    /**
     * Order size classification
     * Rule #9: Immutable enum
     */
    private enum OrderSize {
        SMALL,
        MEDIUM,
        LARGE
    }

    /**
     * Broker score record
     * Rule #9: Immutability with Records
     */
    private record BrokerScore(
        String brokerName,
        double score
    ) {}

    /**
     * Broker selection record
     * Rule #9: Immutability with Records
     */
    private record BrokerSelection(
        String brokerName,
        BigDecimal estimatedFees,
        double confidenceScore
    ) {}
}
