package com.samp.attendance;

import com.samp.attendance.domain.Course;
import com.samp.attendance.repository.CourseRepository;
import com.samp.attendance.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Form handling for attendance capture (US-06). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ctl;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AttendanceControllerTest {

    @Autowired MockMvc mvc;
    @Autowired CourseRepository courses;
    @Autowired StudentRepository students;

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    @DisplayName("a malformed status field is rejected, not silently skipped")
    void malformedStatusFieldIsRejected() {
        Course cs301 = courses.findByCode("CS301").orElseThrow();
        Long studentId = students.findByRollNumber("22CS001").orElseThrow().getId();

        // Skipping the field would leave this student unrecorded while the
        // confirmation still reported success — the silent data loss the review
        // flagged, and exactly the problem this portal exists to remove.
        assertThatThrownBy(() ->
                mvc.perform(post("/attendance").with(csrf())
                        .param("courseId", String.valueOf(cs301.getId()))
                        .param("sessionDate", LocalDate.now().minusDays(1).toString())
                        .param("status-" + studentId, "NOT_A_REAL_STATUS")))
                .as("a malformed status must not be swallowed")
                // The servlet layer wraps our exception, which in turn wraps the
                // enum parse failure, so assert on the message rather than on the
                // root cause (which is the IllegalArgumentException underneath).
                .hasMessageContaining("Malformed attendance field 'status-" + studentId + "'")
                .hasMessageContaining("NOT_A_REAL_STATUS")
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }
}
