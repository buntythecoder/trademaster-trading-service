package com.trademaster.trading.config;

import com.trademaster.trading.security.filter.TradingServiceApiKeyFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Trading Service Security Configuration
 *
 * MANDATORY: Rule #2 - SOLID Principles (SRP, DIP)
 * MANDATORY: Rule #3 - Functional Programming (pattern matching)
 * MANDATORY: Rule #6 - Zero Trust Security Policy (Tiered Security)
 * MANDATORY: Rule #19 - Access Control & Encapsulation
 *
 * Configures Spring Security with Zero Trust architecture:
 * - External API calls → SecurityFacade + SecurityMediator (full security stack)
 * - Internal service-to-service → TradingServiceApiKeyFilter (lightweight authentication)
 * - Stateless JWT authentication with API key validation
 * - Method-level security with @PreAuthorize annotations
 *
 * Security Architecture:
 * 1. Public paths: /actuator/health, /api-docs, /swagger-ui (bypass security)
 * 2. Internal API paths: /api/internal/** (require service authentication)
 * 3. Public API paths: /api/v1/** (require JWT + API key via SecurityFacade)
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class TradingSecurityConfig {

    private final TradingServiceApiKeyFilter tradingServiceApiKeyFilter;

    /**
     * Configure Security Filter Chain with Zero Trust policy
     * Rule #3: Functional configuration using lambda expressions
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔒 Configuring Trading Service Security Filter Chain with Zero Trust policy");

        http
            // Disable CSRF for stateless REST API (Rule #6: Zero Trust with JWT)
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session management (Rule #6: Zero Trust, no server-side sessions)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configure authorization rules using pattern matching
            .authorizeHttpRequests(auth -> auth
                // Public paths (health checks, API docs)
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/ready",
                    "/actuator/alive",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Internal API paths (service-to-service with API key)
                .requestMatchers("/api/internal/**", "/api/v1/internal/**")
                    .hasAnyRole("SERVICE", "INTERNAL", "TRADING_SERVICE")

                // Public API paths (require authentication via SecurityFacade)
                .requestMatchers("/api/v1/**")
                    .authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // Add custom service API key filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(tradingServiceApiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        SecurityFilterChain chain = http.build();

        log.info("✅ Trading Service Security Filter Chain configured successfully");
        log.info("   - TradingServiceApiKeyFilter registered for internal API authentication");
        log.info("   - Stateless JWT authentication enabled");
        log.info("   - Method-level security enabled with @PreAuthorize");

        return chain;
    }
}
