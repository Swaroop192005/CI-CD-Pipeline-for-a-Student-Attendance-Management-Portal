package com.samp.attendance.domain;

/** The three roles the portal recognises (Task 1, §3.1). */
public enum Role {
    ADMIN, FACULTY, STUDENT;

    /** Spring Security expects authorities to carry the {@code ROLE_} prefix. */
    public String authority() {
        return "ROLE_" + name();
    }
}
