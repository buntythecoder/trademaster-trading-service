package com.trademaster.trading.security;

import lombok.Builder;
import lombok.Data;

import java.security.Principal;
import java.util.List;

/**
 * Trading User Principal
 * 
 * Represents an authenticated user in the trading system
 * with relevant user information and permissions.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
public class TradingUserPrincipal implements Principal {
    
    /**
     * User ID from authentication service
     */
    private Long userId;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * User email
     */
    private String email;
    
    /**
     * User roles
     */
    private List<String> roles;
    
    /**
     * Subscription tier (BASIC, PREMIUM, PROFESSIONAL)
     */
    private String subscriptionTier;
    
    /**
     * Whether user is pattern day trader
     */
    @Builder.Default
    private boolean patternDayTrader = false;
    
    /**
     * User's preferred broker
     */
    private String preferredBroker;
    
    @Override
    public String getName() {
        return username;
    }
    
    /**
     * Check if user has specific role
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    /**
     * Check if user is premium subscriber
     */
    public boolean isPremiumUser() {
        return "PREMIUM".equals(subscriptionTier) || "PROFESSIONAL".equals(subscriptionTier);
    }
    
    /**
     * Check if user is professional subscriber
     */
    public boolean isProfessionalUser() {
        return "PROFESSIONAL".equals(subscriptionTier);
    }
    
    /**
     * Get user display name
     */
    public String getDisplayName() {
        return username != null ? username : email;
    }
}