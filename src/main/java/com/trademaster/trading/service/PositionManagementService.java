package com.trademaster.trading.service;

import com.trademaster.trading.dto.PositionSnapshot;
import com.trademaster.trading.dto.PositionAdjustment;
import com.trademaster.trading.dto.PositionRisk;
import com.trademaster.trading.entity.Position;
import com.trademaster.trading.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Position Management Service
 * 
 * Comprehensive position management with Java 24 Virtual Threads:
 * - Real-time position tracking and updates
 * - Multi-asset class position support (Equity, Derivatives, Commodities)
 * - Advanced P&L calculation with cost basis optimization
 * - Position lifecycle management (opening, adjustment, closing)
 * - Cross-margin and netting calculations
 * - Position risk analytics and monitoring
 * - Tax lot management and optimization
 * - Corporate action processing
 * 
 * Performance Targets:
 * - Position updates: <2ms (real-time streaming)
 * - P&L calculation: <1ms (cached with incremental updates)
 * - Position queries: <500 microseconds (in-memory cache)
 * - Risk calculations: <5ms (parallel computation)
 * - Bulk operations: 50,000+ positions/second
 * 
 * Features:
 * - Multi-currency position support
 * - FIFO, LIFO, Average Cost basis methods
 * - Wash sale tracking and tax optimization
 * - Position concentration monitoring
 * - Margin requirement calculations
 * - Corporate action adjustments
 * - Position reconciliation and settlement
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads + Real-time Analytics)
 */
public interface PositionManagementService {
    
    // ========== Core Position Operations ==========
    
    /**
     * Get current position for user and symbol
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @return CompletableFuture<Position> current position
     */
    CompletableFuture<Position> getPosition(Long userId, String symbol);
    
    /**
     * Get all positions for user
     * 
     * @param userId The user ID
     * @return CompletableFuture<List<Position>> all user positions
     */
    CompletableFuture<List<Position>> getAllPositions(Long userId);
    
    /**
     * Get positions by asset class
     * 
     * @param userId The user ID
     * @param assetClass Asset class filter (EQUITY, DERIVATIVE, COMMODITY, etc.)
     * @return CompletableFuture<List<Position>> filtered positions
     */
    CompletableFuture<List<Position>> getPositionsByAssetClass(Long userId, String assetClass);
    
    /**
     * Get positions by sector
     * 
     * @param userId The user ID
     * @param sector Sector filter
     * @return CompletableFuture<List<Position>> sector positions
     */
    CompletableFuture<List<Position>> getPositionsBySector(Long userId, String sector);
    
    /**
     * Update position from trade execution
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param quantity Trade quantity (positive for buy, negative for sell)
     * @param price Trade price
     * @param tradeTime Trade timestamp
     * @param tradeId Trade identifier
     * @return CompletableFuture<Position> updated position
     */
    CompletableFuture<Position> updatePositionFromTrade(Long userId, String symbol, 
                                                       Integer quantity, BigDecimal price, 
                                                       Instant tradeTime, String tradeId);
    
    /**
     * Close position (full or partial)
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param quantity Quantity to close (null for full close)
     * @param closePrice Close price
     * @param reason Close reason
     * @return CompletableFuture<Position> updated position
     */
    CompletableFuture<Position> closePosition(Long userId, String symbol, 
                                            Integer quantity, BigDecimal closePrice, String reason);
    
    // ========== Position Analytics and P&L ==========
    
    /**
     * Calculate real-time P&L for position
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param currentPrice Current market price
     * @return CompletableFuture<Map<String, BigDecimal>> P&L breakdown
     */
    CompletableFuture<Map<String, BigDecimal>> calculatePositionPnL(Long userId, String symbol, 
                                                                   BigDecimal currentPrice);
    
    /**
     * Calculate P&L for all positions
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, Map<String, BigDecimal>>> P&L by symbol
     */
    CompletableFuture<Map<String, Map<String, BigDecimal>>> calculateAllPositionsPnL(Long userId);
    
    /**
     * Get position snapshot with analytics
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @return CompletableFuture<PositionSnapshot> comprehensive position data
     */
    CompletableFuture<PositionSnapshot> getPositionSnapshot(Long userId, String symbol);
    
    
    /**
     * Calculate position risk metrics
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @return CompletableFuture<PositionRisk> position risk analysis
     */
    CompletableFuture<PositionRisk> calculatePositionRisk(Long userId, String symbol);
    
    // ========== Cost Basis Management ==========
    
    /**
     * Get cost basis information for position
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param method Cost basis method (FIFO, LIFO, AVERAGE, SPECIFIC)
     * @return CompletableFuture<Map<String, BigDecimal>> cost basis details
     */
    CompletableFuture<Map<String, BigDecimal>> getCostBasis(Long userId, String symbol, String method);
    
    /**
     * Update cost basis method for position
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param method New cost basis method
     * @return CompletableFuture<Void> update completion
     */
    CompletableFuture<Void> updateCostBasisMethod(Long userId, String symbol, String method);
    
    /**
     * Optimize cost basis for tax efficiency
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param sellQuantity Quantity to sell
     * @return CompletableFuture<Map<String, Object>> optimization recommendation
     */
    CompletableFuture<Map<String, Object>> optimizeCostBasisForTax(Long userId, String symbol, Integer sellQuantity);
    
    /**
     * Get tax lot breakdown for position
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @return CompletableFuture<List<Map<String, Object>>> tax lots
     */
    CompletableFuture<List<Map<String, Object>>> getTaxLotBreakdown(Long userId, String symbol);
    
    // ========== Position Adjustments ==========
    
    /**
     * Apply position adjustment (stock split, dividend, etc.)
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param adjustment Position adjustment details
     * @return CompletableFuture<Position> adjusted position
     */
    CompletableFuture<Position> applyPositionAdjustment(Long userId, String symbol, 
                                                       PositionAdjustment adjustment);
    
    /**
     * Process stock split adjustment
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param splitRatio Split ratio (e.g., 2.0 for 2:1 split)
     * @param exDate Ex-split date
     * @return CompletableFuture<Position> adjusted position
     */
    CompletableFuture<Position> processStockSplit(Long userId, String symbol, 
                                                 BigDecimal splitRatio, LocalDate exDate);
    
    /**
     * Process dividend payment
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param dividendPerShare Dividend per share
     * @param exDate Ex-dividend date
     * @param paymentDate Payment date
     * @return CompletableFuture<Map<String, Object>> dividend processing result
     */
    CompletableFuture<Map<String, Object>> processDividend(Long userId, String symbol, 
                                                          BigDecimal dividendPerShare, 
                                                          LocalDate exDate, LocalDate paymentDate);
    
    /**
     * Process spin-off adjustment
     * 
     * @param userId The user ID
     * @param originalSymbol Original symbol
     * @param newSymbol New symbol from spin-off
     * @param spinRatio Spin-off ratio
     * @param exDate Ex-spin date
     * @return CompletableFuture<List<Position>> adjusted positions
     */
    CompletableFuture<List<Position>> processSpinOff(Long userId, String originalSymbol, 
                                                     String newSymbol, BigDecimal spinRatio, LocalDate exDate);
    
    // ========== Position Monitoring and Alerts ==========
    
    /**
     * Monitor position for risk thresholds
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param thresholds Risk threshold configuration
     * @return CompletableFuture<List<String>> triggered alerts
     */
    CompletableFuture<List<String>> monitorPositionRisk(Long userId, String symbol, 
                                                       Map<String, BigDecimal> thresholds);
    
    /**
     * Set position alerts
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param alertType Alert type (PRICE, PNL, RISK, etc.)
     * @param threshold Alert threshold
     * @param condition Alert condition (ABOVE, BELOW, EQUAL)
     * @return CompletableFuture<String> alert ID
     */
    CompletableFuture<String> setPositionAlert(Long userId, String symbol, String alertType, 
                                              BigDecimal threshold, String condition);
    
    /**
     * Check position concentration limits
     * 
     * @param userId The user ID
     * @param maxConcentrationPercent Maximum concentration per position
     * @return CompletableFuture<List<String>> positions exceeding limits
     */
    CompletableFuture<List<String>> checkConcentrationLimits(Long userId, BigDecimal maxConcentrationPercent);
    
    // ========== Margin and Collateral ==========
    
    /**
     * Calculate margin requirement for positions
     * 
     * @param userId The user ID
     * @param positions Positions to analyze (null for all positions)
     * @return CompletableFuture<Map<String, BigDecimal>> margin requirements
     */
    CompletableFuture<Map<String, BigDecimal>> calculateMarginRequirement(Long userId, List<String> positions);
    
    /**
     * Calculate cross-margin benefits
     * 
     * @param userId The user ID
     * @return CompletableFuture<BigDecimal> cross-margin benefit amount
     */
    CompletableFuture<BigDecimal> calculateCrossMarginBenefit(Long userId);
    
    /**
     * Get collateral value of positions
     * 
     * @param userId The user ID
     * @param haircuts Haircut percentages by asset type
     * @return CompletableFuture<BigDecimal> total collateral value
     */
    CompletableFuture<BigDecimal> calculateCollateralValue(Long userId, Map<String, BigDecimal> haircuts);
    
    /**
     * Calculate excess margin available
     * 
     * @param userId The user ID
     * @return CompletableFuture<BigDecimal> excess margin amount
     */
    CompletableFuture<BigDecimal> calculateExcessMargin(Long userId);
    
    // ========== Position Reconciliation ==========
    
    /**
     * Reconcile positions with external systems
     * 
     * @param userId The user ID
     * @param externalPositions External position data
     * @return CompletableFuture<List<Map<String, Object>>> reconciliation results
     */
    CompletableFuture<List<Map<String, Object>>> reconcilePositions(Long userId, 
                                                                   Map<String, Position> externalPositions);
    
    /**
     * Generate position reconciliation report
     * 
     * @param userId The user ID
     * @param asOfDate As-of date for reconciliation
     * @return CompletableFuture<Map<String, Object>> reconciliation report
     */
    CompletableFuture<Map<String, Object>> generateReconciliationReport(Long userId, LocalDate asOfDate);
    
    /**
     * Fix position discrepancies
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param correctQuantity Correct quantity
     * @param reason Adjustment reason
     * @return CompletableFuture<Position> corrected position
     */
    CompletableFuture<Position> fixPositionDiscrepancy(Long userId, String symbol, 
                                                      Integer correctQuantity, String reason);
    
    // ========== Position History and Reporting ==========
    
    /**
     * Get position history
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param fromDate Start date
     * @param toDate End date
     * @return CompletableFuture<List<Map<String, Object>>> position history
     */
    CompletableFuture<List<Map<String, Object>>> getPositionHistory(Long userId, String symbol, 
                                                                   LocalDate fromDate, LocalDate toDate);
    
    /**
     * Generate position performance report
     * 
     * @param userId The user ID
     * @param symbol Trading symbol (null for all positions)
     * @param period Report period
     * @return CompletableFuture<Map<String, Object>> performance report
     */
    CompletableFuture<Map<String, Object>> generatePositionReport(Long userId, String symbol, String period);
    
    /**
     * Export positions to CSV/Excel
     * 
     * @param userId The user ID
     * @param format Export format (CSV, EXCEL)
     * @param includeHistory Include historical data
     * @return CompletableFuture<byte[]> exported data
     */
    CompletableFuture<byte[]> exportPositions(Long userId, String format, Boolean includeHistory);
    
    // ========== Multi-Currency Support ==========
    
    /**
     * Get positions in base currency
     * 
     * @param userId The user ID
     * @param baseCurrency Base currency for conversion
     * @return CompletableFuture<List<Position>> positions in base currency
     */
    CompletableFuture<List<Position>> getPositionsInBaseCurrency(Long userId, String baseCurrency);
    
    /**
     * Calculate currency exposure
     * 
     * @param userId The user ID
     * @return CompletableFuture<Map<String, BigDecimal>> exposure by currency
     */
    CompletableFuture<Map<String, BigDecimal>> calculateCurrencyExposure(Long userId);
    
    /**
     * Convert position value to different currency
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param targetCurrency Target currency
     * @param fxRate FX rate to use
     * @return CompletableFuture<BigDecimal> converted value
     */
    CompletableFuture<BigDecimal> convertPositionValue(Long userId, String symbol, 
                                                      String targetCurrency, BigDecimal fxRate);
    
    // ========== Advanced Analytics ==========
    
    /**
     * Calculate position Greek sensitivities (for derivatives)
     * 
     * @param userId The user ID
     * @param symbol Derivative symbol
     * @return CompletableFuture<Map<String, BigDecimal>> Greeks (Delta, Gamma, Theta, Vega, Rho)
     */
    CompletableFuture<Map<String, BigDecimal>> calculatePositionGreeks(Long userId, String symbol);
    
    /**
     * Analyze position correlation with market
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @param benchmarkSymbol Benchmark for correlation
     * @param lookbackDays Analysis period
     * @return CompletableFuture<Map<String, BigDecimal>> correlation metrics
     */
    CompletableFuture<Map<String, BigDecimal>> analyzePositionCorrelation(Long userId, String symbol, 
                                                                         String benchmarkSymbol, Integer lookbackDays);
    
    /**
     * Calculate position contribution to portfolio risk
     * 
     * @param userId The user ID
     * @param symbol Trading symbol
     * @return CompletableFuture<Map<String, BigDecimal>> risk contribution metrics
     */
    CompletableFuture<Map<String, BigDecimal>> calculateRiskContribution(Long userId, String symbol);
    
    /**
     * Generate position optimization recommendations
     * 
     * @param userId The user ID
     * @param optimizationGoal Optimization goal (RISK, RETURN, TAX, etc.)
     * @return CompletableFuture<List<Map<String, Object>>> optimization suggestions
     */
    CompletableFuture<List<Map<String, Object>>> getPositionOptimizationRecommendations(Long userId, 
                                                                                        String optimizationGoal);
    
    // ========== Bulk Operations ==========
    
    /**
     * Update multiple positions in batch
     * 
     * @param userId The user ID
     * @param updates List of position updates
     * @return CompletableFuture<List<Position>> updated positions
     */
    CompletableFuture<List<Position>> bulkUpdatePositions(Long userId, 
                                                         List<Map<String, Object>> updates);
    
    /**
     * Close multiple positions
     * 
     * @param userId The user ID
     * @param closeRequests List of close requests
     * @return CompletableFuture<List<Position>> closed positions
     */
    CompletableFuture<List<Position>> bulkClosePositions(Long userId, 
                                                        List<Map<String, Object>> closeRequests);
    
    /**
     * Rebalance positions to target allocation
     * 
     * @param userId The user ID
     * @param targetAllocation Target allocation percentages by symbol
     * @param rebalanceMethod Rebalancing method
     * @return CompletableFuture<List<Map<String, Object>>> rebalancing orders
     */
    CompletableFuture<List<Map<String, Object>>> rebalancePositions(Long userId, 
                                                                   Map<String, BigDecimal> targetAllocation, 
                                                                   String rebalanceMethod);
    
    // ========== Real-time Streaming ==========
    
    /**
     * Subscribe to position updates
     * 
     * @param userId The user ID
     * @param symbols Symbols to monitor (null for all)
     * @param callback Update callback
     */
    void subscribeToPositionUpdates(Long userId, List<String> symbols, 
                                   java.util.function.Consumer<Position> callback);
    
    /**
     * Subscribe to P&L updates
     * 
     * @param userId The user ID
     * @param callback P&L update callback
     */
    void subscribeToPnLUpdates(Long userId, 
                              java.util.function.Consumer<Map<String, BigDecimal>> callback);
    
    /**
     * Get real-time position stream
     * 
     * @param userId The user ID
     * @return Stream of position updates
     */
    java.util.stream.Stream<Position> getPositionStream(Long userId);
}