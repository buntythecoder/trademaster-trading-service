package com.trademaster.trading.validation;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validation Result
 * 
 * Encapsulates the outcome of order validation with details about
 * success/failure and any validation messages.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class ValidationResult {
    
    /**
     * Whether validation passed
     */
    private boolean valid;
    
    /**
     * Validation messages (warnings, errors, info)
     */
    @Builder.Default
    private List<ValidationMessage> messages = new ArrayList<>();
    
    /**
     * Validator that produced this result
     */
    private String validatorName;
    
    /**
     * Processing time in milliseconds
     */
    private long processingTimeMs;
    
    /**
     * Create successful validation result
     */
    public static ValidationResult success(String validatorName) {
        return ValidationResult.builder()
            .valid(true)
            .validatorName(validatorName)
            .build();
    }
    
    /**
     * Create failed validation result with error message
     */
    public static ValidationResult failure(String validatorName, String errorMessage) {
        return ValidationResult.builder()
            .valid(false)
            .validatorName(validatorName)
            .messages(List.of(ValidationMessage.error(errorMessage)))
            .build();
    }
    
    /**
     * Create failed validation result with multiple errors
     */
    public static ValidationResult failure(String validatorName, List<String> errorMessages) {
        List<ValidationMessage> messages = errorMessages.stream()
            .map(ValidationMessage::error)
            .toList();
            
        return ValidationResult.builder()
            .valid(false)
            .validatorName(validatorName)
            .messages(messages)
            .build();
    }
    
    /**
     * Create validation result with warnings (still valid)
     */
    public static ValidationResult warning(String validatorName, String warningMessage) {
        return ValidationResult.builder()
            .valid(true)
            .validatorName(validatorName)
            .messages(List.of(ValidationMessage.warning(warningMessage)))
            .build();
    }
    
    /**
     * Add validation message
     */
    public void addMessage(ValidationMessage message) {
        messages.add(message);
    }
    
    /**
     * Add error message and mark as invalid
     */
    public void addError(String message) {
        messages.add(ValidationMessage.error(message));
        valid = false;
    }
    
    /**
     * Add warning message (keeps valid status)
     */
    public void addWarning(String message) {
        messages.add(ValidationMessage.warning(message));
    }
    
    /**
     * Add info message
     */
    public void addInfo(String message) {
        messages.add(ValidationMessage.info(message));
    }
    
    /**
     * Check if result has any errors
     */
    public boolean hasErrors() {
        return messages.stream()
            .anyMatch(msg -> msg.getLevel() == ValidationLevel.ERROR);
    }
    
    /**
     * Check if result has any warnings
     */
    public boolean hasWarnings() {
        return messages.stream()
            .anyMatch(msg -> msg.getLevel() == ValidationLevel.WARNING);
    }
    
    /**
     * Get all error messages
     */
    public List<String> getErrorMessages() {
        return messages.stream()
            .filter(msg -> msg.getLevel() == ValidationLevel.ERROR)
            .map(ValidationMessage::getMessage)
            .toList();
    }
    
    /**
     * Get all warning messages
     */
    public List<String> getWarningMessages() {
        return messages.stream()
            .filter(msg -> msg.getLevel() == ValidationLevel.WARNING)
            .map(ValidationMessage::getMessage)
            .toList();
    }
    
    /**
     * Get consolidated error message for display - eliminates ternary with Optional
     */
    public String getConsolidatedErrorMessage() {
        List<String> errors = getErrorMessages();
        return Optional.of(errors)
            .filter(list -> !list.isEmpty())
            .map(list -> String.join("; ", list))
            .orElse("");
    }
    
    /**
     * Merge with another validation result
     */
    public ValidationResult merge(ValidationResult other) {
        List<ValidationMessage> allMessages = new ArrayList<>(this.messages);
        allMessages.addAll(other.messages);
        
        return ValidationResult.builder()
            .valid(this.valid && other.valid)
            .messages(allMessages)
            .validatorName(this.validatorName + "+" + other.validatorName)
            .processingTimeMs(this.processingTimeMs + other.processingTimeMs)
            .build();
    }
}