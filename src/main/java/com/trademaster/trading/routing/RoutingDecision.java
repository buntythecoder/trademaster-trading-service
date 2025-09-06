package com.trademaster.trading.routing;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Routing Decision
 * 
 * Encapsulates the routing decision for an order including
 * destination, execution strategy, and timing considerations.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class RoutingDecision {
    
    /**
     * Target broker for order execution
     */
    private String brokerName;
    
    /**
     * Execution venue (exchange, dark pool, etc.)
     */
    private String venue;
    
    /**
     * Execution strategy
     */
    private ExecutionStrategy strategy;
    
    /**
     * Whether to execute immediately or queue
     */
    private boolean immediateExecution;
    
    /**
     * Estimated execution time
     */
    private Instant estimatedExecutionTime;
    
    /**
     * Routing confidence score (0.0 to 1.0)
     */
    private double confidence;
    
    /**
     * Routing reason/explanation
     */
    private String reason;
    
    /**
     * Router that made this decision
     */
    private String routerName;
    
    /**
     * Processing time in milliseconds
     */
    private long processingTimeMs;
    
    /**
     * Create immediate execution decision
     */
    public static RoutingDecision immediate(String brokerName, String venue, String routerName) {
        return RoutingDecision.builder()
            .brokerName(brokerName)
            .venue(venue)
            .strategy(ExecutionStrategy.IMMEDIATE)
            .immediateExecution(true)
            .estimatedExecutionTime(Instant.now())
            .confidence(1.0)
            .routerName(routerName)
            .reason("Immediate execution for " + venue)
            .build();
    }
    
    /**
     * Create delayed execution decision
     */
    public static RoutingDecision delayed(String brokerName, String venue, Instant executionTime, String routerName) {
        return RoutingDecision.builder()
            .brokerName(brokerName)
            .venue(venue)
            .strategy(ExecutionStrategy.SCHEDULED)
            .immediateExecution(false)
            .estimatedExecutionTime(executionTime)
            .confidence(0.8)
            .routerName(routerName)
            .reason("Scheduled execution for " + venue)
            .build();
    }
    
    /**
     * Create rejection decision
     */
    public static RoutingDecision reject(String reason, String routerName) {
        return RoutingDecision.builder()
            .strategy(ExecutionStrategy.REJECT)
            .immediateExecution(false)
            .confidence(0.0)
            .routerName(routerName)
            .reason(reason)
            .build();
    }
}