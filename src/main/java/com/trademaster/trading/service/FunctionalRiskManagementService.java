package com.trademaster.trading.service;

import com.trademaster.trading.entity.Order;
import com.trademaster.trading.common.Result;
import com.trademaster.trading.common.TradeError;
import com.trademaster.trading.dto.PortfolioSnapshot;

import java.math.BigDecimal;

/**
 * Functional Risk Management Service with Result Types
 * 
 * Service interface for functional risk management operations using Result types
 * for error handling without exceptions. All methods return Result<T, TradeError>
 * for safe composition and railway programming patterns.
 * 
 * Key Risk Controls:
 * - Buying power validation (real-time account balance checks)
 * - Position limit enforcement (concentration risk management)  
 * - Daily trading limits (velocity controls and day trading rules)
 * - Margin requirements (leverage and collateral management)
 * - Real-time portfolio exposure monitoring
 * 
 * Performance Targets:
 * - Risk validation: <10ms (cached portfolio data)
 * - Position calculations: <5ms (in-memory aggregation)
 * - Margin checks: <15ms (real-time market data integration)
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Result Types)
 */
public interface FunctionalRiskManagementService {
    
    /**
     * Validate complete order risk profile
     * 
     * @param order The order to validate
     * @param userId The user ID
     * @return Result containing RiskValidationResult on success or TradeError on failure
     */
    Result<RiskValidationResult, TradeError> validateOrderRisk(Order order, Long userId);
    
    /**
     * Validate user has sufficient buying power for the order
     * 
     * @param userId The user ID  
     * @param order The order to validate
     * @return Result containing buying power validation or TradeError on failure
     */
    Result<BuyingPowerValidation, TradeError> validateBuyingPower(Long userId, Order order);
    
    /**
     * Validate order doesn't exceed position limits
     * 
     * @param userId The user ID
     * @param order The order to validate
     * @return Result containing position limit validation or TradeError on failure
     */
    Result<PositionLimitValidation, TradeError> validatePositionLimits(Long userId, Order order);
    
    /**
     * Validate order doesn't exceed daily trading limits
     * 
     * @param userId The user ID
     * @param order The order to validate
     * @return Result containing trading limit validation or TradeError on failure
     */
    Result<TradingLimitValidation, TradeError> validateTradingLimits(Long userId, Order order);
    
    /**
     * Calculate portfolio Value at Risk (VaR)
     * 
     * @param userId The user ID
     * @param confidenceLevel The confidence level (e.g., 0.95 for 95%)
     * @param timeHorizon Time horizon in days
     * @return Result containing VaR calculation or TradeError on failure
     */
    Result<BigDecimal, TradeError> calculatePortfolioVaR(Long userId, BigDecimal confidenceLevel, Integer timeHorizon);
    
    /**
     * Get current portfolio risk metrics
     * 
     * @param userId The user ID
     * @return Result containing portfolio risk metrics or TradeError on failure
     */
    Result<PortfolioRiskMetrics, TradeError> getPortfolioRiskMetrics(Long userId);
    
    /**
     * Calculate margin requirements for order
     * 
     * @param order The order to calculate margin for
     * @return Result containing margin requirement or TradeError on failure
     */
    Result<MarginRequirement, TradeError> calculateMarginRequirement(Order order);
    
    // Result data classes
    
    /**
     * Overall risk validation result
     */
    record RiskValidationResult(
        boolean isValid,
        String riskLevel,
        java.util.List<String> validationMessages,
        java.util.Map<String, Object> riskMetrics
    ) {
        public static RiskValidationResult valid() {
            return new RiskValidationResult(true, "LOW", java.util.List.of(), java.util.Map.of());
        }
        
        public static RiskValidationResult invalid(String riskLevel, java.util.List<String> messages) {
            return new RiskValidationResult(false, riskLevel, messages, java.util.Map.of());
        }
    }
    
    /**
     * Buying power validation result
     */
    record BuyingPowerValidation(
        boolean isValid,
        BigDecimal requiredAmount,
        BigDecimal availableAmount,
        BigDecimal marginUsed,
        BigDecimal buyingPower
    ) {}
    
    /**
     * Position limit validation result
     */
    record PositionLimitValidation(
        boolean isValid,
        Integer currentPosition,
        Integer positionLimit,
        BigDecimal concentrationPercent,
        BigDecimal maxConcentration
    ) {}
    
    /**
     * Trading limit validation result
     */
    record TradingLimitValidation(
        boolean isValid,
        Integer todayTradeCount,
        Integer maxDailyTrades,
        BigDecimal todayVolume,
        BigDecimal maxDailyVolume
    ) {}
    
    /**
     * Portfolio risk metrics
     */
    record PortfolioRiskMetrics(
        BigDecimal portfolioValue,
        BigDecimal portfolioVaR,
        BigDecimal expectedShortfall,
        BigDecimal portfolioBeta,
        BigDecimal portfolioVolatility,
        BigDecimal leverageRatio,
        BigDecimal concentrationRisk,
        java.util.Map<String, BigDecimal> sectorExposure
    ) {}
    
    /**
     * Margin requirement calculation
     */
    record MarginRequirement(
        BigDecimal initialMargin,
        BigDecimal maintenanceMargin,
        BigDecimal availableMargin,
        BigDecimal marginUtilization,
        boolean marginSufficient
    ) {}
}