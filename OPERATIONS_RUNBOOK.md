# TradeMaster Trading Service Operations Runbook

## Overview

Comprehensive operations guide for TradeMaster Trading Service production support, incident response, and routine maintenance.

**Service**: TradeMaster Trading Service
**Criticality**: HIGH (24/7 Trading Platform)
**On-Call**: PagerDuty rotation
**Support Channels**: #trademaster-critical, #trademaster-warnings

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Common Incidents](#common-incidents)
3. [Alert Runbooks](#alert-runbooks)
4. [Maintenance Procedures](#maintenance-procedures)
5. [Performance Optimization](#performance-optimization)
6. [Incident Response](#incident-response)
7. [Escalation Matrix](#escalation-matrix)

---

## Architecture Overview

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Load Balancer (Kong)                     │
└──────────────────┬──────────────────────────────────────────┘
                   │
    ┌──────────────┼──────────────┐
    │              │              │
┌───▼────┐    ┌───▼────┐    ┌───▼────┐
│  Blue  │    │ Green  │    │Monitoring│
│  Env   │    │  Env   │    │  Stack  │
└───┬────┘    └───┬────┘    └───┬────┘
    │              │              │
┌───▼──────────────▼──────────────▼────┐
│       PostgreSQL │ Redis │ Kafka     │
└───────────────────────────────────────┘
```

### Technology Stack
- **Application**: Java 24, Spring Boot 3.4.1, Virtual Threads
- **Database**: PostgreSQL 15+
- **Cache**: Redis 7
- **Message Queue**: Apache Kafka 3.0+
- **API Gateway**: Kong
- **Monitoring**: Prometheus, Grafana, Alertmanager
- **Container**: Docker 20.10+

### Critical Dependencies
1. **PostgreSQL Database** - Primary data store
2. **Redis** - Session management and caching
3. **Kafka** - Event streaming and async processing
4. **External Broker APIs** - Order execution (Zerodha, Upstox, Angel One)
5. **Market Data Providers** - Real-time quotes (Alpha Vantage, NSE, BSE)
6. **Payment Gateway** - UPI, Card payments (Razorpay, Stripe)

---

## Common Incidents

### Incident 1: High Error Rate (5xx Errors)

**Symptoms**:
- Alert: `HighErrorRate` firing
- Grafana dashboard showing error spike
- PagerDuty notification

**Diagnosis**:
```bash
# Check error rate
curl -s "http://prometheus:9090/api/v1/query?query=rate(http_server_requests_seconds_count{status=~'5..'}[5m])" | jq

# View recent errors in logs
docker logs --tail 100 trademaster-trading-service | grep ERROR

# Check specific error types
curl -s "http://prometheus:9090/api/v1/query?query=topk(10,sum(rate(http_server_requests_seconds_count{status=~'5..'}[5m]))by(uri,status))" | jq
```

**Resolution**:
1. **Identify failing endpoint**: Check Grafana or Prometheus query
2. **Check circuit breaker states**:
   ```bash
   curl -s http://localhost:8080/actuator/circuitbreakers | jq '.circuitBreakers[] | select(.state=="open")'
   ```
3. **Review application logs** for root cause
4. **Check database connectivity**:
   ```bash
   docker exec trademaster-trading-service psql -h postgres -U trademaster_user -c "SELECT 1"
   ```
5. **Verify external services**:
   ```bash
   curl -v http://localhost:8080/actuator/health | jq '.components'
   ```
6. **If widespread issue**: Consider rollback
   ```bash
   ./scripts/deployment/blue-green-deploy.sh --environment production --rollback
   ```

**Prevention**:
- Ensure circuit breakers are properly configured
- Add retry mechanisms with exponential backoff
- Improve error handling and logging
- Set up synthetic monitoring

---

### Incident 2: High API Latency

**Symptoms**:
- Alert: `HighAPILatencyP95` or `CriticalAPILatencyP99` firing
- Slow API responses reported by users
- Grafana showing latency spike

**Diagnosis**:
```bash
# Check current latency
curl -s "http://prometheus:9090/api/v1/query?query=histogram_quantile(0.95,rate(http_server_requests_seconds_bucket[5m]))*1000" | jq

# Identify slow endpoints
curl -s "http://prometheus:9090/api/v1/query?query=topk(10,histogram_quantile(0.95,rate(http_server_requests_seconds_bucket[5m])by(uri,le)))*1000" | jq

# Check database query performance
curl -s http://localhost:8080/actuator/metrics/spring.data.repository.invocations | jq
```

**Resolution**:
1. **Check slow database queries**:
   ```sql
   SELECT pid, now() - pg_stat_activity.query_start AS duration, query
   FROM pg_stat_activity
   WHERE (now() - pg_stat_activity.query_start) > interval '5 seconds'
   ORDER BY duration DESC;
   ```
2. **Review connection pool utilization**:
   ```bash
   curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq
   ```
3. **Check for memory pressure**:
   ```bash
   curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq
   ```
4. **Identify N+1 queries** in application logs
5. **Scale horizontally** if CPU/memory constrained:
   ```bash
   docker-compose up --scale trademaster-trading-service=3
   ```

**Prevention**:
- Add database indexes for frequently queried columns
- Implement query result caching
- Use database connection pooling
- Enable query logging for slow queries
- Implement APM (Application Performance Monitoring)

---

### Incident 3: Circuit Breaker Open

**Symptoms**:
- Alert: `CircuitBreakerOpen` firing
- External service calls failing
- Degraded functionality

**Diagnosis**:
```bash
# Check circuit breaker states
curl -s http://localhost:8080/actuator/circuitbreakers | jq '.circuitBreakers'

# Check failure rate
curl -s "http://prometheus:9090/api/v1/query?query=resilience4j_circuitbreaker_failure_rate" | jq

# View buffered calls
curl -s "http://prometheus:9090/api/v1/query?query=resilience4j_circuitbreaker_buffered_calls" | jq
```

**Resolution**:
1. **Identify affected service**: Check which circuit breaker is open
2. **Test external service manually**:
   ```bash
   # Example for broker service
   curl -v https://api.zerodha.com/v1/orders
   ```
3. **Review external service status pages**
4. **Wait for automatic recovery** (circuit breaker will transition to HALF_OPEN)
5. **Manual circuit breaker reset** (if needed):
   ```bash
   curl -X POST http://localhost:8080/actuator/circuitbreakers/brokerService/transition/forceOpen
   curl -X POST http://localhost:8080/actuator/circuitbreakers/brokerService/transition/forceClosed
   ```
6. **Implement fallback mechanism** if not already present

**Prevention**:
- Configure appropriate failure thresholds
- Implement graceful degradation
- Set up monitoring for external service health
- Establish SLA agreements with external providers

---

### Incident 4: Database Connection Pool Exhausted

**Symptoms**:
- Alert: `DatabaseConnectionPoolExhaustion` firing
- Errors: "Unable to acquire JDBC Connection"
- Application hanging or timing out

**Diagnosis**:
```bash
# Check pool metrics
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections | jq

# View active connections
docker exec trademaster-postgres psql -U trademaster_user -d trademaster_trading -c "SELECT count(*) FROM pg_stat_activity WHERE state = 'active';"

# Check for long-running queries
docker exec trademaster-postgres psql -U trademaster_user -d trademaster_trading -c "SELECT pid, now() - pg_stat_activity.query_start AS duration, query FROM pg_stat_activity WHERE (now() - pg_stat_activity.query_start) > interval '10 seconds';"
```

**Resolution**:
1. **Kill long-running queries**:
   ```sql
   SELECT pg_terminate_backend(pid) FROM pg_stat_activity
   WHERE (now() - pg_stat_activity.query_start) > interval '30 seconds'
   AND state = 'active';
   ```
2. **Increase connection pool size** (temporary):
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20  # Increase from 10
   ```
3. **Restart application** to reset connection pool:
   ```bash
   docker-compose restart trademaster-trading-service
   ```
4. **Review and optimize slow queries**
5. **Check for connection leaks** in application code

**Prevention**:
- Ensure proper connection management (@Transactional)
- Set appropriate connection pool size based on load
- Configure connection timeout and idle timeout
- Monitor connection pool metrics continuously

---

### Incident 5: Deployment Failure

**Symptoms**:
- GitHub Actions workflow failing
- Health checks failing during deployment
- Automatic rollback triggered

**Diagnosis**:
```bash
# Check GitHub Actions logs
gh run list --limit 5
gh run view <run-id> --log

# Review deployment logs
cat logs/deployment-*.log

# Check health endpoint
curl -v http://localhost:8080/actuator/health
```

**Resolution**:
1. **Review workflow logs** for specific failure
2. **Check quality gate failures**:
   - Security scans (OWASP, SonarQube)
   - Test failures (unit, integration)
   - Performance tests (Gatling)
3. **Verify Docker image build**:
   ```bash
   docker pull ghcr.io/trademaster/trading-service:${COMMIT_SHA}
   docker run -it ghcr.io/trademaster/trading-service:${COMMIT_SHA} java -version
   ```
4. **Test deployment locally**:
   ```bash
   docker-compose -f docker-compose.green.yml up -d
   ./scripts/deployment/smoke-tests.sh http://localhost:8080
   ```
5. **Fix issues and redeploy**:
   ```bash
   git commit --fixup HEAD
   git push origin main
   ```

**Prevention**:
- Run full test suite locally before pushing
- Use pre-commit hooks for validation
- Implement staged rollout (canary)
- Maintain comprehensive test coverage

---

## Alert Runbooks

### ApplicationDown

**Severity**: CRITICAL
**Description**: Application instance is not responding

**Immediate Actions**:
1. Check if process is running:
   ```bash
   docker ps -a | grep trademaster-trading-service
   ```
2. Check application logs:
   ```bash
   docker logs trademaster-trading-service
   ```
3. Check system resources:
   ```bash
   free -h
   df -h
   docker stats
   ```
4. Restart application:
   ```bash
   docker-compose restart trademaster-trading-service
   ```
5. If restart fails, rollback:
   ```bash
   ./scripts/deployment/blue-green-deploy.sh --environment production --rollback
   ```

**Follow-up**:
- Review logs for root cause
- Check for OOM killer activity: `dmesg | grep -i "killed process"`
- Update resource limits if needed

---

### OrderPlacementFailures

**Severity**: CRITICAL
**Description**: High rate of order placement failures

**Immediate Actions**:
1. Check broker connectivity:
   ```bash
   curl -s http://localhost:8080/actuator/health | jq '.components.brokerService'
   ```
2. Review order failure reasons:
   ```bash
   curl -s "http://prometheus:9090/api/v1/query?query=topk(10,sum(rate(orders_failed_total[5m]))by(reason))" | jq
   ```
3. Check circuit breaker for broker service:
   ```bash
   curl -s http://localhost:8080/actuator/circuitbreakers | jq '.circuitBreakers[] | select(.name=="brokerService")'
   ```
4. Verify API keys are valid
5. Check broker service status page

**Follow-up**:
- Implement retry mechanism with exponential backoff
- Add detailed logging for failure reasons
- Set up synthetic order placement tests

---

### SlowDatabaseQueries

**Severity**: WARNING
**Description**: Database queries taking too long

**Immediate Actions**:
1. Identify slow queries:
   ```sql
   SELECT query, mean_exec_time, calls
   FROM pg_stat_statements
   ORDER BY mean_exec_time DESC
   LIMIT 10;
   ```
2. Check missing indexes:
   ```sql
   SELECT schemaname, tablename, attname, n_distinct, correlation
   FROM pg_stats
   WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
   ORDER BY n_distinct DESC;
   ```
3. Analyze query execution plan:
   ```sql
   EXPLAIN ANALYZE <slow_query>;
   ```
4. Add appropriate indexes:
   ```sql
   CREATE INDEX CONCURRENTLY idx_orders_user_id_created_at
   ON orders(user_id, created_at DESC);
   ```

**Follow-up**:
- Review and optimize frequently run queries
- Set up automated query performance monitoring
- Schedule regular database maintenance (VACUUM, ANALYZE)

---

## Maintenance Procedures

### Daily Maintenance

**Morning Checklist** (10 AM IST):
- [ ] Review Grafana dashboards for overnight activity
- [ ] Check firing alerts (should be 0)
- [ ] Verify backup completion:
  ```bash
  ls -lh /var/backups/postgresql/full/ | tail -n 5
  ```
- [ ] Review application logs for errors
- [ ] Check circuit breaker states
- [ ] Validate system resources (CPU, memory, disk)

### Weekly Maintenance

**Every Monday** (11 AM IST):
- [ ] Review deployment metrics
- [ ] Analyze error trends from past week
- [ ] Review slow queries and optimize
- [ ] Check security scan results
- [ ] Update dependencies if needed
- [ ] Review incident postmortems
- [ ] Plan upcoming deployments

### Monthly Maintenance

**First Monday of Month**:
- [ ] Full system audit
- [ ] Database maintenance:
  ```sql
  VACUUM ANALYZE;
  REINDEX DATABASE trademaster_trading;
  ```
- [ ] Review and rotate API keys
- [ ] Update TLS certificates
- [ ] Disaster recovery test:
  ```bash
  ./scripts/disaster-recovery/test-application-rollback.sh full --dry-run
  ```
- [ ] Capacity planning review
- [ ] Security vulnerability scan
- [ ] Documentation review and updates

---

## Performance Optimization

### Database Optimization

**Identify Slow Queries**:
```sql
-- Enable pg_stat_statements extension
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- View top 10 slowest queries
SELECT query, mean_exec_time, calls, total_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

**Add Indexes**:
```sql
-- Orders table
CREATE INDEX CONCURRENTLY idx_orders_user_id_status ON orders(user_id, status);
CREATE INDEX CONCURRENTLY idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX CONCURRENTLY idx_orders_symbol ON orders(symbol);

-- Positions table
CREATE INDEX CONCURRENTLY idx_positions_portfolio_id ON positions(portfolio_id);
CREATE INDEX CONCURRENTLY idx_positions_symbol ON positions(symbol);

-- Transactions table
CREATE INDEX CONCURRENTLY idx_transactions_user_id_date ON transactions(user_id, transaction_date DESC);
```

**Connection Pooling**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Application Optimization

**JVM Tuning**:
```bash
JAVA_OPTS="-Xms2g -Xmx4g \
  -XX:+UseZGC \
  -XX:+UseStringDeduplication \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/java_heapdump.hprof"
```

**Virtual Threads Configuration**:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

**Caching Strategy**:
```java
@Cacheable(value = "marketData", key = "#symbol")
public MarketData getMarketData(String symbol) {
    // Implementation
}
```

---

## Incident Response

### Severity Levels

**P0 - Critical** (Response: Immediate, PagerDuty):
- Service completely down
- Data loss or corruption
- Security breach
- Payment processing failures

**P1 - High** (Response: < 30 minutes):
- Degraded service affecting multiple users
- High error rates (>1%)
- Database connectivity issues
- Circuit breakers open

**P2 - Medium** (Response: < 2 hours):
- Performance degradation
- Non-critical feature failures
- Elevated latency
- Warning-level alerts

**P3 - Low** (Response: Next business day):
- Minor issues
- Documentation updates
- Enhancement requests

### Incident Response Process

1. **Acknowledge**: Acknowledge PagerDuty alert
2. **Assess**: Determine severity and impact
3. **Communicate**: Post in #trademaster-critical
4. **Diagnose**: Use runbooks to identify root cause
5. **Resolve**: Implement fix or rollback
6. **Verify**: Test resolution thoroughly
7. **Document**: Create incident report
8. **Postmortem**: Schedule within 48 hours

### Incident Communication Template

```
🚨 **INCIDENT ALERT** 🚨

**Severity**: P0 / P1 / P2 / P3
**Status**: INVESTIGATING / IDENTIFIED / RESOLVED
**Impact**: <description of user impact>
**Time**: <incident start time>

**Current Status**:
<what's happening>

**Actions Taken**:
- <action 1>
- <action 2>

**Next Steps**:
- <next step>

**ETA to Resolution**: <estimate>

**Incident Commander**: @<name>
```

---

## Escalation Matrix

### Level 1: On-Call Engineer
- **Response Time**: Immediate
- **Authority**: Restart services, run runbooks
- **Escalate When**: Unable to resolve within 30 minutes

### Level 2: Senior Engineer / Team Lead
- **Response Time**: < 15 minutes
- **Authority**: Code changes, configuration updates
- **Escalate When**: Requires architectural changes

### Level 3: Engineering Manager
- **Response Time**: < 30 minutes
- **Authority**: Resource allocation, external vendor escalation
- **Escalate When**: Business decision required

### Level 4: CTO / VP Engineering
- **Response Time**: < 1 hour
- **Authority**: Executive decisions, major incident declaration
- **Escalate When**: Company-wide impact

### External Escalation

**Database Provider** (AWS RDS):
- AWS Support: Priority ticket
- Phone: +1-866-216-2299

**Infrastructure Provider** (AWS):
- AWS Support Console
- Phone: +1-866-216-2299

**External Services**:
- **Zerodha**: support@zerodha.com
- **Upstox**: support@upstox.com
- **Razorpay**: support@razorpay.com

---

## Quick Reference

### Health Check URLs
- **Application**: http://localhost:8080/actuator/health
- **Prometheus**: http://localhost:9090/-/healthy
- **Grafana**: http://localhost:3000/api/health
- **Alertmanager**: http://localhost:9093/-/healthy

### Log Locations
- **Application**: `docker logs trademaster-trading-service`
- **Deployment**: `./logs/deployment-*.log`
- **Database**: `/var/log/postgresql/postgresql-15-main.log`
- **Nginx**: `/var/log/nginx/access.log`

### Useful Commands
```bash
# Check service status
docker-compose ps

# View logs
docker-compose logs -f trademaster-trading-service

# Restart service
docker-compose restart trademaster-trading-service

# Check metrics
curl -s http://localhost:8080/actuator/metrics | jq

# Database connection
docker exec -it trademaster-postgres psql -U trademaster_user -d trademaster_trading

# Redis connection
docker exec -it trademaster-redis redis-cli

# Trigger manual deployment
gh workflow run ci-cd-pipeline.yml --ref main
```

---

**Runbook Version**: 1.0.0
**Last Updated**: 2025-01-17
**Next Review**: 2025-02-17
**Maintained By**: TradeMaster DevOps Team

**For urgent issues, contact**: #trademaster-critical or page via PagerDuty
