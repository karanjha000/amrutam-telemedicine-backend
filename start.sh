#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required but not found in PATH." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose is required but is not available." >&2
  exit 1
fi

echo "Starting Amrutam stack with Docker Compose..."
docker compose up --build --remove-orphans -d

echo "Amrutam stack started."
echo "Frontend: http://localhost"
echo "API: http://localhost:8080"
echo "Swagger: http://localhost:8080/swagger-ui.html"
