package com.samp.attendance.service;

import com.samp.attendance.domain.AttendanceRecord;
import com.samp.attendance.domain.WorkflowState;
import com.samp.attendance.repository.AttendanceRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Summary dashboard (US-15, US-16) and the student's own view (US-10).
 *
 * <p>Percentages are computed from <strong>approved records only</strong>: the
 * whole point of the workflow is that an unverified draft must never move the
 * official number.
 */
@Service
public class DashboardService {

    private final AttendanceRecordRepository records;
    private final int thresholdPercent;

    public DashboardService(AttendanceRecordRepository records,
                            @Value("${samp.attendance.minimum-percent}") int thresholdPercent) {
        this.records = records;
        this.thresholdPercent = thresholdPercent;
    }

    public int thresholdPercent() {
        return thresholdPercent;
    }

    /** Counts per workflow state, for the dashboard tiles (AC-15.2). */
    @Transactional(readOnly = true)
    public Map<WorkflowState, Long> countsByState() {
        Map<WorkflowState, Long> counts = new EnumMap<>(WorkflowState.class);
        for (WorkflowState state : WorkflowState.values()) {
            counts.put(state, records.countByWorkflowState(state));
        }
        return counts;
    }

    /** Attendance percentage per student per course (AC-15.1, AC-15.3). */
    @Transactional(readOnly = true)
    public List<CourseAttendance> summary() {
        return aggregate(records.findApproved());
    }

    /** The same aggregation scoped to one student (US-10). */
    @Transactional(readOnly = true)
    public List<CourseAttendance> summaryForStudent(String rollNumber) {
        return aggregate(records.findByStudentRollNumber(rollNumber).stream()
                .filter(r -> r.getWorkflowState() == WorkflowState.APPROVED)
                .toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecord> recordsForStudent(String rollNumber) {
        return records.findByStudentRollNumber(rollNumber);
    }

    private List<CourseAttendance> aggregate(List<AttendanceRecord> approved) {
        // Key is (rollNumber, courseCode); LinkedHashMap keeps the query's ordering.
        Map<String, List<AttendanceRecord>> grouped = new LinkedHashMap<>();
        for (AttendanceRecord r : approved) {
            grouped.computeIfAbsent(
                    r.getStudent().getRollNumber() + "\u0000" + r.getCourse().getCode(),
                    k -> new ArrayList<>()).add(r);
        }

        List<CourseAttendance> rows = new ArrayList<>();
        for (List<AttendanceRecord> group : grouped.values()) {
            AttendanceRecord first = group.get(0);
            long total = group.size();
            long attended = group.stream().filter(r -> r.getStatus().countsAsAttended()).count();

            // total is never 0 here: a group only exists because it has records.
            int percentage = (int) Math.round(100.0 * attended / total);

            rows.add(new CourseAttendance(
                    first.getStudent().getRollNumber(),
                    first.getStudent().getFullName(),
                    first.getCourse().getCode(),
                    first.getCourse().getTitle(),
                    total, attended, percentage,
                    percentage < thresholdPercent));
        }
        return rows;
    }
}
