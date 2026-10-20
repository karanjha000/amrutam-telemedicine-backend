#!/bin/sh
# Selects which Spring Boot jar to run from the multi-role Amrutam image.
#
#   AMRUTAM_ROLE=api     -> /app/api.jar
#   AMRUTAM_ROLE=workers -> /app/workers.jar
#
# Compose uses the SAME image for both services and only varies AMRUTAM_ROLE
# plus the per-role env (DB/Redis hosts, JWT secret, workers-enabled flags).
set -eu

ROLE="${AMRUTAM_ROLE:-api}"
case "$ROLE" in
  api)
    JAR=/app/api.jar
    ;;
  workers)
    JAR=/app/workers.jar
    ;;
  *)
    echo "ERROR: AMRUTAM_ROLE must be 'api' or 'workers' (got: $ROLE)" >&2
    exit 1
    ;;
esac

# JAVA_OPTS is honored by operators (e.g. -Xmx, GC flags).
exec java $JAVA_OPTS -jar "$JAR"
