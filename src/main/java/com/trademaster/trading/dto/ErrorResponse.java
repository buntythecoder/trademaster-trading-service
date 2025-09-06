package com.trademaster.trading.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Error Response DTO
 * 
 * Standardized error response for all trading API endpoints.
 * Provides detailed error information for proper client error handling.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    /**
     * Error code (e.g., VALIDATION_ERROR, RISK_ERROR, etc.)
     */
    String errorCode,
    
    /**
     * Human-readable error message
     */
    String message,
    
    /**
     * Detailed error description
     */
    String details,
    
    /**
     * List of validation errors (for field-level errors)
     */
    List<ValidationError> validationErrors,
    
    /**
     * Request path that caused the error
     */
    String path,
    
    /**
     * HTTP status code
     */
    int status,
    
    /**
     * Error timestamp
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Instant timestamp,
    
    /**
     * Correlation ID for tracking
     */
    String correlationId
) {
    
    /**
     * Create error response from TradeError
     */
    public static ErrorResponse fromTradeError(
            com.trademaster.trading.common.TradeError error, 
            String path, 
            int status, 
            String correlationId) {
        
        return new ErrorResponse(
            error.getCode(),
            error.getMessage(),
            null, // details can be added if needed
            null, // validation errors handled separately
            path,
            status,
            Instant.now(),
            correlationId
        );
    }
    
    /**
     * Create validation error response
     */
    public static ErrorResponse validationError(
            List<ValidationError> validationErrors,
            String path,
            String correlationId) {
        
        return new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            "One or more fields contain invalid values",
            validationErrors,
            path,
            400,
            Instant.now(),
            correlationId
        );
    }
    
    /**
     * Create generic error response
     */
    public static ErrorResponse genericError(
            String message,
            String path,
            int status,
            String correlationId) {
        
        return new ErrorResponse(
            "INTERNAL_ERROR",
            message,
            null,
            null,
            path,
            status,
            Instant.now(),
            correlationId
        );
    }
    
    /**
     * Validation error details
     */
    public record ValidationError(
        String field,
        String message,
        Object rejectedValue
    ) {}
}