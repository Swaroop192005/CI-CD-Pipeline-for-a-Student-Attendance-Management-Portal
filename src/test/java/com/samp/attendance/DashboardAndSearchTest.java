package com.samp.attendance;

import com.samp.attendance.domain.*;
import com.samp.attendance.repository.AttendanceRecordRepository;
import com.samp.attendance.repository.CourseRepository;
import com.samp.attendance.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Search (US-09) and the summary dashboard (US-15, US-16). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dash;DB_CLOSE_DELAY=-1",
        "samp.attendance.minimum-percent=75"})
@Transactional
class DashboardAndSearchTest {

    @Autowired AttendanceService attendance;
    @Autowired WorkflowService workflow;
    @Autowired DashboardService dashboard;
    @Autowired AttendanceRecordRepository records;
    @Autowired CourseRepository courses;

    private Long courseId;
    private final LocalDate day1 = LocalDate.now().minusDays(4);
    private final LocalDate day2 = LocalDate.now().minusDays(3);

    @BeforeEach
    @WithMockUser(username = "admin", roles = "ADMIN")
    void seedTwoApprovedSessions() {
        courseId = courses.findByCode("CS301").orElseThrow().getId();

        // Day 1: everyone present. Day 2: 22CS003 absent.
        attendance.saveSession(courseId, day1, allPresent(day1), "faculty1");
        Map<Long, AttendanceStatus> day2Statuses = allPresent(day2);
        Long absentee = attendance.roster(courseId, day2).stream()
                .filter(e -> e.student().getRollNumber().equals("22CS003"))
                .findFirst().orElseThrow().student().getId();
        day2Statuses.put(absentee, AttendanceStatus.ABSENT);
        attendance.saveSession(courseId, day2, day2Statuses, "faculty1");

        // Approve everything so it counts towards the percentages.
        for (AttendanceRecord r : records.findAll()) {
            r.moveTo(WorkflowState.SUBMITTED, r.getRecordedBy(), null);
            r.moveTo(WorkflowState.APPROVED, r.getRecordedBy(), null);
            records.save(r);
        }
    }

    @Test
    @DisplayName("AC-15.1 — percentage per student per course, from approved records only")
    void percentagePerStudentPerCourse() {
        List<CourseAttendance> rows = dashboard.summary();

        assertThat(rows).hasSize(8);
        assertThat(rows).filteredOn(r -> r.rollNumber().equals("22CS001"))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.approvedSessions()).isEqualTo(2);
                    assertThat(r.percentage()).isEqualTo(100);
                    assertThat(r.belowThreshold()).isFalse();
                });
    }

    @Test
    @DisplayName("AC-15.3 — a student below the threshold is flagged")
    void belowThresholdStudentIsFlagged() {
        // 22CS003 attended 1 of 2 approved sessions = 50%, below the 75% threshold.
        assertThat(dashboard.summary())
                .filteredOn(r -> r.rollNumber().equals("22CS003"))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.percentage()).isEqualTo(50);
                    assertThat(r.belowThreshold()).isTrue();
                });

        assertThat(dashboard.summary()).filteredOn(CourseAttendance::belowThreshold)
                .as("exactly the students under the threshold are flagged")
                .hasSize(1);
    }

    @Test
    @DisplayName("LATE counts as attended")
    void lateCountsAsAttended() {
        assertThat(AttendanceStatus.LATE.countsAsAttended()).isTrue();
        assertThat(AttendanceStatus.ABSENT.countsAsAttended()).isFalse();
    }

    @Test
    @DisplayName("AC-15.4 — the threshold comes from configuration, not a constant")
    void thresholdComesFromConfiguration() {
        assertThat(dashboard.thresholdPercent()).isEqualTo(75);
    }

    @Test
    @DisplayName("AC-15.2 — counts per workflow state are reported")
    void countsPerWorkflowState() {
        Map<WorkflowState, Long> counts = dashboard.countsByState();
        assertThat(counts.get(WorkflowState.APPROVED)).isEqualTo(16);
        assertThat(counts.get(WorkflowState.DRAFT)).isZero();
    }

    @Test
    @DisplayName("AC-09.1 — search by roll number returns only that student")
    void searchByRollNumber() {
        var page = attendance.search("22CS003", null, null, null, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(r -> r.getStudent().getRollNumber().equals("22CS003"));
    }

    @Test
    @DisplayName("AC-09.2 — search by course and date range")
    void searchByCourseAndDateRange() {
        var page = attendance.search(null, courseId, day2, day2, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(8);
        assertThat(page.getContent()).allMatch(r -> r.getSessionDate().equals(day2));
    }

    @Test
    @DisplayName("AC-09.3 — search by workflow state")
    void searchByWorkflowState() {
        assertThat(attendance.search(null, null, null, null, WorkflowState.APPROVED,
                PageRequest.of(0, 20)).getTotalElements()).isEqualTo(16);
        assertThat(attendance.search(null, null, null, null, WorkflowState.DRAFT,
                PageRequest.of(0, 20)).getTotalElements()).isZero();
    }

    @Test
    @DisplayName("AC-09.4 — filters matching nothing return empty, not an error")
    void searchMatchingNothingReturnsEmpty() {
        var page = attendance.search("NO-SUCH-ROLL", null, null, null, null, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("filters combine: roll number AND state AND date range")
    void filtersCombine() {
        var page = attendance.search("22CS003", courseId, day1, day2,
                WorkflowState.APPROVED, PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("US-10 — a student's own summary is scoped to them")
    void studentSummaryIsScoped() {
        List<CourseAttendance> rows = dashboard.summaryForStudent("22CS003");
        assertThat(rows).allMatch(r -> r.rollNumber().equals("22CS003"));
        assertThat(rows).singleElement().satisfies(r -> assertThat(r.percentage()).isEqualTo(50));
    }

    private Map<Long, AttendanceStatus> allPresent(LocalDate date) {
        Map<Long, AttendanceStatus> m = new HashMap<>();
        attendance.roster(courseId, date).forEach(e -> m.put(e.student().getId(), AttendanceStatus.PRESENT));
        return m;
    }
}
