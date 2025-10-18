package com.trademaster.trading.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 Configuration for Trading Service
 *
 * Configures comprehensive API documentation with:
 * - API metadata and versioning
 * - JWT authentication scheme
 * - Server configurations
 * - Contact and license information
 *
 * Access documentation at:
 * - Swagger UI: http://localhost:8083/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8083/v3/api-docs
 * - OpenAPI YAML: http://localhost:8083/v3/api-docs.yaml
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Virtual Threads)
 */
@Configuration
public class OpenAPIConfig {

    @Value("${spring.application.name:trading-service}")
    private String applicationName;

    @Value("${server.port:8083}")
    private String serverPort;

    @Bean
    public OpenAPI tradingServiceOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(serverList())
            .components(securityComponents());
    }

    /**
     * API metadata information
     */
    private Info apiInfo() {
        return new Info()
            .title("TradeMaster Trading Service API")
            .description("""
                Comprehensive trading platform API providing:

                **Core Capabilities (28 AgentOS Capabilities)**:
                - Order Execution: Market, limit, stop-loss, trailing-stop, bracket, iceberg, TWAP, VWAP orders
                - Market Data: Real-time quotes, order book data, trade streams, market status
                - Broker Routing: Multi-broker support (Zerodha, Upstox, AngelOne), intelligent routing
                - Portfolio Analytics: Performance metrics, risk metrics, attribution analysis
                - AI Recommendations: Technical analysis, sentiment analysis, ML-powered trade recommendations
                - Risk Management: Pre-trade risk checks, compliance validation, portfolio risk metrics
                - Position Tracking: Real-time position monitoring, P&L calculation, position analytics

                **Technology Stack**:
                - Java 24 with Virtual Threads for unlimited scalability
                - Spring Boot 3.5.3 with Spring MVC (non-reactive)
                - Functional programming with Result types and pattern matching
                - Circuit breakers with Resilience4j for fault tolerance
                - Real-time market data integration

                **Performance Targets**:
                - Order processing: <50ms
                - Risk checks: <25ms
                - API response: <200ms
                - AI recommendations: <5s
                - Concurrent users: 10,000+

                **Security**:
                - JWT-based authentication
                - Role-based access control (TRADER, ADMIN)
                - Zero-trust security architecture
                - Comprehensive audit logging
                """)
            .version("2.0.0")
            .contact(apiContact())
            .license(apiLicense());
    }

    /**
     * API contact information
     */
    private Contact apiContact() {
        return new Contact()
            .name("TradeMaster Development Team")
            .email("dev@trademaster.com")
            .url("https://trademaster.com");
    }

    /**
     * API license information
     */
    private License apiLicense() {
        return new License()
            .name("Proprietary")
            .url("https://trademaster.com/license");
    }

    /**
     * Server configurations for different environments
     */
    private List<Server> serverList() {
        Server localServer = new Server()
            .url("http://localhost:" + serverPort)
            .description("Local development server");

        Server devServer = new Server()
            .url("https://dev-api.trademaster.com")
            .description("Development environment");

        Server prodServer = new Server()
            .url("https://api.trademaster.com")
            .description("Production environment");

        return List.of(localServer, devServer, prodServer);
    }

    /**
     * Security scheme components (JWT Bearer authentication)
     */
    private Components securityComponents() {
        return new Components()
            .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                    JWT authentication token.

                    To obtain a token:
                    1. Login via auth-service: POST /api/v1/auth/login
                    2. Include token in Authorization header: Bearer {token}

                    Token contains:
                    - User ID and email
                    - Roles (TRADER, ADMIN)
                    - Expiration time (24 hours)
                    """)
            );
    }
}
