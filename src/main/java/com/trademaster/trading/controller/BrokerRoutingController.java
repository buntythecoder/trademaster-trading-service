package com.trademaster.trading.controller;

import com.trademaster.trading.agentos.agents.BrokerRoutingAgent;
import com.trademaster.trading.dto.BrokerRouting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Broker Routing REST API Controller
 *
 * Provides intelligent multi-broker order routing capabilities including broker selection,
 * order splitting, load balancing, and performance tracking using the BrokerRoutingAgent.
 *
 * Endpoints:
 * - POST /api/v1/routing/select - Select optimal broker for an order
 * - POST /api/v1/routing/split - Split order across multiple brokers
 * - GET /api/v1/routing/performance - Get broker performance metrics
 *
 * Smart Routing Algorithms:
 * - Best Execution: Weighted scoring across price, speed, reliability, cost
 * - Order Splitting: Weighted allocation, priority-based, adaptive strategies
 * - Load Balancing: Distribute orders based on broker capacity and utilization
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/v1/routing")
@RequiredArgsConstructor
@Validated
@Slf4j
public class BrokerRoutingController {

    private final BrokerRoutingAgent brokerRoutingAgent;

    /**
     * Selects the optimal broker for an order using smart routing algorithms.
     *
     * Uses weighted scoring:
     * - Price (30%): Price improvement and execution quality
     * - Speed (25%): Execution time and latency
     * - Reliability (25%): Success rate and uptime
     * - Cost (20%): Brokerage fees and impact costs
     *
     * @param request Broker selection request
     * @return CompletableFuture with RoutingDecisionResponse
     */
    @PostMapping("/select")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public CompletableFuture<ResponseEntity<RoutingDecisionResponse>> selectBroker(
            @RequestBody @Validated BrokerSelectionRequest request) {

        log.info("Broker selection request: symbol={}, quantity={}", request.symbol(), request.quantity());

        // Generate sample broker performance metrics
        List<BrokerRouting.BrokerPerformance> availableBrokers = generateSampleBrokerPerformance();

        // Create default routing criteria
        BrokerRouting.RoutingCriteria criteria = new BrokerRouting.RoutingCriteria(
            new BigDecimal("0.30"),  // priceWeight
            new BigDecimal("0.25"),  // speedWeight
            new BigDecimal("0.25"),  // reliabilityWeight
            new BigDecimal("0.20"),  // costWeight
            new BigDecimal("0.05"),  // capacityWeight
            new BigDecimal("0.70"),  // minBrokerHealth
            3,                        // maxConsecutiveFailures
            new BigDecimal("0.85"),  // maxBrokerLoad
            List.of(),               // preferredBrokers
            List.of(),               // excludedBrokers
            false,                   // requireRedundancy
            2                        // minAlternativeBrokers
        );

        return brokerRoutingAgent.selectOptimalBroker(
            request.symbol(),
            request.quantity(),
            request.orderType(),
            availableBrokers,
            criteria
        )
        .thenApply(decision -> {
            var response = RoutingDecisionResponse.fromDomain(decision);
            return ResponseEntity.ok(response);
        })
        .exceptionally(ex -> {
            log.error("Failed to select broker: symbol={}", request.symbol(), ex);
            return ResponseEntity.internalServerError().build();
        });
    }

    /**
     * Splits a large order across multiple brokers for optimal execution.
     *
     * Splitting Strategies:
     * - EQUAL_SPLIT: Equal quantity across brokers
     * - WEIGHTED_SPLIT: Allocation by broker score (recommended)
     * - PRIORITY_SPLIT: Best brokers get larger allocations
     * - ADAPTIVE_SPLIT: Dynamic allocation based on real-time conditions
     *
     * @param request Order split request
     * @return CompletableFuture with OrderSplitPlanResponse
     */
    @PostMapping("/split")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public CompletableFuture<ResponseEntity<OrderSplitPlanResponse>> splitOrder(
            @RequestBody @Validated OrderSplitRequest request) {

        log.info("Order split request: symbol={}, quantity={}, strategy={}",
                request.symbol(), request.totalQuantity(), request.strategy());

        // Generate sample broker performance metrics
        List<BrokerRouting.BrokerPerformance> availableBrokers = generateSampleBrokerPerformance();

        return brokerRoutingAgent.splitOrderAcrossBrokers(
            request.orderId(),
            request.symbol(),
            request.totalQuantity(),
            availableBrokers,
            request.strategy()
        )
        .thenApply(splitPlan -> {
            var response = OrderSplitPlanResponse.fromDomain(splitPlan);
            return ResponseEntity.ok(response);
        })
        .exceptionally(ex -> {
            log.error("Failed to split order: orderId={}", request.orderId(), ex);
            return ResponseEntity.internalServerError().build();
        });
    }

    /**
     * Gets real-time broker performance metrics for routing decisions.
     *
     * Performance Metrics:
     * - Execution quality (price improvement, fill rate, slippage)
     * - Reliability (success rate, uptime, consecutive failures)
     * - Capacity (current load, available capacity)
     * - Cost (brokerage fees, impact costs)
     *
     * @return CompletableFuture with BrokerPerformanceResponse
     */
    @GetMapping("/performance")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    public CompletableFuture<ResponseEntity<List<BrokerPerformanceResponse>>> getBrokerPerformance() {

        log.info("Broker performance request");

        List<String> brokerIds = List.of("ZERODHA", "UPSTOX", "ANGELONE", "ICICI", "HDFC");

        return brokerRoutingAgent.monitorBrokerPerformance(brokerIds)
            .thenApply(performances -> {
                var response = performances.stream()
                    .map(BrokerPerformanceResponse::fromDomain)
                    .collect(Collectors.toList());
                return ResponseEntity.ok(response);
            })
            .exceptionally(ex -> {
                log.error("Failed to get broker performance", ex);
                return ResponseEntity.internalServerError().build();
            });
    }

    // ========== Helper Methods ==========

    /**
     * Generates sample broker performance metrics for demonstration.
     */
    private List<BrokerRouting.BrokerPerformance> generateSampleBrokerPerformance() {
        return List.of(
            new BrokerRouting.BrokerPerformance(
                "ZERODHA",
                "Zerodha",
                Instant.now(),
                new BigDecimal("0.18"),     // avgPriceImprovement
                new BigDecimal("245"),      // avgExecutionTime
                new BigDecimal("98.8"),     // fillRate
                new BigDecimal("0.04"),     // slippageRate
                new BigDecimal("99.3"),     // successRate
                new BigDecimal("99.9"),     // uptimePercent
                0,                          // consecutiveFailures
                new BigDecimal("0.42"),     // currentLoad
                new BigDecimal("0.58"),     // availableCapacity
                600,                        // maxConcurrentOrders
                new BigDecimal("0.03"),     // avgBrokerageFee
                new BigDecimal("0.015"),    // avgImpactCost
                new BigDecimal("0.88"),     // overallScore
                BrokerRouting.BrokerPerformance.PerformanceRating.EXCELLENT
            ),
            new BrokerRouting.BrokerPerformance(
                "UPSTOX",
                "Upstox",
                Instant.now(),
                new BigDecimal("0.15"),
                new BigDecimal("280"),
                new BigDecimal("98.2"),
                new BigDecimal("0.06"),
                new BigDecimal("98.9"),
                new BigDecimal("99.7"),
                0,
                new BigDecimal("0.35"),
                new BigDecimal("0.65"),
                550,
                new BigDecimal("0.025"),
                new BigDecimal("0.012"),
                new BigDecimal("0.85"),
                BrokerRouting.BrokerPerformance.PerformanceRating.EXCELLENT
            ),
            new BrokerRouting.BrokerPerformance(
                "ANGELONE",
                "Angel One",
                Instant.now(),
                new BigDecimal("0.12"),
                new BigDecimal("320"),
                new BigDecimal("97.8"),
                new BigDecimal("0.08"),
                new BigDecimal("98.5"),
                new BigDecimal("99.5"),
                1,
                new BigDecimal("0.55"),
                new BigDecimal("0.45"),
                500,
                new BigDecimal("0.04"),
                new BigDecimal("0.018"),
                new BigDecimal("0.80"),
                BrokerRouting.BrokerPerformance.PerformanceRating.GOOD
            ),
            new BrokerRouting.BrokerPerformance(
                "ICICI",
                "ICICI Direct",
                Instant.now(),
                new BigDecimal("0.10"),
                new BigDecimal("350"),
                new BigDecimal("97.5"),
                new BigDecimal("0.10"),
                new BigDecimal("98.2"),
                new BigDecimal("99.8"),
                0,
                new BigDecimal("0.48"),
                new BigDecimal("0.52"),
                450,
                new BigDecimal("0.05"),
                new BigDecimal("0.020"),
                new BigDecimal("0.78"),
                BrokerRouting.BrokerPerformance.PerformanceRating.GOOD
            ),
            new BrokerRouting.BrokerPerformance(
                "HDFC",
                "HDFC Securities",
                Instant.now(),
                new BigDecimal("0.08"),
                new BigDecimal("380"),
                new BigDecimal("97.2"),
                new BigDecimal("0.12"),
                new BigDecimal("98.0"),
                new BigDecimal("99.6"),
                0,
                new BigDecimal("0.52"),
                new BigDecimal("0.48"),
                400,
                new BigDecimal("0.055"),
                new BigDecimal("0.022"),
                new BigDecimal("0.75"),
                BrokerRouting.BrokerPerformance.PerformanceRating.GOOD
            )
        );
    }

    // ========== Request DTOs ==========

    /**
     * Broker selection request DTO.
     */
    public record BrokerSelectionRequest(
        String symbol,
        BigDecimal quantity,
        String orderType
    ) {}

    /**
     * Order split request DTO.
     */
    public record OrderSplitRequest(
        String orderId,
        String symbol,
        BigDecimal totalQuantity,
        BrokerRouting.OrderSplitPlan.SplitStrategy strategy
    ) {}

    // ========== Response DTOs ==========

    /**
     * Routing decision response DTO.
     */
    public record RoutingDecisionResponse(
        String orderId,
        String symbol,
        BigDecimal quantity,
        String orderType,
        String selectedBrokerId,
        String selectedBrokerName,
        String strategy,
        BigDecimal routingScore,
        BigDecimal estimatedPrice,
        BigDecimal estimatedCost,
        BigDecimal priceImprovement,
        Integer estimatedExecutionTime,
        String primaryReason,
        List<String> contributingFactors,
        List<BrokerAlternativeResponse> alternatives,
        String timestamp
    ) {
        public static RoutingDecisionResponse fromDomain(BrokerRouting.RoutingDecision decision) {
            return new RoutingDecisionResponse(
                decision.orderId(),
                decision.symbol(),
                decision.quantity(),
                decision.orderType(),
                decision.selectedBrokerId(),
                decision.selectedBrokerName(),
                decision.strategy().name(),
                decision.routingScore(),
                decision.estimatedPrice(),
                decision.estimatedCost(),
                decision.priceImprovement(),
                decision.estimatedExecutionTime(),
                decision.primaryReason(),
                decision.contributingFactors(),
                decision.alternatives().stream()
                    .map(BrokerAlternativeResponse::fromDomain)
                    .collect(Collectors.toList()),
                decision.timestamp().toString()
            );
        }
    }

    /**
     * Broker alternative response DTO.
     */
    public record BrokerAlternativeResponse(
        String brokerId,
        String brokerName,
        BigDecimal score,
        String reason
    ) {
        public static BrokerAlternativeResponse fromDomain(
                BrokerRouting.RoutingDecision.BrokerAlternative alt) {
            return new BrokerAlternativeResponse(
                alt.brokerId(),
                alt.brokerName(),
                alt.score(),
                alt.reason()
            );
        }
    }

    /**
     * Order split plan response DTO.
     */
    public record OrderSplitPlanResponse(
        String parentOrderId,
        String symbol,
        BigDecimal totalQuantity,
        BigDecimal totalValue,
        String strategy,
        Integer numberOfBrokers,
        List<ChildOrderResponse> childOrders,
        BigDecimal estimatedTotalCost,
        BigDecimal estimatedPriceImprovement,
        Integer estimatedTotalTime,
        BigDecimal maxBrokerExposure,
        String riskLevel,
        String createdAt
    ) {
        public static OrderSplitPlanResponse fromDomain(BrokerRouting.OrderSplitPlan plan) {
            return new OrderSplitPlanResponse(
                plan.parentOrderId(),
                plan.symbol(),
                plan.totalQuantity(),
                plan.totalValue(),
                plan.strategy().name(),
                plan.numberOfBrokers(),
                plan.childOrders().stream()
                    .map(ChildOrderResponse::fromDomain)
                    .collect(Collectors.toList()),
                plan.estimatedTotalCost(),
                plan.estimatedPriceImprovement(),
                plan.estimatedTotalTime(),
                plan.maxBrokerExposure(),
                plan.riskLevel(),
                plan.createdAt().toString()
            );
        }
    }

    /**
     * Child order response DTO.
     */
    public record ChildOrderResponse(
        String childOrderId,
        String brokerId,
        String brokerName,
        BigDecimal quantity,
        BigDecimal allocationPercent,
        Integer executionPriority,
        String reason
    ) {
        public static ChildOrderResponse fromDomain(BrokerRouting.OrderSplitPlan.ChildOrder order) {
            return new ChildOrderResponse(
                order.childOrderId(),
                order.brokerId(),
                order.brokerName(),
                order.quantity(),
                order.allocationPercent(),
                order.executionPriority(),
                order.reason()
            );
        }
    }

    /**
     * Broker performance response DTO.
     */
    public record BrokerPerformanceResponse(
        String brokerId,
        String brokerName,
        String timestamp,
        BigDecimal avgPriceImprovement,
        BigDecimal avgExecutionTime,
        BigDecimal fillRate,
        BigDecimal slippageRate,
        BigDecimal successRate,
        BigDecimal uptimePercent,
        Integer consecutiveFailures,
        BigDecimal currentLoad,
        BigDecimal availableCapacity,
        Integer maxConcurrentOrders,
        BigDecimal avgBrokerageFee,
        BigDecimal avgImpactCost,
        BigDecimal overallScore,
        String rating
    ) {
        public static BrokerPerformanceResponse fromDomain(BrokerRouting.BrokerPerformance perf) {
            return new BrokerPerformanceResponse(
                perf.brokerId(),
                perf.brokerName(),
                perf.timestamp().toString(),
                perf.avgPriceImprovement(),
                perf.avgExecutionTime(),
                perf.fillRate(),
                perf.slippageRate(),
                perf.successRate(),
                perf.uptimePercent(),
                perf.consecutiveFailures(),
                perf.currentLoad(),
                perf.availableCapacity(),
                perf.maxConcurrentOrders(),
                perf.avgBrokerageFee(),
                perf.avgImpactCost(),
                perf.overallScore(),
                perf.rating().name()
            );
        }
    }
}
