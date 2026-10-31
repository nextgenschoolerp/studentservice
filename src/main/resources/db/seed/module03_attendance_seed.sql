-- =====================================================================================================
-- Seed data: daily student attendance (studentservice /api/v1/attendance, built on the US-03018 roll list)
-- Target DB : jdbc:postgresql://localhost:5432/authservice (tables student_attendance / attendance_registers are created
--             by Hibernate: start studentservice once with the attendance code before running this script)
-- Run after : module03_student_seed.sql (uses its DPS North students, ids 5eed0003-0005-…)
--
-- Dates are relative to the day the script runs, so the screens always show a recent fortnight:
--   * the last 10 school days before today (Monday–Friday, the 26–30 Oct 2026 half-term skipped): AM and PM registers of
--     every DPS North section with seeded students. Marks are fixed per student and day (hash), roughly 90% present,
--     3% late, 4% excused with a reason, 3% unexcused; a student absent in the morning stays absent in the afternoon.
--     Unexcused follow-ups: older than 3 school days RESOLVED, newer GUARDIAN_CONTACTED (with a note)
--   * Myra (Class 6-B, STU-2021-00038): unexcused on three of those days — a persistent-absence case
--   * today, on a school day: the AM register of every section EXCEPT Class 4-A and Class 6-A, left for you to take
--     (as classteacher_priya), and no PM registers. Today's AM includes:
--       Kabir (8-A) unexcused, follow-up OPEN · Arjun (10-A) excused "Fever" · Zara (10-A) late "School bus late"
--
-- Registers are submitted by the seeded class teacher classteacher_priya; follow-ups by officeclerk_anil.
-- Re-runnable: every row is created_by = 'seed:attendance' and every id starts with 5eed0003-00a0- (marks) or
-- 5eed0003-00a1- (registers). The script deletes only those rows first. Test-ui cleanup (db.ps1 -Cleanup) keeps them.
-- =====================================================================================================

BEGIN;

DELETE FROM student_attendance   WHERE created_by = 'seed:attendance' OR attendance_id::text LIKE '5eed0003-00a0-%';
DELETE FROM attendance_registers WHERE created_by = 'seed:attendance' OR register_id::text   LIKE '5eed0003-00a1-%';

-- Seeded DPS North students on roll with a class and section
CREATE TEMP TABLE att_students ON COMMIT DROP AS
SELECT m.student_id, m.tenant_id, m.branch_id, m.grade_id, m.section_id, m.student_uid,
       g.grade_name || '-' || s.section_name AS class_name
FROM student_master m
JOIN sections s ON s.section_id = m.section_id
JOIN grades g ON g.grade_id = m.grade_id
WHERE m.student_id::text LIKE '5eed0003-%'
  AND m.branch_id = 'DPS-DELHI-NORTH'
  AND m.student_status IN ('ACTIVE', 'PROMOTED');

-- The last 10 school days before today (n = 1 is the most recent), and today when it is a school day (n = 0)
CREATE TEMP TABLE att_days ON COMMIT DROP AS
WITH past AS (
  SELECT d::date AS day
  FROM generate_series(CURRENT_DATE - 30, CURRENT_DATE - 1, INTERVAL '1 day') d
  WHERE EXTRACT(ISODOW FROM d) <= 5 AND NOT (d::date BETWEEN DATE '2026-10-26' AND DATE '2026-10-30')
)
SELECT day, ROW_NUMBER() OVER (ORDER BY day DESC) AS n FROM past ORDER BY day DESC LIMIT 10;
INSERT INTO att_days
SELECT CURRENT_DATE, 0
WHERE EXTRACT(ISODOW FROM CURRENT_DATE) <= 5 AND NOT (CURRENT_DATE BETWEEN DATE '2026-10-26' AND DATE '2026-10-30');

-- One row per student, day and session with a fixed pseudo-random number 0–99
CREATE TEMP TABLE att_marks ON COMMIT DROP AS
SELECT st.*, dy.day, dy.n, sess.session,
       ABS(hashtext(st.student_id::text || dy.day::text || 'AM')) % 100 AS h_am,
       ABS(hashtext(st.student_id::text || dy.day::text || 'PM')) % 100 AS h_pm
FROM att_students st
CROSS JOIN att_days dy
CROSS JOIN (VALUES ('AM'), ('PM')) AS sess(session)
-- today: AM only, and not for Class 4-A / 6-A (left open)
WHERE dy.n > 0 OR (sess.session = 'AM' AND st.class_name NOT IN ('Class 4-A', 'Class 6-A'));

ALTER TABLE att_marks ADD COLUMN am_status TEXT, ADD COLUMN status TEXT, ADD COLUMN reason TEXT;
UPDATE att_marks SET am_status = CASE WHEN h_am < 3 THEN 'UNEXCUSED' WHEN h_am < 7 THEN 'EXCUSED' WHEN h_am < 10 THEN 'LATE' ELSE 'PRESENT' END;
-- Named cases
UPDATE att_marks SET am_status = 'UNEXCUSED' WHERE student_uid = 'STU-2021-00038' AND n IN (2, 5, 8);   -- Myra: persistent absence
UPDATE att_marks SET am_status = 'UNEXCUSED' WHERE student_uid = 'STU-2020-00027' AND n = 0;           -- Kabir today
UPDATE att_marks SET am_status = 'EXCUSED'   WHERE student_uid = 'STU-2016-00008' AND n = 0;           -- Arjun today
UPDATE att_marks SET am_status = 'LATE'      WHERE student_uid = 'STU-2016-00011' AND n = 0;           -- Zara today
UPDATE att_marks SET am_status = 'PRESENT'   WHERE n = 0 AND am_status <> 'PRESENT'
  AND student_uid NOT IN ('STU-2020-00027', 'STU-2016-00008', 'STU-2016-00011');                      -- today: only the named cases

-- AM keeps its mark; PM: absent all day when absent in the morning, otherwise mostly present
UPDATE att_marks SET status = CASE
    WHEN session = 'AM' THEN am_status
    WHEN am_status IN ('EXCUSED', 'UNEXCUSED') THEN am_status
    WHEN h_pm < 2 THEN 'EXCUSED'
    ELSE 'PRESENT' END;
UPDATE att_marks SET reason = CASE
    WHEN student_uid = 'STU-2016-00008' AND n = 0 THEN 'Fever'
    WHEN student_uid = 'STU-2016-00011' AND n = 0 THEN 'School bus late'
    WHEN status = 'EXCUSED' AND session = 'PM' AND am_status NOT IN ('EXCUSED', 'UNEXCUSED') THEN 'Left early: medical appointment'
    WHEN status = 'EXCUSED' THEN (ARRAY['Fever', 'Medical appointment', 'Family function', 'District sports meet', 'Religious festival'])[1 + h_am % 5]
    WHEN status = 'LATE' THEN (ARRAY['School bus late', 'Traffic at the school gate'])[1 + h_am % 2]
    ELSE NULL END;

INSERT INTO student_attendance (attendance_id, tenant_id, branch_id, student_id, grade_id, section_id, attendance_date, attendance_session,
                                status, reason, marked_by, follow_up_status, follow_up_note, follow_up_by, follow_up_at,
                                created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0003-00a0-4000-8000-' || LPAD(ROW_NUMBER() OVER (ORDER BY day, class_name, student_uid, session)::text, 12, '0'))::uuid,
       tenant_id, branch_id, student_id, grade_id, section_id, day, session, status, reason, 'classteacher_priya',
       CASE WHEN status <> 'UNEXCUSED' THEN NULL WHEN n = 0 THEN 'OPEN' WHEN n > 3 THEN 'RESOLVED' ELSE 'GUARDIAN_CONTACTED' END,
       CASE WHEN status <> 'UNEXCUSED' OR n = 0 THEN NULL
            WHEN n > 3 THEN 'Guardian confirmed the absence; note received in the diary'
            ELSE 'Called the mother; she will send a note tomorrow' END,
       CASE WHEN status = 'UNEXCUSED' AND n > 0 THEN 'officeclerk_anil' END,
       CASE WHEN status = 'UNEXCUSED' AND n > 0 THEN (day + TIME '11:30') AT TIME ZONE 'Asia/Kolkata' END,
       (day + CASE WHEN session = 'AM' THEN TIME '08:50' ELSE TIME '13:20' END) AT TIME ZONE 'Asia/Kolkata',
       (day + CASE WHEN session = 'AM' THEN TIME '08:50' ELSE TIME '13:20' END) AT TIME ZONE 'Asia/Kolkata',
       'seed:attendance', 'seed:attendance', 0
FROM att_marks;

-- One register per section, day and session that has marks
INSERT INTO attendance_registers (register_id, tenant_id, section_id, register_date, register_session, submitted_by, submitted_at,
                                  created_at, updated_at, created_by, updated_by, version)
SELECT ('5eed0003-00a1-4000-8000-' || LPAD(ROW_NUMBER() OVER (ORDER BY day, section_id, session)::text, 12, '0'))::uuid,
       tenant_id, section_id, day, session, 'classteacher_priya',
       (day + CASE WHEN session = 'AM' THEN TIME '08:52' ELSE TIME '13:22' END) AT TIME ZONE 'Asia/Kolkata',
       (day + CASE WHEN session = 'AM' THEN TIME '08:52' ELSE TIME '13:22' END) AT TIME ZONE 'Asia/Kolkata',
       (day + CASE WHEN session = 'AM' THEN TIME '08:52' ELSE TIME '13:22' END) AT TIME ZONE 'Asia/Kolkata',
       'seed:attendance', 'seed:attendance', 0
FROM (SELECT DISTINCT tenant_id, section_id, day, session FROM att_marks) r;


-- The school's register mode (Settings → Academic setup → Attendance registers): DPS takes one register a day.
-- The seeded marks above have AM and PM rows, so switching to AM_PM shows both sessions straight away.
CREATE TABLE IF NOT EXISTS attendance_settings (
    tenant_id character varying(64) NOT NULL PRIMARY KEY,
    created_at timestamp(6) with time zone, created_by character varying(255), updated_at timestamp(6) with time zone,
    updated_by character varying(255), version bigint,
    register_mode character varying(10) DEFAULT 'DAILY' NOT NULL
);
INSERT INTO attendance_settings (tenant_id, register_mode, created_at, created_by, version)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'DAILY', now(), 'seed:attendance', 0)
ON CONFLICT (tenant_id) DO UPDATE SET register_mode = EXCLUDED.register_mode, updated_at = now(), updated_by = 'seed:attendance';

COMMIT;

-- Check: marks and registers per day
-- SELECT attendance_date, attendance_session, status, COUNT(*) FROM student_attendance WHERE created_by = 'seed:attendance'
--   GROUP BY 1, 2, 3 ORDER BY 1 DESC, 2, 3;
