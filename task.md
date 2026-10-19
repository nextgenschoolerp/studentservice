# Task Execution Record: Module 03 — Student Information System (SIS) & Student Lifecycle

Execution of the `brd-to-api` skill for **Module 03** based on [`story/module3/School_ERP_Module_03_SIS_Lifecycle_1000.md`](../story/module3/School_ERP_Module_03_SIS_Lifecycle_1000.md), [`module3.sql`](../story/module3/module3.sql) and [`module3.md`](../story/module3/module3.md).

---

## 1. Input Specification & Target Microservice Resolution

- **User Stories**: `US-03000` … `US-03019` (the 20 base stories, scenario 1 of each) and `US-03020` … `US-03100` (81 scenario variations, verified as a role matrix). The BRD's 1,000 entries are 50 variations of the 20 base stories.
- **Module**: `03` — Student Information System (SIS) & Student Lifecycle
- **Target Microservice**: `studentservice`
- **Base Package**: `com.kvs.student`
- **Allocated Server Port**: `8084`, context path `/studentservice` ([`application.yml`](src/main/resources/application.yml), `SERVER_PORT` / `SERVER_CONTEXT_PATH`). Test port `18084`.
- **Database**: shared PostgreSQL database `authservice` (as authservice, admissionservice, financeservice and examservice), tables created by Hibernate `ddl-auto: update`
- **Service Lifecycle Status**:
  - [ ] Existing Microservice
  - [x] New Microservice (scaffolded via Step 0)
- **Reference Architecture**: `references/java-patterns.md`, `references/microservice-scaffolding-guide.md`; Spring Boot 3.2.5, Java 21, Spring Data JPA, Spring Security 6, `com.kvs.core:ms-core:1.0.0-SNAPSHOT`
- **Downstream readers**: examservice maps `student_master`, `grades`, `sections` and `academic_sessions` read-only (rosters and class names); financeservice copies student identity onto `fee_accounts` when demands are posted.

---

## 2. Requirement Extraction (from BRD)

| Story | Sub-module (BRD) | Actor → Role | Action |
|---|---|---|---|
| US-03000 | Student Enrollment & Permanent UID Generation | Registrar → `ROLE_REGISTRAR` | Enrol a student and issue a permanent, collision-free UID |
| US-03001 | Demographics, Blood Groups & Medical Alerts | School Nurse → `ROLE_SCHOOL_NURSE`; Registrar | Record blood group, medical alerts and emergency phone |
| US-03002 | Consolidated Parent Account & Sibling Linking | Registrar; Office Clerk → `ROLE_OFFICE_CLERK` | Link siblings to one parent account |
| US-03003 | Academic Section & School House Allocation | Class Teacher → `ROLE_CLASS_TEACHER`; House Master → `ROLE_HOUSE_MASTER` | Place a student in a section and house with a roll number |
| US-03004 | Barcoded Student ID Cards & QR Verification | Office Clerk; Registrar | Generate the ID card barcode and QR data |
| US-03005 | Annual Batch Promotion Engine | School Principal → `ROLE_SCHOOL_PRINCIPAL`; Registrar | Advance the students who passed to the next class |
| US-03006 | Dual-Signoff Official Transfer Certificates (TC) | Principal & Vice Principal → `ROLE_VICE_PRINCIPAL` | Issue a TC signed off by two different people |
| US-03007 | Tamper-Evident Bonafide & Character Certificates | Registrar; Office Clerk | Issue bonafide and character certificates with a verification hash |
| US-03008 | Disciplinary Incident Tracking & Counseling Logs | Class Teacher; Principal | Record incidents with severity and counseling notes |
| US-03009 | Permanent Alumni Registry & Ledger Archiving | Alumni Coordinator → `ROLE_ALUMNI_COORDINATOR`; Registrar | Archive graduates into the alumni registry |
| US-03010 | Student Demographic Multi-Criteria Search & Filtering | Class Teacher; Academic Head → `ROLE_ACADEMIC_HEAD` | Search students by class, gender, blood group and more |
| US-03011 | Bulk Contact, Address & Emergency Phone Updates | Office Clerk; School Nurse; Principal | Update contact details of many students at once |
| US-03012 | Extracurricular Credits, Activity Logging & House Points | House Master; Class Teacher | Log activity credits and total house points |
| US-03013 | Historical Alumni Ledger & Credential Verification Engine | Alumni Coordinator; Office Clerk | Verify a former student's credentials for a third party |
| US-03014 | Profile Photo Updates & Class Directory Export | Academic Head; Class Teacher; Office Clerk | Update photos and export the class photo directory |
| US-03015 | Administrative Hold Management (Fee/Document Deficiencies) | School Principal; School Nurse; Registrar | Place and release administrative holds |
| US-03016 | Periodic Health Checkup Metrics & BMI Tracking | School Nurse; House Master; Registrar | Record height and weight and compute BMI |
| US-03017 | Language Preferences & Elective Subject Selections | Office Clerk; Class Teacher; Alumni Coordinator | Record language and elective choices |
| US-03018 | Class Enrollment Roll List Generation for Attendance | Class Teacher; Academic Head | Produce a section's roll list |
| US-03019 | Student Section Transfer Proposal & Approval Workflow | Class Teacher; Academic Head; Principal | Propose and review section transfers |

The BRD rotates eight actors across the scenario variations (`REGISTRAR`, `OFFICE_CLERK`, `CLASS_TEACHER`, `SCHOOL_NURSE`, `HOUSE_MASTER`, `ALUMNI_COORDINATOR`, `ACADEMIC_HEAD`, `SCHOOL_PRINCIPAL`); US-03020 … US-03100 are checked against those roles in `ScenarioActorAccessTest`.

### Acceptance Criteria (each story)
- [x] **AC-1 (Privileges)**: only the story's actors (plus `SUPER_ADMIN`) may act; the exact roles per endpoint are in §3 and served live by `GET /api/v1/academic/access-rules`
- [x] **AC-2 (Execution/Input)**: endpoints accept the documented payloads
- [x] **AC-3 (Validation)**: invalid input → `400 VALIDATION_FAILED` / `BAD_REQUEST`
- [x] **AC-4 (Persistence)**: transactional services; promotion, transfers and certificates update the student record in one transaction
- [x] **AC-5 (Audit Trail)**: `audit_logs` rows from `StudentAuditService` (enrolment, medical, allocation, `BATCH_PROMOTION`, certificates, incidents, holds, transfers, alumni archiving)
- [x] **AC-6 (Security/Isolation)**: `401` without a token, `403` for other roles, `404` for another institution's students (`TenantAccessGuard`; `SUPER_ADMIN` may cross tenants)

---

## 3. API Contract & Architectural Design

Roles are the `@PreAuthorize` of each endpoint, read from the running service (`GET /api/v1/academic/access-rules`). `SUPER_ADMIN` is allowed everywhere.

| Method | Path | Story | Roles |
|---|---|---|---|
| `POST` | `/api/v1/students` | US-03000 | REGISTRAR, OFFICE_CLERK |
| `GET` | `/api/v1/students`, `/api/v1/students/{studentId}`, `/api/v1/students/uid/{studentUid}` | US-03000 | REGISTRAR, OFFICE_CLERK, CLASS_TEACHER, SCHOOL_PRINCIPAL, SCHOOL_NURSE |
| `PUT` | `/api/v1/students/{studentId}/medical` | US-03001 | SCHOOL_NURSE, REGISTRAR |
| `POST` | `/api/v1/students/{studentId}/siblings` | US-03002 | REGISTRAR, OFFICE_CLERK |
| `PUT` | `/api/v1/students/{studentId}/allocation` | US-03003 | REGISTRAR, CLASS_TEACHER, HOUSE_MASTER |
| `GET` | `/api/v1/students/{studentId}/id-card` | US-03004 | OFFICE_CLERK, REGISTRAR, CLASS_TEACHER |
| `POST` | `/api/v1/academic/batch-promotion` | US-03005 | REGISTRAR, SCHOOL_PRINCIPAL |
| `POST` | `/api/v1/students/{studentId}/certificates/transfer` | US-03006 | SCHOOL_PRINCIPAL, VICE_PRINCIPAL, REGISTRAR |
| `POST` | `/api/v1/students/{studentId}/certificates/bonafide` | US-03007 | REGISTRAR, OFFICE_CLERK, SCHOOL_PRINCIPAL |
| `GET` | `/api/v1/students/{studentId}/certificates` | US-03006 / US-03007 | REGISTRAR, OFFICE_CLERK, SCHOOL_PRINCIPAL, CLASS_TEACHER |
| `POST` / `GET` | `/api/v1/students/{studentId}/disciplinary` | US-03008 | CLASS_TEACHER, SCHOOL_PRINCIPAL, VICE_PRINCIPAL |
| `POST` | `/api/v1/alumni/archive` | US-03009 | ALUMNI_COORDINATOR, REGISTRAR |
| `GET` | `/api/v1/alumni/{studentUid}` | US-03009 | ALUMNI_COORDINATOR, REGISTRAR, SCHOOL_PRINCIPAL |
| `GET` | `/api/v1/students/search` | US-03010 | CLASS_TEACHER, ACADEMIC_HEAD, REGISTRAR, OFFICE_CLERK |
| `PUT` | `/api/v1/students/bulk-contact-update` | US-03011 | SCHOOL_NURSE, SCHOOL_PRINCIPAL, REGISTRAR, OFFICE_CLERK |
| `POST` | `/api/v1/students/{studentId}/activities` | US-03012 | HOUSE_MASTER, CLASS_TEACHER, REGISTRAR |
| `GET` | `/api/v1/students/{studentId}/activities` | US-03012 | HOUSE_MASTER, CLASS_TEACHER, REGISTRAR, SCHOOL_PRINCIPAL |
| `GET` | `/api/v1/academic/houses/{houseId}/points` | US-03012 | HOUSE_MASTER, CLASS_TEACHER, ACADEMIC_HEAD, REGISTRAR, SCHOOL_PRINCIPAL |
| `POST` / `GET` | `/api/v1/alumni/credential-verifications[/{requestId}]` | US-03013 | ALUMNI_COORDINATOR, OFFICE_CLERK, REGISTRAR |
| `PATCH` | `/api/v1/alumni/credential-verifications/{requestId}/verify` | US-03013 | ALUMNI_COORDINATOR, REGISTRAR |
| `POST` | `/api/v1/students/{studentId}/photo` | US-03014 | ACADEMIC_HEAD, CLASS_TEACHER, OFFICE_CLERK, REGISTRAR |
| `GET` | `/api/v1/students/export-photo-directory` | US-03014 | ACADEMIC_HEAD, CLASS_TEACHER, OFFICE_CLERK, REGISTRAR |
| `POST` | `/api/v1/students/{studentId}/holds` | US-03015 | SCHOOL_PRINCIPAL, SCHOOL_NURSE, REGISTRAR |
| `PATCH` | `/api/v1/students/holds/{holdId}/release` | US-03015 | SCHOOL_PRINCIPAL, SCHOOL_NURSE, REGISTRAR |
| `GET` | `/api/v1/students/{studentId}/holds` | US-03015 | SCHOOL_PRINCIPAL, SCHOOL_NURSE, REGISTRAR, OFFICE_CLERK |
| `POST` | `/api/v1/students/{studentId}/health-metrics` | US-03016 | SCHOOL_REGISTRAR, REGISTRAR, HOUSE_MASTER, SCHOOL_NURSE |
| `GET` | `/api/v1/students/{studentId}/health-metrics` | US-03016 | as above, plus CLASS_TEACHER |
| `POST` / `GET` | `/api/v1/students/{studentId}/electives` | US-03017 | OFFICE_CLERK, ALUMNI_COORDINATOR, CLASS_TEACHER, REGISTRAR |
| `GET` | `/api/v1/academic/sections/{sectionId}/roll-list` | US-03018 | CLASS_TEACHER, ACADEMIC_HEAD, REGISTRAR, OFFICE_CLERK, SCHOOL_PRINCIPAL |
| `POST` / `GET` | `/api/v1/students/{studentId}/section-transfers` | US-03019 | SCHOOL_NURSE, ACADEMIC_HEAD, SCHOOL_PRINCIPAL, REGISTRAR, CLASS_TEACHER |
| `PATCH` | `/api/v1/students/section-transfers/{transferId}/review` | US-03019 | ACADEMIC_HEAD, SCHOOL_PRINCIPAL, REGISTRAR |
| `POST` / `GET` | `/api/v1/academic/sessions`, `/grades`, `/sections`, `/houses` | US-03003 (academic structure) | create: REGISTRAR; list: REGISTRAR, OFFICE_CLERK, SCHOOL_PRINCIPAL (+ CLASS_TEACHER for grades, sections, houses; + HOUSE_MASTER for houses) |
| `POST` | `/api/v1/students/{studentId}/photo/upload`, `/documents` | US-03014 (files) | photo: REGISTRAR, OFFICE_CLERK; documents: + SCHOOL_NURSE |
| `GET` | `/api/v1/students/{studentId}/photo/file`, `/documents`, `/documents/{documentId}/file` | US-03014 (files) | REGISTRAR, OFFICE_CLERK, CLASS_TEACHER, SCHOOL_PRINCIPAL, SCHOOL_NURSE |
| `GET` | `/api/v1/academic/access-rules` | – (tooling) | any signed-in user |

### Component Mapping
- **Entities** (extend `BaseAuditEntity`): `AcademicSession`, `Grade`, `Section`, `House`, `StudentMaster`, `StudentGuardian`, `CertificateIssued`, `DisciplinaryIncident`, `AlumniRecord`, `StudentActivityCredit`, `CredentialVerificationRequest`, `AdministrativeHold`, `StudentHealthMetric`, `StudentElectiveSelection`, `SectionTransferRequest`, `StudentDocument`, `AuditLog`
- **Enums**: `StudentStatus` (`ACTIVE`, `PROMOTED`, `SUSPENDED`, `TRANSFERRED`, `GRADUATED` …), `Gender`, `CertificateType`, `IncidentSeverity`, `HoldType`, `VerificationStatus`, `TransferRequestStatus`
- **Services**: `StudentService`, `AcademicHierarchyService`, `CertificateService`, `DisciplinaryService`, `AlumniService`, `StudentActivityService`, `StudentFileService` / `StudentFileStorage` (photo and document vault), `StudentAuditService`, `TenantAccessGuard`, `UidGenerator`
- **Controllers**: `StudentController`, `AcademicHierarchyController`, `StudentCertificateController`, `DisciplinaryIncidentController`, `AlumniRegistryController`, `StudentActivityController`, `StudentFileController`, `AccessRuleController`
- **Integration tests**: `StudentControllerIntegrationTest`, `ExtendedCapabilitiesIntegrationTest`, `StudentCapabilitiesBatch2IntegrationTest`, `TenantIsolationIntegrationTest`, `StudentFileControllerIntegrationTest`, `ScenarioActorAccessTest`, `AccessRuleControllerIntegrationTest`

### Design notes
- **Promotion (US-03005)**, changed 18 Oct 2026: the caller's tenant is checked against both grades; only `ACTIVE` or `PROMOTED` students of the class move; each keeps their section letter in the next class (7-A → 8-A; before, the section still pointed at the old class). The UI (Students → **Promote class**) only offers students whose final-term result in examservice is PASS and creates the next academic session when it is missing.
- **Enrolment from admissions (US-03000)**: admissionservice's *Enrol student* step converts the application and then calls `POST /api/v1/students` in the chosen section, passing the date of birth, gender and guardian relationship recorded on the application.
- **Results after promotion**: examservice treats `PROMOTED` students as on roll and keeps a term's results on the section where the marks were entered, so a promoted student's Class 7 progress card still says Class 7.
- **Certificate numbers** skip numbers already in the database, so a restart (counter back at 1001) no longer collides; UIDs do the same.

---

## 4. Implementation Checklist

### Step 0: Microservice Scaffolding
- [x] Directory structure, `postman/`, `wiki/api-docs/`
- [x] `build.gradle` (Spring Boot 3.2.5, Java 21, ms-core), `settings.gradle`, Gradle wrapper
- [x] `application.yml` (port 8084, context path, shared DB, JWT)
- [x] Bootstrap, `WebSecurityConfig`, `JpaAuditConfig`
- [x] `gradlew compileJava` clean

### Steps 1–6
- [x] Entities with tenant anchors, unique UIDs and indexes
- [x] Request DTOs with Jakarta validation; response DTOs with Lombok builders
- [x] Repositories with tenant / branch queries
- [x] Services with domain exceptions and audit entries
- [x] Controllers with `@PreAuthorize`, `@Valid`, `ApiResponse<T>`
- [x] Integration tests: 201/200, 400, 401, 403, 404, scenario role matrix — **122 tests, 0 failures**
- [x] `GET /api/v1/academic/access-rules` (endpoint → roles, read from the running service) for tools such as the workflow runners

### Step 7: Postman
- [x] [`postman/School_ERP_StudentService.postman_collection.json`](postman/School_ERP_StudentService.postman_collection.json): 101 story folders (US-03000 … US-03100), 112 requests

### UI mapping
- [x] Vite proxy `/api/v1/students`, `/api/v1/academic`, `/api/v1/alumni` → `http://localhost:8084/studentservice` (`STUDENT_SERVICE_URL`)
- [x] Students directory, student profile (overview, guardians, documents, conduct, certificates, ID card), Add / Edit student, **Promote class** — [`frontend/docs/studentservice-api-mapping.md`](../frontend/docs/studentservice-api-mapping.md)
- [x] Admissions → Review application → **Enrol student** creates the studentservice record — [`frontend/docs/admissionservice-api-mapping.md`](../frontend/docs/admissionservice-api-mapping.md)

### Step 8: API Wiki
- [x] [`wiki/api-docs/School_ERP_Module_03_SIS_API_Documentation.md`](wiki/api-docs/School_ERP_Module_03_SIS_API_Documentation.md) with `{expand}` blocks per story and the scenario actor matrix

### Step 9: Seed
- [x] [`src/main/resources/db/seed/module03_student_seed.sql`](src/main/resources/db/seed/module03_student_seed.sql) (re-runnable): Module 03 roles and one DPS North account per role; sessions 2025-2026 / 2026-2027; Nursery … Class 12 with sections A–E; four houses; 54 students (52 North, 2 South) with guardians, siblings, certificates, incidents, a transferred and a graduated student
- [x] [`src/main/resources/db/seed/module05_exam_seed.sql`](src/main/resources/db/seed/module05_exam_seed.sql) is kept here too, beside the Module 03 seed whose students it grades

---

## 5. Verification & Quality Gates

```bash
cd studentservice
.\gradlew.bat test
```

Run on 19 Oct 2026:

| Test class | Tests | Result |
|---|---|---|
| `StudentControllerIntegrationTest` | 11 | pass |
| `ExtendedCapabilitiesIntegrationTest` | 6 | pass |
| `StudentCapabilitiesBatch2IntegrationTest` | 12 | pass |
| `TenantIsolationIntegrationTest` | 7 | pass |
| `StudentFileControllerIntegrationTest` | 3 | pass |
| `ScenarioActorAccessTest` (US-03020 … US-03100) | 81 | pass |
| `AccessRuleControllerIntegrationTest` | 2 | pass |
| **Total** | **122** | **0 failures, 0 errors** |

- [x] UI regression `node .agents/skills/test-ui/scripts/api-regression.mjs --service studentservice`: **38 of 38 cases pass** (test stack, 19 Oct 2026)
- [x] Student lifecycle runner (`/dev/student-lifecycle-workflow.html`): all 15 steps green, from enquiry to promotion (enrolment, promotion and the record check are studentservice steps)
- [x] Browser: admission → enrol into a section → fees → Annual results → **Promote class** (Class 7-A → 8-A, the failing student held back); promoted students show **Promoted** in 8-A and keep their Class 7 results

## 6. Not yet implemented / known gaps

- No update endpoint for names, date of birth, gender or guardians after enrolment (Edit student can change medical, allocation, photo and documents only).
- The alumni registry has no list endpoint, so the Archived tab is built from the student list.
- Graduation from Class 12 into the alumni registry is not part of **Promote class** (the alumni archive endpoint exists, with no screen).
- Sibling linking (US-03002) and lookup by UID have no screen yet.
- Attendance (roll list US-03018 exists) has no attendance service to feed.
