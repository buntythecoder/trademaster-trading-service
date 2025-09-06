package com.trademaster.trading.repository;

import com.trademaster.trading.entity.SimpleOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Simple Order Repository
 * 
 * Basic CRUD operations for orders without complex queries
 * that might cause compilation issues.
 */
@Repository
public interface SimpleOrderRepository extends JpaRepository<SimpleOrder, Long> {
    
    Optional<SimpleOrder> findByOrderId(String orderId);
    
    List<SimpleOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT o FROM SimpleOrder o WHERE o.userId = ?1 AND o.status = ?2")
    List<SimpleOrder> findByUserIdAndStatus(Long userId, String status);
}