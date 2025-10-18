# TradeMaster Load Testing Guide

## Overview

This guide provides comprehensive instructions for running load tests on TradeMaster trading platform using Gatling. The load testing suite validates system performance, identifies bottlenecks, and ensures the platform can handle production-scale traffic.

### Test Suite Components

1. **OrderPlacementLoadTest** - Order placement workflow performance
2. **PortfolioRetrievalLoadTest** - Portfolio data retrieval performance
3. **ComprehensiveStressTest** - System behavior under extreme load
4. **EnduranceTest** - Long-duration stability validation

---

## Prerequisites

### System Requirements

- **Java 24**: Gatling simulations require Java 24 with preview features
- **Memory**: Minimum 8GB RAM (16GB recommended for stress testing)
- **CPU**: 4+ cores recommended for load generation
- **Network**: Stable connection to target environment

### Environment Setup

```bash
# Verify Java 24 is installed
java -version # Should show Java 24

# Verify Gradle setup
./gradlew --version # Should show Gradle 8.14+

# Ensure trading service is running
curl http://localhost:8080/actuator/health
```

### Authentication Setup

Generate a valid JWT token for load testing:

```bash
# Option 1: Use existing authentication endpoint
export AUTH_TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpassword"}' | jq -r '.token')

# Option 2: Use test token (for local testing only)
export AUTH_TOKEN="test-jwt-token"
```

---

## Running Load Tests

### Quick Start

```bash
# Run all load tests with normal load profile
./gradlew gatlingRun

# Run specific simulation
./gradlew gatlingRun-com.trademaster.trading.simulations.OrderPlacementLoadTest

# Run with custom parameters
./gradlew gatlingRun -DbaseUrl=http://localhost:8000 \
  -DloadProfile=peak \
  -Dduration=60 \
  -DauthToken=$AUTH_TOKEN
```

### Test Scenarios

#### 1. Normal Load Test (Baseline)

**Objective**: Establish performance baseline with 50% capacity load.

```bash
./gradlew gatlingRun \
  -Dgatling.simulationClass=com.trademaster.trading.simulations.OrderPlacementLoadTest \
  -DbaseUrl=http://localhost:8000 \
  -DloadProfile=normal \
  -Dduration=30 \
  -DauthToken=$AUTH_TOKEN
```

**Expected Results**:
- Concurrent Users: 5,000
- Response Time p95: <200ms
- Response Time p99: <500ms
- Error Rate: <0.1%
- Duration: 30 minutes

#### 2. Peak Load Test (Full Capacity)

**Objective**: Validate performance at 100% expected capacity.

```bash
./gradlew gatlingRun \
  -Dgatling.simulationClass=com.trademaster.trading.simulations.OrderPlacementLoadTest \
  -DbaseUrl=http://localhost:8000 \
  -DloadProfile=peak \
  -Dduration=60 \
  -DauthToken=$AUTH_TOKEN
```

**Expected Results**:
- Concurrent Users: 10,000
- Response Time p95: <200ms
- Response Time p99: <500ms
- Error Rate: <0.1%
- Duration: 60 minutes

#### 3. Stress Test (Beyond Capacity)

**Objective**: Identify system breaking point and failure modes.

```bash
./gradlew gatlingRun \
  -Dgatling.simulationClass=com.trademaster.trading.simulations.ComprehensiveStressTest \
  -DbaseUrl=http://localhost:8000 \
  -Dduration=45 \
  -DauthToken=$AUTH_TOKEN
```

**Expected Results**:
- Concurrent Users: 15,000 (150% capacity)
- Response Time p99: <2000ms (relaxed under stress)
- Error Rate: <5% (acceptable under extreme stress)
- Duration: 45 minutes
- Circuit breaker activation expected

#### 4. Endurance Test (Long Duration)

**Objective**: Detect memory leaks and gradual performance degradation.

```bash
./gradlew gatlingRun \
  -Dgatling.simulationClass=com.trademaster.trading.simulations.EnduranceTest \
  -DbaseUrl=http://localhost:8000 \
  -Dduration=240 \
  -DauthToken=$AUTH_TOKEN
```

**Expected Results**:
- Concurrent Users: 5,000 (sustained)
- Response Time p95: <200ms (stable over time)
- Error Rate: <0.1% (consistent)
- Duration: 4 hours
- No memory leaks or resource exhaustion

#### 5. Portfolio Retrieval Test

**Objective**: Validate read-heavy operations performance.

```bash
./gradlew gatlingRun \
  -Dgatling.simulationClass=com.trademaster.trading.simulations.PortfolioRetrievalLoadTest \
  -DbaseUrl=http://localhost:8000 \
  -DloadProfile=peak \
  -Dduration=30 \
  -DauthToken=$AUTH_TOKEN
```

**Expected Results**:
- Concurrent Users: 10,000
- Response Time p95: <150ms (reads are faster)
- Error Rate: <0.1%
- Duration: 30 minutes

---

## Test Configuration

### System Properties

All simulations support the following system properties:

| Property | Description | Default | Valid Values |
|----------|-------------|---------|--------------|
| `baseUrl` | Target server URL | `http://localhost:8080` | Any valid URL |
| `loadProfile` | Load intensity level | `normal` | `normal`, `peak`, `stress` |
| `duration` | Test duration (minutes) | Varies by test | 1-480 |
| `authToken` | JWT authentication token | `test-jwt-token` | Valid JWT |

### Load Profiles

**Normal Load** (50% Capacity):
- Users: 5,000
- Ramp-up: 5 minutes
- RPS: ~1,000 req/sec

**Peak Load** (100% Capacity):
- Users: 10,000
- Ramp-up: 10 minutes
- RPS: ~2,000 req/sec

**Stress Load** (150% Capacity):
- Users: 15,000
- Ramp-up: 10 minutes
- RPS: ~3,000 req/sec

### Gatling Configuration

Edit `src/gatling/resources/gatling.conf` to customize:

```hocon
gatling {
  core {
    connectionPoolSize = 500 # Adjust for higher concurrency
  }

  http {
    ahc {
      maxConnectionsPerHost = 500 # Match connection pool size
      requestTimeout = 60000 # Request timeout in ms
    }
  }

  charting {
    indicators {
      lowerBound = 200  # p95 target
      higherBound = 500 # p99 target
    }
  }
}
```

---

## Interpreting Results

### Gatling Reports

After each test run, Gatling generates detailed HTML reports at:

```
trading-service/build/reports/gatling/[simulation-name]-[timestamp]/
```

Open `index.html` in a browser to view:
- Response time distribution and percentiles
- Request/response throughput
- Success/failure rates
- Active users over time

### Key Metrics

#### Response Time Percentiles

| Metric | Target | Meaning |
|--------|--------|---------|
| p50 (Median) | <100ms | Half of requests faster than this |
| p75 | <150ms | 75% of requests faster than this |
| p95 | <200ms | 95% of requests faster than this (SLA) |
| p99 | <500ms | 99% of requests faster than this (SLA) |
| Max | <5000ms | Worst-case latency |

#### Throughput Metrics

- **Requests/Second (RPS)**: Target 2,000+ at peak load
- **Response Bytes/Second**: Network bandwidth utilization
- **Concurrent Users**: Active users at any point in time

#### Error Rate Analysis

```
Error Rate = (Failed Requests / Total Requests) × 100%
```

**Acceptable Thresholds**:
- Normal/Peak Load: <0.1% (1 failure per 1,000 requests)
- Stress Load: <5% (acceptable under extreme conditions)

**Common Error Codes**:
- `429 Too Many Requests`: Rate limiting activated (expected under stress)
- `503 Service Unavailable`: Circuit breaker open (expected under stress)
- `500 Internal Server Error`: Application error (investigate immediately)
- `408 Request Timeout`: Slow responses exceeding timeout

---

## Performance Baseline

### Current Baseline (Phase 7 Integration Testing)

| Metric | Baseline | Phase 8 Target | Notes |
|--------|----------|----------------|-------|
| Order Processing | 50ms | <50ms | 10x faster with Virtual Threads |
| API Response p95 | 150ms | <200ms | Well within SLA |
| API Response p99 | 300ms | <500ms | Well within SLA |
| Concurrent Users | 10,000+ | 10,000+ | Validated in Phase 7 |
| Error Rate | <0.01% | <0.1% | Excellent reliability |
| Throughput | 2,000 RPS | 2,000+ RPS | Target sustained |

### Virtual Threads Performance

**Comparison: Platform Threads vs. Virtual Threads**

| Operation | Platform Threads | Virtual Threads | Improvement |
|-----------|-----------------|-----------------|-------------|
| Order Processing | 500ms | 50ms | **10x faster** |
| Concurrent Users | 1,000 | 10,000+ | **10x more** |
| API Response Time | 300ms | 150ms | **2x faster** |
| Memory Usage | Baseline | 90% of baseline | **10% reduction** |

---

## Monitoring During Load Tests

### Grafana Dashboards

Monitor these dashboards during load testing:

1. **Business Metrics Dashboard**
   - Orders placed per minute
   - Order success rate
   - Trading volume

2. **Technical Performance Dashboard**
   - API response time percentiles
   - Request throughput
   - Error rates

3. **Infrastructure Dashboard**
   - CPU utilization (<70% target)
   - Memory usage (<80% target)
   - JVM garbage collection

4. **Circuit Breaker Dashboard**
   - Circuit breaker states
   - Failure rates
   - Fallback activations

### Prometheus Queries

```promql
# API response time p95
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))

# Request throughput
rate(http_server_requests_total[1m])

# Error rate percentage
(sum(rate(http_server_requests_total{status=~"5.."}[5m])) / sum(rate(http_server_requests_total[5m]))) * 100

# Active Virtual Threads
jvm_threads_live_threads{name="VirtualThreadScheduler"}

# JVM heap usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### Real-Time Monitoring Commands

```bash
# Watch API response time
watch -n 1 "curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements'"

# Monitor JVM heap
watch -n 5 "curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq"

# Watch active threads
watch -n 2 "curl -s http://localhost:8080/actuator/metrics/jvm.threads.live | jq"

# Monitor error rate
watch -n 1 "curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements[] | select(.statistic == \"COUNT\")'"
```

---

## Troubleshooting

### Common Issues

#### 1. High Response Times

**Symptoms**: p95 > 200ms or p99 > 500ms

**Possible Causes**:
- Database connection pool exhaustion
- Slow database queries
- Insufficient CPU or memory
- Network latency

**Investigation Steps**:

```bash
# Check database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Identify slow queries
curl http://localhost:8080/actuator/metrics/spring.data.repository.invocations | jq

# Check JVM GC pressure
curl http://localhost:8080/actuator/metrics/jvm.gc.pause | jq
```

**Solutions**:
- Increase database connection pool size in `application.yml`
- Optimize slow queries with proper indexing
- Increase JVM heap size: `-Xmx4g`
- Enable Virtual Threads: `spring.threads.virtual.enabled=true`

#### 2. Circuit Breaker Activation

**Symptoms**: 503 errors, circuit breaker dashboard shows "OPEN" state

**Expected Behavior**: Circuit breakers should open under stress to protect downstream services

**Investigation Steps**:

```bash
# Check circuit breaker state
curl http://localhost:8080/actuator/circuitbreakers

# View circuit breaker metrics
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

**Solutions**:
- Verify circuit breaker is configured correctly
- Check if downstream services are healthy
- Adjust circuit breaker thresholds if too aggressive
- Implement proper fallback mechanisms

#### 3. Memory Exhaustion

**Symptoms**: OutOfMemoryError, increasing heap usage over time

**Investigation Steps**:

```bash
# Generate heap dump for analysis
jmap -dump:format=b,file=heap.bin <pid>

# Analyze heap dump with Eclipse MAT
# Look for memory leaks and object retention

# Monitor heap usage trend
curl http://localhost:8080/actuator/metrics/jvm.memory.used | jq
```

**Solutions**:
- Increase JVM heap size: `-Xmx8g`
- Enable G1GC for better garbage collection: `-XX:+UseG1GC`
- Fix memory leaks in application code
- Review object creation patterns

#### 4. Database Connection Pool Exhaustion

**Symptoms**: Connection timeout errors, slow response times

**Investigation Steps**:

```bash
# Check active connections
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq

# Check connection pool configuration
grep -A 10 "spring.datasource.hikari" src/main/resources/application.yml
```

**Solutions**:

```yaml
# Increase connection pool size in application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 100 # Increase from default 10
      minimum-idle: 20
      connection-timeout: 30000
```

#### 5. Gatling Load Generator Issues

**Symptoms**: Gatling process crashes, "Too many open files" error

**Solutions**:

```bash
# Increase file descriptor limit (Linux/Mac)
ulimit -n 65536

# Increase JVM memory for Gatling
export JAVA_OPTS="-Xms2g -Xmx8g"

# Run with increased resources
./gradlew gatlingRun -Dorg.gradle.jvmargs="-Xmx8g"
```

---

## Performance Optimization

### Pre-Test Optimization

1. **Database Indexing**

```sql
-- Ensure proper indexes exist
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_portfolios_user_id ON portfolios(user_id);
```

2. **Connection Pool Tuning**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 100
      minimum-idle: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

3. **JVM Tuning**

```bash
# Enable G1GC for low-latency
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication

# Heap size
-Xms4g -Xmx8g

# GC logging
-Xlog:gc*:file=gc.log:time,uptime:filecount=10,filesize=100M
```

4. **Virtual Threads Configuration**

```yaml
spring:
  threads:
    virtual:
      enabled: true # MANDATORY for TradeMaster
```

### Post-Test Optimization

Based on load test results, apply targeted optimizations:

**If response times are high**:
- Optimize slow database queries
- Add caching for frequently accessed data
- Increase connection pool size

**If error rates are high**:
- Review circuit breaker configurations
- Implement retry mechanisms
- Add request throttling

**If memory usage is growing**:
- Analyze heap dumps for memory leaks
- Optimize object creation patterns
- Tune garbage collection

---

## Continuous Performance Testing

### Automated Load Testing in CI/CD

Integrate load testing into GitHub Actions pipeline:

```yaml
# .github/workflows/load-test.yml
name: Load Testing

on:
  schedule:
    - cron: '0 2 * * 0' # Weekly on Sunday at 2 AM
  workflow_dispatch: # Manual trigger

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Java 24
        uses: actions/setup-java@v3
        with:
          java-version: '24'
          distribution: 'temurin'

      - name: Start TradeMaster Services
        run: docker-compose up -d

      - name: Run Load Tests
        run: |
          ./gradlew gatlingRun \
            -DbaseUrl=http://localhost:8000 \
            -DloadProfile=normal \
            -Dduration=15

      - name: Archive Gatling Reports
        uses: actions/upload-artifact@v3
        with:
          name: gatling-reports
          path: build/reports/gatling/

      - name: Performance Regression Check
        run: |
          # Compare with baseline metrics
          python scripts/check-performance-regression.py
```

### Performance Baseline Tracking

Track performance metrics over time:

```bash
# Extract key metrics from Gatling report
./scripts/extract-performance-metrics.sh > metrics/baseline-$(date +%Y%m%d).json

# Compare with previous baseline
./scripts/compare-baselines.sh metrics/baseline-*.json
```

---

## Load Testing Best Practices

### 1. Test Environment

- **Use staging environment**: Never run stress tests against production
- **Isolate test environment**: Ensure no other traffic during testing
- **Match production config**: Use production-like data and configuration

### 2. Test Data Management

- **Realistic data**: Use production-like data volumes and patterns
- **Data cleanup**: Clean up test data after each run
- **Test data generation**: Use scripts to generate realistic test data

### 3. Gradual Load Increase

- **Ramp-up period**: Use 5-10 minute ramp-up to avoid sudden spikes
- **Progressive testing**: Start with normal load before peak/stress
- **Monitor during ramp-up**: Watch for early warning signs

### 4. Result Analysis

- **Compare with baseline**: Track performance trends over time
- **Investigate anomalies**: Analyze outliers and unexpected behavior
- **Document findings**: Record all observations and optimizations

### 5. Post-Test Actions

- **Archive reports**: Save Gatling reports for historical comparison
- **Update baselines**: Update performance baselines after optimizations
- **Share results**: Communicate findings with development team

---

## Appendix

### A. Gatling Simulation Structure

```scala
class MyLoadTest extends Simulation {
  // HTTP protocol configuration
  val httpProtocol = http.baseUrl("http://localhost:8080")

  // Test data feeder
  val feeder = Iterator.continually(Map("data" -> "value"))

  // Scenario definition
  val scenario = scenario("My Scenario")
    .feed(feeder)
    .exec(http("Request").get("/api/endpoint"))

  // Load profile
  setUp(scenario.inject(rampUsers(1000).during(5.minutes)))
    .protocols(httpProtocol)
    .assertions(global.responseTime.percentile3.lt(200))
}
```

### B. Common Gatling Commands

```bash
# List available simulations
./gradlew gatlingRun --list

# Run specific simulation
./gradlew gatlingRun-com.trademaster.trading.simulations.OrderPlacementLoadTest

# Run with custom JVM options
./gradlew gatlingRun -Dorg.gradle.jvmargs="-Xmx8g -XX:+UseG1GC"

# Generate reports from existing results
./gradlew gatlingReport
```

### C. Performance Testing Checklist

**Before Testing**:
- [ ] Staging environment deployed and stable
- [ ] Database seeded with realistic test data
- [ ] Monitoring dashboards configured
- [ ] Authentication tokens generated
- [ ] Load generator has sufficient resources

**During Testing**:
- [ ] Monitor response times in real-time
- [ ] Watch error rates and circuit breaker states
- [ ] Track resource utilization (CPU, memory, disk)
- [ ] Observe database connection pool health
- [ ] Check JVM garbage collection metrics

**After Testing**:
- [ ] Archive Gatling HTML reports
- [ ] Extract key performance metrics
- [ ] Compare with baseline metrics
- [ ] Document performance improvements/regressions
- [ ] Create tickets for identified bottlenecks
- [ ] Update performance baselines

---

## Support

For issues or questions about load testing:

- **Slack**: #trademaster-performance
- **Email**: performance-team@trademaster.com
- **Documentation**: https://docs.trademaster.com/performance
- **Gatling Docs**: https://gatling.io/docs/

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-15
**Authors**: TradeMaster Development Team
