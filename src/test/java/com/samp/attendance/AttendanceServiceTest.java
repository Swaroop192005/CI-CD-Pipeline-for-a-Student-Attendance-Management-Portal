package com.samp.attendance;

import com.samp.attendance.domain.*;
import com.samp.attendance.repository.*;
import com.samp.attendance.service.AttendanceService;
import com.samp.attendance.service.RosterEntry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Business rules for attendance capture (US-06) and listing (US-07).
 *
 * <p>These assert the service directly rather than through the UI, because the
 * rules must hold regardless of which screen calls them.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:svc;DB_CLOSE_DELAY=-1")
@Transactional
class AttendanceServiceTest {

    @Autowired AttendanceService attendance;
    @Autowired CourseRepository courses;
    @Autowired StudentRepository students;
    @Autowired AttendanceRecordRepository records;

    private Long cs301Id;

    @BeforeEach
    void findSeededCourse() {
        cs301Id = courses.findByCode("CS301").orElseThrow().getId();
    }

    @Test
    @DisplayName("AC-06.1 — roster lists every enrolled student, defaulting to PRESENT")
    void rosterDefaultsToPresent() {
        List<RosterEntry> roster = attendance.roster(cs301Id, LocalDate.now());

        assertThat(roster).hasSize(8);
        assertThat(roster).allSatisfy(entry -> {
            assertThat(entry.status()).isEqualTo(AttendanceStatus.PRESENT);
            assertThat(entry.alreadyRecorded()).isFalse();
        });
        assertThat(roster.get(0).student().getRollNumber()).isEqualTo("22CS001");
    }

    @Test
    @DisplayName("AC-06.2 — saving a session creates one DRAFT record per student")
    void savingCreatesOneDraftRecordPerStudent() {
        LocalDate date = LocalDate.now().minusDays(1);
        Map<Long, AttendanceStatus> statuses = statusesFor(date, AttendanceStatus.PRESENT);

        int saved = attendance.saveSession(cs301Id, date, statuses, "faculty1");

        assertThat(saved).isEqualTo(8);
        List<AttendanceRecord> stored =
                records.findByCourseAndSessionDate(courses.findById(cs301Id).orElseThrow(), date);
        assertThat(stored).hasSize(8);
        assertThat(stored).allMatch(r -> r.getWorkflowState() == WorkflowState.DRAFT);
        assertThat(stored).allMatch(r -> r.getRecordedBy().getUsername().equals("faculty1"));
    }

    @Test
    @DisplayName("AC-06.3 — re-saving the same session updates records instead of duplicating them")
    void reSavingUpdatesRatherThanDuplicating() {
        LocalDate date = LocalDate.now().minusDays(2);
        attendance.saveSession(cs301Id, date, statusesFor(date, AttendanceStatus.PRESENT), "faculty1");

        Long absentStudentId = students.findByRollNumber("22CS003").orElseThrow().getId();
        Map<Long, AttendanceStatus> revised = statusesFor(date, AttendanceStatus.PRESENT);
        revised.put(absentStudentId, AttendanceStatus.ABSENT);

        attendance.saveSession(cs301Id, date, revised, "faculty1");

        Course cs301 = courses.findById(cs301Id).orElseThrow();
        List<AttendanceRecord> stored = records.findByCourseAndSessionDate(cs301, date);

        assertThat(stored).as("re-saving must not create a second set of records").hasSize(8);
        assertThat(stored)
                .filteredOn(r -> r.getStudent().getRollNumber().equals("22CS003"))
                .singleElement()
                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(AttendanceStatus.ABSENT));
    }

    @Test
    @DisplayName("AC-06.3 — reopening the screen pre-selects what was already recorded")
    void rosterReflectsExistingRecords() {
        LocalDate date = LocalDate.now().minusDays(3);
        Long lateStudentId = students.findByRollNumber("22CS002").orElseThrow().getId();
        Map<Long, AttendanceStatus> statuses = statusesFor(date, AttendanceStatus.PRESENT);
        statuses.put(lateStudentId, AttendanceStatus.LATE);
        attendance.saveSession(cs301Id, date, statuses, "faculty1");

        List<RosterEntry> roster = attendance.roster(cs301Id, date);

        assertThat(roster).allMatch(RosterEntry::alreadyRecorded);
        assertThat(roster)
                .filteredOn(e -> e.student().getRollNumber().equals("22CS002"))
                .singleElement()
                .satisfies(e -> assertThat(e.status()).isEqualTo(AttendanceStatus.LATE));
    }

    @Test
    @DisplayName("AC-06.4 — a future session date is refused and nothing is saved")
    void futureSessionDateIsRefused() {
        LocalDate future = LocalDate.now().plusDays(1);
        Map<Long, AttendanceStatus> statuses = statusesFor(future, AttendanceStatus.PRESENT);

        assertThatThrownBy(() -> attendance.saveSession(cs301Id, future, statuses, "faculty1"))
                .isInstanceOf(AttendanceService.FutureSessionDateException.class)
                .hasMessageContaining("cannot be in the future");

        assertThat(records.findByCourseAndSessionDate(courses.findById(cs301Id).orElseThrow(), future))
                .as("nothing may be persisted for a rejected date")
                .isEmpty();
    }

    @Test
    @DisplayName("AC-08.2 — a SUBMITTED record is not overwritten by a re-save")
    void submittedRecordsAreNotOverwritten() {
        LocalDate date = LocalDate.now().minusDays(4);
        attendance.saveSession(cs301Id, date, statusesFor(date, AttendanceStatus.PRESENT), "faculty1");

        Course cs301 = courses.findById(cs301Id).orElseThrow();
        AttendanceRecord locked = records.findByCourseAndSessionDate(cs301, date).get(0);
        locked.moveTo(WorkflowState.SUBMITTED, locked.getRecordedBy(), null);
        records.save(locked);

        Map<Long, AttendanceStatus> allAbsent = statusesFor(date, AttendanceStatus.ABSENT);
        attendance.saveSession(cs301Id, date, allAbsent, "faculty1");

        AttendanceRecord reloaded = records.findById(locked.getId()).orElseThrow();
        assertThat(reloaded.getStatus())
                .as("a SUBMITTED record must keep its value")
                .isEqualTo(AttendanceStatus.PRESENT);
        assertThat(reloaded.getWorkflowState()).isEqualTo(WorkflowState.SUBMITTED);
    }

    @Test
    @DisplayName("workflow state machine permits only the defined transitions")
    void stateMachinePermitsOnlyDefinedTransitions() {
        assertThat(WorkflowState.DRAFT.canTransitionTo(WorkflowState.SUBMITTED)).isTrue();
        assertThat(WorkflowState.SUBMITTED.canTransitionTo(WorkflowState.APPROVED)).isTrue();
        assertThat(WorkflowState.SUBMITTED.canTransitionTo(WorkflowState.REJECTED)).isTrue();
        assertThat(WorkflowState.REJECTED.canTransitionTo(WorkflowState.DRAFT)).isTrue();

        // AC-12.3 — APPROVED is terminal.
        assertThat(WorkflowState.APPROVED.allowedTransitions()).isEmpty();
        // Skipping review is not permitted.
        assertThat(WorkflowState.DRAFT.canTransitionTo(WorkflowState.APPROVED)).isFalse();
    }

    private Map<Long, AttendanceStatus> statusesFor(LocalDate date, AttendanceStatus status) {
        return new java.util.HashMap<>(attendance.roster(cs301Id, date).stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.student().getId(), e -> status)));
    }
}
