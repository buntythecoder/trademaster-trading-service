package com.trademaster.trading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Risk Violation Model
 * 
 * Represents a risk violation detected during trading operations.
 * Used for risk alerts, notifications, and audit trails.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskViolation {
    
    /**
     * Violation Identification
     */
    private String violationId;
    private Long userId;
    private String orderId;
    private String symbol;
    private Instant detectedAt;
    
    /**
     * Violation Details
     */
    private String violationType; // POSITION_LIMIT, VAR_LIMIT, LEVERAGE_LIMIT, etc.
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String description;
    private String ruleViolated;
    
    /**
     * Violation Metrics
     */
    private BigDecimal currentValue;
    private BigDecimal limitValue;
    private BigDecimal excessAmount; // Amount by which limit is exceeded
    private BigDecimal utilizationPercent; // Utilization percentage
    
    /**
     * Context Information
     */
    private String marketCondition; // NORMAL, VOLATILE, STRESSED
    private String tradingSession; // PRE_MARKET, REGULAR, POST_MARKET
    private String assetClass; // EQUITY, DERIVATIVE, COMMODITY
    private String sector;
    private String exchange;
    
    /**
     * Risk Assessment
     */
    private BigDecimal riskScore; // 0.0 to 1.0
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private BigDecimal potentialLoss; // Potential loss if not addressed
    private String timeToResolve; // Expected time to resolve
    
    /**
     * Action Required
     */
    private String actionRequired; // BLOCK_ORDER, REDUCE_POSITION, INCREASE_MARGIN, etc.
    private String urgency; // IMMEDIATE, HIGH, MEDIUM, LOW
    private Boolean blocking; // Whether this violation blocks trading
    private String recommendation;
    
    /**
     * Resolution Information
     */
    private Boolean resolved;
    private Instant resolvedAt;
    private String resolutionAction;
    private String resolvedBy;
    private String resolutionNotes;
    
    /**
     * Notification Status
     */
    private Boolean notified;
    private Instant notifiedAt;
    private String notificationChannel; // EMAIL, SMS, PUSH, SYSTEM
    private Boolean acknowledged;
    private Instant acknowledgedAt;
    private String acknowledgedBy;
    
    /**
     * Additional Metadata
     */
    private String source; // REAL_TIME_MONITOR, PRE_TRADE_CHECK, POST_TRADE_ANALYSIS
    private String detectionMethod; // RULE_BASED, ML_MODEL, STRESS_TEST
    private BigDecimal confidenceLevel; // Confidence in violation detection
    
    /**
     * Constructor for simple violations
     */
    public RiskViolation(String violationType, String description) {
        this.violationType = violationType;
        this.description = description;
        this.detectedAt = Instant.now();
        this.violationId = generateViolationId();
        this.resolved = false;
        this.notified = false;
        this.acknowledged = false;
    }
    
    /**
     * Constructor with severity
     */
    public RiskViolation(String violationType, String description, String severity) {
        this(violationType, description);
        this.severity = severity;
        this.blocking = "CRITICAL".equals(severity) || "HIGH".equals(severity);
        this.urgency = mapSeverityToUrgency(severity);
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if violation is critical
     */
    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }
    
    /**
     * Check if violation blocks trading
     */
    public boolean isBlocking() {
        return blocking != null && blocking;
    }
    
    /**
     * Check if violation is resolved
     */
    public boolean isResolved() {
        return resolved != null && resolved;
    }
    
    /**
     * Check if violation requires immediate action
     */
    public boolean requiresImmediateAction() {
        return "IMMEDIATE".equals(urgency) || "HIGH".equals(urgency);
    }
    
    /**
     * Get excess percentage - eliminates if-statement with Optional
     */
    public BigDecimal getExcessPercent() {
        return Optional.ofNullable(currentValue)
            .flatMap(current -> Optional.ofNullable(limitValue)
                .filter(limit -> limit.compareTo(BigDecimal.ZERO) != 0)
                .map(limit -> current.subtract(limit)
                    .divide(limit, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))))
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Calculate age of violation - eliminates if-statement with Optional
     */
    public long getAgeMinutes() {
        return Optional.ofNullable(detectedAt)
            .map(detected -> java.time.Duration.between(detected, Instant.now()).toMinutes())
            .orElse(0L);
    }
    
    /**
     * Check if violation is stale (older than specified minutes)
     */
    public boolean isStale(int thresholdMinutes) {
        return getAgeMinutes() > thresholdMinutes;
    }
    
    /**
     * Mark violation as resolved
     */
    public void resolve(String action, String resolvedBy, String notes) {
        this.resolved = true;
        this.resolvedAt = Instant.now();
        this.resolutionAction = action;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
    }
    
    /**
     * Mark violation as notified
     */
    public void markNotified(String channel) {
        this.notified = true;
        this.notifiedAt = Instant.now();
        this.notificationChannel = channel;
    }
    
    /**
     * Mark violation as acknowledged
     */
    public void acknowledge(String acknowledgedBy) {
        this.acknowledged = true;
        this.acknowledgedAt = Instant.now();
        this.acknowledgedBy = acknowledgedBy;
    }
    
    /**
     * Get violation priority score (higher = more urgent) - eliminates if-else chain with Stream pattern
     */
    public int getPriorityScore() {
        // Severity contribution - eliminates if-else chain with Stream pattern
        record SeverityScore(String severity, int score) {}
        int severityScore = Stream.of(
                new SeverityScore("CRITICAL", 100),
                new SeverityScore("HIGH", 75),
                new SeverityScore("MEDIUM", 50),
                new SeverityScore("LOW", 25)
            )
            .filter(ss -> ss.severity().equals(severity))
            .findFirst()
            .map(SeverityScore::score)
            .orElse(0);

        // Risk score contribution - eliminates if-statement with Optional
        int riskContribution = Optional.ofNullable(riskScore)
            .map(rs -> rs.multiply(BigDecimal.valueOf(50)).intValue())
            .orElse(0);

        // Age penalty (older violations get higher priority)
        int ageScore = (int) Math.min(getAgeMinutes(), 60); // Max 60 points for age

        // Blocking violations get priority boost - eliminates if-statement with Optional
        int blockingBoost = Optional.of(isBlocking())
            .filter(blocking -> blocking)
            .map(blocking -> 50)
            .orElse(0);

        return severityScore + riskContribution + ageScore + blockingBoost;
    }
    
    /**
     * Get violation summary for logging - eliminates ternaries with Optional
     */
    public String getSummary() {
        return String.format("[%s] %s - %s (Current: %s, Limit: %s, Excess: %s%%)",
            severity,
            violationType,
            description,
            Optional.ofNullable(currentValue).map(BigDecimal::toString).orElse("N/A"),
            Optional.ofNullable(limitValue).map(BigDecimal::toString).orElse("N/A"),
            getExcessPercent().toString());
    }
    
    /**
     * Private helper methods
     */
    
    private String generateViolationId() {
        return "RV_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(this.hashCode()).toUpperCase();
    }
    
    private String mapSeverityToUrgency(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "IMMEDIATE";
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            case "LOW" -> "LOW";
            default -> "LOW";
        };
    }
    
    /**
     * Static factory methods
     */
    
    public static RiskViolation positionLimitViolation(Long userId, String symbol, 
                                                       BigDecimal currentPosition, BigDecimal limit) {
        return RiskViolation.builder()
            .userId(userId)
            .symbol(symbol)
            .violationType("POSITION_LIMIT")
            .severity("HIGH")
            .description("Position limit exceeded for " + symbol)
            .currentValue(currentPosition)
            .limitValue(limit)
            .blocking(true)
            .actionRequired("REDUCE_POSITION")
            .build();
    }
    
    public static RiskViolation varLimitViolation(Long userId, BigDecimal currentVaR, BigDecimal varLimit) {
        return RiskViolation.builder()
            .userId(userId)
            .violationType("VAR_LIMIT")
            .severity("HIGH")
            .description("Portfolio VaR limit exceeded")
            .currentValue(currentVaR)
            .limitValue(varLimit)
            .blocking(true)
            .actionRequired("REDUCE_RISK")
            .build();
    }
    
    public static RiskViolation leverageLimitViolation(Long userId, BigDecimal currentLeverage, 
                                                       BigDecimal leverageLimit) {
        return RiskViolation.builder()
            .userId(userId)
            .violationType("LEVERAGE_LIMIT")
            .severity("CRITICAL")
            .description("Leverage limit exceeded")
            .currentValue(currentLeverage)
            .limitValue(leverageLimit)
            .blocking(true)
            .actionRequired("INCREASE_MARGIN")
            .urgency("IMMEDIATE")
            .build();
    }
    
    public static RiskViolation buyingPowerViolation(Long userId, String orderId, 
                                                     BigDecimal required, BigDecimal available) {
        return RiskViolation.builder()
            .userId(userId)
            .orderId(orderId)
            .violationType("BUYING_POWER")
            .severity("HIGH")
            .description("Insufficient buying power")
            .currentValue(required)
            .limitValue(available)
            .blocking(true)
            .actionRequired("BLOCK_ORDER")
            .build();
    }
}