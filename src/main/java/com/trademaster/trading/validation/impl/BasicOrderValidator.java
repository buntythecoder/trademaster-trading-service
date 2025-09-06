package com.trademaster.trading.validation.impl;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;
import com.trademaster.trading.validation.OrderValidator;
import com.trademaster.trading.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Basic Order Validator
 * 
 * Performs fundamental validation of order requests including:
 * - Required field validation
 * - Business rule compliance
 * - Price and quantity constraints
 * - Order type specific validation
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
public class BasicOrderValidator implements OrderValidator {
    
    @Override
    public ValidationResult validate(OrderRequest orderRequest, Long userId) {
        long startTime = System.currentTimeMillis();
        
        ValidationResult result = ValidationResult.builder()
            .valid(true)
            .validatorName(getValidatorName())
            .build();
        
        try {
            // Validate basic fields
            validateBasicFields(orderRequest, result);
            
            // Validate order type specific requirements
            validateOrderTypeRequirements(orderRequest, result);
            
            // Validate price relationships
            validatePriceRelationships(orderRequest, result);
            
            // Validate time in force requirements
            validateTimeInForceRequirements(orderRequest, result);
            
            // Validate quantity constraints
            validateQuantityConstraints(orderRequest, result);
            
            // Validate symbol format
            validateSymbolFormat(orderRequest, result);
            
        } catch (Exception e) {
            log.error("Error during basic validation for user {}: {}", userId, e.getMessage());
            result.addError("Validation error: " + e.getMessage());
        }
        
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        return result;
    }
    
    @Override
    public ValidationResult validateModification(Order existingOrder, OrderRequest modificationRequest, Long userId) {
        ValidationResult result = ValidationResult.builder()
            .valid(true)
            .validatorName(getValidatorName())
            .build();
        
        // Check if order can be modified
        if (!existingOrder.getStatus().isModifiable()) {
            result.addError("Order cannot be modified in status: " + existingOrder.getStatus());
            return result;
        }
        
        // Validate that core fields match
        if (!existingOrder.getSymbol().equals(modificationRequest.symbol())) {
            result.addError("Symbol cannot be changed during modification");
        }
        
        if (!existingOrder.getSide().equals(modificationRequest.side())) {
            result.addError("Order side cannot be changed during modification");
        }
        
        if (!existingOrder.getOrderType().equals(modificationRequest.orderType())) {
            result.addError("Order type cannot be changed during modification");
        }
        
        // Validate new quantity is not less than filled quantity
        if (modificationRequest.quantity() < existingOrder.getFilledQuantity()) {
            result.addError("New quantity cannot be less than already filled quantity: " + 
                          existingOrder.getFilledQuantity());
        }
        
        // Perform standard validation on the modification
        ValidationResult basicValidation = validate(modificationRequest, userId);
        if (basicValidation != null && !basicValidation.isValid()) {
            result = result.merge(basicValidation);
        }
        
        return result;
    }
    
    private void validateBasicFields(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.symbol() == null || orderRequest.symbol().trim().isEmpty()) {
            result.addError("Symbol is required");
        }
        
        if (orderRequest.exchange() == null || orderRequest.exchange().trim().isEmpty()) {
            result.addError("Exchange is required");
        }
        
        if (orderRequest.orderType() == null) {
            result.addError("Order type is required");
        }
        
        if (orderRequest.side() == null) {
            result.addError("Order side is required");
        }
        
        if (orderRequest.quantity() == null || orderRequest.quantity() <= 0) {
            result.addError("Quantity must be a positive number");
        }
        
        if (orderRequest.timeInForce() == null) {
            result.addError("Time in force is required");
        }
    }
    
    private void validateOrderTypeRequirements(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.orderType() == null) {
            return; // Already handled in basic validation
        }
        
        OrderType orderType = orderRequest.orderType();
        
        // Validate limit price requirements
        if (orderType.requiresLimitPrice() && orderRequest.limitPrice() == null) {
            result.addError("Limit price is required for " + orderType + " orders");
        }
        
        if (!orderType.requiresLimitPrice() && orderRequest.limitPrice() != null) {
            result.addWarning("Limit price is not used for " + orderType + " orders");
        }
        
        // Validate stop price requirements
        if (orderType.requiresStopPrice() && orderRequest.stopPrice() == null) {
            result.addError("Stop price is required for " + orderType + " orders");
        }
        
        if (!orderType.requiresStopPrice() && orderRequest.stopPrice() != null) {
            result.addWarning("Stop price is not used for " + orderType + " orders");
        }
        
        // Validate positive prices
        if (orderRequest.limitPrice() != null && orderRequest.limitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Limit price must be positive");
        }
        
        if (orderRequest.stopPrice() != null && orderRequest.stopPrice().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Stop price must be positive");
        }
    }
    
    private void validatePriceRelationships(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.orderType() != OrderType.STOP_LIMIT || 
            orderRequest.limitPrice() == null || 
            orderRequest.stopPrice() == null) {
            return;
        }
        
        // For stop-limit orders, validate price relationships
        switch (orderRequest.side()) {
            case BUY:
                // For buy stop-limit: stop price >= limit price (buying on upward momentum)
                if (orderRequest.stopPrice().compareTo(orderRequest.limitPrice()) < 0) {
                    result.addError("For buy stop-limit orders, stop price must be >= limit price");
                }
                break;
            case SELL:
                // For sell stop-limit: stop price <= limit price (selling on downward momentum)
                if (orderRequest.stopPrice().compareTo(orderRequest.limitPrice()) > 0) {
                    result.addError("For sell stop-limit orders, stop price must be <= limit price");
                }
                break;
        }
    }
    
    private void validateTimeInForceRequirements(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.timeInForce() == null) {
            return; // Already handled in basic validation
        }
        
        TimeInForce timeInForce = orderRequest.timeInForce();
        
        // Validate GTD expiry date requirement
        if (timeInForce == TimeInForce.GTD) {
            if (orderRequest.expiryDate() == null) {
                result.addError("Expiry date is required for Good Till Date orders");
            } else if (!orderRequest.expiryDate().isAfter(LocalDate.now())) {
                result.addError("Expiry date must be in the future");
            } else if (orderRequest.expiryDate().isAfter(LocalDate.now().plusDays(365))) {
                result.addWarning("Expiry date is more than 1 year in the future");
            }
        } else if (orderRequest.expiryDate() != null) {
            result.addWarning("Expiry date is only used for Good Till Date orders");
        }
        
        // Validate IOC/FOK with order types
        if ((timeInForce == TimeInForce.IOC || timeInForce == TimeInForce.FOK) && 
            orderRequest.orderType() != OrderType.MARKET && 
            orderRequest.orderType() != OrderType.LIMIT) {
            result.addWarning("IOC/FOK orders are typically used with Market or Limit order types");
        }
    }
    
    private void validateQuantityConstraints(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.quantity() == null) {
            return; // Already handled in basic validation
        }
        
        int quantity = orderRequest.quantity();
        
        // Maximum quantity constraints
        if (quantity > 1_000_000) {
            result.addError("Order quantity cannot exceed 1,000,000 shares");
        }
        
        // Minimum quantity constraints
        if (quantity < 1) {
            result.addError("Order quantity must be at least 1 share");
        }
        
        // Large order warning
        if (quantity > 100_000) {
            result.addWarning("Large order detected (" + quantity + " shares). Consider breaking into smaller orders.");
        }
    }
    
    private void validateSymbolFormat(OrderRequest orderRequest, ValidationResult result) {
        if (orderRequest.symbol() == null) {
            return; // Already handled in basic validation
        }
        
        String symbol = orderRequest.symbol().trim().toUpperCase();
        
        // Validate symbol format
        if (!symbol.matches("^[A-Z0-9_]{1,20}$")) {
            result.addError("Symbol must contain only uppercase letters, numbers, and underscores (max 20 characters)");
        }
        
        // Validate exchange-specific symbol formats
        if ("NSE".equals(orderRequest.exchange()) || "BSE".equals(orderRequest.exchange())) {
            if (symbol.length() > 20) {
                result.addError("Symbol too long for Indian exchanges (max 20 characters)");
            }
        }
        
        // Common symbol validation
        if (symbol.startsWith("_") || symbol.endsWith("_")) {
            result.addWarning("Symbol should not start or end with underscore");
        }
    }
    
    @Override
    public int getPriority() {
        return 1; // Highest priority - basic validation should run first
    }
    
    @Override
    public String getValidatorName() {
        return "BasicOrderValidator";
    }
}