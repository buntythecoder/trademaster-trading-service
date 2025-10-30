package com.trademaster.trading.service.impl;

import com.trademaster.trading.entity.Order;
import com.trademaster.trading.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Trading Notification Service Implementation
 *
 * Sends order notifications via Kafka to notification-service for multi-channel delivery.
 *
 * MANDATORY COMPLIANCE:
 * - Rule #1: Java 24 Virtual Threads for async operations
 * - Rule #3: Functional programming patterns
 * - Rule #11: Result types for error handling
 * - Rule #15: Structured logging with correlation IDs
 * - Rule #25: Circuit breaker for Kafka operations
 *
 * Notification Flow:
 * 1. OrderService calls notification methods
 * 2. NotificationServiceImpl publishes to Kafka topic "trading.notifications"
 * 3. notification-service consumes Kafka messages
 * 4. notification-service delivers via WebSocket, Email, SMS, Push
 *
 * Performance Targets:
 * - Kafka publish: <50ms
 * - Non-blocking async: No impact on order processing
 * - Circuit breaker: Prevent cascading failures
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String KAFKA_TOPIC = "trading.notifications";
    private static final String CIRCUIT_BREAKER_NAME = "kafka-notifications";

    /**
     * Notify user that order has been placed
     *
     * MANDATORY: Rule #12 - Virtual Threads via @Async
     * MANDATORY: Rule #25 - Circuit Breaker for Kafka
     * MANDATORY: Rule #15 - Structured Logging
     */
    @Override
    @Async("notificationExecutor")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "notificationFallback")
    public void notifyOrderPlaced(Long userId, Order order) {
        String correlationId = generateCorrelationId();

        log.info("Sending order placed notification - userId: {}, orderId: {}, correlationId: {}",
                userId, order.getOrderId(), correlationId);

        TradingNotificationMessage message = createNotificationMessage(
            userId,
            "ORDER_PLACED",
            "Order Placed Successfully",
            String.format("Your %s order for %d shares of %s has been placed successfully",
                         order.getSide(), order.getQuantity(), order.getSymbol()),
            Map.of(
                "orderId", order.getOrderId(),
                "symbol", order.getSymbol(),
                "exchange", order.getExchange(),
                "orderType", order.getOrderType().toString(),
                "side", order.getSide().toString(),
                "quantity", order.getQuantity(),
                "limitPrice", order.getLimitPrice() != null ? order.getLimitPrice().toString() : "MARKET",
                "status", order.getStatus().toString(),
                "correlationId", correlationId
            )
        );

        publishToKafka(userId, message, correlationId);
    }

    /**
     * Notify user that order has been cancelled
     *
     * MANDATORY: Rule #12 - Virtual Threads via @Async
     * MANDATORY: Rule #25 - Circuit Breaker
     */
    @Override
    @Async("notificationExecutor")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "notificationFallback")
    public void notifyOrderCancelled(Long userId, Order order) {
        String correlationId = generateCorrelationId();

        log.info("Sending order cancelled notification - userId: {}, orderId: {}, correlationId: {}",
                userId, order.getOrderId(), correlationId);

        TradingNotificationMessage message = createNotificationMessage(
            userId,
            "ORDER_CANCELLED",
            "Order Cancelled",
            String.format("Your order for %s (%s) has been cancelled",
                         order.getSymbol(), order.getOrderId()),
            Map.of(
                "orderId", order.getOrderId(),
                "symbol", order.getSymbol(),
                "exchange", order.getExchange(),
                "quantity", order.getQuantity(),
                "filledQuantity", order.getFilledQuantity(),
                "remainingQuantity", order.getRemainingQuantity(),
                "status", order.getStatus().toString(),
                "correlationId", correlationId
            )
        );

        publishToKafka(userId, message, correlationId);
    }

    /**
     * Notify user of order fill
     *
     * MANDATORY: Rule #12 - Virtual Threads via @Async
     * MANDATORY: Rule #25 - Circuit Breaker
     */
    @Override
    @Async("notificationExecutor")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "notificationFallback")
    public void notifyOrderFilled(Long userId, Order order) {
        String correlationId = generateCorrelationId();

        log.info("Sending order filled notification - userId: {}, orderId: {}, correlationId: {}",
                userId, order.getOrderId(), correlationId);

        String fillStatus = order.isCompletelyFilled() ? "completely filled" : "partially filled";
        TradingNotificationMessage message = createNotificationMessage(
            userId,
            "ORDER_FILLED",
            "Order Executed Successfully",
            String.format("Your order for %s has been %s at avg price %s",
                         order.getSymbol(), fillStatus, order.getAvgFillPrice()),
            Map.ofEntries(
                Map.entry("orderId", order.getOrderId()),
                Map.entry("symbol", order.getSymbol()),
                Map.entry("exchange", order.getExchange()),
                Map.entry("side", order.getSide().toString()),
                Map.entry("quantity", order.getQuantity()),
                Map.entry("filledQuantity", order.getFilledQuantity()),
                Map.entry("avgFillPrice", order.getAvgFillPrice() != null ? order.getAvgFillPrice().toString() : "0"),
                Map.entry("totalFilledValue", order.getTotalFilledValue() != null ? order.getTotalFilledValue().toString() : "0"),
                Map.entry("status", order.getStatus().toString()),
                Map.entry("fillPercentage", String.format("%.2f%%", order.getFillPercentage())),
                Map.entry("correlationId", correlationId)
            )
        );

        publishToKafka(userId, message, correlationId);
    }

    /**
     * Notify user of order rejection
     *
     * MANDATORY: Rule #12 - Virtual Threads via @Async
     * MANDATORY: Rule #25 - Circuit Breaker
     */
    @Override
    @Async("notificationExecutor")
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "notificationFallback")
    public void notifyOrderRejected(Long userId, Order order, String reason) {
        String correlationId = generateCorrelationId();

        log.error("Sending order rejected notification - userId: {}, orderId: {}, reason: {}, correlationId: {}",
                userId, order.getOrderId(), reason, correlationId);

        TradingNotificationMessage message = createNotificationMessage(
            userId,
            "ORDER_REJECTED",
            "Order Rejected",
            String.format("Your order for %s was rejected. Reason: %s",
                         order.getSymbol(), reason),
            Map.of(
                "orderId", order.getOrderId(),
                "symbol", order.getSymbol(),
                "exchange", order.getExchange(),
                "orderType", order.getOrderType().toString(),
                "side", order.getSide().toString(),
                "quantity", order.getQuantity(),
                "rejectionReason", reason,
                "status", order.getStatus().toString(),
                "correlationId", correlationId
            )
        );

        publishToKafka(userId, message, correlationId);
    }

    /**
     * Publish notification to Kafka topic
     *
     * MANDATORY: Rule #3 - Functional Programming (CompletableFuture)
     * MANDATORY: Rule #11 - Error Handling with functional patterns
     */
    private void publishToKafka(Long userId, TradingNotificationMessage message, String correlationId) {
        CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(KAFKA_TOPIC, userId.toString(), message)
                    .thenAccept(result ->
                        log.debug("Notification published to Kafka - userId: {}, type: {}, correlationId: {}",
                                userId, message.type(), correlationId))
                    .exceptionally(throwable -> {
                        log.error("Failed to publish notification to Kafka - userId: {}, correlationId: {}, error: {}",
                                userId, correlationId, throwable.getMessage());
                        return null;
                    });
            } catch (Exception e) {
                log.error("Exception publishing to Kafka - userId: {}, correlationId: {}, error: {}",
                        userId, correlationId, e.getMessage());
            }
        });
    }

    /**
     * Circuit breaker fallback method
     *
     * MANDATORY: Rule #25 - Circuit Breaker Fallback
     */
    private void notificationFallback(Long userId, Order order, Exception ex) {
        log.warn("Notification circuit breaker opened - userId: {}, orderId: {}, error: {}",
                userId, order.getOrderId(), ex.getMessage());
        // Fallback: Store in database for retry or log for monitoring
    }

    /**
     * Fallback for rejection notifications
     */
    private void notificationFallback(Long userId, Order order, String reason, Exception ex) {
        log.warn("Notification circuit breaker opened (rejection) - userId: {}, orderId: {}, error: {}",
                userId, order.getOrderId(), ex.getMessage());
    }

    /**
     * Create notification message
     *
     * MANDATORY: Rule #9 - Immutable Records
     */
    private TradingNotificationMessage createNotificationMessage(
            Long userId,
            String type,
            String title,
            String content,
            Map<String, Object> data) {

        return new TradingNotificationMessage(
            UUID.randomUUID().toString(),
            userId,
            type,
            title,
            content,
            data,
            Instant.now()
        );
    }

    /**
     * Generate correlation ID for tracing
     *
     * MANDATORY: Rule #15 - Structured Logging
     */
    private String generateCorrelationId() {
        return "NOTIF-" + System.currentTimeMillis() + "-" +
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Trading Notification Message Record
     *
     * MANDATORY: Rule #9 - Immutable Records for DTOs
     *
     * This message is published to Kafka and consumed by notification-service
     */
    public record TradingNotificationMessage(
        String notificationId,
        Long userId,
        String type,  // ORDER_PLACED, ORDER_CANCELLED, ORDER_FILLED, ORDER_REJECTED
        String title,
        String content,
        Map<String, Object> data,
        Instant timestamp
    ) {}
}
