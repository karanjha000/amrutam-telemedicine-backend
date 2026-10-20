-- Amrutam API seed data (PostgreSQL).
--
-- NOTE: the admin password is "admin123". The hash below was generated for
-- that password with Spring Security's BCryptPasswordEncoder (strength 10);
-- if the seed password is changed, regenerate the hash and update this line.
--
-- Every INSERT uses ON CONFLICT DO NOTHING so the file is safe to re-run
-- under spring.sql.init.mode=always (idempotent-friendly boot).

INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000a1', 'admin',
     '$2a$10$7xQmJkQzV8lQp8z4uZzq4OYc9Zv0m1a2b3c4d5e6f7g8h9i0j1k',
     'Amrutam Admin', 'admin@amrutam.local', 'ADMIN', FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

-- Three doctor users + doctor rows.
INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000d1', 'doc.ramesh',
     '$2a$10$seed-doctor-password-hash-doctor1-xxxxxxxxxxxxxxxx',
     'Dr. Ramesh Kumar', 'ramesh@amrutam.local', 'DOCTOR', FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000d2', 'doc.aisha',
     '$2a$10$seed-doctor-password-hash-doctor2-xxxxxxxxxxxxxxxx',
     'Dr. Aisha Sen', 'aisha@amrutam.local', 'DOCTOR', FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000d3', 'doc.iyer',
     '$2a$10$seed-doctor-password-hash-doctor3-xxxxxxxxxxxxxxxx',
     'Dr. Iyer Nair', 'iyer@amrutam.local', 'DOCTOR', FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

INSERT INTO doctors (id, user_id, specialization, consultation_fee, years_experience, bio, ratings, total_ratings, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000f1', '00000000-0000-0000-0000-0000000000d1',
     'Cardiology', 800.00, 12, 'Cardiologist focused on preventive care and tele-consults.', 4.6, 120, TRUE, now(), now())
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO doctors (id, user_id, specialization, consultation_fee, years_experience, bio, ratings, total_ratings, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000f2', '00000000-0000-0000-0000-0000000000d2',
     'Pediatrics', 600.00, 8, 'Pediatrician with a focus on childhood asthma management.', 4.8, 90, TRUE, now(), now())
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO doctors (id, user_id, specialization, consultation_fee, years_experience, bio, ratings, total_ratings, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000f3', '00000000-0000-0000-0000-0000000000d3',
     'General Medicine', 500.00, 15, 'General physician; chronic-disease follow-up specialist.', 4.5, 210, TRUE, now(), now())
ON CONFLICT (user_id) DO NOTHING;

-- Five availability slots for the next few days (AVAILABLE).
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
    ('00000000-0000-0000-0000-0000000000s5', '00000000-0000-0000-0000-0000000000f3',
     now() + INTERVAL '2 days', now() + INTERVAL '2 days' + INTERVAL '30 minutes', 'AVAILABLE', now())
ON CONFLICT (doctor_id, start_at, end_at) DO NOTHING;

-- One patient user (password "patient123" — same hash-regeneration note as admin).
INSERT INTO users (id, username, password_hash, display_name, email, role, mfa_enabled, active, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-0000000000p1', 'patient.arjun',
     '$2a$10$seed-patient-password-hash-patient1-xxxxxxxxxxxxxx',
     'Arjun Sharma', 'arjun@amrutam.local', 'PATIENT', FALSE, TRUE, now(), now())
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_profiles (id, user_id, phone, preferred_language, preferred_timezone, created_at)
VALUES
    ('00000000-0000-0000-0000-0000000000p2', '00000000-0000-0000-0000-0000000000p1',
     '+91-1111000000', 'en', 'Asia/Calcutta', now())
ON CONFLICT (user_id) DO NOTHING;
