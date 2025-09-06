package com.trademaster.trading.exception;

/**
 * Risk Check Exception
 * 
 * Exception thrown when an order fails pre-trade risk validation.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public class RiskCheckException extends RuntimeException {
    
    public RiskCheckException(String message) {
        super(message);
    }
    
    public RiskCheckException(String message, Throwable cause) {
        super(message, cause);
    }
}