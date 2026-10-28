-- =====================================================================================================
-- studentservice upgrade: class teacher (form tutor) of a section.
-- Adds the columns behind PATCH /api/v1/academic/sections/{id}/class-teacher and the Class teacher shown on the student forms.
-- Idempotent; run once per database before (or after) starting the new studentservice build:
--     powershell -ExecutionPolicy Bypass -File .agents/skills/test-ui/scripts/db/db.ps1 -Script <this file>
-- =====================================================================================================
BEGIN;

ALTER TABLE sections
    ADD COLUMN IF NOT EXISTS class_teacher_id uuid,
    ADD COLUMN IF NOT EXISTS class_teacher_name varchar(150);

COMMIT;
