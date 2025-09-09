package com.trademaster.trading.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Meter Registry Configuration
 * 
 * Isolated configuration for meter registry customization to avoid
 * circular dependencies with other metrics components.
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class MeterRegistryConfiguration {
    
    /**
     * Customize meter registry with trading-specific configuration
     * 
     * Separated from MetricsEnhancedConfiguration to break circular dependency
     * between MeterRegistry → MetricsEnhancedConfiguration → TradingMetricsService → MeterRegistry
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags(
                "application", "trading-service",
                "environment", getEnvironment(),
                "instance", getInstanceId(),
                "version", "2.0.0"
            )
            .meterFilter(MeterFilter.deny(id -> {
                String name = id.getName();
                // Filter out noisy metrics that aren't useful
                return name.startsWith("jvm.gc.overhead") ||
                       name.startsWith("process.files") ||
                       name.startsWith("system.load.average.1m");
            }));
    }
    
    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "development");
    }
    
    private String getInstanceId() {
        return System.getProperty("instance.id", "trading-service-1");
    }
}