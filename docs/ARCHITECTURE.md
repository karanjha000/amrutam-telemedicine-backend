# Amrutam Architecture

Module map, data flow, booking saga, ER model, async/worker boundary,
caching, partitioning and DR notes for the Amrutam telemedicine platform.

## 1. Overview

Amrutam is a Java 21 / Spring Boot 3.3.x telemedicine backend composed of
three Maven modules plus shared infrastructure (PostgreSQL 16 + Redis 7):

| Module | Responsibility | Owns |
| --- | --- | --- |
| amrutam-common | Shared kernel: security (JWT filter, JwTokenService, Claims), exceptions, idempotency SPI + default impl, rate limiting, audit SPI, MDC trace-id | No tables; pure SPIs + beans |
| amrutam-api | REST + GraphQL surface; booking saga; JPA persistence for ALL domain entities | All entity + repository classes, schema.sql, data.sql, async_jobs/saga_steps producers |
| amrutam-workers | Background: job poller (async_jobs), saga reconciliation, notification fan-out, audit stream consumer | No JPA entities; raw JdbcTemplate SQL only |

The module dependency graph is strictly acyclic: api depends on common;
workers depends on common; api depends on workers (the api reads
worker-produced rows through its own JPA entities, never through a
compile-time import of worker classes).

## 2. Module boundaries and the shared-JPA ownership rule

Single source of truth for the schema lives in amrutam-api. Every JPA
entity and Spring Data repository - including the worker-facing
async_jobs, saga_steps and audit_logs tables - is declared under
amrutam-api/src/main/java/com/amrutam/api/entity and .../repository.

Consequences:

- amrutam-workers owns NO JPA entity and NO Spring Data repository. It
  reaches the database with parameterized JdbcTemplate SQL against the
  same tables (see JdbcTemplateJobStore, AuditLogWriter).
- The workers module never imports com.amrutam.api.** classes.
- Because both processes share one datasource, schema changes are made
  once, in the api module's schema.sql / JPA model, and redeployed to both.
- If a worker feature needs a new table, add the DDL + entity to
  amrutam-api first, then reference it by plain SQL from workers.

## 3. Data flow

        Browser / Mobile
        (JWT bearer)
              |
              v
        +--------------------------------+
        |           amrutam-api          |
        |  AuthHeaderFilter ->          |
        |  SecurityConfig /             |
        |  @PreAuthorize                |
        +------+--------------+---------+
               |              |
               | REST/GraphQL | Booking saga
               |              | IdempotencyService (common)
               v              v
        +-----------------------------------------------+
        |              PostgreSQL 16                    |
        | users, doctors, availability_slots,           |
        | consultations, payments, prescriptions,       |
        | audit_logs, idempotency_keys, saga_steps,     |
        | async_jobs                                     |
        +-------+----------------------+----------------+
                |                      |
   JdbcTemplate |   PENDING rows       |
        SQL      v                      v
        +-----------------------------------------------+
        |            amrutam-workers                    |
        |  JobProcessor -> JobHandler (notify)          |
        |  BookingSagaOrchestrator (reconcile)          |
        |  AuditWorker (Redis stream -> audit_logs)     |
        +-----------------------------------------------+
                ^
                | Redis 7: audit stream, notification
                | sink, cache
                |

## 4. Booking saga

### 4.1 Sequence

        Client                     amrutam-api (BookingService)     PostgreSQL     AsyncBridge
           |  POST /consultations/booking                                |             |
           |------------------------------------------------------------->|            |
           |                                     1 load slot (404/409)  |             |
           |                                     2 insert consultation   |             |
           |                                       PENDING_PAYMENT       |             |
           |                                     3 lock slot             |             |
           |                                       AVAILABLE->LOCKED     |             |
           |                                       (409 on race)         |             |
           |                                     4 insert payment PAYING |             |
           |                                       (dedupe by key)       |             |
           |                                     5 capture PAYING->      |             |
           |                                       CAPTURED, consult PAID|            |
           |                                     6 enqueue async job ----+--> PENDING  |
           |                                     + saga_steps rows       |             |
           |<- 201 {consultationId, slotId, paymentId, status} ---------|             |
           |                                                            |             |
        amrutam-workers (later poll):
           |                                     claim PENDING job (lease, attempt++)
           |                                     run JobHandler; complete/retry/dead

Numbered steps:

1. Load slot - 404 when unknown, 409 when not AVAILABLE.
2. Create consultation - status PENDING_PAYMENT, scheduledAt = slot.startAt.
3. Lock slot - atomic UPDATE ... WHERE status='AVAILABLE'; 0 rows means a
   competing booking won, so a ConflictException (HTTP 409) is raised.
4. Create payment - status PAYING; a duplicate idempotency key returns the
   existing payment instead of a second charge.
5. Capture - PAYING -> CAPTURED (simulated gateway in this scope); the
   consultation becomes PAID. On failure, compensation runs in the same
   transaction.
6. Enqueue - a consultation.scheduled-notification job is written to
   async_jobs via a REQUIRES_NEW transaction (durable even if the outer
   transaction rolls back), plus saga_steps rows for the trace.

### 4.2 Compensation matrix

| Failed step | Compensation (reverse order) |
| --- | --- |
| 5 capture fails | payments.markFailed + slots.unlockSlot + consultation CANCELLED + saga step "compensation" |
| 4 payment insert fails | nothing created yet; outer transaction rolls back |
| 3 lock race | ConflictException (409); consultation rolls back |
| 1 slot missing | NotFoundException (404) |

A PENDING_PAYMENT consultation that never reaches PAID (crash between
steps 4 and 5) is picked up by the workers' BookingSagaOrchestrator, which
releases the slot and cancels it - the out-of-band backstop.

## 5. ER diagram

        users ||--o| user_profiles : has
        users ||--o| doctors       : backs
        doctors ||--o| availability_slots : owns
        users ||--o{ consultations  : patient
        doctors ||--o{ consultations : doctor
        availability_slots ||--o| consultations : slot-for
        consultations ||--o| payments          : paid-by
        consultations ||--o| prescriptions     : prescribed
        users ||--o{ idempotency_keys  : issued
        users ||--o{ async_jobs        : owns
        consultations ||--o{ saga_steps : traced-by

Entity columns (key fields only):

- users: id (uuid PK), username (unique), password_hash, role
  (PATIENT|DOCTOR|ADMIN), mfa_enabled, active, created_at/updated_at
- doctors: id, user_id (unique), specialization, consultation_fee,
  years_experience, ratings, total_ratings, active
- availability_slots: id, doctor_id, start_at, end_at, status
  (AVAILABLE|LOCKED|BOOKED|CANCELLED), locked_by_id, lock_expires_at;
  unique (doctor_id, start_at, end_at)
- consultations: id, patient_id, doctor_id, slot_id, status
  (PENDING_PAYMENT|PAID|COMPLETED|CANCELLED|NO_SHOW), scheduled_at,
  completed_at, notes
- prescriptions: id, consultation_id (unique), patient_id, doctor_id,
  medications (JSON text), dosage_notes
- payments: id, consultation_id, amount, currency (default INR), status
  (CREATED|PAYING|CAPTURED|FAILED|REFUNDED), gateway_ref,
  idempotency_key (unique)
- audit_logs: id, actor_id, action, target, ip, details, occurred_at
- idempotency_keys: id, user_id, method, path, body_hash, state
  (PENDING|COMPLETED|FAILED), result_json; unique
  (user_id, method, path, body_hash)
- saga_steps: id, saga_id, step_index, step_name, status, detail
- async_jobs: id, user_id, type, status (PENDING|IN_PROGRESS|DONE|FAILED|DEAD),
  payload, attempt, max_attempt, scheduled_at, last_run_at, next_run_at,
  lease_owner, lease_expiry

## 6. Async jobs: retry, backoff, dead-letter

- Producer: JdbcAsyncBridge.enqueueAsyncJob writes a PENDING row with
  attempt=0, max_attempt=5, next_run_at NULL (due immediately).
- Consumer: workers poll findDueJobs(before) (status PENDING and
  next_run_at IS NULL OR next_run_at <= before), claim under a lease
  (PENDING -> IN_PROGRESS, attempt += 1).
- On success: DONE.
- On failure: scheduleRetry(nextRun) back to PENDING with
  next_run_at = now + min(base * 2^(attempt-1), cap); when
  attempt > maxRetries the job is dead-lettered to status DEAD.
- Health signals: countByStatus(PENDING) and countByStatus(DEAD) feed the
  SystemHealthResponse admin endpoint.

## 7. Caching strategy

CacheConfig picks a RedisCacheManager when amrutam.cache.redis-enabled is
true (default), else an in-memory InMemoryCacheManager with identical TTLs.
Per-cache TTLs and key namespaces:

| Cache | TTL | Namespace |
| --- | --- | --- |
| users | 5m | users:* |
| doctors | 15m | doctors:* |
| availability | 60s | availability:<doctorId>:<from>:<to> |
| consultations | 30s | consultations:* |
| analytics | 5m | analytics:* |

Null values are disabled for caching; the in-memory manager mirrors the
same TTLs locally for dev/tests.

## 8. Partitioning (future ops step)

schema.sql is a single-partition baseline today. When row counts grow,
the documented ops path is:

- consultations -> RANGE partition by scheduled_at (monthly)
- audit_logs -> RANGE partition by occurred_at (weekly)
- payments / idempotency_keys -> RANGE by created_at when hot

Adopt a managed migration tool (e.g. Flyway) in a future module;
spring.sql.init.mode=always + schema.sql is the dev/docker path only.

## 9. DR / backup

- PostgreSQL: pg_dump logical backups (RPO target 15 min in prod); PITR
  via WAL archiving as a follow-up. RTO target under 30 min for standby.
- Redis: AOF everysec; cache is rebuildable (may lose up to one TTL
  window after a restart).
- async_jobs / saga_steps: live in Postgres, so they inherit the same
  RPO/RTO; a crash mid-saga is recovered by the workers'
  BookingSagaOrchestrator reconciliation sweep.
