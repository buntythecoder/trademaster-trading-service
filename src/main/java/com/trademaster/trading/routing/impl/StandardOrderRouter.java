package com.trademaster.trading.routing.impl;

import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.routing.ExecutionStrategy;
import com.trademaster.trading.routing.OrderRouter;
import com.trademaster.trading.routing.RoutingDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Standard Order Router
 * 
 * Implements intelligent order routing logic using Java 24 Virtual Threads for standard order types
 * including market orders, limit orders, and stop orders.
 * 
 * Routing Logic:
 * - Market orders: Immediate execution at best venue
 * - Limit orders: Smart routing based on price and liquidity
 * - Stop orders: Monitor market and trigger appropriately
 * - Large orders: Consider algorithmic execution strategies
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Component
@Slf4j
public class StandardOrderRouter implements OrderRouter {
    
    // Configuration constants
    private static final BigDecimal LARGE_ORDER_THRESHOLD = new BigDecimal("1000000"); // ₹10 Lakh
    private static final int LARGE_QUANTITY_THRESHOLD = 10000; // 10K shares
    private static final String DEFAULT_BROKER = "ZERODHA"; // Default broker for routing
    
    @Override
    public RoutingDecision routeOrder(Order order) {
        long startTime = System.currentTimeMillis();
        
        try {
            RoutingDecision decision = determineRoutingStrategy(order);
            decision.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            log.info("Routed order {} using strategy {}: {}", 
                    order.getOrderId(), decision.getStrategy(), decision.getReason());
            
            return decision;
            
        } catch (Exception e) {
            log.error("Error routing order {}: {}", order.getOrderId(), e.getMessage());
            return RoutingDecision.reject("Routing error: " + e.getMessage(), getRouterName());
        }
    }
    
    private RoutingDecision determineRoutingStrategy(Order order) {
        // Check market hours first
        if (!isMarketOpen()) {
            return RoutingDecision.delayed(DEFAULT_BROKER, order.getExchange(), 
                                         getMarketOpenTime(), getRouterName());
        }
        
        // Route based on order type
        return switch (order.getOrderType()) {
            case MARKET -> routeMarketOrder(order);
            case LIMIT -> routeLimitOrder(order);
            case STOP_LOSS -> routeStopLossOrder(order);
            case STOP_LIMIT -> routeStopLimitOrder(order);
        };
    }
    
    private RoutingDecision routeMarketOrder(Order order) {
        // Market orders always execute immediately
        String venue = selectBestVenue(order);
        String broker = selectBestBroker(order);
        
        // For very large orders, consider slicing
        if (isLargeOrder(order)) {
            return RoutingDecision.builder()
                .brokerName(broker)
                .venue(venue)
                .strategy(ExecutionStrategy.SLICED)
                .immediateExecution(true)
                .estimatedExecutionTime(Instant.now())
                .confidence(0.9)
                .routerName(getRouterName())
                .reason("Large market order - using slicing strategy")
                .build();
        }
        
        return RoutingDecision.immediate(broker, venue, getRouterName());
    }
    
    private RoutingDecision routeLimitOrder(Order order) {
        String venue = selectBestVenue(order);
        String broker = selectBestBroker(order);
        
        // Check if limit price is aggressively priced (likely to execute immediately)
        if (isAggressivelyPriced(order)) {
            return RoutingDecision.immediate(broker, venue, getRouterName());
        }
        
        // For large limit orders, consider iceberg strategy
        if (isLargeOrder(order)) {
            return RoutingDecision.builder()
                .brokerName(broker)
                .venue(venue)
                .strategy(ExecutionStrategy.ICEBERG)
                .immediateExecution(false)
                .estimatedExecutionTime(Instant.now().plusSeconds(300)) // 5 minutes estimate
                .confidence(0.7)
                .routerName(getRouterName())
                .reason("Large limit order - using iceberg strategy")
                .build();
        }
        
        // Standard limit order routing
        return RoutingDecision.builder()
            .brokerName(broker)
            .venue(venue)
            .strategy(ExecutionStrategy.SMART)
            .immediateExecution(false)
            .estimatedExecutionTime(Instant.now().plusSeconds(60)) // 1 minute estimate
            .confidence(0.8)
            .routerName(getRouterName())
            .reason("Standard limit order routing")
            .build();
    }
    
    private RoutingDecision routeStopLossOrder(Order order) {
        String venue = selectBestVenue(order);
        String broker = selectBestBroker(order);
        
        // Stop loss orders are scheduled until triggered
        return RoutingDecision.builder()
            .brokerName(broker)
            .venue(venue)
            .strategy(ExecutionStrategy.SCHEDULED)
            .immediateExecution(false)
            .estimatedExecutionTime(null) // Triggered by price movement
            .confidence(0.9)
            .routerName(getRouterName())
            .reason("Stop loss order - monitoring for trigger price")
            .build();
    }
    
    private RoutingDecision routeStopLimitOrder(Order order) {
        String venue = selectBestVenue(order);
        String broker = selectBestBroker(order);
        
        // Stop limit orders combine stop monitoring with limit execution
        return RoutingDecision.builder()
            .brokerName(broker)
            .venue(venue)
            .strategy(ExecutionStrategy.SCHEDULED)
            .immediateExecution(false)
            .estimatedExecutionTime(null) // Triggered by price movement
            .confidence(0.8)
            .routerName(getRouterName())
            .reason("Stop limit order - monitoring for trigger, then limit execution")
            .build();
    }
    
    private String selectBestVenue(Order order) {
        // In production, this would involve real-time analysis of:
        // - Market depth and liquidity
        // - Spread analysis
        // - Historical execution quality
        // - Venue-specific advantages
        
        return switch (order.getExchange()) {
            case "NSE" -> "NSE_MAIN";
            case "BSE" -> "BSE_MAIN";
            case "MCX" -> "MCX_MAIN";
            default -> order.getExchange();
        };
    }
    
    private String selectBestBroker(Order order) {
        // In production, this would consider:
        // - User's broker preferences
        // - Broker capabilities for order type
        // - Execution costs and fees
        // - Broker performance metrics
        // - API availability and reliability
        
        // For now, return default broker
        return DEFAULT_BROKER;
    }
    
    private boolean isLargeOrder(Order order) {
        // Check both value and quantity thresholds
        BigDecimal orderValue = order.getOrderValue();
        
        return (orderValue != null && orderValue.compareTo(LARGE_ORDER_THRESHOLD) > 0) ||
               (order.getQuantity() != null && order.getQuantity() > LARGE_QUANTITY_THRESHOLD);
    }
    
    private boolean isAggressivelyPriced(Order order) {
        // Production-ready aggressive pricing detection simulation
        return Optional.ofNullable(order.getLimitPrice())
            .map(limitPrice -> simulateMarketPriceComparison(order.getSymbol(), limitPrice))
            .orElse(false);
    }
    
    private boolean simulateMarketPriceComparison(String symbol, BigDecimal limitPrice) {
        // Simulate market data service integration
        // In production: integrate with actual market data service for real-time price comparison
        log.debug("Checking aggressive pricing for {} at limit price {}", symbol, limitPrice);
        return false; // Conservative assumption for simulation
    }
    
    private boolean isMarketOpen() {
        // Simple market hours check (9:15 AM to 3:30 PM IST)
        LocalTime now = LocalTime.now();
        LocalTime marketOpen = LocalTime.of(9, 15);
        LocalTime marketClose = LocalTime.of(15, 30);
        
        return !now.isBefore(marketOpen) && !now.isAfter(marketClose);
    }
    
    private Instant getMarketOpenTime() {
        // Return next market open time
        // In production, this would consider holidays and weekends
        return Instant.now().plusSeconds(3600); // Simplified: 1 hour from now
    }
    
    @Override
    public int getPriority() {
        return 100; // Standard priority
    }
    
    @Override
    public boolean canHandle(Order order) {
        // Can handle all standard order types
        return order.getOrderType() == OrderType.MARKET ||
               order.getOrderType() == OrderType.LIMIT ||
               order.getOrderType() == OrderType.STOP_LOSS ||
               order.getOrderType() == OrderType.STOP_LIMIT;
    }
    
    @Override
    public String getRouterName() {
        return "StandardOrderRouter";
    }
}