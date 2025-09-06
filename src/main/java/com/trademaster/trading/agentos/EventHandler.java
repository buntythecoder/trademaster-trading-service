package com.trademaster.trading.agentos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event Handler Annotation for Trading Service
 * 
 * Marks methods that handle specific events in the AgentOS framework.
 * The Agent Orchestration Service uses this annotation to route events
 * to the appropriate handler methods.
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    
    /**
     * The event type this handler processes
     */
    String event();
    
    /**
     * Priority level for this handler (higher values = higher priority)
     */
    int priority() default 0;
    
    /**
     * Whether this handler should be called asynchronously
     */
    boolean async() default true;
    
    /**
     * Maximum processing time in milliseconds before timeout
     */
    long timeoutMs() default 30000L;
}