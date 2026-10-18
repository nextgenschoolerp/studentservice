# API Specification: Module 03 — Student Information System (SIS) & Student Lifecycle

- **Target Microservice**: `studentservice` (Port: `8084`)
- **API Base Path**: `/api/v1`
- **Security Standard**: Bearer JWT Authentication with Role-Based Access Control (`@PreAuthorize`)
- **Traceability**: Micrometer Distributed Tracing (`X-Trace-Id`, `X-Span-Id`)
- **BRD Specification**: [`story/module3/School_ERP_Module_03_SIS_Lifecycle_1000.md`](../../../story/module3/School_ERP_Module_03_SIS_Lifecycle_1000.md)

---

## 1. Executive Summary & Capabilities

Module 03 implements comprehensive lifecycle management for enrolled students from initial entry to graduation:
1. **US-03000**: Collision-free permanent Student UID generation and enrollment confirmation.
2. **US-03001**: Real-time student medical alert, allergy, and emergency contact administration.
3. **US-03002**: Consolidated multi-sibling linking under parent user accounts.
4. **US-03003**: Dynamic academic section allocation and school house assignment.
5. **US-03004**: Barcoded student ID card generation with tamper-proof QR code verification URLs.
6. **US-03005**: Annual batch promotion engine advancing eligible students across grades.
7. **US-03006**: Dual administrative sign-off Transfer Certificate (TC) generation.
8. **US-03007**: Digital Bonafide and Character certificate issuance with verification tokens.
9. **US-03008**: Disciplinary incident logging and counseling intervention tracking.
10. **US-03009**: Permanent alumni registry archiving for graduating students.

---

## 2. Security & Header Specifications

All endpoints require standard security and tracing headers:

| Header Name | Type | Requirement | Description |
|---|---|---|---|
| `Authorization` | String | **Required** | `Bearer <JWT_TOKEN>` carrying role claims |
| `Content-Type` | String | **Required** (for POST/PUT) | `application/json` |
| `X-Trace-Id` | String | Optional | Distributed trace correlation identifier echoed in responses |
| `X-Span-Id` | String | Optional | Distributed span correlation identifier echoed in responses |

---

## 3. Endpoints Breakdown

{expand:title=US-03000: Student Enrollment & Permanent UID Generation}

### Endpoint
`POST /api/v1/students`

### Authorized Roles
`ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_OFFICE_CLERK`

### Request Payload
```json
{
  "tenantId": "DPS-DELHI",
  "branchId": "MAIN-01",
  "firstName": "Aarav",
  "lastName": "Sharma",
  "dateOfBirth": "2015-05-14",
  "gender": "MALE",
  "bloodGroup": "O+",
  "medicalAlerts": "Peanut allergy",
  "emergencyContactPhone": "+919876543210",
  "guardianName": "Rajesh Sharma",
  "guardianRelationship": "Father",
  "guardianPhone": "+919876543210",
  "guardianEmail": "rajesh.sharma@example.com"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Student enrolled successfully with permanent UID",
  "data": {
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "tenantId": "DPS-DELHI",
    "branchId": "MAIN-01",
    "studentUid": "STU2026000001",
    "firstName": "Aarav",
    "lastName": "Sharma",
    "studentStatus": "ACTIVE",
    "idCardBarcode": "BC-STU2026000001",
    "idCardQrCode": "https://verify.kvs.local/students/STU2026000001"
  }
}
```
{expand}

{expand:title=US-03001: Demographics, Blood Groups & Medical Alerts}

### Endpoint
`PUT /api/v1/students/{studentId}/medical`

### Authorized Roles
`ROLE_SCHOOL_NURSE`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`

### Request Payload
```json
{
  "bloodGroup": "B+",
  "medicalAlerts": "Asthma inhaler required before physical exercise",
  "emergencyContactPhone": "+919876543999"
}
```

### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Student medical alerts updated successfully",
  "data": {
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "studentUid": "STU2026000001",
    "bloodGroup": "B+",
    "medicalAlerts": "Asthma inhaler required before physical exercise",
    "emergencyContactPhone": "+919876543999"
  }
}
```
{expand}

{expand:title=US-03002: Sibling Profile Linking}

### Endpoint
`POST /api/v1/students/{studentId}/siblings`

### Authorized Roles
`ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_OFFICE_CLERK`

### Request Payload
```json
{
  "parentAccountId": "PARENT-ACC-8899",
  "siblingStudentId": "4bb96f75-6828-5673-c4fd-3d074f77bfb7"
}
```

### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Student sibling linked successfully",
  "data": {
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "studentUid": "STU2026000001",
    "parentAccountId": "PARENT-ACC-8899"
  }
}
```
{expand}

{expand:title=US-03003: Academic Section & House Allocation}

### Endpoint
`PUT /api/v1/students/{studentId}/allocation`

### Authorized Roles
`ROLE_REGISTRAR`, `ROLE_CLASS_TEACHER`, `ROLE_SUPER_ADMIN`, `ROLE_HOUSE_MASTER`

### Request Payload
```json
{
  "gradeId": "11111111-1111-1111-1111-111111111111",
  "sectionId": "22222222-2222-2222-2222-222222222222",
  "houseId": "33333333-3333-3333-3333-333333333333",
  "rollNumber": 15
}
```

### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Student allocated to section and house successfully",
  "data": {
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "studentUid": "STU2026000001",
    "gradeName": "Grade 5",
    "sectionName": "Section A",
    "houseName": "Red Falcons",
    "rollNumber": 15
  }
}
```
{expand}

{expand:title=US-03004: Barcoded ID Cards & QR Verification}

### Endpoint
`GET /api/v1/students/{studentId}/id-card`

### Authorized Roles
`ROLE_OFFICE_CLERK`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_CLASS_TEACHER`

### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "ID card data generated successfully",
  "data": {
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "studentUid": "STU2026000001",
    "fullName": "Aarav Sharma",
    "gradeName": "Grade 5",
    "sectionName": "Section A",
    "houseName": "Red Falcons",
    "barcodeData": "BC-STU2026000001",
    "qrCodeData": "https://verify.kvs.local/students/STU2026000001",
    "verificationUrl": "https://verify.kvs.local/students/STU2026000001"
  }
}
```
{expand}

{expand:title=US-03005: Annual Batch Promotion Engine}

### Endpoint
`POST /api/v1/academic/batch-promotion`

### Authorized Roles
`ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_SCHOOL_PRINCIPAL`

### Request Payload
```json
{
  "currentAcademicYearId": "10000000-0000-0000-0000-000000000001",
  "nextAcademicYearId": "20000000-0000-0000-0000-000000000002",
  "fromGradeId": "11111111-1111-1111-1111-111111111111",
  "toGradeId": "22222222-2222-2222-2222-222222222222"
}
```

### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Batch promotion executed successfully",
  "data": {
    "nextAcademicYearId": "20000000-0000-0000-0000-000000000002",
    "toGradeId": "22222222-2222-2222-2222-222222222222",
    "totalPromoted": 45,
    "status": "SUCCESS"
  }
}
```
{expand}

{expand:title=US-03006: Dual-Signoff Official Transfer Certificates}

### Endpoint
`POST /api/v1/students/{studentId}/certificates/transfer`

### Authorized Roles
`ROLE_SCHOOL_PRINCIPAL`, `ROLE_VICE_PRINCIPAL`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`

### Request Payload
```json
{
  "reasonForLeaving": "Parent job relocation to Bangalore",
  "conductRemarks": "Exemplary conduct and active sports participation",
  "secondSignOffUserId": "principal@kvs.local"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Transfer certificate issued successfully with dual sign-off",
  "data": {
    "certificateId": "5cc07f86-7939-6784-d5fe-4e185f88cac8",
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "studentUid": "STU2026000001",
    "studentName": "Aarav Sharma",
    "certificateType": "TRANSFER_CERTIFICATE",
    "certificateNumber": "TC-2026-100001",
    "authorizedByUserId": "registrar@kvs.local",
    "secondSignOffUserId": "principal@kvs.local",
    "verificationUrl": "https://verify.kvs.local/certificates/TC-2026-100001"
  }
}
```
{expand}

{expand:title=US-03007: Tamper-Evident Bonafide & Character Certificates}

### Endpoint
`POST /api/v1/students/{studentId}/certificates/bonafide`

### Authorized Roles
`ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_OFFICE_CLERK`, `ROLE_SCHOOL_PRINCIPAL`

### Request Payload
```json
{
  "certificateType": "BONAFIDE",
  "purpose": "Passport Application"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Certificate issued successfully with verification QR code",
  "data": {
    "certificateId": "6dd18f97-8a4a-7895-e6ff-5f296f99dbd9",
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "studentUid": "STU2026000001",
    "studentName": "Aarav Sharma",
    "certificateType": "BONAFIDE",
    "certificateNumber": "BON-2026-100002",
    "verificationUrl": "https://verify.kvs.local/certificates/BON-2026-100002"
  }
}
```
{expand}

{expand:title=US-03008: Disciplinary Incidents & Counseling Logs}

### Endpoint
`POST /api/v1/students/{studentId}/disciplinary`

### Authorized Roles
`ROLE_CLASS_TEACHER`, `ROLE_SCHOOL_PRINCIPAL`, `ROLE_VICE_PRINCIPAL`, `ROLE_SUPER_ADMIN`

### Request Payload
```json
{
  "incidentDate": "2026-09-10",
  "severity": "MODERATE",
  "incidentCategory": "Repeated classroom disruption",
  "description": "Disrupted physics lab session twice",
  "actionTaken": "Written reflection submitted and parent counseling scheduled",
  "counselingNotes": "Student advised on focus and team collaboration"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Disciplinary incident recorded successfully",
  "data": {
    "incidentId": "7ee29fa8-9b5b-89a6-f700-603a7000ece0",
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "studentUid": "STU2026000001",
    "severity": "MODERATE",
    "incidentCategory": "Repeated classroom disruption",
    "reportedByUserId": "teacher@kvs.local"
  }
}
```
{expand}

{expand:title=US-03009: Permanent Alumni Registry Archiving}

### Endpoint
`POST /api/v1/alumni/archive`

### Authorized Roles
`ROLE_ALUMNI_COORDINATOR`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`

### Request Payload
```json
{
  "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "graduationYear": 2026,
  "finalGradePassed": "Grade 12 - Science",
  "alumniEmail": "aarav.alumni@example.com",
  "alumniPhone": "+919876543210",
  "higherEducationInstitution": "Indian Institute of Technology, Bombay",
  "currentOccupation": "Undergraduate Student - Computer Science"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Graduating student archived to alumni registry successfully",
  "data": {
    "alumniId": "8ff3a0b9-ac6c-9ab7-0811-714b8111fdf1",
    "studentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "studentUid": "STU2026000001",
    "fullName": "Aarav Sharma",
    "graduationYear": 2026,
    "finalGradePassed": "Grade 12 - Science",
    "higherEducationInstitution": "Indian Institute of Technology, Bombay"
  }
}
```
{expand}


{expand:title=US-03010: Student Demographic Multi-Criteria Search & Filtering}

### Endpoint
`GET /api/v1/students/search`

### Authorized Roles
`ROLE_CLASS_TEACHER`, `ROLE_ACADEMIC_HEAD`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_OFFICE_CLERK`

### Query Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| `tenantId` | String | No | Target tenant ID |
| `branchId` | String | No | Target branch ID |
| `gradeId` | UUID | No | Grade identifier filter |
| `sectionId` | UUID | No | Section identifier filter |
| `houseId` | UUID | No | School house identifier filter |
| `transportRouteId` | String | No | Bus / Transport route code |
| `status` | String | No | `ACTIVE`, `PROMOTED`, `DETAINED`, `SUSPENDED`, `TRANSFERRED`, `GRADUATED` |

### Success Response (`200 OK`)
```json
{
  "success": true,
  "data": [
    {
      "studentId": "6eed0003-0001-4000-8000-000000000001",
      "studentUid": "STU2026000010",
      "firstName": "Rohan",
      "lastName": "Verma",
      "gradeName": "Grade 6",
      "sectionName": "Section A",
      "houseName": "Tagore",
      "transportRouteId": "ROUTE-12",
      "studentStatus": "ACTIVE",
      "isOnHold": false
    }
  ]
}
```
{expand}

{expand:title=US-03011: Bulk Contact & Residential Address Updates}

### Endpoint
`PUT /api/v1/students/bulk-contact-update`

### Authorized Roles
`ROLE_SCHOOL_NURSE`, `ROLE_SCHOOL_PRINCIPAL`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_OFFICE_CLERK`

### Request Payload
```json
{
  "items": [
    {
      "studentId": "6eed0003-0001-4000-8000-000000000001",
      "residentialAddress": "Flat 402, Sunshine Apartments, Delhi",
      "emergencyContactPhone": "+919876543210"
    }
  ]
}
```

### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Bulk contacts updated successfully",
  "data": 1
}
```
{expand}

{expand:title=US-03012: Extracurricular Activity Credits & House Points}

### Endpoints
- `POST /api/v1/students/{studentId}/activities` — Log student participation credits
- `GET /api/v1/students/{studentId}/activities` — List student activity credits
- `GET /api/v1/academic/houses/{houseId}/points` — Aggregate total points earned by a house

### Authorized Roles
- Logging: `ROLE_HOUSE_MASTER`, `ROLE_CLASS_TEACHER`, `ROLE_SUPER_ADMIN`, `ROLE_REGISTRAR`
- Querying: `ROLE_HOUSE_MASTER`, `ROLE_CLASS_TEACHER`, `ROLE_ACADEMIC_HEAD`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_SCHOOL_PRINCIPAL`

### Request Payload (`POST /api/v1/students/{studentId}/activities`)
```json
{
  "activityName": "Inter-School Debate Championship",
  "activityCategory": "Literary",
  "pointsAwarded": 15,
  "eventDate": "2026-10-15",
  "remarks": "First Runner-up in senior division"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Activity credit recorded successfully",
  "data": {
    "creditId": "e1f2a3b4-0001-4000-8000-000000000001",
    "studentId": "6eed0003-0001-4000-8000-000000000001",
    "studentUid": "STU2026000012",
    "studentName": "Ananya Sen",
    "activityName": "Inter-School Debate Championship",
    "activityCategory": "Literary",
    "pointsAwarded": 15,
    "eventDate": "2026-10-15",
    "awardedByUserId": "house_master@kvs.local"
  }
}
```
{expand}

{expand:title=US-03013: Alumni Credential Verification Engine}

### Endpoints
- `POST /api/v1/alumni/credential-verifications` — Submit third-party verification request
- `GET /api/v1/alumni/credential-verifications/{requestId}` — Get verification details
- `PATCH /api/v1/alumni/credential-verifications/{requestId}/verify` — Review & mark verified/rejected

### Authorized Roles
- Submit / View: `ROLE_ALUMNI_COORDINATOR`, `ROLE_OFFICE_CLERK`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`
- Review: `ROLE_ALUMNI_COORDINATOR`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`

### Request Payload (`POST /api/v1/alumni/credential-verifications`)
```json
{
  "studentUid": "STU2024000100",
  "requestorName": "Infosys Background Verification Team",
  "requestorOrganization": "Infosys Limited",
  "credentialType": "TRANSFER_CERTIFICATE",
  "remarks": "Employment onboarding credential verification"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Credential verification request submitted successfully",
  "data": {
    "requestId": "c1d2e3f4-0001-4000-8000-000000000001",
    "studentUid": "STU2024000100",
    "studentName": "Aarav Sharma",
    "requestorName": "Infosys Background Verification Team",
    "requestorOrganization": "Infosys Limited",
    "credentialType": "TRANSFER_CERTIFICATE",
    "status": "PENDING",
    "createdAt": "2026-10-18T06:00:00Z"
  }
}
```
{expand}

{expand:title=US-03014: Student Photo Management & Batch Image Export}

### Endpoints
- `POST /api/v1/students/{studentId}/photo` — Update single student profile photo
- `GET /api/v1/students/export-photo-directory` — Batch export student image directory by grade/section

### Authorized Roles
`ROLE_ACADEMIC_HEAD`, `ROLE_CLASS_TEACHER`, `ROLE_OFFICE_CLERK`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`

### Request Payload (`POST /api/v1/students/{studentId}/photo`)
```json
{
  "photoUrl": "https://storage.kvs.local/photos/stu2026000014.jpg"
}
```

### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Student photo updated successfully",
  "data": {
    "studentId": "6eed0003-0001-4000-8000-000000000001",
    "studentUid": "STU2026000014",
    "profilePhotoUrl": "https://storage.kvs.local/photos/stu2026000014.jpg"
  }
}
```
{expand}

{expand:title=US-03015: Administrative Holds Management}

### Endpoints
- `POST /api/v1/students/{studentId}/holds` — Place administrative hold
- `PATCH /api/v1/students/holds/{holdId}/release` — Release active administrative hold
- `GET /api/v1/students/{studentId}/holds` — List all administrative holds for a student

### Authorized Roles
- Place: `ROLE_SCHOOL_PRINCIPAL`, `ROLE_SCHOOL_NURSE`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`
- Release: `ROLE_SCHOOL_PRINCIPAL`, `ROLE_SCHOOL_NURSE`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`
- List: `ROLE_SCHOOL_PRINCIPAL`, `ROLE_SCHOOL_NURSE`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_OFFICE_CLERK`

### Request Payload (`POST /api/v1/students/{studentId}/holds`)
```json
{
  "holdType": "FEE_DEFAULT",
  "reason": "Term 2 tuition fee payment default exceeding 45 days"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Administrative hold placed successfully",
  "data": {
    "holdId": "b1a2c3d4-0001-4000-8000-000000000001",
    "studentId": "6eed0003-0001-4000-8000-000000000001",
    "studentUid": "STU2026000015",
    "holdType": "FEE_DEFAULT",
    "reason": "Term 2 tuition fee payment default exceeding 45 days",
    "isActive": true,
    "placedByUserId": "principal@kvs.local",
    "placedAt": "2026-10-18T06:10:00Z"
  }
}
```
{expand}

{expand:title=US-03016: Periodic Health Metrics Tracking}

### Endpoints
- `POST /api/v1/students/{studentId}/health-metrics` — Record annual health checkup metrics
- `GET /api/v1/students/{studentId}/health-metrics` — List historical health metric checkups

### Authorized Roles
`ROLE_SCHOOL_REGISTRAR`, `ROLE_REGISTRAR`, `ROLE_HOUSE_MASTER`, `ROLE_SCHOOL_NURSE`, `ROLE_SUPER_ADMIN`

### Request Payload (`POST /api/v1/students/{studentId}/health-metrics`)
```json
{
  "checkupDate": "2026-10-18",
  "heightCm": 142.50,
  "weightKg": 36.00,
  "visionLeft": "6/6",
  "visionRight": "6/6",
  "healthNotes": "Normal vitals and healthy BMI"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Health metric recorded successfully",
  "data": {
    "metricId": "h1e2a3l4-0001-4000-8000-000000000001",
    "studentId": "6eed0003-0001-4000-8000-000000000001",
    "studentUid": "STU2026000016",
    "checkupDate": "2026-10-18",
    "heightCm": 142.50,
    "weightKg": 36.00,
    "bmi": 17.73,
    "visionLeft": "6/6",
    "visionRight": "6/6",
    "checkedByUserId": "nurse@kvs.local"
  }
}
```
{expand}

{expand:title=US-03017: Language Preferences & Elective Course Selections}

### Endpoints
- `POST /api/v1/students/{studentId}/electives` — Record or update elective selections
- `GET /api/v1/students/{studentId}/electives` — Retrieve student elective selections

### Authorized Roles
`ROLE_OFFICE_CLERK`, `ROLE_ALUMNI_COORDINATOR`, `ROLE_CLASS_TEACHER`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`

### Request Payload (`POST /api/v1/students/{studentId}/electives`)
```json
{
  "academicYearId": "6eed0003-0002-4000-8000-000000000002",
  "firstLanguage": "English",
  "secondLanguage": "Hindi",
  "thirdLanguage": "Sanskrit",
  "electiveSubjectCode": "CS-101"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Elective selection recorded successfully",
  "data": {
    "selectionId": "e1l2e3c4-0001-4000-8000-000000000001",
    "studentId": "6eed0003-0001-4000-8000-000000000001",
    "studentUid": "STU2026000017",
    "academicYearId": "6eed0003-0002-4000-8000-000000000002",
    "firstLanguage": "English",
    "secondLanguage": "Hindi",
    "thirdLanguage": "Sanskrit",
    "electiveSubjectCode": "CS-101",
    "status": "CONFIRMED"
  }
}
```
{expand}

{expand:title=US-03018: Class Enrollment Roll Lists}

### Endpoint
`GET /api/v1/academic/sections/{sectionId}/roll-list`

### Authorized Roles
`ROLE_CLASS_TEACHER`, `ROLE_ACADEMIC_HEAD`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_OFFICE_CLERK`, `ROLE_SCHOOL_PRINCIPAL`

### Success Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "gradeId": "6eed0003-0001-4000-8000-000000000001",
    "gradeName": "Grade 6",
    "sectionId": "6eed0003-0002-4000-8000-000000000002",
    "sectionName": "Section A",
    "generatedDate": "2026-10-18",
    "totalActiveStudents": 42,
    "students": [
      {
        "studentId": "6eed0003-0001-4000-8000-000000000001",
        "studentUid": "STU2026000018",
        "rollNumber": 1,
        "fullName": "Aarush Das",
        "gender": "MALE",
        "bloodGroup": "O+",
        "houseName": "Tagore",
        "emergencyContactPhone": "+91-9876543211",
        "isOnHold": false
      }
    ]
  }
}
```
{expand}

{expand:title=US-03019: Section Transfer Approval Workflow}

### Endpoints
- `POST /api/v1/students/{studentId}/section-transfers` — Propose student section transfer
- `PATCH /api/v1/students/section-transfers/{transferId}/review` — Review transfer request
- `GET /api/v1/students/{studentId}/section-transfers` — List student section transfer history

### Authorized Roles
- Propose / List: `ROLE_SCHOOL_NURSE`, `ROLE_ACADEMIC_HEAD`, `ROLE_SCHOOL_PRINCIPAL`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`, `ROLE_CLASS_TEACHER`
- Review: `ROLE_ACADEMIC_HEAD`, `ROLE_SCHOOL_PRINCIPAL`, `ROLE_REGISTRAR`, `ROLE_SUPER_ADMIN`

### Request Payload (`POST /api/v1/students/{studentId}/section-transfers`)
```json
{
  "targetSectionId": "6eed0003-0002-4000-8000-000000000002",
  "reason": "Balancing section student strengths and language lab schedules"
}
```

### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Section transfer request submitted successfully",
  "data": {
    "transferId": "t1r2a3n4-0001-4000-8000-000000000001",
    "studentId": "6eed0003-0001-4000-8000-000000000001",
    "studentUid": "STU2026000019",
    "toSectionId": "6eed0003-0002-4000-8000-000000000002",
    "toSectionName": "Section B",
    "status": "PENDING",
    "reason": "Balancing section student strengths and language lab schedules",
    "createdAt": "2026-10-18T06:20:00Z"
  }
}
```
{expand}

---

## 4. Scenario Matrix & Role Access Decisions (US-03020 to US-03100)

Stories `US-03020` through `US-03100` execute the 20 fundamental capabilities across multi-scenario variations (Scenarios 2–6) rotating across organizational roles. Each row indicates whether the designated actor is authorized (granted: 2xx response) or restricted (not granted: 403 Forbidden).

| Story ID | Capability | Sub-Module Domain | Rotated Scenario Actor | Access Decision |
|---|---|---|---|---|
| **US-03020** | Permanent Student UID Generation | UID Master Engine | `House Master` | Restricted (403) |
| **US-03021** | Real-Time Medical Alerts & Emergency | Demographics & Medical Alerts | `Alumni Coordinator` | Restricted (403) |
| **US-03022** | Multi-Sibling Linking | Parent Accounts & Guardian Mapping | `Academic Head` | Restricted (403) |
| **US-03023** | Academic Section & House Allocation | Section & House Allocation | `School Principal` | Restricted (403) |
| **US-03024** | Barcoded ID Card Generation | ID Card Generation Engine | `School Registrar` | Granted (2xx) |
| **US-03025** | Annual Batch Promotions | Academic Session Batch Promotion | `Office Clerk` | Restricted (403) |
| **US-03026** | Dual Sign-off Transfer Certificates | Transfer Certificate Dual Sign-off | `Class Teacher` | Restricted (403) |
| **US-03027** | Bonafide & Character Certificates | Bonafide & Character Certificate | `School Nurse` | Restricted (403) |
| **US-03028** | Disciplinary Incidents & Counseling | Disciplinary Incident Logs | `House Master` | Restricted (403) |
| **US-03029** | Permanent Alumni Archiving | Permanent Alumni Registry | `Alumni Coordinator` | Granted (2xx) |
| **US-03030** | Demographic Multi-Criteria Filter | Demographic Filtering | `Academic Head` | Granted (2xx) |
| **US-03031** | Bulk Contact Update | Bulk Contact Updates | `School Principal` | Granted (2xx) |
| **US-03032** | Extracurricular Participation Credits | Extracurricular & House Points | `School Registrar` | Granted (2xx) |
| **US-03033** | Alumni Credential Verification | Historical Archive Verification | `Office Clerk` | Granted (2xx) |
| **US-03034** | Student Photo Management | Photo Updates & Directory Export | `Class Teacher` | Granted (2xx) |
| **US-03035** | Administrative Hold Management | Administrative Holds | `School Nurse` | Granted (2xx) |
| **US-03036** | Periodic Health Metrics Tracking | Health Metrics & BMI Tracking | `House Master` | Granted (2xx) |
| **US-03037** | Language Preferences & Electives | Language & Elective Selection | `Alumni Coordinator` | Granted (2xx) |
| **US-03038** | Class Enrollment Roll Lists | Attendance Roll List Generation | `Academic Head` | Granted (2xx) |
| **US-03039** | Section Transfer Workflow | Section Transfer Approval Workflow | `School Principal` | Granted (2xx) |
| **US-03040** | Permanent Student UID Generation | UID Master Engine | `School Registrar` | Granted (2xx) |
| **US-03041** | Real-Time Medical Alerts & Emergency | Demographics & Medical Alerts | `Office Clerk` | Restricted (403) |
| **US-03042** | Multi-Sibling Linking | Parent Accounts & Guardian Mapping | `Class Teacher` | Restricted (403) |
| **US-03043** | Academic Section & House Allocation | Section & House Allocation | `School Nurse` | Restricted (403) |
| **US-03044** | Barcoded ID Card Generation | ID Card Generation Engine | `House Master` | Restricted (403) |
| **US-03045** | Annual Batch Promotions | Academic Session Batch Promotion | `Alumni Coordinator` | Restricted (403) |
| **US-03046** | Dual Sign-off Transfer Certificates | Transfer Certificate Dual Sign-off | `Academic Head` | Restricted (403) |
| **US-03047** | Bonafide & Character Certificates | Bonafide & Character Certificate | `School Principal` | Granted (2xx) |
| **US-03048** | Disciplinary Incidents & Counseling | Disciplinary Incident Logs | `School Registrar` | Restricted (403) |
| **US-03049** | Permanent Alumni Archiving | Permanent Alumni Registry | `Office Clerk` | Restricted (403) |
| **US-03050** | Demographic Multi-Criteria Filter | Demographic Filtering | `Class Teacher` | Granted (2xx) |
| **US-03051** | Bulk Contact Update | Bulk Contact Updates | `School Nurse` | Granted (2xx) |
| **US-03052** | Extracurricular Participation Credits | Extracurricular & House Points | `House Master` | Granted (2xx) |
| **US-03053** | Alumni Credential Verification | Historical Archive Verification | `Alumni Coordinator` | Granted (2xx) |
| **US-03054** | Student Photo Management | Photo Updates & Directory Export | `Academic Head` | Granted (2xx) |
| **US-03055** | Administrative Hold Management | Administrative Holds | `School Principal` | Granted (2xx) |
| **US-03056** | Periodic Health Metrics Tracking | Health Metrics & BMI Tracking | `School Registrar` | Granted (2xx) |
| **US-03057** | Language Preferences & Electives | Language & Elective Selection | `Office Clerk` | Granted (2xx) |
| **US-03058** | Class Enrollment Roll Lists | Attendance Roll List Generation | `Class Teacher` | Granted (2xx) |
| **US-03059** | Section Transfer Workflow | Section Transfer Approval Workflow | `School Nurse` | Granted (2xx) |
| **US-03060** | Permanent Student UID Generation | UID Master Engine | `House Master` | Restricted (403) |
| **US-03061** | Real-Time Medical Alerts & Emergency | Demographics & Medical Alerts | `Alumni Coordinator` | Restricted (403) |
| **US-03062** | Multi-Sibling Linking | Parent Accounts & Guardian Mapping | `Academic Head` | Restricted (403) |
| **US-03063** | Academic Section & House Allocation | Section & House Allocation | `School Principal` | Restricted (403) |
| **US-03064** | Barcoded ID Card Generation | ID Card Generation Engine | `School Registrar` | Granted (2xx) |
| **US-03065** | Annual Batch Promotions | Academic Session Batch Promotion | `Office Clerk` | Restricted (403) |
| **US-03066** | Dual Sign-off Transfer Certificates | Transfer Certificate Dual Sign-off | `Class Teacher` | Restricted (403) |
| **US-03067** | Bonafide & Character Certificates | Bonafide & Character Certificate | `School Nurse` | Restricted (403) |
| **US-03068** | Disciplinary Incidents & Counseling | Disciplinary Incident Logs | `House Master` | Restricted (403) |
| **US-03069** | Permanent Alumni Archiving | Permanent Alumni Registry | `Alumni Coordinator` | Granted (2xx) |
| **US-03070** | Demographic Multi-Criteria Filter | Demographic Filtering | `Academic Head` | Granted (2xx) |
| **US-03071** | Bulk Contact Update | Bulk Contact Updates | `School Principal` | Granted (2xx) |
| **US-03072** | Extracurricular Participation Credits | Extracurricular & House Points | `School Registrar` | Granted (2xx) |
| **US-03073** | Alumni Credential Verification | Historical Archive Verification | `Office Clerk` | Granted (2xx) |
| **US-03074** | Student Photo Management | Photo Updates & Directory Export | `Class Teacher` | Granted (2xx) |
| **US-03075** | Administrative Hold Management | Administrative Holds | `School Nurse` | Granted (2xx) |
| **US-03076** | Periodic Health Metrics Tracking | Health Metrics & BMI Tracking | `House Master` | Granted (2xx) |
| **US-03077** | Language Preferences & Electives | Language & Elective Selection | `Alumni Coordinator` | Granted (2xx) |
| **US-03078** | Class Enrollment Roll Lists | Attendance Roll List Generation | `Academic Head` | Granted (2xx) |
| **US-03079** | Section Transfer Workflow | Section Transfer Approval Workflow | `School Principal` | Granted (2xx) |
| **US-03080** | Permanent Student UID Generation | UID Master Engine | `School Registrar` | Granted (2xx) |
| **US-03081** | Real-Time Medical Alerts & Emergency | Demographics & Medical Alerts | `Office Clerk` | Restricted (403) |
| **US-03082** | Multi-Sibling Linking | Parent Accounts & Guardian Mapping | `Class Teacher` | Restricted (403) |
| **US-03083** | Academic Section & House Allocation | Section & House Allocation | `School Nurse` | Restricted (403) |
| **US-03084** | Barcoded ID Card Generation | ID Card Generation Engine | `House Master` | Restricted (403) |
| **US-03085** | Annual Batch Promotions | Academic Session Batch Promotion | `Alumni Coordinator` | Restricted (403) |
| **US-03086** | Dual Sign-off Transfer Certificates | Transfer Certificate Dual Sign-off | `Academic Head` | Restricted (403) |
| **US-03087** | Bonafide & Character Certificates | Bonafide & Character Certificate | `School Principal` | Granted (2xx) |
| **US-03088** | Disciplinary Incidents & Counseling | Disciplinary Incident Logs | `School Registrar` | Restricted (403) |
| **US-03089** | Permanent Alumni Archiving | Permanent Alumni Registry | `Office Clerk` | Restricted (403) |
| **US-03090** | Demographic Multi-Criteria Filter | Demographic Filtering | `Class Teacher` | Granted (2xx) |
| **US-03091** | Bulk Contact Update | Bulk Contact Updates | `School Nurse` | Granted (2xx) |
| **US-03092** | Extracurricular Participation Credits | Extracurricular & House Points | `House Master` | Granted (2xx) |
| **US-03093** | Alumni Credential Verification | Historical Archive Verification | `Alumni Coordinator` | Granted (2xx) |
| **US-03094** | Student Photo Management | Photo Updates & Directory Export | `Academic Head` | Granted (2xx) |
| **US-03095** | Administrative Hold Management | Administrative Holds | `School Principal` | Granted (2xx) |
| **US-03096** | Periodic Health Metrics Tracking | Health Metrics & BMI Tracking | `School Registrar` | Granted (2xx) |
| **US-03097** | Language Preferences & Electives | Language & Elective Selection | `Office Clerk` | Granted (2xx) |
| **US-03098** | Class Enrollment Roll Lists | Attendance Roll List Generation | `Class Teacher` | Granted (2xx) |
| **US-03099** | Section Transfer Workflow | Section Transfer Approval Workflow | `School Nurse` | Granted (2xx) |
| **US-03100** | Permanent Student UID Generation | UID Master Engine | `House Master` | Restricted (403) |
