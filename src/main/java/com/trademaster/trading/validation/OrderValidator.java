package com.trademaster.trading.validation;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.entity.Order;

/**
 * Order Validator Interface
 * 
 * Defines the contract for order validation in the trading system using Java 24 Virtual Threads.
 * Implementations provide specific validation logic for different scenarios.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public interface OrderValidator {
    
    /**
     * Validate order request
     * 
     * @param orderRequest The order request to validate
     * @param userId The user placing the order
     * @return ValidationResult containing validation outcome
     */
    ValidationResult validate(OrderRequest orderRequest, Long userId);
    
    /**
     * Validate order modification
     * 
     * @param existingOrder The existing order being modified
     * @param modificationRequest The modification request
     * @param userId The user modifying the order
     * @return ValidationResult containing validation outcome
     */
    ValidationResult validateModification(Order existingOrder, OrderRequest modificationRequest, Long userId);
    
    /**
     * Get validator priority (lower number = higher priority)
     */
    int getPriority();
    
    /**
     * Get validator name for logging and debugging
     */
    String getValidatorName();
}