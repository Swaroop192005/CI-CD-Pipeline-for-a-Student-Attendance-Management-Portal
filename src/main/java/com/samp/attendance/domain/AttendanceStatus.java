package com.samp.attendance.domain;

/** How a student was marked for one session. */
public enum AttendanceStatus {
    PRESENT, ABSENT, LATE;

    /**
     * Whether this status counts towards the attendance percentage.
     * LATE counts as attended: the student was in the room.
     */
    public boolean countsAsAttended() {
        return this == PRESENT || this == LATE;
    }
}
