-- Optional partitioning migration for production sizing.
-- This is intentionally not enabled in the dev schema because the local docker
-- stack is intentionally simple and single-partition by default.
--
-- Example: create monthly partitions for consultations and audit_logs.
-- Run this only against a production PostgreSQL database after confirming the
-- current table structures and backup plan.

BEGIN;

-- Example: consultation partitioning by scheduled_at.
-- ALTER TABLE consultations
--   PARTITION BY RANGE (scheduled_at);
--
-- CREATE TABLE consultations_p2026_10 PARTITION OF consultations
-- FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');
--
-- CREATE TABLE consultations_p2026_11 PARTITION OF consultations
-- FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');

-- Example: audit log partitioning by occurred_at.
-- ALTER TABLE audit_logs
--   PARTITION BY RANGE (occurred_at);
--
-- CREATE TABLE audit_logs_p2026_10 PARTITION OF audit_logs
-- FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');

COMMIT;
