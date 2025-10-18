# Phase 7: Integration Testing Plan

**Date**: 2025-10-15
**Phase**: 7 - Integration Testing for Refactored Services
**Status**: 🔄 IN PROGRESS

---

## Executive Summary

Phase 7 validates that all Phase 6 refactorings (6C-6G) work correctly in integration, with no functional regressions. We'll test 5 refactored services through comprehensive integration tests, cross-service workflows, and regression validation.

### Objectives

1. **Validate Refactored Code**: Ensure Pattern 2 extractions work correctly in integration
2. **Prevent Regressions**: Verify business logic unchanged after refactoring
3. **Test Interactions**: Validate cross-service communication and workflows
4. **Measure Performance**: Ensure performance unchanged or improved
5. **Document Coverage**: Achieve >70% integration test coverage

### Services Under Test

| Service | Phase | Refactoring | Methods | Helpers | Priority |
|---------|-------|-------------|---------|---------|----------|
| MarketDataService | 6C | 20% → 100% | 8 | ~15 | **HIGH** |
| PortfolioService | 6D | 60% → 100% | 3 | ~8 | **HIGH** |
| OrderService | 6E | 80% → 100% | 3 | ~6 | **CRITICAL** |
| OrderRouter | 6F | 78% → 100% | 4 | ~8 | **CRITICAL** |
| MetricsService | 6G | 90% → 100% | 2 | 6 | **MEDIUM** |

---

## Testing Strategy

### 1. Service-Level Integration Tests

**Purpose**: Validate each service works correctly with real dependencies.

**Approach**:
- Use TestContainers for PostgreSQL, Redis, Kafka
- Test with real HTTP clients (OkHttp/HttpClient5)
- Validate JPA/Hibernate interactions
- Test with Spring Boot test context
- Verify functional patterns (Optional, Result types, Stream API)

**Coverage Target**: >70% for each service

**Test Categories**:
- **Happy Path**: Standard successful operations
- **Error Handling**: Exception handling, fallback logic
- **Edge Cases**: Boundary conditions, null handling
- **Concurrency**: Virtual thread safety, race conditions
- **Database**: ACID compliance, transaction management

---

### 2. Cross-Service Workflow Tests

**Purpose**: Validate end-to-end workflows across multiple services.

**Critical Workflows**:

#### Workflow 1: Order Placement → Portfolio Update
```
OrderService.placeOrder()
  → OrderRouter.routeOrder()
  → BrokerClient.executeOrder()
  → PortfolioService.updatePosition()
  → MetricsService.recordOrder()
```

#### Workflow 2: Market Data → Risk Assessment
```
MarketDataService.getQuote()
  → RiskService.assessRisk()
  → OrderRouter.validateOrder()
  → OrderService.processOrder()
```

#### Workflow 3: Authentication → Order Execution
```
SecurityService.authenticate()
  → OrderService.validateUser()
  → OrderRouter.routeOrder()
  → MetricsService.recordActivity()
```

**Coverage Target**: 100% for critical paths

---

### 3. Regression Testing

**Purpose**: Ensure refactoring didn't change business behavior.

**Validation Approach**:
- Compare API responses before/after refactoring
- Verify database state consistency
- Check audit logs for completeness
- Validate metrics collection accuracy
- Compare performance benchmarks

**Success Criteria**:
- API contracts unchanged
- Business logic identical
- Performance unchanged or improved
- No new errors or exceptions

---

## Integration Test Architecture

### TestContainers Setup

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
public abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("trademaster_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

### Test Data Builders

```java
public class TestDataBuilder {

    public static OrderRequest validOrderRequest() {
        return OrderRequest.builder()
            .userId("TEST_USER_001")
            .symbol("AAPL")
            .quantity(100)
            .orderType(OrderType.MARKET)
            .side(OrderSide.BUY)
            .build();
    }

    public static Portfolio testPortfolio() {
        return Portfolio.builder()
            .userId("TEST_USER_001")
            .totalValue(BigDecimal.valueOf(100000))
            .cashBalance(BigDecimal.valueOf(50000))
            .positions(new ArrayList<>())
            .build();
    }
}
```

### Functional Test Patterns

```java
// Pattern: Result Type Validation
@Test
void testOrderPlacement_Success() {
    // Given
    OrderRequest request = TestDataBuilder.validOrderRequest();

    // When
    Result<Order, OrderError> result = orderService.placeOrder(request);

    // Then
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValue())
        .hasFieldOrPropertyWithValue("symbol", "AAPL")
        .hasFieldOrPropertyWithValue("status", OrderStatus.PLACED);
}

// Pattern: Optional Handling
@Test
void testPortfolioRetrieval_NotFound() {
    // When
    Optional<Portfolio> portfolio = portfolioService.findByUserId("NON_EXISTENT");

    // Then
    assertThat(portfolio).isEmpty();
}

// Pattern: Stream API Validation
@Test
void testOrderFiltering_MultipleConditions() {
    // Given
    List<Order> orders = createTestOrders();

    // When
    List<Order> filtered = orderService.filterOrders(orders,
        order -> order.getSymbol().equals("AAPL") && order.getStatus() == OrderStatus.FILLED);

    // Then
    assertThat(filtered)
        .hasSize(3)
        .allMatch(order -> order.getSymbol().equals("AAPL"))
        .allMatch(order -> order.getStatus() == OrderStatus.FILLED);
}
```

---

## Test Plan by Service

### Task 7.2: MarketDataService Integration Tests

**Focus**: Pattern 2 extractions for market data providers

**Test Coverage**:
- ✅ `getQuote()` - Quote retrieval with fallback
- ✅ `getHistoricalData()` - Historical data processing
- ✅ `validateSymbol()` - Symbol validation with cache
- ✅ `enrichQuoteData()` - Data enrichment pipeline
- ✅ Provider fallback chain (NSE → BSE → AlphaVantage)
- ✅ Circuit breaker integration
- ✅ Cache hit/miss scenarios
- ✅ Error handling and resilience

**Critical Workflows**:
1. Primary provider success → Cache update
2. Primary provider failure → Fallback to secondary
3. All providers down → Circuit breaker open
4. Cache hit → Skip provider call

**Estimated Tests**: 15-20 test cases

---

### Task 7.3: PortfolioService Integration Tests

**Focus**: Position management and P&L calculations

**Test Coverage**:
- ✅ `createPortfolio()` - Portfolio creation with validation
- ✅ `updatePosition()` - Position updates with concurrency
- ✅ `calculatePnL()` - P&L calculation accuracy
- ✅ `getPortfolioSummary()` - Aggregation with virtual threads
- ✅ Transaction processing with ACID compliance
- ✅ Database consistency under concurrent updates
- ✅ Virtual thread safety for parallel calculations
- ✅ Audit trail completeness

**Critical Workflows**:
1. Order execution → Position update → P&L recalculation
2. Concurrent position updates → Consistency maintained
3. Portfolio valuation → Market data integration
4. Transaction rollback → State consistency

**Estimated Tests**: 12-15 test cases

---

### Task 7.4: OrderService Integration Tests

**Focus**: Order lifecycle and validation

**Test Coverage**:
- ✅ `placeOrder()` - Order placement with validation
- ✅ `validateOrder()` - Validation chain with Result types
- ✅ `processOrder()` - Order processing with state transitions
- ✅ `enrichOrderData()` - Data enrichment pipeline
- ✅ Risk assessment integration
- ✅ Order status transitions
- ✅ Error handling with functional patterns
- ✅ Audit logging completeness

**Critical Workflows**:
1. Order placement → Validation → Risk check → Router
2. Order rejection → Error handling → User notification
3. Order execution → Portfolio update → Metrics
4. Order cancellation → State rollback

**Estimated Tests**: 12-15 test cases

---

### Task 7.5: OrderRouter Integration Tests

**Focus**: Smart routing logic and broker selection

**Test Coverage**:
- ✅ `routeOrder()` - Routing logic with pattern matching
- ✅ `selectBroker()` - Broker selection strategy
- ✅ `validateRouting()` - Routing validation
- ✅ `handleRoutingFailure()` - Failure recovery
- ✅ Load balancing across brokers
- ✅ Broker health monitoring
- ✅ Fallback routing strategies
- ✅ Performance under load

**Critical Workflows**:
1. Primary broker available → Direct routing
2. Primary broker down → Fallback broker
3. All brokers down → Circuit breaker
4. Load balancing → Even distribution

**Estimated Tests**: 10-12 test cases

---

### Task 7.6: MetricsService Integration Tests

**Focus**: Metrics collection and Prometheus integration

**Test Coverage**:
- ✅ `recordOrderPlaced()` - Order metrics
- ✅ `recordOrderExecuted()` - Execution metrics
- ✅ `recordRiskViolation()` - Risk metrics
- ✅ `updatePnL()` - Financial metrics
- ✅ Gauge registration accuracy
- ✅ Counter increment consistency
- ✅ Timer measurement accuracy
- ✅ Metric tag propagation

**Critical Workflows**:
1. Order event → Metrics recorded → Prometheus export
2. Concurrent metrics updates → Thread safety
3. Metric aggregation → Correct totals
4. Health checks → Accurate reporting

**Estimated Tests**: 8-10 test cases

---

### Task 7.7: Cross-Service Workflow Tests

**Purpose**: End-to-end integration across all services

**Test Scenarios**:

#### Scenario 1: Complete Order Lifecycle
```
User Authentication
  → Order Placement (OrderService)
  → Order Validation (OrderService + RiskService)
  → Order Routing (OrderRouter)
  → Broker Execution (BrokerClient)
  → Position Update (PortfolioService)
  → P&L Calculation (PortfolioService)
  → Metrics Recording (MetricsService)
  → Audit Logging
```

#### Scenario 2: Market Data Driven Trading
```
Market Data Update (MarketDataService)
  → Price Alert Trigger
  → Risk Assessment (RiskService)
  → Order Generation (OrderService)
  → Order Routing (OrderRouter)
  → Portfolio Impact Analysis (PortfolioService)
```

#### Scenario 3: Error Recovery Flow
```
Order Placement (OrderService)
  → Risk Violation Detected
  → Order Rejection
  → User Notification
  → Metrics Recording (failure counter)
  → Audit Trail Update
```

**Estimated Tests**: 5-7 comprehensive scenarios

---

### Task 7.8: Regression Testing & Validation

**Validation Matrix**:

| Validation Type | Before Refactoring | After Refactoring | Status |
|-----------------|-------------------|-------------------|--------|
| API Response Structure | Baseline | Current | ⏳ |
| Business Logic Output | Baseline | Current | ⏳ |
| Database State | Baseline | Current | ⏳ |
| Performance Metrics | Baseline | Current | ⏳ |
| Error Handling | Baseline | Current | ⏳ |
| Audit Logs | Baseline | Current | ⏳ |

**Regression Test Categories**:
1. **API Compatibility**: All endpoints return same structure
2. **Data Consistency**: Database state identical for same inputs
3. **Error Messages**: User-facing errors unchanged
4. **Audit Completeness**: All operations logged correctly
5. **Performance**: Response times unchanged or improved

**Estimated Tests**: 10-12 regression validation tests

---

## Performance Benchmarks

### Baseline Targets (from Phase 6 refactoring)

| Service | Operation | Target | Measurement |
|---------|-----------|--------|-------------|
| MarketData | getQuote() | <200ms | Response time |
| Portfolio | calculatePnL() | <100ms | Processing time |
| Order | placeOrder() | <50ms | Order placement |
| Router | routeOrder() | <30ms | Routing decision |
| Metrics | recordMetric() | <10ms | Metric recording |

### Virtual Thread Performance

| Scenario | Platform Threads | Virtual Threads | Improvement |
|----------|-----------------|-----------------|-------------|
| 10K concurrent orders | ~5000ms | ~500ms | 10x faster |
| Portfolio valuation | ~1000ms | ~100ms | 10x faster |
| Market data fetch | ~2000ms | ~200ms | 10x faster |

---

## Test Execution Plan

### Phase 7 Timeline

| Task | Description | Duration | Dependencies |
|------|-------------|----------|--------------|
| 7.1 | Create integration test plan | 1h | None |
| 7.2 | MarketDataService tests | 3h | 7.1 |
| 7.3 | PortfolioService tests | 2-3h | 7.1 |
| 7.4 | OrderService tests | 2-3h | 7.1 |
| 7.5 | OrderRouter tests | 2h | 7.1 |
| 7.6 | MetricsService tests | 1-2h | 7.1 |
| 7.7 | Cross-service workflow tests | 3-4h | 7.2-7.6 |
| 7.8 | Regression testing | 2-3h | 7.7 |
| 7.9 | Documentation | 1h | 7.8 |

**Total Estimated Time**: 16-22 hours

### Execution Strategy

1. **Parallel Execution**: Run service-level tests in parallel (7.2-7.6)
2. **Sequential Workflows**: Run cross-service tests after service tests
3. **Continuous Validation**: Run regression tests throughout
4. **Incremental Documentation**: Document as we test

---

## Success Criteria

### Mandatory Requirements

- ✅ All integration tests pass with >70% coverage
- ✅ No functional regressions detected
- ✅ Performance benchmarks met or exceeded
- ✅ API contracts preserved
- ✅ Database consistency validated
- ✅ Audit trails complete
- ✅ Error handling unchanged

### Quality Metrics

- **Test Coverage**: >70% integration coverage per service
- **Pass Rate**: 100% of critical workflow tests
- **Performance**: No degradation, 10x improvement with virtual threads
- **Reliability**: Zero new exceptions or errors
- **Documentation**: Complete test documentation

---

## Tools & Framework

### Testing Stack

- **JUnit 5**: Test framework
- **AssertJ**: Fluent assertions
- **TestContainers**: Infrastructure (PostgreSQL, Redis, Kafka)
- **MockMvc**: REST API testing
- **WireMock**: External service mocking
- **JMH**: Performance benchmarking
- **Jacoco**: Code coverage

### Test Configuration

```yaml
# application-integration-test.yml
spring:
  threads:
    virtual:
      enabled: true

  datasource:
    # Configured by TestContainers dynamically

  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  kafka:
    # Configured by TestContainers dynamically

  redis:
    # Configured by TestContainers dynamically

logging:
  level:
    com.trademaster: DEBUG
    org.springframework.test: DEBUG
```

---

## Risk Assessment

### Low Risks ✅

- Service-level tests straightforward with TestContainers
- Pattern 2 extractions are simple delegation
- Functional patterns well-tested in unit tests

### Medium Risks ⚠️

- Cross-service workflow complexity
- TestContainers startup time
- Concurrent test execution reliability

### Mitigation Strategies

- Start with service-level tests to build confidence
- Use TestContainers singleton pattern for speed
- Implement test isolation and cleanup
- Use dedicated test database per service
- Monitor test execution time and optimize

---

## Next Steps

1. ✅ **Task 7.1 Complete**: Integration testing plan created
2. **Task 7.2 Next**: Begin MarketDataService integration tests
3. Continue with PortfolioService, OrderService, OrderRouter, MetricsService
4. Execute cross-service workflow tests
5. Run regression validation
6. Document results and create Phase 7 summary

---

**Document Version**: 1.0
**Last Updated**: 2025-10-15
**Author**: TradeMaster Development Team
**Status**: 🔄 PLAN COMPLETE - READY FOR TASK 7.2 EXECUTION
