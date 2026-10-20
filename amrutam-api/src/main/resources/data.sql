-- Amrutam API seed data (PostgreSQL).
--
-- Every INSERT is `ON CONFLICT DO NOTHING` so the file is safe to re-run
-- under spring.sql.init.mode=always (idempotent-friendly boot).
--
-- Passwords are seeded with PLACEHOLDER hashes for all users except the
-- admin. The DevAdminSeedRunner (profile "dev" only) upgrades every
-- placeholder hash to a real BCrypt value at boot time, so logins work in
-- dev. The prod profile never activates that runner; prod users are
-- created through the API with their own passwords.
--
-- Placeholder-hash sentinel: any hash starting with "$2a$10$PLACEHOLDER"
-- is recognised by the runner and upgraded in place.

-- Admin (password "admin123" — upgraded by the dev seed runner; see the
-- placeholder-hash note above).
INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000a1', 'admin',
     '$2a$10$PLACEHOLDER', 'Amrutam Admin', 'admin@amrutam.local', 'ADMIN',
     FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

-- Three doctor users.
INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000d1', 'doc.ramesh',
     '$2a$10$PLACEHOLDER', 'Dr. Ramesh Kumar', 'ramesh@amrutam.local',
     'DOCTOR', FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000d2', 'doc.aisha',
     '$2a$10$PLACEHOLDER', 'Dr. Aisha Sen', 'aisha@amrutam.local',
     'DOCTOR', FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000d3', 'doc.iyer',
     '$2a$10$PLACEHOLDER', 'Dr. Iyer Nair', 'iyer@amrutam.local',
     'DOCTOR', FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

-- Doctor profiles keyed to the doctor user ids above.
INSERT INTO doctors (id, user_id, specialization, consultation_fee, years_experience,
                     bio, ratings, total_ratings, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000f1', '00000000-0000-0000-0000-0000000000d1',
     'Cardiology', 800.00, 12,
     'Cardiologist focused on preventive care and tele-consults.',
     4.6, 120, TRUE, now(), now())
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO doctors (id, user_id, specialization, consultation_fee, years_experience,
                     bio, ratings, total_ratings, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000f2', '00000000-0000-0000-0000-0000000000d2',
     'Pediatrics', 600.00, 8,
     'Pediatrician with a focus on childhood asthma management.',
     4.8, 90, TRUE, now(), now())
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO doctors (id, user_id, specialization, consultation_fee, years_experience,
                     bio, ratings, total_ratings, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000f3', '00000000-0000-0000-0000-0000000000d3',
     'General Medicine', 500.00, 15,
     'General physician; chronic-disease follow-up specialist.',
     4.5, 210, TRUE, now(), now())
ON CONFLICT (user_id) DO NOTHING;

-- Five availability slots (windows relative to boot time; the fixed uuid PKs
-- keep ON CONFLICT deterministic).
INSERT INTO availability_slots (id, doctor_id, start_at, end_at, status, created_at)
VALUES
    ('00000000-0000-0000-0000-0000000000s1', '00000000-0000-0000-0000-0000000000f1',
     now() + INTERVAL '1 day', now() + INTERVAL '1 day' + INTERVAL '30 minutes', 'AVAILABLE', now())
ON CONFLICT (doctor_id, start_at, end_at) DO NOTHING;

INSERT INTO availability_slots (id, doctor_id, start_at, end_at, status, created_at)
VALUES
    ('00000000-0000-0000-0000-0000000000s2', '00000000-0000-0000-0000-0000000000f1',
     now() + INTERVAL '2 days', now() + INTERVAL '2 days' + INTERVAL '30 minutes', 'AVAILABLE', now())
ON CONFLICT (doctor_id, start_at, end_at) DO NOTHING;

INSERT INTO availability_slots (id, doctor_id, start_at, end_at, status, created_at)
VALUES
    ('00000000-0000-0000-0000-0000000000s3', '00000000-0000-0000-0000-0000000000f2',
     now() + INTERVAL '1 day', now() + INTERVAL '1 day' + INTERVAL '30 minutes', 'AVAILABLE', now())
ON CONFLICT (doctor_id, start_at, end_at) DO NOTHING;

INSERT INTO availability_slots (id, doctor_id, start_at, end_at, status, created_at)
VALUES
    ('00000000-0000-0000-0000-0000000000s4', '00000000-0000-0000-0000-0000000000f2',
     now() + INTERVAL '3 days', now() + INTERVAL '3 days' + INTERVAL '30 minutes', 'AVAILABLE', now())
ON CONFLICT (doctor_id, start_at, end_at) DO NOTHING;

INSERT INTO availability_slots (id, doctor_id, start_at, end_at, status, created_at)
VALUES
    ('00000000-0000-0000-0000-0000-0000000000s5', '00000000-0000-0000-0000-0000000000f3',
     now() + INTERVAL '2 days', now() + INTERVAL '2 days' + INTERVAL '30 minutes', 'AVAILABLE', now())
ON CONFLICT (doctor_id, start_at, end_at) DO NOTHING;

-- One patient user.
INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000p1', 'patient.arjun',
     '$2a$10$PLACEHOLDER', 'Arjun Sharma', 'arjun@amrutam.local',
     'PATIENT', FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_profiles (id, user_id, phone, preferred_language, preferred_timezone)
VALUES
    ('00000000-0000-0000-0000-0000000000p2', '00000000-0000-0000-0000-0000000000p1',
     '+91-1111000000', 'en', 'Asia/Calcutta')
ON CONFLICT (user_id) DO NOTHING;
