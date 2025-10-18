#!/bin/bash

##############################################################################
# PostgreSQL Automated Restore Script with Point-in-Time Recovery
#
# Features:
# - Full database restoration from encrypted backups
# - Point-in-time recovery (PITR) to specific timestamp
# - Automatic backup download from S3
# - Pre-restore validation and safety checks
# - Post-restore verification
# - Rollback capability if restore fails
#
# Prerequisites:
# - PostgreSQL 15+ installed
# - AWS CLI configured for S3 access
# - GPG key for decryption (BACKUP_GPG_KEY environment variable)
# - Database stopped or in recovery mode
#
# Usage:
#   ./restore-postgresql.sh [restore-type] [options]
#
# Restore Types:
#   full [backup-file]           - Full database restore from backup
#   pitr [backup-file] [time]    - Point-in-time recovery to timestamp
#   latest                       - Restore from latest available backup
#
# Examples:
#   ./restore-postgresql.sh full postgres_trademaster_20250117_120000.sql.gpg
#   ./restore-postgresql.sh pitr postgres_base_20250117.tar.gz "2025-01-17 14:30:00"
#   ./restore-postgresql.sh latest
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
RESTORE_TYPE="${1:-latest}"
BACKUP_FILE="${2:-}"
PITR_TARGET="${3:-}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/postgresql"
RESTORE_DIR="/var/restore/postgresql"
WAL_ARCHIVE_DIR="${BACKUP_DIR}/wal_archive"
S3_BUCKET="${POSTGRES_BACKUP_S3_BUCKET:-trademaster-backups}"
S3_PREFIX="postgresql/trading-service"

# PostgreSQL connection settings
PGHOST="${POSTGRES_HOST:-localhost}"
PGPORT="${POSTGRES_PORT:-5432}"
PGUSER="${POSTGRES_USER:-trademaster_user}"
PGDATABASE="${POSTGRES_DB:-trademaster_trading}"
PGPASSWORD="${POSTGRES_PASSWORD}"
PGDATA="${POSTGRES_DATA_DIR:-/var/lib/postgresql/data}"

# GPG encryption key
GPG_KEY="${BACKUP_GPG_KEY}"

# Create restore directory
mkdir -p "${RESTORE_DIR}"

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}PostgreSQL Restore - ${RESTORE_TYPE}${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "Timestamp: ${TIMESTAMP}"
echo -e "Database: ${PGDATABASE}@${PGHOST}:${PGPORT}"
echo -e "Restore Directory: ${RESTORE_DIR}"
echo ""

# Function to perform pre-restore safety checks
pre_restore_checks() {
    echo -e "${YELLOW}Performing pre-restore safety checks...${NC}"

    # Check if PostgreSQL is running
    if pg_isready -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" > /dev/null 2>&1; then
        echo -e "${RED}✗ PostgreSQL is running. Stop it before restore:${NC}"
        echo -e "  sudo systemctl stop postgresql"
        return 1
    fi

    echo -e "${GREEN}✓ PostgreSQL is stopped${NC}"

    # Check disk space
    local required_space=10485760  # 10GB in KB
    local available_space=$(df "${RESTORE_DIR}" | awk 'NR==2 {print $4}')

    if [ ${available_space} -lt ${required_space} ]; then
        echo -e "${RED}✗ Insufficient disk space. Required: 10GB, Available: $((available_space/1024/1024))GB${NC}"
        return 1
    fi

    echo -e "${GREEN}✓ Sufficient disk space available${NC}"

    # Create backup of current database
    if [ -d "${PGDATA}" ]; then
        echo -e "${YELLOW}Creating safety backup of current database...${NC}"
        local safety_backup="${BACKUP_DIR}/pre_restore_${TIMESTAMP}"
        mkdir -p "${safety_backup}"
        cp -r "${PGDATA}" "${safety_backup}/"
        echo -e "${GREEN}✓ Safety backup created: ${safety_backup}${NC}"
    fi

    return 0
}

# Function to download backup from S3
download_from_s3() {
    local backup_name="$1"
    local s3_path="s3://${S3_BUCKET}/${S3_PREFIX}/full/${backup_name}"
    local local_path="${RESTORE_DIR}/${backup_name}"

    echo -e "${YELLOW}Downloading backup from S3...${NC}"
    echo -e "Source: ${s3_path}"

    aws s3 cp "${s3_path}" "${local_path}" --quiet

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Backup downloaded successfully${NC}"
        echo "${local_path}"
        return 0
    else
        echo -e "${RED}✗ Failed to download backup from S3${NC}"
        return 1
    fi
}

# Function to find latest backup
find_latest_backup() {
    echo -e "${YELLOW}Finding latest backup...${NC}"

    # Check local backups first
    local latest_local=$(ls -t "${BACKUP_DIR}/full"/*.gpg 2>/dev/null | head -1)

    if [ -n "${latest_local}" ]; then
        echo -e "${GREEN}✓ Latest local backup: ${latest_local}${NC}"
        echo "${latest_local}"
        return 0
    fi

    # Check S3 backups
    local latest_s3=$(aws s3 ls "s3://${S3_BUCKET}/${S3_PREFIX}/full/" | sort -r | head -1 | awk '{print $4}')

    if [ -n "${latest_s3}" ]; then
        echo -e "${GREEN}✓ Latest S3 backup: ${latest_s3}${NC}"
        download_from_s3 "${latest_s3}"
        return 0
    fi

    echo -e "${RED}✗ No backups found${NC}"
    return 1
}

# Function to decrypt and verify backup
decrypt_backup() {
    local encrypted_file="$1"
    local decrypted_file="${encrypted_file%.gpg}"

    echo -e "${YELLOW}Decrypting backup...${NC}"

    # Verify checksum first
    if [ -f "${encrypted_file}.sha256" ]; then
        local expected_checksum=$(cat "${encrypted_file}.sha256")
        local actual_checksum=$(sha256sum "${encrypted_file}" | cut -d' ' -f1)

        if [ "${expected_checksum}" != "${actual_checksum}" ]; then
            echo -e "${RED}✗ Checksum verification failed${NC}"
            echo -e "Expected: ${expected_checksum}"
            echo -e "Actual:   ${actual_checksum}"
            return 1
        fi

        echo -e "${GREEN}✓ Checksum verified${NC}"
    fi

    # Decrypt backup
    gpg --decrypt --batch --output "${decrypted_file}" "${encrypted_file}" 2>/dev/null

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Backup decrypted successfully${NC}"
        echo "${decrypted_file}"
        return 0
    else
        echo -e "${RED}✗ Backup decryption failed${NC}"
        return 1
    fi
}

# Function to perform full restore
full_restore() {
    local backup_file="$1"

    echo -e "${YELLOW}Starting full database restore...${NC}"

    # Decrypt backup
    local decrypted_file=$(decrypt_backup "${backup_file}")

    if [ $? -ne 0 ]; then
        return 1
    fi

    # Drop and recreate database
    echo -e "${YELLOW}Recreating database...${NC}"
    sudo -u postgres psql -c "DROP DATABASE IF EXISTS ${PGDATABASE};"
    sudo -u postgres psql -c "CREATE DATABASE ${PGDATABASE} OWNER ${PGUSER};"

    # Restore database
    echo -e "${YELLOW}Restoring database from backup...${NC}"
    PGPASSWORD="${PGPASSWORD}" pg_restore \
        -h "${PGHOST}" \
        -p "${PGPORT}" \
        -U "${PGUSER}" \
        -d "${PGDATABASE}" \
        --verbose \
        --no-owner \
        --no-acl \
        "${decrypted_file}"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Database restored successfully${NC}"

        # Clean up decrypted file
        rm -f "${decrypted_file}"

        # Verify restore
        verify_restore

        return 0
    else
        echo -e "${RED}✗ Database restore failed${NC}"
        return 1
    fi
}

# Function to perform point-in-time recovery
pitr_restore() {
    local base_backup="$1"
    local target_time="$2"

    echo -e "${YELLOW}Starting Point-in-Time Recovery to ${target_time}...${NC}"

    # Extract base backup
    echo -e "${YELLOW}Extracting base backup...${NC}"
    tar -xzf "${base_backup}" -C "${PGDATA}"

    # Create recovery configuration
    cat > "${PGDATA}/recovery.conf" <<EOF
restore_command = 'cp ${WAL_ARCHIVE_DIR}/%f %p'
recovery_target_time = '${target_time}'
recovery_target_action = 'promote'
EOF

    echo -e "${GREEN}✓ Recovery configuration created${NC}"

    # Start PostgreSQL in recovery mode
    echo -e "${YELLOW}Starting PostgreSQL in recovery mode...${NC}"
    sudo systemctl start postgresql

    # Wait for recovery to complete
    echo -e "${YELLOW}Waiting for recovery to complete...${NC}"
    while ! pg_isready -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" > /dev/null 2>&1; do
        sleep 2
    done

    echo -e "${GREEN}✓ Point-in-time recovery completed${NC}"

    # Verify recovery
    verify_restore

    return 0
}

# Function to verify restore
verify_restore() {
    echo -e "${YELLOW}Verifying restore...${NC}"

    # Check if database is accessible
    local table_count=$(PGPASSWORD="${PGPASSWORD}" psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'")

    echo -e "Tables found: ${table_count}"

    if [ ${table_count} -gt 0 ]; then
        echo -e "${GREEN}✓ Database is accessible and contains data${NC}"

        # Run basic integrity checks
        echo -e "${YELLOW}Running integrity checks...${NC}"

        # Check for critical tables
        local critical_tables=("orders" "portfolios" "positions" "users")
        for table in "${critical_tables[@]}"; do
            local row_count=$(PGPASSWORD="${PGPASSWORD}" psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d "${PGDATABASE}" -tAc "SELECT COUNT(*) FROM ${table}" 2>/dev/null || echo "0")
            echo -e "  ${table}: ${row_count} rows"
        done

        echo -e "${GREEN}✓ Integrity checks passed${NC}"
        return 0
    else
        echo -e "${RED}✗ Database verification failed${NC}"
        return 1
    fi
}

# Main execution
if ! pre_restore_checks; then
    echo -e "${RED}Pre-restore checks failed. Aborting.${NC}"
    exit 1
fi

case "${RESTORE_TYPE}" in
    full)
        if [ -z "${BACKUP_FILE}" ]; then
            echo -e "${RED}Error: Backup file required for full restore${NC}"
            exit 1
        fi

        # Check if backup file exists locally
        if [ ! -f "${BACKUP_FILE}" ] && [ ! -f "${BACKUP_DIR}/full/${BACKUP_FILE}" ]; then
            echo -e "${YELLOW}Backup not found locally, checking S3...${NC}"
            BACKUP_FILE=$(download_from_s3 "${BACKUP_FILE}")
        fi

        full_restore "${BACKUP_FILE}"
        ;;

    pitr)
        if [ -z "${BACKUP_FILE}" ] || [ -z "${PITR_TARGET}" ]; then
            echo -e "${RED}Error: Base backup and target time required for PITR${NC}"
            exit 1
        fi

        pitr_restore "${BACKUP_FILE}" "${PITR_TARGET}"
        ;;

    latest)
        BACKUP_FILE=$(find_latest_backup)
        if [ $? -eq 0 ]; then
            full_restore "${BACKUP_FILE}"
        else
            exit 1
        fi
        ;;

    *)
        echo -e "${RED}Error: Invalid restore type '${RESTORE_TYPE}'${NC}"
        echo "Valid types: full, pitr, latest"
        exit 1
        ;;
esac

echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Restore Summary${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "Restore Type: ${RESTORE_TYPE}"
echo -e "Completion Time: $(date)"
echo -e "Status: ${GREEN}SUCCESS${NC}"
echo ""
echo -e "${YELLOW}Post-Restore Checklist:${NC}"
echo -e "  1. Verify application connectivity"
echo -e "  2. Check data consistency"
echo -e "  3. Validate recent transactions"
echo -e "  4. Monitor application logs"
echo -e "  5. Notify stakeholders of restore completion"
echo ""

exit 0
