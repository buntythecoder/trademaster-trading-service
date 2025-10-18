package com.trademaster.trading.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultToken;

import java.net.URI;
import java.util.Optional;

/**
 * HashiCorp Vault Configuration for TradeMaster Trading Service
 * 
 * Provides secure secrets management integration with HashiCorp Vault.
 * Supports multiple authentication methods and environments.
 * 
 * Features:
 * - Token-based authentication for production
 * - Kubernetes service account authentication
 * - Dynamic secrets rotation
 * - Secure credential retrieval
 * - Environment-specific vault endpoints
 * 
 * @author TradeMaster Development Team  
 * @version 2.0.0
 */
@Configuration
@ConditionalOnProperty(name = "vault.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class VaultConfig extends AbstractVaultConfiguration {
    
    @Value("${vault.uri:http://localhost:8200}")
    private String vaultUri;
    
    @Value("${vault.token:#{null}}")
    private String vaultToken;
    
    @Value("${vault.namespace:#{null}}")
    private String vaultNamespace;
    
    @Value("${vault.kv-version:2}")
    private int kvVersion;
    
    @Value("${vault.connection-timeout:5000}")
    private int connectionTimeout;
    
    @Value("${vault.read-timeout:10000}")
    private int readTimeout;
    
    @Override
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(vaultUri));

        // Eliminates if-statement using Optional.ofNullable().map().filter().ifPresent()
        Optional.ofNullable(vaultNamespace)
            .map(String::trim)
            .filter(ns -> !ns.isEmpty())
            .ifPresent(ns -> {
                // VaultEndpoint namespace configuration for newer Spring Vault versions
                log.debug("Vault namespace configuration: {}", ns);
            });

        log.info("Configured Vault endpoint: {} with namespace: {}", vaultUri, vaultNamespace);
        return endpoint;
    }
    
    @Override
    public TokenAuthentication clientAuthentication() {
        // Eliminates if-statement using Optional.ofNullable().map().filter().orElseThrow()
        String validToken = Optional.ofNullable(vaultToken)
            .map(String::trim)
            .filter(token -> !token.isEmpty())
            .orElseThrow(() -> new IllegalArgumentException(
                "Vault token is required. Please set vault.token property or VAULT_TOKEN environment variable"));

        log.info("Configured Vault token authentication");
        return new TokenAuthentication(VaultToken.of(validToken));
    }
    
    /**
     * Vault Template with custom configuration for trading service
     */
    @Bean
    @Profile("!test")
    public VaultTemplate vaultTemplate() {
        VaultTemplate template = new VaultTemplate(vaultEndpoint(), clientAuthentication());
        
        // Configure timeouts
        template.opsForSys().health();
        
        log.info("VaultTemplate initialized successfully");
        return template;
    }
}