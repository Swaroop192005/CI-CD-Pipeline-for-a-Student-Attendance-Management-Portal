package com.samp.attendance.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The approval workflow state (US-11..US-14).
 *
 * <p>The permitted transitions live here rather than in a controller or a
 * template, because AC-14.1 requires an illegal transition to be <em>refused</em>,
 * not merely un-clickable. A hidden button is not access control.
 */
public enum WorkflowState {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED;

    static {
        DRAFT.allowed     = Collections.unmodifiableSet(EnumSet.of(SUBMITTED));
        SUBMITTED.allowed = Collections.unmodifiableSet(EnumSet.of(APPROVED, REJECTED));
        REJECTED.allowed  = Collections.unmodifiableSet(EnumSet.of(DRAFT));
        // APPROVED is terminal (AC-12.3): no transition out of it is permitted.
        APPROVED.allowed  = Collections.unmodifiableSet(EnumSet.noneOf(WorkflowState.class));
    }

    private Set<WorkflowState> allowed;

    public boolean canTransitionTo(WorkflowState target) {
        return allowed.contains(target);
    }

    public Set<WorkflowState> allowedTransitions() {
        return allowed;
    }

    /** A record may only be edited by its owning faculty member in these states (AC-08.2). */
    public boolean isEditableByFaculty() {
        return this == DRAFT || this == REJECTED;
    }
}
