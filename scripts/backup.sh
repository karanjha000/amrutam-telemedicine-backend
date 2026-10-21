#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
mkdir -p "$BACKUP_DIR"

TS="$(date -u +%Y%m%dT%H%M%SZ)"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-amrutam}"
DB_USER="${DB_USER:-amrutam}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"

PG_DUMP_BIN="${PG_DUMP_BIN:-pg_dump}"
REDIS_CLI_BIN="${REDIS_CLI_BIN:-redis-cli}"

DUMP_FILE="$BACKUP_DIR/amrutam-postgres-${TS}.sql"
REDIS_FILE="$BACKUP_DIR/amrutam-redis-${TS}.rdb"

echo "Backing up PostgreSQL database ${DB_NAME} to ${DUMP_FILE}"
PGPASSWORD="${DB_PASSWORD:-amrutam}" "$PG_DUMP_BIN" \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  --clean --if-exists \
  > "$DUMP_FILE"

echo "Performing Redis snapshot to ${REDIS_FILE}"
"$REDIS_CLI_BIN" -h "$REDIS_HOST" -p "$REDIS_PORT" SAVE >/dev/null
"$REDIS_CLI_BIN" -h "$REDIS_HOST" -p "$REDIS_PORT" --rdb "$REDIS_FILE" >/dev/null

ARCHIVE_FILE="$BACKUP_DIR/amrutam-backup-${TS}.tar.gz"
tar -czf "$ARCHIVE_FILE" -C "$BACKUP_DIR" "$(basename "$DUMP_FILE")" "$(basename "$REDIS_FILE")"
rm -f "$DUMP_FILE" "$REDIS_FILE"

echo "Backup archive created: ${ARCHIVE_FILE}"
