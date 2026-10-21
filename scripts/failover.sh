#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"
DRY_RUN="${DRY_RUN:-false}"

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

echo "Selected backup: ${ARCHIVE_PATH}"

if [[ "$DRY_RUN" == "true" ]]; then
  echo "DRY RUN: this recovery would stop app services, restore the latest backup, and restart the stack."
  echo "To perform the cutover, run: DRY_RUN=false $0 "$ARCHIVE_PATH""
  exit 0
fi

echo "Stopping application services before cutover..."
if command -v docker >/dev/null 2>&1; then
  docker compose -f "$ROOT_DIR/docker-compose.yml" stop api workers || true
else
  echo "docker is not installed or not on PATH; skipping service shutdown."
fi

echo "Running restore flow..."
"$ROOT_DIR/scripts/restore.sh" "$ARCHIVE_PATH"

echo "Starting application services after cutover..."
if command -v docker >/dev/null 2>&1; then
  docker compose -f "$ROOT_DIR/docker-compose.yml" up -d postgres redis api workers || true
else
  echo "docker is not installed or not on PATH; manual start is required."
fi

echo "Failover completed. Verify the API health endpoint and the worker backlog before returning to production traffic."
