-- =============================================================================
-- amrutam-api schema (PostgreSQL 14+, public schema of the `amrutam` db).
--
-- Design notes:
--  * Every DDL statement is idempotent (IF NOT EXISTS) so `spring.sql.init.mode:
--    always` can safely re-run the script on every boot in dev / docker-compose.
--    Production should switch to `never` once a managed migration path
--    (Flyway / Liquibase) owns the schema.
--  * Primary keys are `uuid` columns with `DEFAULT gen_random_uuid()` (a
--    built-in PG13+ function, no extension required).
--  * All instants are `TIMESTAMPTZ`; the JDBC driver is pinned to UTC
--    (application.yml: spring.jackson.time-zone / hibernate jdbc time_zone).
--  * Money is NUMERIC(10,2) (INR by convention, stored as the string 'INR').
--  * Free-form payloads (audit details, idempotency results, job payloads)
--    are plain TEXT rather than JSONB: this keeps the schema portable
--    (H2 test mode included) and avoids jsonb operator functions. Parsing
--    happens in application code.
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username        varchar(64)  NOT NULL UNIQUE,
    password_hash   varchar(128) NOT NULL,
    display_name    varchar(128),
    email           varchar(160),
    role            varchar(16)  NOT NULL,
    mfa_secret      varchar(64),
    mfa_enabled     boolean      NOT NULL DEFAULT false,
    active          boolean      NOT NULL DEFAULT true,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

CREATE TABLE IF NOT EXISTS user_profiles (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             uuid NOT NULL UNIQUE,
    phone               varchar(32),
    dob                 date,
    gender              varchar(20),
    preferred_language  varchar(16),
    preferred_timezone  varchar(64),
    bio                 text,
    avatar_url          varchar(512),
    notes               text
);

CREATE TABLE IF NOT EXISTS doctors (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           uuid NOT NULL UNIQUE,
    specialization    varchar(160),
    consultation_fee  numeric(10,2),
    years_experience  integer NOT NULL DEFAULT 0,
    bio               text,
    avatar_url        varchar(512),
    ratings           double precision NOT NULL DEFAULT 0,
    total_ratings     integer NOT NULL DEFAULT 0,
    active            boolean NOT NULL DEFAULT true,
    created_at        timestamptz NOT NULL,
    updated_at        timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS availability_slots (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id        uuid NOT NULL,
    start_at         timestamptz NOT NULL,
    end_at           timestamptz NOT NULL,
    status           varchar(16) NOT NULL,
    locked_by_id     uuid,
    lock_expires_at  timestamptz,
    created_at       timestamptz NOT NULL,
    -- A doctor never owns two slots with the same window.
    CONSTRAINT uq_slots_doctor_window UNIQUE (doctor_id, start_at, end_at)
);
CREATE INDEX IF NOT EXISTS idx_slots_doctor_start ON availability_slots (doctor_id, start_at);

CREATE TABLE IF NOT EXISTS consultations (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id    uuid NOT NULL,
    doctor_id     uuid NOT NULL,
    slot_id       uuid NOT NULL,
    status        varchar(24) NOT NULL,
    scheduled_at  timestamptz NOT NULL,
    completed_at  timestamptz,
    notes         text,
    created_at    timestamptz NOT NULL,
    updated_at    timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_consultations_patient_doctor ON consultations (patient_id, doctor_id);
CREATE INDEX IF NOT EXISTS idx_consultations_status_sched ON consultations (status, scheduled_at);

CREATE TABLE IF NOT EXISTS prescriptions (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id  uuid NOT NULL UNIQUE,
    patient_id       uuid NOT NULL,
    doctor_id        uuid NOT NULL,
    medications      text,
    dosage_notes     text,
    created_at       timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id  uuid NOT NULL,
    amount           numeric(10,2) NOT NULL,
    currency         varchar(8) NOT NULL DEFAULT 'INR',
    status           varchar(16) NOT NULL,
    gateway_ref      varchar(128),
    idempotency_key  varchar(128) NOT NULL UNIQUE,
    created_at       timestamptz NOT NULL,
    updated_at       timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_payments_consultation ON payments (consultation_id);
CREATE INDEX IF NOT EXISTS idx_payments_status_created ON payments (status, created_at);

CREATE TABLE IF NOT EXISTS audit_logs (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    uuid,
    action      varchar(128) NOT NULL,
    target      varchar(128),
    ip          varchar(64),
    details     text,
    occurred_at timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_occurred ON audit_logs (occurred_at DESC);

-- Idempotency keys (run-once semantics for mutating requests).
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL,
    method     varchar(10)  NOT NULL,
    path       varchar(255) NOT NULL,
    body_hash  varchar(128) NOT NULL,
    state      varchar(16)  NOT NULL,
    result_json text,
    created_at timestamptz  NOT NULL,
    updated_at timestamptz  NOT NULL,
    -- One active logical operation per (user, method, path, body hash).
    CONSTRAINT uq_idempotency_key UNIQUE (user_id, method, path, body_hash)
);

CREATE TABLE IF NOT EXISTS async_jobs (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid,
    type         varchar(64) NOT NULL,
    status       varchar(16) NOT NULL,
    payload      text,
    attempt      integer NOT NULL DEFAULT 0,
    max_attempt  integer NOT NULL DEFAULT 5,
    scheduled_at timestamptz NOT NULL,
    last_run_at  timestamptz,
    next_run_at  timestamptz,
    lease_owner  varchar(128),
    lease_expiry timestamptz,
    created_at   timestamptz NOT NULL,
    updated_at   timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_jobs_status_next ON async_jobs (status, next_run_at);

CREATE TABLE IF NOT EXISTS saga_steps (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    saga_id     uuid NOT NULL,
    step_index  integer NOT NULL,
    step_name   varchar(64) NOT NULL,
    status      varchar(16) NOT NULL,
    detail      text,
    created_at  timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_saga_steps_saga ON saga_steps (saga_id, step_index);
