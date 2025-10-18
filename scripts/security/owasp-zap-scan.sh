#!/bin/bash

##############################################################################
# OWASP ZAP Security Scanning Script
#
# Automates web application security testing using OWASP ZAP
# Tests TradeMaster Trading Service for OWASP Top 10 vulnerabilities
#
# Prerequisites:
# - OWASP ZAP installed (https://www.zaproxy.org/download/)
# - TradeMaster services running
# - Authentication token available
#
# Usage:
#   ./owasp-zap-scan.sh [target_url] [scan_type]
#
# Examples:
#   ./owasp-zap-scan.sh http://localhost:8080 baseline
#   ./owasp-zap-scan.sh http://localhost:8080 full
#
# @author TradeMaster Development Team
# @version 1.0.0
##############################################################################

set -e # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
TARGET_URL="${1:-http://localhost:8080}"
SCAN_TYPE="${2:-baseline}" # baseline, full, api
REPORT_DIR="$(pwd)/build/reports/security/zap"
ZAP_PORT="${ZAP_PORT:-8090}"
AUTH_TOKEN="${AUTH_TOKEN:-test-jwt-token}"

# ZAP Docker image
ZAP_IMAGE="ghcr.io/zaproxy/zaproxy:stable"

echo -e "${GREEN}==============================================

${NC}"
echo -e "${GREEN}OWASP ZAP Security Scan${NC}"
echo -e "${GREEN}==============================================${NC}"
echo -e "Target URL: ${TARGET_URL}"
echo -e "Scan Type: ${SCAN_TYPE}"
echo -e "Report Directory: ${REPORT_DIR}"
echo ""

# Create report directory
mkdir -p "${REPORT_DIR}"

# Check if target is reachable
echo -e "${YELLOW}Checking target availability...${NC}"
if ! curl -f -s -o /dev/null "${TARGET_URL}/actuator/health"; then
    echo -e "${RED}Error: Target ${TARGET_URL} is not reachable${NC}"
    echo -e "Please ensure TradeMaster services are running"
    exit 1
fi
echo -e "${GREEN}✓ Target is reachable${NC}"
echo ""

# Function to run baseline scan
run_baseline_scan() {
    echo -e "${YELLOW}Running baseline scan (quick passive scan)...${NC}"

    docker run --rm -v "${REPORT_DIR}:/zap/wrk:rw" \
        -t "${ZAP_IMAGE}" \
        zap-baseline.py \
        -t "${TARGET_URL}" \
        -r zap-baseline-report.html \
        -J zap-baseline-report.json \
        -w zap-baseline-report.md \
        -I || true # Don't fail on warnings

    echo -e "${GREEN}✓ Baseline scan complete${NC}"
}

# Function to run full scan
run_full_scan() {
    echo -e "${YELLOW}Running full scan (active + passive scan)...${NC}"

    docker run --rm -v "${REPORT_DIR}:/zap/wrk:rw" \
        -t "${ZAP_IMAGE}" \
        zap-full-scan.py \
        -t "${TARGET_URL}" \
        -r zap-full-report.html \
        -J zap-full-report.json \
        -w zap-full-report.md \
        -I || true

    echo -e "${GREEN}✓ Full scan complete${NC}"
}

# Function to run API scan
run_api_scan() {
    echo -e "${YELLOW}Running API scan (OpenAPI-based)...${NC}"

    # Download OpenAPI spec
    curl -s "${TARGET_URL}/v3/api-docs" > "${REPORT_DIR}/openapi.json"

    docker run --rm -v "${REPORT_DIR}:/zap/wrk:rw" \
        -t "${ZAP_IMAGE}" \
        zap-api-scan.py \
        -t /zap/wrk/openapi.json \
        -f openapi \
        -r zap-api-report.html \
        -J zap-api-report.json \
        -w zap-api-report.md \
        -I || true

    echo -e "${GREEN}✓ API scan complete${NC}"
}

# Function to run authenticated scan
run_authenticated_scan() {
    echo -e "${YELLOW}Running authenticated scan...${NC}"

    # Create ZAP context configuration
    cat > "${REPORT_DIR}/zap-context.json" <<EOF
{
  "context": {
    "name": "TradeMaster",
    "urls": ["${TARGET_URL}.*"],
    "includedTechnologies": ["Java", "Spring"],
    "authentication": {
      "type": "script",
      "parameters": {
        "scriptName": "JWT Authentication",
        "token": "${AUTH_TOKEN}"
      }
    }
  }
}
EOF

    docker run --rm -v "${REPORT_DIR}:/zap/wrk:rw" \
        -t "${ZAP_IMAGE}" \
        zap-full-scan.py \
        -t "${TARGET_URL}" \
        -n /zap/wrk/zap-context.json \
        -r zap-authenticated-report.html \
        -J zap-authenticated-report.json \
        -w zap-authenticated-report.md \
        -I || true

    echo -e "${GREEN}✓ Authenticated scan complete${NC}"
}

# Execute scan based on type
case "${SCAN_TYPE}" in
    baseline)
        run_baseline_scan
        ;;
    full)
        run_full_scan
        ;;
    api)
        run_api_scan
        ;;
    authenticated)
        run_authenticated_scan
        ;;
    all)
        run_baseline_scan
        run_api_scan
        run_full_scan
        run_authenticated_scan
        ;;
    *)
        echo -e "${RED}Error: Invalid scan type '${SCAN_TYPE}'${NC}"
        echo "Valid types: baseline, full, api, authenticated, all"
        exit 1
        ;;
esac

# Generate summary
echo ""
echo -e "${GREEN}==============================================${NC}"
echo -e "${GREEN}Security Scan Summary${NC}"
echo -e "${GREEN}==============================================${NC}"
echo -e "Reports generated in: ${REPORT_DIR}"
echo ""
echo "Available reports:"
ls -lh "${REPORT_DIR}"/*.html 2>/dev/null || echo "No HTML reports found"
echo ""

# Parse results and display summary
if [ -f "${REPORT_DIR}/zap-baseline-report.json" ] || [ -f "${REPORT_DIR}/zap-full-report.json" ]; then
    echo -e "${YELLOW}Vulnerability Summary:${NC}"

    # Count vulnerabilities by risk level
    for report in "${REPORT_DIR}"/*.json; do
        if [ -f "$report" ]; then
            echo -e "\nFrom: $(basename $report)"

            HIGH=$(jq '.site[].alerts[] | select(.riskcode == "3") | .riskcode' "$report" 2>/dev/null | wc -l || echo "0")
            MEDIUM=$(jq '.site[].alerts[] | select(.riskcode == "2") | .riskcode' "$report" 2>/dev/null | wc -l || echo "0")
            LOW=$(jq '.site[].alerts[] | select(.riskcode == "1") | .riskcode' "$report" 2>/dev/null | wc -l || echo "0")
            INFO=$(jq '.site[].alerts[] | select(.riskcode == "0") | .riskcode' "$report" 2>/dev/null | wc -l || echo "0")

            echo -e "  ${RED}High:${NC} ${HIGH}"
            echo -e "  ${YELLOW}Medium:${NC} ${MEDIUM}"
            echo -e "  ${YELLOW}Low:${NC} ${LOW}"
            echo -e "  Info: ${INFO}"
        fi
    done
fi

echo ""
echo -e "${GREEN}==============================================${NC}"
echo -e "Next steps:"
echo -e "1. Review HTML reports in ${REPORT_DIR}"
echo -e "2. Investigate high/medium vulnerabilities"
echo -e "3. Create remediation tickets for findings"
echo -e "4. Update security documentation"
echo -e "${GREEN}==============================================${NC}"

exit 0
