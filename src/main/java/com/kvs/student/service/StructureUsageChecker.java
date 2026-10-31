package com.kvs.student.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Finds what still points at a class, section or house before it is deleted. The services share one database, so every
 * table of the schema with a matching column (section_id, grade_id, house_id …) is counted: students, attendance,
 * timetables, subject allocations, homework, marks, fee schedules, admissions and so on.
 */
@Component
@Slf4j
public class StructureUsageChecker {

    /** Readable names for the tables that usually hold references; others are shown by their table name. */
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("student_master", "students"), Map.entry("student_master_records", "admission records"),
            Map.entry("student_attendance", "attendance marks"), Map.entry("attendance_registers", "attendance registers"),
            Map.entry("section_transfer_requests", "section transfer requests"), Map.entry("student_activity_credits", "house activity credits"),
            Map.entry("master_timetables", "timetable periods"), Map.entry("subject_allocations", "subject allocations"),
            Map.entry("homework_assignments", "homework"), Map.entry("lesson_plans", "lesson plans"), Map.entry("syllabus_topics", "syllabus topics"),
            Map.entry("syllabus_coverage", "syllabus coverage"), Map.entry("lms_quizzes", "quizzes"), Map.entry("learning_assets", "learning resources"),
            Map.entry("lab_reservations", "lab bookings"), Map.entry("remedial_sessions", "remedial sessions"), Map.entry("skill_badges", "skill badges"),
            Map.entry("marks_entry_register", "marks entries"), Map.entry("grade_lock_tickets", "marks unlock tickets"),
            Map.entry("exam_term_subjects", "exam subjects"), Map.entry("fee_schedules", "fee schedules"),
            Map.entry("admission_enquiries", "admission applications"), Map.entry("merit_lists", "merit lists"),
            Map.entry("entrance_exam_schedules", "entrance exams"), Map.entry("waitlist_entries", "waitlist entries"),
            Map.entry("waitlist_rules", "waitlist rules"), Map.entry("archived_lead_snapshots", "archived applications"));

    @Autowired
    private JdbcTemplate jdbc;

    /** Rows that refer to the id, by readable name; tables in {@code own} (the record's own table) are skipped. */
    public Map<String, Long> usage(UUID id, Set<String> columns, Set<String> own) {
        String in = columns.stream().map(c -> "'" + c + "'").collect(Collectors.joining(", "));
        List<Map<String, Object>> refs = jdbc.queryForList(
                "SELECT table_name, column_name FROM information_schema.columns WHERE table_schema = current_schema() AND column_name IN (" + in + ")");
        Map<String, Long> found = new LinkedHashMap<>();
        for (Map<String, Object> ref : refs) {
            String table = String.valueOf(ref.get("table_name"));
            String column = String.valueOf(ref.get("column_name"));
            if (own.contains(table) || !table.matches("[a-z0-9_]+") || !column.matches("[a-z0-9_]+")) continue;
            try {
                Long n = jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + column + "::text = ?", Long.class, id.toString());
                if (n != null && n > 0) found.merge(LABELS.getOrDefault(table, table.replace('_', ' ')), n, Long::sum);
            } catch (RuntimeException e) {
                log.warn("Could not check {}.{} for {}: {}", table, column, id, e.getMessage());
            }
        }
        return found;
    }

    /** "students (4), attendance marks (120) and timetable periods (6)" */
    public static String describe(Map<String, Long> uses) {
        // students first, then the rest by count: "students (16), attendance marks (344) and timetable periods (124)"
        List<String> parts = uses.entrySet().stream()
                .sorted((x, y) -> x.getKey().equals("students") ? -1 : y.getKey().equals("students") ? 1 : Long.compare(y.getValue(), x.getValue()))
                .map(e -> e.getKey() + " (" + e.getValue() + ")").toList();
        if (parts.size() <= 1) return String.join("", parts);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }
}
