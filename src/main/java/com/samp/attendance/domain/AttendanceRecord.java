package com.samp.attendance.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One student's attendance for one session of one course.
 *
 * <p>The unique constraint on (student, course, session_date) is what enforces
 * AC-06.3 — reopening the entry screen for a date that already has records must
 * update them, never create a second set.
 */
@Entity
@Table(name = "attendance_record",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_record_student_course_date",
           columnNames = {"student_id", "course_id", "session_date"}))
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AttendanceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_state", nullable = false, length = 10)
    private WorkflowState workflowState = WorkflowState.DRAFT;

    @Column(length = 500)
    private String remark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private AppUser recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private AppUser updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AttendanceRecord() { }

    public AttendanceRecord(Student student, Course course, LocalDate sessionDate,
                            AttendanceStatus status, AppUser recordedBy) {
        this.student = student;
        this.course = course;
        this.sessionDate = sessionDate;
        this.status = status;
        this.recordedBy = recordedBy;
        this.updatedBy = recordedBy;
        this.workflowState = WorkflowState.DRAFT;
        this.updatedAt = Instant.now();
    }

    /** Records who changed the record and when, satisfying the audit requirement (FR-18). */
    public void changeStatus(AttendanceStatus newStatus, AppUser actor) {
        this.status = newStatus;
        touch(actor);
    }

    public void moveTo(WorkflowState target, AppUser actor, String remark) {
        this.workflowState = target;
        this.remark = remark;
        touch(actor);
    }

    private void touch(AppUser actor) {
        this.updatedBy = actor;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public LocalDate getSessionDate() { return sessionDate; }
    public AttendanceStatus getStatus() { return status; }
    public WorkflowState getWorkflowState() { return workflowState; }
    public String getRemark() { return remark; }
    public AppUser getRecordedBy() { return recordedBy; }
    public AppUser getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
