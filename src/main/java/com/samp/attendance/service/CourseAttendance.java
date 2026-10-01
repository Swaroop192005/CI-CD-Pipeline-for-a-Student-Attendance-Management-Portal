package com.samp.attendance.service;

/**
 * One row of the dashboard or the student view: a student's attendance in a
 * course, computed from approved records only (§4.2 of the architecture).
 */
public record CourseAttendance(String rollNumber,
                               String studentName,
                               String courseCode,
                               String courseTitle,
                               long approvedSessions,
                               long attendedSessions,
                               Integer percentage,
                               boolean belowThreshold) {

    /** A student with no approved records shows "—", never 0%, so an empty
     *  record is not mistaken for a shortage. */
    public String displayPercentage() {
        return percentage == null ? "—" : percentage + "%";
    }
}
