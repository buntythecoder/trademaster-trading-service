package com.trademaster.trading.config;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Trading Service Structured Logger
 * 
 * Provides trading-context aware logging methods for order management and execution.
 * All logs include correlation IDs and structured data for trading analysis.
 */
@Component
@Slf4j
public class TradingLogger {
    
    private static final String CORRELATION_ID = "correlationId";
    private static final String ORDER_ID = "orderId";
    private static final String SYMBOL = "symbol";
    private static final String STRATEGY = "strategy";
    private static final String ACCOUNT_ID = "accountId";
    private static final String BROKER = "broker";
    private static final String OPERATION = "operation";
    private static final String DURATION_MS = "durationMs";
    private static final String STATUS = "status";
    private static final String SIDE = "side";
    private static final String QUANTITY = "quantity";
    private static final String PRICE = "price";
    
    /**
     * Set correlation ID for the current thread context
     */
    public void setCorrelationId() {
        MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
    }
    
    /**
     * Set correlation ID with custom value
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
    }
    
    /**
     * Clear correlation ID from thread context
     */
    public void clearCorrelationId() {
        MDC.remove(CORRELATION_ID);
    }
    
    /**
     * Set trading context for all subsequent logs
     * Uses Optional patterns to eliminate if-statements
     */
    public void setTradingContext(String orderId, String symbol, String strategy, String accountId) {
        Optional.ofNullable(orderId).ifPresent(id -> MDC.put(ORDER_ID, id));
        Optional.ofNullable(symbol).ifPresent(sym -> MDC.put(SYMBOL, sym));
        Optional.ofNullable(strategy).ifPresent(strat -> MDC.put(STRATEGY, strat));
        Optional.ofNullable(accountId).ifPresent(acc -> MDC.put(ACCOUNT_ID, acc));
    }
    
    /**
     * Set broker context for external integrations
     * Uses Optional patterns to eliminate if-statements
     */
    public void setBrokerContext(String broker) {
        Optional.ofNullable(broker).ifPresent(b -> MDC.put(BROKER, b));
    }
    
    /**
     * Clear all context from thread
     */
    public void clearContext() {
        MDC.clear();
    }
    
    /**
     * Log order submission
     */
    public void logOrderSubmission(String orderId, String symbol, String side, double quantity,
                                  double price, String orderType, String strategy, long processingTimeMs) {
        log.info("Order submitted",
            StructuredArguments.kv(ORDER_ID, orderId),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv(SIDE, side),
            StructuredArguments.kv(QUANTITY, quantity),
            StructuredArguments.kv(PRICE, price),
            StructuredArguments.kv("orderType", orderType),
            StructuredArguments.kv(STRATEGY, strategy),
            StructuredArguments.kv(DURATION_MS, processingTimeMs),
            StructuredArguments.kv(OPERATION, "order_submission"),
            StructuredArguments.kv(STATUS, "submitted"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log order execution
     */
    public void logOrderExecution(String orderId, String symbol, String side, double quantity,
                                 double executedPrice, double slippageBps, long executionLatencyMs,
                                 boolean isPartialFill) {
        log.info("Order executed",
            StructuredArguments.kv(ORDER_ID, orderId),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv(SIDE, side),
            StructuredArguments.kv(QUANTITY, quantity),
            StructuredArguments.kv("executedPrice", executedPrice),
            StructuredArguments.kv("slippageBps", slippageBps),
            StructuredArguments.kv("executionLatency", executionLatencyMs),
            StructuredArguments.kv("isPartialFill", isPartialFill),
            StructuredArguments.kv("tradeValue", quantity * executedPrice),
            StructuredArguments.kv(OPERATION, "order_execution"),
            StructuredArguments.kv(STATUS, "executed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log order cancellation
     */
    public void logOrderCancellation(String orderId, String symbol, String reason,
                                    double remainingQuantity, long processingTimeMs) {
        log.info("Order cancelled",
            StructuredArguments.kv(ORDER_ID, orderId),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("reason", reason),
            StructuredArguments.kv("remainingQuantity", remainingQuantity),
            StructuredArguments.kv(DURATION_MS, processingTimeMs),
            StructuredArguments.kv(OPERATION, "order_cancellation"),
            StructuredArguments.kv(STATUS, "cancelled"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log order rejection
     */
    public void logOrderRejection(String orderId, String symbol, String reason, String errorCode,
                                 String broker, long processingTimeMs) {
        log.warn("Order rejected",
            StructuredArguments.kv(ORDER_ID, orderId),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("reason", reason),
            StructuredArguments.kv("errorCode", errorCode),
            StructuredArguments.kv(BROKER, broker),
            StructuredArguments.kv(DURATION_MS, processingTimeMs),
            StructuredArguments.kv(OPERATION, "order_rejection"),
            StructuredArguments.kv(STATUS, "rejected"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log portfolio update
     */
    public void logPortfolioUpdate(String accountId, String updateType, double totalValue,
                                  double unrealizedPnL, double realizedPnL, int positions,
                                  long calculationTimeMs) {
        log.info("Portfolio updated",
            StructuredArguments.kv(ACCOUNT_ID, accountId),
            StructuredArguments.kv("updateType", updateType),
            StructuredArguments.kv("totalValue", totalValue),
            StructuredArguments.kv("unrealizedPnL", unrealizedPnL),
            StructuredArguments.kv("realizedPnL", realizedPnL),
            StructuredArguments.kv("positions", positions),
            StructuredArguments.kv("calculationTime", calculationTimeMs),
            StructuredArguments.kv(OPERATION, "portfolio_update"),
            StructuredArguments.kv(STATUS, "updated"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log risk violation
     */
    public void logRiskViolation(String violationType, String symbol, String accountId,
                                String severity, double currentValue, double limit,
                                Map<String, Object> violationDetails) {
        var logBuilder = log.atWarn();
        
        logBuilder = logBuilder.addKeyValue("violationType", violationType)
            .addKeyValue(SYMBOL, symbol)
            .addKeyValue(ACCOUNT_ID, accountId)
            .addKeyValue("severity", severity)
            .addKeyValue("currentValue", currentValue)
            .addKeyValue("limit", limit)
            .addKeyValue("breachPercentage", ((currentValue - limit) / limit) * 100)
            .addKeyValue(OPERATION, "risk_violation")
            .addKeyValue(STATUS, "violation_detected")
            .addKeyValue("timestamp", Instant.now());
        
        var finalLogBuilder = Optional.ofNullable(violationDetails)
            .map(Map::entrySet)
            .orElse(Set.of())
            .stream()
            .reduce(logBuilder, 
                (builder, entry) -> builder.addKeyValue("risk_" + entry.getKey(), entry.getValue()),
                (b1, b2) -> b1);
        
        finalLogBuilder.log("Risk violation detected");
    }
    
    /**
     * Log strategy signal
     */
    public void logStrategySignal(String strategyName, String signal, String symbol,
                                 double confidence, Map<String, Object> signalData,
                                 long processingTimeMs) {
        var logBuilder = log.atInfo();
        
        logBuilder = logBuilder.addKeyValue(STRATEGY, strategyName)
            .addKeyValue("signal", signal)
            .addKeyValue(SYMBOL, symbol)
            .addKeyValue("confidence", confidence)
            .addKeyValue("processingTime", processingTimeMs)
            .addKeyValue(OPERATION, "strategy_signal")
            .addKeyValue(STATUS, "generated")
            .addKeyValue("timestamp", Instant.now());
        
        var finalSignalLogBuilder = Optional.ofNullable(signalData)
            .map(Map::entrySet)
            .orElse(Set.of())
            .stream()
            .reduce(logBuilder,
                (builder, entry) -> builder.addKeyValue("signal_" + entry.getKey(), entry.getValue()),
                (b1, b2) -> b1);
        
        finalSignalLogBuilder.log("Strategy signal generated");
    }
    
    /**
     * Log strategy execution
     * Uses Optional patterns to eliminate ternary operators
     */
    public void logStrategyExecution(String strategyName, String symbol, String action,
                                   boolean success, double pnl, long executionTimeMs) {
        String status = Optional.of(success)
            .filter(s -> s)
            .map(s -> "success")
            .orElse("failure");

        log.info("Strategy executed",
            StructuredArguments.kv(STRATEGY, strategyName),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("action", action),
            StructuredArguments.kv("pnl", pnl),
            StructuredArguments.kv("executionTime", executionTimeMs),
            StructuredArguments.kv(OPERATION, "strategy_execution"),
            StructuredArguments.kv(STATUS, status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log broker request
     * Uses Optional patterns to eliminate ternary operators
     */
    public void logBrokerRequest(String broker, String operation, String endpoint,
                                int statusCode, long responseTimeMs, boolean success) {
        String status = Optional.of(success)
            .filter(s -> s)
            .map(s -> "success")
            .orElse("failure");

        log.info("Broker request completed",
            StructuredArguments.kv(BROKER, broker),
            StructuredArguments.kv("brokerOperation", operation),
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("statusCode", statusCode),
            StructuredArguments.kv("responseTime", responseTimeMs),
            StructuredArguments.kv(OPERATION, "broker_request"),
            StructuredArguments.kv(STATUS, status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log market impact analysis
     */
    public void logMarketImpactAnalysis(String symbol, double orderSize, double marketImpactBps,
                                       String severity, long analysisTimeMs) {
        log.info("Market impact analyzed",
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("orderSize", orderSize),
            StructuredArguments.kv("marketImpactBps", marketImpactBps),
            StructuredArguments.kv("severity", severity),
            StructuredArguments.kv("analysisTime", analysisTimeMs),
            StructuredArguments.kv(OPERATION, "market_impact_analysis"),
            StructuredArguments.kv(STATUS, "analyzed"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log position update
     */
    public void logPositionUpdate(String symbol, String accountId, double quantity,
                                 double averagePrice, double unrealizedPnL, String updateReason) {
        log.info("Position updated",
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv(ACCOUNT_ID, accountId),
            StructuredArguments.kv(QUANTITY, quantity),
            StructuredArguments.kv("averagePrice", averagePrice),
            StructuredArguments.kv("unrealizedPnL", unrealizedPnL),
            StructuredArguments.kv("marketValue", quantity * averagePrice),
            StructuredArguments.kv("updateReason", updateReason),
            StructuredArguments.kv(OPERATION, "position_update"),
            StructuredArguments.kv(STATUS, "updated"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log margin call
     */
    public void logMarginCall(String accountId, double requiredMargin, double availableMargin,
                             String severity, Map<String, Object> marginDetails) {
        var logBuilder = log.atWarn();
        
        logBuilder = logBuilder.addKeyValue(ACCOUNT_ID, accountId)
            .addKeyValue("requiredMargin", requiredMargin)
            .addKeyValue("availableMargin", availableMargin)
            .addKeyValue("marginDeficit", requiredMargin - availableMargin)
            .addKeyValue("marginRatio", availableMargin / requiredMargin)
            .addKeyValue("severity", severity)
            .addKeyValue(OPERATION, "margin_call")
            .addKeyValue(STATUS, "margin_call_triggered")
            .addKeyValue("timestamp", Instant.now());
        
        var finalMarginLogBuilder = Optional.ofNullable(marginDetails)
            .map(Map::entrySet)
            .orElse(Set.of())
            .stream()
            .reduce(logBuilder,
                (builder, entry) -> builder.addKeyValue("margin_" + entry.getKey(), entry.getValue()),
                (b1, b2) -> b1);
        
        finalMarginLogBuilder.log("Margin call triggered");
    }
    
    /**
     * Log slippage event
     */
    public void logSlippageEvent(String orderId, String symbol, double expectedPrice,
                                double actualPrice, double slippageBps, String severity) {
        log.info("Slippage event recorded",
            StructuredArguments.kv(ORDER_ID, orderId),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv("expectedPrice", expectedPrice),
            StructuredArguments.kv("actualPrice", actualPrice),
            StructuredArguments.kv("slippageBps", slippageBps),
            StructuredArguments.kv("slippageAmount", actualPrice - expectedPrice),
            StructuredArguments.kv("severity", severity),
            StructuredArguments.kv(OPERATION, "slippage_analysis"),
            StructuredArguments.kv(STATUS, "recorded"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log API request
     * Uses Optional patterns to eliminate ternary operators
     */
    public void logApiRequest(String endpoint, String method, String accountId, int statusCode,
                             long durationMs, String userAgent) {
        String status = Optional.of(statusCode)
            .filter(code -> code < 400)
            .map(code -> "success")
            .orElse("error");

        log.info("API request processed",
            StructuredArguments.kv("endpoint", endpoint),
            StructuredArguments.kv("method", method),
            StructuredArguments.kv(ACCOUNT_ID, accountId),
            StructuredArguments.kv("statusCode", statusCode),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv("userAgent", sanitizeUserAgent(userAgent)),
            StructuredArguments.kv(OPERATION, "api_request"),
            StructuredArguments.kv(STATUS, status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log database operation
     * Uses Optional patterns to eliminate ternary operators
     */
    public void logDatabaseOperation(String operation, String table, int recordsAffected,
                                   long durationMs, boolean success) {
        String status = Optional.of(success)
            .filter(s -> s)
            .map(s -> "success")
            .orElse("failure");

        log.debug("Database operation completed",
            StructuredArguments.kv("dbOperation", operation),
            StructuredArguments.kv("table", table),
            StructuredArguments.kv("recordsAffected", recordsAffected),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "database_operation"),
            StructuredArguments.kv(STATUS, status),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log cache operation
     */
    public void logCacheOperation(String operation, String key, boolean hit, long durationMs) {
        log.debug("Cache operation completed",
            StructuredArguments.kv("cacheOperation", operation),
            StructuredArguments.kv("key", key),
            StructuredArguments.kv("hit", hit),
            StructuredArguments.kv(DURATION_MS, durationMs),
            StructuredArguments.kv(OPERATION, "cache_operation"),
            StructuredArguments.kv(STATUS, "success"),
            StructuredArguments.kv("timestamp", Instant.now())
        );
    }
    
    /**
     * Log audit event for compliance
     */
    public void logAuditEvent(String eventType, String accountId, String action, String resource,
                             String outcome, Map<String, Object> auditData) {
        var logBuilder = log.atInfo();
        
        logBuilder = logBuilder.addKeyValue("eventType", eventType)
            .addKeyValue(ACCOUNT_ID, accountId)
            .addKeyValue("action", action)
            .addKeyValue("resource", resource)
            .addKeyValue("outcome", outcome)
            .addKeyValue("timestamp", Instant.now())
            .addKeyValue("category", "audit");
        
        var finalAuditLogBuilder = Optional.ofNullable(auditData)
            .map(Map::entrySet)
            .orElse(Set.of())
            .stream()
            .reduce(logBuilder,
                (builder, entry) -> builder.addKeyValue("audit_" + entry.getKey(), entry.getValue()),
                (b1, b2) -> b1);
        
        finalAuditLogBuilder.log("Audit event recorded for compliance");
    }
    
    /**
     * Log error with context
     * Uses Optional patterns to eliminate ternary operators
     */
    public void logError(String operation, String errorType, String errorMessage,
                        String symbol, String orderId, Exception exception) {
        String exceptionClass = Optional.ofNullable(exception)
            .map(ex -> ex.getClass().getSimpleName())
            .orElse(null);

        log.error("Operation failed",
            StructuredArguments.kv(OPERATION, operation),
            StructuredArguments.kv("errorType", errorType),
            StructuredArguments.kv("errorMessage", errorMessage),
            StructuredArguments.kv(SYMBOL, symbol),
            StructuredArguments.kv(ORDER_ID, orderId),
            StructuredArguments.kv("exceptionClass", exceptionClass),
            StructuredArguments.kv(STATUS, "error"),
            StructuredArguments.kv("timestamp", Instant.now()),
            exception
        );
    }
    
    /**
     * Log performance metrics
     * Uses Optional patterns to eliminate ternary operators
     */
    public void logPerformanceMetrics(String operation, long durationMs, boolean success,
                                     Map<String, Object> additionalMetrics) {
        var logBuilder = log.atInfo();

        String status = Optional.of(success)
            .filter(s -> s)
            .map(s -> "success")
            .orElse("failure");

        logBuilder = logBuilder.addKeyValue(OPERATION, operation)
            .addKeyValue(DURATION_MS, durationMs)
            .addKeyValue(STATUS, status)
            .addKeyValue("timestamp", Instant.now())
            .addKeyValue("category", "performance");

        var finalMetricsLogBuilder = Optional.ofNullable(additionalMetrics)
            .map(Map::entrySet)
            .orElse(Set.of())
            .stream()
            .reduce(logBuilder,
                (builder, entry) -> builder.addKeyValue("metric_" + entry.getKey(), entry.getValue()),
                (b1, b2) -> b1);

        finalMetricsLogBuilder.log("Performance metrics recorded");
    }
    
    /**
     * Log with custom structured data
     */
    public void logWithStructuredData(String message, String logLevel, 
                                     Map<String, Object> structuredData) {
        var logBuilder = switch (logLevel.toUpperCase()) {
            case "ERROR" -> log.atError();
            case "WARN" -> log.atWarn();
            case "INFO" -> log.atInfo();
            case "DEBUG" -> log.atDebug();
            case "TRACE" -> log.atTrace();
            default -> log.atInfo();
        };
        
        logBuilder = logBuilder.addKeyValue("timestamp", Instant.now());
        
        var finalStructuredLogBuilder = structuredData.entrySet()
            .stream()
            .reduce(logBuilder,
                (builder, entry) -> builder.addKeyValue(entry.getKey(), entry.getValue()),
                (b1, b2) -> b1);
        
        finalStructuredLogBuilder.log(message);
    }
    
    /**
     * Utility Methods
     * Uses Optional patterns to eliminate if-statements and ternary operators
     */
    private String sanitizeUserAgent(String userAgent) {
        return Optional.ofNullable(userAgent)
            .filter(ua -> !ua.trim().isEmpty())
            .map(ua -> Optional.of(ua)
                .filter(agent -> agent.length() > 200)
                .map(agent -> agent.substring(0, 200))
                .orElse(ua))
            .orElse("unknown");
    }
}