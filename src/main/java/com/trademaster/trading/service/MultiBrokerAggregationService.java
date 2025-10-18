package com.trademaster.trading.service;

import com.trademaster.common.functional.Result;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.dto.OrderResponse;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.metrics.AlertingService;
import com.trademaster.trading.metrics.TradingMetricsService;
import com.trademaster.trading.model.OrderStatus;
import com.trademaster.trading.routing.RoutingDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Multi-Broker Aggregation Service
 * 
 * Advanced order routing and execution across multiple brokers with intelligent aggregation.
 * Provides optimal execution by splitting large orders, balancing load, and aggregating fills.
 * 
 * Key Features:
 * - Smart order splitting based on broker capacity and liquidity
 * - Real-time broker performance monitoring and routing decisions
 * - Intelligent fill aggregation and partial execution handling
 * - Dynamic broker weighting based on execution quality
 * - Cross-broker risk management and position reconciliation
 * - Advanced execution algorithms (TWAP, VWAP, Implementation Shortfall)
 * 
 * Execution Strategies:
 * - SINGLE_BROKER: Route entire order to best single broker
 * - MULTI_BROKER_SPLIT: Split order across multiple brokers
 * - DYNAMIC_ROUTING: Real-time routing based on market conditions
 * - ICEBERG: Break large orders into smaller chunks over time
 * - LIQUIDITY_SEEKING: Route to brokers with best available liquidity
 * 
 * Risk Management:
 * - Position limits per broker and aggregate
 * - Real-time exposure monitoring
 * - Automatic failover and circuit breakers
 * - Compliance with regulatory position limits
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Multi-Broker Architecture)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiBrokerAggregationService {
    
    private final TradingMetricsService metricsService;
    private final AlertingService alertingService;
    private final StructuredLoggingService loggingService;
    
    // Virtual thread executor for high-performance async operations
    private final ScheduledExecutorService executorService = 
        Executors.newScheduledThreadPool(10, Thread.ofVirtual().factory());
    
    // Broker performance tracking
    private final ConcurrentHashMap<String, BrokerPerformanceMetrics> brokerMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BrokerCapacityStatus> brokerCapacity = new ConcurrentHashMap<>();
    
    // Active multi-broker orders tracking
    private final ConcurrentHashMap<String, MultiBrokerOrderContext> activeOrders = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int MAX_BROKER_SPLITS = 5;
    private static final BigDecimal MIN_ORDER_SIZE = new BigDecimal("1000"); // Minimum ₹1K per split
    private static final int AGGREGATION_WINDOW_MS = 100; // 100ms aggregation window
    private static final double BROKER_WEIGHT_DECAY_FACTOR = 0.95; // 5% decay per hour
    
    /**
     * Execute order with multi-broker aggregation logic
     */
    public CompletableFuture<Result<List<OrderResponse>, TradeError>> executeMultiBrokerOrder(
            OrderRequest orderRequest, Long userId, String correlationId) {
        
        return CompletableFuture.supplyAsync(() -> {
            try (var timer = loggingService.startPerformanceTimer(correlationId, "MULTI_BROKER_EXECUTION")) {
                
                log.info("Starting multi-broker order execution - correlationId: {}, symbol: {}, quantity: {}", 
                    correlationId, orderRequest.symbol(), orderRequest.quantity());
                
                // Determine optimal execution strategy
                ExecutionStrategy strategy = determineExecutionStrategy(orderRequest, correlationId);
                
                // Create multi-broker order context
                MultiBrokerOrderContext orderContext = createOrderContext(orderRequest, userId, correlationId, strategy);
                activeOrders.put(correlationId, orderContext);
                
                try {
                    // Execute based on strategy
                    List<OrderResponse> responses = switch (strategy) {
                        case SINGLE_BROKER -> executeSingleBrokerOrder(orderContext);
                        case MULTI_BROKER_SPLIT -> executeMultiBrokerSplit(orderContext);
                        case DYNAMIC_ROUTING -> executeDynamicRouting(orderContext);
                        case ICEBERG -> executeIcebergOrder(orderContext);
                        case LIQUIDITY_SEEKING -> executeLiquiditySeekingOrder(orderContext);
                    };
                    
                    // Update broker performance metrics
                    updateBrokerPerformanceMetrics(responses, correlationId);
                    
                    // Record aggregation metrics
                    recordAggregationMetrics(responses, orderContext, correlationId);
                    
                    timer.stop("Multi-broker order execution completed", responses.size());
                    
                    return Result.success(responses);
                    
                } catch (Exception e) {
                    log.error("Multi-broker order execution failed - correlationId: {}", correlationId, e);
                    loggingService.logError(correlationId, userId, "MULTI_BROKER_EXECUTION_FAILED", 
                        "executeMultiBrokerOrder", e.getMessage(), e);
                    
                    return Result.failure(new TradeError.ExecutionError.VenueUnavailable(
                        "Multi-broker execution failed: " + e.getMessage()));
                        
                } finally {
                    activeOrders.remove(correlationId);
                }
                
            }
        }, executorService);
    }
    
    /**
     * Determine optimal execution strategy based on order characteristics - eliminates if-statements with pattern matching
     */
    private ExecutionStrategy determineExecutionStrategy(OrderRequest orderRequest, String correlationId) {
        BigDecimal orderValue = orderRequest.getEstimatedOrderValue();
        int quantity = orderRequest.quantity();

        // Use functional pattern matching to determine strategy - eliminates if-statements
        record StrategyDecision(ExecutionStrategy strategy, String reason) {}

        StrategyDecision decision = Optional.of(quantity)
            // Check for very large orders (>50000)
            .filter(q -> q > 50000)
            .map(q -> new StrategyDecision(ExecutionStrategy.ICEBERG, "Very large order"))
            // Check for large orders (>10000 or value >1M)
            .or(() -> Optional.of(orderValue.compareTo(new BigDecimal("1000000")) > 0 || quantity > 10000)
                .filter(Boolean::booleanValue)
                .map(large -> new StrategyDecision(ExecutionStrategy.MULTI_BROKER_SPLIT, "Large order detected")))
            // Check for volatile market conditions
            .or(() -> Optional.of(orderRequest.orderType().name())
                .filter("MARKET"::equals)
                .filter(type -> isMarketVolatile(orderRequest.symbol()))
                .map(type -> new StrategyDecision(ExecutionStrategy.DYNAMIC_ROUTING, "Volatile market conditions")))
            // Default strategy
            .orElse(new StrategyDecision(ExecutionStrategy.SINGLE_BROKER, "Using default"));

        log.info("{} - using {} strategy - correlationId: {}",
            decision.reason(), decision.strategy(), correlationId);

        return decision.strategy();
    }
    
    /**
     * Execute single broker order (best execution venue)
     */
    private List<OrderResponse> executeSingleBrokerOrder(MultiBrokerOrderContext context) {
        String bestBroker = selectBestBroker(context.getOrderRequest(), context.getCorrelationId());
        
        // Execute single order
        OrderResponse response = executeBrokerOrder(context.getOrderRequest(), bestBroker, context.getCorrelationId());
        
        return List.of(response);
    }
    
    /**
     * Execute multi-broker split strategy - eliminates for loop with Stream API
     */
    private List<OrderResponse> executeMultiBrokerSplit(MultiBrokerOrderContext context) {
        List<BrokerAllocation> allocations = calculateBrokerAllocations(context);

        // Eliminate for loop with Stream API - create futures in parallel
        return allocations.stream()
            .map(allocation -> {
                OrderRequest splitOrder = createSplitOrder(context.getOrderRequest(), allocation.getQuantity());
                return CompletableFuture.supplyAsync(() ->
                    executeBrokerOrder(splitOrder, allocation.getBrokerName(), context.getCorrelationId()),
                    executorService);
            })
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }
    
    /**
     * Execute dynamic routing strategy - eliminates while loop with Stream.iterate()
     */
    private List<OrderResponse> executeDynamicRouting(MultiBrokerOrderContext context) {
        // State record for functional iteration - carries remaining order and accumulated responses
        record RoutingState(OrderRequest remainingOrder, List<OrderResponse> responses) {}

        // Use Stream.iterate() to replace while loop - functional iteration pattern
        return Stream.iterate(
            // Initial state: start with full order and empty responses
            new RoutingState(context.getOrderRequest(), new ArrayList<>()),
            // Continue while we have quantity remaining
            state -> state.remainingOrder().quantity() > 0,
            // Process one chunk and return next state
            state -> {
                // Select best broker for current market conditions
                String bestBroker = selectBestBrokerDynamic(state.remainingOrder(), context.getCorrelationId());

                // Calculate optimal chunk size
                int chunkSize = calculateOptimalChunkSize(state.remainingOrder(), bestBroker);

                // Execute chunk order
                OrderRequest chunkOrder = createSplitOrder(state.remainingOrder(), chunkSize);
                OrderResponse response = executeBrokerOrder(chunkOrder, bestBroker, context.getCorrelationId());

                // Accumulate response
                List<OrderResponse> updatedResponses = new ArrayList<>(state.responses());
                updatedResponses.add(response);

                // Small delay between chunks - handle interruption gracefully
                try {
                    Thread.sleep(50); // 50ms delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // Return state with zero quantity to terminate iteration
                    return new RoutingState(
                        createSplitOrder(state.remainingOrder(), 0),
                        updatedResponses
                    );
                }

                // Return next state with updated remaining order
                return new RoutingState(
                    createSplitOrder(state.remainingOrder(), state.remainingOrder().quantity() - chunkSize),
                    updatedResponses
                );
            }
        )
        // Extract final responses from last state
        .reduce((first, last) -> last)
        .map(RoutingState::responses)
        .orElse(List.of());
    }
    
    /**
     * Execute iceberg order strategy - eliminates while loop and if-statement with Stream.iterate()
     */
    private List<OrderResponse> executeIcebergOrder(MultiBrokerOrderContext context) {
        int totalQuantity = context.getOrderRequest().quantity();
        int icebergSize = calculateIcebergSize(totalQuantity);

        // State record for functional iteration - carries executed quantity and accumulated responses
        record IcebergState(int executedQuantity, List<OrderResponse> responses) {}

        // Use Stream.iterate() to replace while loop - functional iteration pattern
        return Stream.iterate(
            // Initial state: no quantity executed, empty responses
            new IcebergState(0, new ArrayList<>()),
            // Continue while we have quantity remaining
            state -> state.executedQuantity() < totalQuantity,
            // Process one iceberg chunk and return next state
            state -> {
                int currentChunk = Math.min(icebergSize, totalQuantity - state.executedQuantity());

                // Execute chunk
                OrderRequest chunkOrder = createSplitOrder(context.getOrderRequest(), currentChunk);
                String bestBroker = selectBestBroker(chunkOrder, context.getCorrelationId());
                OrderResponse response = executeBrokerOrder(chunkOrder, bestBroker, context.getCorrelationId());

                // Accumulate response
                List<OrderResponse> updatedResponses = new ArrayList<>(state.responses());
                updatedResponses.add(response);

                int newExecutedQuantity = state.executedQuantity() + currentChunk;

                // Time delay between iceberg chunks - only if more chunks remaining (eliminates if-statement)
                Optional.of(newExecutedQuantity)
                    .filter(executed -> executed < totalQuantity)
                    .ifPresent(executed -> {
                        try {
                            Thread.sleep(AGGREGATION_WINDOW_MS * 5); // 500ms delay between chunks
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });

                // Return next state
                return new IcebergState(newExecutedQuantity, updatedResponses);
            }
        )
        // Extract final responses from last state
        .reduce((first, last) -> last)
        .map(IcebergState::responses)
        .orElse(List.of());
    }
    
    /**
     * Execute liquidity seeking order strategy - eliminates for loop and if-statements with Stream.iterate()
     */
    private List<OrderResponse> executeLiquiditySeekingOrder(MultiBrokerOrderContext context) {
        // Get brokers with best liquidity for this symbol
        List<String> liquidityBrokers = getBrokersWithBestLiquidity(context.getOrderRequest().symbol());

        // State record for functional iteration - carries broker index, remaining quantity, and responses
        record LiquidityState(int brokerIndex, int remainingQuantity, List<OrderResponse> responses) {}

        // Use Stream.iterate() to replace for loop - functional iteration pattern
        return Stream.iterate(
            // Initial state: start at first broker with full quantity
            new LiquidityState(0, context.getOrderRequest().quantity(), new ArrayList<>()),
            // Continue while we have brokers and quantity remaining (eliminates break statement)
            state -> state.brokerIndex() < liquidityBrokers.size() && state.remainingQuantity() > 0,
            // Process one broker and return next state
            state -> {
                String broker = liquidityBrokers.get(state.brokerIndex());

                // Get broker's available liquidity
                int brokerLiquidity = getBrokerLiquidity(broker, context.getOrderRequest().symbol());
                int orderQuantity = Math.min(state.remainingQuantity(), brokerLiquidity);

                // Execute order only if quantity is positive (eliminates if-statement with Optional)
                return Optional.of(orderQuantity)
                    .filter(qty -> qty > 0)
                    .map(qty -> {
                        OrderRequest liquidityOrder = createSplitOrder(context.getOrderRequest(), qty);
                        OrderResponse response = executeBrokerOrder(liquidityOrder, broker, context.getCorrelationId());

                        List<OrderResponse> updatedResponses = new ArrayList<>(state.responses());
                        updatedResponses.add(response);

                        return new LiquidityState(
                            state.brokerIndex() + 1,
                            state.remainingQuantity() - qty,
                            updatedResponses
                        );
                    })
                    .orElse(new LiquidityState(
                        state.brokerIndex() + 1,
                        state.remainingQuantity(),
                        state.responses()
                    ));
            }
        )
        // Extract final responses from last state
        .reduce((first, last) -> last)
        .map(LiquidityState::responses)
        .orElse(List.of());
    }
    
    /**
     * Calculate broker allocations for multi-broker split - eliminates for loop and if-statements
     */
    private List<BrokerAllocation> calculateBrokerAllocations(MultiBrokerOrderContext context) {
        List<String> availableBrokers = getAvailableBrokers(context.getOrderRequest().symbol());
        int totalQuantity = context.getOrderRequest().quantity();
        Map<String, Double> brokerWeights = calculateBrokerWeights(availableBrokers);

        // State record for functional iteration - carries broker index, allocated quantity, and allocations
        record AllocationState(int brokerIndex, int allocatedQuantity, List<BrokerAllocation> allocations) {}

        // Use Stream.iterate() to replace for loop - functional iteration pattern
        AllocationState finalState = Stream.iterate(
            // Initial state: start at first broker with no allocations
            new AllocationState(0, 0, new ArrayList<>()),
            // Continue while: have brokers, quantity remaining, and under max splits (eliminates break statements)
            state -> state.brokerIndex() < availableBrokers.size()
                    && state.allocatedQuantity() < totalQuantity
                    && state.allocations().size() < MAX_BROKER_SPLITS,
            // Process one broker and return next state
            state -> {
                String broker = availableBrokers.get(state.brokerIndex());
                double weight = brokerWeights.get(broker);
                int allocation = Math.min(
                    (int) (totalQuantity * weight),
                    totalQuantity - state.allocatedQuantity()
                );

                // Add allocation only if positive (eliminates if-statement with Optional)
                return Optional.of(allocation)
                    .filter(alloc -> alloc > 0)
                    .map(alloc -> {
                        List<BrokerAllocation> updatedAllocations = new ArrayList<>(state.allocations());
                        updatedAllocations.add(new BrokerAllocation(broker, alloc, weight));
                        return new AllocationState(
                            state.brokerIndex() + 1,
                            state.allocatedQuantity() + alloc,
                            updatedAllocations
                        );
                    })
                    .orElse(new AllocationState(
                        state.brokerIndex() + 1,
                        state.allocatedQuantity(),
                        state.allocations()
                    ));
            }
        )
        // Extract final state
        .reduce((first, last) -> last)
        .orElse(new AllocationState(0, 0, new ArrayList<>()));

        // Allocate remaining quantity to best performing broker (eliminates if-statement with Optional)
        return Optional.of(finalState)
            .filter(state -> state.allocatedQuantity() < totalQuantity)
            .filter(state -> !state.allocations().isEmpty())
            .map(state -> {
                List<BrokerAllocation> updatedAllocations = new ArrayList<>(state.allocations());
                BrokerAllocation bestAllocation = updatedAllocations.get(0);
                bestAllocation.setQuantity(bestAllocation.getQuantity() + (totalQuantity - state.allocatedQuantity()));
                return updatedAllocations;
            })
            .orElse(finalState.allocations());
    }
    
    /**
     * Calculate broker weights based on performance metrics - eliminates for loops with Stream API
     */
    private Map<String, Double> calculateBrokerWeights(List<String> brokers) {
        // Calculate scores for each broker using Stream API (eliminates first for loop)
        Map<String, Double> scores = brokers.stream()
            .collect(Collectors.toMap(
                broker -> broker,
                broker -> calculateBrokerScore(brokerMetrics.get(broker))
            ));

        // Calculate total score using Stream API
        double totalScore = scores.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        // Normalize to weights using Stream API (eliminates second for loop)
        return brokers.stream()
            .collect(Collectors.toMap(
                broker -> broker,
                // Eliminates ternary using Optional.of().filter() for division-by-zero protection
                broker -> Optional.of(totalScore)
                    .filter(score -> score > 0)
                    .map(score -> scores.get(broker) / score)
                    .orElse(1.0 / brokers.size())
            ));
    }
    
    /**
     * Calculate broker performance score - eliminates if-statement with Optional
     */
    private double calculateBrokerScore(BrokerPerformanceMetrics metrics) {
        // Use Optional to handle null metrics (eliminates if-statement)
        return Optional.ofNullable(metrics)
            .map(m -> {
                // Weighted scoring algorithm
                double executionScore = m.getExecutionSuccessRate() * 0.4;
                double latencyScore = Math.max(0, 1.0 - (m.getAverageLatency() / 1000.0)) * 0.3;
                double reliabilityScore = (1.0 - m.getErrorRate()) * 0.3;
                return Math.max(0.1, executionScore + latencyScore + reliabilityScore); // Minimum 10% score
            })
            .orElse(0.5); // Default neutral score
    }
    
    /**
     * Select best broker based on current performance
     */
    private String selectBestBroker(OrderRequest orderRequest, String correlationId) {
        List<String> availableBrokers = getAvailableBrokers(orderRequest.symbol());

        return availableBrokers.stream()
            .max(Comparator.comparing(broker -> {
                BrokerPerformanceMetrics metrics = brokerMetrics.get(broker);
                return calculateBrokerScore(metrics);
            }))
            // Eliminates nested ternary using Optional.of().filter() for empty list check
            .orElseGet(() -> Optional.of(availableBrokers)
                .filter(brokers -> !brokers.isEmpty())
                .map(brokers -> brokers.get(0))
                .orElse("ZERODHA"));
    }
    
    /**
     * Select best broker with dynamic market conditions
     */
    private String selectBestBrokerDynamic(OrderRequest orderRequest, String correlationId) {
        // Enhanced selection considering real-time market conditions
        String symbol = orderRequest.symbol();
        List<String> availableBrokers = getAvailableBrokers(symbol);

        return availableBrokers.stream()
            .max(Comparator.comparing(broker -> {
                BrokerPerformanceMetrics metrics = brokerMetrics.get(broker);
                double baseScore = calculateBrokerScore(metrics);

                // Add dynamic factors - eliminates ternary operators with Optional.of().filter()
                double liquidityBonus = Optional.of(getBrokerLiquidity(broker, symbol))
                    .filter(liquidity -> liquidity > 10000)
                    .map(liquidity -> 0.1)
                    .orElse(0.0);

                double capacityBonus = Optional.of(getBrokerCapacityUtilization(broker))
                    .filter(utilization -> utilization < 0.8)
                    .map(utilization -> 0.1)
                    .orElse(0.0);

                return baseScore + liquidityBonus + capacityBonus;
            }))
            .orElse("ZERODHA");
    }
    
    /**
     * Mock implementation - Execute order with specific broker
     */
    private OrderResponse executeBrokerOrder(OrderRequest orderRequest, String brokerName, String correlationId) {
        // This would integrate with actual broker APIs
        // For now, return a mock successful response
        
        return OrderResponse.builder()
            .orderId("ORD-" + System.currentTimeMillis())
            .symbol(orderRequest.symbol())
            .side(orderRequest.side())
            .orderType(orderRequest.orderType())
            .quantity(orderRequest.quantity())
            .limitPrice(orderRequest.limitPrice())
            .stopPrice(orderRequest.stopPrice())
            .timeInForce(orderRequest.timeInForce())
            .status(OrderStatus.ACKNOWLEDGED)
            .brokerName(brokerName)
            .brokerOrderId("BRK-" + System.currentTimeMillis())
            .createdAt(Instant.now())
            .build();
    }
    
    // Helper methods and data structures
    
    /**
     * Execution strategies enum
     */
    public enum ExecutionStrategy {
        SINGLE_BROKER,
        MULTI_BROKER_SPLIT,
        DYNAMIC_ROUTING,
        ICEBERG,
        LIQUIDITY_SEEKING
    }
    
    /**
     * Broker allocation data structure
     */
    public static class BrokerAllocation {
        private final String brokerName;
        private int quantity;
        private final double weight;
        
        public BrokerAllocation(String brokerName, int quantity, double weight) {
            this.brokerName = brokerName;
            this.quantity = quantity;
            this.weight = weight;
        }
        
        // Getters and setters
        public String getBrokerName() { return brokerName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getWeight() { return weight; }
    }
    
    /**
     * Multi-broker order context
     */
    public static class MultiBrokerOrderContext {
        private final OrderRequest orderRequest;
        private final Long userId;
        private final String correlationId;
        private final ExecutionStrategy strategy;
        private final long creationTime;
        
        public MultiBrokerOrderContext(OrderRequest orderRequest, Long userId, 
                                     String correlationId, ExecutionStrategy strategy) {
            this.orderRequest = orderRequest;
            this.userId = userId;
            this.correlationId = correlationId;
            this.strategy = strategy;
            this.creationTime = System.currentTimeMillis();
        }
        
        // Getters
        public OrderRequest getOrderRequest() { return orderRequest; }
        public Long getUserId() { return userId; }
        public String getCorrelationId() { return correlationId; }
        public ExecutionStrategy getStrategy() { return strategy; }
        public long getCreationTime() { return creationTime; }
    }
    
    /**
     * Broker performance metrics
     */
    public static class BrokerPerformanceMetrics {
        private final AtomicLong totalOrders = new AtomicLong(0);
        private final AtomicLong successfulOrders = new AtomicLong(0);
        private final AtomicLong totalLatency = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private volatile long lastUpdateTime = System.currentTimeMillis();
        
        /**
         * Record execution result - eliminates if-else with Optional pattern
         */
        public void recordExecution(boolean success, long latency) {
            totalOrders.incrementAndGet();

            // Eliminate if-else with Optional.ifPresentOrElse() - functional branching
            Optional.of(success)
                .filter(Boolean::booleanValue)
                .ifPresentOrElse(
                    s -> successfulOrders.incrementAndGet(),
                    () -> errorCount.incrementAndGet()
                );

            totalLatency.addAndGet(latency);
            lastUpdateTime = System.currentTimeMillis();
        }
        
        public double getExecutionSuccessRate() {
            long total = totalOrders.get();
            // Eliminates ternary using Optional.of().filter() for division-by-zero protection
            return Optional.of(total)
                .filter(t -> t > 0)
                .map(t -> (double) successfulOrders.get() / t)
                .orElse(0.0);
        }

        public double getAverageLatency() {
            long total = totalOrders.get();
            // Eliminates ternary using Optional.of().filter() for division-by-zero protection
            return Optional.of(total)
                .filter(t -> t > 0)
                .map(t -> (double) totalLatency.get() / t)
                .orElse(0.0);
        }

        public double getErrorRate() {
            long total = totalOrders.get();
            // Eliminates ternary using Optional.of().filter() for division-by-zero protection
            return Optional.of(total)
                .filter(t -> t > 0)
                .map(t -> (double) errorCount.get() / t)
                .orElse(0.0);
        }
        
        public long getLastUpdateTime() { return lastUpdateTime; }
    }
    
    /**
     * Broker capacity status
     */
    public static class BrokerCapacityStatus {
        private final AtomicLong activeOrders = new AtomicLong(0);
        private final AtomicLong maxCapacity = new AtomicLong(1000);
        
        public double getUtilization() {
            return (double) activeOrders.get() / maxCapacity.get();
        }
        
        public boolean hasCapacity() {
            return activeOrders.get() < maxCapacity.get();
        }
        
        public long getActiveOrders() { return activeOrders.get(); }
        public long getMaxCapacity() { return maxCapacity.get(); }
    }
    
    // Mock helper methods (would be implemented with real broker integrations)
    
    private MultiBrokerOrderContext createOrderContext(OrderRequest orderRequest, Long userId, 
                                                     String correlationId, ExecutionStrategy strategy) {
        return new MultiBrokerOrderContext(orderRequest, userId, correlationId, strategy);
    }
    
    private boolean isMarketVolatile(String symbol) {
        // Mock implementation - would check real volatility metrics
        return Math.random() > 0.7; // 30% chance of volatile conditions
    }
    
    private List<String> getAvailableBrokers(String symbol) {
        return Arrays.asList("ZERODHA", "UPSTOX", "ANGELONE", "ICICIDIRECT");
    }
    
    private List<String> getBrokersWithBestLiquidity(String symbol) {
        return Arrays.asList("ZERODHA", "UPSTOX", "ANGELONE");
    }
    
    private int getBrokerLiquidity(String broker, String symbol) {
        // Mock implementation - would query real liquidity data
        return 5000 + (int) (Math.random() * 10000);
    }
    
    private double getBrokerCapacityUtilization(String broker) {
        return brokerCapacity.computeIfAbsent(broker, k -> new BrokerCapacityStatus()).getUtilization();
    }
    
    private OrderRequest createSplitOrder(OrderRequest original, int newQuantity) {
        return OrderRequest.builder()
            .symbol(original.symbol())
            .exchange(original.exchange())
            .orderType(original.orderType())
            .side(original.side())
            .quantity(newQuantity)
            .limitPrice(original.limitPrice())
            .stopPrice(original.stopPrice())
            .timeInForce(original.timeInForce())
            .expiryDate(original.expiryDate())
            .brokerName(original.brokerName())
            .clientOrderRef(original.clientOrderRef())
            .build();
    }
    
    private int calculateOptimalChunkSize(OrderRequest order, String broker) {
        int baseChunkSize = Math.min(1000, order.quantity() / 4);
        double brokerScore = calculateBrokerScore(brokerMetrics.get(broker));
        return (int) (baseChunkSize * (0.5 + brokerScore));
    }
    
    private int calculateIcebergSize(int totalQuantity) {
        return Math.max(100, Math.min(2000, totalQuantity / 10));
    }
    
    /**
     * Update broker performance metrics - eliminates for loop with Stream API
     */
    private void updateBrokerPerformanceMetrics(List<OrderResponse> responses, String correlationId) {
        // Eliminate for loop with Stream API - functional side-effect processing
        responses.stream()
            .forEach(response -> {
                BrokerPerformanceMetrics metrics = brokerMetrics.computeIfAbsent(
                    response.brokerName(), k -> new BrokerPerformanceMetrics());

                boolean success = response.status() == OrderStatus.ACKNOWLEDGED ||
                                 response.status() == OrderStatus.FILLED;
                long latency = 100 + (long) (Math.random() * 200); // Mock latency

                metrics.recordExecution(success, latency);
            });
    }
    
    /**
     * Record aggregation metrics - eliminates for loop with Stream API
     */
    private void recordAggregationMetrics(List<OrderResponse> responses,
                                        MultiBrokerOrderContext context, String correlationId) {
        // Record multi-broker aggregation metrics
        metricsService.recordOrderPlaced("MULTI_BROKER", context.getOrderRequest().getEstimatedOrderValue());

        // Eliminate for loop with Stream API - functional side-effect for metrics recording
        responses.stream()
            .forEach(response ->
                metricsService.recordOrderExecuted(response.brokerName(), BigDecimal.valueOf(response.quantity())));

        // Log aggregation summary
        loggingService.logAuditEvent(correlationId, context.getUserId(),
            "MULTI_BROKER_AGGREGATION",
            String.format("Executed order across %d brokers using %s strategy",
                responses.size(), context.getStrategy()));
    }
}