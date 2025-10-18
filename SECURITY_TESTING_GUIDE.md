# TradeMaster Security Testing Guide

## Overview

This guide provides comprehensive instructions for performing security testing on TradeMaster trading platform. Security testing ensures the platform meets enterprise security standards and protects against OWASP Top 10 vulnerabilities before production deployment.

### Security Testing Objectives

1. **Identify Vulnerabilities** - Discover security weaknesses in code, dependencies, and configuration
2. **Validate Security Controls** - Ensure authentication, authorization, and encryption work correctly
3. **Test for OWASP Top 10** - Validate protection against common web application vulnerabilities
4. **Dependency Security** - Scan for known CVEs in third-party libraries
5. **Container Security** - Validate Docker image security and configuration

---

## Prerequisites

### Required Tools

1. **OWASP Dependency Check** (included in Gradle build)
2. **SonarQube** (https://www.sonarqube.org/)
3. **OWASP ZAP** (https://www.zaproxy.org/download/)
4. **Trivy** (https://github.com/aquasecurity/trivy) - Container scanning
5. **Docker** - For running security tools
6. **jq** - JSON processing for reports

### Installation

```bash
# Install OWASP ZAP (Mac)
brew install --cask owasp-zap

# Install OWASP ZAP (Linux - using Docker)
docker pull ghcr.io/zaproxy/zaproxy:stable

# Install Trivy for container scanning
brew install aquasecurity/trivy/trivy # Mac
# Or: https://aquasecurity.github.io/trivy/latest/getting-started/installation/

# Install jq for JSON processing
brew install jq # Mac
sudo apt-get install jq # Linux
```

### Environment Setup

```bash
# Set environment variables
export SONAR_HOST_URL="http://localhost:9000"
export SONAR_TOKEN="your-sonarqube-token"
export NVD_API_KEY="your-nvd-api-key" # Optional for faster CVE updates

# Ensure services are running
docker-compose up -d

# Verify services are accessible
curl http://localhost:8080/actuator/health
```

---

## Quick Start

### Run All Security Tests

```bash
# Run comprehensive security test suite
./scripts/security/run-security-tests.sh

# Run with specific options
./scripts/security/run-security-tests.sh \
  --target-url http://localhost:8000 \
  --skip-sonarqube
```

### Individual Security Tests

```bash
# 1. OWASP Dependency Check
./gradlew dependencyCheckAnalyze

# 2. SonarQube Analysis
./gradlew sonarqube

# 3. OWASP ZAP Scanning
./scripts/security/owasp-zap-scan.sh http://localhost:8080 baseline

# 4. Container Security
trivy image trademaster-trading-service:latest
```

---

## Automated Security Scanning

### 1. OWASP Dependency Check

Scans dependencies for known CVE vulnerabilities from the National Vulnerability Database (NVD).

#### Running Dependency Check

```bash
# Basic scan
./gradlew dependencyCheckAnalyze

# With NVD API key (faster updates)
NVD_API_KEY=your-key ./gradlew dependencyCheckAnalyze

# View report
open build/reports/dependency-check-report.html
```

#### Configuration

Edit `build.gradle` to customize:

```groovy
dependencyCheck {
    failBuildOnCVSS = 7.0 // Fail on High/Critical (CVSS >= 7.0)
    formats = ['HTML', 'JSON', 'XML']
    suppressionFile = file('owasp-dependency-check-suppressions.xml')
}
```

#### Interpreting Results

**Vulnerability Severity** (CVSS Score):
- **Critical**: 9.0 - 10.0 (Immediate action required)
- **High**: 7.0 - 8.9 (Address before production)
- **Medium**: 4.0 - 6.9 (Address in sprint)
- **Low**: 0.1 - 3.9 (Address when possible)

**Common Issues**:
- Transitive dependencies with CVEs
- Outdated library versions
- Known vulnerabilities in Spring Boot

**Remediation**:
```bash
# Update dependency versions
./gradlew dependencyUpdates

# Suppress false positives
# Create owasp-dependency-check-suppressions.xml
```

### 2. SonarQube Analysis

Static code analysis for security vulnerabilities, code smells, and technical debt.

#### Running SonarQube

```bash
# Start SonarQube (if using Docker)
docker run -d --name sonarqube \
  -p 9000:9000 \
  sonarqube:lts-community

# Run analysis
./gradlew sonarqube \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=your-token

# View results
open http://localhost:9000/dashboard?id=trademaster-trading-service
```

#### Security-Specific Checks

SonarQube detects:
- **SQL Injection** - Unsafe query construction
- **Cross-Site Scripting (XSS)** - Unescaped user input
- **Path Traversal** - File system access vulnerabilities
- **Weak Cryptography** - Insecure encryption algorithms
- **Hardcoded Credentials** - Secrets in code
- **Insecure Deserialization** - Unsafe object deserialization

#### Quality Gate Configuration

```yaml
# sonar-project.properties
sonar.qualitygate.wait=true
sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml

# Quality Gate Conditions
# - Code Coverage >= 80%
# - No Critical/High Security Vulnerabilities
# - Technical Debt Ratio <= 5%
```

### 3. OWASP ZAP Scanning

Dynamic application security testing (DAST) for web application vulnerabilities.

#### Baseline Scan (Quick)

```bash
# Passive scanning only - no attacks
./scripts/security/owasp-zap-scan.sh http://localhost:8080 baseline

# View report
open build/reports/security/zap/zap-baseline-report.html
```

#### Full Scan (Comprehensive)

```bash
# Active + passive scanning with attacks
./scripts/security/owasp-zap-scan.sh http://localhost:8080 full

# This may take 30-60 minutes
```

#### API Scan (OpenAPI-based)

```bash
# Scans based on OpenAPI specification
./scripts/security/owasp-zap-scan.sh http://localhost:8080 api
```

#### Authenticated Scan

```bash
# Generate JWT token
export AUTH_TOKEN=$(curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}' | jq -r '.token')

# Run authenticated scan
AUTH_TOKEN=$AUTH_TOKEN ./scripts/security/owasp-zap-scan.sh http://localhost:8080 authenticated
```

#### ZAP Findings

**Common Vulnerabilities Detected**:
- Missing security headers (CSP, HSTS, X-Frame-Options)
- Cookie security issues (missing Secure, HttpOnly flags)
- TLS/SSL configuration weaknesses
- Cross-Site Scripting (XSS) vulnerabilities
- SQL Injection attempts
- Insecure authentication mechanisms

**Risk Levels**:
- **High**: Critical vulnerabilities requiring immediate fix
- **Medium**: Important vulnerabilities to address before production
- **Low**: Minor issues to address when possible
- **Informational**: Best practice recommendations

### 4. Container Security Scanning

Scans Docker images for vulnerabilities and misconfigurations.

#### Using Trivy

```bash
# Scan Docker image for vulnerabilities
trivy image trademaster-trading-service:latest

# High and Critical vulnerabilities only
trivy image --severity HIGH,CRITICAL trademaster-trading-service:latest

# Generate JSON report
trivy image --format json --output trivy-report.json trademaster-trading-service:latest

# Scan for secrets
trivy image --scanners secret trademaster-trading-service:latest
```

#### Common Issues

- Base image vulnerabilities (outdated Alpine/Ubuntu)
- Unnecessary packages in container
- Secrets in image layers
- Running as root user
- Exposed sensitive ports

#### Remediation

```dockerfile
# Use minimal, updated base image
FROM eclipse-temurin:24-jre-alpine

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy only necessary files
COPY --chown=appuser:appgroup target/*.jar app.jar

# Remove unnecessary packages
RUN apk del apk-tools
```

---

## Manual Penetration Testing

### OWASP Top 10 Testing Checklist

#### A01: Broken Access Control

**Test Cases**:
1. **Horizontal Privilege Escalation**
   ```bash
   # Try accessing another user's data
   curl -H "Authorization: Bearer $USER1_TOKEN" \
     http://localhost:8080/api/v1/orders/user/9999

   # Should return 403 Forbidden
   ```

2. **Vertical Privilege Escalation**
   ```bash
   # Try accessing admin endpoint with regular user token
   curl -H "Authorization: Bearer $USER_TOKEN" \
     http://localhost:8080/api/v1/admin/users

   # Should return 403 Forbidden
   ```

3. **Insecure Direct Object References (IDOR)**
   ```bash
   # Try manipulating order IDs
   curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/v1/orders/12345

   # Should verify ownership before returning data
   ```

#### A02: Cryptographic Failures

**Test Cases**:
1. **TLS Configuration**
   ```bash
   # Test SSL/TLS configuration
   nmap --script ssl-enum-ciphers -p 8080 localhost

   # Should use TLS 1.2+ with strong ciphers
   ```

2. **Sensitive Data Exposure**
   ```bash
   # Check for sensitive data in logs
   grep -i "password\|token\|secret" logs/application.log

   # Should not find any sensitive data
   ```

3. **Password Storage**
   ```sql
   -- Verify passwords are hashed
   SELECT user_id, password FROM users LIMIT 1;

   -- Should show bcrypt/argon2 hashes, not plaintext
   ```

#### A03: Injection

**Test Cases**:
1. **SQL Injection**
   ```bash
   # Try SQL injection in order endpoint
   curl -H "Authorization: Bearer $TOKEN" \
     "http://localhost:8080/api/v1/orders?symbol=' OR '1'='1"

   # Should be parameterized, not vulnerable
   ```

2. **NoSQL Injection**
   ```bash
   # Try MongoDB injection
   curl -X POST http://localhost:8080/api/v1/orders \
     -H "Content-Type: application/json" \
     -d '{"userId": {"$ne": null}}'

   # Should validate input types
   ```

3. **Command Injection**
   ```bash
   # Try command injection in filename parameter
   curl "http://localhost:8080/api/v1/export?file=test;ls"

   # Should sanitize input and prevent command execution
   ```

#### A04: Insecure Design

**Test Cases**:
1. **Business Logic Flaws**
   ```bash
   # Try placing negative quantity order
   curl -X POST http://localhost:8080/api/v1/orders \
     -H "Content-Type: application/json" \
     -d '{"quantity": -100}'

   # Should validate business rules
   ```

2. **Race Conditions**
   ```bash
   # Try concurrent order placement to exceed limits
   for i in {1..10}; do
     curl -X POST http://localhost:8080/api/v1/orders \
       -H "Authorization: Bearer $TOKEN" \
       -d @order.json &
   done
   wait

   # Should handle concurrent requests safely
   ```

#### A05: Security Misconfiguration

**Test Cases**:
1. **Default Credentials**
   ```bash
   # Try default admin credentials
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -d '{"username":"admin","password":"admin"}'

   # Should not have default credentials
   ```

2. **Exposed Actuator Endpoints**
   ```bash
   # Check for unsecured actuator endpoints
   curl http://localhost:8080/actuator

   # Should require authentication
   ```

3. **Stack Traces in Errors**
   ```bash
   # Trigger error and check response
   curl http://localhost:8080/api/v1/orders/invalid-id

   # Should not expose stack traces
   ```

#### A06: Vulnerable and Outdated Components

**Test Cases**:
1. **Dependency Vulnerabilities**
   ```bash
   # Run OWASP Dependency Check
   ./gradlew dependencyCheckAnalyze

   # Check for HIGH/CRITICAL CVEs
   ```

2. **Outdated Framework Versions**
   ```bash
   # Check Spring Boot version
   ./gradlew dependencies | grep spring-boot

   # Should use latest stable version
   ```

#### A07: Identification and Authentication Failures

**Test Cases**:
1. **Weak Password Policy**
   ```bash
   # Try creating user with weak password
   curl -X POST http://localhost:8080/api/v1/users \
     -d '{"username":"test","password":"123"}'

   # Should enforce strong password policy
   ```

2. **JWT Token Security**
   ```bash
   # Check JWT token expiration
   echo $TOKEN | cut -d. -f2 | base64 -d | jq .exp

   # Should have reasonable expiration (15-60 minutes)
   ```

3. **Session Management**
   ```bash
   # Try using expired token
   curl -H "Authorization: Bearer $EXPIRED_TOKEN" \
     http://localhost:8080/api/v1/orders

   # Should return 401 Unauthorized
   ```

#### A08: Software and Data Integrity Failures

**Test Cases**:
1. **Unsigned Updates**
   ```bash
   # Check if updates are signed
   # TradeMaster should verify update signatures
   ```

2. **Insecure Deserialization**
   ```bash
   # Try sending malicious serialized object
   # Should use safe serialization (JSON, not Java serialization)
   ```

#### A09: Security Logging and Monitoring Failures

**Test Cases**:
1. **Authentication Logging**
   ```bash
   # Check if failed login attempts are logged
   grep "Authentication failed" logs/application.log

   # Should log all authentication events
   ```

2. **Audit Trail**
   ```bash
   # Verify critical operations are logged
   grep "Order placed\|Order executed" logs/application.log

   # Should have complete audit trail
   ```

#### A10: Server-Side Request Forgery (SSRF)

**Test Cases**:
1. **SSRF via URL Parameter**
   ```bash
   # Try SSRF attack
   curl "http://localhost:8080/api/v1/fetch?url=http://169.254.169.254/metadata"

   # Should validate and whitelist URLs
   ```

---

## Security Testing Automation

### CI/CD Integration

#### GitHub Actions Workflow

```yaml
# .github/workflows/security-scan.yml
name: Security Scanning

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 2 * * *' # Daily at 2 AM

jobs:
  security-scan:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up Java 24
        uses: actions/setup-java@v3
        with:
          java-version: '24'
          distribution: 'temurin'

      - name: Run OWASP Dependency Check
        run: ./gradlew dependencyCheckAnalyze

      - name: Upload Dependency Check Report
        uses: actions/upload-artifact@v3
        with:
          name: dependency-check-report
          path: build/reports/dependency-check-report.html

      - name: Run SonarQube Analysis
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        run: ./gradlew sonarqube

      - name: Build Docker Image
        run: docker build -t trademaster-trading-service:latest .

      - name: Run Trivy Container Scan
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'trademaster-trading-service:latest'
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'

      - name: Upload Trivy Results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'

      - name: Fail on Critical Vulnerabilities
        run: |
          if [ $(jq '.runs[0].results | length' trivy-results.sarif) -gt 0 ]; then
            echo "Critical vulnerabilities found!"
            exit 1
          fi
```

### Pre-Commit Security Checks

```bash
# .git/hooks/pre-commit
#!/bin/bash

# Run quick security checks before commit
./gradlew dependencyCheckAnalyze -x test

# Check for secrets in code
git secrets --scan

# Check for security issues
./gradlew spotbugsMain
```

---

## Security Remediation

### Vulnerability Prioritization

**Priority Matrix**:

| Severity | Production Impact | Timeline | Action |
|----------|------------------|----------|---------|
| Critical | Production down | Immediate (< 24h) | Hotfix + emergency patch |
| High | Data breach risk | Urgent (< 72h) | Scheduled fix |
| Medium | Limited impact | Sprint (1-2 weeks) | Planned fix |
| Low | Minimal risk | Backlog (as available) | Optional fix |

### Remediation Workflow

1. **Triage**
   - Review security findings
   - Verify vulnerabilities (eliminate false positives)
   - Assess business impact and exploitability

2. **Prioritize**
   - Use CVSS score + business context
   - Consider attack likelihood and impact
   - Create remediation tickets

3. **Fix**
   - Update vulnerable dependencies
   - Patch code vulnerabilities
   - Improve security configurations

4. **Validate**
   - Re-run security scans
   - Verify fixes with penetration testing
   - Update security documentation

5. **Monitor**
   - Track metrics (vulnerabilities over time)
   - Set up alerts for new CVEs
   - Regular security reviews

### Common Remediations

#### Update Dependencies

```bash
# Find dependency updates
./gradlew dependencyUpdates

# Update Spring Boot version
# Edit build.gradle:
# id 'org.springframework.boot' version '3.4.1' // Update to latest

# Update other dependencies
./gradlew dependencies --write-locks
```

#### Fix Security Headers

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'"))
            .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            .contentTypeOptions(Customizer.withDefaults())
            .frameOptions(frame -> frame.deny())
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true))
        );
    return http.build();
}
```

#### Secure Cookies

```yaml
# application.yml
server:
  servlet:
    session:
      cookie:
        secure: true
        http-only: true
        same-site: strict
```

---

## Reporting

### Security Assessment Report Template

```markdown
# Security Assessment Report
## TradeMaster Trading Service

**Date**: 2025-10-15
**Tester**: Security Team
**Scope**: TradeMaster Trading Service v1.0.0

### Executive Summary
- Total Vulnerabilities: X
- Critical: X
- High: X
- Medium: X
- Low: X

### Findings

#### Finding 1: [Vulnerability Name]
**Severity**: Critical
**CVSS Score**: 9.0
**Description**: [Detailed description]
**Impact**: [Business impact]
**Remediation**: [Fix recommendation]
**Status**: Open/Fixed

[Repeat for each finding]

### Recommendations
1. [Recommendation 1]
2. [Recommendation 2]

### Next Steps
- [Action item 1]
- [Action item 2]
```

---

## Continuous Security

### Security Metrics Tracking

```bash
# Track vulnerabilities over time
./scripts/security/track-vulnerabilities.sh

# Generate security dashboard
./scripts/security/generate-security-dashboard.sh
```

### Regular Security Activities

- **Daily**: Automated dependency scanning in CI/CD
- **Weekly**: Review security alerts and new CVEs
- **Monthly**: Manual penetration testing
- **Quarterly**: Comprehensive security audit
- **Annually**: Third-party security assessment

---

## Support

For security issues or questions:

- **Security Team**: security@trademaster.com
- **Slack**: #security-alerts
- **JIRA**: SECURITY project
- **Documentation**: https://docs.trademaster.com/security

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-15
**Authors**: TradeMaster Security Team
