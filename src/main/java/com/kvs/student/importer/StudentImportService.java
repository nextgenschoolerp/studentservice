package com.kvs.student.importer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kvs.core.exception.BadRequestException;
import com.kvs.core.exception.ResourceNotFoundException;
import com.kvs.core.util.SecurityUtils;
import com.kvs.student.dto.EnrollStudentRequest;
import com.kvs.student.dto.StudentResponse;
import com.kvs.student.dto.UpdateStudentProfileRequest;
import com.kvs.student.entity.Grade;
import com.kvs.student.entity.House;
import com.kvs.student.entity.Section;
import com.kvs.student.entity.StudentGuardian;
import com.kvs.student.entity.StudentMaster;
import com.kvs.student.enums.Gender;
import com.kvs.student.enums.StudentStatus;
import com.kvs.student.importer.ImportJobResponse.RowError;
import com.kvs.student.importer.ImportJobResponse.RowResult;
import com.kvs.student.importer.Spreadsheet.Column;
import com.kvs.student.repo.GradeRepository;
import com.kvs.student.repo.HouseRepository;
import com.kvs.student.repo.SectionRepository;
import com.kvs.student.repo.StudentGuardianRepository;
import com.kvs.student.repo.StudentMasterRepository;
import com.kvs.student.service.StudentAuditService;
import com.kvs.student.service.StudentFileService;
import com.kvs.student.service.StudentService;
import com.kvs.student.service.TenantAccessGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bulk student import (Settings → Data import): template, check (nothing saved), import, history and error report.
 * Each new student goes through {@link StudentService#enrollStudent}, so UIDs, the primary guardian and the audit log
 * are exactly as for a student added on the form.
 */
@Service
@Slf4j
public class StudentImportService {

    static final String VALID = "VALID", ERROR = "ERROR", CREATED = "CREATED", UPDATED = "UPDATED", UNCHANGED = "UNCHANGED", FAILED = "FAILED";
    static final String CREATE = "CREATE", UPDATE = "UPDATE", PHOTOS = "PHOTOS";
    private static final DateTimeFormatter SHOWN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Duration EXPIRY = Duration.ofHours(24);
    private static final List<String> BLOOD = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private static final List<String> RELATIONS = List.of("Father", "Mother", "Guardian", "Grandparent", "Uncle", "Aunt", "Sibling", "Other");
    private static final List<String> GENDERS = List.of("Male", "Female", "Other");

    @Autowired private GradeRepository gradeRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private HouseRepository houseRepository;
    @Autowired private StudentMasterRepository studentRepository;
    @Autowired private StudentImportJobRepository jobRepository;
    @Autowired private StudentService studentService;
    @Autowired private StudentGuardianRepository guardianRepository;
    @Autowired private StudentFileService fileService;
    @Autowired private StudentAuditService auditService;
    @Autowired private TenantAccessGuard tenantGuard;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PlatformTransactionManager transactionManager;

    static String mode(String mode) {
        String m = mode == null || mode.isBlank() ? CREATE : mode.trim().toUpperCase(Locale.ROOT);
        if (!CREATE.equals(m) && !UPDATE.equals(m)) throw new BadRequestException("Unknown import type " + mode + ": use CREATE or UPDATE", "IMPORT_MODE");
        return m;
    }

    /** CREATE: the columns of the template. UPDATE: the same columns after a mandatory Student UID, none of them mandatory (blank keeps the current value). */
    List<Column> columns(String tenantId, String mode) {
        List<Column> base = baseColumns(tenantId);
        if (!UPDATE.equals(mode)) return base;
        List<Column> cols = new ArrayList<>();
        cols.add(new Column("Student UID", "studentUid", true, "The student's UID, e.g. STU-2026-0001 (Students list and profile). Says which student to update", null));
        base.forEach(c -> cols.add(new Column(c.header(), c.key(), false, c.help() + ". Leave blank to keep the current value", c.choices())));
        return cols;
    }

    /** The columns of the template, in order; the dropdowns use this school's classes, sections and houses. */
    private List<Column> baseColumns(String tenantId) {
        List<Grade> grades = gradeRepository.findByTenantIdOrderBySequenceOrderAsc(tenantId);
        List<String> sections = grades.stream().flatMap(g -> sectionRepository.findByGradeId(g.getGradeId()).stream())
                .map(Section::getSectionName).distinct().sorted().toList();
        List<String> houses = houseRepository.findByTenantId(tenantId).stream().map(House::getHouseName).sorted().toList();
        return List.of(
                new Column("First name", "firstName", true, "Given name, up to 100 characters", null),
                new Column("Last name", "lastName", true, "Family name, up to 100 characters", null),
                new Column("Date of birth", "dateOfBirth", true, "DD/MM/YYYY (or YYYY-MM-DD); must suit the class", null),
                new Column("Gender", "gender", true, "Male, Female or Other", GENDERS),
                new Column("Class", "className", true, "A class set up for the school, e.g. Class 6 or Nursery", grades.stream().map(Grade::getGradeName).toList()),
                new Column("Section", "section", true, "Section letter of that class, e.g. A", sections),
                new Column("Roll number", "rollNumber", false, "Whole number, unique within the section", null),
                new Column("House", "house", false, "One of the school houses", houses),
                new Column("Admission number", "admissionNumber", false, "The school's own admission number, up to 50 characters", null),
                new Column("Admission date", "admissionDate", false, "DD/MM/YYYY, not in the future", null),
                new Column("Nationality", "nationality", false, "e.g. Indian", null),
                new Column("Blood group", "bloodGroup", false, "A+, A-, B+, B-, AB+, AB-, O+ or O-", BLOOD),
                new Column("Medical conditions", "medicalAlerts", false, "Conditions staff must know about, or leave blank", null),
                new Column("Allergies", "allergies", false, "Known allergies, or leave blank", null),
                new Column("Residential address", "address", false, "Home address", null),
                new Column("Guardian name", "guardianName", true, "Primary guardian's full name", null),
                new Column("Guardian relationship", "guardianRelationship", true, String.join(", ", RELATIONS), RELATIONS),
                new Column("Guardian mobile", "guardianMobile", true, "10-digit Indian mobile number, with or without +91", null),
                new Column("Guardian email", "guardianEmail", false, "Email for school messages", null),
                new Column("Emergency phone", "emergencyPhone", true, "10-digit mobile number to call in an emergency", null));
    }

    /** The template; for UPDATE with prefill it already holds the campus's current students, to correct and upload again. */
    public byte[] template(String tenantId, String branchId, String format, String mode, boolean prefill) {
        tenantGuard.requireTenant(tenantId);
        String m = mode(mode);
        boolean update = UPDATE.equals(m);
        List<Column> cols = columns(tenantId, m);
        List<List<String>> current = update && prefill ? currentStudents(tenantId, branchId, cols) : List.of();
        if ("csv".equalsIgnoreCase(format)) return Spreadsheet.csv(cols.stream().map(Column::label).toList(), current);
        Map<String, String> ex = new HashMap<>(Map.ofEntries(Map.entry("studentUid", "STU-2026-0001"), Map.entry("firstName", "Aarav"), Map.entry("lastName", "Sharma"),
                Map.entry("dateOfBirth", "14/03/2015"), Map.entry("gender", "Male"), Map.entry("className", "Class 6"), Map.entry("section", "A"), Map.entry("rollNumber", "12"),
                Map.entry("house", "Tagore"), Map.entry("admissionNumber", "ADM-2026-0145"), Map.entry("admissionDate", "01/04/2026"), Map.entry("nationality", "Indian"),
                Map.entry("bloodGroup", "B+"), Map.entry("medicalAlerts", "None"), Map.entry("allergies", "Peanuts"), Map.entry("address", "12 Rose Avenue, New Delhi 110017"),
                Map.entry("guardianName", "Rajesh Sharma"), Map.entry("guardianRelationship", "Father"), Map.entry("guardianMobile", "9810012301"),
                Map.entry("guardianEmail", "rajesh.sharma@example.com"), Map.entry("emergencyPhone", "9810012302")));
        if (update) { // an update example changes just two things; everything else stays blank
            ex.replaceAll((k, v) -> k.equals("studentUid") || k.equals("guardianMobile") || k.equals("house") ? v : "");
            ex.put("house", "Raman");
        }
        List<String> example = cols.stream().map(c -> ex.getOrDefault(c.key(), "")).toList();
        List<String> notes = update ? List.of(
                "Student update template",
                "1. Each row updates ONE existing student, found by the Student UID in the first column. Rows are numbered from row 2.",
                "2. Fill only the cells you want to change. A blank cell keeps the current value; it never clears it.",
                "3. Easiest: use 'Download current students' in Settings → Data import. The sheet then holds today's values; change what you need and upload it.",
                "4. Class and Section cannot be changed here (use Transfer on the student's profile); leave them as they are or blank.",
                "5. Dates: DD/MM/YYYY. Mobile numbers: 10 digits; +91 is added automatically. Guardian columns change the primary guardian.",
                "6. At most " + Spreadsheet.MAX_ROWS + " students per file, from the campus selected in the app. Check the file first; nothing is saved until you choose Import.")
            : List.of(
                "Student bulk import template",
                "1. Fill one student per row on the Students sheet, starting on row 2. Do not rename or remove the header row.",
                "2. Columns marked * (shaded yellow) are mandatory. Use the dropdowns where a column has one.",
                "3. Dates: DD/MM/YYYY. Mobile numbers: 10 digits; +91 is added automatically.",
                "4. Save as .xlsx (or CSV, UTF-8) and upload it in Settings → Data import. Check the file first; nothing is saved until you choose Import.",
                "5. At most " + Spreadsheet.MAX_ROWS + " students per file. Students are added to the campus selected in the app.");
        return Spreadsheet.template("Students", cols, example, notes, current);
    }

    /** The campus's active students in template layout: class order, then section, roll number and name. */
    private List<List<String>> currentStudents(String tenantId, String branchId, List<Column> cols) {
        List<Grade> grades = gradeRepository.findByTenantIdOrderBySequenceOrderAsc(tenantId);
        Map<UUID, Grade> gradeById = new HashMap<>();
        Map<UUID, Section> sectionById = new HashMap<>();
        grades.forEach(g -> { gradeById.put(g.getGradeId(), g); sectionRepository.findByGradeId(g.getGradeId()).forEach(s -> sectionById.put(s.getSectionId(), s)); });
        Map<UUID, House> houseById = new HashMap<>();
        houseRepository.findByTenantId(tenantId).forEach(h -> houseById.put(h.getHouseId(), h));
        List<StudentMaster> students = studentRepository.findByTenantIdAndBranchId(tenantId, branchId).stream()
                .filter(s -> s.getStudentStatus() == StudentStatus.ACTIVE).limit(Spreadsheet.MAX_ROWS).toList();
        Map<UUID, StudentGuardian> primary = new HashMap<>();
        guardianRepository.findByStudentIdIn(students.stream().map(StudentMaster::getStudentId).toList()).stream()
                .sorted(Comparator.comparing((StudentGuardian g) -> !Boolean.TRUE.equals(g.getIsPrimaryContact())))
                .forEach(g -> primary.putIfAbsent(g.getStudentId(), g));
        Comparator<StudentMaster> order = Comparator
                .comparing((StudentMaster s) -> Optional.ofNullable(gradeById.get(s.getGradeId())).map(Grade::getSequenceOrder).orElse(0))
                .thenComparing(s -> Optional.ofNullable(sectionById.get(s.getSectionId())).map(Section::getSectionName).orElse(""))
                .thenComparing(s -> Optional.ofNullable(s.getRollNumber()).orElse(0))
                .thenComparing(StudentMaster::getFirstName);
        return students.stream().sorted(order).map(s -> {
            StudentGuardian g = primary.get(s.getStudentId());
            Map<String, String> v = new HashMap<>();
            v.put("studentUid", s.getStudentUid()); v.put("firstName", s.getFirstName()); v.put("lastName", s.getLastName());
            v.put("dateOfBirth", s.getDateOfBirth() == null ? "" : s.getDateOfBirth().format(SHOWN_DATE));
            v.put("gender", s.getGender() == null ? "" : s.getGender().name().charAt(0) + s.getGender().name().substring(1).toLowerCase(Locale.ROOT));
            v.put("className", Optional.ofNullable(gradeById.get(s.getGradeId())).map(Grade::getGradeName).orElse(""));
            v.put("section", Optional.ofNullable(sectionById.get(s.getSectionId())).map(Section::getSectionName).orElse(""));
            v.put("rollNumber", s.getRollNumber() == null ? "" : String.valueOf(s.getRollNumber()));
            v.put("house", Optional.ofNullable(houseById.get(s.getHouseId())).map(House::getHouseName).orElse(""));
            v.put("admissionNumber", nz(s.getAdmissionNumber()));
            v.put("admissionDate", s.getAdmissionDate() == null ? "" : s.getAdmissionDate().format(SHOWN_DATE));
            v.put("nationality", nz(s.getNationality())); v.put("bloodGroup", nz(s.getBloodGroup())); v.put("medicalAlerts", nz(s.getMedicalAlerts()));
            v.put("allergies", nz(s.getAllergies())); v.put("address", nz(s.getResidentialAddress())); v.put("emergencyPhone", nz(s.getEmergencyContactPhone()));
            if (g != null) { v.put("guardianName", nz(g.getFullName())); v.put("guardianRelationship", nz(g.getRelationship())); v.put("guardianMobile", nz(g.getMobileNumber())); v.put("guardianEmail", nz(g.getEmail())); }
            return cols.stream().map(c -> v.getOrDefault(c.key(), "")).toList();
        }).toList();
    }

    /** Reads and checks the file; nothing is saved except the checked job, which Import uses within 24 hours. */
    public ImportJobResponse check(String tenantId, String branchId, MultipartFile file, String mode) {
        tenantGuard.requireTenant(tenantId);
        if (branchId == null || branchId.isBlank()) throw new BadRequestException("Choose a campus before importing", "BRANCH_REQUIRED");
        String m = mode(mode);
        List<Column> cols = columns(tenantId, m);
        List<Spreadsheet.Row> rows = Spreadsheet.read(file, cols);
        List<RowResult> results = UPDATE.equals(m) ? validateUpdate(tenantId, branchId, rows, cols) : validate(tenantId, rows, cols);
        int errors = (int) results.stream().filter(r -> ERROR.equals(r.getStatus())).count();
        int ready = (int) results.stream().filter(r -> VALID.equals(r.getStatus())).count();
        StudentImportJob job = jobRepository.save(StudentImportJob.builder()
                .tenantId(tenantId).branchId(branchId).fileName(file.getOriginalFilename()).status("VALIDATED").mode(m)
                .totalRows(results.size()).validRows(ready).errorRows(errors)
                .rowsJson(write(results)).build());
        auditService.logSuccess("student-import:" + job.getJobId(), "CHECK_STUDENT_IMPORT",
                "Checked " + results.size() + " rows from " + file.getOriginalFilename() + ": " + errors + " with errors");
        return toResponse(job, results);
    }

    /** Creates the students of a checked upload. ALL_OR_NOTHING refuses when any row has errors and saves all rows in one transaction. */
    public ImportJobResponse commit(UUID jobId, String mode) {
        StudentImportJob job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("StudentImportJob", "jobId", jobId));
        tenantGuard.requireTenantRecord(job.getTenantId(), "StudentImportJob", "jobId", jobId);
        if (!"VALIDATED".equals(job.getStatus())) throw new BadRequestException("This upload has already been imported", "IMPORT_ALREADY_DONE");
        if (job.getCreatedAt() != null && job.getCreatedAt().plus(EXPIRY).isBefore(Instant.now())) {
            job.setStatus("EXPIRED");
            jobRepository.save(job);
            throw new BadRequestException("This check is older than 24 hours. Upload the file again.", "IMPORT_EXPIRED");
        }
        boolean allOrNothing = "ALL_OR_NOTHING".equalsIgnoreCase(mode);
        // check again: the school's data may have changed since the file was checked
        boolean update = UPDATE.equals(job.getMode());
        List<Column> cols = columns(job.getTenantId(), job.getMode());
        List<RowResult> stored = read(job.getRowsJson());
        List<Spreadsheet.Row> rows = stored.stream().map(r -> new Spreadsheet.Row(r.getRowNumber(), r.getValues())).toList();
        List<RowResult> results = update ? validateUpdate(job.getTenantId(), job.getBranchId(), rows, cols) : validate(job.getTenantId(), rows, cols);
        long errors = results.stream().filter(r -> ERROR.equals(r.getStatus())).count();
        java.util.function.BiConsumer<StudentImportJob, RowResult> apply = update ? this::update : this::create;
        if (allOrNothing && errors > 0) {
            throw new BadRequestException(errors + " row" + (errors == 1 ? " has" : "s have") + " errors. Fix them and check the file again, or import only the valid rows.", "IMPORT_HAS_ERRORS");
        }
        if (results.stream().noneMatch(r -> VALID.equals(r.getStatus()))) throw new BadRequestException(update ? "No row has a change to save" : "No row is ready to import", "IMPORT_NOTHING_VALID");

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        if (allOrNothing) {
            tx.executeWithoutResult(s -> results.stream().filter(r -> VALID.equals(r.getStatus())).forEach(r -> apply.accept(job, r)));
        } else {
            for (RowResult r : results) {
                if (!VALID.equals(r.getStatus())) continue;
                try {
                    tx.executeWithoutResult(s -> apply.accept(job, r));
                } catch (RuntimeException e) {
                    log.warn("Import row {} failed: {}", r.getRowNumber(), e.getMessage());
                    r.setStatus(FAILED);
                    r.setRecordId(null);
                    r.setRecordCode(null);
                    r.getErrors().add(new RowError(null, "Could not be saved: " + e.getMessage()));
                }
            }
        }
        int done = (int) results.stream().filter(r -> CREATED.equals(r.getStatus()) || UPDATED.equals(r.getStatus())).count();
        // keep the values only of rows that still need fixing (the error report); saved rows keep their summary
        results.stream().filter(r -> CREATED.equals(r.getStatus()) || UPDATED.equals(r.getStatus())).forEach(r -> r.setValues(null));
        job.setStatus("COMMITTED");
        job.setCreatedCount(update ? 0 : done);
        job.setUpdatedCount(update ? done : 0);
        job.setErrorRows((int) results.stream().filter(r -> ERROR.equals(r.getStatus()) || FAILED.equals(r.getStatus())).count());
        job.setValidRows((int) results.stream().filter(r -> !ERROR.equals(r.getStatus()) && !UNCHANGED.equals(r.getStatus())).count());
        job.setCommittedAt(Instant.now());
        job.setCommittedBy(currentUser());
        job.setRowsJson(write(results));
        jobRepository.save(job);
        auditService.logSuccess("student-import:" + job.getJobId(), "COMMIT_STUDENT_IMPORT",
                (update ? "Updated " : "Imported ") + done + " of " + results.size() + " students from " + job.getFileName());
        return toResponse(job, results);
    }

    private void create(StudentImportJob job, RowResult r) {
        Map<String, String> v = r.getValues();
        EnrollStudentRequest req = EnrollStudentRequest.builder()
                .tenantId(job.getTenantId()).branchId(job.getBranchId())
                .firstName(v.get("firstName")).lastName(v.get("lastName"))
                .dateOfBirth(ImportRules.date(v.get("dateOfBirth"))).gender(gender(v.get("gender")))
                .bloodGroup(blood(v.get("bloodGroup"))).medicalAlerts(Optional.ofNullable(v.get("medicalAlerts")).orElse("None"))
                .emergencyContactPhone(ImportRules.mobile(v.get("emergencyPhone")))
                .gradeId(r.getGradeId()).sectionId(r.getSectionId()).houseId(r.getHouseId()).rollNumber(ImportRules.positive(v.get("rollNumber")))
                .guardianName(v.get("guardianName")).guardianRelationship(relation(v.get("guardianRelationship")))
                .guardianPhone(ImportRules.mobile(v.get("guardianMobile"))).guardianEmail(v.get("guardianEmail"))
                .build();
        StudentResponse saved = studentService.enrollStudent(req);
        // details the enrolment request does not carry
        StudentMaster s = studentRepository.findByStudentId(saved.getStudentId()).orElseThrow();
        s.setAdmissionNumber(v.get("admissionNumber"));
        s.setAdmissionDate(ImportRules.date(v.get("admissionDate")));
        s.setNationality(v.get("nationality"));
        s.setAllergies(v.get("allergies"));
        s.setResidentialAddress(v.get("address"));
        studentRepository.save(s);
        r.setStatus(CREATED);
        r.setRecordId(saved.getStudentId().toString());
        r.setRecordCode(saved.getStudentUid());
    }

    /** Applies one checked UPDATE row: only the cells that were filled change; blank cells keep what is on record. */
    private void update(StudentImportJob job, RowResult r) {
        Map<String, String> v = r.getValues();
        UUID id = UUID.fromString(r.getRecordId());
        StudentMaster s = studentRepository.findByStudentId(id).orElseThrow(() -> new ResourceNotFoundException("Student", "studentId", id));
        // the profile fields go through the service behind the Edit student form
        studentService.updateProfile(id, UpdateStudentProfileRequest.builder()
                .firstName(or(v.get("firstName"), s.getFirstName())).lastName(or(v.get("lastName"), s.getLastName()))
                .preferredName(s.getPreferredName())
                .dateOfBirth(has(v, "dateOfBirth") ? ImportRules.date(v.get("dateOfBirth")) : s.getDateOfBirth())
                .gender(has(v, "gender") ? gender(v.get("gender")) : s.getGender())
                .nationality(or(v.get("nationality"), s.getNationality()))
                .admissionDate(has(v, "admissionDate") ? ImportRules.date(v.get("admissionDate")) : s.getAdmissionDate())
                .admissionNumber(or(v.get("admissionNumber"), s.getAdmissionNumber()))
                .residentialAddress(or(v.get("address"), s.getResidentialAddress()))
                .medicationInstructions(s.getMedicationInstructions()).gpContact(s.getGpContact()).collectionPermission(s.getCollectionPermission())
                .build());
        s = studentRepository.findByStudentId(id).orElseThrow();
        if (has(v, "bloodGroup")) s.setBloodGroup(blood(v.get("bloodGroup")));
        if (has(v, "medicalAlerts")) s.setMedicalAlerts(v.get("medicalAlerts"));
        if (has(v, "allergies")) s.setAllergies(v.get("allergies"));
        if (has(v, "emergencyPhone")) s.setEmergencyContactPhone(ImportRules.mobile(v.get("emergencyPhone")));
        if (r.getHouseId() != null) s.setHouseId(r.getHouseId());
        if (has(v, "rollNumber")) s.setRollNumber(ImportRules.positive(v.get("rollNumber")));
        studentRepository.save(s);

        if (has(v, "guardianName") || has(v, "guardianRelationship") || has(v, "guardianMobile") || has(v, "guardianEmail")) {
            StudentGuardian g = primaryGuardian(id);
            if (g == null) g = StudentGuardian.builder().studentId(id).build();
            g.setFullName(or(v.get("guardianName"), g.getFullName()));
            g.setRelationship(has(v, "guardianRelationship") ? relation(v.get("guardianRelationship")) : g.getRelationship());
            g.setMobileNumber(has(v, "guardianMobile") ? ImportRules.mobile(v.get("guardianMobile")) : g.getMobileNumber());
            g.setEmail(or(v.get("guardianEmail"), g.getEmail()));
            g.setIsPrimaryContact(true);
            guardianRepository.save(g);
        }
        auditService.logSuccess("student:" + id, "UPDATE_STUDENT_IMPORT", "Updated by bulk import from " + job.getFileName() + ": " + r.getSummary().get("changes"));
        r.setStatus(UPDATED);
    }

    private StudentGuardian primaryGuardian(UUID studentId) {
        List<StudentGuardian> all = guardianRepository.findByStudentId(studentId);
        return all.stream().filter(g -> Boolean.TRUE.equals(g.getIsPrimaryContact())).findFirst().orElse(all.isEmpty() ? null : all.get(0));
    }

    /**
     * Photos from a ZIP, each file named after the student's UID (STU-2026-0001.jpg). dryRun only reports what would happen;
     * otherwise the matched photos replace the students' current ones and the upload is kept in the history.
     */
    public ImportJobResponse photos(String tenantId, String branchId, MultipartFile zip, boolean dryRun) {
        tenantGuard.requireTenant(tenantId);
        List<PhotoZip.Entry> entries = PhotoZip.read(zip);
        Map<String, StudentMaster> byUid = new HashMap<>();
        studentRepository.findByTenantId(tenantId).forEach(s -> byUid.put(s.getStudentUid().toLowerCase(Locale.ROOT), s));
        Map<String, Integer> seen = new HashMap<>();
        List<RowResult> results = new ArrayList<>();
        Map<Integer, PhotoZip.Entry> byRow = new HashMap<>();
        int n = 0;
        for (PhotoZip.Entry e : entries) {
            n++;
            List<RowError> errs = new ArrayList<>();
            StudentMaster s = byUid.get(e.key());
            if (!e.extension().matches("jpe?g|png")) errs.add(new RowError(null, "Only JPG or PNG photos are accepted"));
            else if (e.tooBig()) errs.add(new RowError(null, "The photo is larger than 5 MB"));
            else if (e.imageType() == null) errs.add(new RowError(null, "The file is not a valid JPEG or PNG image"));
            if (s == null) errs.add(new RowError(null, "No student has the UID \"" + e.key().toUpperCase(Locale.ROOT) + "\"; name the file after the Student UID"));
            Integer first = seen.putIfAbsent(e.key(), n);
            if (first != null) errs.add(new RowError(null, "Repeats the photo in row " + first));
            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("name", s == null ? "—" : s.getFirstName() + " " + s.getLastName());
            summary.put("file", e.fileName());
            RowResult row = RowResult.builder().rowNumber(n).status(errs.isEmpty() ? VALID : ERROR).summary(summary).errors(errs).build();
            if (s != null) { row.setRecordId(s.getStudentId().toString()); row.setRecordCode(s.getStudentUid()); }
            results.add(row);
            byRow.put(n, e);
        }
        if (!dryRun) {
            for (RowResult row : results) {
                if (!VALID.equals(row.getStatus())) continue;
                PhotoZip.Entry e = byRow.get(row.getRowNumber());
                try {
                    fileService.uploadPhoto(UUID.fromString(row.getRecordId()), new BytesMultipartFile(e.fileName(), e.imageType(), e.bytes()));
                    row.setStatus(UPDATED);
                } catch (RuntimeException ex) {
                    log.warn("Photo {} failed: {}", e.fileName(), ex.getMessage());
                    row.setStatus(FAILED);
                    row.getErrors().add(new RowError(null, "Could not be saved: " + ex.getMessage()));
                }
            }
        }
        int saved = (int) results.stream().filter(r -> UPDATED.equals(r.getStatus())).count();
        int bad = (int) results.stream().filter(r -> ERROR.equals(r.getStatus()) || FAILED.equals(r.getStatus())).count();
        int ready = (int) results.stream().filter(r -> VALID.equals(r.getStatus())).count();
        if (dryRun) {
            return ImportJobResponse.builder().fileName(zip.getOriginalFilename()).branchId(branchId).status("VALIDATED").mode(PHOTOS)
                    .totalRows(results.size()).validRows(ready).errorRows(bad).rows(results).build();
        }
        StudentImportJob job = jobRepository.save(StudentImportJob.builder().tenantId(tenantId).branchId(branchId).fileName(zip.getOriginalFilename())
                .status("COMMITTED").mode(PHOTOS).totalRows(results.size()).validRows(saved).errorRows(bad).updatedCount(saved)
                .committedAt(Instant.now()).committedBy(currentUser()).rowsJson(write(results)).build());
        auditService.logSuccess("student-import:" + job.getJobId(), "IMPORT_STUDENT_PHOTOS", "Saved " + saved + " of " + results.size() + " photos from " + zip.getOriginalFilename());
        return toResponse(job, results);
    }

    public List<ImportJobResponse> history(String tenantId) {
        tenantGuard.requireTenant(tenantId);
        return jobRepository.findTop50ByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(j -> toResponse(j, null)).toList();
    }

    public ImportJobResponse get(UUID jobId) {
        StudentImportJob job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("StudentImportJob", "jobId", jobId));
        tenantGuard.requireTenantRecord(job.getTenantId(), "StudentImportJob", "jobId", jobId);
        return toResponse(job, read(job.getRowsJson()));
    }

    /** The rows that need fixing, as in the file, with a Row and an Errors column; upload it again once fixed. */
    public byte[] errorReport(UUID jobId) {
        StudentImportJob job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("StudentImportJob", "jobId", jobId));
        tenantGuard.requireTenantRecord(job.getTenantId(), "StudentImportJob", "jobId", jobId);
        if (PHOTOS.equals(job.getMode())) {
            List<List<String>> lines = read(job.getRowsJson()).stream().filter(r -> ERROR.equals(r.getStatus()) || FAILED.equals(r.getStatus()))
                    .map(r -> List.of(String.valueOf(r.getRowNumber()), r.getSummary().getOrDefault("file", ""),
                            r.getErrors().stream().map(RowError::getMessage).collect(Collectors.joining("; ")))).toList();
            return Spreadsheet.csv(List.of("Row", "File", "Problems"), lines);
        }
        List<Column> cols = columns(job.getTenantId(), job.getMode());
        List<String> header = new ArrayList<>(List.of("Row"));
        cols.forEach(c -> header.add(c.label()));
        header.add("Errors");
        List<List<String>> lines = read(job.getRowsJson()).stream().filter(r -> ERROR.equals(r.getStatus()) || FAILED.equals(r.getStatus())).map(r -> {
            List<String> line = new ArrayList<>(List.of(String.valueOf(r.getRowNumber())));
            Map<String, String> v = Optional.ofNullable(r.getValues()).orElse(Map.of());
            cols.forEach(c -> line.add(v.getOrDefault(c.key(), "")));
            line.add(r.getErrors().stream().map(e -> (e.getColumn() == null ? "" : e.getColumn() + ": ") + e.getMessage()).collect(Collectors.joining("; ")));
            return line;
        }).toList();
        return Spreadsheet.csv(header, lines);
    }

    /* ---------- checks ---------- */

    private List<RowResult> validate(String tenantId, List<Spreadsheet.Row> rows, List<Column> cols) {
        Map<String, String> headers = cols.stream().collect(Collectors.toMap(Column::key, Column::header));
        List<Grade> grades = gradeRepository.findByTenantIdOrderBySequenceOrderAsc(tenantId);
        Map<String, House> houses = new HashMap<>();
        houseRepository.findByTenantId(tenantId).forEach(h -> houses.put(h.getHouseName().toLowerCase(Locale.ROOT), h));
        Map<UUID, List<Section>> sectionsByGrade = new HashMap<>();
        Map<UUID, Long> taken = new HashMap<>();          // students already in a section, plus rows of this file going there
        Map<UUID, Set<Integer>> rolls = new HashMap<>();  // roll numbers in use per section
        Map<String, String> existing = new HashMap<>();   // name + date of birth → Student UID, for the duplicate check
        studentRepository.findByTenantId(tenantId).forEach(s -> existing.put(personKey(s.getFirstName(), s.getLastName(), s.getDateOfBirth()), s.getStudentUid()));
        Map<String, Integer> inFile = new HashMap<>();

        List<RowResult> out = new ArrayList<>();
        for (Spreadsheet.Row row : rows) {
            Map<String, String> v = row.values();
            List<RowError> errs = new ArrayList<>();
            java.util.function.BiConsumer<String, String> err = (key, msg) -> errs.add(new RowError(headers.getOrDefault(key, key), msg));
            cols.stream().filter(Column::required).filter(c -> isBlank(v.get(c.key()))).forEach(c -> err.accept(c.key(), "is required"));

            LocalDate dob = checkFields(v, err);

            // class, section, house, roll number and capacity
            Grade grade = isBlank(v.get("className")) ? null : grade(grades, v.get("className"));
            Section section = null;
            if (!isBlank(v.get("className")) && grade == null) err.accept("className", "\"" + v.get("className") + "\" is not a class of this school");
            if (grade != null && !isBlank(v.get("section"))) {
                List<Section> secs = sectionsByGrade.computeIfAbsent(grade.getGradeId(), sectionRepository::findByGradeId);
                section = secs.stream().filter(s -> s.getSectionName().equalsIgnoreCase(v.get("section"))).findFirst().orElse(null);
                if (section == null) err.accept("section", "Section " + v.get("section") + " is not set up for " + grade.getGradeName());
            }
            House house = isBlank(v.get("house")) ? null : houses.get(v.get("house").toLowerCase(Locale.ROOT));
            if (!isBlank(v.get("house")) && house == null) err.accept("house", "\"" + v.get("house") + "\" is not a house of this school");
            if (grade != null && dob != null) {
                int age = Period.between(dob, LocalDate.now()).getYears();
                int expected = expectedAge(grade);
                if (expected > 0 && Math.abs(age - expected) > 3) err.accept("dateOfBirth", "age " + age + " does not suit " + grade.getGradeName());
            }
            Integer roll = isBlank(v.get("rollNumber")) ? null : ImportRules.positive(v.get("rollNumber"));
            if (!isBlank(v.get("rollNumber")) && roll == null) err.accept("rollNumber", "must be a whole number above 0");
            if (section != null) {
                final Section sec = section;
                Set<Integer> used = rolls.computeIfAbsent(sec.getSectionId(), id -> studentRepository.findBySectionIdOrderByRollNumberAsc(id).stream()
                        .map(StudentMaster::getRollNumber).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new)));
                if (roll != null && !used.add(roll)) err.accept("rollNumber", "Roll number " + roll + " is already used in " + grade.getGradeName() + "-" + sec.getSectionName());
                long count = taken.computeIfAbsent(sec.getSectionId(), id -> studentRepository.countByGradeIdAndSectionId(sec.getGradeId(), id)) + 1;
                taken.put(sec.getSectionId(), count);
                int cap = Optional.ofNullable(sec.getMaxCapacity()).orElse(40);
                if (count > cap) err.accept("section", grade.getGradeName() + "-" + sec.getSectionName() + " is full (" + cap + " students)");
            }

            // duplicates: in this file and already enrolled
            if (dob != null && !isBlank(v.get("firstName")) && !isBlank(v.get("lastName"))) {
                String key = personKey(v.get("firstName"), v.get("lastName"), dob);
                if (existing.containsKey(key)) errs.add(new RowError(null, "Duplicate: already enrolled as " + existing.get(key)));
                Integer first = inFile.putIfAbsent(key, row.rowNumber());
                if (first != null) errs.add(new RowError(null, "Duplicate of row " + first + " in this file"));
            }

            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("name", (v.getOrDefault("firstName", "") + " " + v.getOrDefault("lastName", "")).trim());
            summary.put("class", grade != null && section != null ? grade.getGradeName() + "-" + section.getSectionName()
                    : (v.getOrDefault("className", "") + " " + v.getOrDefault("section", "")).trim());
            RowResult r = RowResult.builder().rowNumber(row.rowNumber()).status(errs.isEmpty() ? VALID : ERROR).summary(summary)
                    .errors(errs).values(new LinkedHashMap<>(v)).build();
            if (errs.isEmpty()) { r.setGradeId(grade.getGradeId()); r.setSectionId(section.getSectionId()); r.setHouseId(house == null ? null : house.getHouseId()); }
            out.add(r);
        }
        return out;
    }

    /** The format checks shared by new and updated rows; only filled cells are checked. Returns the parsed date of birth, if valid. */
    private LocalDate checkFields(Map<String, String> v, java.util.function.BiConsumer<String, String> err) {
        length(v, "firstName", 100, err);
        length(v, "lastName", 100, err);
        length(v, "admissionNumber", 50, err);
        length(v, "nationality", 60, err);
        length(v, "allergies", 500, err);
        LocalDate dob = null;
        if (!isBlank(v.get("dateOfBirth"))) {
            dob = ImportRules.date(v.get("dateOfBirth"));
            if (dob == null) err.accept("dateOfBirth", "is not a valid date; use DD/MM/YYYY, e.g. 14/03/2015");
            else if (!dob.isBefore(LocalDate.now())) err.accept("dateOfBirth", "must be in the past");
        }
        if (!isBlank(v.get("gender")) && gender(v.get("gender")) == null) err.accept("gender", "use Male, Female or Other");
        if (!isBlank(v.get("bloodGroup")) && blood(v.get("bloodGroup")) == null) err.accept("bloodGroup", "use A+, A-, B+, B-, AB+, AB-, O+ or O-");
        if (!isBlank(v.get("guardianRelationship")) && relation(v.get("guardianRelationship")) == null) err.accept("guardianRelationship", "use " + String.join(", ", RELATIONS));
        if (!isBlank(v.get("guardianMobile")) && ImportRules.mobile(v.get("guardianMobile")) == null) err.accept("guardianMobile", "enter a 10-digit Indian mobile number");
        if (!isBlank(v.get("emergencyPhone")) && ImportRules.mobile(v.get("emergencyPhone")) == null) err.accept("emergencyPhone", "enter a 10-digit Indian mobile number");
        if (!isBlank(v.get("guardianEmail")) && !ImportRules.email(v.get("guardianEmail"))) err.accept("guardianEmail", "is not a valid email address");
        if (!isBlank(v.get("admissionDate"))) {
            LocalDate ad = ImportRules.date(v.get("admissionDate"));
            if (ad == null) err.accept("admissionDate", "is not a valid date; use DD/MM/YYYY");
            else if (ad.isAfter(LocalDate.now())) err.accept("admissionDate", "cannot be in the future");
        }
        return dob;
    }

    /** Checks UPDATE rows: the student is found by UID; filled cells are checked and compared with what is on record. */
    private List<RowResult> validateUpdate(String tenantId, String branchId, List<Spreadsheet.Row> rows, List<Column> cols) {
        Map<String, String> headers = cols.stream().collect(Collectors.toMap(Column::key, Column::header));
        List<Grade> grades = gradeRepository.findByTenantIdOrderBySequenceOrderAsc(tenantId);
        Map<String, House> houses = new HashMap<>();
        houseRepository.findByTenantId(tenantId).forEach(h -> houses.put(h.getHouseName().toLowerCase(Locale.ROOT), h));
        Map<UUID, Grade> gradeById = new HashMap<>();
        Map<UUID, Section> sectionById = new HashMap<>();
        grades.forEach(g -> { gradeById.put(g.getGradeId(), g); sectionRepository.findByGradeId(g.getGradeId()).forEach(s -> sectionById.put(s.getSectionId(), s)); });
        Map<String, StudentMaster> byUid = new HashMap<>();
        Map<String, String> people = new HashMap<>();     // name + date of birth → Student UID
        studentRepository.findByTenantId(tenantId).forEach(s -> {
            byUid.put(s.getStudentUid().toLowerCase(Locale.ROOT), s);
            people.put(personKey(s.getFirstName(), s.getLastName(), s.getDateOfBirth()), s.getStudentUid());
        });
        Map<UUID, Set<Integer>> rolls = new HashMap<>();
        Map<String, Integer> uidInFile = new HashMap<>();

        List<RowResult> out = new ArrayList<>();
        for (Spreadsheet.Row row : rows) {
            Map<String, String> v = row.values();
            List<RowError> errs = new ArrayList<>();
            java.util.function.BiConsumer<String, String> err = (key, msg) -> errs.add(new RowError(headers.getOrDefault(key, key), msg));
            List<String> changes = new ArrayList<>();
            StudentMaster s = null;
            String uid = v.get("studentUid");
            if (isBlank(uid)) err.accept("studentUid", "is required");
            else {
                s = byUid.get(uid.toLowerCase(Locale.ROOT));
                if (s == null) err.accept("studentUid", "No student has the UID " + uid);
                else {
                    if (!branchId.equals(s.getBranchId())) err.accept("studentUid", uid + " is on campus " + s.getBranchId() + "; choose that campus to update this student");
                    Integer first = uidInFile.putIfAbsent(uid.toLowerCase(Locale.ROOT), row.rowNumber());
                    if (first != null) err.accept("studentUid", "repeats row " + first + " of this file");
                }
            }
            LocalDate dob = checkFields(v, err);
            House house = null;
            String classLabel = "";
            if (s != null) {
                Grade curGrade = gradeById.get(s.getGradeId());
                Section curSection = sectionById.get(s.getSectionId());
                classLabel = curGrade == null ? "" : curGrade.getGradeName() + (curSection == null ? "" : "-" + curSection.getSectionName());
                if (!isBlank(v.get("className"))) {
                    Grade g = grade(grades, v.get("className"));
                    if (g == null) err.accept("className", "\"" + v.get("className") + "\" is not a class of this school");
                    else if (!g.getGradeId().equals(s.getGradeId())) err.accept("className", "cannot be changed by import (now " + (curGrade == null ? "—" : curGrade.getGradeName()) + "); use Transfer on the student's profile");
                }
                if (!isBlank(v.get("section")) && curSection != null && !curSection.getSectionName().equalsIgnoreCase(v.get("section"))) {
                    err.accept("section", "cannot be changed by import (now " + curSection.getSectionName() + "); use Transfer on the student's profile");
                }
                if (!isBlank(v.get("house"))) {
                    house = houses.get(v.get("house").toLowerCase(Locale.ROOT));
                    if (house == null) err.accept("house", "\"" + v.get("house") + "\" is not a house of this school");
                    else if (!house.getHouseId().equals(s.getHouseId())) changes.add("House");
                }
                if (!isBlank(v.get("rollNumber"))) {
                    Integer roll = ImportRules.positive(v.get("rollNumber"));
                    if (roll == null) err.accept("rollNumber", "must be a whole number above 0");
                    else if (!roll.equals(s.getRollNumber())) {
                        changes.add("Roll number");
                        if (s.getSectionId() != null) {
                            Set<Integer> used = rolls.computeIfAbsent(s.getSectionId(), id -> studentRepository.findBySectionIdOrderByRollNumberAsc(id).stream()
                                    .map(StudentMaster::getRollNumber).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new)));
                            if (!used.add(roll)) err.accept("rollNumber", "Roll number " + roll + " is already used in " + classLabel);
                        }
                    }
                }
                changed(changes, "First name", v.get("firstName"), s.getFirstName());
                changed(changes, "Last name", v.get("lastName"), s.getLastName());
                if (dob != null && !dob.equals(s.getDateOfBirth())) changes.add("Date of birth");
                if (!isBlank(v.get("gender")) && gender(v.get("gender")) != null && gender(v.get("gender")) != s.getGender()) changes.add("Gender");
                changed(changes, "Admission number", v.get("admissionNumber"), s.getAdmissionNumber());
                LocalDate admitted = isBlank(v.get("admissionDate")) ? null : ImportRules.date(v.get("admissionDate"));
                if (admitted != null && !admitted.equals(s.getAdmissionDate())) changes.add("Admission date");
                changed(changes, "Nationality", v.get("nationality"), s.getNationality());
                String blood = isBlank(v.get("bloodGroup")) ? null : blood(v.get("bloodGroup"));
                if (blood != null && !blood.equals(s.getBloodGroup())) changes.add("Blood group");
                changed(changes, "Medical conditions", v.get("medicalAlerts"), s.getMedicalAlerts());
                changed(changes, "Allergies", v.get("allergies"), s.getAllergies());
                changed(changes, "Address", v.get("address"), s.getResidentialAddress());
                String emergency = isBlank(v.get("emergencyPhone")) ? null : ImportRules.mobile(v.get("emergencyPhone"));
                if (emergency != null && !emergency.equals(ImportRules.mobile(s.getEmergencyContactPhone()))) changes.add("Emergency phone");

                boolean anyGuardian = Stream.of("guardianName", "guardianRelationship", "guardianMobile", "guardianEmail").anyMatch(k -> !isBlank(v.get(k)));
                if (anyGuardian) {
                    StudentGuardian g = primaryGuardian(s.getStudentId());
                    if (g == null) {
                        if (Stream.of("guardianName", "guardianRelationship", "guardianMobile").anyMatch(k -> isBlank(v.get(k)))) {
                            errs.add(new RowError(null, "No guardian is on record: give the guardian's name, relationship and mobile together"));
                        } else changes.add("Guardian (new)");
                    } else {
                        changed(changes, "Guardian name", v.get("guardianName"), g.getFullName());
                        String rel = isBlank(v.get("guardianRelationship")) ? null : relation(v.get("guardianRelationship"));
                        if (rel != null && !rel.equals(g.getRelationship())) changes.add("Guardian relationship");
                        String mobile = isBlank(v.get("guardianMobile")) ? null : ImportRules.mobile(v.get("guardianMobile"));
                        if (mobile != null && !mobile.equals(ImportRules.mobile(g.getMobileNumber()))) changes.add("Guardian mobile");
                        changed(changes, "Guardian email", v.get("guardianEmail"), g.getEmail());
                    }
                }
                // a changed name or date of birth must not make this student a copy of another
                LocalDate newDob = dob != null ? dob : s.getDateOfBirth();
                String owner = people.get(personKey(or(v.get("firstName"), s.getFirstName()), or(v.get("lastName"), s.getLastName()), newDob));
                if (owner != null && !owner.equalsIgnoreCase(s.getStudentUid())) errs.add(new RowError(null, "Duplicate: another student, " + owner + ", has this name and date of birth"));
            }
            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("name", s == null ? (uid == null ? "" : uid) : (or(v.get("firstName"), s.getFirstName()) + " " + or(v.get("lastName"), s.getLastName())).trim());
            summary.put("class", classLabel);
            summary.put("changes", changes.isEmpty() ? "Nothing to change" : String.join(", ", changes));
            RowResult r = RowResult.builder().rowNumber(row.rowNumber()).status(!errs.isEmpty() ? ERROR : changes.isEmpty() ? UNCHANGED : VALID)
                    .summary(summary).errors(errs).values(new LinkedHashMap<>(v)).build();
            if (s != null) { r.setRecordId(s.getStudentId().toString()); r.setRecordCode(s.getStudentUid()); r.setHouseId(house == null ? null : house.getHouseId()); }
            out.add(r);
        }
        return out;
    }

    /** Adds the label when the cell is filled and differs from the value on record (ignoring case and extra spaces). */
    private static void changed(List<String> changes, String label, String given, String current) {
        if (isBlank(given)) return;
        String a = given.trim().replaceAll("\\s+", " ");
        String b = current == null ? "" : current.trim().replaceAll("\\s+", " ");
        if (!a.equalsIgnoreCase(b)) changes.add(label);
    }

    private static boolean has(Map<String, String> v, String key) { return !isBlank(v.get(key)); }

    private static String or(String given, String current) { return isBlank(given) ? current : given.trim(); }

    private static String nz(String s) { return s == null ? "" : s; }

    private static Grade grade(List<Grade> grades, String name) {
        String n = name.trim();
        String alt = n.matches("\\d+") ? "Class " + n : n;
        return grades.stream().filter(g -> g.getGradeName().equalsIgnoreCase(n) || g.getGradeName().equalsIgnoreCase(alt)
                || (g.getGradeCode() != null && g.getGradeCode().equalsIgnoreCase(n))).findFirst().orElse(null);
    }

    /** Usual age at the start of the year: Nursery 3, LKG 4, UKG 5, Class n → n + 5; 0 when the class is not one of these. */
    private static int expectedAge(Grade g) {
        String n = g.getGradeName().toLowerCase(Locale.ROOT);
        if (n.startsWith("nursery")) return 3;
        if (n.equals("lkg")) return 4;
        if (n.equals("ukg")) return 5;
        if (n.matches("class \\d+")) return Integer.parseInt(n.substring(6)) + 5;
        return 0;
    }

    private static Gender gender(String v) {
        if (v == null) return null;
        return switch (v.trim().toLowerCase(Locale.ROOT)) {
            case "male", "m", "boy" -> Gender.MALE;
            case "female", "f", "girl" -> Gender.FEMALE;
            case "other", "o", "prefer not to say", "non-binary" -> Gender.OTHER;
            default -> null;
        };
    }

    private static String blood(String v) {
        if (v == null) return null;
        String b = v.replace(" ", "").toUpperCase(Locale.ROOT).replace("VE", "").replace("POS", "+").replace("NEG", "-");
        return BLOOD.contains(b) ? b : null;
    }

    private static String relation(String v) {
        if (v == null) return null;
        return RELATIONS.stream().filter(r -> r.equalsIgnoreCase(v.trim())).findFirst().orElse(null);
    }

    private static void length(Map<String, String> v, String key, int max, java.util.function.BiConsumer<String, String> err) {
        if (v.get(key) != null && v.get(key).length() > max) err.accept(key, "is longer than " + max + " characters");
    }

    private static String personKey(String first, String last, LocalDate dob) {
        return (first + "|" + last).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ") + "|" + dob;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String currentUser() {
        return SecurityUtils.getAuthentication().map(Authentication::getName).orElse(null);
    }

    /* ---------- JSON and responses ---------- */

    private String write(List<RowResult> rows) {
        try { return objectMapper.writeValueAsString(rows); }
        catch (Exception e) { throw new IllegalStateException("Could not store the checked rows", e); }
    }

    private List<RowResult> read(String json) {
        if (json == null) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<List<RowResult>>() { }); }
        catch (Exception e) { throw new IllegalStateException("Could not read the checked rows", e); }
    }

    private ImportJobResponse toResponse(StudentImportJob j, List<RowResult> rows) {
        return ImportJobResponse.builder().jobId(j.getJobId()).fileName(j.getFileName()).branchId(j.getBranchId()).status(j.getStatus())
                .mode(j.getMode()).totalRows(j.getTotalRows()).validRows(j.getValidRows()).errorRows(j.getErrorRows()).createdCount(j.getCreatedCount()).updatedCount(j.getUpdatedCount())
                .createdBy(j.getCreatedBy()).createdAt(j.getCreatedAt()).committedBy(j.getCommittedBy()).committedAt(j.getCommittedAt())
                .rows(rows).build();
    }
}
