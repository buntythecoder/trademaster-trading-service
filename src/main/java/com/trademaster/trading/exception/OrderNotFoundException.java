package com.trademaster.trading.exception;

/**
 * Order Not Found Exception
 * 
 * Exception thrown when a requested order cannot be found or user lacks access.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public class OrderNotFoundException extends RuntimeException {
    
    public OrderNotFoundException(String message) {
        super(message);
    }
    
    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}