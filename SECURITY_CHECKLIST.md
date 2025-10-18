# TradeMaster Security Testing Checklist

## Overview

Comprehensive security checklist for TradeMaster Trading Service. Use this checklist before production deployment to ensure all security controls are properly implemented and tested.

---

## Pre-Testing Checklist

### Environment Setup
- [ ] Testing environment matches production configuration
- [ ] All services deployed and running
- [ ] Authentication tokens generated
- [ ] Security testing tools installed (OWASP ZAP, Trivy, etc.)
- [ ] Network access configured for security scanners
- [ ] Backup of test environment taken

### Documentation Review
- [ ] Security requirements documented
- [ ] Threat model updated
- [ ] Security architecture diagrams current
- [ ] Data flow diagrams include security controls

---

## Automated Security Scanning

### OWASP Dependency Check
- [ ] Run OWASP Dependency Check
- [ ] Zero Critical vulnerabilities (CVSS >= 9.0)
- [ ] Zero High vulnerabilities (CVSS >= 7.0)
- [ ] Medium/Low vulnerabilities documented and accepted
- [ ] Dependency update plan created for all findings
- [ ] False positives added to suppression file

**Command**:
```bash
./gradlew dependencyCheckAnalyze
```

**Report Location**: `build/reports/dependency-check-report.html`

### SonarQube Analysis
- [ ] Run SonarQube analysis
- [ ] Quality Gate passed
- [ ] Zero Security Hotspots (High/Medium)
- [ ] Code coverage >= 80%
- [ ] Technical debt ratio <= 5%
- [ ] All critical code smells resolved

**Command**:
```bash
./gradlew sonarqube
```

**Dashboard**: http://localhost:9000/dashboard?id=trademaster-trading-service

### OWASP ZAP Scanning
- [ ] Baseline scan completed
- [ ] API scan completed
- [ ] Full scan completed (if time permits)
- [ ] Authenticated scan completed
- [ ] Zero High-risk vulnerabilities
- [ ] Medium-risk vulnerabilities documented
- [ ] False positives documented

**Commands**:
```bash
./scripts/security/owasp-zap-scan.sh http://localhost:8080 baseline
./scripts/security/owasp-zap-scan.sh http://localhost:8080 api
./scripts/security/owasp-zap-scan.sh http://localhost:8080 authenticated
```

**Report Location**: `build/reports/security/zap/`

### Container Security
- [ ] Docker image scanned with Trivy
- [ ] Zero Critical container vulnerabilities
- [ ] Zero High container vulnerabilities
- [ ] Base image updated to latest
- [ ] No secrets in container layers
- [ ] Container runs as non-root user

**Command**:
```bash
trivy image --severity HIGH,CRITICAL trademaster-trading-service:latest
```

---

## OWASP Top 10 Testing

### A01: Broken Access Control

#### Horizontal Privilege Escalation
- [ ] Users cannot access other users' data
- [ ] Order access requires ownership validation
- [ ] Portfolio access requires user authentication
- [ ] Position data isolated by user ID

**Test Case**:
```bash
# Try accessing another user's orders
curl -H "Authorization: Bearer $USER1_TOKEN" \
  http://localhost:8080/api/v1/orders/user/9999
# Expected: 403 Forbidden
```

#### Vertical Privilege Escalation
- [ ] Regular users cannot access admin endpoints
- [ ] Role-based access control (RBAC) enforced
- [ ] Admin operations require ADMIN role
- [ ] Method-level security annotations present

**Test Case**:
```bash
# Try admin endpoint with user token
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8080/api/v1/admin/users
# Expected: 403 Forbidden
```

#### Insecure Direct Object References (IDOR)
- [ ] All resource IDs validated for ownership
- [ ] Order IDs checked against user session
- [ ] Portfolio IDs verified before access
- [ ] No sequential/guessable IDs exposed

**Test Case**:
```bash
# Try sequential order ID access
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/orders/12345
# Expected: 403 if not owner, 404 if doesn't exist
```

### A02: Cryptographic Failures

#### TLS/SSL Configuration
- [ ] TLS 1.3 enabled (TLS 1.2 minimum)
- [ ] Strong cipher suites only
- [ ] SSL certificates valid and not self-signed
- [ ] HSTS header configured
- [ ] Certificate expiration monitored

**Test Case**:
```bash
nmap --script ssl-enum-ciphers -p 8080 localhost
# Expected: TLS 1.2+, strong ciphers only
```

#### Sensitive Data Protection
- [ ] Passwords hashed with bcrypt/argon2
- [ ] No plaintext passwords in database
- [ ] API keys encrypted at rest
- [ ] JWT tokens use strong signing algorithm
- [ ] No sensitive data in logs

**Test Case**:
```bash
grep -i "password\|token\|secret" logs/application.log
# Expected: No sensitive data found
```

#### Encryption at Rest
- [ ] Database encryption enabled
- [ ] Sensitive fields encrypted (API keys, tokens)
- [ ] Encryption keys stored securely (Vault)
- [ ] No hardcoded encryption keys

### A03: Injection

#### SQL Injection
- [ ] All queries use parameterized statements
- [ ] No string concatenation in queries
- [ ] ORM (JPA/Hibernate) used correctly
- [ ] Input validation on all parameters

**Test Case**:
```bash
curl "http://localhost:8080/api/v1/orders?symbol=' OR '1'='1"
# Expected: 400 Bad Request or safe handling
```

#### NoSQL Injection (MongoDB)
- [ ] MongoDB queries use proper operators
- [ ] Input types validated
- [ ] No $where operator with user input
- [ ] Query sanitization implemented

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": {"$ne": null}}'
# Expected: 400 Bad Request - type validation failed
```

#### Command Injection
- [ ] No system command execution with user input
- [ ] File paths sanitized
- [ ] No eval() or similar unsafe functions
- [ ] Process execution disabled or sandboxed

**Test Case**:
```bash
curl "http://localhost:8080/api/v1/export?file=test;ls"
# Expected: 400 Bad Request - invalid filename
```

### A04: Insecure Design

#### Business Logic Validation
- [ ] Negative quantities rejected
- [ ] Price limits enforced
- [ ] Order size limits validated
- [ ] Risk limits checked before order placement
- [ ] Balance validation before trades

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"quantity": -100, "symbol": "RELIANCE"}'
# Expected: 400 Bad Request - validation error
```

#### Race Condition Protection
- [ ] Concurrent order placement handled safely
- [ ] Database transactions use proper isolation
- [ ] Optimistic locking on critical resources
- [ ] Rate limiting prevents abuse

**Test Case**:
```bash
# Submit 10 concurrent orders
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/v1/orders -d @order.json &
done
wait
# Expected: All orders processed safely, no race conditions
```

#### Rate Limiting
- [ ] API rate limiting configured
- [ ] 429 Too Many Requests returned when exceeded
- [ ] Rate limits per user/IP
- [ ] Circuit breakers protect downstream services

### A05: Security Misconfiguration

#### Default Configurations
- [ ] Default credentials disabled
- [ ] Admin accounts use strong passwords
- [ ] Unnecessary features disabled
- [ ] Debug mode disabled in production
- [ ] Sample data removed

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -d '{"username":"admin","password":"admin"}'
# Expected: 401 Unauthorized
```

#### Security Headers
- [ ] Content-Security-Policy header present
- [ ] X-Frame-Options: DENY
- [ ] X-Content-Type-Options: nosniff
- [ ] X-XSS-Protection: 1; mode=block
- [ ] Strict-Transport-Security configured

**Test Case**:
```bash
curl -I http://localhost:8080
# Expected: All security headers present
```

#### Actuator Endpoints
- [ ] Actuator endpoints secured
- [ ] Health check public, others authenticated
- [ ] Sensitive endpoints require authentication
- [ ] No sensitive data in responses

**Test Case**:
```bash
curl http://localhost:8080/actuator/env
# Expected: 401 Unauthorized (requires authentication)
```

#### Error Handling
- [ ] No stack traces in responses
- [ ] Generic error messages for users
- [ ] Detailed errors logged server-side
- [ ] Error codes documented

**Test Case**:
```bash
curl http://localhost:8080/api/v1/orders/invalid-id
# Expected: Generic error, no stack trace
```

### A06: Vulnerable and Outdated Components

#### Dependency Management
- [ ] All dependencies up-to-date
- [ ] No known CVEs in dependencies
- [ ] Transitive dependencies checked
- [ ] Security advisories monitored

**Test Case**:
```bash
./gradlew dependencyCheckAnalyze
# Expected: Zero High/Critical vulnerabilities
```

#### Framework Versions
- [ ] Spring Boot latest stable version
- [ ] Java 24 with latest security patches
- [ ] Database drivers updated
- [ ] Third-party libraries current

### A07: Identification and Authentication Failures

#### Password Policy
- [ ] Minimum 12 characters enforced
- [ ] Complexity requirements (uppercase, lowercase, numbers, symbols)
- [ ] Common passwords blocked
- [ ] Password history enforced (last 5)

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -d '{"username":"test","password":"123"}'
# Expected: 400 Bad Request - password too weak
```

#### JWT Token Security
- [ ] JWT tokens expire (15-60 minutes)
- [ ] Refresh tokens implemented
- [ ] Tokens signed with strong algorithm (RS256/ES256)
- [ ] Token blacklist for logout

**Test Case**:
```bash
echo $TOKEN | cut -d. -f2 | base64 -d | jq .exp
# Expected: Expiration within 15-60 minutes
```

#### Session Management
- [ ] Session timeout configured (30 minutes)
- [ ] Secure cookie flags set (Secure, HttpOnly, SameSite)
- [ ] Session fixation protection
- [ ] Concurrent session control

#### Multi-Factor Authentication
- [ ] MFA available for admin accounts
- [ ] TOTP/SMS verification implemented
- [ ] MFA bypass requires approval

### A08: Software and Data Integrity Failures

#### Dependency Integrity
- [ ] Dependencies verified with checksums
- [ ] Gradle lock files used
- [ ] No dependencies from untrusted sources
- [ ] CI/CD pipeline validates dependencies

#### Serialization Security
- [ ] Java serialization disabled
- [ ] JSON used for data interchange
- [ ] No untrusted deserialization
- [ ] Input validation on all deserializations

### A09: Security Logging and Monitoring Failures

#### Authentication Logging
- [ ] Successful logins logged
- [ ] Failed login attempts logged
- [ ] Account lockout events logged
- [ ] Logs include correlation IDs

**Test Case**:
```bash
grep "Authentication" logs/application.log
# Expected: All auth events logged with correlation IDs
```

#### Audit Trail
- [ ] Critical operations logged (orders, trades, transfers)
- [ ] User actions auditable
- [ ] Log retention policy (90 days minimum)
- [ ] Logs protected from tampering

**Test Case**:
```bash
grep "Order placed\|Order executed" logs/application.log
# Expected: Complete audit trail with user IDs
```

#### Security Monitoring
- [ ] Real-time alerting configured
- [ ] Failed auth attempts trigger alerts
- [ ] Unusual activity detected
- [ ] Security events correlate

### A10: Server-Side Request Forgery (SSRF)

#### URL Validation
- [ ] URLs validated and whitelisted
- [ ] No access to internal IPs (169.254.x.x, 10.x.x.x)
- [ ] No cloud metadata access (169.254.169.254)
- [ ] DNS rebinding protection

**Test Case**:
```bash
curl "http://localhost:8080/api/v1/fetch?url=http://169.254.169.254/metadata"
# Expected: 400 Bad Request - URL not whitelisted
```

---

## Additional Security Tests

### API Security

#### Input Validation
- [ ] All inputs validated for type and format
- [ ] Max length enforced on strings
- [ ] Numeric ranges validated
- [ ] Enums validated against allowed values
- [ ] JSON schema validation enabled

#### Output Encoding
- [ ] All outputs properly encoded
- [ ] XSS prevention in place
- [ ] Content-Type headers correct
- [ ] No reflected user input

#### CORS Configuration
- [ ] CORS properly configured
- [ ] Allowed origins whitelisted
- [ ] Credentials flag set correctly
- [ ] Preflight requests handled

### Infrastructure Security

#### Network Security
- [ ] Firewalls configured
- [ ] Only necessary ports exposed
- [ ] Internal services not publicly accessible
- [ ] Network segmentation in place

#### Container Security
- [ ] Containers run as non-root
- [ ] Minimal base images used
- [ ] No secrets in images
- [ ] Security context configured

#### Database Security
- [ ] Database access restricted
- [ ] Strong database passwords
- [ ] Connections encrypted
- [ ] Least privilege access

---

## Post-Testing Checklist

### Documentation
- [ ] All findings documented
- [ ] Security assessment report created
- [ ] Remediation plan documented
- [ ] Risk acceptance forms signed (if applicable)

### Remediation
- [ ] Critical vulnerabilities fixed
- [ ] High vulnerabilities fixed or accepted
- [ ] Medium vulnerabilities documented
- [ ] Re-scanned after fixes

### Validation
- [ ] Fixes verified with re-testing
- [ ] No regressions introduced
- [ ] Security controls validated
- [ ] Documentation updated

### Sign-off
- [ ] Security team approval
- [ ] Tech lead approval
- [ ] Product owner acceptance
- [ ] Compliance verification (if required)

---

## Production Deployment Checklist

### Final Security Validation
- [ ] All security scans passed
- [ ] Zero critical/high vulnerabilities
- [ ] Penetration testing completed
- [ ] Security monitoring configured
- [ ] Incident response plan ready

### Operational Security
- [ ] Log aggregation configured
- [ ] Alerting rules deployed
- [ ] Security runbooks created
- [ ] On-call rotation established
- [ ] Backup and recovery tested

### Compliance
- [ ] Regulatory requirements met
- [ ] Data privacy compliance validated
- [ ] Security policies documented
- [ ] Training completed

---

## Continuous Security

### Regular Activities
- [ ] Weekly: Review security alerts
- [ ] Monthly: Penetration testing
- [ ] Quarterly: Comprehensive security audit
- [ ] Annually: Third-party assessment

### Metrics Tracking
- [ ] Vulnerability count trending down
- [ ] Mean time to remediation tracked
- [ ] Security incidents logged
- [ ] Compliance status monitored

---

## Sign-off

**Tested By**: ___________________________
**Date**: ___________________________
**Security Lead Approval**: ___________________________
**Date**: ___________________________
**Tech Lead Approval**: ___________________________
**Date**: ___________________________

---

**Checklist Version**: 1.0.0
**Last Updated**: 2025-10-15
**Next Review**: 2025-11-15
