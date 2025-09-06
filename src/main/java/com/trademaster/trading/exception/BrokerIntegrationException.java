package com.trademaster.trading.exception;

/**
 * Broker Integration Exception
 * 
 * Exception thrown when broker integration operations fail.
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
public class BrokerIntegrationException extends RuntimeException {
    
    public BrokerIntegrationException(String message) {
        super(message);
    }
    
    public BrokerIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}