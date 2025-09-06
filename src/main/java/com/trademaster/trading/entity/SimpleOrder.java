package com.trademaster.trading.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Simple Order Entity - Core Trading Functionality
 * 
 * Simplified order entity focusing on essential trading operations
 * without complex relationships that cause compilation issues.
 */
@Entity
@Table(name = "simple_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String orderId;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String symbol;
    
    @Column(nullable = false)
    private String side; // BUY, SELL
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private String orderType; // MARKET, LIMIT
    
    @Column(nullable = false)
    private String status; // PENDING, FILLED, CANCELLED
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    @Column
    private String brokerOrderId;
    
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}