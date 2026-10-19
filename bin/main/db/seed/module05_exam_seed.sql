-- =====================================================================================================
-- Module 05 (examservice) seed: Delhi Public School 2026-2027
-- Kept in studentservice next to module03_student_seed.sql, whose students, classes and sections it grades.
-- =====================================================================================================
-- Run after the Module 01 and Module 03 seeds, and after examservice has started once
-- (Hibernate ddl-auto creates exam_terms, marks_entry_register, … ):
--     powershell -ExecutionPolicy Bypass -File .agents/skills/test-ui/scripts/db/db.ps1 -Script <this file>
--
-- Adds:
--   * roles EXAM_CONTROLLER, SUBJECT_TEACHER, ACADEMIC_AUDITOR and one DPS North account each
--     (examcontroller_rajiv, subjectteacher_neha, academicauditor_vikram; users 5eed0001-0001-4000-8000-000000000033 … 35).
--     Class Teacher = classteacher_priya, Academic Principal = SCHOOL_PRINCIPAL (principal_anita, viceprincipal_kiran),
--     Parent = parent_rohit (Aarav, Ishaan) already exist.
--
-- Story coverage (academic year 2026-2027, every active DPS student in Class 1 … Class 12):
--   US-05000 grading scale "CBSE 9-point" (A1 91+ … D 33+, E below; pass 33%); terms Periodic Test 1 (formative, 20%),
--            Half-Yearly (summative, 30%), Periodic Test 2 (formative, 20%, open) and Annual (summative, 30%);
--            subjects per class: Class 1-5 ENG HIN MATH EVS, Class 6-10 ENG HIN MATH SCI SST (SCI weight 1.5),
--            Class 11-12 ENG PHY CHEM MATH CS; periodic tests out of 40 (pass 13), the others out of 80 (pass 27)
--   US-05003 marks for Periodic Test 1 and Half-Yearly for every student
--   US-05005 Periodic Test 1 locked by the exam controller; Half-Yearly closed by its deadline (10 Oct 2026, T_lock)
--   US-05006 a pending ticket: subjectteacher_neha asks to reopen Half-Yearly Science for Class 7-A
--   US-05016 Riya Bansal (7-A) has no Half-Yearly Science marks → 7-A is incomplete
--   US-05007/05011 broadsheets and rank lists from those marks; Om Varma (6-C) fails Half-Yearly Mathematics (18/80);
--            Diya Nair (12-C) was absent for Half-Yearly English
--   US-05008 co-scholastic grades for Half-Yearly in Class 6-A and 7-A
--   US-05010 Periodic Test 1 published for every section; Half-Yearly published for Class 6-A only (parent_rohit sees Aarav's)
--   US-05018 class teacher remarks on the Class 6-A Half-Yearly cards
--   US-05013 every student has two terms, so the profile shows a trend
--
-- Re-runnable: every row is marked created_by = 'seed:module05' and every seed UUID starts with 5eed0005-.
-- The script deletes those rows first, plus rows later added through the API to the seeded terms.
-- =====================================================================================================

BEGIN;

-- ---------------------------------------------------------------------------------------------------
-- 0. Clean previous seed rows (children first)
-- ---------------------------------------------------------------------------------------------------
DELETE FROM holistic_progress_cards WHERE exam_term_id::text LIKE '5eed0005-%';
DELETE FROM co_scholastic_grades    WHERE exam_term_id::text LIKE '5eed0005-%';
DELETE FROM grade_lock_tickets      WHERE exam_term_id::text LIKE '5eed0005-%';
DELETE FROM marks_entry_register    WHERE exam_term_id::text LIKE '5eed0005-%';
DELETE FROM exam_term_subjects      WHERE exam_term_id::text LIKE '5eed0005-%';
DELETE FROM exam_terms              WHERE exam_term_id::text LIKE '5eed0005-%';
DELETE FROM grading_scale_bands     WHERE grading_scale_id::text LIKE '5eed0005-%';
DELETE FROM grading_scales          WHERE grading_scale_id::text LIKE '5eed0005-%';
DELETE FROM user_roles              WHERE created_by = 'seed:module05';
DELETE FROM user_credentials        WHERE created_by = 'seed:module05';
DELETE FROM users                   WHERE created_by = 'seed:module05';
DELETE FROM roles                   WHERE created_by = 'seed:module05';

-- ---------------------------------------------------------------------------------------------------
-- 1. Module 05 roles and staff accounts
-- ---------------------------------------------------------------------------------------------------
INSERT INTO roles (name, description, active, created_at, updated_at, created_by, updated_by, version)
SELECT r.name, r.description, true, TIMESTAMPTZ '2026-03-10 04:30:00+00', TIMESTAMPTZ '2026-03-10 04:30:00+00', 'seed:module05', NULL, 0
FROM (VALUES
    ('EXAM_CONTROLLER',  'Configures terms, subjects and grading scales, locks mark entry, publishes results'),
    ('SUBJECT_TEACHER',  'Enters marks and co-scholastic grades, raises unlock tickets'),
    ('ACADEMIC_AUDITOR', 'Reviews broadsheets, rank lists and incomplete entries')
) AS r(name, description)
WHERE NOT EXISTS (SELECT 1 FROM roles x WHERE x.name = r.name);

CREATE TEMP TABLE seed_staff (user_id text, username text, first_name text, last_name text, email text, phone text, user_type text,
                              created_at timestamptz) ON COMMIT DROP;
INSERT INTO seed_staff VALUES
('5eed0001-0001-4000-8000-000000000033', 'examcontroller_rajiv',   'Rajiv',  'Bhalla', 'rajiv.bhalla@dps.edu.in',  '+919811100133', 'EXAM_CONTROLLER',  '2026-03-10 05:00:00+00'),
('5eed0001-0001-4000-8000-000000000034', 'subjectteacher_neha',    'Neha',   'Kapoor', 'neha.kapoor@dps.edu.in',   '+919811100134', 'SUBJECT_TEACHER',  '2026-03-10 05:05:00+00'),
('5eed0001-0001-4000-8000-000000000035', 'academicauditor_vikram', 'Vikram', 'Sethi',  'vikram.sethi@dps.edu.in',  '+919811100135', 'ACADEMIC_AUDITOR', '2026-03-10 05:10:00+00');

INSERT INTO users (user_id, tenant_id, branch_id, username, first_name, last_name, email, phone_number, user_type, is_active, is_mfa_enabled,
                   created_at, updated_at, created_by, updated_by, version)
SELECT user_id, 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DPS-DELHI-NORTH', username, first_name, last_name, email, phone, user_type, true, false,
       created_at, created_at, 'seed:module05', NULL, 0
FROM seed_staff;

INSERT INTO user_credentials (user_id, password_hash, password_salt, failed_login_attempts, locked_until, must_change_password,
                              password_changed_at, created_at, updated_at, created_by, updated_by, version)
SELECT user_id, '$2a$10$8liDxOW98AN4N7eJQbqKd.LMq6EZ7rg6Q11jCO3sxeqrp39CXcXTq',                 -- Password@123
       'salt_' || substr(md5(user_id), 1, 8), 0, NULL, false, created_at + interval '10 minutes',
       created_at, created_at, 'seed:module05', NULL, 0
FROM seed_staff;

INSERT INTO user_roles (user_id, role_code, created_at, updated_at, created_by, updated_by, version)
SELECT user_id, user_type, created_at, created_at, 'seed:module05', NULL, 0 FROM seed_staff;

-- ---------------------------------------------------------------------------------------------------
-- 2. Grading scale and terms (US-05000, US-05005)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO grading_scales (grading_scale_id, tenant_id, scale_name, pass_percent, created_at, updated_at, created_by, updated_by, version)
VALUES ('5eed0005-0001-4000-8000-000000000001', 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'CBSE 9-point', 33.00,
        TIMESTAMPTZ '2026-03-12 05:00:00+00', TIMESTAMPTZ '2026-03-12 05:00:00+00', 'seed:module05', NULL, 0);

INSERT INTO grading_scale_bands (grading_scale_id, grade_label, min_percent, grade_point, description)
SELECT '5eed0005-0001-4000-8000-000000000001', b.label, b.min, b.point, b.descr
FROM (VALUES ('A1', 91, 10.0, 'Outstanding'), ('A2', 81, 9.0, 'Excellent'), ('B1', 71, 8.0, 'Very good'), ('B2', 61, 7.0, 'Good'),
             ('C1', 51, 6.0, 'Above average'), ('C2', 41, 5.0, 'Average'), ('D', 33, 4.0, 'Pass'), ('E', 0, 0.0, 'Needs improvement'))
     AS b(label, min, point, descr);

INSERT INTO exam_terms (exam_term_id, tenant_id, academic_year_id, term_name, term_type, weightage_percent, start_date, end_date,
                        mark_entry_deadline, locked_at, grading_scale_id, created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0005-0002-4000-8000-00000000000' || t.n)::uuid, 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11', '5eed0003-0001-4000-8000-000000000002',
       t.name, t.type, t.weight, t.start::date, t.finish::date, t.deadline::timestamptz, t.locked::timestamptz,
       '5eed0005-0001-4000-8000-000000000001', TIMESTAMPTZ '2026-03-15 05:00:00+00', TIMESTAMPTZ '2026-03-15 05:00:00+00', 'seed:module05', NULL, 0
FROM (VALUES
    ('1', 'Periodic Test 1', 'FORMATIVE', 20, '2026-05-04', '2026-05-09', '2026-05-20 12:30:00+00', '2026-05-18 10:00:00+00'),
    ('2', 'Half-Yearly',     'SUMMATIVE', 30, '2026-09-14', '2026-09-26', '2026-10-10 12:30:00+00', NULL),
    ('3', 'Periodic Test 2', 'FORMATIVE', 20, '2026-11-16', '2026-11-21', '2026-12-05 12:30:00+00', NULL),
    ('4', 'Annual',          'SUMMATIVE', 30, '2027-02-22', '2027-03-10', '2027-03-20 12:30:00+00', NULL)
) AS t(n, name, type, weight, start, finish, deadline, locked);

-- Subjects: ids 5eed0005-0003-4000-8000-0000000TGGSS (T term, GG grade sequence, SS subject order)
CREATE TEMP TABLE seed_subject ON COMMIT DROP AS
SELECT t.n AS term_no, g.grade_id, g.sequence_order AS seq, s.ord, s.code, s.name, s.weight,
       ('5eed0005-0002-4000-8000-00000000000' || t.n)::uuid AS term_id,
       ('5eed0005-0003-4000-8000-0000000' || t.n || lpad(g.sequence_order::text, 2, '0') || lpad(s.ord::text, 2, '0'))::uuid AS subject_id,
       CASE WHEN t.n IN ('1', '3') THEN 40 ELSE 80 END AS max_marks,
       CASE WHEN t.n IN ('1', '3') THEN 13 ELSE 27 END AS pass_marks
FROM grades g
CROSS JOIN (VALUES ('1'), ('2'), ('3'), ('4')) AS t(n)
JOIN (VALUES
    ('P', 1, 'ENG', 'English', 1.0), ('P', 2, 'HIN', 'Hindi', 1.0), ('P', 3, 'MATH', 'Mathematics', 1.0), ('P', 4, 'EVS', 'Environmental Studies', 1.0),
    ('M', 1, 'ENG', 'English', 1.0), ('M', 2, 'HIN', 'Hindi', 1.0), ('M', 3, 'MATH', 'Mathematics', 1.0), ('M', 4, 'SCI', 'Science', 1.5),
    ('M', 5, 'SST', 'Social Science', 1.0),
    ('S', 1, 'ENG', 'English', 1.0), ('S', 2, 'PHY', 'Physics', 1.0), ('S', 3, 'CHEM', 'Chemistry', 1.0), ('S', 4, 'MATH', 'Mathematics', 1.0),
    ('S', 5, 'CS', 'Computer Science', 1.0)
) AS s(band, ord, code, name, weight)
  ON s.band = CASE WHEN g.sequence_order BETWEEN 4 AND 8 THEN 'P' WHEN g.sequence_order BETWEEN 9 AND 13 THEN 'M' ELSE 'S' END
WHERE g.grade_id::text LIKE '5eed0003-0002-%' AND g.sequence_order >= 4;

INSERT INTO exam_term_subjects (exam_subject_id, exam_term_id, grade_id, subject_code, subject_name, max_marks, pass_marks, weightage,
                                display_order, created_at, updated_at, created_by, updated_by, version)
SELECT subject_id, term_id, grade_id, code, name, max_marks, pass_marks, weight, ord,
       TIMESTAMPTZ '2026-03-15 05:30:00+00', TIMESTAMPTZ '2026-03-15 05:30:00+00', 'seed:module05', NULL, 0
FROM seed_subject;

-- ---------------------------------------------------------------------------------------------------
-- 3. Marks for Periodic Test 1 and Half-Yearly (US-05003)
--    Each student has an ability (45-90%, fixed for the named students) plus a per-paper variation of ±8;
--    the Half-Yearly moves each student by -6 … +8 so the profile trend varies.
-- ---------------------------------------------------------------------------------------------------
CREATE TEMP TABLE seed_mark ON COMMIT DROP AS
SELECT s.student_id, s.student_uid, s.section_id, sub.term_no, sub.term_id, sub.subject_id, sub.code, sub.max_marks,
       greatest(15, least(99,
           CASE s.student_uid WHEN 'STU-2026-00101' THEN 82 WHEN 'STU-2022-00045' THEN 91 WHEN 'STU-2020-00027' THEN 68
                              WHEN 'STU-2021-00038' THEN 55 WHEN 'STU-2018-00012' THEN 76 ELSE 45 + abs(hashtext(s.student_uid)) % 46 END
           + (abs(hashtext(s.student_uid || sub.code || sub.term_no)) % 17) - 8
           + CASE WHEN sub.term_no = '2' THEN (abs(hashtext(s.student_uid || 'trend')) % 15) - 6 ELSE 0 END)) AS pct
FROM student_master s
JOIN seed_subject sub ON sub.grade_id = s.grade_id AND sub.term_no IN ('1', '2')
WHERE s.tenant_id = 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11' AND s.student_status = 'ACTIVE' AND s.section_id IS NOT NULL
  -- Riya Bansal's Half-Yearly Science is still missing (US-05016)
  AND NOT (s.student_uid = 'STU-2024-00242' AND sub.term_no = '2' AND sub.code = 'SCI');

INSERT INTO marks_entry_register (mark_entry_id, exam_term_id, exam_subject_id, student_id, section_id, marks_obtained, is_absent, is_locked,
                                  evaluated_by, entered_at, created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0005-' || substr(m.h, 1, 4) || '-4' || substr(m.h, 6, 3) || '-8' || substr(m.h, 10, 3) || '-' || substr(m.h, 13, 12))::uuid,
       m.term_id, m.subject_id, m.student_id, m.section_id,
       CASE WHEN m.absent THEN NULL
            WHEN m.student_uid = 'STU-2024-00253' AND m.term_no = '2' AND m.code = 'MATH' THEN 18     -- Om Varma fails Mathematics
            ELSE round(m.pct * m.max_marks / 100.0) END,
       m.absent, m.term_no = '1', 'neha.kapoor@dps.edu.in', m.entered, m.entered, m.entered, 'seed:module05', NULL, 0
FROM (SELECT x.*, md5(x.student_id::text || x.subject_id::text) AS h,
             (x.student_uid = 'STU-2017-00235' AND x.term_no = '2' AND x.code = 'ENG') AS absent,          -- Diya Nair absent
             CASE x.term_no WHEN '1' THEN TIMESTAMPTZ '2026-05-14 09:00:00+00' ELSE TIMESTAMPTZ '2026-10-03 09:00:00+00' END
               + (abs(hashtext(x.student_uid || x.code)) % 300 || ' minutes')::interval AS entered
      FROM seed_mark x) m;

-- ---------------------------------------------------------------------------------------------------
-- 4. Unlock ticket, co-scholastic grades, remarks and publication
-- ---------------------------------------------------------------------------------------------------
INSERT INTO grade_lock_tickets (ticket_id, exam_term_id, exam_subject_id, section_id, requested_by, reason, approval_status,
                                created_at, updated_at, created_by, updated_by, version)
SELECT '5eed0005-0005-4000-8000-000000000001', '5eed0005-0002-4000-8000-000000000002', sub.subject_id,
       '5eed0003-0003-4000-8000-000000000101',                                                   -- Class 7-A
       'neha.kapoor@dps.edu.in', 'Riya Bansal was absent on the exam day and took the Science re-test on 12 Oct; please reopen to enter her marks.',
       'PENDING', TIMESTAMPTZ '2026-10-13 06:00:00+00', TIMESTAMPTZ '2026-10-13 06:00:00+00', 'seed:module05', NULL, 0
FROM seed_subject sub WHERE sub.term_no = '2' AND sub.seq = 10 AND sub.code = 'SCI';

INSERT INTO co_scholastic_grades (co_scholastic_id, exam_term_id, student_id, area, grade, remarks, assessed_by,
                                  created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0005-' || substr(h, 1, 4) || '-4' || substr(h, 6, 3) || '-8' || substr(h, 10, 3) || '-' || substr(h, 13, 12))::uuid,
       '5eed0005-0002-4000-8000-000000000002', student_id, area,
       (ARRAY['A+', 'A', 'A', 'B', 'B', 'C'])[1 + abs(hashtext(student_uid || area)) % 6],
       CASE WHEN area = 'HEALTH_AND_PHYSICAL_EDUCATION' AND student_uid = 'STU-2020-00027' THEN 'House football captain' END,
       'priya.nair@dps.edu.in', TIMESTAMPTZ '2026-10-05 08:00:00+00', TIMESTAMPTZ '2026-10-05 08:00:00+00', 'seed:module05', NULL, 0
FROM (SELECT s.student_id, s.student_uid, a.area, md5(s.student_id::text || a.area || 'cs') AS h
      FROM student_master s
      CROSS JOIN (VALUES ('ART_EDUCATION'), ('HEALTH_AND_PHYSICAL_EDUCATION'), ('WORK_EDUCATION'), ('VALUE_EDUCATION'), ('DISCIPLINE')) AS a(area)
      WHERE s.tenant_id = 'a0eEBc99-9c0b-4ef8-bb6d-6bb9bd380a11' AND s.student_status = 'ACTIVE'
        AND s.section_id IN ('5eed0003-0003-4000-8000-000000000091', '5eed0003-0003-4000-8000-000000000101')) x;   -- 6-A, 7-A

-- Progress cards: Periodic Test 1 published for everyone; Half-Yearly published for Class 6-A with class teacher remarks
INSERT INTO holistic_progress_cards (hpc_id, exam_term_id, student_id, teacher_qualitative_remarks, remarks_by, is_published, published_at,
                                     published_by, created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0005-' || substr(h, 1, 4) || '-4' || substr(h, 6, 3) || '-8' || substr(h, 10, 3) || '-' || substr(h, 13, 12))::uuid,
       term_id, student_id, remarks, CASE WHEN remarks IS NOT NULL THEN 'priya.nair@dps.edu.in' END, true, published_at,
       'rajiv.bhalla@dps.edu.in', published_at, published_at, 'seed:module05', NULL, 0
FROM (
    SELECT DISTINCT m.student_id, m.term_id, md5(m.student_id::text || m.term_id::text || 'hpc') AS h,
           CASE WHEN m.term_no = '2' THEN
               CASE s.student_uid
                   WHEN 'STU-2026-00101' THEN 'Aarav has settled in well this year. Strong in Mathematics and Science; should read more in Hindi.'
                   WHEN 'STU-2022-00045' THEN 'Saanvi is a consistent top performer and a helpful class monitor.'
                   WHEN 'STU-2021-00032' THEN 'Vihaan participates eagerly; needs more regular revision before tests.'
                   ELSE 'Steady progress this term.' END END AS remarks,
           CASE m.term_no WHEN '1' THEN TIMESTAMPTZ '2026-05-25 06:00:00+00' ELSE TIMESTAMPTZ '2026-10-14 06:00:00+00' END AS published_at
    FROM seed_mark m JOIN student_master s ON s.student_id = m.student_id
    WHERE m.term_no = '1' OR m.section_id = '5eed0003-0003-4000-8000-000000000091'
) c;

COMMIT;
