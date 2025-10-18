#!/bin/bash

##############################################################################
# Application Rollback Testing Script
#
# Features:
# - Blue-green deployment rollback simulation
# - Version rollback with compatibility testing
# - Configuration rollback validation
# - Database schema compatibility verification
# - Automated rollback with health checks
# - Rollback time measurement (RTO validation)
#
# Prerequisites:
# - Docker and Docker Compose installed
# - Blue and green deployment environments configured
# - Previous application version available
# - Database migration rollback scripts
#
# Usage:
#   ./test-application-rollback.sh [rollback-type] [options]
#
# Rollback Types:
#   blue-green     - Switch traffic from green to blue environment
#   version        - Rollback to previous application version
#   config         - Rollback configuration changes
#   database       - Rollback database schema changes
#   full           - Complete rollback (application + database + config)
#
# Examples:
#   ./test-application-rollback.sh blue-green
#   ./test-application-rollback.sh version --target-version 1.2.3
#   ./test-application-rollback.sh full --reason "Critical bug in production"
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
ROLLBACK_TYPE="${1:-blue-green}"
ROLLBACK_REASON="${ROLLBACK_REASON:-Manual rollback test}"
TARGET_VERSION="${TARGET_VERSION:-}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
ROLLBACK_LOG="/var/log/trademaster/rollback_${TIMESTAMP}.log"
RTO_START_TIME=$(date +%s)

# Deployment configuration
BLUE_ENV="blue"
GREEN_ENV="green"
CURRENT_ENV="${ACTIVE_ENVIRONMENT:-green}"
ROLLBACK_ENV=$( [ "${CURRENT_ENV}" = "green" ] && echo "blue" || echo "green" )
LOAD_BALANCER_CONFIG="/etc/nginx/sites-available/trademaster"

# Application configuration
APP_NAME="trademaster-trading-service"
APP_PORT="${APP_PORT:-8080}"
HEALTH_CHECK_URL="http://localhost:${APP_PORT}/actuator/health"
HEALTH_CHECK_TIMEOUT=60

# Create log directory
mkdir -p "$(dirname ${ROLLBACK_LOG})"

# Logging function
log() {
    local level="$1"
    shift
    local message="$@"
    echo -e "[$(date +'%Y-%m-%d %H:%M:%S')] [${level}] ${message}" | tee -a "${ROLLBACK_LOG}"
}

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Application Rollback Test${NC}"
echo -e "${BLUE}=========================================${NC}"
log "INFO" "Rollback Type: ${ROLLBACK_TYPE}"
log "INFO" "Rollback Reason: ${ROLLBACK_REASON}"
log "INFO" "Current Environment: ${CURRENT_ENV}"
log "INFO" "Rollback Target: ${ROLLBACK_ENV}"
echo ""

# Function to calculate RTO (Recovery Time Objective)
calculate_rto() {
    local end_time=$(date +%s)
    local rto_seconds=$((end_time - RTO_START_TIME))
    local rto_minutes=$((rto_seconds / 60))
    local rto_seconds_remainder=$((rto_seconds % 60))

    echo -e "${YELLOW}Recovery Time Objective (RTO):${NC} ${rto_minutes}m ${rto_seconds_remainder}s"

    if [ ${rto_seconds} -lt 300 ]; then  # < 5 minutes
        echo -e "${GREEN}✓ RTO target met (< 5 minutes)${NC}"
        return 0
    else
        echo -e "${RED}✗ RTO target exceeded (> 5 minutes)${NC}"
        return 1
    fi
}

# Function to check health of application
check_health() {
    local env="$1"
    local max_attempts="${2:-30}"
    local attempt=0

    log "INFO" "Checking health of ${env} environment..."

    while [ ${attempt} -lt ${max_attempts} ]; do
        if curl -sf "${HEALTH_CHECK_URL}" > /dev/null 2>&1; then
            local health_status=$(curl -s "${HEALTH_CHECK_URL}" | jq -r '.status')
            if [ "${health_status}" = "UP" ]; then
                log "INFO" "✓ ${env} environment is healthy"
                return 0
            fi
        fi

        attempt=$((attempt + 1))
        sleep 2
    done

    log "ERROR" "✗ ${env} environment health check failed after ${max_attempts} attempts"
    return 1
}

# Function to switch load balancer traffic (blue-green rollback)
switch_load_balancer() {
    local target_env="$1"

    log "INFO" "Switching load balancer traffic to ${target_env} environment..."

    # Update nginx configuration
    sudo sed -i "s/server ${CURRENT_ENV}:${APP_PORT}/server ${target_env}:${APP_PORT}/g" "${LOAD_BALANCER_CONFIG}"

    # Reload nginx
    sudo nginx -t && sudo systemctl reload nginx

    if [ $? -eq 0 ]; then
        log "INFO" "✓ Load balancer switched to ${target_env}"

        # Verify traffic is routed correctly
        sleep 2
        if curl -sf "http://localhost/actuator/health" > /dev/null 2>&1; then
            log "INFO" "✓ Traffic routing verified"
            return 0
        else
            log "ERROR" "✗ Traffic routing verification failed"
            return 1
        fi
    else
        log "ERROR" "✗ Load balancer switch failed"
        return 1
    fi
}

# Function to perform blue-green rollback
blue_green_rollback() {
    log "INFO" "Starting blue-green deployment rollback..."

    # Step 1: Verify rollback environment is healthy
    log "INFO" "Step 1: Verifying ${ROLLBACK_ENV} environment health..."
    if ! check_health "${ROLLBACK_ENV}"; then
        log "ERROR" "Rollback environment is not healthy. Starting rollback environment..."

        # Start rollback environment
        docker-compose -f docker-compose.${ROLLBACK_ENV}.yml up -d

        # Wait for health check
        if ! check_health "${ROLLBACK_ENV}" 30; then
            log "ERROR" "Failed to start ${ROLLBACK_ENV} environment"
            return 1
        fi
    fi

    # Step 2: Switch load balancer traffic
    log "INFO" "Step 2: Switching load balancer traffic..."
    if ! switch_load_balancer "${ROLLBACK_ENV}"; then
        log "ERROR" "Load balancer switch failed"
        return 1
    fi

    # Step 3: Verify application functionality
    log "INFO" "Step 3: Verifying application functionality..."
    if ! verify_application_functionality; then
        log "ERROR" "Application functionality verification failed"

        # Rollback the rollback (switch back)
        log "WARN" "Rolling back to ${CURRENT_ENV} environment..."
        switch_load_balancer "${CURRENT_ENV}"
        return 1
    fi

    # Step 4: Gracefully shutdown current environment
    log "INFO" "Step 4: Shutting down ${CURRENT_ENV} environment..."
    docker-compose -f docker-compose.${CURRENT_ENV}.yml stop

    log "INFO" "✓ Blue-green rollback completed successfully"
    return 0
}

# Function to perform version rollback
version_rollback() {
    if [ -z "${TARGET_VERSION}" ]; then
        log "ERROR" "Target version not specified. Use --target-version flag."
        return 1
    fi

    log "INFO" "Starting version rollback to ${TARGET_VERSION}..."

    # Step 1: Pull previous version image
    log "INFO" "Step 1: Pulling Docker image for version ${TARGET_VERSION}..."
    docker pull "${APP_NAME}:${TARGET_VERSION}"

    if [ $? -ne 0 ]; then
        log "ERROR" "Failed to pull version ${TARGET_VERSION}"
        return 1
    fi

    # Step 2: Check database schema compatibility
    log "INFO" "Step 2: Checking database schema compatibility..."
    if ! check_schema_compatibility "${TARGET_VERSION}"; then
        log "ERROR" "Database schema incompatible with version ${TARGET_VERSION}"
        return 1
    fi

    # Step 3: Stop current application
    log "INFO" "Step 3: Stopping current application..."
    docker-compose stop ${APP_NAME}

    # Step 4: Start application with target version
    log "INFO" "Step 4: Starting application with version ${TARGET_VERSION}..."
    docker-compose up -d --force-recreate ${APP_NAME}

    # Step 5: Wait for health check
    log "INFO" "Step 5: Waiting for application to become healthy..."
    if ! check_health "application" 30; then
        log "ERROR" "Application failed to start with version ${TARGET_VERSION}"

        # Rollback to current version
        log "WARN" "Rolling back to current version..."
        docker-compose up -d --force-recreate ${APP_NAME}
        return 1
    fi

    # Step 6: Verify application functionality
    log "INFO" "Step 6: Verifying application functionality..."
    if ! verify_application_functionality; then
        log "ERROR" "Application functionality verification failed"
        return 1
    fi

    log "INFO" "✓ Version rollback completed successfully"
    return 0
}

# Function to check database schema compatibility
check_schema_compatibility() {
    local target_version="$1"

    log "INFO" "Checking schema compatibility for version ${target_version}..."

    # Get current schema version
    local current_schema=$(psql -h localhost -U trademaster_user -d trademaster_trading -tAc "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1")

    # Get target schema version (from version metadata)
    local target_schema=$(docker run --rm "${APP_NAME}:${target_version}" cat /app/schema_version.txt 2>/dev/null || echo "unknown")

    log "INFO" "Current schema: ${current_schema}, Target schema: ${target_schema}"

    # Check if rollback migration exists
    local migration_file="db/migration/rollback/V${current_schema}__rollback_to_${target_schema}.sql"

    if [ -f "${migration_file}" ]; then
        log "INFO" "✓ Rollback migration found: ${migration_file}"
        return 0
    else
        log "WARN" "⚠ No rollback migration found. Manual schema verification required."
        return 1
    fi
}

# Function to perform configuration rollback
config_rollback() {
    log "INFO" "Starting configuration rollback..."

    # Step 1: Backup current configuration
    log "INFO" "Step 1: Backing up current configuration..."
    cp /etc/trademaster/application.yml "/etc/trademaster/application.yml.backup.${TIMESTAMP}"

    # Step 2: Restore previous configuration
    log "INFO" "Step 2: Restoring previous configuration..."
    local previous_config=$(ls -t /etc/trademaster/application.yml.backup.* | head -2 | tail -1)

    if [ -z "${previous_config}" ]; then
        log "ERROR" "No previous configuration backup found"
        return 1
    fi

    cp "${previous_config}" /etc/trademaster/application.yml

    # Step 3: Validate configuration
    log "INFO" "Step 3: Validating configuration..."
    if ! validate_configuration; then
        log "ERROR" "Configuration validation failed"

        # Restore current configuration
        cp "/etc/trademaster/application.yml.backup.${TIMESTAMP}" /etc/trademaster/application.yml
        return 1
    fi

    # Step 4: Restart application
    log "INFO" "Step 4: Restarting application with rolled back configuration..."
    docker-compose restart ${APP_NAME}

    # Step 5: Wait for health check
    if ! check_health "application" 30; then
        log "ERROR" "Application failed to start with rolled back configuration"

        # Restore current configuration
        cp "/etc/trademaster/application.yml.backup.${TIMESTAMP}" /etc/trademaster/application.yml
        docker-compose restart ${APP_NAME}
        return 1
    fi

    log "INFO" "✓ Configuration rollback completed successfully"
    return 0
}

# Function to validate configuration
validate_configuration() {
    log "INFO" "Validating configuration file..."

    # Check YAML syntax
    if ! yq eval '.' /etc/trademaster/application.yml > /dev/null 2>&1; then
        log "ERROR" "Invalid YAML syntax in configuration"
        return 1
    fi

    # Check required properties exist
    local required_props=("spring.datasource.url" "spring.kafka.bootstrap-servers" "spring.redis.host")

    for prop in "${required_props[@]}"; do
        if ! yq eval ".${prop}" /etc/trademaster/application.yml > /dev/null 2>&1; then
            log "ERROR" "Missing required property: ${prop}"
            return 1
        fi
    done

    log "INFO" "✓ Configuration validation passed"
    return 0
}

# Function to verify application functionality
verify_application_functionality() {
    log "INFO" "Verifying application functionality..."

    # Test 1: Health check endpoint
    if ! curl -sf "${HEALTH_CHECK_URL}" > /dev/null 2>&1; then
        log "ERROR" "Health check endpoint failed"
        return 1
    fi

    # Test 2: Database connectivity
    local db_status=$(curl -s "${HEALTH_CHECK_URL}" | jq -r '.components.db.status')
    if [ "${db_status}" != "UP" ]; then
        log "ERROR" "Database connectivity check failed"
        return 1
    fi

    # Test 3: Kafka connectivity
    local kafka_status=$(curl -s "${HEALTH_CHECK_URL}" | jq -r '.components.kafka.status')
    if [ "${kafka_status}" != "UP" ]; then
        log "ERROR" "Kafka connectivity check failed"
        return 1
    fi

    # Test 4: Redis connectivity
    local redis_status=$(curl -s "${HEALTH_CHECK_URL}" | jq -r '.components.redis.status')
    if [ "${redis_status}" != "UP" ]; then
        log "ERROR" "Redis connectivity check failed"
        return 1
    fi

    # Test 5: Basic API functionality
    local test_response=$(curl -sf -X GET "http://localhost:${APP_PORT}/api/v1/health" -H "Accept: application/json")
    if [ -z "${test_response}" ]; then
        log "ERROR" "API functionality test failed"
        return 1
    fi

    log "INFO" "✓ All functionality checks passed"
    return 0
}

# Function to perform full rollback
full_rollback() {
    log "INFO" "Starting full rollback (application + database + configuration)..."

    # Step 1: Configuration rollback
    log "INFO" "Step 1: Rolling back configuration..."
    if ! config_rollback; then
        log "ERROR" "Configuration rollback failed"
        return 1
    fi

    # Step 2: Application rollback
    log "INFO" "Step 2: Rolling back application..."
    if [ -n "${TARGET_VERSION}" ]; then
        if ! version_rollback; then
            log "ERROR" "Version rollback failed"
            return 1
        fi
    else
        if ! blue_green_rollback; then
            log "ERROR" "Blue-green rollback failed"
            return 1
        fi
    fi

    # Step 3: Database rollback (if needed)
    log "INFO" "Step 3: Checking if database rollback is needed..."
    if [ -n "${DATABASE_ROLLBACK_REQUIRED}" ] && [ "${DATABASE_ROLLBACK_REQUIRED}" = "true" ]; then
        log "INFO" "Rolling back database..."
        if ! ./restore-postgresql.sh latest; then
            log "ERROR" "Database rollback failed"
            return 1
        fi
    fi

    log "INFO" "✓ Full rollback completed successfully"
    return 0
}

# Main execution
case "${ROLLBACK_TYPE}" in
    blue-green)
        blue_green_rollback
        exit_code=$?
        ;;
    version)
        version_rollback
        exit_code=$?
        ;;
    config)
        config_rollback
        exit_code=$?
        ;;
    database)
        ./restore-postgresql.sh latest
        exit_code=$?
        ;;
    full)
        full_rollback
        exit_code=$?
        ;;
    *)
        log "ERROR" "Invalid rollback type '${ROLLBACK_TYPE}'"
        echo "Valid types: blue-green, version, config, database, full"
        exit 1
        ;;
esac

# Calculate and display RTO
echo ""
calculate_rto

echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Rollback Summary${NC}"
echo -e "${BLUE}=========================================${NC}"
log "INFO" "Rollback Type: ${ROLLBACK_TYPE}"
log "INFO" "Rollback Reason: ${ROLLBACK_REASON}"
log "INFO" "Completion Time: $(date)"

if [ ${exit_code} -eq 0 ]; then
    log "INFO" "Status: ${GREEN}SUCCESS${NC}"
    echo ""
    echo -e "${YELLOW}Post-Rollback Checklist:${NC}"
    echo -e "  1. Verify application functionality with end-to-end tests"
    echo -e "  2. Check monitoring dashboards for anomalies"
    echo -e "  3. Review application logs for errors"
    echo -e "  4. Notify stakeholders of rollback completion"
    echo -e "  5. Document rollback reason and outcomes"
    echo -e "  6. Schedule post-mortem review"
    echo ""
else
    log "ERROR" "Status: ${RED}FAILED${NC}"
    echo ""
    echo -e "${RED}Rollback failed. Review logs at: ${ROLLBACK_LOG}${NC}"
    echo ""
fi

exit ${exit_code}
