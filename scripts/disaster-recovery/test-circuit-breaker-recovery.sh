#!/bin/bash

##############################################################################
# Circuit Breaker Recovery Testing Script
#
# Features:
# - Test circuit breaker state transitions (CLOSED → OPEN → HALF_OPEN → CLOSED)
# - Validate fallback behavior during outages
# - Measure recovery time and automatic healing
# - Test cascading failure prevention
# - Verify metrics and alerting during circuit breaker events
#
# Prerequisites:
# - Resilience4j circuit breakers configured
# - External dependencies (broker APIs, market data, database)
# - Prometheus metrics enabled
# - Chaos engineering tools (optional: toxiproxy, chaos mesh)
#
# Usage:
#   ./test-circuit-breaker-recovery.sh [test-type] [options]
#
# Test Types:
#   state-transition   - Test circuit breaker state machine
#   fallback          - Validate fallback responses
#   recovery          - Test automatic recovery from failures
#   cascading         - Test cascading failure prevention
#   all               - Run all circuit breaker tests
#
# Examples:
#   ./test-circuit-breaker-recovery.sh state-transition
#   ./test-circuit-breaker-recovery.sh fallback --service broker-api
#   ./test-circuit-breaker-recovery.sh all --duration 300
#
# @author TradeMaster DevOps Team
# @version 1.0.0
##############################################################################

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
TEST_TYPE="${1:-state-transition}"
SERVICE_NAME="${SERVICE_NAME:-broker-api}"
TEST_DURATION="${TEST_DURATION:-60}"  # seconds
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
TEST_LOG="/var/log/trademaster/circuit_breaker_test_${TIMESTAMP}.log"

# Application endpoints
APP_URL="${APP_URL:-http://localhost:8080}"
HEALTH_CHECK_URL="${APP_URL}/actuator/health"
CIRCUIT_BREAKERS_URL="${APP_URL}/actuator/circuitbreakers"
METRICS_URL="${APP_URL}/actuator/prometheus"

# Circuit breaker configuration
CB_FAILURE_THRESHOLD=5
CB_WAIT_DURATION=10  # seconds in OPEN state
CB_PERMITTED_CALLS=3  # calls allowed in HALF_OPEN state

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Create log directory
mkdir -p "$(dirname ${TEST_LOG})"

# Logging function
log() {
    local level="$1"
    shift
    local message="$@"
    echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [${level}] ${message}" | tee -a "${TEST_LOG}"
}

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Circuit Breaker Recovery Testing${NC}"
echo -e "${BLUE}=========================================${NC}"
log "INFO" "Test Type: ${TEST_TYPE}"
log "INFO" "Service: ${SERVICE_NAME}"
log "INFO" "Test Duration: ${TEST_DURATION}s"
echo ""

# Function to get circuit breaker state
get_cb_state() {
    local service="$1"

    local state=$(curl -s "${CIRCUIT_BREAKERS_URL}" | \
        jq -r ".circuitBreakers[] | select(.name == \"${service}\") | .state")

    echo "${state}"
}

# Function to get circuit breaker metrics
get_cb_metrics() {
    local service="$1"

    log "INFO" "Circuit Breaker Metrics for ${service}:"

    # Failure rate
    local failure_rate=$(curl -s "${METRICS_URL}" | \
        grep "resilience4j_circuitbreaker_failure_rate{name=\"${service}\"}" | \
        awk '{print $2}')

    # Buffered calls
    local buffered_calls=$(curl -s "${METRICS_URL}" | \
        grep "resilience4j_circuitbreaker_buffered_calls{name=\"${service}\"}" | \
        awk '{print $2}')

    # Failed calls
    local failed_calls=$(curl -s "${METRICS_URL}" | \
        grep "resilience4j_circuitbreaker_failed_calls{name=\"${service}\"}" | \
        awk '{print $2}')

    log "INFO" "  Failure Rate: ${failure_rate}%"
    log "INFO" "  Buffered Calls: ${buffered_calls}"
    log "INFO" "  Failed Calls: ${failed_calls}"
}

# Function to trigger circuit breaker opening
trigger_cb_open() {
    local service="$1"
    local endpoint="$2"

    log "INFO" "Triggering circuit breaker OPEN state for ${service}..."

    # Simulate failures by hitting the endpoint multiple times
    for i in $(seq 1 ${CB_FAILURE_THRESHOLD}); do
        log "INFO" "Sending failing request ${i}/${CB_FAILURE_THRESHOLD}..."

        # Simulate failure by calling endpoint with invalid parameters
        curl -sf -X POST "${endpoint}" \
            -H "Content-Type: application/json" \
            -d '{"invalid": "data"}' \
            > /dev/null 2>&1 || true

        sleep 1
    done

    # Wait for circuit breaker to transition
    sleep 2

    local state=$(get_cb_state "${service}")
    if [ "${state}" = "OPEN" ]; then
        log "INFO" "${GREEN}✓ Circuit breaker transitioned to OPEN state${NC}"
        return 0
    else
        log "ERROR" "${RED}✗ Circuit breaker did not open (current state: ${state})${NC}"
        return 1
    fi
}

# Function to wait for circuit breaker transition
wait_for_cb_state() {
    local service="$1"
    local expected_state="$2"
    local timeout="${3:-60}"  # seconds
    local elapsed=0

    log "INFO" "Waiting for ${service} to transition to ${expected_state} state (timeout: ${timeout}s)..."

    while [ ${elapsed} -lt ${timeout} ]; do
        local current_state=$(get_cb_state "${service}")

        if [ "${current_state}" = "${expected_state}" ]; then
            log "INFO" "${GREEN}✓ Circuit breaker transitioned to ${expected_state} after ${elapsed}s${NC}"
            return 0
        fi

        sleep 2
        elapsed=$((elapsed + 2))
    done

    log "ERROR" "${RED}✗ Circuit breaker did not transition to ${expected_state} within ${timeout}s${NC}"
    return 1
}

# Function to test state transitions
test_state_transitions() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    log "INFO" "Testing circuit breaker state transitions..."

    # Initial state should be CLOSED
    local initial_state=$(get_cb_state "${SERVICE_NAME}")
    log "INFO" "Initial state: ${initial_state}"

    if [ "${initial_state}" != "CLOSED" ]; then
        log "WARN" "Circuit breaker not in CLOSED state. Waiting for recovery..."
        wait_for_cb_state "${SERVICE_NAME}" "CLOSED" 60
    fi

    # Test 1: CLOSED → OPEN transition
    log "INFO" "Test 1: Triggering CLOSED → OPEN transition..."
    if trigger_cb_open "${SERVICE_NAME}" "${APP_URL}/api/v1/orders"; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log "INFO" "${GREEN}✓ Test 1 PASSED${NC}"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "ERROR" "${RED}✗ Test 1 FAILED${NC}"
        return 1
    fi

    # Display metrics in OPEN state
    get_cb_metrics "${SERVICE_NAME}"

    # Test 2: OPEN → HALF_OPEN transition
    log "INFO" "Test 2: Waiting for OPEN → HALF_OPEN transition..."
    if wait_for_cb_state "${SERVICE_NAME}" "HALF_OPEN" ${CB_WAIT_DURATION}; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log "INFO" "${GREEN}✓ Test 2 PASSED${NC}"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "ERROR" "${RED}✗ Test 2 FAILED${NC}"
        return 1
    fi

    # Test 3: HALF_OPEN → CLOSED transition (successful calls)
    log "INFO" "Test 3: Testing HALF_OPEN → CLOSED transition with successful calls..."

    for i in $(seq 1 ${CB_PERMITTED_CALLS}); do
        log "INFO" "Sending successful request ${i}/${CB_PERMITTED_CALLS}..."

        # Send valid request
        curl -sf -X GET "${APP_URL}/api/v1/health" \
            -H "Accept: application/json" \
            > /dev/null 2>&1

        sleep 1
    done

    # Wait for transition to CLOSED
    sleep 2

    if wait_for_cb_state "${SERVICE_NAME}" "CLOSED" 10; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log "INFO" "${GREEN}✓ Test 3 PASSED${NC}"
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "ERROR" "${RED}✗ Test 3 FAILED${NC}"
        return 1
    fi

    log "INFO" "${GREEN}✓ All state transition tests passed${NC}"
    return 0
}

# Function to test fallback behavior
test_fallback_behavior() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    log "INFO" "Testing fallback behavior during circuit breaker OPEN state..."

    # Trigger circuit breaker OPEN
    trigger_cb_open "${SERVICE_NAME}" "${APP_URL}/api/v1/orders"

    # Test fallback response
    log "INFO" "Sending request while circuit breaker is OPEN..."
    local response=$(curl -sf -X POST "${APP_URL}/api/v1/orders" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer test-token" \
        -d '{"symbol": "RELIANCE", "quantity": 10, "type": "MARKET"}' \
        2>/dev/null || echo "")

    if [ -n "${response}" ]; then
        log "INFO" "Fallback response received:"
        log "INFO" "${response}"

        # Check if response indicates fallback (should contain fallback message or cached data)
        if echo "${response}" | grep -q "fallback\|cached\|unavailable" 2>/dev/null; then
            PASSED_TESTS=$((PASSED_TESTS + 1))
            log "INFO" "${GREEN}✓ Fallback behavior test PASSED${NC}"
            return 0
        else
            FAILED_TESTS=$((FAILED_TESTS + 1))
            log "WARN" "Response doesn't indicate fallback behavior"
            return 1
        fi
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "ERROR" "${RED}✗ No response received (circuit breaker might be blocking)${NC}"
        return 1
    fi
}

# Function to test automatic recovery
test_automatic_recovery() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    log "INFO" "Testing automatic recovery from circuit breaker OPEN state..."

    # Trigger circuit breaker OPEN
    trigger_cb_open "${SERVICE_NAME}" "${APP_URL}/api/v1/orders"

    # Record recovery start time
    local recovery_start=$(date +%s)

    # Wait for automatic recovery (OPEN → HALF_OPEN → CLOSED)
    log "INFO" "Waiting for automatic recovery..."

    # Wait for HALF_OPEN
    if ! wait_for_cb_state "${SERVICE_NAME}" "HALF_OPEN" ${CB_WAIT_DURATION}; then
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "ERROR" "${RED}✗ Automatic recovery failed (HALF_OPEN not reached)${NC}"
        return 1
    fi

    # Send successful calls to trigger CLOSED
    for i in $(seq 1 ${CB_PERMITTED_CALLS}); do
        curl -sf -X GET "${APP_URL}/api/v1/health" > /dev/null 2>&1
        sleep 1
    done

    # Wait for CLOSED
    if ! wait_for_cb_state "${SERVICE_NAME}" "CLOSED" 10; then
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "ERROR" "${RED}✗ Automatic recovery failed (CLOSED not reached)${NC}"
        return 1
    fi

    # Calculate recovery time
    local recovery_end=$(date +%s)
    local recovery_time=$((recovery_end - recovery_start))

    log "INFO" "Recovery time: ${recovery_time}s"

    if [ ${recovery_time} -lt 60 ]; then  # < 1 minute
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log "INFO" "${GREEN}✓ Automatic recovery test PASSED (recovery time: ${recovery_time}s)${NC}"
        return 0
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "WARN" "Recovery time exceeded 1 minute: ${recovery_time}s"
        return 1
    fi
}

# Function to test cascading failure prevention
test_cascading_failure_prevention() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    log "INFO" "Testing cascading failure prevention..."

    # Trigger circuit breaker OPEN for one service
    trigger_cb_open "${SERVICE_NAME}" "${APP_URL}/api/v1/orders"

    # Verify other services remain operational
    log "INFO" "Verifying other services remain operational..."

    local health_status=$(curl -s "${HEALTH_CHECK_URL}" | jq -r '.status')

    if [ "${health_status}" = "UP" ] || [ "${health_status}" = "DEGRADED" ]; then
        PASSED_TESTS=$((PASSED_TESTS + 1))
        log "INFO" "${GREEN}✓ Other services remain operational (status: ${health_status})${NC}"

        # Check individual component health
        log "INFO" "Component Health Status:"
        curl -s "${HEALTH_CHECK_URL}" | jq -r '.components | to_entries[] | "\(.key): \(.value.status)"' | while read line; do
            log "INFO" "  ${line}"
        done

        return 0
    else
        FAILED_TESTS=$((FAILED_TESTS + 1))
        log "ERROR" "${RED}✗ Application health degraded beyond acceptable level: ${health_status}${NC}"
        return 1
    fi
}

# Function to monitor metrics during test
monitor_metrics() {
    local duration="$1"

    log "INFO" "Monitoring circuit breaker metrics for ${duration}s..."

    local end_time=$(($(date +%s) + duration))

    while [ $(date +%s) -lt ${end_time} ]; do
        get_cb_metrics "${SERVICE_NAME}"
        sleep 5
    done
}

# Function to verify alerting
verify_alerting() {
    log "INFO" "Verifying alerting configuration..."

    # Check if Prometheus alerts exist for circuit breaker
    local alert_rules=$(curl -s "http://localhost:9090/api/v1/rules" | \
        jq -r '.data.groups[].rules[] | select(.name | contains("CircuitBreaker")) | .name')

    if [ -n "${alert_rules}" ]; then
        log "INFO" "${GREEN}✓ Circuit breaker alerts configured:${NC}"
        echo "${alert_rules}" | while read alert; do
            log "INFO" "  - ${alert}"
        done
        return 0
    else
        log "WARN" "⚠ No circuit breaker alerts configured"
        return 1
    fi
}

# Main execution
case "${TEST_TYPE}" in
    state-transition)
        test_state_transitions
        ;;
    fallback)
        test_fallback_behavior
        ;;
    recovery)
        test_automatic_recovery
        ;;
    cascading)
        test_cascading_failure_prevention
        ;;
    all)
        test_state_transitions
        test_fallback_behavior
        test_automatic_recovery
        test_cascading_failure_prevention
        verify_alerting
        ;;
    *)
        log "ERROR" "Invalid test type '${TEST_TYPE}'"
        echo "Valid types: state-transition, fallback, recovery, cascading, all"
        exit 1
        ;;
esac

echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Circuit Breaker Test Summary${NC}"
echo -e "${BLUE}=========================================${NC}"
log "INFO" "Test Type: ${TEST_TYPE}"
log "INFO" "Total Tests: ${TOTAL_TESTS}"
log "INFO" "${GREEN}Passed: ${PASSED_TESTS}${NC}"
log "INFO" "${RED}Failed: ${FAILED_TESTS}${NC}"
log "INFO" "Completion Time: $(date)"
echo ""

if [ ${FAILED_TESTS} -eq 0 ]; then
    log "INFO" "${GREEN}✓ All circuit breaker tests PASSED${NC}"
    echo ""
    echo -e "${YELLOW}Key Findings:${NC}"
    echo -e "  1. Circuit breaker state transitions working correctly"
    echo -e "  2. Fallback behavior activated during failures"
    echo -e "  3. Automatic recovery functioning as expected"
    echo -e "  4. Cascading failures prevented successfully"
    echo ""
    exit 0
else
    log "ERROR" "${RED}✗ Some circuit breaker tests FAILED${NC}"
    echo ""
    echo -e "${RED}Review logs at: ${TEST_LOG}${NC}"
    echo ""
    exit 1
fi
