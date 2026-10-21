#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE_NAME="${IMAGE_NAME:-amrutam:local}"

cd "$ROOT_DIR"

echo "[1/4] Building application image"
docker build -t "$IMAGE_NAME" .

echo "[2/4] Validating docker-compose config"
docker compose config >/dev/null

echo "[3/4] Bringing stack up for a smoke check"
docker compose up -d --build

echo "[4/4] Waiting for health endpoint"
for attempt in $(seq 1 30); do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "Deployment readiness check passed."
    docker compose down --remove-orphans
    exit 0
  fi
  sleep 2
done

echo "Health check failed after retries." >&2
docker compose down --remove-orphans || true
exit 1
