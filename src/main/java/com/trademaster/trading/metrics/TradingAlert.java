package com.trademaster.trading.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Optional;

/**
 * Trading Alert Data Model
 * 
 * Represents a trading system alert with all necessary context for
 * routing, processing, and escalation decisions.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingAlert {
    
    private AlertingService.AlertType alertType;
    private AlertingService.AlertSeverity severity;
    private String title;
    private String message;
    private String alertKey;
    private Instant timestamp;
    private String correlationId;
    
    // Additional context fields
    private String affectedService;
    private String brokerName;
    private String userId;
    private String orderId;
    private String symbol;
    
    // Alert resolution tracking
    private boolean resolved;
    private Instant resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;
    
    // Escalation tracking
    private boolean escalated;
    private Instant escalatedAt;
    private String escalatedTo;
    
    /**
     * Mark alert as resolved
     */
    public void resolve(String resolvedBy, String notes) {
        this.resolved = true;
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
    }
    
    /**
     * Mark alert as escalated
     */
    public void escalate(String escalatedTo) {
        this.escalated = true;
        this.escalatedAt = Instant.now();
        this.escalatedTo = escalatedTo;
    }
    
    /**
     * Get alert age in minutes
     */
    public long getAgeInMinutes() {
        return java.time.Duration.between(timestamp, Instant.now()).toMinutes();
    }
    
    /**
     * Check if alert requires escalation based on age and severity
     */
    public boolean requiresEscalation() {
        // Eliminates if-statement using Optional.of().filter()
        return Optional.of(escalated || resolved)
            .filter(alreadyHandled -> !alreadyHandled)
            .map(notHandled -> switch (severity) {
                case EMERGENCY -> getAgeInMinutes() > 5;  // 5 minutes
                case CRITICAL -> getAgeInMinutes() > 15;  // 15 minutes
                case WARNING -> getAgeInMinutes() > 60;   // 1 hour
                case INFO -> false;  // Info alerts don't escalate
            })
            .orElse(false);
    }
}