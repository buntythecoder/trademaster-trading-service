package com.trademaster.trading.validation;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Validation Message
 * 
 * Represents a single validation message with level and content.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@AllArgsConstructor
public class ValidationMessage {
    
    private ValidationLevel level;
    private String message;
    private String field;
    
    public ValidationMessage(ValidationLevel level, String message) {
        this(level, message, null);
    }
    
    /**
     * Create error message
     */
    public static ValidationMessage error(String message) {
        return new ValidationMessage(ValidationLevel.ERROR, message);
    }
    
    /**
     * Create error message with field
     */
    public static ValidationMessage error(String message, String field) {
        return new ValidationMessage(ValidationLevel.ERROR, message, field);
    }
    
    /**
     * Create warning message
     */
    public static ValidationMessage warning(String message) {
        return new ValidationMessage(ValidationLevel.WARNING, message);
    }
    
    /**
     * Create warning message with field
     */
    public static ValidationMessage warning(String message, String field) {
        return new ValidationMessage(ValidationLevel.WARNING, message, field);
    }
    
    /**
     * Create info message
     */
    public static ValidationMessage info(String message) {
        return new ValidationMessage(ValidationLevel.INFO, message);
    }
    
    /**
     * Create info message with field
     */
    public static ValidationMessage info(String message, String field) {
        return new ValidationMessage(ValidationLevel.INFO, message, field);
    }
}