# TradeMaster Monitoring & Alerting Setup Guide

## Overview

Comprehensive monitoring and alerting infrastructure for TradeMaster Trading Service using Prometheus, Grafana, and Alertmanager with PagerDuty and Slack integrations.

**Monitoring Stack Components**:
- **Prometheus**: Metrics collection and storage (30-day retention)
- **Grafana**: Visualization with 4 pre-configured dashboards
- **Alertmanager**: Alert routing to PagerDuty and Slack
- **Node Exporter**: Host-level metrics (CPU, memory, disk, network)
- **cAdvisor**: Container metrics (Docker resource usage)

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Component Configuration](#component-configuration)
3. [Dashboard Overview](#dashboard-overview)
4. [Alert Configuration](#alert-configuration)
5. [Integration Setup](#integration-setup)
6. [Monitoring Best Practices](#monitoring-best-practices)
7. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- TradeMaster Trading Service running with Prometheus metrics exposed
- PagerDuty service key (for critical alerts)
- Slack webhook URL (for all alerts)

### Step 1: Configure Environment Variables

```bash
# Create monitoring/.env file
cat > monitoring/.env << 'EOF'
# PagerDuty Configuration
PAGERDUTY_SERVICE_KEY=your-pagerduty-service-key-here

# Slack Configuration
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL

# Grafana Configuration
GF_SECURITY_ADMIN_PASSWORD=your-secure-admin-password
EOF
```

### Step 2: Start Monitoring Stack

```bash
cd monitoring

# Start all monitoring components
docker-compose -f docker-compose.monitoring.yml up -d

# Verify all services are healthy
docker-compose -f docker-compose.monitoring.yml ps

# Check logs
docker-compose -f docker-compose.monitoring.yml logs -f
```

### Step 3: Access Dashboards

**Prometheus**:
- URL: http://localhost:9090
- Verify targets: http://localhost:9090/targets
- View alerts: http://localhost:9090/alerts

**Grafana**:
- URL: http://localhost:3000
- Default credentials: `admin` / `admin` (change immediately)
- Pre-loaded dashboards: "TradeMaster" folder

**Alertmanager**:
- URL: http://localhost:9093
- View active alerts: http://localhost:9093/#/alerts
- Silence alerts: http://localhost:9093/#/silences

### Step 4: Verify Data Collection

```bash
# Check if metrics are being scraped
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job, health, lastScrape}'

# Query sample metrics
curl -s http://localhost:9090/api/v1/query?query=up | jq .

# Test alert rules
curl -s http://localhost:9090/api/v1/rules | jq '.data.groups[] | {name, file}'
```

---

## Component Configuration

### Prometheus Configuration

**File**: `monitoring/prometheus/prometheus.yml`

**Key Settings**:
- **Scrape Interval**: 15 seconds (balances precision vs. resource usage)
- **Retention**: 30 days, 50GB max
- **Targets**: Trading service, PostgreSQL, Redis, Kafka exporters

**Reload Configuration**:
```bash
# Hot reload configuration without restart
curl -X POST http://localhost:9090/-/reload

# Or restart Prometheus
docker-compose -f docker-compose.monitoring.yml restart prometheus
```

### Alert Rules

**File**: `monitoring/prometheus/alert-rules.yml`

**Alert Categories** (42 total alerts):
1. **Application Health** (4 alerts): ApplicationDown, HealthCheckFailing, HighErrorRate, ApplicationRestarted
2. **Performance & Latency** (4 alerts): HighAPILatencyP95, CriticalAPILatencyP99, SlowOrderProcessing, HighThreadPoolUtilization
3. **Business Metrics** (4 alerts): OrderPlacementFailures, TradeExecutionDelays, LowOrderVolume, PortfolioCalculationErrors
4. **Database** (4 alerts): DatabaseConnectionPoolExhaustion, SlowDatabaseQueries, DatabaseConnectionErrors, DataConsistencyFailures
5. **Circuit Breakers** (3 alerts): CircuitBreakerOpen, HighCircuitBreakerFailureRate, CircuitBreakerHalfOpen
6. **Security** (4 alerts): HighAuthenticationFailureRate, SuspiciousAuthenticationActivity, UnauthorizedAccessAttempts, JWTTokenValidationFailures
7. **Infrastructure** (6 alerts): HighCPUUsage, HighMemoryUsage, FrequentGarbageCollections, DiskSpaceLow, HighNetworkErrorRate, RedisConnectionFailures, KafkaConsumerLag

**Test Alert Rules**:
```bash
# Check alert rule syntax
promtool check rules monitoring/prometheus/alert-rules.yml

# Simulate alert firing (in Grafana or Prometheus UI)
# Or manually trigger test alert:
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[{
    "labels": {
      "alertname": "TestAlert",
      "severity": "warning"
    },
    "annotations": {
      "summary": "Test alert for validation"
    }
  }]'
```

### Alertmanager Configuration

**File**: `monitoring/alertmanager/alertmanager.yml`

**Routing Strategy**:
- **Critical Alerts** → PagerDuty + Slack (#trademaster-critical)
- **Warning Alerts** → Slack (#trademaster-warnings)
- **Info Alerts** → Slack (#trademaster-info)

**Inhibition Rules**:
- Critical alerts suppress warnings for same instance
- ApplicationDown suppresses component-level alerts

**Edit Alertmanager Config**:
```bash
# Validate configuration
docker run --rm -v $(pwd)/monitoring/alertmanager:/etc/alertmanager \
  prom/alertmanager:latest amtool check-config /etc/alertmanager/alertmanager.yml

# Reload Alertmanager
curl -X POST http://localhost:9093/-/reload
```

### Grafana Dashboards

**Location**: `monitoring/grafana/dashboards/`

**4 Pre-Configured Dashboards**:

#### 1. Application Health & Performance
- **UID**: `application-health`
- **Refresh**: 30s
- **Panels**: 11 panels covering status, latency, memory, CPU, GC

**Key Metrics**:
- Application UP/DOWN status
- Health check status (HEALTHY/UNHEALTHY)
- Request rate and response times (p95, p99)
- JVM memory usage (heap used vs. max)
- Thread pool utilization
- HTTP status code distribution
- GC pause times

#### 2. Trading Operations
- **UID**: `trading-operations`
- **Refresh**: 10s
- **Panels**: 13 panels focused on business metrics

**Key Metrics**:
- Orders placed rate (orders/sec)
- Order success rate (target: >99%)
- Order processing latency (p95 target: <50ms)
- Trade execution volume (₹)
- Orders by type (MARKET, LIMIT, STOP_LOSS)
- Portfolio update rate
- PnL calculation rate and errors
- Top trading symbols by volume
- Order failure reasons breakdown

#### 3. Database Performance
- **UID**: `database-performance`
- **Refresh**: 30s
- **Panels**: 8 panels for database monitoring

**Key Metrics**:
- Connection pool utilization (target: <90%)
- Active vs. idle connections
- Connection wait time (p95)
- Query execution time (p95, p99)
- Connection creation rate
- Connection errors (creation, timeout)
- Top slow queries

#### 4. Circuit Breakers & Resilience
- **UID**: `circuit-breakers`
- **Refresh**: 10s
- **Panels**: 8 panels for resilience monitoring

**Key Metrics**:
- Circuit breaker states (CLOSED/OPEN/HALF_OPEN)
- Failure rate by circuit breaker
- Call rates (successful vs. failed)
- Buffered calls
- Slow call rate
- State transition events
- Not permitted calls (blocked)

---

## Alert Configuration

### Alert Severity Levels

**Critical** (PagerDuty + Slack):
- System down or data loss risk
- Response required: Immediate
- Examples: ApplicationDown, DatabaseConnectionPoolExhaustion, OrderPlacementFailures

**Warning** (Slack only):
- Degraded performance or potential issues
- Response required: Within 1 hour
- Examples: HighAPILatencyP95, SlowDatabaseQueries, HighAuthenticationFailureRate

**Info** (Slack only):
- Informational, no immediate action
- Response required: Next business day
- Examples: CircuitBreakerHalfOpen, LowOrderVolume

### Alert Thresholds

| Metric | Warning | Critical | Duration |
|--------|---------|----------|----------|
| API Latency (p95) | >200ms | >500ms | 5 minutes |
| Order Processing | >50ms | >100ms | 5 minutes |
| Error Rate | >5 req/s | >10 req/s | 5 minutes |
| CPU Usage | >80% | >90% | 10 minutes |
| Memory Usage | >85% | >90% | 10 minutes |
| DB Pool Utilization | >80% | >90% | 5 minutes |
| Circuit Breaker Open | - | >2 minutes | 2 minutes |

### Customizing Alert Rules

**Example: Modify API Latency Threshold**

```yaml
# monitoring/prometheus/alert-rules.yml
- alert: HighAPILatencyP95
  expr: |
    histogram_quantile(0.95,
      rate(http_server_requests_seconds_bucket{job="trademaster-trading-service"}[5m])
    ) > 0.2  # Change this value (in seconds)
  for: 5m    # Change alert duration
  labels:
    severity: warning  # Change severity level
```

After modifying, reload Prometheus:
```bash
curl -X POST http://localhost:9090/-/reload
```

---

## Integration Setup

### PagerDuty Integration

**Step 1: Create PagerDuty Service**
1. Login to PagerDuty: https://app.pagerduty.com
2. Navigate to **Services** → **Create New Service**
3. Name: "TradeMaster Trading Critical Alerts"
4. Integration Type: "Prometheus"
5. Copy the **Service Key**

**Step 2: Configure Alertmanager**
```bash
# Update monitoring/.env
PAGERDUTY_SERVICE_KEY=your-service-key-here

# Restart Alertmanager
docker-compose -f docker-compose.monitoring.yml restart alertmanager
```

**Step 3: Test PagerDuty Integration**
```bash
# Trigger test alert
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[{
    "labels": {
      "alertname": "PagerDutyTest",
      "severity": "critical"
    },
    "annotations": {
      "summary": "PagerDuty integration test"
    }
  }]'

# Check PagerDuty incidents page
```

### Slack Integration

**Step 1: Create Slack Webhook**
1. Go to: https://api.slack.com/messaging/webhooks
2. Click **Create New Webhook**
3. Select workspace and channel: `#trademaster-alerts`
4. Copy the **Webhook URL**

**Step 2: Configure Alertmanager**
```bash
# Update monitoring/.env
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL

# Restart Alertmanager
docker-compose -f docker-compose.monitoring.yml restart alertmanager
```

**Step 3: Create Additional Slack Channels**
```
#trademaster-critical    - Critical alerts (PagerDuty + Slack)
#trademaster-warnings    - Warning alerts
#trademaster-info        - Informational alerts
#trademaster-database    - Database-specific alerts
#trademaster-trading     - Trading-specific alerts
```

**Step 4: Test Slack Integration**
```bash
# Send test message to Slack webhook
curl -X POST "${SLACK_WEBHOOK_URL}" \
  -H "Content-Type: application/json" \
  -d '{"text": "TradeMaster monitoring integration test"}'
```

---

## Monitoring Best Practices

### Dashboard Usage

**Daily Monitoring Routine**:
1. **Morning**: Check Application Health dashboard for overnight issues
2. **During Trading Hours**: Monitor Trading Operations dashboard (real-time)
3. **End of Day**: Review Database Performance for slow queries
4. **Weekly**: Review Circuit Breakers dashboard for resilience patterns

**Alert Fatigue Prevention**:
- Tune alert thresholds based on actual baseline performance
- Use inhibition rules to suppress redundant alerts
- Set appropriate `repeat_interval` (12h for critical, 4h for warnings)
- Silence non-actionable alerts during maintenance windows

### Metric Collection Best Practices

**Application Instrumentation**:
```java
// Example: Custom business metric
@Timed(value = "order.processing", percentiles = {0.5, 0.95, 0.99})
public Order processOrder(OrderRequest request) {
    // Order processing logic
}

// Example: Custom counter
@Counted(value = "orders.failed", extraTags = {"reason", "validation_error"})
public void handleOrderFailure(OrderRequest request, String reason) {
    // Failure handling
}
```

**Naming Conventions**:
- Use snake_case: `order_processing_duration_seconds`
- Include units: `_seconds`, `_bytes`, `_total`
- Use consistent prefixes: `http_`, `db_`, `kafka_`

### Performance Optimization

**Reduce Cardinality**:
```yaml
# Bad: High cardinality (unique user IDs)
orders_total{user_id="12345"}

# Good: Low cardinality (aggregated)
orders_total{user_type="premium"}
```

**Query Optimization**:
```promql
# Bad: Expensive query
rate(http_server_requests_seconds_count[1h])

# Good: Use recording rules for expensive queries
job:http_requests:rate5m
```

---

## Troubleshooting

### Prometheus Issues

**Problem**: Targets showing as DOWN

**Solution**:
```bash
# Check target health
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.health != "up")'

# Check network connectivity
docker exec trademaster-prometheus curl http://trademaster-trading-service:8080/actuator/prometheus

# Verify Docker network
docker network inspect trademaster-monitoring
```

**Problem**: High memory usage

**Solution**:
```bash
# Check TSDB status
curl -s http://localhost:9090/api/v1/status/tsdb

# Reduce retention time in docker-compose.monitoring.yml
--storage.tsdb.retention.time=15d  # Reduce from 30d
```

### Grafana Issues

**Problem**: Dashboards not loading

**Solution**:
```bash
# Check Grafana logs
docker-compose -f docker-compose.monitoring.yml logs grafana

# Verify datasource connection
curl -s -u admin:admin http://localhost:3000/api/datasources | jq .

# Re-provision dashboards
docker-compose -f docker-compose.monitoring.yml restart grafana
```

**Problem**: No data in panels

**Solution**:
```bash
# Test Prometheus query directly
curl -s 'http://localhost:9090/api/v1/query?query=up' | jq .

# Check time range in Grafana dashboard (top-right corner)
# Verify metric names match your application
```

### Alertmanager Issues

**Problem**: Alerts not being sent

**Solution**:
```bash
# Check Alertmanager status
curl -s http://localhost:9093/api/v1/status | jq .

# Verify alert is firing in Prometheus
curl -s http://localhost:9090/api/v1/alerts | jq '.data.alerts[] | select(.state == "firing")'

# Check Alertmanager logs
docker-compose -f docker-compose.monitoring.yml logs alertmanager | grep -i error

# Test notification manually
amtool alert add alertname=test severity=critical --alertmanager.url=http://localhost:9093
```

**Problem**: Duplicate alerts

**Solution**:
- Check `group_by` configuration in alertmanager.yml
- Verify alert labels are consistent
- Add inhibition rules for related alerts

---

## Maintenance

### Backup Prometheus Data

```bash
# Create snapshot
curl -X POST http://localhost:9090/api/v1/admin/tsdb/snapshot

# Backup volume
docker run --rm -v trademaster-prometheus-data:/data -v $(pwd):/backup \
  ubuntu tar czf /backup/prometheus-backup-$(date +%Y%m%d).tar.gz /data
```

### Upgrade Monitoring Stack

```bash
# Pull latest images
docker-compose -f docker-compose.monitoring.yml pull

# Recreate containers
docker-compose -f docker-compose.monitoring.yml up -d --force-recreate

# Verify upgrade
docker-compose -f docker-compose.monitoring.yml ps
```

### Clean Up Old Data

```bash
# Prometheus automatically handles retention
# Manual cleanup if needed:
curl -X POST http://localhost:9090/api/v1/admin/tsdb/delete_series?match[]={job="old-job"}
curl -X POST http://localhost:9090/api/v1/admin/tsdb/clean_tombstones
```

---

## Monitoring Checklist

### Initial Setup
- [ ] Environment variables configured (PagerDuty, Slack)
- [ ] Monitoring stack started and healthy
- [ ] All targets UP in Prometheus
- [ ] Grafana dashboards loaded
- [ ] Alertmanager routing tested
- [ ] PagerDuty integration verified
- [ ] Slack channels created and integrated

### Daily Operations
- [ ] Review Application Health dashboard
- [ ] Check for firing alerts
- [ ] Verify trading operations metrics
- [ ] Monitor database performance
- [ ] Check circuit breaker states

### Weekly Maintenance
- [ ] Review alert accuracy (false positives/negatives)
- [ ] Tune alert thresholds if needed
- [ ] Check Prometheus disk usage
- [ ] Review slow queries
- [ ] Update dashboards based on feedback

### Monthly Review
- [ ] Analyze incident response times
- [ ] Review on-call effectiveness
- [ ] Update runbooks based on incidents
- [ ] Optimize resource usage
- [ ] Plan capacity upgrades if needed

---

**Document Version**: 1.0.0
**Last Updated**: 2025-01-17
**Next Review**: 2025-02-17
**Maintained By**: TradeMaster DevOps Team
