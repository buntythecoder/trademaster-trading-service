package com.trademaster.trading.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;

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
            
            // Application is alive if all internal components are healthy
            if (memoryHealthy && threadHealthy && gcHealthy && vmHealthy) {
                builder.up();
            } else {
                builder.down();
            }
            
            builder.withDetail("memory", memoryHealthy ? "HEALTHY" : "UNHEALTHY")
                   .withDetail("threads", threadHealthy ? "HEALTHY" : "UNHEALTHY")
                   .withDetail("garbageCollection", gcHealthy ? "HEALTHY" : "UNHEALTHY")
                   .withDetail("virtualMachine", vmHealthy ? "HEALTHY" : "UNHEALTHY")
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
    
    private boolean checkMemoryHealth() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            // Check heap memory usage
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            
            if (heapMax > 0) {
                double heapUsagePercent = (double) heapUsed / heapMax * 100;
                
                if (heapUsagePercent > MEMORY_THRESHOLD) {
                    log.warn("High heap memory usage detected: {}%", heapUsagePercent);
                    return false;
                }
            }
            
            // Check non-heap memory usage (metaspace, etc.)
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
            
            if (nonHeapMax > 0) {
                double nonHeapUsagePercent = (double) nonHeapUsed / nonHeapMax * 100;
                
                if (nonHeapUsagePercent > MEMORY_THRESHOLD) {
                    log.warn("High non-heap memory usage detected: {}%", nonHeapUsagePercent);
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.warn("Memory health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean checkThreadHealth() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            
            // Check for deadlocked threads
            long[] deadlockedThreads = threadBean.findDeadlockedThreads();
            if (deadlockedThreads != null && deadlockedThreads.length > DEADLOCK_CHECK_THRESHOLD) {
                log.error("Deadlocked threads detected: {}", deadlockedThreads.length);
                return false;
            }
            
            // Check thread counts
            int activeThreads = threadBean.getThreadCount();
            int peakThreads = threadBean.getPeakThreadCount();
            
            // If peak threads is significantly higher than current, might indicate thread leak recovery
            if (peakThreads > 0 && activeThreads > (peakThreads * 0.9)) {
                log.warn("High thread usage: active={}, peak={}", activeThreads, peakThreads);
            }
            
            return true;
            
        } catch (Exception e) {
            log.warn("Thread health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean checkGarbageCollectionHealth() {
        try {
            // Check GC frequency and pause times rather than cumulative time
            long totalCollections = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionCount())
                .sum();

            long totalGcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(gc -> gc.getCollectionTime())
                .sum();

            // If no collections have occurred, consider it healthy
            if (totalCollections == 0) {
                return true;
            }

            // Calculate average GC pause time
            double avgGcPause = (double) totalGcTime / totalCollections;

            // Consider GC unhealthy if average pause > 1000ms (1 second) or total time > 5 minutes
            boolean gcHealthy = (avgGcPause < 1000.0) && (totalGcTime < GC_TIME_THRESHOLD);

            if (!gcHealthy) {
                log.warn("GC health check failed - Total collections: {}, Total GC time: {}ms, Avg pause: {}ms",
                        totalCollections, totalGcTime, avgGcPause);
            }

            return gcHealthy;

        } catch (Exception e) {
            log.warn("Garbage collection health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean checkVirtualMachineHealth() {
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // Check available processors
            int availableProcessors = runtime.availableProcessors();
            if (availableProcessors < 1) {
                log.error("No available processors detected");
                return false;
            }
            
            // Basic JVM state check
            try {
                // Try to allocate a small amount of memory to ensure JVM is responsive
                @SuppressWarnings("unused")
                byte[] testAllocation = new byte[1024];
                return true;
            } catch (OutOfMemoryError e) {
                log.error("JVM out of memory during liveness check");
                return false;
            }
            
        } catch (Exception e) {
            log.warn("Virtual machine health check failed: {}", e.getMessage());
            return false;
        }
    }
}