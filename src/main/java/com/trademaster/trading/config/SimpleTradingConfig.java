package com.trademaster.trading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Simple Trading Configuration
 * 
 * Basic configuration beans needed for the simplified trading service.
 */
@Configuration
public class SimpleTradingConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}