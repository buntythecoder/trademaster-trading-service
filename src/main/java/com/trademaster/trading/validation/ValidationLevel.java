package com.trademaster.trading.validation;

/**
 * Validation Level Enumeration
 * 
 * Defines the severity levels for validation messages.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
public enum ValidationLevel {
    /**
     * Error - Order cannot be processed
     */
    ERROR,
    
    /**
     * Warning - Order can be processed but with caution
     */
    WARNING,
    
    /**
     * Info - Informational message
     */
    INFO
}