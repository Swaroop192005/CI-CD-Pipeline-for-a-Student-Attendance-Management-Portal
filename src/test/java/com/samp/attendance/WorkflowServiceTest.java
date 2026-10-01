package com.samp.attendance;

import com.samp.attendance.domain.*;
import com.samp.attendance.repository.AttendanceRecordRepository;
import com.samp.attendance.repository.CourseRepository;
import com.samp.attendance.service.AttendanceService;
import com.samp.attendance.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * The role-based approval workflow (US-11 … US-14).
 *
 * <p>These call the service directly rather than going through a screen, because
 * AC-14.1 requires the refusal to come from the service layer. A test that only
 * proved a button was hidden would prove nothing.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wf;DB_CLOSE_DELAY=-1")
@Transactional
class WorkflowServiceTest {

    @Autowired WorkflowService workflow;
    @Autowired AttendanceService attendance;
    @Autowired AttendanceRecordRepository records;
    @Autowired CourseRepository courses;

    private Long recordId;

    @BeforeEach
    void recordOneSession() {
        Long courseId = courses.findByCode("CS301").orElseThrow().getId();
        LocalDate date = LocalDate.now().minusDays(1);
        attendance.saveSession(courseId, date,
                attendance.roster(courseId, date).stream()
                        .collect(Collectors.toMap(e -> e.student().getId(), e -> AttendanceStatus.PRESENT)),
                "faculty1");
        recordId = records.findByCourseAndSessionDate(
                courses.findById(courseId).orElseThrow(), date).get(0).getId();
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    @DisplayName("AC-11.1 — a FACULTY user may submit a DRAFT record")
    void facultyMaySubmitDraft() {
        workflow.submit(recordId, "faculty1");
        assertThat(records.findById(recordId).orElseThrow().getWorkflowState())
                .isEqualTo(WorkflowState.SUBMITTED);
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    @DisplayName("AC-12.2 — a FACULTY user is REFUSED approval by the service, not just the UI")
    void facultyIsRefusedApproval() {
        workflow.submit(recordId, "faculty1");

        assertThatThrownBy(() -> workflow.approve(recordId, "faculty1"))
                .as("approval by a non-admin must be refused at the service layer")
                .isInstanceOf(AccessDeniedException.class);

        assertThat(records.findById(recordId).orElseThrow().getWorkflowState())
                .as("the record must be unchanged after a refused approval")
                .isEqualTo(WorkflowState.SUBMITTED);
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    @DisplayName("AC-12.2 — a FACULTY user is refused rejection as well")
    void facultyIsRefusedRejection() {
        workflow.submit(recordId, "faculty1");
        assertThatThrownBy(() -> workflow.reject(recordId, "faculty1", "not allowed"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("AC-12.1 — an ADMIN may approve a SUBMITTED record")
    void adminMayApprove() {
        workflow.submit(recordId, "admin");
        workflow.approve(recordId, "admin");

        AttendanceRecord approved = records.findById(recordId).orElseThrow();
        assertThat(approved.getWorkflowState()).isEqualTo(WorkflowState.APPROVED);
        assertThat(approved.getUpdatedBy().getUsername()).isEqualTo("admin");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("AC-12.3 — APPROVED is terminal: no transition out of it is permitted")
    void approvedIsTerminal() {
        workflow.submit(recordId, "admin");
        workflow.approve(recordId, "admin");

        assertThatThrownBy(() -> workflow.reject(recordId, "admin", "changed my mind"))
                .isInstanceOf(WorkflowService.IllegalTransitionException.class)
                .hasMessageContaining("from APPROVED");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("AC-13.1 — rejecting with a remark stores it and moves to REJECTED")
    void rejectionStoresRemark() {
        workflow.submit(recordId, "admin");
        workflow.reject(recordId, "admin", "Register does not match the lab sheet");

        AttendanceRecord rejected = records.findById(recordId).orElseThrow();
        assertThat(rejected.getWorkflowState()).isEqualTo(WorkflowState.REJECTED);
        assertThat(rejected.getRemark()).isEqualTo("Register does not match the lab sheet");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("AC-13.2 — rejecting without a remark is refused and the state is unchanged")
    void rejectionWithoutRemarkIsRefused() {
        workflow.submit(recordId, "admin");

        assertThatThrownBy(() -> workflow.reject(recordId, "admin", "   "))
                .isInstanceOf(WorkflowService.RemarkRequiredException.class);

        assertThat(records.findById(recordId).orElseThrow().getWorkflowState())
                .isEqualTo(WorkflowState.SUBMITTED);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("AC-14.1 — skipping review (DRAFT straight to APPROVED) is refused")
    void draftCannotBeApprovedDirectly() {
        assertThatThrownBy(() -> workflow.approve(recordId, "admin"))
                .isInstanceOf(WorkflowService.IllegalTransitionException.class)
                .hasMessageContaining("from DRAFT to APPROVED");
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    @DisplayName("a REJECTED record can be returned to DRAFT for rework")
    void rejectedCanBeRevised() {
        workflow.submit(recordId, "faculty1");
        AttendanceRecord r = records.findById(recordId).orElseThrow();
        r.moveTo(WorkflowState.REJECTED, r.getRecordedBy(), "fix it");
        records.save(r);

        workflow.revise(recordId, "faculty1");
        assertThat(records.findById(recordId).orElseThrow().getWorkflowState())
                .isEqualTo(WorkflowState.DRAFT);
    }
}
