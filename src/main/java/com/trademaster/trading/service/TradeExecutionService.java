package com.trademaster.trading.service;

import com.trademaster.trading.dto.ExecutionReport;
import com.trademaster.trading.dto.MarketDataSnapshot;
import com.trademaster.trading.dto.OrderExecution;
import com.trademaster.trading.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Trade Execution Service
 * 
 * Ultra-high performance trade execution engine with Java 24 Virtual Threads:
 * - Smart order routing across multiple venues
 * - Advanced execution algorithms (TWAP, VWAP, Implementation Shortfall)
 * - Real-time market data integration
 * - Sub-millisecond execution latency
 * - Multi-venue connectivity and arbitrage
 * - Dark pool and lit venue optimization
 * 
 * Performance Targets:
 * - Order-to-market latency: <500 microseconds
 * - Market data processing: <100 microseconds
 * - Order routing decision: <200 microseconds
 * - Fill reporting: <300 microseconds
 * - Throughput: 100,000+ orders/second per instance
 * 
 * Venue Connectivity:
 * - NSE (National Stock Exchange)
 * - BSE (Bombay Stock Exchange)  
 * - MCX (Multi Commodity Exchange)
 * - Dark pools and ATS venues
 * - International venues (US, EU, APAC)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Ultra-Low Latency)
 */
public interface TradeExecutionService {
    
    // ========== Core Order Execution ==========
    
    /**
     * Execute order with optimal routing
     * 
     * @param userId The user ID
     * @param order The order to execute
     * @return CompletableFuture<OrderExecution> execution result
     */
    CompletableFuture<OrderExecution> executeOrder(Long userId, Order order);
    
    /**
     * Execute order with specific venue
     * 
     * @param userId The user ID
     * @param order The order to execute
     * @param venue Target venue
     * @return CompletableFuture<OrderExecution> execution result
     */
    CompletableFuture<OrderExecution> executeOrderOnVenue(Long userId, Order order, String venue);
    
    /**
     * Execute multiple orders in batch
     * 
     * @param userId The user ID
     * @param orders List of orders to execute
     * @return CompletableFuture<List<OrderExecution>> execution results
     */
    CompletableFuture<List<OrderExecution>> executeBatchOrders(Long userId, List<Order> orders);
    
    
    // ========== Smart Order Routing ==========
    
    /**
     * Find best execution venue for order
     * 
     * @param order The order to route
     * @param marketData Current market data
     * @return CompletableFuture<String> optimal venue
     */
    CompletableFuture<String> findBestVenue(Order order, MarketDataSnapshot marketData);
    
    /**
     * Route order across multiple venues for optimal execution
     * 
     * @param order The order to route
     * @param maxVenues Maximum number of venues to use
     * @return CompletableFuture<List<OrderExecution>> split order executions
     */
    CompletableFuture<List<OrderExecution>> routeOrderAcrossVenues(Order order, Integer maxVenues);
    
    /**
     * Calculate optimal order splitting strategy
     * 
     * @param order The large order to split
     * @param venues Available venues
     * @return CompletableFuture<Map<String, Integer>> venue allocation map
     */
    CompletableFuture<Map<String, Integer>> calculateOrderSplitting(Order order, List<String> venues);
    
    /**
     * Perform latency arbitrage analysis
     * 
     * @param symbol Trading symbol
     * @param venues List of venues to analyze
     * @return CompletableFuture<Map<String, BigDecimal>> latency advantages by venue
     */
    CompletableFuture<Map<String, BigDecimal>> analyzeLatencyArbitrage(String symbol, List<String> venues);
    
    // ========== Execution Algorithms ==========
    
    /**
     * Execute TWAP (Time-Weighted Average Price) strategy
     * 
     * @param userId The user ID
     * @param order The parent order
     * @param timeHorizonMinutes TWAP time horizon
     * @param sliceCount Number of child orders
     * @return CompletableFuture<List<OrderExecution>> child order executions
     */
    CompletableFuture<List<OrderExecution>> executeTWAP(Long userId, Order order, 
                                                        Integer timeHorizonMinutes, Integer sliceCount);
    
    /**
     * Execute VWAP (Volume-Weighted Average Price) strategy
     * 
     * @param userId The user ID
     * @param order The parent order
     * @param participationRate Market participation rate (0.0 - 1.0)
     * @return CompletableFuture<List<OrderExecution>> child order executions
     */
    CompletableFuture<List<OrderExecution>> executeVWAP(Long userId, Order order, BigDecimal participationRate);
    
    /**
     * Execute Implementation Shortfall strategy
     * 
     * @param userId The user ID
     * @param order The parent order
     * @param riskAversion Risk aversion parameter
     * @return CompletableFuture<List<OrderExecution>> optimized executions
     */
    CompletableFuture<List<OrderExecution>> executeImplementationShortfall(Long userId, Order order, 
                                                                           BigDecimal riskAversion);
    
    /**
     * Execute Arrival Price strategy
     * 
     * @param userId The user ID
     * @param order The parent order
     * @param urgency Execution urgency (0.0 - 1.0)
     * @return CompletableFuture<List<OrderExecution>> execution results
     */
    CompletableFuture<List<OrderExecution>> executeArrivalPrice(Long userId, Order order, BigDecimal urgency);
    
    /**
     * Execute Iceberg strategy with hidden quantity
     * 
     * @param userId The user ID
     * @param order The parent order
     * @param visibleQuantity Visible quantity per slice
     * @param refreshTime Time between slice refreshes
     * @return CompletableFuture<List<OrderExecution>> iceberg executions
     */
    CompletableFuture<List<OrderExecution>> executeIceberg(Long userId, Order order, 
                                                           Integer visibleQuantity, Integer refreshTime);
    
    // ========== Market Data Integration ==========
    
    /**
     * Get real-time market data snapshot
     * 
     * @param symbol Trading symbol
     * @param venues List of venues to query
     * @return CompletableFuture<MarketDataSnapshot> market data
     */
    CompletableFuture<MarketDataSnapshot> getMarketDataSnapshot(String symbol, List<String> venues);
    
    /**
     * Subscribe to real-time market data feed
     * 
     * @param symbols List of symbols to subscribe
     * @param venues List of venues
     * @param callback Market data callback handler
     */
    void subscribeToMarketData(List<String> symbols, List<String> venues, 
                              java.util.function.Consumer<MarketDataSnapshot> callback);
    
    /**
     * Calculate order book imbalance
     * 
     * @param symbol Trading symbol
     * @param venue Target venue
     * @return CompletableFuture<BigDecimal> order book imbalance ratio
     */
    CompletableFuture<BigDecimal> calculateOrderBookImbalance(String symbol, String venue);
    
    /**
     * Estimate market impact for order
     * 
     * @param order Order to analyze
     * @param marketData Current market conditions
     * @return CompletableFuture<BigDecimal> estimated market impact
     */
    CompletableFuture<BigDecimal> estimateMarketImpact(Order order, MarketDataSnapshot marketData);
    
    // ========== Venue Management ==========
    
    /**
     * Get available trading venues for symbol
     * 
     * @param symbol Trading symbol
     * @param assetClass Asset class (EQUITY, DERIVATIVE, etc.)
     * @return CompletableFuture<List<String>> available venues
     */
    CompletableFuture<List<String>> getAvailableVenues(String symbol, String assetClass);
    
    /**
     * Check venue connectivity status
     * 
     * @param venue Venue identifier
     * @return CompletableFuture<Boolean> connectivity status
     */
    CompletableFuture<Boolean> checkVenueConnectivity(String venue);
    
    /**
     * Get venue trading hours
     * 
     * @param venue Venue identifier
     * @return CompletableFuture<Map<String, Instant>> trading hours (open/close)
     */
    CompletableFuture<Map<String, Instant>> getVenueTradingHours(String venue);
    
    /**
     * Get venue latency metrics
     * 
     * @param venue Venue identifier
     * @return CompletableFuture<Map<String, Long>> latency metrics in microseconds
     */
    CompletableFuture<Map<String, Long>> getVenueLatencyMetrics(String venue);
    
    /**
     * Monitor venue performance
     * 
     * @param venues List of venues to monitor
     * @return CompletableFuture<Map<String, Map<String, Object>>> performance metrics
     */
    CompletableFuture<Map<String, Map<String, Object>>> monitorVenuePerformance(List<String> venues);
    
    // ========== Dark Pool Integration ==========
    
    /**
     * Route order to dark pools
     * 
     * @param order Order to execute in dark pool
     * @param darkPools List of available dark pools
     * @return CompletableFuture<OrderExecution> dark pool execution
     */
    CompletableFuture<OrderExecution> routeToDarkPools(Order order, List<String> darkPools);
    
    /**
     * Analyze dark pool liquidity
     * 
     * @param symbol Trading symbol
     * @param darkPools List of dark pools
     * @return CompletableFuture<Map<String, BigDecimal>> liquidity by dark pool
     */
    CompletableFuture<Map<String, BigDecimal>> analyzeDarkPoolLiquidity(String symbol, List<String> darkPools);
    
    /**
     * Execute with dark pool sweep
     * 
     * @param userId The user ID
     * @param order Order to execute
     * @param maxDarkPools Maximum dark pools to sweep
     * @return CompletableFuture<List<OrderExecution>> dark pool executions
     */
    CompletableFuture<List<OrderExecution>> executeDarkPoolSweep(Long userId, Order order, Integer maxDarkPools);
    
    // ========== Execution Monitoring ==========
    
    /**
     * Monitor order execution progress
     * 
     * @param orderId Order to monitor
     * @return CompletableFuture<ExecutionReport> real-time execution status
     */
    CompletableFuture<ExecutionReport> monitorExecution(Long orderId);
    
    /**
     * Get execution analytics
     * 
     * @param userId The user ID
     * @param fromTime Start time for analysis
     * @param toTime End time for analysis
     * @return CompletableFuture<Map<String, Object>> execution analytics
     */
    CompletableFuture<Map<String, Object>> getExecutionAnalytics(Long userId, Instant fromTime, Instant toTime);
    
    /**
     * Calculate execution quality metrics
     * 
     * @param executions List of executions to analyze
     * @param benchmark Benchmark for comparison (VWAP, TWAP, etc.)
     * @return CompletableFuture<Map<String, BigDecimal>> quality metrics
     */
    CompletableFuture<Map<String, BigDecimal>> calculateExecutionQuality(List<OrderExecution> executions, 
                                                                        String benchmark);
    
    /**
     * Generate execution performance report
     * 
     * @param userId The user ID
     * @param period Reporting period
     * @return CompletableFuture<ExecutionReport> performance report
     */
    CompletableFuture<ExecutionReport> generatePerformanceReport(Long userId, String period);
    
    // ========== Cross-Venue Arbitrage ==========
    
    /**
     * Detect arbitrage opportunities
     * 
     * @param symbol Trading symbol
     * @param venues List of venues to analyze
     * @return CompletableFuture<List<Map<String, Object>>> arbitrage opportunities
     */
    CompletableFuture<List<Map<String, Object>>> detectArbitrageOpportunities(String symbol, List<String> venues);
    
    /**
     * Execute cross-venue arbitrage
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param buyVenue Venue to buy from
     * @param sellVenue Venue to sell to
     * @param quantity Arbitrage quantity
     * @return CompletableFuture<List<OrderExecution>> arbitrage executions
     */
    CompletableFuture<List<OrderExecution>> executeCrossVenueArbitrage(Long userId, String symbol, 
                                                                       String buyVenue, String sellVenue, 
                                                                       Integer quantity);
    
    // ========== High-Frequency Trading Support ==========
    
    /**
     * Execute high-frequency trading strategy
     * 
     * @param userId The user ID
     * @param strategy HFT strategy parameters
     * @return CompletableFuture<List<OrderExecution>> HFT executions
     */
    CompletableFuture<List<OrderExecution>> executeHFTStrategy(Long userId, Map<String, Object> strategy);
    
    /**
     * Cancel and replace order with minimal latency
     * 
     * @param orderId Order to cancel and replace
     * @param newOrder Replacement order
     * @return CompletableFuture<OrderExecution> replace execution
     */
    CompletableFuture<OrderExecution> cancelReplace(Long orderId, Order newOrder);
    
    /**
     * Bulk cancel orders
     * 
     * @param orderIds List of order IDs to cancel
     * @return CompletableFuture<Map<Long, Boolean>> cancellation results
     */
    CompletableFuture<Map<Long, Boolean>> bulkCancelOrders(List<Long> orderIds);
    
    // ========== Risk Controls ==========
    
    /**
     * Apply pre-trade risk controls
     * 
     * @param userId The user ID
     * @param order Order to validate
     * @return CompletableFuture<Boolean> risk validation result
     */
    CompletableFuture<Boolean> applyPreTradeRiskControls(Long userId, Order order);
    
    /**
     * Monitor post-trade risk
     * 
     * @param userId The user ID
     * @param execution Completed execution
     * @return CompletableFuture<Void> monitoring completion
     */
    CompletableFuture<Void> monitorPostTradeRisk(Long userId, OrderExecution execution);
    
    /**
     * Emergency stop for user
     * 
     * @param userId The user ID
     * @param reason Stop reason
     * @return CompletableFuture<Boolean> stop execution result
     */
    CompletableFuture<Boolean> emergencyStop(Long userId, String reason);
    
    // ========== Performance Monitoring ==========
    
    /**
     * Get real-time execution performance metrics
     * 
     * @return CompletableFuture<Map<String, Object>> performance metrics
     */
    CompletableFuture<Map<String, Object>> getExecutionPerformanceMetrics();
    
    /**
     * Get venue performance comparison
     * 
     * @param symbol Trading symbol
     * @param timeHorizonHours Time horizon for comparison
     * @return CompletableFuture<Map<String, Map<String, Object>>> venue comparison
     */
    CompletableFuture<Map<String, Map<String, Object>>> getVenuePerformanceComparison(String symbol, 
                                                                                     Integer timeHorizonHours);
    
    /**
     * Optimize execution parameters
     * 
     * @param userId The user ID
     * @param historicalExecutions Historical execution data
     * @return CompletableFuture<Map<String, Object>> optimized parameters
     */
    CompletableFuture<Map<String, Object>> optimizeExecutionParameters(Long userId, 
                                                                       List<OrderExecution> historicalExecutions);
}