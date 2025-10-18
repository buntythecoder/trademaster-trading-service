#!/bin/bash

##############################################################################
# Comprehensive Security Testing Script
#
# Runs multiple security testing tools in sequence:
# 1. OWASP Dependency Check (CVE scanning)
# 2. SonarQube (code quality and security)
# 3. OWASP ZAP (web application security)
# 4. Container security scanning (if Docker images present)
#
# Usage:
#   ./run-security-tests.sh [options]
#
# Options:
#   --skip-dependency-check   Skip OWASP Dependency Check
#   --skip-sonarqube         Skip SonarQube analysis
#   --skip-zap               Skip OWASP ZAP scanning
#   --skip-container         Skip container security scanning
#   --target-url URL         Target URL for ZAP (default: http://localhost:8080)
#
# @author TradeMaster Development Team
# @version 1.0.0
##############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
SKIP_DEPENDENCY_CHECK=false
SKIP_SONARQUBE=false
SKIP_ZAP=false
SKIP_CONTAINER=false
TARGET_URL="http://localhost:8080"
REPORT_DIR="$(pwd)/build/reports/security"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-dependency-check)
            SKIP_DEPENDENCY_CHECK=true
            shift
            ;;
        --skip-sonarqube)
            SKIP_SONARQUBE=true
            shift
            ;;
        --skip-zap)
            SKIP_ZAP=true
            shift
            ;;
        --skip-container)
            SKIP_CONTAINER=true
            shift
            ;;
        --target-url)
            TARGET_URL="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Create report directory
mkdir -p "${REPORT_DIR}"

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}TradeMaster Security Testing Suite${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "Report Directory: ${REPORT_DIR}"
echo -e "Target URL: ${TARGET_URL}"
echo ""

# Track overall status
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run test with error handling
run_test() {
    local test_name="$1"
    local test_command="$2"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo ""
    echo -e "${YELLOW}=====================================${NC}"
    echo -e "${YELLOW}Running: ${test_name}${NC}"
    echo -e "${YELLOW}=====================================${NC}"

    if eval "${test_command}"; then
        echo -e "${GREEN}✓ ${test_name} PASSED${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}✗ ${test_name} FAILED${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# 1. OWASP Dependency Check
if [ "${SKIP_DEPENDENCY_CHECK}" = false ]; then
    run_test "OWASP Dependency Check" \
        "./gradlew dependencyCheckAnalyze"
fi

# 2. SonarQube Analysis
if [ "${SKIP_SONARQUBE}" = false ]; then
    if [ -n "${SONAR_TOKEN}" ] && [ -n "${SONAR_HOST_URL}" ]; then
        run_test "SonarQube Analysis" \
            "./gradlew sonarqube -Dsonar.token=${SONAR_TOKEN} -Dsonar.host.url=${SONAR_HOST_URL}"
    else
        echo -e "${YELLOW}Skipping SonarQube (SONAR_TOKEN or SONAR_HOST_URL not set)${NC}"
    fi
fi

# 3. OWASP ZAP Scanning
if [ "${SKIP_ZAP}" = false ]; then
    # Check if target is reachable
    if curl -f -s -o /dev/null "${TARGET_URL}/actuator/health"; then
        run_test "OWASP ZAP Baseline Scan" \
            "bash scripts/security/owasp-zap-scan.sh ${TARGET_URL} baseline"

        run_test "OWASP ZAP API Scan" \
            "bash scripts/security/owasp-zap-scan.sh ${TARGET_URL} api"
    else
        echo -e "${YELLOW}Skipping OWASP ZAP (target ${TARGET_URL} not reachable)${NC}"
    fi
fi

# 4. Container Security Scanning (using Trivy)
if [ "${SKIP_CONTAINER}" = false ]; then
    if command -v trivy &> /dev/null; then
        # Check if Docker images exist
        if docker images | grep -q "trademaster-trading-service"; then
            run_test "Container Security Scan (Trivy)" \
                "trivy image --severity HIGH,CRITICAL --format json --output ${REPORT_DIR}/trivy-report.json trademaster-trading-service:latest"
        else
            echo -e "${YELLOW}Skipping container scan (no Docker images found)${NC}"
        fi
    else
        echo -e "${YELLOW}Skipping container scan (Trivy not installed)${NC}"
    fi
fi

# Generate comprehensive security report
echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Security Testing Summary${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "Total Tests: ${TOTAL_TESTS}"
echo -e "${GREEN}Passed: ${PASSED_TESTS}${NC}"
echo -e "${RED}Failed: ${FAILED_TESTS}${NC}"
echo ""

# List all generated reports
echo -e "${BLUE}Generated Reports:${NC}"
find "${REPORT_DIR}" -type f \( -name "*.html" -o -name "*.json" -o -name "*.xml" \) -exec ls -lh {} \;

echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Next Steps:${NC}"
echo -e "1. Review generated reports in ${REPORT_DIR}"
echo -e "2. Prioritize vulnerabilities by severity"
echo -e "3. Create remediation tickets for findings"
echo -e "4. Update security documentation"
echo -e "5. Re-run tests after fixes to validate"
echo -e "${BLUE}=========================================${NC}"

# Exit with failure if any tests failed
if [ ${FAILED_TESTS} -gt 0 ]; then
    exit 1
fi

exit 0
