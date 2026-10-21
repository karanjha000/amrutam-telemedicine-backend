# syntax=docker/dockerfile:1

# =============================================================================
# Amrutam multi-role image.
#
# One image carries BOTH the api and workers Spring Boot jars. The entrypoint
# selects which to run via AMRUTAM_ROLE=api|workers, so docker-compose can
# deploy the same image twice with different roles/env.
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: build all three Maven modules from the canonical backend reactor
# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MAVEN_OPTS="-Xmx1g"
WORKDIR /build

# The source of truth for the backend reactor lives under backend/, not at the
# repository root. Copy the parent POM and all module sources so a single build
# covers common -> workers -> api.
COPY backend/pom.xml ./pom.xml
COPY backend/amrutam-common backend/amrutam-common
COPY backend/amrutam-workers backend/amrutam-workers
COPY backend/amrutam-api backend/amrutam-api

# Install all modules (api and workers produce re-packaged boot jars; common
# is a plain jar). Skip tests in the image build; CI runs them separately.
RUN mvn -B -ntp -DskipTests clean install

# ---------------------------------------------------------------------------
# Stage 2: minimal runtime JRE with both jars
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Both re-packaged Spring Boot jars (only api and workers use the
# spring-boot-maven-plugin; common is a plain jar).
COPY --from=build /build/backend/amrutam-api/target/amrutam-api-*.jar  /app/api.jar
COPY --from=build /build/backend/amrutam-workers/target/amrutam-workers-*.jar /app/workers.jar

COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENV AMRUTAM_ROLE=api
EXPOSE 8080

# HEALTHCHECK is api-only (workers has no /actuator/health surface in scope).
# docker-compose overrides this per-service when needed.
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD ["/bin/sh", "-c", "AMRUTAM_ROLE=api && curl -fs http://localhost:8080/actuator/health || exit 1"]

ENTRYPOINT ["/app/entrypoint.sh"]
