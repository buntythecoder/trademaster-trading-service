# TradeMaster Disaster Recovery Guide

## Overview

Comprehensive disaster recovery (DR) procedures for TradeMaster Trading Service ensuring business continuity during system failures, data corruption, or infrastructure outages. This guide provides step-by-step recovery procedures with automated scripts for rapid restoration.

**Recovery Objectives**:
- **RTO (Recovery Time Objective)**: < 5 minutes
- **RPO (Recovery Point Objective)**: < 1 minute (with PITR)

---

## Table of Contents

1. [Disaster Recovery Scenarios](#disaster-recovery-scenarios)
2. [Database Backup & Restore](#database-backup--restore)
3. [Application Rollback Procedures](#application-rollback-procedures)
4. [Circuit Breaker Recovery](#circuit-breaker-recovery)
5. [Incident Response Workflow](#incident-response-workflow)
6. [DR Testing Schedule](#dr-testing-schedule)
7. [Post-Incident Analysis](#post-incident-analysis)

---

## Disaster Recovery Scenarios

### Scenario 1: Database Corruption

**Symptoms**:
- Database query failures
- Data integrity violations
- Transaction rollback errors
- Application errors related to database

**Recovery Steps**:
```bash
# Step 1: Verify database corruption
psql -h localhost -U trademaster_user -d trademaster_trading -c "SELECT 1"

# Step 2: Stop application to prevent further corruption
docker-compose stop trademaster-trading-service

# Step 3: Restore from latest backup
cd scripts/disaster-recovery
./restore-postgresql.sh latest

# Step 4: Verify restoration
./restore-postgresql.sh verify

# Step 5: Restart application
docker-compose up -d trademaster-trading-service

# Step 6: Monitor application logs
docker-compose logs -f trademaster-trading-service
```

**Expected RTO**: 3-5 minutes
**Expected RPO**: < 1 minute (with PITR)

---

### Scenario 2: Application Deployment Failure

**Symptoms**:
- Application crashes after deployment
- Health check failures
- Increased error rates
- Circuit breakers opening

**Recovery Steps**:
```bash
# Step 1: Verify deployment failure
curl -s http://localhost:8080/actuator/health | jq .

# Step 2: Check application logs
docker-compose logs --tail=100 trademaster-trading-service

# Step 3: Initiate blue-green rollback
cd scripts/disaster-recovery
./test-application-rollback.sh blue-green

# Step 4: Verify rollback success
curl -s http://localhost:8080/actuator/health | jq .

# Step 5: Validate application functionality
./test-application-rollback.sh verify
```

**Expected RTO**: 2-3 minutes
**Expected RPO**: 0 (no data loss)

---

### Scenario 3: External Service Outage

**Symptoms**:
- Circuit breakers opening
- Increased latency
- Timeouts connecting to external services
- Degraded application health

**Recovery Steps**:
```bash
# Step 1: Verify circuit breaker state
curl -s http://localhost:8080/actuator/circuitbreakers | jq .

# Step 2: Check metrics for affected services
curl -s http://localhost:8080/actuator/prometheus | grep resilience4j

# Step 3: Test circuit breaker recovery
cd scripts/disaster-recovery
./test-circuit-breaker-recovery.sh state-transition

# Step 4: Verify automatic recovery
./test-circuit-breaker-recovery.sh recovery

# Step 5: Monitor for cascading failures
./test-circuit-breaker-recovery.sh cascading
```

**Expected RTO**: 1-2 minutes (automatic recovery)
**Expected RPO**: 0 (fallback to cached data)

---

### Scenario 4: Configuration Error

**Symptoms**:
- Application startup failures
- Configuration validation errors
- Service connection failures
- Missing environment variables

**Recovery Steps**:
```bash
# Step 1: Identify configuration error
docker-compose logs trademaster-trading-service | grep -i "error\|exception"

# Step 2: Rollback configuration
cd scripts/disaster-recovery
./test-application-rollback.sh config

# Step 3: Verify configuration validity
./test-application-rollback.sh verify-config

# Step 4: Restart application
docker-compose restart trademaster-trading-service

# Step 5: Validate health checks
curl -s http://localhost:8080/actuator/health | jq .
```

**Expected RTO**: 1-2 minutes
**Expected RPO**: 0

---

### Scenario 5: Complete System Failure

**Symptoms**:
- All services down
- Infrastructure unavailable
- Data center outage
- Multiple component failures

**Recovery Steps**:
```bash
# Step 1: Assess scope of failure
./assess-system-health.sh

# Step 2: Restore infrastructure (if needed)
# Follow infrastructure DR procedures

# Step 3: Restore database from backup
cd scripts/disaster-recovery
./restore-postgresql.sh latest

# Step 4: Restore application (latest stable version)
docker-compose up -d

# Step 5: Full rollback if needed
./test-application-rollback.sh full --reason "Complete system failure"

# Step 6: Comprehensive verification
./verify-system-recovery.sh
```

**Expected RTO**: 5-10 minutes
**Expected RPO**: < 1 minute (with PITR)

---

## Database Backup & Restore

### Automated Backup Procedures

#### Full Backup (Daily)
```bash
cd scripts/disaster-recovery

# Create full encrypted backup
./backup-postgresql.sh full

# Verify backup integrity
./backup-postgresql.sh verify

# Upload to S3 cross-region
aws s3 sync /var/backups/postgresql s3://trademaster-backups/postgresql/
```

**Schedule**: Daily at 02:00 UTC
**Retention**: 7 daily, 4 weekly, 12 monthly

#### Point-in-Time Recovery (PITR) Setup
```bash
cd scripts/disaster-recovery

# Enable WAL archiving for PITR
./backup-postgresql.sh pitr

# Verify PITR is active
psql -c "SHOW archive_mode"
psql -c "SHOW wal_level"
```

### Restore Procedures

#### Full Restore
```bash
cd scripts/disaster-recovery

# Restore from latest backup
./restore-postgresql.sh latest

# Restore from specific backup
./restore-postgresql.sh full postgres_trademaster_20250117_120000.sql.gpg

# Restore from S3
aws s3 cp s3://trademaster-backups/postgresql/full/postgres_latest.sql.gpg .
./restore-postgresql.sh full postgres_latest.sql.gpg
```

#### Point-in-Time Recovery
```bash
cd scripts/disaster-recovery

# Restore to specific timestamp
./restore-postgresql.sh pitr postgres_base_20250117.tar.gz "2025-01-17 14:30:00"

# Restore to latest committed transaction
./restore-postgresql.sh pitr postgres_base_20250117.tar.gz "now"
```

### Backup Verification

**Pre-Production Restore Test** (Monthly):
```bash
# Test restore in isolated environment
export POSTGRES_HOST=test-db-server
export POSTGRES_DB=trademaster_trading_test

cd scripts/disaster-recovery
./restore-postgresql.sh latest
./verify-restore.sh

# Cleanup test environment
dropdb -h test-db-server trademaster_trading_test
```

---

## Application Rollback Procedures

### Blue-Green Deployment Rollback

**When to Use**: Deployment introduces bugs or performance regression

**Procedure**:
```bash
cd scripts/disaster-recovery

# Initiate blue-green rollback
./test-application-rollback.sh blue-green

# Monitor rollback progress
tail -f /var/log/trademaster/rollback_*.log

# Verify successful rollback
curl -s http://localhost:8080/actuator/health | jq .status
```

**Rollback Time**: 2-3 minutes

### Version Rollback

**When to Use**: Need to revert to specific previous version

**Procedure**:
```bash
cd scripts/disaster-recovery

# List available versions
docker images | grep trademaster-trading-service

# Rollback to specific version
export TARGET_VERSION=1.2.3
./test-application-rollback.sh version --target-version ${TARGET_VERSION}

# Verify version rollback
docker-compose ps | grep trademaster-trading-service
```

**Rollback Time**: 3-4 minutes

### Configuration Rollback

**When to Use**: Configuration change causes errors

**Procedure**:
```bash
cd scripts/disaster-recovery

# Rollback configuration
./test-application-rollback.sh config

# Verify configuration validity
./validate-config.sh

# Restart application
docker-compose restart trademaster-trading-service
```

**Rollback Time**: 1-2 minutes

### Database Schema Rollback

**When to Use**: Database migration causes errors

**Pre-requisites**: Rollback migration scripts exist in `db/migration/rollback/`

**Procedure**:
```bash
# Identify current schema version
psql -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1"

# Execute rollback migration
flyway migrate -target=<previous-version>

# Verify rollback success
flyway info

# Restart application
docker-compose restart trademaster-trading-service
```

---

## Circuit Breaker Recovery

### Automatic Recovery Process

Circuit breakers automatically recover through state transitions:

**State Machine**:
1. **CLOSED** (Normal) → **OPEN** (Failure threshold reached)
2. **OPEN** (Blocking calls) → **HALF_OPEN** (After wait duration)
3. **HALF_OPEN** (Testing) → **CLOSED** (Success) OR **OPEN** (Failure)

**Configuration**:
- Failure Rate Threshold: 50%
- Wait Duration (OPEN state): 10 seconds
- Permitted Calls (HALF_OPEN): 3 calls

### Manual Circuit Breaker Reset

**When to Use**: Circuit breaker stuck in OPEN state after external service recovery

**Procedure**:
```bash
# Check current circuit breaker state
curl -s http://localhost:8080/actuator/circuitbreakers | jq .

# Transition to HALF_OPEN manually
curl -X POST http://localhost:8080/actuator/circuitbreakers/<name>/transitionToHalfOpenState

# Force CLOSED (use with caution)
curl -X POST http://localhost:8080/actuator/circuitbreakers/<name>/transitionToClosedState

# Verify state transition
curl -s http://localhost:8080/actuator/circuitbreakers | jq '.circuitBreakers[] | {name, state}'
```

### Circuit Breaker Recovery Testing

**Regular Testing Schedule**: Weekly

```bash
cd scripts/disaster-recovery

# Test complete circuit breaker lifecycle
./test-circuit-breaker-recovery.sh all

# Test specific scenarios
./test-circuit-breaker-recovery.sh state-transition
./test-circuit-breaker-recovery.sh fallback
./test-circuit-breaker-recovery.sh recovery
./test-circuit-breaker-recovery.sh cascading
```

---

## Incident Response Workflow

### Phase 1: Detection & Assessment (0-5 minutes)

**Actions**:
1. **Alert Reception**: Receive alert from monitoring system
2. **Initial Assessment**:
   ```bash
   # Check overall system health
   curl -s http://localhost:8080/actuator/health | jq .

   # Check metrics
   curl -s http://localhost:8080/actuator/prometheus | grep -E "error|latency|circuit"

   # Check logs
   docker-compose logs --tail=50 trademaster-trading-service
   ```
3. **Severity Classification**:
   - **P0 (Critical)**: Complete system outage, data loss risk
   - **P1 (High)**: Major functionality impaired, degraded performance
   - **P2 (Medium)**: Partial functionality impaired, workarounds available
   - **P3 (Low)**: Minor issues, no business impact

### Phase 2: Containment (5-15 minutes)

**P0/P1 Actions**:
```bash
# Stop affected services
docker-compose stop <affected-service>

# Enable maintenance mode (if available)
curl -X POST http://localhost:8080/admin/maintenance-mode/enable

# Isolate problematic components
./isolate-component.sh <component-name>

# Notify stakeholders
./send-incident-notification.sh --severity P1 --message "System under maintenance"
```

### Phase 3: Recovery (15-25 minutes)

**Execute Recovery Plan**:
```bash
cd scripts/disaster-recovery

# Choose appropriate recovery procedure based on failure type
./test-application-rollback.sh [rollback-type]
# OR
./restore-postgresql.sh [restore-type]
# OR
./test-circuit-breaker-recovery.sh [recovery-type]

# Monitor recovery progress
tail -f /var/log/trademaster/*.log
```

### Phase 4: Verification (25-35 minutes)

**Validation Steps**:
```bash
# 1. Health checks
curl -s http://localhost:8080/actuator/health | jq .status

# 2. Smoke tests
./run-smoke-tests.sh

# 3. End-to-end test
./run-e2e-tests.sh --quick

# 4. Performance validation
./validate-performance.sh

# 5. Data integrity check
./check-data-integrity.sh
```

### Phase 5: Post-Incident (35-60 minutes)

**Documentation & Learning**:
1. **Incident Timeline**: Document all actions taken
2. **Root Cause Analysis**: Identify underlying cause
3. **Post-Mortem**: Schedule within 48 hours
4. **Action Items**: Create tickets for preventive measures
5. **Update Runbooks**: Improve DR procedures

---

## DR Testing Schedule

### Weekly Tests
- Circuit breaker recovery testing
- Configuration rollback validation
- Health check verification

### Monthly Tests
- Full database restore in test environment
- Blue-green deployment rollback
- Application version rollback
- End-to-end disaster recovery simulation

### Quarterly Tests
- Complete system recovery simulation
- Multi-region failover testing
- Load testing with failure injection
- Security incident response drill

### Annual Tests
- Comprehensive DR audit
- Third-party DR assessment
- Business continuity exercise
- Cross-team coordination drill

---

## Post-Incident Analysis

### Incident Report Template

```markdown
# Incident Report: [Incident ID]

## Summary
- **Incident Date**: YYYY-MM-DD HH:MM UTC
- **Severity**: P0/P1/P2/P3
- **Duration**: X hours Y minutes
- **Impact**: [Customer impact, revenue impact, affected services]

## Timeline
| Time (UTC) | Event | Action Taken |
|------------|-------|--------------|
| HH:MM | Incident detected | [Description] |
| HH:MM | [Event] | [Action] |
| HH:MM | Incident resolved | [Final action] |

## Root Cause Analysis
**Root Cause**: [Detailed explanation]

**Contributing Factors**:
1. [Factor 1]
2. [Factor 2]

## Recovery Actions Taken
1. [Action 1 - timestamp]
2. [Action 2 - timestamp]
3. [Action 3 - timestamp]

## Metrics
- **RTO Achieved**: X minutes
- **RPO Achieved**: Y seconds
- **Data Loss**: [Yes/No - amount if applicable]

## Action Items
| Item | Owner | Priority | Due Date | Status |
|------|-------|----------|----------|--------|
| [Action 1] | [Name] | High | YYYY-MM-DD | Open |
| [Action 2] | [Name] | Medium | YYYY-MM-DD | Open |

## Lessons Learned
**What Went Well**:
- [Item 1]
- [Item 2]

**What Needs Improvement**:
- [Item 1]
- [Item 2]

## Prevention Measures
1. [Prevention measure 1]
2. [Prevention measure 2]
```

---

## Disaster Recovery Metrics

### Key Performance Indicators

**RTO Metrics**:
- Target: < 5 minutes
- Actual: [Measured during DR tests]
- Trend: [Improving/Stable/Degrading]

**RPO Metrics**:
- Target: < 1 minute
- Actual: [Measured during DR tests]
- Trend: [Improving/Stable/Degrading]

**Backup Metrics**:
- Backup Success Rate: 100% target
- Backup Duration: < 30 minutes
- Restore Test Success Rate: 100% target

**Recovery Metrics**:
- Recovery Success Rate: 100% target
- Mean Time to Recover (MTTR): < 5 minutes
- Recovery Validation Time: < 10 minutes

### Monitoring Dashboard

**Grafana Dashboard**: `http://grafana:3000/d/disaster-recovery`

**Key Panels**:
- Backup Status & Trends
- Recovery Time Trends
- Circuit Breaker States
- System Health Score
- Alert Frequency

---

## Emergency Contacts

### On-Call Rotation

| Role | Primary Contact | Backup Contact | Escalation |
|------|----------------|----------------|------------|
| Incident Commander | [Name] | [Name] | [Name] |
| Database Admin | [Name] | [Name] | [Name] |
| DevOps Lead | [Name] | [Name] | [Name] |
| Security Lead | [Name] | [Name] | [Name] |
| Product Owner | [Name] | [Name] | [Name] |

### Communication Channels

- **PagerDuty**: `trademaster-trading-critical`
- **Slack**: `#trademaster-incidents`
- **Email**: `incidents@trademaster.com`
- **Video Conference**: [Link to war room]

---

## Appendix

### DR Scripts Reference

| Script | Purpose | Location |
|--------|---------|----------|
| `backup-postgresql.sh` | Database backup automation | `scripts/disaster-recovery/` |
| `restore-postgresql.sh` | Database restore automation | `scripts/disaster-recovery/` |
| `test-application-rollback.sh` | Application rollback testing | `scripts/disaster-recovery/` |
| `test-circuit-breaker-recovery.sh` | Circuit breaker recovery testing | `scripts/disaster-recovery/` |

### External Dependencies

- **PostgreSQL 15+**: Primary database
- **AWS S3**: Backup storage
- **Docker**: Container runtime
- **Nginx**: Load balancer
- **Prometheus**: Metrics collection
- **Grafana**: Monitoring dashboards

---

**Document Version**: 1.0.0
**Last Updated**: 2025-01-17
**Next Review**: 2025-04-17
**Maintained By**: TradeMaster DevOps Team
