package com.trademaster.trading.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Broker Routing Data Transfer Objects
 *
 * Contains DTOs for multi-broker order routing, smart order routing algorithms,
 * broker selection, and order splitting strategies.
 *
 * Smart Routing Algorithms:
 * - Best Execution: Price improvement + liquidity + historical performance
 * - Load Balancing: Distribute orders across brokers based on capacity
 * - Cost Optimization: Minimize brokerage fees and impact costs
 * - Redundancy Routing: Backup broker selection for resilience
 *
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
public sealed interface BrokerRouting {

    /**
     * Broker connection status and health metrics.
     */
    record BrokerStatus(
        String brokerId,
        String brokerName,
        ConnectionState connectionState,
        BigDecimal healthScore,
        Integer successRate,
        BigDecimal avgExecutionTime,
        Integer pendingOrders,
        Integer totalOrdersToday,
        BigDecimal orderValueCapacity,
        Instant lastHeartbeat,
        List<String> supportedOrderTypes
    ) implements BrokerRouting {

        public enum ConnectionState {
            CONNECTED, DISCONNECTED, DEGRADED, MAINTENANCE
        }
    }

    /**
     * Smart order routing decision with broker selection rationale.
     */
    record RoutingDecision(
        String orderId,
        String symbol,
        BigDecimal quantity,
        String orderType,

        // Selected Broker
        String selectedBrokerId,
        String selectedBrokerName,

        // Routing Strategy
        RoutingStrategy strategy,
        BigDecimal routingScore,

        // Execution Details
        BigDecimal estimatedPrice,
        BigDecimal estimatedCost,
        BigDecimal priceImprovement,
        Integer estimatedExecutionTime,

        // Decision Rationale
        String primaryReason,
        List<String> contributingFactors,

        // Alternative Brokers
        List<BrokerAlternative> alternatives,

        Instant timestamp
    ) implements BrokerRouting {

        public enum RoutingStrategy {
            BEST_EXECUTION,      // Best price + liquidity + performance
            LOAD_BALANCING,      // Distribute load across brokers
            COST_OPTIMIZATION,   // Minimize total trading costs
            REDUNDANCY,          // Backup broker for resilience
            SMART_HYBRID         // Adaptive strategy combining multiple factors
        }

        public record BrokerAlternative(
            String brokerId,
            String brokerName,
            BigDecimal score,
            String reason
        ) {}
    }

    /**
     * Multi-broker order split for large orders.
     */
    record OrderSplitPlan(
        String parentOrderId,
        String symbol,
        BigDecimal totalQuantity,
        BigDecimal totalValue,

        // Split Strategy
        SplitStrategy strategy,
        Integer numberOfBrokers,

        // Child Orders
        List<ChildOrder> childOrders,

        // Execution Plan
        BigDecimal estimatedTotalCost,
        BigDecimal estimatedPriceImprovement,
        Integer estimatedTotalTime,

        // Risk Mitigation
        BigDecimal maxBrokerExposure,
        String riskLevel,

        Instant createdAt
    ) implements BrokerRouting {

        public enum SplitStrategy {
            EQUAL_SPLIT,         // Equal quantity across brokers
            WEIGHTED_SPLIT,      // Weighted by broker capacity and performance
            PRIORITY_SPLIT,      // Best brokers get larger allocations
            SEQUENTIAL_SPLIT,    // Execute one broker at a time
            ADAPTIVE_SPLIT       // Dynamic allocation based on real-time conditions
        }

        public record ChildOrder(
            String childOrderId,
            String brokerId,
            String brokerName,
            BigDecimal quantity,
            BigDecimal allocationPercent,
            Integer executionPriority,
            String reason
        ) {}
    }

    /**
     * Broker performance metrics for routing decisions.
     */
    record BrokerPerformance(
        String brokerId,
        String brokerName,
        Instant timestamp,

        // Execution Quality
        BigDecimal avgPriceImprovement,
        BigDecimal avgExecutionTime,
        BigDecimal fillRate,
        BigDecimal slippageRate,

        // Reliability
        BigDecimal successRate,
        BigDecimal uptimePercent,
        Integer consecutiveFailures,

        // Capacity
        BigDecimal currentLoad,
        BigDecimal availableCapacity,
        Integer maxConcurrentOrders,

        // Cost
        BigDecimal avgBrokerageFee,
        BigDecimal avgImpactCost,

        // Performance Score
        BigDecimal overallScore,
        PerformanceRating rating
    ) implements BrokerRouting {

        public enum PerformanceRating {
            EXCELLENT, GOOD, AVERAGE, POOR, CRITICAL
        }
    }

    /**
     * Broker selection criteria and weights.
     */
    record RoutingCriteria(
        // Criteria Weights (sum = 1.0)
        BigDecimal priceWeight,
        BigDecimal speedWeight,
        BigDecimal reliabilityWeight,
        BigDecimal costWeight,
        BigDecimal capacityWeight,

        // Constraints
        BigDecimal minBrokerHealth,
        Integer maxConsecutiveFailures,
        BigDecimal maxBrokerLoad,

        // Preferences
        List<String> preferredBrokers,
        List<String> excludedBrokers,

        // Risk Parameters
        boolean requireRedundancy,
        Integer minAlternativeBrokers
    ) implements BrokerRouting {}

    /**
     * Broker routing statistics and analytics.
     */
    record RoutingAnalytics(
        Instant timestamp,
        String timeframe,

        // Routing Distribution
        List<BrokerDistribution> brokerDistribution,

        // Performance Metrics
        BigDecimal avgRoutingScore,
        BigDecimal avgPriceImprovement,
        BigDecimal avgExecutionTime,
        BigDecimal overallSuccessRate,

        // Strategy Effectiveness
        List<StrategyPerformance> strategyPerformance,

        // Cost Savings
        BigDecimal totalCostSavings,
        BigDecimal totalPriceImprovement,

        Integer totalOrdersRouted,
        Integer totalBrokersUsed
    ) implements BrokerRouting {

        public record BrokerDistribution(
            String brokerId,
            String brokerName,
            Integer ordersRouted,
            BigDecimal volumePercent,
            BigDecimal avgScore
        ) {}

        public record StrategyPerformance(
            String strategy,
            Integer ordersRouted,
            BigDecimal successRate,
            BigDecimal avgPriceImprovement,
            BigDecimal avgExecutionTime
        ) {}
    }

    /**
     * Real-time broker capacity and availability.
     */
    record BrokerCapacity(
        String brokerId,
        String brokerName,
        Instant timestamp,

        // Current Load
        Integer activeOrders,
        Integer pendingOrders,
        BigDecimal currentVolume,

        // Capacity Limits
        Integer maxOrdersPerMinute,
        Integer maxConcurrentOrders,
        BigDecimal maxOrderValue,
        BigDecimal dailyVolumeLimit,

        // Availability
        BigDecimal utilizationPercent,
        BigDecimal availableCapacity,
        CapacityStatus status,

        // Queue Status
        Integer queueDepth,
        Integer avgQueueTime
    ) implements BrokerRouting {

        public enum CapacityStatus {
            AVAILABLE, HIGH_UTILIZATION, NEAR_CAPACITY, AT_CAPACITY, UNAVAILABLE
        }
    }

    /**
     * Broker redundancy and failover configuration.
     */
    record RedundancyPlan(
        String primaryBrokerId,
        List<String> backupBrokerIds,

        // Failover Strategy
        FailoverStrategy strategy,
        Integer maxFailoverAttempts,
        Integer failoverTimeoutSeconds,

        // Health Thresholds
        BigDecimal minHealthScore,
        Integer maxConsecutiveFailures,

        // Automatic Recovery
        boolean autoRecoveryEnabled,
        Integer recoveryCheckIntervalSeconds
    ) implements BrokerRouting {

        public enum FailoverStrategy {
            IMMEDIATE,           // Switch immediately on failure
            GRACEFUL,            // Complete pending orders before switching
            WEIGHTED_PRIORITY,   // Try backups in priority order
            ROUND_ROBIN         // Rotate through backups
        }
    }
}
