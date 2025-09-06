package com.trademaster.trading.service;

import com.trademaster.trading.dto.PortfolioSnapshot;
import com.trademaster.trading.dto.PositionSummary;
import com.trademaster.trading.dto.PnLReport;
import com.trademaster.trading.dto.RiskMetrics;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.entity.Position;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced Portfolio Service
 * 
 * Comprehensive portfolio and position management with real-time analytics:
 * - Real-time position tracking with WebSocket updates
 * - Advanced P&L calculations with attribution analysis
 * - Portfolio optimization and rebalancing recommendations
 * - Risk analytics with VaR, stress testing, and correlation analysis
 * - Performance benchmarking and attribution analysis
 * - Margin and cash management with lending integration
 * - Real-time portfolio streaming and alerts
 * 
 * Built with Java 24 Virtual Threads and Structured Concurrency for:
 * - Parallel position calculations across 1000+ holdings
 * - Concurrent risk metric computation (VaR, CVaR, Greeks)
 * - Real-time market data integration with sub-second latency
 * - Streaming portfolio updates via WebSocket
 * 
 * Performance Targets:
 * - Position updates: <3ms (cached with Redis)
 * - P&L calculations: <8ms (parallel computation)
 * - Portfolio queries: <12ms (pre-computed aggregates)
 * - Risk metrics: <25ms (concurrent calculation)
 * - Real-time streaming: <100ms end-to-end latency
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Structured Concurrency)
 */
public interface PortfolioService {
    
    // ========== Basic Position Management ==========
    
    /**
     * Update pending position when order is placed
     * 
     * @param userId The user ID
     * @param order The pending order
     */
    void updatePendingPosition(Long userId, Order order);
    
    /**
     * Remove pending position when order is cancelled
     * 
     * @param userId The user ID
     * @param order The cancelled order
     */
    void removePendingPosition(Long userId, Order order);
    
    /**
     * Update filled position when order is executed
     * 
     * @param userId The user ID
     * @param order The filled order
     * @param fillQuantity The fill quantity
     * @param fillPrice The fill price
     */
    void updateFilledPosition(Long userId, Order order, Integer fillQuantity, BigDecimal fillPrice);
    
    // ========== Position Queries and Management ==========
    
    /**
     * Get current positions for a user
     * 
     * @param userId The user ID
     * @return List<Position> current positions
     */
    CompletableFuture<List<Position>> getCurrentPositions(Long userId);
    
    /**
     * Get position for a specific symbol
     * 
     * @param userId The user ID
     * @param symbol The trading symbol
     * @return Position current position or null if no position
     */
    CompletableFuture<Position> getPosition(Long userId, String symbol);
    
    /**
     * Get positions with pagination and filtering
     * 
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @param filters Position filters
     * @return List<Position> filtered positions
     */
    CompletableFuture<List<Position>> getPositions(Long userId, Pageable pageable, Map<String, Object> filters);
    
    /**
     * Close position (market order to zero out position)
     * 
     * @param userId The user ID
     * @param symbol The trading symbol
     * @param reason Reason for closing position
     * @return CompletableFuture<Order> closing order
     */
    CompletableFuture<Order> closePosition(Long userId, String symbol, String reason);
    
    /**
     * Reduce position by specified quantity
     * 
     * @param userId The user ID
     * @param symbol The trading symbol
     * @param quantity Quantity to reduce
     * @param orderType Order type for reduction
     * @return CompletableFuture<Order> reduction order
     */
    CompletableFuture<Order> reducePosition(Long userId, String symbol, Integer quantity, String orderType);
    
    // ========== Portfolio Snapshots and Analytics ==========
    
    /**
     * Get real-time portfolio snapshot
     * 
     * @param userId The user ID
     * @return CompletableFuture<PortfolioSnapshot> current portfolio state
     */
    CompletableFuture<PortfolioSnapshot> getPortfolioSnapshot(Long userId);
    
    /**
     * Get historical portfolio snapshots
     * 
     * @param userId The user ID
     * @param fromDate Start date
     * @param toDate End date
     * @return CompletableFuture<List<PortfolioSnapshot>> historical snapshots
     */
    CompletableFuture<List<PortfolioSnapshot>> getHistoricalSnapshots(Long userId, LocalDate fromDate, LocalDate toDate);
    
    /**
     * Get position summary grouped by sector, industry, etc.
     * 
     * @param userId The user ID
     * @param groupBy Grouping criteria (SECTOR, INDUSTRY, ASSET_CLASS)
     * @return CompletableFuture<List<PositionSummary>> grouped position summaries
     */
    CompletableFuture<List<PositionSummary>> getPositionSummary(Long userId, String groupBy);
    
    // ========== P&L Calculations and Reporting ==========
    
    /**
     * Calculate real-time P&L for user's portfolio
     * 
     * @param userId The user ID
     * @return CompletableFuture<PnLReport> comprehensive P&L report
     */
    CompletableFuture<PnLReport> calculateRealTimePnL(Long userId);
    
    /**
     * Calculate P&L for specific time period
     * 
     * @param userId The user ID
     * @param fromDate Start date
     * @param toDate End date
     * @return CompletableFuture<PnLReport> period P&L report
     */
    CompletableFuture<PnLReport> calculatePeriodPnL(Long userId, LocalDate fromDate, LocalDate toDate);
    
    /**
     * Get P&L attribution analysis
     * 
     * @param userId The user ID
     * @param period Analysis period
     * @return CompletableFuture<Map<String, BigDecimal>> P&L attribution by source
     */
    CompletableFuture<Map<String, BigDecimal>> getPnLAttribution(Long userId, String period);
    
    /**
     * Calculate intraday P&L changes
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> intraday P&L by position
     */
    CompletableFuture<Map<String, BigDecimal>> getIntradayPnLChanges(Long userId);
    
    // ========== Risk Management and Analytics ==========
    
    /**
     * Calculate comprehensive risk metrics
     * 
     * @param userId The user ID
     * @return CompletableFuture<RiskMetrics> portfolio risk analysis
     */
    CompletableFuture<RiskMetrics> calculateRiskMetrics(Long userId);
    
    /**
     * Calculate Value at Risk (VaR) for portfolio
     * 
     * @param userId The user ID
     * @param confidenceLevel Confidence level (0.95, 0.99)
     * @param timeHorizonDays Time horizon in days
     * @return CompletableFuture<BigDecimal> VaR amount
     */
    CompletableFuture<BigDecimal> calculateVaR(Long userId, BigDecimal confidenceLevel, Integer timeHorizonDays);
    
    /**
     * Perform stress testing on portfolio
     * 
     * @param userId The user ID
     * @param stressScenarios Stress test scenarios
     * @return CompletableFuture<Map<String, BigDecimal>> stress test results
     */
    CompletableFuture<Map<String, BigDecimal>> performStressTests(Long userId, List<String> stressScenarios);
    
    /**
     * Calculate portfolio concentration risk
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> concentration metrics
     */
    CompletableFuture<Map<String, BigDecimal>> calculateConcentrationRisk(Long userId);
    
    /**
     * Calculate correlation matrix for portfolio holdings
     * 
     * @param userId The user ID
     * @param lookBackDays Historical period for correlation calculation
     * @return CompletableFuture<Map<String, Map<String, BigDecimal>>> correlation matrix
     */
    CompletableFuture<Map<String, Map<String, BigDecimal>>> calculateCorrelationMatrix(Long userId, Integer lookBackDays);
    
    // ========== Performance Analytics ==========
    
    /**
     * Calculate portfolio performance metrics (Sharpe, Sortino, etc.)
     * 
     * @param userId The user ID
     * @param benchmarkSymbol Benchmark for comparison
     * @param period Performance calculation period
     * @return CompletableFuture<Map<String, BigDecimal>> performance metrics
     */
    CompletableFuture<Map<String, BigDecimal>> calculatePerformanceMetrics(Long userId, String benchmarkSymbol, String period);
    
    /**
     * Calculate rolling returns for various periods
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> rolling returns (1D, 7D, 30D, YTD, 1Y)
     */
    CompletableFuture<Map<String, BigDecimal>> calculateRollingReturns(Long userId);
    
    /**
     * Calculate maximum drawdown and recovery statistics
     * 
     * @param userId The user ID
     * @param period Analysis period
     * @return CompletableFuture<Map<String, Object>> drawdown statistics
     */
    CompletableFuture<Map<String, Object>> calculateDrawdownStatistics(Long userId, String period);
    
    // ========== Cash and Margin Management ==========
    
    /**
     * Get current buying power and margin details
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> buying power breakdown
     */
    CompletableFuture<Map<String, BigDecimal>> getBuyingPower(Long userId);
    
    /**
     * Calculate margin requirements for potential position
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param quantity Position quantity
     * @param price Position price
     * @return CompletableFuture<BigDecimal> margin requirement
     */
    CompletableFuture<BigDecimal> calculateMarginRequirement(Long userId, String symbol, Integer quantity, BigDecimal price);
    
    /**
     * Get cash balance and available funds
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> cash position details
     */
    CompletableFuture<Map<String, BigDecimal>> getCashBalance(Long userId);
    
    // ========== Real-time Streaming and Notifications ==========
    
    /**
     * Start real-time portfolio streaming for user
     * 
     * @param userId The user ID
     * @param sessionId WebSocket session ID
     */
    void startPortfolioStreaming(Long userId, String sessionId);
    
    /**
     * Stop real-time portfolio streaming
     * 
     * @param userId The user ID
     * @param sessionId WebSocket session ID
     */
    void stopPortfolioStreaming(Long userId, String sessionId);
    
    /**
     * Send real-time portfolio update to connected clients
     * 
     * @param userId The user ID
     * @param updateType Type of update
     * @param updateData Update payload
     */
    void sendPortfolioUpdate(Long userId, String updateType, Map<String, Object> updateData);
    
    // ========== Portfolio Optimization and Rebalancing ==========
    
    /**
     * Generate portfolio rebalancing recommendations
     * 
     * @param userId The user ID
     * @param targetAllocation Target allocation percentages
     * @param rebalanceThreshold Minimum threshold for rebalancing
     * @return CompletableFuture<List<Order>> recommended rebalancing orders
     */
    CompletableFuture<List<Order>> generateRebalancingRecommendations(Long userId, Map<String, BigDecimal> targetAllocation, BigDecimal rebalanceThreshold);
    
    /**
     * Optimize portfolio allocation using Modern Portfolio Theory
     * 
     * @param userId The user ID
     * @param optimizationObjective Objective (MAX_RETURN, MIN_RISK, MAX_SHARPE)
     * @param constraints Portfolio constraints
     * @return CompletableFuture<Map<String, BigDecimal>> optimal allocation
     */
    CompletableFuture<Map<String, BigDecimal>> optimizePortfolioAllocation(Long userId, String optimizationObjective, Map<String, Object> constraints);
    
    /**
     * Execute portfolio rebalancing with optimal order sequencing
     * 
     * @param userId The user ID
     * @param rebalancingOrders List of rebalancing orders
     * @param executionStrategy Execution strategy for rebalancing
     * @return CompletableFuture<List<Order>> executed orders
     */
    CompletableFuture<List<Order>> executeRebalancing(Long userId, List<Order> rebalancingOrders, String executionStrategy);
    
    // ========== Reporting and Export ==========
    
    /**
     * Generate comprehensive portfolio report
     * 
     * @param userId The user ID
     * @param reportType Type of report (SUMMARY, DETAILED, PERFORMANCE, RISK)
     * @param period Report period
     * @return CompletableFuture<byte[]> PDF report
     */
    CompletableFuture<byte[]> generatePortfolioReport(Long userId, String reportType, String period);
    
    /**
     * Export portfolio data in various formats
     * 
     * @param userId The user ID
     * @param format Export format (CSV, JSON, EXCEL)
     * @param dataType Data to export (POSITIONS, TRANSACTIONS, PERFORMANCE)
     * @return CompletableFuture<byte[]> exported data
     */
    CompletableFuture<byte[]> exportPortfolioData(Long userId, String format, String dataType);
    
    // ========== Portfolio Alerts and Monitoring ==========
    
    /**
     * Set up portfolio alerts (P&L thresholds, risk limits, etc.)
     * 
     * @param userId The user ID
     * @param alertType Type of alert
     * @param threshold Alert threshold
     * @param notification Notification preferences
     */
    void setupPortfolioAlert(Long userId, String alertType, BigDecimal threshold, Map<String, Object> notification);
    
    /**
     * Check portfolio against risk limits and alerts
     * 
     * @param userId The user ID
     * @return CompletableFuture<List<String>> triggered alerts
     */
    CompletableFuture<List<String>> checkPortfolioAlerts(Long userId);
    
    /**
     * Get portfolio health score based on risk and performance metrics
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, Object>> health score and factors
     */
    CompletableFuture<Map<String, Object>> calculatePortfolioHealthScore(Long userId);
}