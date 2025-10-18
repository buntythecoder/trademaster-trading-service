# TradeMaster Trading Service - Team Onboarding Guide

## Welcome to TradeMaster Trading Service! 🎉

This guide will help you get up to speed with our trading platform, development practices, and operational procedures.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Architecture Deep Dive](#architecture-deep-dive)
3. [Development Workflow](#development-workflow)
4. [Testing Strategy](#testing-strategy)
5. [Deployment Process](#deployment-process)
6. [Monitoring & Operations](#monitoring--operations)
7. [Learning Resources](#learning-resources)

---

## Getting Started

### Day 1: Environment Setup

**Prerequisites**:
- Java 24 JDK installed
- Docker Desktop 20.10+
- IntelliJ IDEA or VS Code
- Git configured
- GitHub account with repository access

**Setup Steps**:

```bash
# Clone repository
git clone https://github.com/trademaster/trading-service.git
cd trading-service

# Start infrastructure services
docker-compose up -d postgres redis kafka

# Build application
./gradlew clean build

# Run application locally
./gradlew bootRun

# Verify setup
curl http://localhost:8080/actuator/health
```

**Access Credentials** (Development):
```
PostgreSQL:
  Host: localhost:5432
  Database: trademaster_trading
  Username: trademaster_user
  Password: trademaster_password

Redis:
  Host: localhost:6379
  Password: (none for dev)

Kafka:
  Bootstrap: localhost:9092
```

### Day 1: Codebase Tour

**Project Structure**:
```
trading-service/
├── src/main/java/com/trademaster/trading/
│   ├── agentos/          # AgentOS framework integration
│   ├── config/           # Spring configuration
│   ├── controller/       # REST API controllers
│   ├── dto/             # Data transfer objects (Records)
│   ├── entity/          # JPA entities
│   ├── repository/      # Data access layer
│   ├── service/         # Business logic
│   └── websocket/       # WebSocket handlers
├── src/main/resources/
│   ├── application.yml  # Main configuration
│   └── db/migration/    # Flyway migrations
├── src/test/java/       # Test code
├── scripts/             # Deployment & maintenance scripts
├── monitoring/          # Prometheus, Grafana configs
└── .github/workflows/   # CI/CD pipelines
```

**Key Technologies**:
- **Java 24** with Virtual Threads (--enable-preview)
- **Spring Boot 3.4.1** (NO WebFlux - Spring MVC only)
- **PostgreSQL 15+** with JPA/Hibernate
- **Redis 7** for caching
- **Apache Kafka** for event streaming
- **Prometheus + Grafana** for monitoring

**Architecture Patterns**:
- Functional Programming (no if-else, no loops)
- SOLID principles enforced
- Circuit Breaker pattern for resilience
- Repository pattern for data access
- Builder pattern for object construction
- Zero Trust Security with SecurityFacade

---

## Architecture Deep Dive

### Virtual Threads Architecture

**Why Virtual Threads?**
- 10x performance improvement for I/O-bound operations
- Simplified concurrency model
- Better resource utilization
- Built-in to Java 24

**Example Usage**:
```java
@Service
public class OrderService {
    // Virtual threads automatically used with Spring Boot 3.4+
    public CompletableFuture<Order> placeOrderAsync(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // This runs on a virtual thread
            return processOrder(request);
        });
    }
}
```

### Zero Trust Security Pattern

**Security Tiers**:

```java
// TIER 1: External Access (REST APIs)
@RestController
public class OrderController {
    private final SecurityFacade securityFacade;  // MANDATORY
    private final OrderService orderService;

    @PostMapping("/api/v1/orders")
    public ResponseEntity<Order> placeOrder(@RequestBody OrderRequest request) {
        return securityFacade.secureAccess(
            SecurityContext.fromRequest(request),
            () -> orderService.placeOrder(request)
        );
    }
}

// TIER 2: Internal Service Communication
@Service
public class OrderService {
    private final PortfolioService portfolioService;  // Direct injection
    private final RiskService riskService;

    public Order placeOrder(OrderRequest request) {
        // Direct service calls - already inside security boundary
        riskService.checkRisk(request);
        return portfolioService.updatePosition(order);
    }
}
```

### Functional Programming Patterns

**No if-else - Use Pattern Matching**:
```java
// ❌ FORBIDDEN
public String getOrderStatus(Order order) {
    if (order.isFilled()) {
        return "FILLED";
    } else if (order.isCancelled()) {
        return "CANCELLED";
    } else {
        return "PENDING";
    }
}

// ✅ CORRECT
public String getOrderStatus(Order order) {
    return switch (order.status()) {
        case FILLED -> "FILLED";
        case CANCELLED -> "CANCELLED";
        case PENDING -> "PENDING";
    };
}
```

**No loops - Use Stream API**:
```java
// ❌ FORBIDDEN
List<Order> activeOrders = new ArrayList<>();
for (Order order : orders) {
    if (order.isActive()) {
        activeOrders.add(order);
    }
}

// ✅ CORRECT
List<Order> activeOrders = orders.stream()
    .filter(Order::isActive)
    .toList();
```

### Circuit Breaker Pattern

**Configuration**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      brokerService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-size: 10
```

**Usage**:
```java
@Service
public class BrokerService {
    @CircuitBreaker(name = "brokerService", fallbackMethod = "placeBrokerOrderFallback")
    public Order placeBrokerOrder(OrderRequest request) {
        // External API call to broker
        return brokerApi.placeOrder(request);
    }

    private Order placeBrokerOrderFallback(OrderRequest request, Exception ex) {
        log.error("Broker service unavailable, using fallback", ex);
        return Order.createPendingOrder(request);
    }
}
```

---

## Development Workflow

### Daily Development Process

1. **Pull Latest Changes**:
   ```bash
   git checkout develop
   git pull origin develop
   ```

2. **Create Feature Branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Write Code Following Standards**:
   - Read `CLAUDE.md` for mandatory coding standards
   - Use functional programming patterns
   - Follow SOLID principles
   - Add tests (>80% coverage requirement)

4. **Run Local Validation**:
   ```bash
   # Build and test
   ./gradlew clean build

   # Run specific tests
   ./gradlew test --tests "*OrderServiceTest"

   # Check code coverage
   ./gradlew jacocoTestReport
   open build/reports/jacoco/test/html/index.html
   ```

5. **Security & Quality Checks**:
   ```bash
   # OWASP dependency check
   ./gradlew dependencyCheckAnalyze

   # Run security tests
   ./scripts/security/run-security-tests.sh
   ```

6. **Commit with Conventional Commits**:
   ```bash
   git add .
   git commit -m "feat: add order cancellation functionality"
   git push origin feature/your-feature-name
   ```

7. **Create Pull Request**:
   ```bash
   gh pr create --title "feat: add order cancellation" --body "Description of changes"
   ```

### Code Review Checklist

**For Authors**:
- [ ] All tests passing locally
- [ ] Code follows functional programming patterns
- [ ] SOLID principles applied
- [ ] Test coverage >80%
- [ ] No TODOs or placeholders
- [ ] No compilation warnings
- [ ] Circuit breakers implemented for external calls
- [ ] Proper error handling with Result types
- [ ] Documentation updated

**For Reviewers**:
- [ ] Code quality and readability
- [ ] Functional programming compliance
- [ ] Security considerations
- [ ] Performance implications
- [ ] Test coverage adequacy
- [ ] Error handling robustness
- [ ] Documentation clarity

---

## Testing Strategy

### Testing Pyramid

```
        /\
       /  \  E2E Tests (5%)
      /    \
     /------\ Integration Tests (20%)
    /        \
   /----------\ Unit Tests (75%)
  /____________\
```

### Unit Testing

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldPlaceMarketOrder() {
        // Given
        OrderRequest request = OrderRequest.builder()
            .symbol("RELIANCE")
            .quantity(10)
            .type(OrderType.MARKET)
            .side(OrderSide.BUY)
            .build();

        // When
        Order order = orderService.placeOrder(request);

        // Then
        assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
        verify(portfolioService).updatePosition(any());
    }
}
```

### Integration Testing

**Example**:
```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test_db");

    @Autowired
    private OrderService orderService;

    @Test
    void shouldPersistOrderToDatabase() {
        // Test with real database
        OrderRequest request = createTestOrderRequest();
        Order order = orderService.placeOrder(request);

        assertThat(order.id()).isNotNull();
    }
}
```

### Load Testing

**Run Gatling Tests**:
```bash
# Normal load (5K users)
./gradlew gatlingRun-com.trademaster.trading.loadtest.OrderPlacementLoadTest

# Stress test (15K users)
./gradlew gatlingRun-com.trademaster.trading.loadtest.ComprehensiveStressTest

# View results
open build/reports/gatling/*/index.html
```

---

## Deployment Process

### Development → Staging → Production

```
Developer Push → CI/CD Pipeline → Tests → Security Scans → Load Tests
                                     ↓
                              Deploy to Staging
                                     ↓
                              Smoke Tests Pass
                                     ↓
                     Pull Request to main branch
                                     ↓
                              Code Review Approval
                                     ↓
                           Merge to main branch
                                     ↓
                         Deploy to Production (Blue-Green)
                                     ↓
                   Canary Rollout (10% → 25% → 50% → 75% → 100%)
                                     ↓
                           Production Validation
```

### Deployment Commands

**View Recent Deployments**:
```bash
gh run list --limit 10
```

**Trigger Manual Deployment**:
```bash
# Deploy to staging
gh workflow run ci-cd-pipeline.yml --ref develop

# Deploy to production
gh workflow run ci-cd-pipeline.yml --ref main
```

**Monitor Deployment**:
```bash
# Watch workflow progress
gh run watch

# Check deployment status
gh run view <run-id>
```

**Emergency Rollback**:
```bash
./scripts/deployment/blue-green-deploy.sh --environment production --rollback
```

---

## Monitoring & Operations

### Key Dashboards

**Grafana Dashboards** (http://localhost:3000):
1. **Application Health**: Overall system health, response times, error rates
2. **Trading Operations**: Order metrics, portfolio updates, business KPIs
3. **Database Performance**: Query performance, connection pools, slow queries
4. **Circuit Breakers**: External service health, failure rates, state transitions

### Daily Monitoring Routine

**Morning Checklist** (30 minutes):
```bash
# 1. Check Grafana dashboards
open http://localhost:3000/d/application-health

# 2. Review overnight alerts
curl -s http://localhost:9090/api/v1/alerts | jq '.data.alerts[] | select(.state=="firing")'

# 3. Check application logs
docker logs --since 24h trademaster-trading-service | grep ERROR

# 4. Verify backups completed
ls -lh /var/backups/postgresql/full/ | tail -n 5

# 5. Review metrics
curl -s http://localhost:8080/actuator/metrics | jq
```

### On-Call Responsibilities

**When You're On-Call**:
- Respond to PagerDuty alerts within 15 minutes
- Monitor #trademaster-critical Slack channel
- Follow incident response procedures (see OPERATIONS_RUNBOOK.md)
- Document all incidents with resolution steps
- Escalate if unable to resolve within 30 minutes

**Common On-Call Scenarios**:
1. **High Error Rate**: Check OPERATIONS_RUNBOOK.md → "High Error Rate"
2. **Service Down**: Restart service, check logs, escalate if needed
3. **Database Issues**: Check connection pool, kill long queries
4. **Circuit Breaker Open**: Verify external service status, wait for recovery

---

## Learning Resources

### Week 1: Foundation

**Day 1-2: Setup & Architecture**
- [ ] Complete environment setup
- [ ] Read `README.md` and `CLAUDE.md`
- [ ] Review architecture diagrams
- [ ] Run application locally

**Day 3-4: Code Exploration**
- [ ] Read `standards/functional-programming-guide.md`
- [ ] Study existing service implementations
- [ ] Run and understand tests
- [ ] Review circuit breaker implementations

**Day 5: Small Bug Fix**
- [ ] Pick a beginner-friendly issue
- [ ] Create feature branch
- [ ] Implement fix with tests
- [ ] Submit pull request

### Week 2: Feature Development

**Day 1-3: Implement Small Feature**
- [ ] Design feature following functional patterns
- [ ] Write tests first (TDD)
- [ ] Implement feature
- [ ] Add integration tests

**Day 4: Security & Performance**
- [ ] Review security patterns
- [ ] Run load tests
- [ ] Optimize if needed
- [ ] Add monitoring metrics

**Day 5: Deployment**
- [ ] Deploy to staging
- [ ] Run smoke tests
- [ ] Create pull request
- [ ] Deploy to production (with mentor)

### Week 3: Operations

**Day 1-2: Monitoring Deep Dive**
- [ ] Learn Grafana dashboards
- [ ] Understand Prometheus queries
- [ ] Review alert configurations
- [ ] Practice incident response

**Day 3-4: On-Call Shadow**
- [ ] Shadow on-call engineer
- [ ] Practice using runbooks
- [ ] Respond to test incidents
- [ ] Document learnings

**Day 5: Operations Review**
- [ ] Review OPERATIONS_RUNBOOK.md
- [ ] Understand escalation procedures
- [ ] Know who to contact
- [ ] Complete onboarding quiz

### Month 1: Mastery

**Week 4: Advanced Topics**
- [ ] Virtual Threads deep dive
- [ ] Advanced functional patterns
- [ ] Performance optimization techniques
- [ ] Security best practices
- [ ] Disaster recovery procedures

### Recommended Reading

**Internal Documentation**:
1. `CLAUDE.md` - Mandatory coding standards
2. `standards/functional-programming-guide.md` - FP patterns
3. `standards/advanced-design-patterns.md` - Design patterns
4. `DEPLOYMENT_GUIDE.md` - Deployment procedures
5. `OPERATIONS_RUNBOOK.md` - Operational procedures

**External Resources**:
- **Java 24 Virtual Threads**: [JEP 444](https://openjdk.org/jeps/444)
- **Spring Boot 3.4**: [Documentation](https://docs.spring.io/spring-boot/docs/3.4.x/reference/html/)
- **Functional Java**: "Functional Programming in Java" by Pierre-Yves Saumont
- **Resilience Patterns**: [Resilience4j Documentation](https://resilience4j.readme.io/)
- **Trading Systems**: "Building Winning Algorithmic Trading Systems" by Kevin Davey

### Training Exercises

**Exercise 1: Simple Order Service Enhancement**
```
Task: Add order modification functionality
- Accept modification requests
- Validate changes
- Update order in database
- Publish event to Kafka
- Add tests (unit + integration)
- Follow functional programming patterns
```

**Exercise 2: Circuit Breaker Implementation**
```
Task: Add circuit breaker for payment service
- Configure Resilience4j
- Implement fallback mechanism
- Add monitoring metrics
- Test circuit breaker transitions
- Document configuration
```

**Exercise 3: Performance Optimization**
```
Task: Optimize portfolio calculation performance
- Profile current performance
- Identify bottlenecks
- Implement caching strategy
- Add database indexes
- Validate improvements with load tests
```

### Onboarding Checklist

**Technical Setup** (Day 1):
- [ ] Development environment configured
- [ ] GitHub access granted
- [ ] AWS access configured (if needed)
- [ ] Slack channels joined
- [ ] PagerDuty account created
- [ ] First application run successful

**Knowledge** (Week 1):
- [ ] Architecture understanding
- [ ] Coding standards reviewed
- [ ] Testing strategy understood
- [ ] First code contribution merged

**Operations** (Week 2-3):
- [ ] Monitoring dashboards familiar
- [ ] Runbooks reviewed
- [ ] On-call procedures understood
- [ ] Shadow on-call completed

**Independence** (Month 1):
- [ ] Feature implemented independently
- [ ] Deployment performed successfully
- [ ] Incident handled independently
- [ ] Code review given to peer

### Getting Help

**Questions During Onboarding**:
- Technical questions: #trademaster-team on Slack
- Architecture questions: Tag @tech-lead
- Operations questions: Tag @on-call
- General questions: Tag @mentor

**Pair Programming Sessions**:
- Schedule with mentor: 2-3 sessions per week
- Focus areas: architecture, patterns, testing

**Weekly 1:1 with Mentor**:
- Review progress
- Address blockers
- Set goals for next week
- Career development discussion

---

## Onboarding Success Criteria

**By End of Week 1**:
- ✅ Can run application locally
- ✅ Understand architecture
- ✅ Submitted first pull request

**By End of Week 2**:
- ✅ Feature implemented with tests
- ✅ Pull request reviewed and merged
- ✅ Understand deployment process

**By End of Month 1**:
- ✅ Independent feature development
- ✅ Can respond to incidents
- ✅ Comfortable with on-call rotation
- ✅ Contributing to code reviews

---

## Welcome Again! 🚀

You're joining a high-performing team building a mission-critical trading platform. We're excited to have you here!

**Next Steps**:
1. Complete Day 1 setup
2. Schedule intro meeting with mentor
3. Join #trademaster-team on Slack
4. Start Week 1 learning plan

**Questions?** Don't hesitate to ask in #trademaster-team or message your mentor directly.

---

**Onboarding Guide Version**: 1.0.0
**Last Updated**: 2025-01-17
**Next Review**: 2025-02-17
**Maintained By**: TradeMaster Engineering Team
