package com.trademaster.trading.dto;

import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Advanced Order Request DTO
 * 
 * Comprehensive order request supporting advanced execution strategies:
 * - Iceberg orders with display quantity management
 * - Bracket orders with profit target and stop loss
 * - OCO (One-Cancels-Other) order pairs
 * - TWAP/VWAP execution algorithms
 * - Implementation Shortfall strategy
 * - Smart order routing across multiple venues
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedOrderRequest {
    
    /**
     * Basic Order Parameters
     */
    @NotBlank(message = "Symbol is required")
    @Size(min = 1, max = 20, message = "Symbol must be 1-20 characters")
    private String symbol;
    
    @NotNull(message = "Order side is required")
    private OrderSide side;
    
    @NotNull(message = "Order type is required")
    private OrderType orderType;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    @Max(value = 100000, message = "Quantity cannot exceed 100,000 shares")
    private Integer quantity;
    
    @DecimalMin(value = "0.01", message = "Limit price must be at least 0.01")
    @DecimalMax(value = "999999.99", message = "Limit price cannot exceed 999,999.99")
    private BigDecimal limitPrice;
    
    @DecimalMin(value = "0.01", message = "Stop price must be at least 0.01")
    @DecimalMax(value = "999999.99", message = "Stop price cannot exceed 999,999.99")
    private BigDecimal stopPrice;
    
    @NotNull(message = "Time in force is required")
    private TimeInForce timeInForce;
    
    private LocalDate expiryDate;
    
    /**
     * Advanced Order Parameters
     */
    @NotBlank(message = "Execution strategy is required")
    @Pattern(regexp = "^(AGGRESSIVE|PATIENT|TWAP|VWAP|ICEBERG|BRACKET|OCO|IMPLEMENTATION_SHORTFALL)$", 
             message = "Invalid execution strategy")
    private String executionStrategy;
    
    /**
     * Iceberg Order Parameters
     */
    @Positive(message = "Display quantity must be positive")
    @Max(value = 10000, message = "Display quantity cannot exceed 10,000 shares")
    private Integer displayQuantity;
    
    @DecimalMin(value = "0.01", message = "Refresh rate must be at least 0.01")
    @DecimalMax(value = "1.0", message = "Refresh rate cannot exceed 1.0")
    private BigDecimal refreshRate; // Percentage of display quantity to refresh
    
    /**
     * Bracket Order Parameters
     */
    @DecimalMin(value = "0.01", message = "Profit target must be at least 0.01")
    private BigDecimal profitTarget;
    
    @DecimalMin(value = "0.01", message = "Stop loss must be at least 0.01")
    private BigDecimal stopLoss;
    
    /**
     * OCO Order Parameters
     */
    @DecimalMin(value = "0.01", message = "Second price must be at least 0.01")
    private BigDecimal secondPrice;
    
    /**
     * TWAP Strategy Parameters
     */
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 480, message = "Duration cannot exceed 480 minutes (8 hours)")
    private Integer durationMinutes;
    
    @Min(value = 1, message = "Number of slices must be at least 1")
    @Max(value = 100, message = "Number of slices cannot exceed 100")
    private Integer numberOfSlices;
    
    /**
     * VWAP Strategy Parameters
     */
    @DecimalMin(value = "0.01", message = "Participation rate must be at least 1%")
    @DecimalMax(value = "0.50", message = "Participation rate cannot exceed 50%")
    private BigDecimal participationRate;
    
    @Min(value = 1, message = "Look-back periods must be at least 1")
    @Max(value = 100, message = "Look-back periods cannot exceed 100")
    private Integer lookBackPeriods;
    
    /**
     * Implementation Shortfall Parameters
     */
    @DecimalMin(value = "0.0", message = "Risk aversion cannot be negative")
    @DecimalMax(value = "1.0", message = "Risk aversion cannot exceed 1.0")
    private BigDecimal riskAversion;
    
    @DecimalMin(value = "0.01", message = "Alpha must be at least 0.01")
    @DecimalMax(value = "0.99", message = "Alpha cannot exceed 0.99")
    private BigDecimal alpha; // Market impact parameter
    
    @DecimalMin(value = "0.001", message = "Sigma must be at least 0.001")
    @DecimalMax(value = "1.0", message = "Sigma cannot exceed 1.0")
    private BigDecimal sigma; // Volatility parameter
    
    /**
     * Smart Order Routing Parameters
     */
    @Pattern(regexp = "^(NSE|BSE|AUTO)$", message = "Invalid exchange specification")
    private String preferredExchange;
    
    @DecimalMin(value = "0.0", message = "Price tolerance cannot be negative")
    @DecimalMax(value = "0.10", message = "Price tolerance cannot exceed 10%")
    private BigDecimal priceTolerance;
    
    @DecimalMin(value = "0.0", message = "Latency tolerance cannot be negative")
    @DecimalMax(value = "1000.0", message = "Latency tolerance cannot exceed 1000ms")
    private BigDecimal latencyToleranceMs;
    
    /**
     * Risk and Compliance Parameters
     */
    @DecimalMin(value = "0.0", message = "Max position size cannot be negative")
    private BigDecimal maxPositionSize;
    
    @DecimalMin(value = "0.0", message = "Max order value cannot be negative")
    private BigDecimal maxOrderValue;
    
    private Boolean bypassRiskChecks;
    
    private String complianceTag;
    
    /**
     * Monitoring and Analytics Parameters
     */
    private Boolean enableRealTimeMonitoring;
    
    private Boolean generateExecutionReport;
    
    @Min(value = 1, message = "Monitoring interval must be at least 1 second")
    @Max(value = 300, message = "Monitoring interval cannot exceed 300 seconds")
    private Integer monitoringIntervalSeconds;
    
    /**
     * Additional Metadata
     */
    private Map<String, String> customParameters;
    
    private String clientOrderId;
    
    private String executionVenue;
    
    private String tradingSession;
    
    /**
     * Urgency and Priority
     */
    @DecimalMin(value = "0.0", message = "Urgency cannot be negative")
    @DecimalMax(value = "1.0", message = "Urgency cannot exceed 1.0")
    private BigDecimal urgency; // 0.0 = patient, 1.0 = aggressive
    
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority cannot exceed 10")
    private Integer priority; // 1 = highest priority, 10 = lowest
    
    /**
     * Conditional Order Parameters
     */
    private String triggerCondition; // Custom trigger conditions
    
    @DecimalMin(value = "0.01", message = "Trigger price must be at least 0.01")
    private BigDecimal triggerPrice;
    
    private String triggerSymbol; // Different symbol for trigger condition
    
    /**
     * Validation helper methods
     */
    public boolean isIcebergOrder() {
        return "ICEBERG".equals(executionStrategy) && displayQuantity != null && 
               displayQuantity > 0 && displayQuantity < quantity;
    }
    
    public boolean isBracketOrder() {
        return "BRACKET".equals(executionStrategy) && profitTarget != null && stopLoss != null;
    }
    
    public boolean isOCOOrder() {
        return "OCO".equals(executionStrategy) && secondPrice != null;
    }
    
    public boolean isTWAPOrder() {
        return "TWAP".equals(executionStrategy) && durationMinutes != null;
    }
    
    public boolean isVWAPOrder() {
        return "VWAP".equals(executionStrategy) && participationRate != null;
    }
    
    public boolean isImplementationShortfallOrder() {
        return "IMPLEMENTATION_SHORTFALL".equals(executionStrategy) && riskAversion != null;
    }
    
    public boolean requiresAdvancedExecution() {
        return !("AGGRESSIVE".equals(executionStrategy) || "PATIENT".equals(executionStrategy));
    }
    
    public boolean hasRiskParameters() {
        return maxPositionSize != null || maxOrderValue != null || 
               Boolean.TRUE.equals(bypassRiskChecks);
    }
    
    public boolean hasSmartRoutingParameters() {
        return priceTolerance != null || latencyToleranceMs != null || 
               "AUTO".equals(preferredExchange);
    }
    
    /**
     * Calculate estimated execution time based on strategy - eliminates all 3 ternaries with Optional
     */
    public Integer getEstimatedExecutionTimeMinutes() {
        return switch (executionStrategy) {
            case "AGGRESSIVE" -> 1;
            case "PATIENT" -> 30;
            case "TWAP" -> Optional.ofNullable(durationMinutes).orElse(60);
            case "VWAP" -> Optional.ofNullable(lookBackPeriods).map(periods -> periods * 5).orElse(120);
            case "ICEBERG" -> Optional.ofNullable(quantity)
                .flatMap(q -> Optional.ofNullable(displayQuantity)
                    .map(dq -> (q / dq) * 5))
                .orElse(60);
            case "BRACKET", "OCO" -> 15;
            case "IMPLEMENTATION_SHORTFALL" -> 45;
            default -> 30;
        };
    }
    
    /**
     * Get strategy complexity score (0.0 = simple, 1.0 = most complex)
     */
    public BigDecimal getComplexityScore() {
        return switch (executionStrategy) {
            case "AGGRESSIVE", "PATIENT" -> new BigDecimal("0.1");
            case "ICEBERG" -> new BigDecimal("0.4");
            case "BRACKET", "OCO" -> new BigDecimal("0.6");
            case "TWAP" -> new BigDecimal("0.7");
            case "VWAP" -> new BigDecimal("0.8");
            case "IMPLEMENTATION_SHORTFALL" -> new BigDecimal("0.9");
            default -> new BigDecimal("0.5");
        };
    }
}