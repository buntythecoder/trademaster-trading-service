package com.trademaster.trading.agentos;

import java.util.List;

/**
 * AgentOS Component Interface for Trading Service
 * 
 * Defines the contract for TradeMaster Agent OS components that integrate
 * with the multi-agent orchestration framework. Components implementing
 * this interface can be registered, discovered, and coordinated by the
 * Agent Orchestration Service.
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
public interface AgentOSComponent {
    
    /**
     * Gets the unique identifier for this agent component
     */
    String getAgentId();
    
    /**
     * Gets the type classification of this agent
     */
    String getAgentType();
    
    /**
     * Gets the list of capabilities this agent provides
     */
    List<String> getCapabilities();
    
    /**
     * Gets the current health score of this agent (0.0 to 1.0)
     */
    Double getHealthScore();
    
    /**
     * Called when the agent is registered with the orchestration service
     */
    default void onRegistration() {
        // Default implementation - override if needed
    }
    
    /**
     * Called when the agent is deregistered from the orchestration service
     */
    default void onDeregistration() {
        // Default implementation - override if needed
    }
    
    /**
     * Called periodically to check agent health and update metrics
     */
    default void performHealthCheck() {
        // Default implementation - override if needed
    }
}