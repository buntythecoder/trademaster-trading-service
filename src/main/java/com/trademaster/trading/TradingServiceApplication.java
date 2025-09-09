package com.trademaster.trading;

import com.trademaster.trading.config.JwtConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Trading Service Application
 * 
 * Main entry point for the TradeMaster Trading API service that provides:
 * - Order placement, modification, and cancellation
 * - Real-time portfolio tracking and P&L calculations
 * - Risk management and pre-trade checks
 * - Broker integration for trade execution
 * - Order lifecycle management with audit trail
 * 
 * Technology Stack:
 * - Spring Boot 3.5.3 with MVC and Virtual Threads
 * - Java 24 with Virtual Threads for high concurrency (millions of threads)
 * - PostgreSQL with JPA/Hibernate for robust database access
 * - Redis for sub-30ms caching of portfolio data
 * - JWT authentication integration with Epic 1 auth service
 * 
 * Performance Targets:
 * - Order placement API: <50ms response time
 * - Order execution latency: <200ms end-to-end
 * - Portfolio update propagation: <10ms via WebSocket
 * - Risk check processing: <25ms per order
 * - Support 10,000+ concurrent trading users with Virtual Threads
 * 
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 * @since 2025-08-21
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration.class
})
@EnableConfigurationProperties(JwtConfigurationProperties.class)
@EnableCaching
@EnableJpaRepositories(basePackages = "com.trademaster.trading.repository")
@EnableJpaAuditing
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableWebSocket
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@EnableFeignClients
public class TradingServiceApplication {

    public static void main(String[] args) {
        // Enable virtual threads for unlimited scalability
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(TradingServiceApplication.class, args);
    }
}