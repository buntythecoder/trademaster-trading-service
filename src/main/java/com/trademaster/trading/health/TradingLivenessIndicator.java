package com.trademaster.trading.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.Optional;

/**
 * Trading Liveness Health Indicator
 * 
 * Kubernetes liveness probe implementation for trading service.
 * Checks internal application state that indicates if the service needs to be restarted.
 * 
 * IMPORTANT: This indicator MUST NOT check external dependencies to avoid cascading failures.
 * 
 * Liveness Checks:
 * - JVM memory usage and garbage collection
 * - Thread pool health and deadlock detection
 * - Virtual threads performance
 * - Application internal state
 * 
 * This indicator determines if Kubernetes should restart the pod.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component("tradingLivenessIndicator")
@Slf4j
public class TradingLivenessIndicator implements HealthIndicator {
    
    private static final double MEMORY_THRESHOLD = 90.0; // 90% memory usage threshold
    private static final int DEADLOCK_CHECK_THRESHOLD = 5; // Max deadlocked threads
    private static final long GC_TIME_THRESHOLD = 300000; // 5 minutes GC time threshold (more reasonable for long-running services)
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        long startTime = System.currentTimeMillis();
        
        try {
            // Check internal application state only (no external dependencies)
            boolean memoryHealthy = checkMemoryHealth();
            boolean threadHealthy = checkThreadHealth();
            boolean gcHealthy = checkGarbageCollectionHealth();
            boolean vmHealthy = checkVirtualMachineHealth();
            
            long responseTime = System.currentTimeMillis() - startTime;

            // Application is alive if all internal components are healthy - eliminates all 5 ternaries with Optional patterns
            Health.Builder healthBuilder = Optional.of(memoryHealthy && threadHealthy && gcHealthy && vmHealthy)
                .filter(Boolean::booleanValue)
                .map(healthy -> builder.up())
                .orElseGet(() -> builder.down());

            healthBuilder
                   .withDetail("memory", Optional.of(memoryHealthy)
                       .filter(Boolean::booleanValue)
                       .map(healthy -> "HEALTHY")
                       .orElse("UNHEALTHY"))
                   .withDetail("threads", Optional.of(threadHealthy)
                       .filter(Boolean::booleanValue)
                       .map(healthy -> "HEALTHY")
                       .orElse("UNHEALTHY"))
                   .withDetail("garbageCollection", Optional.of(gcHealthy)
                       .filter(Boolean::booleanValue)
                       .map(healthy -> "HEALTHY")
                       .orElse("UNHEALTHY"))
                   .withDetail("virtualMachine", Optional.of(vmHealthy)
                       .filter(Boolean::booleanValue)
                       .map(healthy -> "HEALTHY")
                       .orElse("UNHEALTHY"))
                   .withDetail("responseTimeMs", responseTime)
                   .withDetail("checkType", "LIVENESS")
                   .withDetail("virtualThreads", "ENABLED")
                   .withDetail("timestamp", Instant.now().toString());
                   
        } catch (Exception e) {
            log.error("Liveness check failed", e);
            builder.down()
                   .withDetail("error", e.getMessage())
                   .withDetail("checkType", "LIVENESS")
                   .withDetail("timestamp", Instant.now().toString());
        }
        
        return builder.build();
    }
    
    /**
     * Check memory health - eliminates nested if-statements with Optional patterns
     */
    private boolean checkMemoryHealth() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            // Check heap memory - eliminates nested if-statements with Optional
            boolean heapHealthy = Optional.of(memoryBean.getHeapMemoryUsage().getMax())
                .filter(max -> max > 0)
                .map(max -> {
                    long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
                    double heapUsagePercent = (double) heapUsed / max * 100;

                    // Log warning if unhealthy - eliminates if-statement
                    Optional.of(heapUsagePercent)
                        .filter(pct -> pct > MEMORY_THRESHOLD)
                        .ifPresent(pct -> log.warn("High heap memory usage detected: {}%", pct));

                    return heapUsagePercent <= MEMORY_THRESHOLD;
                })
                .orElse(true);

            // Check non-heap memory - eliminates nested if-statements with Optional
            boolean nonHeapHealthy = Optional.of(memoryBean.getNonHeapMemoryUsage().getMax())
                .filter(max -> max > 0)
                .map(max -> {
                    long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
                    double nonHeapUsagePercent = (double) nonHeapUsed / max * 100;

                    // Log warning if unhealthy - eliminates if-statement
                    Optional.of(nonHeapUsagePercent)
                        .filter(pct -> pct > MEMORY_THRESHOLD)
                        .ifPresent(pct -> log.warn("High non-heap memory usage detected: {}%", pct));

                    return nonHeapUsagePercent <= MEMORY_THRESHOLD;
                })
                .orElse(true);

            return heapHealthy && nonHeapHealthy;

        } catch (Exception e) {
            log.warn("Memory health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check thread health - eliminates if-statements with Optional patterns
     */
    private boolean checkThreadHealth() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            // Check for deadlocked threads - eliminates if-statement with Optional
            boolean noDeadlocks = Optional.ofNullable(threadBean.findDeadlockedThreads())
                .filter(threads -> threads.length > DEADLOCK_CHECK_THRESHOLD)
                .map(threads -> {
                    log.error("Deadlocked threads detected: {}", threads.length);
                    return false;
                })
                .orElse(true);

            // Check thread counts - eliminates if-statement with Optional
            int activeThreads = threadBean.getThreadCount();
            int peakThreads = threadBean.getPeakThreadCount();

            Optional.of(peakThreads)
                .filter(peak -> peak > 0)
                .filter(peak -> activeThreads > (peak * 0.9))
                .ifPresent(peak -> log.warn("High thread usage: active={}, peak={}", activeThreads, peak));

            return noDeadlocks;

        } catch (Exception e) {
            log.warn("Thread health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check garbage collection health - eliminates if-statements with Optional patterns
     */
    private boolean checkGarbageCollectionHealth() {
        try {
            // Check GC frequency and pause times rather than cumulative time
            long totalCollections = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionCount())
                .sum();

            long totalGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionTime())
                .sum();

            // If no collections, healthy - eliminates if-statements with Optional
            return Optional.of(totalCollections)
                .filter(count -> count > 0)
                .map(count -> {
                    double avgGcPause = (double) totalGcTime / count;
                    boolean gcHealthy = (avgGcPause < 1000.0) && (totalGcTime < GC_TIME_THRESHOLD);

                    // Log warning if unhealthy - eliminates if-statement with Optional
                    Optional.of(gcHealthy)
                        .filter(healthy -> !healthy)
                        .ifPresent(healthy -> log.warn(
                            "GC health check failed - Total collections: {}, Total GC time: {}ms, Avg pause: {}ms",
                            count, totalGcTime, avgGcPause));

                    return gcHealthy;
                })
                .orElse(true);

        } catch (Exception e) {
            log.warn("Garbage collection health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check virtual machine health - eliminates if-statements with Optional patterns
     */
    private boolean checkVirtualMachineHealth() {
        try {
            Runtime runtime = Runtime.getRuntime();

            // Check available processors - eliminates if-statement with Optional
            boolean processorsOk = Optional.of(runtime.availableProcessors())
                .filter(count -> count >= 1)
                .map(count -> true)
                .orElseGet(() -> {
                    log.error("No available processors detected");
                    return false;
                });

            // Return early if processors check failed - eliminates nested if with Optional
            return Optional.of(processorsOk)
                .filter(Boolean::booleanValue)
                .map(ok -> {
                    // Try to allocate memory to ensure JVM is responsive
                    try {
                        @SuppressWarnings("unused")
                        byte[] testAllocation = new byte[1024];
                        return true;
                    } catch (OutOfMemoryError e) {
                        log.error("JVM out of memory during liveness check");
                        return false;
                    }
                })
                .orElse(false);

        } catch (Exception e) {
            log.warn("Virtual machine health check failed: {}", e.getMessage());
            return false;
        }
    }
}