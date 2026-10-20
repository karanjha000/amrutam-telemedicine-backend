-- H2 (PostgreSQL mode) schema for amrutam-workers tests.
--
-- Column sets mirror amrutam-api's schema.sql for the four shared tables
-- that this module touches via raw JDBC (async_jobs, consultations,
-- payments, availability_slots) plus audit_logs (drain target). H2 2.x in
-- MODE=PostgreSQL supports the `uuid` type natively.

DROP TABLE IF EXISTS availability_slots;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS consultations;
DROP TABLE IF EXISTS async_jobs;
DROP TABLE IF EXISTS audit_logs;

CREATE TABLE availability_slots (
    id uuid PRIMARY KEY,
    doctor_id uuid NOT NULL,
    start_at timestamp WITH TIME ZONE NOT NULL,
    end_at timestamp WITH TIME ZONE NOT NULL,
    status text NOT NULL,
    locked_by_id uuid,
    lock_expires_at timestamp WITH TIME ZONE,
    created_at timestamp WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consultations (
    id uuid PRIMARY KEY,
    doctor_id uuid NOT NULL,
    patient_id uuid NOT NULL,
    slot_id uuid,
    status text NOT NULL,
    created_at timestamp WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payments (
    id uuid PRIMARY KEY,
    consultation_id uuid NOT NULL,
    amount numeric(10, 2) NOT NULL,
    currency text NOT NULL DEFAULT 'INR',
    status text NOT NULL,
    created_at timestamp WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE async_jobs (
    id uuid PRIMARY KEY,
    type text NOT NULL,
    status text NOT NULL,
    payload text,
    attempt integer NOT NULL DEFAULT 0,
    max_attempt integer NOT NULL DEFAULT 5,
    scheduled_at timestamp WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_run_at timestamp WITH TIME ZONE,
    next_run_at timestamp WITH TIME ZONE,
    lease_owner text,
    lease_expiry timestamp WITH TIME ZONE,
    created_at timestamp WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id uuid PRIMARY KEY,
    actor_id uuid,
    action text NOT NULL,
    target text,
    ip text,
    details text,
    occurred_at timestamp WITH TIME ZONE NOT NULL
);
