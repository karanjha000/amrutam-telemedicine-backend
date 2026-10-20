# Amrutam — Setup & Run Guide

Amrutam is a Java 21 / Spring Boot 3.3.x multi-module Maven project
(telemedicine backend). This guide covers prerequisites, local run,
docker-compose, tests, and sample API calls. See `docs/` for architecture,
security, and threat-model details.

## 1. Prerequisites

| Tool | Version |
| --- | --- |
| JDK | 21 (Temurin recommended) |
| Maven | 3.9.9+ |
| Docker + Docker Compose | any recent |
| PostgreSQL | 16 (or let compose start it) |
| Redis | 7 (optional in dev; `amrutam.cache.redis-enabled=false` disables it) |

Verify:

```bash
java -version   # 21
mvn -v          # 3.9.x
```

## 2. Clone & build

```bash
git clone <repo> amrutam
cd amrutam
mvn -B -ntp clean install
```

The root reactor builds `amrutam-common` → `amrutam-workers` → `amrutam-api`
in that order. A successful build produces re-packaged Spring Boot jars
`amrutam-api/target/amrutam-api-1.0.0-SNAPSHOT.jar` and
`amrutam-workers/target/amrutam-workers-1.0.0-SNAPSHOT.jar` (common is a
plain jar).

## 3. Database

Create the database once (the schema itself is auto-applied by
`spring.sql.init.mode=always` + `schema.sql`):

```sql
CREATE DATABASE amrutam;
CREATE USER amrutam WITH PASSWORD 'amrutam';
GRANT ALL PRIVILEGES ON DATABASE amrutam TO amrutam;
```

The app connects via `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`
(defaults: localhost / 5432 / amrutam / amrutam / amrutam).

## 4. Run the API locally

Prereq: PostgreSQL running (and Redis if you want caching on).

```bash
mvn -pl amrutam-api spring-boot:run
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html` (groups `api-v1` and
  `admin`), OpenAPI: `http://localhost:8080/v3/api-docs`
- Actuator: `http://localhost:8080/actuator/health`, `/actuator/prometheus`

Run without a Redis server by disabling the cache:

```bash
mvn -pl amrutam-api spring-boot:run -Dspring-boot.run.profiles=dev
```

(the `dev` profile sets `amrutam.cache.redis-enabled=false`.)

## 5. Run the workers locally

> The `amrutam-workers` module is a sibling-delivered artifact: it may not
> exist on disk yet when you first read this file. The intended command is:

```bash
mvn -pl amrutam-workers spring-boot:run
```

Once present, the workers poll `async_jobs`, reconcile stuck booking sagas,
fan out notifications, and drain the Redis audit stream.

## 6. Docker compose (full stack)

```bash
docker compose up -d
```

Brings up: `postgres` (16-alpine), `redis` (7-alpine), `api` and
`workers` (same multi-role image, selected by `AMRUTAM_ROLE`). The API is
on host port 8080.

## 7. Health checks

```bash
curl http://localhost:8080/actuator/health
```

## 8. Sample API calls

Register a patient:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"secret123","email":"alice@example.com","displayName":"Alice","phone":"+91-0000000000"}'
```

Log in (seed admin: username `admin`, password `admin123`):

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
# -> {"success":true,"data":{"token":"<jwt>","mfaChallenge":null,"user":{...}}}
```

Book a consultation (requires a valid patient JWT; add
`X-Idempotency-Key` to make it idempotent):

```bash
curl -s -X POST http://localhost:8080/api/v1/consultations/booking \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'X-Idempotency-Key: book-1' \
  -d '{"doctorId":"<uuid>","slotId":"<uuid>","paymentMethod":"card","paymentToken":"tok"}'
```

List a doctor's availability:

```bash
curl -s "http://localhost:8080/api/v1/doctors/<doctorId>/availability?from=2026-10-21T00:00:00Z&to=2026-10-31T23:59:59Z"
```

Admin analytics (requires admin JWT):

```bash
curl -s -X POST http://localhost:8080/api/v1/admin/analytics/daily-consultations \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"date":"2026-10-20"}'
```

## 9. Tests

Run the api module's unit + slice tests against H2 (no Docker needed):

```bash
mvn test -pl amrutam-api
```

Optionally run the real-PostgreSQL Testcontainers integration test by
enabling the env flag (requires Docker):

```bash
AMRUTAM_TEST_PG=true mvn test -pl amrutam-api
```

(Without the flag, `AmrutamPostgresIntegrationTest` is skipped, not failed.)
