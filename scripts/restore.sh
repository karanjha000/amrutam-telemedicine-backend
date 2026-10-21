#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"

if [[ $# -gt 0 ]]; then
  ARCHIVE_PATH="$1"
else
  ARCHIVE_PATH="$(ls -1t "$BACKUP_DIR"/amrutam-backup-*.tar.gz 2>/dev/null | head -n 1 || true)"
fi

if [[ -z "${ARCHIVE_PATH:-}" || ! -f "$ARCHIVE_PATH" ]]; then
  echo "No backup archive found in $BACKUP_DIR" >&2
  echo "Usage: $0 [/path/to/amrutam-backup-YYYYMMDDTHHMMSSZ.tar.gz]" >&2
  exit 1
fi

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

tar -xzf "$ARCHIVE_PATH" -C "$WORK_DIR"

DB_SQL="$(find "$WORK_DIR" -maxdepth 1 -type f -name 'amrutam-postgres-*.sql' | head -n 1 || true)"
REDIS_RDB="$(find "$WORK_DIR" -maxdepth 1 -type f -name 'amrutam-redis-*.rdb' | head -n 1 || true)"

if [[ -z "$DB_SQL" ]]; then
  echo "Backup archive did not contain a PostgreSQL dump." >&2
  exit 1
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-amrutam}"
DB_USER="${DB_USER:-amrutam}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required to restore the PostgreSQL backup." >&2
  exit 1
fi

echo "Restoring PostgreSQL backup from ${DB_SQL}"
PGPASSWORD="${DB_PASSWORD:-amrutam}" psql \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d postgres \
  -v ON_ERROR_STOP=1 \
  -c "DROP DATABASE IF EXISTS \"$DB_NAME\";"

PGPASSWORD="${DB_PASSWORD:-amrutam}" psql \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d postgres \
  -v ON_ERROR_STOP=1 \
  -c "CREATE DATABASE \"$DB_NAME\";"

PGPASSWORD="${DB_PASSWORD:-amrutam}" psql \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  -v ON_ERROR_STOP=1 \
  -f "$DB_SQL"

echo "PostgreSQL restore complete."

if [[ -n "$REDIS_RDB" ]]; then
  echo "Redis backup file found at ${REDIS_RDB}"
  echo "Copy this file into the Redis persistence directory and restart Redis before resuming traffic."
  echo "Example: cp ${REDIS_RDB} /var/lib/redis/dump.rdb && docker restart redis"
  echo "Alternatively, if you are running a managed Redis instance, restore the snapshot through the platform's backup workflow."
else
  echo "No Redis snapshot included in the archive."
fi

echo "Backup restore process finished."
