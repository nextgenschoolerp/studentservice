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
