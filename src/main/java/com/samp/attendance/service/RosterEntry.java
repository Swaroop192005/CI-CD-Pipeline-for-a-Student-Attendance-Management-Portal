package com.samp.attendance.service;

import com.samp.attendance.domain.AttendanceStatus;
import com.samp.attendance.domain.Student;
import com.samp.attendance.domain.WorkflowState;

/**
 * One row of the attendance entry screen: a student, plus whatever was already
 * recorded for them on this date (AC-06.3).
 */
public record RosterEntry(Student student,
                          AttendanceStatus status,
                          WorkflowState workflowState,
                          boolean alreadyRecorded) {

    public static RosterEntry unrecorded(Student student) {
        // AC-06.1: the selector defaults to PRESENT.
        return new RosterEntry(student, AttendanceStatus.PRESENT, null, false);
    }

    /** A record already approved or awaiting approval must not be silently overwritten. */
    public boolean isLocked() {
        return workflowState != null && !workflowState.isEditableByFaculty();
    }
}
