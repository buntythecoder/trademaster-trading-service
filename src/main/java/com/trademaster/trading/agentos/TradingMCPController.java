package com.trademaster.trading.agentos;

import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Trading Service MCP (Multi-Agent Communication Protocol) Controller
 * 
 * Provides standardized endpoints for agent-to-agent communication within
 * the TradeMaster AgentOS ecosystem. Handles order execution, risk management,
 * broker routing, position tracking, and compliance validation requests
 * from other agents in the system.
 * 
 * MCP Protocol Features:
 * - Standardized request/response formats
 * - Authentication and authorization for agent communications
 * - Real-time coordination with other trading agents
 * - Structured concurrency for high-performance processing
 * - Comprehensive error handling and circuit breaker patterns
 * 
 * @author TradeMaster Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mcp/trading")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "https://trademaster.io"})
public class TradingMCPController {
    
    private final TradingAgent tradingAgent;
    private final TradingCapabilityRegistry capabilityRegistry;
    
    /**
     * MCP Endpoint: Execute Order
     * Handles order execution requests from other agents with full validation
     */
    @PostMapping("/executeOrder")
    @MCPMethod("executeOrder")
    @PreAuthorize("hasRole('AGENT') or hasRole('TRADING_SYSTEM')")
    public ResponseEntity<MCPResponse<String>> executeOrder(
            @MCPParam("orderRequest") @RequestBody @Validated OrderExecutionRequest request) {
        
        log.info("MCP: Received order execution request for symbol: {} quantity: {}", 
                request.getSymbol(), request.getQuantity());
        
        try {
            OrderRequest orderRequest = new OrderRequest(
                request.getSymbol(),
                "NSE", // Default exchange
                OrderType.valueOf(request.getOrderType()),
                OrderSide.valueOf(request.getSide()),
                request.getQuantity().intValue(),
                request.getPrice(),
                null, // stop price
                TimeInForce.valueOf(request.getTimeInForce()),
                null, // expiry date
                null, // broker name
                request.getRequestId() != null ? request.getRequestId().toString() : null
            );
                
            CompletableFuture<String> result = tradingAgent.executeOrder(orderRequest);
            String executionResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.<String>builder()
                .success(true)
                .data(executionResult)
                .message("Order executed successfully")
                .agentId(tradingAgent.getAgentId())
                .requestId(request.getRequestId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to execute order", e);
            return ResponseEntity.badRequest().body(MCPResponse.<String>builder()
                .success(false)
                .error("Order execution failed: " + e.getMessage())
                .agentId(tradingAgent.getAgentId())
                .requestId(request.getRequestId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Perform Risk Assessment
     * Handles risk management requests from other agents
     */
    @PostMapping("/performRiskAssessment")
    @MCPMethod("performRiskAssessment")
    @PreAuthorize("hasRole('AGENT') or hasRole('RISK_SYSTEM')")
    public ResponseEntity<MCPResponse<String>> performRiskAssessment(
            @MCPParam("orderRequest") @RequestBody @Validated OrderExecutionRequest request) {
        
        log.info("MCP: Received risk assessment request for symbol: {}", request.getSymbol());
        
        try {
            OrderRequest orderRequest = new OrderRequest(
                request.getSymbol(),
                "NSE", // Default exchange
                OrderType.MARKET, // Default for risk assessment
                OrderSide.valueOf(request.getSide()),
                request.getQuantity().intValue(),
                request.getPrice(),
                null, // stop price
                TimeInForce.DAY,
                null, // expiry date
                null, // broker name
                null // client order ref
            );
                
            CompletableFuture<String> result = tradingAgent.performRiskAssessment(orderRequest);
            String assessmentResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.<String>builder()
                .success(true)
                .data(assessmentResult)
                .message("Risk assessment completed")
                .agentId(tradingAgent.getAgentId())
                .requestId(request.getRequestId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to perform risk assessment", e);
            return ResponseEntity.badRequest().body(MCPResponse.<String>builder()
                .success(false)
                .error("Risk assessment failed: " + e.getMessage())
                .agentId(tradingAgent.getAgentId())
                .requestId(request.getRequestId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Route to Optimal Broker
     * Handles broker routing requests from other agents
     */
    @PostMapping("/routeToOptimalBroker")
    @MCPMethod("routeToOptimalBroker")
    @PreAuthorize("hasRole('AGENT') or hasRole('BROKER_SYSTEM')")
    public ResponseEntity<MCPResponse<String>> routeToOptimalBroker(
            @MCPParam("orderRequest") @RequestBody @Validated OrderExecutionRequest request,
            @MCPParam("availableBrokers") @RequestParam List<String> availableBrokers) {
        
        log.info("MCP: Received broker routing request for symbol: {} with {} brokers", 
                request.getSymbol(), availableBrokers.size());
        
        try {
            OrderRequest orderRequest = new OrderRequest(
                request.getSymbol(),
                "NSE", // Default exchange
                OrderType.valueOf(request.getOrderType()),
                OrderSide.valueOf(request.getSide()),
                request.getQuantity().intValue(),
                request.getPrice(),
                null, // stop price
                TimeInForce.DAY,
                null, // expiry date
                null, // broker name
                null // client order ref
            );
                
            CompletableFuture<String> result = tradingAgent.routeToOptimalBroker(orderRequest, availableBrokers);
            String routingResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.<String>builder()
                .success(true)
                .data(routingResult)
                .message("Broker routing completed")
                .agentId(tradingAgent.getAgentId())
                .requestId(request.getRequestId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to route to optimal broker", e);
            return ResponseEntity.badRequest().body(MCPResponse.<String>builder()
                .success(false)
                .error("Broker routing failed: " + e.getMessage())
                .agentId(tradingAgent.getAgentId())
                .requestId(request.getRequestId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Track Positions
     * Handles position tracking requests from other agents
     */
    @PostMapping("/trackPositions")
    @MCPMethod("trackPositions")
    @PreAuthorize("hasRole('AGENT') or hasRole('PORTFOLIO_SYSTEM')")
    public ResponseEntity<MCPResponse<String>> trackPositions(
            @MCPParam("userId") @RequestParam String userId,
            @MCPParam("symbols") @RequestParam List<String> symbols,
            @MCPParam("requestId") @RequestParam(required = false) Long requestId) {
        
        log.info("MCP: Received position tracking request for user: {} symbols: {}", 
                userId, symbols.size());
        
        try {
            CompletableFuture<String> result = tradingAgent.trackPositions(userId, symbols);
            String trackingResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.<String>builder()
                .success(true)
                .data(trackingResult)
                .message("Position tracking initiated")
                .agentId(tradingAgent.getAgentId())
                .requestId(requestId)
                .processingTimeMs(System.currentTimeMillis())
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to track positions", e);
            return ResponseEntity.badRequest().body(MCPResponse.<String>builder()
                .success(false)
                .error("Position tracking failed: " + e.getMessage())
                .agentId(tradingAgent.getAgentId())
                .requestId(requestId)
                .processingTimeMs(System.currentTimeMillis())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Perform Compliance Check
     * Handles compliance validation requests from other agents
     */
    @PostMapping("/performComplianceCheck")
    @MCPMethod("performComplianceCheck")
    @PreAuthorize("hasRole('AGENT') or hasRole('COMPLIANCE_SYSTEM')")
    public ResponseEntity<MCPResponse<String>> performComplianceCheck(
            @MCPParam("orderRequest") @RequestBody @Validated OrderExecutionRequest request) {
        
        log.info("MCP: Received compliance check request for user: {} symbol: {}", 
                request.getUserId(), request.getSymbol());
        
        try {
            OrderRequest orderRequest = new OrderRequest(
                request.getSymbol(),
                "NSE", // Default exchange
                OrderType.MARKET, // Default for compliance check
                OrderSide.valueOf(request.getSide()),
                request.getQuantity().intValue(),
                null, // limit price
                null, // stop price
                TimeInForce.DAY,
                null, // expiry date
                null, // broker name
                null // client order ref
            );
                
            CompletableFuture<String> result = tradingAgent.performComplianceCheck(orderRequest, request.getUserId());
            String complianceResult = result.join();
            
            return ResponseEntity.ok(MCPResponse.<String>builder()
                .success(true)
                .data(complianceResult)
                .message("Compliance check completed")
                .agentId(tradingAgent.getAgentId())
                .requestId(request.getRequestId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to perform compliance check", e);
            return ResponseEntity.badRequest().body(MCPResponse.<String>builder()
                .success(false)
                .error("Compliance check failed: " + e.getMessage())
                .agentId(tradingAgent.getAgentId())
                .requestId(request.getRequestId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Get Agent Capabilities
     * Returns current agent capabilities and health metrics
     */
    @GetMapping("/capabilities")
    @MCPMethod("getCapabilities")
    @PreAuthorize("hasRole('AGENT') or hasRole('ORCHESTRATION_SERVICE')")
    public ResponseEntity<MCPResponse<AgentCapabilitiesResponse>> getCapabilities() {
        
        log.info("MCP: Received capabilities request");
        
        try {
            AgentCapabilitiesResponse capabilities = AgentCapabilitiesResponse.builder()
                .agentId(tradingAgent.getAgentId())
                .agentType(tradingAgent.getAgentType())
                .capabilities(tradingAgent.getCapabilities())
                .healthScore(tradingAgent.getHealthScore())
                .performanceSummary(capabilityRegistry.getPerformanceSummary())
                .build();
                
            return ResponseEntity.ok(MCPResponse.<AgentCapabilitiesResponse>builder()
                .success(true)
                .data(capabilities)
                .message("Agent capabilities retrieved")
                .agentId(tradingAgent.getAgentId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to get capabilities", e);
            return ResponseEntity.badRequest().body(MCPResponse.<AgentCapabilitiesResponse>builder()
                .success(false)
                .error("Failed to get capabilities: " + e.getMessage())
                .agentId(tradingAgent.getAgentId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
        }
    }
    
    /**
     * MCP Endpoint: Agent Health Check
     * Returns current agent health and performance metrics
     */
    @GetMapping("/health")
    @MCPMethod("getHealth")
    @PreAuthorize("hasRole('AGENT') or hasRole('ORCHESTRATION_SERVICE')")
    public ResponseEntity<MCPResponse<AgentHealthResponse>> getHealth() {
        
        log.debug("MCP: Received health check request");
        
        try {
            AgentHealthResponse health = AgentHealthResponse.builder()
                .agentId(tradingAgent.getAgentId())
                .healthScore(tradingAgent.getHealthScore())
                .status(tradingAgent.getHealthScore() > 0.8 ? "HEALTHY" : "DEGRADED")
                .capabilities(tradingAgent.getCapabilities())
                .lastUpdate(System.currentTimeMillis())
                .build();
                
            return ResponseEntity.ok(MCPResponse.<AgentHealthResponse>builder()
                .success(true)
                .data(health)
                .message("Agent health retrieved")
                .agentId(tradingAgent.getAgentId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
                
        } catch (Exception e) {
            log.error("MCP: Failed to get health", e);
            return ResponseEntity.badRequest().body(MCPResponse.<AgentHealthResponse>builder()
                .success(false)
                .error("Failed to get health: " + e.getMessage())
                .agentId(tradingAgent.getAgentId())
                .processingTimeMs(System.currentTimeMillis())
                .build());
        }
    }
}

// MCP Request/Response DTOs

@lombok.Builder
@lombok.Data
class OrderExecutionRequest {
    private Long requestId;
    private String symbol;
    private String side;
    private BigDecimal quantity;
    private BigDecimal price;
    private String orderType;
    private String timeInForce;
    private String userId;
}

@lombok.Builder
@lombok.Data
class MCPResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private String message;
    private String agentId;
    private Long requestId;
    private Long processingTimeMs;
}

@lombok.Builder
@lombok.Data
class AgentCapabilitiesResponse {
    private String agentId;
    private String agentType;
    private List<String> capabilities;
    private Double healthScore;
    private Map<String, String> performanceSummary;
}

@lombok.Builder
@lombok.Data
class AgentHealthResponse {
    private String agentId;
    private Double healthScore;
    private String status;
    private List<String> capabilities;
    private Long lastUpdate;
}

// MCP Annotations for protocol compliance

@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface MCPMethod {
    String value();
}

@java.lang.annotation.Target(java.lang.annotation.ElementType.PARAMETER)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface MCPParam {
    String value();
}