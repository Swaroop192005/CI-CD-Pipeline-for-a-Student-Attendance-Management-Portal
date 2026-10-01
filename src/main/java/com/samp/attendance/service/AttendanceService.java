package com.samp.attendance.service;

import com.samp.attendance.domain.*;
import com.samp.attendance.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Attendance capture (US-06) and listing (US-07).
 *
 * <p>This is the transaction boundary and the only place that decides whether a
 * record may be created or changed. Controllers bind HTTP; they do not decide.
 */
@Service
public class AttendanceService {

    private final AttendanceRecordRepository records;
    private final EnrolmentRepository enrolments;
    private final CourseRepository courses;
    private final AppUserRepository users;

    public AttendanceService(AttendanceRecordRepository records,
                             EnrolmentRepository enrolments,
                             CourseRepository courses,
                             AppUserRepository users) {
        this.records = records;
        this.enrolments = enrolments;
        this.courses = courses;
        this.users = users;
    }

    public List<Course> allCourses() {
        return courses.findAll();
    }

    public Course requireCourse(Long courseId) {
        return courses.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("No such course: " + courseId));
    }

    /**
     * Builds the entry-screen roster for a course and date, pre-selecting any
     * values already recorded so that reopening the screen edits rather than
     * duplicates (AC-06.1, AC-06.3).
     */
    @Transactional(readOnly = true)
    public List<RosterEntry> roster(Long courseId, LocalDate sessionDate) {
        Course course = requireCourse(courseId);

        Map<Long, AttendanceRecord> existing = records
                .findByCourseAndSessionDate(course, sessionDate).stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), Function.identity()));

        return enrolments.findRosterForCourse(course).stream()
                .map(Enrolment::getStudent)
                .map(student -> {
                    AttendanceRecord record = existing.get(student.getId());
                    return record == null
                            ? RosterEntry.unrecorded(student)
                            : new RosterEntry(student, record.getStatus(), record.getWorkflowState(), true);
                })
                .toList();
    }

    /**
     * Creates or updates the records for one session.
     *
     * @return how many records were created or updated
     * @throws FutureSessionDateException if the session date is in the future (AC-06.4)
     */
    @Transactional
    public int saveSession(Long courseId, LocalDate sessionDate,
                           Map<Long, AttendanceStatus> statusByStudentId, String actorUsername) {

        if (sessionDate == null || sessionDate.isAfter(LocalDate.now())) {
            throw new FutureSessionDateException();
        }

        Course course = requireCourse(courseId);
        AppUser actor = users.findByUsername(actorUsername)
                .orElseThrow(() -> new IllegalStateException("Unknown actor: " + actorUsername));

        Map<Long, AttendanceRecord> existing = records
                .findByCourseAndSessionDate(course, sessionDate).stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), Function.identity()));

        int affected = 0;
        for (Enrolment enrolment : enrolments.findRosterForCourse(course)) {
            Student student = enrolment.getStudent();
            AttendanceStatus submitted = statusByStudentId.get(student.getId());
            if (submitted == null) {
                continue;
            }

            AttendanceRecord record = existing.get(student.getId());
            if (record == null) {
                records.save(new AttendanceRecord(student, course, sessionDate, submitted, actor));
                affected++;
            } else if (record.getWorkflowState().isEditableByFaculty()) {
                // Only DRAFT and REJECTED may be changed (AC-08.2); a SUBMITTED or
                // APPROVED record is left exactly as it is rather than silently reset.
                if (record.getStatus() != submitted) {
                    record.changeStatus(submitted, actor);
                }
                affected++;
            }
        }
        return affected;
    }

    @Transactional(readOnly = true)
    public Page<AttendanceRecord> list(Pageable pageable) {
        return records.findAllNewestFirst(pageable);
    }

    /** Thrown when a session date is in the future (AC-06.4). */
    public static class FutureSessionDateException extends RuntimeException {
        public FutureSessionDateException() {
            super("Session date cannot be in the future");
        }
    }
}
