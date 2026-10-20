-- H2 (MODE=PostgreSQL) mirror of the PostgreSQL schema.sql.
--
-- Differences from the Postgres DDL:
--  * PKs use H2's native `uuid` type with no DEFAULT expression — JPA
--    @GeneratedValue fills ids on save; H2 2.x supports the uuid type
--    natively in PostgreSQL mode.
--  * TIMESTAMPTZ is written as `timestamp with time zone` (H2 keyword form);
--    plain `TIMESTAMP` would silently drop the zone.
--  * NUMERIC / DECIMAL, TEXT and VARCHAR map identically in both.

DROP TABLE IF EXISTS availability_slots;
DROP TABLE IF EXISTS consultations;
DROP TABLE IF EXISTS prescriptions;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS idempotency_keys;
DROP TABLE IF EXISTS async_jobs;
DROP TABLE IF EXISTS saga_steps;
DROP TABLE IF EXISTS doctors;
DROP TABLE IF EXISTS user_profiles;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id              uuid PRIMARY KEY,
    username        varchar(64)  NOT NULL UNIQUE,
    password_hash   varchar(128) NOT NULL,
    display_name    varchar(128),
    email           varchar(160),
    role            varchar(16)  NOT NULL,
    mfa_secret      varchar(64),
    mfa_enabled     boolean      NOT NULL DEFAULT false,
    active          boolean      NOT NULL DEFAULT true,
    created_at      timestamp with time zone NOT NULL,
    updated_at      timestamp with time zone NOT NULL
);
CREATE INDEX idx_users_email ON users (email);

CREATE TABLE user_profiles (
    id                  uuid PRIMARY KEY,
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

CREATE TABLE doctors (
    id                uuid PRIMARY KEY,
    user_id           uuid NOT NULL UNIQUE,
    specialization    varchar(160),
    consultation_fee  decimal(10,2),
    years_experience  integer NOT NULL DEFAULT 0,
    bio               text,
    avatar_url        varchar(512),
    ratings           double precision NOT NULL DEFAULT 0,
    total_ratings     integer NOT NULL DEFAULT 0,
    active            boolean NOT NULL DEFAULT true,
    created_at        timestamp with time zone NOT NULL,
    updated_at        timestamp with time zone NOT NULL
);

CREATE TABLE availability_slots (
    id               uuid PRIMARY KEY,
    doctor_id        uuid NOT NULL,
    start_at         timestamp with time zone NOT NULL,
    end_at           timestamp with time zone NOT NULL,
    status           varchar(16) NOT NULL,
    locked_by_id     uuid,
    lock_expires_at  timestamp with time zone,
    created_at       timestamp with time zone NOT NULL,
    CONSTRAINT uq_slots_doctor_window UNIQUE (doctor_id, start_at, end_at)
);
CREATE INDEX idx_slots_doctor_start ON availability_slots (doctor_id, start_at);

CREATE TABLE consultations (
    id            uuid PRIMARY KEY,
    patient_id    uuid NOT NULL,
    doctor_id     uuid NOT NULL,
    slot_id       uuid NOT NULL,
    status        varchar(24) NOT NULL,
    scheduled_at  timestamp with time zone NOT NULL,
    completed_at  timestamp with time zone,
    notes         text,
    created_at    timestamp with time zone NOT NULL,
    updated_at    timestamp with time zone NOT NULL
);
CREATE INDEX idx_consultations_patient_doctor ON consultations (patient_id, doctor_id);
CREATE INDEX idx_consultations_status_sched ON consultations (status, scheduled_at);

CREATE TABLE prescriptions (
    id               uuid PRIMARY KEY,
    consultation_id  uuid NOT NULL UNIQUE,
    patient_id       uuid NOT NULL,
    doctor_id        uuid NOT NULL,
    medications      text,
    dosage_notes     text,
    created_at       timestamp with time zone NOT NULL
);

CREATE TABLE payments (
    id               uuid PRIMARY KEY,
    consultation_id  uuid NOT NULL,
    amount           decimal(10,2) NOT NULL,
    currency         varchar(8) NOT NULL DEFAULT 'INR',
    status           varchar(16) NOT NULL,
    gateway_ref      varchar(128),
    idempotency_key  varchar(128) NOT NULL UNIQUE,
    created_at       timestamp with time zone NOT NULL,
    updated_at       timestamp with time zone NOT NULL
);
CREATE INDEX idx_payments_consultation ON payments (consultation_id);
CREATE INDEX idx_payments_status_created ON payments (status, created_at);

CREATE TABLE audit_logs (
    id          uuid PRIMARY KEY,
    actor_id    uuid,
    action      varchar(128) NOT NULL,
    target      varchar(128),
    ip          varchar(64),
    details     text,
    occurred_at timestamp with time zone NOT NULL
);
CREATE INDEX idx_audit_occurred ON audit_logs (occurred_at);

CREATE TABLE idempotency_keys (
    id         uuid PRIMARY KEY,
    user_id    uuid NOT NULL,
    method     varchar(10)  NOT NULL,
    path       varchar(255) NOT NULL,
    body_hash  varchar(128) NOT NULL,
    state      varchar(16)  NOT NULL,
    result_json text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    CONSTRAINT uq_idempotency_key UNIQUE (user_id, method, path, body_hash)
);

CREATE TABLE async_jobs (
    id           uuid PRIMARY KEY,
    user_id      uuid,
    type         varchar(64) NOT NULL,
    status       varchar(16) NOT NULL,
    payload      text,
    attempt      integer NOT NULL DEFAULT 0,
    max_attempt  integer NOT NULL DEFAULT 5,
    scheduled_at timestamp with time zone NOT NULL,
    last_run_at  timestamp with time zone,
    next_run_at  timestamp with time zone,
    lease_owner  varchar(128),
    lease_expiry timestamp with time zone,
    created_at   timestamp with time zone NOT NULL,
    updated_at   timestamp with time zone NOT NULL
);
CREATE INDEX idx_jobs_status_next ON async_jobs (status, next_run_at);

CREATE TABLE saga_steps (
    id          uuid PRIMARY KEY,
    saga_id     uuid NOT NULL,
    step_index  integer NOT NULL,
    step_name   varchar(64) NOT NULL,
    status      varchar(16) NOT NULL,
    detail      text,
    created_at  timestamp with time zone NOT NULL
);
CREATE INDEX idx_saga_steps_saga ON saga_steps (saga_id, step_index);
