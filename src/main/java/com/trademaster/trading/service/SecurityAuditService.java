package com.trademaster.trading.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enterprise Security Audit Service
 * 
 * Comprehensive audit logging for security events with:
 * - Real-time threat detection
 * - Compliance reporting
 * - Automated alerting
 * - Performance metrics
 * 
 * @author TradeMaster Security Team
 * @version 2.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityAuditService {
    
    private final AuditEventRepository auditEventRepository;
    
    @Value("${security.audit.enabled:true}")
    private boolean auditEnabled;
    
    @Value("${security.audit.alert.threshold:10}")
    private int alertThreshold;
    
    // Security metrics tracking
    private final AtomicLong totalSecurityEvents = new AtomicLong(0);
    private final AtomicLong failedAuthenticationAttempts = new AtomicLong(0);
    private final AtomicLong suspiciousActivityDetected = new AtomicLong(0);
    private final AtomicLong accessDeniedEvents = new AtomicLong(0);
    
    // Rate tracking for threat detection
    private final Map<String, AtomicLong> ipRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> userFailureAttempts = new ConcurrentHashMap<>();
    
    /**
     * Log authentication success event - eliminates if-statement with Optional
     */
    public CompletableFuture<Void> logAuthenticationSuccess(String username, String clientIP, String userAgent) {
        return CompletableFuture.runAsync(() ->
            // Eliminates if-statement with Optional.filter()
            Optional.of(auditEnabled)
                .filter(Boolean::booleanValue)
                .ifPresent(enabled -> {
                    Map<String, Object> data = Map.of(
                        "username", username,
                        "clientIP", clientIP,
                        "userAgent", userAgent,
                        "timestamp", Instant.now(),
                        "success", true,
                        "sessionId", generateSessionId()
                    );

                    AuditEvent event = new AuditEvent(username, "AUTHENTICATION_SUCCESS", data);
                    auditEventRepository.add(event);

                    // Reset failure counter for user
                    userFailureAttempts.put(username, new AtomicLong(0));

                    log.info("Authentication successful - User: {} IP: {}", username, clientIP);
                    totalSecurityEvents.incrementAndGet();
                })
        );
    }
    
    /**
     * Log authentication failure with threat detection - eliminates all if-statements with Optional
     */
    public CompletableFuture<Void> logAuthenticationFailure(String username, String clientIP,
                                                          String userAgent, String reason) {
        return CompletableFuture.runAsync(() ->
            // Eliminates audit check with Optional.filter()
            Optional.of(auditEnabled)
                .filter(Boolean::booleanValue)
                .ifPresent(enabled -> {
                    Map<String, Object> data = Map.of(
                        "username", username,
                        "clientIP", clientIP,
                        "userAgent", userAgent,
                        "timestamp", Instant.now(),
                        "success", false,
                        "reason", reason,
                        "sessionId", generateSessionId()
                    );

                    AuditEvent event = new AuditEvent(username, "AUTHENTICATION_FAILURE", data);
                    auditEventRepository.add(event);

                    // Track failure attempts for threat detection
                    long userFailures = userFailureAttempts.computeIfAbsent(username,
                        k -> new AtomicLong(0)).incrementAndGet();
                    long ipRequests = ipRequestCounts.computeIfAbsent(clientIP,
                        k -> new AtomicLong(0)).incrementAndGet();

                    // Threat detection - eliminates if-statements with Optional.filter()
                    Optional.of(userFailures)
                        .filter(failures -> failures >= alertThreshold)
                        .ifPresent(failures -> logSuspiciousActivity("MULTIPLE_FAILED_LOGINS", username, clientIP,
                            "User exceeded failed login threshold: " + failures));

                    Optional.of(ipRequests)
                        .filter(requests -> requests >= alertThreshold * 2)
                        .ifPresent(requests -> logSuspiciousActivity("IP_BRUTE_FORCE", username, clientIP,
                            "IP exceeded request threshold: " + requests));

                    log.warn("Authentication failed - User: {} IP: {} Reason: {} Attempts: {}",
                        username, clientIP, reason, userFailures);

                    failedAuthenticationAttempts.incrementAndGet();
                    totalSecurityEvents.incrementAndGet();
                })
        );
    }
    
    /**
     * Log access denied events - eliminates if-statement with Optional
     */
    public CompletableFuture<Void> logAccessDenied(String username, String resource,
                                                 String action, String clientIP) {
        return CompletableFuture.runAsync(() ->
            Optional.of(auditEnabled)
                .filter(Boolean::booleanValue)
                .ifPresent(enabled -> {
                    Map<String, Object> data = Map.of(
                        "username", username,
                        "resource", resource,
                        "action", action,
                        "clientIP", clientIP,
                        "timestamp", Instant.now(),
                        "eventType", "ACCESS_DENIED"
                    );

                    AuditEvent event = new AuditEvent(username, "ACCESS_DENIED", data);
                    auditEventRepository.add(event);

                    log.warn("Access denied - User: {} Resource: {} Action: {} IP: {}",
                        username, resource, action, clientIP);

                    accessDeniedEvents.incrementAndGet();
                    totalSecurityEvents.incrementAndGet();
                })
        );
    }
    
    /**
     * Log privileged operations - eliminates if-statement with Optional
     */
    public CompletableFuture<Void> logPrivilegedOperation(String username, String operation,
                                                        String details, String clientIP) {
        return CompletableFuture.runAsync(() ->
            Optional.of(auditEnabled)
                .filter(Boolean::booleanValue)
                .ifPresent(enabled -> {
                    Map<String, Object> data = Map.of(
                        "username", username,
                        "operation", operation,
                        "details", details,
                        "clientIP", clientIP,
                        "timestamp", Instant.now(),
                        "privileged", true
                    );

                    AuditEvent event = new AuditEvent(username, "PRIVILEGED_OPERATION", data);
                    auditEventRepository.add(event);

                    log.info("Privileged operation - User: {} Operation: {} IP: {}",
                        username, operation, clientIP);

                    totalSecurityEvents.incrementAndGet();
                })
        );
    }
    
    /**
     * Log suspicious activity with automated alerting - eliminates if-statement with Optional
     */
    public CompletableFuture<Void> logSuspiciousActivity(String threatType, String username,
                                                       String clientIP, String description) {
        return CompletableFuture.runAsync(() ->
            Optional.of(auditEnabled)
                .filter(Boolean::booleanValue)
                .ifPresent(enabled -> {
                    Map<String, Object> data = Map.of(
                        "threatType", threatType,
                        "username", username,
                        "clientIP", clientIP,
                        "description", description,
                        "timestamp", Instant.now(),
                        "severity", "HIGH",
                        "automated", true
                    );

                    AuditEvent event = new AuditEvent(username, "SUSPICIOUS_ACTIVITY", data);
                    auditEventRepository.add(event);

                    // Critical alert logging
                    log.error("SECURITY ALERT - Threat: {} User: {} IP: {} Description: {}",
                        threatType, username, clientIP, description);

                    suspiciousActivityDetected.incrementAndGet();
                    totalSecurityEvents.incrementAndGet();

                    // In production, this would trigger alerting system
                    // alertingService.sendSecurityAlert(threatType, data);
                })
        );
    }
    
    /**
     * Log data access events for compliance - eliminates if-statement with Optional
     */
    public CompletableFuture<Void> logDataAccess(String username, String dataType,
                                               String operation, String recordId) {
        return CompletableFuture.runAsync(() ->
            Optional.of(auditEnabled)
                .filter(Boolean::booleanValue)
                .ifPresent(enabled -> {
                    Map<String, Object> data = Map.of(
                        "username", username,
                        "dataType", dataType,
                        "operation", operation,
                        "recordId", recordId,
                        "timestamp", Instant.now(),
                        "compliance", true
                    );

                    AuditEvent event = new AuditEvent(username, "DATA_ACCESS", data);
                    auditEventRepository.add(event);

                    log.debug("Data access - User: {} Type: {} Operation: {} ID: {}",
                        username, dataType, operation, recordId);

                    totalSecurityEvents.incrementAndGet();
                })
        );
    }
    
    /**
     * Get security metrics for monitoring
     */
    public Map<String, Long> getSecurityMetrics() {
        return Map.of(
            "totalSecurityEvents", totalSecurityEvents.get(),
            "failedAuthenticationAttempts", failedAuthenticationAttempts.get(),
            "suspiciousActivityDetected", suspiciousActivityDetected.get(),
            "accessDeniedEvents", accessDeniedEvents.get(),
            "uniqueIPs", (long) ipRequestCounts.size(),
            "usersWithFailures", (long) userFailureAttempts.size()
        );
    }
    
    /**
     * Reset security counters (for scheduled cleanup)
     */
    public void resetSecurityCounters() {
        ipRequestCounts.clear();
        userFailureAttempts.clear();
        log.info("Security counters reset for new monitoring period");
    }
    
    /**
     * Generate unique session ID for correlation
     */
    private String generateSessionId() {
        return "TM-" + System.currentTimeMillis() + "-" + 
               Thread.currentThread().getId();
    }
    
    /**
     * Check if IP is showing suspicious behavior
     */
    public boolean isSuspiciousIP(String clientIP) {
        return ipRequestCounts.getOrDefault(clientIP, new AtomicLong(0))
                             .get() >= alertThreshold;
    }
    
    /**
     * Check if user is showing suspicious behavior
     */
    public boolean isSuspiciousUser(String username) {
        return userFailureAttempts.getOrDefault(username, new AtomicLong(0))
                                 .get() >= alertThreshold;
    }
}