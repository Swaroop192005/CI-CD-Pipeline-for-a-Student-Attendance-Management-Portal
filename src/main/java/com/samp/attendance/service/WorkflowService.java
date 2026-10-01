package com.samp.attendance.service;

import com.samp.attendance.domain.*;
import com.samp.attendance.repository.AppUserRepository;
import com.samp.attendance.repository.AttendanceRecordRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The role-based approval workflow (US-11 … US-14).
 *
 * <p>Every transition goes through here, and every one is checked twice: the role
 * by {@link PreAuthorize}, and the legality of the transition itself against
 * {@link WorkflowState}. AC-14.1 requires an illegal transition to be
 * <em>refused</em>, so neither check may live in a controller or a template — a
 * disabled button is not access control.
 */
@Service
public class WorkflowService {

    private final AttendanceRecordRepository records;
    private final AppUserRepository users;

    public WorkflowService(AttendanceRecordRepository records, AppUserRepository users) {
        this.records = records;
        this.users = users;
    }

    /** US-11 — a faculty member submits a draft for approval. */
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    @Transactional
    public void submit(Long recordId, String actorUsername) {
        transition(recordId, WorkflowState.SUBMITTED, actorUsername, null);
    }

    /** US-12 — only an ADMIN may approve. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void approve(Long recordId, String actorUsername) {
        transition(recordId, WorkflowState.APPROVED, actorUsername, null);
    }

    /** US-13 — only an ADMIN may reject, and a remark is mandatory (AC-13.2). */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void reject(Long recordId, String actorUsername, String remark) {
        if (remark == null || remark.isBlank()) {
            throw new RemarkRequiredException();
        }
        transition(recordId, WorkflowState.REJECTED, actorUsername, remark);
    }

    /** A rejected record goes back to DRAFT for rework. */
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    @Transactional
    public void revise(Long recordId, String actorUsername) {
        transition(recordId, WorkflowState.DRAFT, actorUsername, null);
    }

    private void transition(Long recordId, WorkflowState target, String actorUsername, String remark) {
        AttendanceRecord record = records.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("No such record: " + recordId));

        if (!record.getWorkflowState().canTransitionTo(target)) {
            throw new IllegalTransitionException(record.getWorkflowState(), target);
        }

        AppUser actor = users.findByUsername(actorUsername)
                .orElseThrow(() -> new IllegalStateException("Unknown actor: " + actorUsername));

        record.moveTo(target, actor, remark);
        records.save(record);
    }

    /** Thrown when a transition is not permitted by the state machine (AC-14.1). */
    public static class IllegalTransitionException extends RuntimeException {
        public IllegalTransitionException(WorkflowState from, WorkflowState to) {
            super("Cannot move an attendance record from " + from + " to " + to);
        }
    }

    /** Thrown when a rejection is attempted without a remark (AC-13.2). */
    public static class RemarkRequiredException extends RuntimeException {
        public RemarkRequiredException() {
            super("A remark is required when rejecting a record");
        }
    }
}
