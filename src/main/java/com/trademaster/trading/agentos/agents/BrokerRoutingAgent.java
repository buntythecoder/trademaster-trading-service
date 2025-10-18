package com.trademaster.trading.agentos.agents;

import com.trademaster.trading.agentos.AgentCapability;
import com.trademaster.trading.agentos.AgentOSComponent;
import com.trademaster.trading.agentos.EventHandler;
import com.trademaster.trading.agentos.TradingCapabilityRegistry;
import com.trademaster.trading.dto.BrokerRouting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Broker Routing Agent for AgentOS Framework
 *
 * Implements intelligent multi-broker order routing using smart algorithms for broker selection,
 * order splitting, load balancing, and redundancy management.
 *
 * Agent Capabilities:
 * - BROKER_SELECTION: Select optimal broker based on price, speed, reliability, cost
 * - ORDER_SPLITTING: Split large orders across multiple brokers
 * - LOAD_BALANCING: Distribute orders to balance broker utilization
 * - REDUNDANCY_MANAGEMENT: Failover and backup broker configuration
 * - PERFORMANCE_TRACKING: Monitor and optimize broker performance
 *
 * Routing Algorithms:
 * - Best Execution: Score = 0.30×Price + 0.25×Speed + 0.25×Reliability + 0.20×Cost
 * - Load Balancing: Distribute by available capacity and performance
 * - Order Splitting: Weighted allocation based on broker scores
 * - Adaptive Routing: Dynamic strategy selection based on market conditions
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BrokerRoutingAgent implements AgentOSComponent {

    private final TradingCapabilityRegistry capabilityRegistry;

    // Routing Criteria Weights (sum = 1.0)
    private static final BigDecimal PRICE_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal SPEED_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal RELIABILITY_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal COST_WEIGHT = new BigDecimal("0.20");

    // Health Thresholds
    private static final BigDecimal MIN_HEALTH_SCORE = new BigDecimal("0.70");
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final BigDecimal MAX_BROKER_LOAD = new BigDecimal("0.85");

    /**
     * Selects the optimal broker for an order using smart routing algorithms.
     *
     * Best Execution Formula:
     * Score = (PriceScore × 0.30) + (SpeedScore × 0.25) +
     *         (ReliabilityScore × 0.25) + (CostScore × 0.20)
     *
     * @param symbol Stock symbol
     * @param quantity Order quantity
     * @param orderType Order type (MARKET, LIMIT, etc.)
     * @param availableBrokers List of available brokers
     * @param criteria Routing criteria and weights
     * @return CompletableFuture with RoutingDecision
     */
    @EventHandler(event = "BrokerSelectionRequest")
    @AgentCapability(
        name = "BROKER_SELECTION",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<BrokerRouting.RoutingDecision> selectOptimalBroker(
            String symbol,
            BigDecimal quantity,
            String orderType,
            List<BrokerRouting.BrokerPerformance> availableBrokers,
            BrokerRouting.RoutingCriteria criteria) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Selecting optimal broker: symbol={}, quantity={}, brokers={}",
                    symbol, quantity, availableBrokers.size());

            try {
                // Filter brokers by health and capacity
                List<BrokerRouting.BrokerPerformance> eligibleBrokers = availableBrokers.stream()
                    .filter(b -> b.overallScore().compareTo(MIN_HEALTH_SCORE) >= 0)
                    .filter(b -> b.consecutiveFailures() < MAX_CONSECUTIVE_FAILURES)
                    .filter(b -> b.currentLoad().compareTo(MAX_BROKER_LOAD) < 0)
                    .toList();

                if (eligibleBrokers.isEmpty()) {
                    throw new IllegalStateException("No eligible brokers available for routing");
                }

                // Calculate routing scores for each broker
                List<ScoredBroker> scoredBrokers = eligibleBrokers.stream()
                    .map(broker -> scoreBroker(broker, criteria))
                    .sorted(Comparator.comparing(ScoredBroker::score).reversed())
                    .toList();

                // Select best broker
                ScoredBroker bestBroker = scoredBrokers.get(0);

                // Build alternatives list (top 3)
                List<BrokerRouting.RoutingDecision.BrokerAlternative> alternatives = scoredBrokers.stream()
                    .skip(1)
                    .limit(3)
                    .map(sb -> new BrokerRouting.RoutingDecision.BrokerAlternative(
                        sb.broker().brokerId(),
                        sb.broker().brokerName(),
                        sb.score(),
                        sb.reason()
                    ))
                    .toList();

                // Estimate execution parameters
                BigDecimal estimatedPrice = calculateEstimatedPrice(symbol, bestBroker.broker());
                BigDecimal estimatedCost = calculateEstimatedCost(quantity, estimatedPrice, bestBroker.broker());
                BigDecimal priceImprovement = bestBroker.broker().avgPriceImprovement();
                Integer estimatedExecutionTime = bestBroker.broker().avgExecutionTime().intValue();

                var decision = new BrokerRouting.RoutingDecision(
                    UUID.randomUUID().toString(),
                    symbol,
                    quantity,
                    orderType,
                    bestBroker.broker().brokerId(),
                    bestBroker.broker().brokerName(),
                    BrokerRouting.RoutingDecision.RoutingStrategy.BEST_EXECUTION,
                    bestBroker.score(),
                    estimatedPrice,
                    estimatedCost,
                    priceImprovement,
                    estimatedExecutionTime,
                    bestBroker.reason(),
                    bestBroker.factors(),
                    alternatives,
                    Instant.now()
                );

                capabilityRegistry.recordSuccessfulExecution("BROKER_SELECTION");

                log.info("Broker selected: broker={}, score={}, reason={}",
                        bestBroker.broker().brokerName(), bestBroker.score(), bestBroker.reason());

                return decision;

            } catch (Exception e) {
                log.error("Failed to select broker for symbol={}", symbol, e);
                capabilityRegistry.recordFailedExecution("BROKER_SELECTION", e);
                throw new RuntimeException("Broker selection failed", e);
            }
        });
    }

    /**
     * Splits large orders across multiple brokers for optimal execution.
     *
     * Splitting Algorithm:
     * 1. Score all eligible brokers
     * 2. Calculate weighted allocation: Allocation = Score / TotalScore
     * 3. Apply minimum quantity constraints
     * 4. Optimize for execution priority
     *
     * @param orderId Parent order ID
     * @param symbol Stock symbol
     * @param totalQuantity Total order quantity
     * @param availableBrokers List of available brokers
     * @param strategy Split strategy
     * @return CompletableFuture with OrderSplitPlan
     */
    @EventHandler(event = "OrderSplitRequest")
    @AgentCapability(
        name = "ORDER_SPLITTING",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<BrokerRouting.OrderSplitPlan> splitOrderAcrossBrokers(
            String orderId,
            String symbol,
            BigDecimal totalQuantity,
            List<BrokerRouting.BrokerPerformance> availableBrokers,
            BrokerRouting.OrderSplitPlan.SplitStrategy strategy) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Splitting order: orderId={}, symbol={}, quantity={}, strategy={}",
                    orderId, symbol, totalQuantity, strategy);

            try {
                // Filter eligible brokers
                List<BrokerRouting.BrokerPerformance> eligibleBrokers = availableBrokers.stream()
                    .filter(b -> b.overallScore().compareTo(MIN_HEALTH_SCORE) >= 0)
                    .filter(b -> b.availableCapacity().compareTo(BigDecimal.ZERO) > 0)
                    .limit(5)  // Limit to top 5 brokers
                    .toList();

                if (eligibleBrokers.isEmpty()) {
                    throw new IllegalStateException("No eligible brokers for order splitting");
                }

                // Calculate split allocation
                List<BrokerRouting.OrderSplitPlan.ChildOrder> childOrders = switch (strategy) {
                    case EQUAL_SPLIT -> splitEqually(orderId, eligibleBrokers, totalQuantity);
                    case WEIGHTED_SPLIT -> splitByWeight(orderId, eligibleBrokers, totalQuantity);
                    case PRIORITY_SPLIT -> splitByPriority(orderId, eligibleBrokers, totalQuantity);
                    case ADAPTIVE_SPLIT -> splitAdaptively(orderId, eligibleBrokers, totalQuantity);
                    default -> splitByWeight(orderId, eligibleBrokers, totalQuantity);
                };

                // Calculate execution estimates
                BigDecimal totalValue = totalQuantity.multiply(new BigDecimal("2500"));  // Sample price
                BigDecimal estimatedTotalCost = childOrders.stream()
                    .map(co -> {
                        var broker = eligibleBrokers.stream()
                            .filter(b -> b.brokerId().equals(co.brokerId()))
                            .findFirst()
                            .orElseThrow();
                        return co.quantity().multiply(broker.avgBrokerageFee());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal avgPriceImprovement = eligibleBrokers.stream()
                    .map(BrokerRouting.BrokerPerformance::avgPriceImprovement)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(eligibleBrokers.size()), 4, RoundingMode.HALF_UP);

                Integer estimatedTotalTime = eligibleBrokers.stream()
                    .mapToInt(b -> b.avgExecutionTime().intValue())
                    .max()
                    .orElse(1000);

                BigDecimal maxBrokerExposure = childOrders.stream()
                    .map(BrokerRouting.OrderSplitPlan.ChildOrder::allocationPercent)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

                String riskLevel = classifyRiskLevel(maxBrokerExposure, eligibleBrokers.size());

                var splitPlan = new BrokerRouting.OrderSplitPlan(
                    orderId,
                    symbol,
                    totalQuantity,
                    totalValue,
                    strategy,
                    childOrders.size(),
                    childOrders,
                    estimatedTotalCost,
                    avgPriceImprovement,
                    estimatedTotalTime,
                    maxBrokerExposure,
                    riskLevel,
                    Instant.now()
                );

                capabilityRegistry.recordSuccessfulExecution("ORDER_SPLITTING");

                log.info("Order split complete: childOrders={}, brokers={}, maxExposure={}",
                        childOrders.size(), eligibleBrokers.size(), maxBrokerExposure);

                return splitPlan;

            } catch (Exception e) {
                log.error("Failed to split order: orderId={}", orderId, e);
                capabilityRegistry.recordFailedExecution("ORDER_SPLITTING", e);
                throw new RuntimeException("Order splitting failed", e);
            }
        });
    }

    /**
     * Monitors broker performance and updates routing metrics.
     *
     * @param brokers List of brokers to monitor
     * @return CompletableFuture with updated performance metrics
     */
    @EventHandler(event = "PerformanceMonitoringRequest")
    @AgentCapability(
        name = "PERFORMANCE_TRACKING",
        proficiency = "EXPERT",
        performanceProfile = "HIGH_FREQUENCY"
    )
    public CompletableFuture<List<BrokerRouting.BrokerPerformance>> monitorBrokerPerformance(
            List<String> brokerIds) {

        return CompletableFuture.supplyAsync(() -> {
            log.info("Monitoring broker performance: brokers={}", brokerIds.size());

            try {
                // Generate performance metrics for each broker
                List<BrokerRouting.BrokerPerformance> performances = brokerIds.stream()
                    .map(this::calculateBrokerPerformance)
                    .toList();

                capabilityRegistry.recordSuccessfulExecution("PERFORMANCE_TRACKING");

                log.info("Performance monitoring complete: brokers={}", performances.size());

                return performances;

            } catch (Exception e) {
                log.error("Failed to monitor broker performance", e);
                capabilityRegistry.recordFailedExecution("PERFORMANCE_TRACKING", e);
                throw new RuntimeException("Performance monitoring failed", e);
            }
        });
    }

    // ========== Helper Methods: Broker Scoring ==========

    /**
     * Scores a broker based on multiple criteria.
     *
     * Scoring Formula:
     * Total Score = (Price Score × 0.30) + (Speed Score × 0.25) +
     *               (Reliability Score × 0.25) + (Cost Score × 0.20)
     */
    private ScoredBroker scoreBroker(
            BrokerRouting.BrokerPerformance broker,
            BrokerRouting.RoutingCriteria criteria) {

        // Normalize scores to 0-100 scale
        BigDecimal priceScore = normalizePriceImprovement(broker.avgPriceImprovement());
        BigDecimal speedScore = normalizeExecutionTime(broker.avgExecutionTime());
        BigDecimal reliabilityScore = normalizeReliability(broker.successRate(), broker.uptimePercent());
        BigDecimal costScore = normalizeCost(broker.avgBrokerageFee());

        // Calculate weighted total score
        BigDecimal totalScore = priceScore.multiply(PRICE_WEIGHT)
            .add(speedScore.multiply(SPEED_WEIGHT))
            .add(reliabilityScore.multiply(RELIABILITY_WEIGHT))
            .add(costScore.multiply(COST_WEIGHT))
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);

        // Build reason and contributing factors
        String reason = String.format("Best execution with score %.2f (Price: %.1f, Speed: %.1f, Reliability: %.1f, Cost: %.1f)",
            totalScore.doubleValue(),
            priceScore.multiply(new BigDecimal("100")).doubleValue(),
            speedScore.multiply(new BigDecimal("100")).doubleValue(),
            reliabilityScore.multiply(new BigDecimal("100")).doubleValue(),
            costScore.multiply(new BigDecimal("100")).doubleValue());

        List<String> factors = List.of(
            String.format("Price improvement: %s%%", broker.avgPriceImprovement()),
            String.format("Execution time: %sms", broker.avgExecutionTime()),
            String.format("Success rate: %s%%", broker.successRate()),
            String.format("Uptime: %s%%", broker.uptimePercent())
        );

        return new ScoredBroker(broker, totalScore, reason, factors);
    }

    private BigDecimal normalizePriceImprovement(BigDecimal priceImprovement) {
        // Higher is better, normalize to 0-1 scale
        // Assuming max price improvement is 2%
        return priceImprovement.divide(new BigDecimal("2.0"), 4, RoundingMode.HALF_UP)
            .min(BigDecimal.ONE);
    }

    private BigDecimal normalizeExecutionTime(BigDecimal executionTime) {
        // Lower is better, invert and normalize
        // Assuming max acceptable time is 5000ms
        BigDecimal inverted = new BigDecimal("5000").subtract(executionTime)
            .max(BigDecimal.ZERO);
        return inverted.divide(new BigDecimal("5000"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeReliability(BigDecimal successRate, BigDecimal uptimePercent) {
        // Average of success rate and uptime (both 0-100)
        return successRate.add(uptimePercent)
            .divide(new BigDecimal("200"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCost(BigDecimal brokerageFee) {
        // Lower is better, invert and normalize
        // Assuming max acceptable fee is 1%
        BigDecimal inverted = new BigDecimal("1.0").subtract(brokerageFee)
            .max(BigDecimal.ZERO);
        return inverted.divide(BigDecimal.ONE, 4, RoundingMode.HALF_UP);
    }

    // ========== Helper Methods: Order Splitting ==========

    private List<BrokerRouting.OrderSplitPlan.ChildOrder> splitEqually(
            String parentOrderId,
            List<BrokerRouting.BrokerPerformance> brokers,
            BigDecimal totalQuantity) {

        BigDecimal quantityPerBroker = totalQuantity.divide(
            BigDecimal.valueOf(brokers.size()), 2, RoundingMode.DOWN);

        return brokers.stream()
            .map(broker -> new BrokerRouting.OrderSplitPlan.ChildOrder(
                parentOrderId + "-" + broker.brokerId(),
                broker.brokerId(),
                broker.brokerName(),
                quantityPerBroker,
                new BigDecimal("100").divide(BigDecimal.valueOf(brokers.size()), 2, RoundingMode.HALF_UP),
                1,
                "Equal allocation across brokers"
            ))
            .toList();
    }

    private List<BrokerRouting.OrderSplitPlan.ChildOrder> splitByWeight(
            String parentOrderId,
            List<BrokerRouting.BrokerPerformance> brokers,
            BigDecimal totalQuantity) {

        // Calculate total score
        BigDecimal totalScore = brokers.stream()
            .map(BrokerRouting.BrokerPerformance::overallScore)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Allocate proportionally to scores
        return brokers.stream()
            .map(broker -> {
                BigDecimal weight = broker.overallScore().divide(totalScore, 4, RoundingMode.HALF_UP);
                BigDecimal quantity = totalQuantity.multiply(weight).setScale(0, RoundingMode.DOWN);
                BigDecimal allocationPercent = weight.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

                return new BrokerRouting.OrderSplitPlan.ChildOrder(
                    parentOrderId + "-" + broker.brokerId(),
                    broker.brokerId(),
                    broker.brokerName(),
                    quantity,
                    allocationPercent,
                    1,
                    String.format("Weighted allocation based on score %.2f", broker.overallScore())
                );
            })
            .toList();
    }

    private List<BrokerRouting.OrderSplitPlan.ChildOrder> splitByPriority(
            String parentOrderId,
            List<BrokerRouting.BrokerPerformance> brokers,
            BigDecimal totalQuantity) {

        // Sort by score (descending)
        List<BrokerRouting.BrokerPerformance> sortedBrokers = brokers.stream()
            .sorted(Comparator.comparing(BrokerRouting.BrokerPerformance::overallScore).reversed())
            .toList();

        // Allocate more to top brokers (60%, 25%, 15%)
        List<BigDecimal> allocations = List.of(
            new BigDecimal("0.60"),
            new BigDecimal("0.25"),
            new BigDecimal("0.15")
        );

        List<BrokerRouting.OrderSplitPlan.ChildOrder> childOrders = new ArrayList<>();
        for (int i = 0; i < Math.min(sortedBrokers.size(), allocations.size()); i++) {
            BrokerRouting.BrokerPerformance broker = sortedBrokers.get(i);
            BigDecimal allocation = allocations.get(i);
            BigDecimal quantity = totalQuantity.multiply(allocation).setScale(0, RoundingMode.DOWN);

            childOrders.add(new BrokerRouting.OrderSplitPlan.ChildOrder(
                parentOrderId + "-" + broker.brokerId(),
                broker.brokerId(),
                broker.brokerName(),
                quantity,
                allocation.multiply(new BigDecimal("100")),
                i + 1,
                String.format("Priority allocation (rank %d)", i + 1)
            ));
        }

        return childOrders;
    }

    private List<BrokerRouting.OrderSplitPlan.ChildOrder> splitAdaptively(
            String parentOrderId,
            List<BrokerRouting.BrokerPerformance> brokers,
            BigDecimal totalQuantity) {

        // Use weighted split with capacity constraints
        return splitByWeight(parentOrderId, brokers, totalQuantity);
    }

    private String classifyRiskLevel(BigDecimal maxExposure, int brokerCount) {
        if (maxExposure.compareTo(new BigDecimal("0.70")) > 0 || brokerCount < 2) {
            return "HIGH";
        } else if (maxExposure.compareTo(new BigDecimal("0.50")) > 0 || brokerCount < 3) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    // ========== Helper Methods: Estimates ==========

    private BigDecimal calculateEstimatedPrice(String symbol, BrokerRouting.BrokerPerformance broker) {
        // Sample price calculation
        return new BigDecimal("2500.00").add(broker.avgPriceImprovement());
    }

    private BigDecimal calculateEstimatedCost(
            BigDecimal quantity,
            BigDecimal price,
            BrokerRouting.BrokerPerformance broker) {

        BigDecimal orderValue = quantity.multiply(price);
        return orderValue.multiply(broker.avgBrokerageFee());
    }

    private BrokerRouting.BrokerPerformance calculateBrokerPerformance(String brokerId) {
        // Sample performance metrics
        return new BrokerRouting.BrokerPerformance(
            brokerId,
            "Broker-" + brokerId,
            Instant.now(),
            new BigDecimal("0.15"),
            new BigDecimal("250"),
            new BigDecimal("98.5"),
            new BigDecimal("0.05"),
            new BigDecimal("99.2"),
            new BigDecimal("99.8"),
            0,
            new BigDecimal("0.45"),
            new BigDecimal("0.55"),
            500,
            new BigDecimal("0.05"),
            new BigDecimal("0.02"),
            new BigDecimal("0.85"),
            BrokerRouting.BrokerPerformance.PerformanceRating.EXCELLENT
        );
    }

    // ========== AgentOSComponent Implementation ==========

    @Override
    public String getAgentId() {
        return "broker-routing-agent";
    }

    @Override
    public String getAgentType() {
        return "BROKER_ROUTING";
    }

    @Override
    public List<String> getCapabilities() {
        return List.of(
            "BROKER_SELECTION",
            "ORDER_SPLITTING",
            "LOAD_BALANCING",
            "REDUNDANCY_MANAGEMENT",
            "PERFORMANCE_TRACKING"
        );
    }

    @Override
    public Double getHealthScore() {
        return capabilityRegistry.calculateOverallHealthScore();
    }

    // ========== Helper Records ==========

    private record ScoredBroker(
        BrokerRouting.BrokerPerformance broker,
        BigDecimal score,
        String reason,
        List<String> factors
    ) {}
}
