-- =====================================================================================================
-- Seed data: Module 03 — Student Information System (SIS) & Student Lifecycle (studentservice)
-- Target DB : jdbc:postgresql://localhost:5432/authservice (tables created by Hibernate from the entities;
--             start studentservice once before running this script)
--
-- Tenants, branches and users match the authservice seed (module01_auth_seed.sql):
--   a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11  Delhi Public Education Society  (DPS-DELHI-NORTH, DPS-DELHI-SOUTH)
--
-- Adds:
--   * the Module 03 roles (SCHOOL_NURSE, OFFICE_CLERK, CLASS_TEACHER, HOUSE_MASTER, VICE_PRINCIPAL,
--     ALUMNI_COORDINATOR) and one DPS North account per role; viceprincipal_kiran also gets VICE_PRINCIPAL
--     so transfer certificates can be signed off by two different people
--   * DPS academic structure: sessions 2025-2026 / 2026-2027 (active), classes Nursery … Class 12
--     (grade codes GRADE-NURSERY, GRADE-LKG, GRADE-UKG, GRADE-01 … GRADE-12, as admissionservice uses),
--     sections A–E of every class, houses Tagore / Raman / Shivaji / Ashoka;
--     terms 1–3 of 2026-2027 with half-terms (Term 1: 31 Aug – 18 Dec, half-term 26–30 Oct)
--
-- Story coverage (DPS North unless noted):
--   US-03000 54 student master records with permanent UIDs (52 North, 2 South; 40 generated ones fill the pages)
--   US-03001 blood groups for all but Myra (an incomplete record for "Record health"), medical alerts for 3 students
--   US-03002 Aarav (Class 6-A, enrolled through the module 02 pipeline) and his elder brother Ishaan (Class 9-A)
--            linked to parent_rohit's account
--   US-03003 every active student placed in a class, section and house with a roll number
--   US-03004 ID card barcode and QR data on every record
--   US-03006 one transferred student with a dual sign-off TC (principal_anita + viceprincipal_kiran)
--   US-03007 a bonafide and a character certificate
--   US-03008 two disciplinary incidents (minor, moderate) with counseling notes
--   US-03009 one graduated Class 12 student archived in the alumni registry
--
-- Login password for every seeded account: Password@123 (BCrypt, cost 10)
--
-- Re-runnable: every row is marked created_by = 'seed:module03' and every seed UUID starts with 5eed0003-
-- (users: 5eed0001-0001-4000-8000-0000000000 24-28). The script deletes only those rows first.
-- =====================================================================================================

BEGIN;

-- ---------------------------------------------------------------------------------------------------
-- 0. Clean previous seed rows (children first)
-- ---------------------------------------------------------------------------------------------------
-- Uploaded student photos survive a re-run: the links are remembered here and put back after the students are re-created
DROP TABLE IF EXISTS seed_photo_links;
CREATE TEMP TABLE seed_photo_links AS
SELECT student_id, profile_photo_url FROM student_master
 WHERE student_id::text LIKE '5eed0003-%' AND profile_photo_url IS NOT NULL;

DELETE FROM alumni_records          WHERE alumni_id::text      LIKE '5eed0003-%';
DELETE FROM disciplinary_incidents  WHERE incident_id::text    LIKE '5eed0003-%';
DELETE FROM certificates_issued     WHERE certificate_id::text LIKE '5eed0003-%';
DELETE FROM student_guardians       WHERE guardian_id::text    LIKE '5eed0003-%';
DELETE FROM student_master          WHERE student_id::text     LIKE '5eed0003-%';
DELETE FROM sections                WHERE section_id::text     LIKE '5eed0003-%';
DELETE FROM grades                  WHERE grade_id::text       LIKE '5eed0003-%';
DELETE FROM houses                  WHERE house_id::text       LIKE '5eed0003-%';
DELETE FROM academic_terms          WHERE term_id::text        LIKE '5eed0003-%';
DELETE FROM academic_sessions       WHERE academic_year_id::text LIKE '5eed0003-%';
DELETE FROM user_roles              WHERE created_by = 'seed:module03';
DELETE FROM user_credentials        WHERE created_by = 'seed:module03';
DELETE FROM users                   WHERE created_by = 'seed:module03';
DELETE FROM roles                   WHERE created_by = 'seed:module03';

-- ---------------------------------------------------------------------------------------------------
-- 1. Module 03 roles and staff accounts
-- ---------------------------------------------------------------------------------------------------
INSERT INTO roles (name, description, active, created_at, updated_at, created_by, updated_by, version)
SELECT r.name, r.description, true, TIMESTAMPTZ '2026-07-01 04:30:00+00', TIMESTAMPTZ '2026-07-01 04:30:00+00', 'seed:module03', NULL, 0
FROM (VALUES
    ('SCHOOL_NURSE',       'Maintains blood groups, medical alerts and emergency contacts'),
    ('OFFICE_CLERK',       'Enrols students, links sibling accounts and prints ID cards'),
    ('CLASS_TEACHER',      'Allocates sections and records disciplinary incidents for the class'),
    ('HOUSE_MASTER',       'Allocates students to school houses'),
    ('VICE_PRINCIPAL',     'Second sign-off on transfer certificates; reviews disciplinary incidents'),
    ('ALUMNI_COORDINATOR', 'Archives graduating students in the permanent alumni registry')
) AS r(name, description)
WHERE NOT EXISTS (SELECT 1 FROM roles x WHERE x.name = r.name);

CREATE TEMP TABLE seed_staff (user_id text, username text, first_name text, last_name text, email text, phone text, user_type text,
                              created_at timestamptz) ON COMMIT DROP;
INSERT INTO seed_staff VALUES
('5eed0001-0001-4000-8000-000000000024', 'nurse_lata',        'Lata',    'Menon',     'lata.menon@dps.edu.in',      '+919811100124', 'SCHOOL_NURSE',       '2026-07-01 05:40:00+00'),
('5eed0001-0001-4000-8000-000000000025', 'officeclerk_anil',  'Anil',    'Saxena',    'anil.saxena@dps.edu.in',     '+919811100125', 'OFFICE_CLERK',       '2026-07-01 05:45:00+00'),
('5eed0001-0001-4000-8000-000000000026', 'classteacher_priya','Priya',   'Nair',      'priya.nair@dps.edu.in',      '+919811100126', 'CLASS_TEACHER',      '2026-07-01 05:50:00+00'),
('5eed0001-0001-4000-8000-000000000027', 'housemaster_vivek', 'Vivek',   'Chopra',    'vivek.chopra@dps.edu.in',    '+919811100127', 'HOUSE_MASTER',       '2026-07-01 05:55:00+00'),
('5eed0001-0001-4000-8000-000000000028', 'alumni_rekha',      'Rekha',   'Ghosh',     'rekha.ghosh@dps.edu.in',     '+919811100128', 'ALUMNI_COORDINATOR', '2026-07-01 06:00:00+00');

INSERT INTO users (user_id, tenant_id, branch_id, username, first_name, last_name, email, phone_number, user_type, is_active, is_mfa_enabled,
                   created_at, updated_at, created_by, updated_by, version)
SELECT user_id, 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', username, first_name, last_name, email, phone, user_type, true, false,
       created_at, created_at, 'seed:module03', NULL, 0
FROM seed_staff;

INSERT INTO user_credentials (user_id, password_hash, password_salt, failed_login_attempts, locked_until, must_change_password,
                              password_changed_at, created_at, updated_at, created_by, updated_by, version)
SELECT user_id, '$2a$10$8liDxOW98AN4N7eJQbqKd.LMq6EZ7rg6Q11jCO3sxeqrp39CXcXTq',                 -- Password@123
       'salt_' || substr(md5(user_id), 1, 8), 0, NULL, false, created_at + interval '10 minutes',
       created_at, created_at, 'seed:module03', NULL, 0
FROM seed_staff;

INSERT INTO user_roles (user_id, role_code, created_at, updated_at, created_by, updated_by, version)
SELECT user_id, user_type, created_at, created_at, 'seed:module03', NULL, 0 FROM seed_staff
UNION ALL
-- the class teacher of 6-A also enrols walk-in students as office clerk cover
SELECT '5eed0001-0001-4000-8000-000000000026', 'OFFICE_CLERK', TIMESTAMPTZ '2026-07-01 05:50:00+00', TIMESTAMPTZ '2026-07-01 05:50:00+00', 'seed:module03', NULL, 0
UNION ALL
-- viceprincipal_kiran (module 01 seed): second sign-off on transfer certificates
SELECT '5eed0001-0001-4000-8000-000000000023', 'VICE_PRINCIPAL', TIMESTAMPTZ '2026-07-01 06:05:00+00', TIMESTAMPTZ '2026-07-01 06:05:00+00', 'seed:module03', NULL, 0;

-- ---------------------------------------------------------------------------------------------------
-- 2. Academic structure (DPS)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO academic_sessions (academic_year_id, tenant_id, session_name, start_date, end_date, is_active, week_structure, created_at, updated_at, created_by, updated_by, version)
VALUES
('5eed0003-0001-4000-8000-000000000001', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '2025-2026', DATE '2025-04-01', DATE '2026-03-31', false, 'MON_FRI',
 '2025-02-01 05:00:00+00', '2026-04-01 05:00:00+00', 'seed:module03', NULL, 0),
('5eed0003-0001-4000-8000-000000000002', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '2026-2027', DATE '2026-04-01', DATE '2027-03-31', true, 'MON_FRI',
 '2026-02-01 05:00:00+00', '2026-04-01 05:00:00+00', 'seed:module03', NULL, 0);

-- Terms of 2026-2027 (Settings → Academic setup, Calendar → Term dates); Term 1 is current
INSERT INTO academic_terms (term_id, tenant_id, academic_year_id, term_name, start_date, end_date, half_term_start, half_term_end,
                            is_current, sort_order, created_at, updated_at, created_by, updated_by, version)
VALUES
('5eed0003-0011-4000-8000-000000000001', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0003-0001-4000-8000-000000000002', 'Term 1',
 DATE '2026-08-31', DATE '2026-12-18', DATE '2026-10-26', DATE '2026-10-30', true, 1, '2026-04-01 05:00:00+00', NULL, 'seed:module03', NULL, 0),
('5eed0003-0011-4000-8000-000000000002', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0003-0001-4000-8000-000000000002', 'Term 2',
 DATE '2027-01-05', DATE '2027-03-26', DATE '2027-02-15', DATE '2027-02-19', false, 2, '2026-04-01 05:00:00+00', NULL, 'seed:module03', NULL, 0),
('5eed0003-0011-4000-8000-000000000003', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0003-0001-4000-8000-000000000002', 'Term 3',
 DATE '2027-04-12', DATE '2027-07-23', DATE '2027-05-31', DATE '2027-06-04', false, 3, '2026-04-01 05:00:00+00', NULL, 'seed:module03', NULL, 0);

-- Grade ids: 5eed0003-0002-4000-8000-0000000000NN, NN = sequence order (01 Nursery … 15 Class 12)
INSERT INTO grades (grade_id, tenant_id, grade_name, grade_code, sequence_order, created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0003-0002-4000-8000-0000000000' || lpad(g.seq::text, 2, '0'))::uuid, 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', g.name, g.code, g.seq,
       TIMESTAMPTZ '2026-02-01 05:00:00+00', TIMESTAMPTZ '2026-02-01 05:00:00+00', 'seed:module03', NULL, 0
FROM (SELECT 1 AS seq, 'Nursery' AS name, 'GRADE-NURSERY' AS code UNION ALL SELECT 2, 'LKG', 'GRADE-LKG' UNION ALL SELECT 3, 'UKG', 'GRADE-UKG'
      UNION ALL SELECT n + 3, 'Class ' || n, 'GRADE-' || lpad(n::text, 2, '0') FROM generate_series(1, 12) AS n) AS g;

-- Section ids: 5eed0003-0003-4000-8000-0000000000GS, G = grade sequence (01-15), S = 1-5 for A-E (e.g. Class 6-A = …000000000091)
INSERT INTO sections (section_id, grade_id, section_name, max_capacity, created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0003-0003-4000-8000-000000000' || lpad(g.seq::text, 2, '0') || s.idx)::uuid,
       ('5eed0003-0002-4000-8000-0000000000' || lpad(g.seq::text, 2, '0'))::uuid, s.name, CASE WHEN g.seq <= 3 THEN 30 ELSE 40 END,
       TIMESTAMPTZ '2026-02-01 05:00:00+00', TIMESTAMPTZ '2026-02-01 05:00:00+00', 'seed:module03', NULL, 0
FROM generate_series(1, 15) AS g(seq)
CROSS JOIN (VALUES (1, 'A'), (2, 'B'), (3, 'C'), (4, 'D'), (5, 'E')) AS s(idx, name);

INSERT INTO houses (house_id, tenant_id, house_name, color_hex, created_at, updated_at, created_by, updated_by, version)
VALUES
('5eed0003-0004-4000-8000-000000000001', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Tagore',  '#1E88E5', '2026-02-01 05:00:00+00', '2026-02-01 05:00:00+00', 'seed:module03', NULL, 0),
('5eed0003-0004-4000-8000-000000000002', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Raman',   '#E53935', '2026-02-01 05:00:00+00', '2026-02-01 05:00:00+00', 'seed:module03', NULL, 0),
('5eed0003-0004-4000-8000-000000000003', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Shivaji', '#43A047', '2026-02-01 05:00:00+00', '2026-02-01 05:00:00+00', 'seed:module03', NULL, 0),
('5eed0003-0004-4000-8000-000000000004', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Ashoka',  '#FDD835', '2026-02-01 05:00:00+00', '2026-02-01 05:00:00+00', 'seed:module03', NULL, 0);

-- ---------------------------------------------------------------------------------------------------
-- 3. US-03000 – US-03004 Students (grade = sequence order, section 1-5 = A-E, house 1-4)
-- ---------------------------------------------------------------------------------------------------
CREATE TEMP TABLE seed_students (n int, branch text, uid text, first_name text, last_name text, dob date, gender text, blood text,
                                 alerts text, emergency text, grade int, section int, house int, roll int, status text, parent text,
                                 guardian text, relationship text, g_phone text, g_email text, created_at timestamptz) ON COMMIT DROP;
INSERT INTO seed_students VALUES
-- DPS North, Class 6-A (Aarav joined through the module 02 admissions pipeline)
( 1, 'DPS-DELHI-NORTH', 'STU-2026-00101', 'Aarav',   'Sharma',    DATE '2015-03-14', 'MALE',   'B+',  NULL,                                         '+919810012301',  9, 1, 1, 1,  'ACTIVE',      '5eed0001-0001-4000-8000-000000000014', 'Rohit Sharma',   'Father',   '+919810012301', 'rohit.sharma@gmail.com',     '2026-10-05 07:00:00+00'),
( 2, 'DPS-DELHI-NORTH', 'STU-2022-00045', 'Saanvi',  'Gupta',     DATE '2014-11-02', 'FEMALE', 'O+',  'Peanut allergy: EpiPen kept at the medical room', '+919811234502', 9, 1, 2, 2,  'ACTIVE',      NULL, 'Neha Gupta',     'Mother',   '+919811234502', 'neha.gupta@example.in',      '2022-04-04 05:00:00+00'),
( 3, 'DPS-DELHI-NORTH', 'STU-2021-00032', 'Vihaan',  'Reddy',     DATE '2014-07-21', 'MALE',   'A+',  NULL,                                         '+919811234503',  9, 1, 3, 3,  'ACTIVE',      NULL, 'Srinivas Reddy', 'Father',   '+919811234503', 'srinivas.reddy@example.in',  '2021-04-05 05:00:00+00'),
( 4, 'DPS-DELHI-NORTH', 'STU-2021-00038', 'Myra',    'Kapoor',    DATE '2014-09-30', 'FEMALE', NULL,  NULL,                                         '+919811234504',  9, 2, 4, 1,  'ACTIVE',      NULL, 'Ritu Kapoor',    'Mother',   '+919811234504', 'ritu.kapoor@example.in',     '2021-04-05 05:10:00+00'),
-- Class 7 and 9 (Ishaan is Aarav's elder brother)
( 5, 'DPS-DELHI-NORTH', 'STU-2020-00027', 'Kabir',   'Singh',     DATE '2013-05-18', 'MALE',   'O-',  'Mild asthma: inhaler in school bag',         '+919811234505', 10, 1, 1, 4,  'ACTIVE',      NULL, 'Harpreet Singh', 'Father',   '+919811234505', 'harpreet.singh@example.in',  '2020-04-06 05:00:00+00'),
( 6, 'DPS-DELHI-NORTH', 'STU-2018-00012', 'Ishaan',  'Sharma',    DATE '2011-12-09', 'MALE',   'B+',  NULL,                                         '+919810012301', 12, 1, 1, 7,  'ACTIVE',      '5eed0001-0001-4000-8000-000000000014', 'Rohit Sharma',   'Father',   '+919810012301', 'rohit.sharma@gmail.com',     '2018-04-02 05:00:00+00'),
( 7, 'DPS-DELHI-NORTH', 'STU-2018-00019', 'Anika',   'Joshi',     DATE '2012-02-25', 'FEMALE', 'A-',  NULL,                                         '+919811234507', 12, 2, 3, 2,  'ACTIVE',      NULL, 'Meenal Joshi',   'Mother',   '+919811234507', 'meenal.joshi@example.in',    '2018-04-02 05:20:00+00'),
-- Class 10 and 12
( 8, 'DPS-DELHI-NORTH', 'STU-2016-00008', 'Arjun',   'Mehta',     DATE '2010-08-11', 'MALE',   'B-',  NULL,                                         '+919811234508', 13, 1, 2, 5,  'ACTIVE',      NULL, 'Sanjay Mehta',   'Father',   '+919811234508', 'sanjay.mehta@example.in',    '2016-04-04 05:00:00+00'),
( 9, 'DPS-DELHI-NORTH', 'STU-2016-00011', 'Zara',    'Khan',      DATE '2010-04-03', 'FEMALE', 'O+',  'Type 1 diabetes: insulin schedule with the nurse', '+919811234509', 13, 1, 4, 6, 'ACTIVE',      NULL, 'Farida Khan',    'Mother',   '+919811234509', 'farida.khan@example.in',     '2016-04-04 05:10:00+00'),
(10, 'DPS-DELHI-NORTH', 'STU-2014-00004', 'Rohan',   'Verma',     DATE '2008-06-27', 'MALE',   'A+',  NULL,                                         '+919811234510', 15, 1, 3, 3,  'ACTIVE',      NULL, 'Alok Verma',     'Father',   '+919811234510', 'alok.verma@example.in',      '2014-04-01 05:00:00+00'),
-- left the school: TC issued (US-03006), and a 2025-2026 Class 12 graduate (US-03009)
(11, 'DPS-DELHI-NORTH', 'STU-2019-00021', 'Tanvi',   'Agarwal',   DATE '2012-10-14', 'FEMALE', 'B+',  NULL,                                         '+919811234511', 12, 2, 2, 9,  'TRANSFERRED', NULL, 'Pankaj Agarwal', 'Father',   '+919811234511', 'pankaj.agarwal@example.in',  '2019-04-03 05:00:00+00'),
(12, 'DPS-DELHI-NORTH', 'STU-2013-00002', 'Nikhil',  'Bose',      DATE '2007-01-19', 'MALE',   'O+',  NULL,                                         '+919811234512', 15, 2, 1, 11, 'GRADUATED',   NULL, 'Sumita Bose',    'Mother',   '+919811234512', 'sumita.bose@example.in',     '2013-04-01 05:00:00+00'),
-- DPS South (not listed for DPS North staff)
(13, 'DPS-DELHI-SOUTH', 'STU-2024-00061', 'Ira',     'Malhotra',  DATE '2019-02-08', 'FEMALE', 'A+',  NULL,                                         '+919811234513',  4, 1, 2, 1,  'ACTIVE',      NULL, 'Kunal Malhotra', 'Father',   '+919811234513', 'kunal.malhotra@example.in',  '2024-04-01 05:00:00+00'),
(14, 'DPS-DELHI-SOUTH', 'STU-2023-00054', 'Dev',     'Arora',     DATE '2016-09-15', 'MALE',   'B+',  NULL,                                         '+919811234514',  7, 1, 3, 2,  'ACTIVE',      NULL, 'Pooja Arora',    'Mother',   '+919811234514', 'pooja.arora@example.in',     '2023-04-03 05:00:00+00');

-- 40 more DPS North students (n = 15 … 54) so the directory spans several pages: Class 1 … Class 12, sections A–C,
-- houses in rotation, a mix of names; every one complete (guardian, emergency phone, blood group)
INSERT INTO seed_students
SELECT n, 'DPS-DELHI-NORTH', 'STU-' || (2015 + n % 11) || '-' || lpad((200 + n)::text, 5, '0'),
       (ARRAY['Aditya','Ananya','Arnav','Avni','Dhruv','Diya','Ishita','Kiara','Krish','Meera','Nikita','Om','Pari','Reyansh','Riya','Sai',
              'Shaurya','Siya','Tara','Yash'])[1 + (n * 7) % 20],
       (ARRAY['Bansal','Chopra','Desai','Iyer','Jain','Kulkarni','Menon','Nair','Pandey','Rao','Saxena','Sinha','Tiwari','Varma'])[1 + (n * 5) % 14],
       DATE '2009-06-01' + ((54 - n) * 73) % 3650, CASE WHEN n % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
       (ARRAY['A+','A-','B+','B-','AB+','O+','O-','B+'])[1 + n % 8],
       CASE WHEN n % 9 = 0 THEN 'Dust allergy: avoid the science store room' END,
       '+9198112' || lpad((34500 + n)::text, 5, '0'),
       4 + n % 12,                       -- grade sequence 4-15 = Class 1 … Class 12
       1 + n % 3,                        -- sections A–C
       1 + n % 4,                        -- Tagore, Raman, Shivaji, Ashoka
       10 + n / 12,                      -- roll numbers after the named students
       'ACTIVE', NULL,
       'Guardian of ' || (ARRAY['Aditya','Ananya','Arnav','Avni','Dhruv','Diya','Ishita','Kiara','Krish','Meera','Nikita','Om','Pari','Reyansh','Riya','Sai',
              'Shaurya','Siya','Tara','Yash'])[1 + (n * 7) % 20],
       CASE WHEN n % 3 = 0 THEN 'Father' ELSE 'Mother' END,
       '+9198112' || lpad((34500 + n)::text, 5, '0'), 'guardian' || n || '@example.in',
       TIMESTAMPTZ '2019-04-01 05:00:00+00' + (n * interval '41 days')
FROM generate_series(15, 54) AS n;

INSERT INTO student_master (student_id, tenant_id, branch_id, student_uid, first_name, last_name, date_of_birth, gender, blood_group,
                            medical_alerts, emergency_contact_phone, grade_id, section_id, house_id, roll_number, student_status,
                            parent_account_id, id_card_barcode, id_card_qr_code, profile_photo_url,
                            created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0003-0005-4000-8000-0000000000' || lpad(s.n::text, 2, '0'))::uuid, 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', s.branch, s.uid,
       s.first_name, s.last_name, s.dob, s.gender, s.blood, s.alerts, s.emergency,
       ('5eed0003-0002-4000-8000-0000000000' || lpad(s.grade::text, 2, '0'))::uuid,
       ('5eed0003-0003-4000-8000-000000000' || lpad(s.grade::text, 2, '0') || s.section)::uuid,
       ('5eed0003-0004-4000-8000-00000000000' || s.house)::uuid,
       s.roll, s.status, s.parent, 'BC-' || s.uid, 'https://verify.kvs.local/students/' || s.uid, NULL,
       s.created_at, GREATEST(s.created_at, TIMESTAMPTZ '2026-04-01 05:00:00+00'), 'seed:module03', NULL, 0
FROM seed_students s;

-- Personal, admission and allergy fields (Module 03 profile upgrade)
-- Personal, admission, contact and allergy fields (columns from db/upgrade/module03_student_profile_allergies.sql).
-- Allergy text moves out of the medical alerts into its own column, as the upgrade does for existing rows.
UPDATE student_master
   SET allergies = medical_alerts, medical_alerts = 'None'
 WHERE tenant_id = 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11'
   AND student_id::text LIKE '5eed0003-%' AND medical_alerts ILIKE '%allerg%';

UPDATE student_master
   SET nationality = 'Indian',
       admission_date = created_at::date,
       admission_number = 'ADM-' || student_uid,
       collection_permission = 'Both guardians authorised'
 WHERE student_id::text LIKE '5eed0003-%';

UPDATE student_master
   SET medication_instructions = 'Salbutamol inhaler held at the school office',
       gp_contact = 'Willow Medical Centre, +91 11 4000 1000'
 WHERE student_id::text LIKE '5eed0003-%' AND medical_alerts ILIKE '%asthma%';

-- Put the remembered photo links back (the files live in studentservice/student-vault)
UPDATE student_master m
   SET profile_photo_url = l.profile_photo_url
  FROM seed_photo_links l
 WHERE m.student_id = l.student_id;
DROP TABLE IF EXISTS seed_photo_links;

-- One primary guardian per student; Kabir and Zara also have a second (emergency) contact
INSERT INTO student_guardians (guardian_id, student_id, full_name, relationship, mobile_number, email, is_primary_contact,
                               created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0003-0006-4000-8000-0000000000' || lpad(s.n::text, 2, '0'))::uuid, ('5eed0003-0005-4000-8000-0000000000' || lpad(s.n::text, 2, '0'))::uuid,
       s.guardian, s.relationship, s.g_phone, s.g_email, true, s.created_at, s.created_at, 'seed:module03', NULL, 0
FROM seed_students s
UNION ALL
SELECT '5eed0003-0006-4000-8000-000000000105', '5eed0003-0005-4000-8000-000000000005', 'Gurleen Singh', 'Mother', '+919811234605', 'gurleen.singh@example.in', false,
       TIMESTAMPTZ '2020-04-06 05:00:00+00', TIMESTAMPTZ '2020-04-06 05:00:00+00', 'seed:module03', NULL, 0
UNION ALL
SELECT '5eed0003-0006-4000-8000-000000000109', '5eed0003-0005-4000-8000-000000000009', 'Imran Khan', 'Father', '+919811234609', NULL, false,
       TIMESTAMPTZ '2016-04-04 05:10:00+00', TIMESTAMPTZ '2016-04-04 05:10:00+00', 'seed:module03', NULL, 0;

-- ---------------------------------------------------------------------------------------------------
-- 4. US-03006 / US-03007 Certificates
-- ---------------------------------------------------------------------------------------------------
INSERT INTO certificates_issued (certificate_id, student_id, certificate_type, certificate_number, issue_date, authorized_by_user_id,
                                 second_sign_off_user_id, qr_verification_token, pdf_vault_path, remarks,
                                 created_at, updated_at, created_by, updated_by, version)
VALUES
('5eed0003-0007-4000-8000-000000000001', '5eed0003-0005-4000-8000-000000000011', 'TRANSFER_CERTIFICATE', 'TC-2026-00011', DATE '2026-06-12',
 'principal_anita', 'viceprincipal_kiran', 'TC-5eed0003-7a11-4c0e-9f00-000000000011', '/vault/certificates/TC-2026-00011.pdf',
 'Reason: Family relocating to Bengaluru | Conduct: Good', '2026-06-12 06:00:00+00', '2026-06-12 06:00:00+00', 'seed:module03', NULL, 0),
('5eed0003-0007-4000-8000-000000000002', '5eed0003-0005-4000-8000-000000000008', 'BONAFIDE', 'BON-2026-00008', DATE '2026-08-20',
 'registrar_suresh', NULL, 'BON-5eed0003-7a11-4c0e-9f00-000000000008', '/vault/certificates/BON-2026-00008.pdf',
 'Passport application', '2026-08-20 05:30:00+00', '2026-08-20 05:30:00+00', 'seed:module03', NULL, 0),
('5eed0003-0007-4000-8000-000000000003', '5eed0003-0005-4000-8000-000000000010', 'CHARACTER', 'CHAR-2026-00010', DATE '2026-09-15',
 'registrar_suresh', NULL, 'CHAR-5eed0003-7a11-4c0e-9f00-000000000010', '/vault/certificates/CHAR-2026-00010.pdf',
 'National Science Olympiad camp', '2026-09-15 05:30:00+00', '2026-09-15 05:30:00+00', 'seed:module03', NULL, 0);

-- ---------------------------------------------------------------------------------------------------
-- 5. US-03008 Disciplinary incidents
-- ---------------------------------------------------------------------------------------------------
INSERT INTO disciplinary_incidents (incident_id, student_id, incident_date, severity, incident_category, description, action_taken,
                                    counseling_notes, reported_by_user_id, created_at, updated_at, created_by, updated_by, version)
VALUES
('5eed0003-0008-4000-8000-000000000001', '5eed0003-0005-4000-8000-000000000003', DATE '2026-09-08', 'MINOR', 'Late submission',
 'Science project submitted four days after the deadline without a note from home.', 'Verbal warning; new deadline agreed',
 'Student says he struggled with the experiment; extra lab time offered on Thursdays.', 'classteacher_priya',
 '2026-09-08 09:30:00+00', '2026-09-08 09:30:00+00', 'seed:module03', NULL, 0),
('5eed0003-0008-4000-8000-000000000002', '5eed0003-0005-4000-8000-000000000008', DATE '2026-10-06', 'MODERATE', 'Classroom disruption',
 'Repeated use of a mobile phone during the mathematics periodic test.', 'Phone held at the office for a week; parents informed',
 'Met with the counselor on 08 Oct; agreed to hand in the phone at the start of each test.', 'viceprincipal_kiran',
 '2026-10-06 08:15:00+00', '2026-10-08 10:00:00+00', 'seed:module03', NULL, 0);

-- ---------------------------------------------------------------------------------------------------
-- 6. US-03009 Alumni registry
-- ---------------------------------------------------------------------------------------------------
INSERT INTO alumni_records (alumni_id, student_id, tenant_id, branch_id, student_uid, full_name, graduation_year, final_grade_passed,
                            alumni_email, alumni_phone, higher_education_institution, current_occupation, archived_date,
                            created_at, updated_at, created_by, updated_by, version)
VALUES
('5eed0003-0009-4000-8000-000000000001', '5eed0003-0005-4000-8000-000000000012', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH',
 'STU-2013-00002', 'Nikhil Bose', 2026, 'Class 12', 'nikhil.bose@alumni.dps.edu.in', '+919811234612', 'IIT Delhi', 'B.Tech student',
 DATE '2026-06-30', '2026-06-30 06:00:00+00', '2026-06-30 06:00:00+00', 'seed:module03', NULL, 0);

COMMIT;
