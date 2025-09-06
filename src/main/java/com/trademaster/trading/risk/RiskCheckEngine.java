package com.trademaster.trading.risk;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.entity.Order;

import java.util.concurrent.CompletableFuture;

/**
 * Risk Check Engine Interface
 * 
 * Defines the contract for pre-trade risk management using Java 24 Virtual Threads.
 * Implementations provide comprehensive risk assessment before order execution.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface RiskCheckEngine {
    
    /**
     * Perform comprehensive pre-trade risk check
     * 
     * @param orderRequest The order request to check
     * @param userId The user placing the order
     * @return RiskCheckResult containing risk assessment
     */
    RiskCheckResult performRiskCheck(OrderRequest orderRequest, Long userId);
    
    /**
     * Perform risk check for order modification
     * 
     * @param existingOrder The existing order
     * @param modificationRequest The modification request
     * @param userId The user modifying the order
     * @return RiskCheckResult containing risk assessment
     */
    RiskCheckResult performModificationRiskCheck(Order existingOrder, OrderRequest modificationRequest, Long userId);
    
    /**
     * Get real-time risk metrics for user (async with Virtual Threads)
     * 
     * @param userId The user ID
     * @return CompletableFuture<RiskMetrics> containing current risk metrics
     */
    CompletableFuture<RiskMetrics> getRiskMetrics(Long userId);
    
    /**
     * Check if user is approaching risk limits (async with Virtual Threads)
     * 
     * @param userId The user ID
     * @return CompletableFuture<Boolean> true if approaching limits
     */
    CompletableFuture<Boolean> isApproachingRiskLimits(Long userId);
}