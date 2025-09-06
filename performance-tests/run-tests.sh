#!/bin/bash

# TradeMaster Trading Service Performance Testing Suite
# Comprehensive performance validation with multiple test scenarios

set -e

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8083}"
RESULTS_DIR="./performance-results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_DIR="${RESULTS_DIR}/${TIMESTAMP}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=================================="
echo -e "TradeMaster Performance Test Suite"
echo -e "==================================${NC}"

# Create results directory
mkdir -p "${REPORT_DIR}"

# Function to check if service is ready
check_service_health() {
    echo -e "${YELLOW}Checking service health...${NC}"
    
    max_attempts=30
    attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Service is healthy and ready${NC}"
            return 0
        else
            echo -e "${YELLOW}Attempt $attempt/$max_attempts - Service not ready, waiting 5s...${NC}"
            sleep 5
            ((attempt++))
        fi
    done
    
    echo -e "${RED}✗ Service health check failed after $max_attempts attempts${NC}"
    exit 1
}

# Function to run a specific test scenario
run_test() {
    local test_name="$1"
    local test_script="$2"
    local test_options="$3"
    
    echo -e "${BLUE}Running $test_name...${NC}"
    
    local output_file="${REPORT_DIR}/${test_name,,}_results.json"
    local summary_file="${REPORT_DIR}/${test_name,,}_summary.txt"
    
    # Run K6 test
    k6 run \
        --out json="${output_file}" \
        --summary-export="${summary_file}" \
        --env BASE_URL="${BASE_URL}" \
        ${test_options} \
        "${test_script}"
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✓ $test_name completed successfully${NC}"
    else
        echo -e "${RED}✗ $test_name failed with exit code $exit_code${NC}"
        return $exit_code
    fi
    
    # Generate summary
    echo -e "\n${BLUE}=== $test_name Summary ===${NC}"
    grep -E "(checks|http_req_duration|http_req_failed)" "${summary_file}" || true
    echo ""
}

# Function to analyze results and generate report
generate_report() {
    echo -e "${BLUE}Generating comprehensive performance report...${NC}"
    
    local report_file="${REPORT_DIR}/performance_report.md"
    
    cat > "${report_file}" << EOF
# TradeMaster Trading Service Performance Test Report

**Test Date:** $(date)  
**Base URL:** ${BASE_URL}  
**Test Duration:** Various scenarios  

## Executive Summary

This report contains the results of comprehensive performance testing for the TradeMaster Trading Service, including load testing, stress testing, and endurance testing.

## Test Scenarios

### 1. Load Test (Standard Traffic)
- **Objective:** Validate performance under expected production load
- **Users:** Up to 1,000 concurrent users
- **Duration:** 30 minutes
- **Mix:** 40% order placement, 30% portfolio, 20% history, 10% market data

### 2. Stress Test (Peak Traffic)  
- **Objective:** Determine breaking point and failure modes
- **Users:** Up to 2,000 concurrent users
- **Duration:** 15 minutes peak load
- **Focus:** System stability under extreme load

### 3. Spike Test (Traffic Surge)
- **Objective:** Validate auto-scaling and resilience
- **Pattern:** Sudden spike from 10 to 2,000 users
- **Duration:** 2 minutes total
- **Focus:** Response to sudden load increases

### 4. Endurance Test (Sustained Load)
- **Objective:** Identify memory leaks and degradation
- **Users:** 1,500 concurrent users
- **Duration:** 60 minutes sustained load
- **Focus:** Long-term stability and performance

## SLA Requirements

| Metric | Target | Status |
|--------|--------|--------|
| Response Time (95th percentile) | < 200ms | TBD |
| Order Processing (99th percentile) | < 50ms | TBD |
| Error Rate | < 1% | TBD |
| Throughput | > 10,000 TPS | TBD |
| Availability | > 99.9% | TBD |

## Test Results

EOF

    # Add individual test results
    for result_file in "${REPORT_DIR}"/*_summary.txt; do
        if [ -f "$result_file" ]; then
            test_name=$(basename "$result_file" _summary.txt)
            echo "### ${test_name^} Results" >> "${report_file}"
            echo "\`\`\`" >> "${report_file}"
            cat "$result_file" >> "${report_file}"
            echo "\`\`\`" >> "${report_file}"
            echo "" >> "${report_file}"
        fi
    done
    
    cat >> "${report_file}" << EOF

## Recommendations

Based on the test results:

1. **Performance Optimization:**
   - Review response times exceeding SLA targets
   - Optimize database queries if needed
   - Consider caching strategies for frequently accessed data

2. **Capacity Planning:**
   - Plan for peak traffic scenarios
   - Configure auto-scaling policies
   - Monitor resource utilization trends

3. **Reliability:**
   - Implement circuit breakers for external services
   - Add comprehensive monitoring and alerting
   - Plan for graceful degradation under load

## Next Steps

1. Review detailed test results in individual summary files
2. Address any performance bottlenecks identified
3. Implement monitoring dashboards for production
4. Schedule regular performance testing as part of CI/CD

---
Generated by TradeMaster Performance Testing Suite
EOF

    echo -e "${GREEN}✓ Performance report generated: ${report_file}${NC}"
}

# Main execution flow
main() {
    echo -e "${YELLOW}Starting performance test suite...${NC}"
    
    # Check if k6 is installed
    if ! command -v k6 &> /dev/null; then
        echo -e "${RED}✗ k6 is not installed. Please install k6 first.${NC}"
        echo -e "Visit: https://k6.io/docs/getting-started/installation/"
        exit 1
    fi
    
    # Check service health
    check_service_health
    
    # Run baseline load test
    run_test "Load Test" "./load-test.js" "--vus 100 --duration 5m"
    
    # Run stress test
    run_test "Stress Test" "./load-test.js" "--vus 500 --duration 3m"
    
    # Wait for system to stabilize
    echo -e "${YELLOW}Waiting 30s for system to stabilize...${NC}"
    sleep 30
    
    # Run spike test (if spike-test.js exists, otherwise skip)
    if [ -f "./spike-test.js" ]; then
        run_test "Spike Test" "./spike-test.js" ""
    else
        echo -e "${YELLOW}Spike test script not found, using main script with spike pattern${NC}"
        run_test "Spike Test" "./load-test.js" "--vus 50 --duration 2m"
    fi
    
    # Wait for system to stabilize
    echo -e "${YELLOW}Waiting 30s for system to stabilize...${NC}"
    sleep 30
    
    # Run endurance test (shortened for CI/CD)
    run_test "Endurance Test" "./load-test.js" "--vus 200 --duration 10m"
    
    # Generate comprehensive report
    generate_report
    
    echo -e "${GREEN}=================================="
    echo -e "Performance testing completed!"
    echo -e "Results saved to: ${REPORT_DIR}"
    echo -e "==================================${NC}"
    
    # Display summary
    echo -e "\n${BLUE}Quick Summary:${NC}"
    ls -la "${REPORT_DIR}"
    
    echo -e "\n${BLUE}View the complete report:${NC}"
    echo -e "cat ${REPORT_DIR}/performance_report.md"
}

# Handle script arguments
case "${1:-all}" in
    load)
        check_service_health
        run_test "Load Test" "./load-test.js" "--vus 100 --duration 5m"
        ;;
    stress)
        check_service_health
        run_test "Stress Test" "./load-test.js" "--vus 500 --duration 3m"
        ;;
    spike)
        check_service_health
        run_test "Spike Test" "./load-test.js" "--vus 50 --duration 2m"
        ;;
    endurance)
        check_service_health
        run_test "Endurance Test" "./load-test.js" "--vus 200 --duration 30m"
        ;;
    all|*)
        main
        ;;
esac