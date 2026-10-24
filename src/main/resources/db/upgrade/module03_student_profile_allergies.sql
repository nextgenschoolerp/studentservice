-- =====================================================================================================
-- studentservice upgrade: personal details, admission details and separate allergies on student_master.
-- Adds the columns the Student profile save (PUT /students/{id}/profile, PUT /students/{id}/medical) needs,
-- and moves allergy text that was typed into the medical conditions out to its own column.
-- Idempotent; run once per database before (or after) starting the new studentservice build:
--     powershell -ExecutionPolicy Bypass -File .agents/skills/test-ui/scripts/db/db.ps1 -Script <this file>
-- =====================================================================================================
BEGIN;

ALTER TABLE student_master
    ADD COLUMN IF NOT EXISTS preferred_name varchar(100),
    ADD COLUMN IF NOT EXISTS nationality varchar(60),
    ADD COLUMN IF NOT EXISTS admission_date date,
    ADD COLUMN IF NOT EXISTS admission_number varchar(50),
    ADD COLUMN IF NOT EXISTS allergies varchar(500),
    ADD COLUMN IF NOT EXISTS medication_instructions varchar(500),
    ADD COLUMN IF NOT EXISTS gp_contact varchar(255),
    ADD COLUMN IF NOT EXISTS collection_permission varchar(60);

-- Allergy text typed into the medical alerts moves to allergies; only rows not yet moved are touched
UPDATE student_master
   SET allergies = medical_alerts, medical_alerts = 'None'
 WHERE allergies IS NULL AND medical_alerts ILIKE '%allerg%';

COMMIT;
