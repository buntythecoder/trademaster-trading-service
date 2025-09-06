package com.trademaster.trading.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Trading Business Metrics Service
 * 
 * Comprehensive business metrics collection for trading operations.
 * Provides real-time monitoring of trading performance, risk metrics, and business KPIs.
 * 
 * Metrics Categories:
 * - Order Processing Metrics (volume, latency, success rate)
 * - Risk Management Metrics (exposure, violations, alerts)
 * - Financial Metrics (PnL, fees, portfolio value)
 * - Performance Metrics (throughput, error rates, SLA compliance)
 * - Business Metrics (user activity, broker distribution, market coverage)
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradingMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    // Order Processing Metrics
    private Counter ordersPlaced;
    private Counter ordersExecuted;
    private Counter ordersFailed;
    private Timer orderProcessingTime;
    private Timer riskCheckTime;
    
    // Risk Management Metrics
    private Counter riskViolations;
    private Counter riskAlerts;
    
    private final AtomicReference<BigDecimal> totalExposure = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> maxDailyLoss = new AtomicReference<>(BigDecimal.ZERO);
    
    // Financial Metrics
    private final AtomicReference<BigDecimal> totalPnL = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> dailyPnL = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> totalFees = new AtomicReference<>(BigDecimal.ZERO);
    
    // Performance Metrics
    private final AtomicLong activeOrders = new AtomicLong(0);
    private final AtomicLong activePositions = new AtomicLong(0);
    private final AtomicLong connectedUsers = new AtomicLong(0);
    
    // Business Metrics by Broker
    private final ConcurrentHashMap<String, AtomicLong> ordersByBroker = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicReference<BigDecimal>> volumeByBroker = new ConcurrentHashMap<>();
    
    // Circuit Breaker Metrics
    private Counter circuitBreakerTrips;
    
    // Initialize metrics after dependency injection
    @PostConstruct
    public void init() {
        initializeMetrics();
        registerGauges();
    }
    
    private void initializeMetrics() {
        ordersPlaced = Counter.builder("trading.orders.placed")
            .description("Total number of orders placed")
            .register(meterRegistry);
        
        ordersExecuted = Counter.builder("trading.orders.executed")
            .description("Total number of orders successfully executed")
            .register(meterRegistry);
        
        ordersFailed = Counter.builder("trading.orders.failed")
            .description("Total number of failed orders")
            .register(meterRegistry);
        
        orderProcessingTime = Timer.builder("trading.orders.processing_time")
            .description("Order processing latency")
            .register(meterRegistry);
        
        riskCheckTime = Timer.builder("trading.risk.check_time")
            .description("Risk assessment processing time")
            .register(meterRegistry);
        
        riskViolations = Counter.builder("trading.risk.violations")
            .description("Total risk rule violations")
            .register(meterRegistry);
        
        riskAlerts = Counter.builder("trading.risk.alerts")
            .description("Risk alerts triggered")
            .register(meterRegistry);
        
        circuitBreakerTrips = Counter.builder("trading.circuit_breaker.trips")
            .description("Circuit breaker activations")
            .register(meterRegistry);
    }
    
    private void registerGauges() {
        // Risk Exposure Gauges
        Gauge.builder("trading.risk.total_exposure", this, metrics -> metrics.totalExposure.get().doubleValue())
            .description("Current total market exposure")
            .register(meterRegistry);
        
        Gauge.builder("trading.risk.max_daily_loss", this, metrics -> metrics.maxDailyLoss.get().doubleValue())
            .description("Maximum daily loss threshold")
            .register(meterRegistry);
        
        // Financial Gauges
        Gauge.builder("trading.pnl.total", this, metrics -> metrics.totalPnL.get().doubleValue())
            .description("Total profit and loss")
            .register(meterRegistry);
        
        Gauge.builder("trading.pnl.daily", this, metrics -> metrics.dailyPnL.get().doubleValue())
            .description("Daily profit and loss")
            .register(meterRegistry);
        
        Gauge.builder("trading.fees.total", this, metrics -> metrics.totalFees.get().doubleValue())
            .description("Total trading fees")
            .register(meterRegistry);
        
        // Performance Gauges
        Gauge.builder("trading.orders.active", this, metrics -> metrics.activeOrders.get())
            .description("Number of active orders")
            .register(meterRegistry);
        
        Gauge.builder("trading.positions.active", this, metrics -> metrics.activePositions.get())
            .description("Number of active positions")
            .register(meterRegistry);
        
        Gauge.builder("trading.users.connected", this, metrics -> metrics.connectedUsers.get())
            .description("Number of connected users")
            .register(meterRegistry);
    }
    
    // Order Processing Metrics
    public void recordOrderPlaced(String brokerType, BigDecimal orderValue) {
        ordersPlaced.increment();
        recordOrderByBroker(brokerType, orderValue);
        log.debug("Order placed metric recorded for broker: {}", brokerType);
    }
    
    public void recordOrderExecuted(String brokerType, BigDecimal executedValue) {
        ordersExecuted.increment();
        updateVolumeByBroker(brokerType, executedValue);
        log.debug("Order executed metric recorded for broker: {}", brokerType);
    }
    
    public void recordOrderFailed(String brokerType, String errorType) {
        Counter.builder("trading.orders.failed")
            .description("Total number of failed orders")
            .tags(Tags.of(
                "broker", brokerType,
                "error_type", errorType
            ))
            .register(meterRegistry)
            .increment();
        log.debug("Order failed metric recorded: broker={}, error={}", brokerType, errorType);
    }
    
    public Timer.Sample startOrderProcessing() {
        return Timer.start(meterRegistry);
    }
    
    public void recordOrderProcessingTime(Timer.Sample sample) {
        sample.stop(orderProcessingTime);
    }
    
    // Risk Management Metrics
    public void recordRiskViolation(String violationType, String severity) {
        Counter.builder("trading.risk.violations")
            .description("Total risk rule violations")
            .tags(Tags.of(
                "violation_type", violationType,
                "severity", severity
            ))
            .register(meterRegistry)
            .increment();
        log.warn("Risk violation recorded: type={}, severity={}", violationType, severity);
    }
    
    public void recordRiskAlert(String alertType, String symbol) {
        Counter.builder("trading.risk.alerts")
            .description("Risk alerts triggered")
            .tags(Tags.of(
                "alert_type", alertType,
                "symbol", symbol
            ))
            .register(meterRegistry)
            .increment();
        log.info("Risk alert recorded: type={}, symbol={}", alertType, symbol);
    }
    
    public Timer.Sample startRiskCheck() {
        return Timer.start(meterRegistry);
    }
    
    public void recordRiskCheckTime(Timer.Sample sample) {
        sample.stop(riskCheckTime);
    }
    
    public void updateTotalExposure(BigDecimal exposure) {
        totalExposure.set(exposure);
    }
    
    public void updateMaxDailyLoss(BigDecimal maxLoss) {
        maxDailyLoss.set(maxLoss);
    }
    
    // Financial Metrics
    public void updateTotalPnL(BigDecimal pnl) {
        totalPnL.set(pnl);
    }
    
    public void updateDailyPnL(BigDecimal pnl) {
        dailyPnL.set(pnl);
    }
    
    public void addFees(BigDecimal fees) {
        totalFees.updateAndGet(current -> current.add(fees));
    }
    
    // Performance Metrics
    public void incrementActiveOrders() {
        activeOrders.incrementAndGet();
    }
    
    public void decrementActiveOrders() {
        activeOrders.decrementAndGet();
    }
    
    public void incrementActivePositions() {
        activePositions.incrementAndGet();
    }
    
    public void decrementActivePositions() {
        activePositions.decrementAndGet();
    }
    
    public void updateConnectedUsers(long count) {
        connectedUsers.set(count);
    }
    
    // Circuit Breaker Metrics
    public void recordCircuitBreakerTrip(String service, String reason) {
        Counter.builder("trading.circuit_breaker.trips")
            .description("Circuit breaker activations")
            .tags(Tags.of(
                "service", service,
                "reason", reason
            ))
            .register(meterRegistry)
            .increment();
        log.warn("Circuit breaker trip recorded: service={}, reason={}", service, reason);
    }
    
    // Business Metrics by Broker
    private void recordOrderByBroker(String brokerType, BigDecimal orderValue) {
        ordersByBroker.computeIfAbsent(brokerType, k -> {
            AtomicLong counter = new AtomicLong(0);
            // Register gauge for this broker
            Gauge.builder("trading.orders.by_broker", counter, AtomicLong::get)
                .description("Number of orders by broker")
                .tag("broker", brokerType)
                .register(meterRegistry);
            return counter;
        }).incrementAndGet();
        
        updateVolumeByBroker(brokerType, orderValue);
    }
    
    private void updateVolumeByBroker(String brokerType, BigDecimal volume) {
        volumeByBroker.computeIfAbsent(brokerType, k -> {
            AtomicReference<BigDecimal> volumeRef = new AtomicReference<>(BigDecimal.ZERO);
            // Register gauge for broker volume
            Gauge.builder("trading.volume.by_broker", volumeRef, ref -> ref.get().doubleValue())
                .description("Trading volume by broker")
                .tag("broker", brokerType)
                .register(meterRegistry);
            return volumeRef;
        }).updateAndGet(current -> current.add(volume));
    }
    
    // SLA Compliance Metrics
    public void recordSLAViolation(String slaType, long actualTime, long slaThreshold) {
        Counter.builder("trading.sla.violations")
            .description("SLA violations")
            .tag("sla_type", slaType)
            .register(meterRegistry)
            .increment();
        
        log.warn("SLA violation: type={}, actual={}ms, threshold={}ms", 
            slaType, actualTime, slaThreshold);
    }
    
    // Business Intelligence Metrics
    public void recordMarketDataLatency(String source, long latencyMs) {
        Timer.builder("trading.market_data.latency")
            .description("Market data feed latency")
            .tag("source", source)
            .register(meterRegistry)
            .record(java.time.Duration.ofMillis(latencyMs));
    }
    
    public void recordUserActivity(String activityType, String userId) {
        Counter.builder("trading.user.activity")
            .description("User activity tracking")
            .tag("activity_type", activityType)
            .register(meterRegistry)
            .increment();
        
        log.debug("User activity recorded: type={}, user={}", activityType, userId);
    }
    
    // Aggregated Business Metrics
    public BigDecimal getOrderSuccessRate() {
        double placed = ordersPlaced.count();
        double executed = ordersExecuted.count();
        return placed > 0 ? 
            BigDecimal.valueOf(executed / placed * 100) : 
            BigDecimal.ZERO;
    }
    
    public BigDecimal getAverageOrderProcessingTime() {
        return BigDecimal.valueOf(orderProcessingTime.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
    }
    
    public long getTotalActiveEntities() {
        return activeOrders.get() + activePositions.get();
    }
}