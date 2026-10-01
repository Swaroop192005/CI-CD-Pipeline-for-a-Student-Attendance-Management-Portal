package com.samp.attendance;

import com.samp.attendance.domain.AttendanceStatus;
import com.samp.attendance.repository.CourseRepository;
import com.samp.attendance.service.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Regression guard for a defect found in Task 5 by running the application rather
 * than by testing it.
 *
 * <p>The records list rendered fine while empty and returned HTTP 500 as soon as
 * it had rows: the template reads {@code updatedBy}, and with
 * {@code spring.jpa.open-in-view=false} that lazy proxy could no longer be
 * initialised once the transaction had closed.
 *
 * <p>The service tests missed it because they run inside {@code @Transactional},
 * which keeps the persistence context open. This test deliberately does not, so
 * it renders the view the way a real request does.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:render;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AttendanceListRenderingTest {

    @Autowired MockMvc mvc;
    @Autowired AttendanceService attendance;
    @Autowired CourseRepository courses;

    @BeforeEach
    void recordOneSession() {
        Long courseId = courses.findByCode("CS301").orElseThrow().getId();
        LocalDate date = LocalDate.now().minusDays(1);
        attendance.saveSession(courseId, date,
                attendance.roster(courseId, date).stream()
                        .collect(Collectors.toMap(e -> e.student().getId(), e -> AttendanceStatus.PRESENT)),
                "faculty1");
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    @DisplayName("AC-07.1 — the records list renders with rows present, outside a transaction")
    void listRendersWithRows() throws Exception {
        mvc.perform(get("/attendance"))
           .andExpect(status().isOk())
           .andExpect(view().name("attendance/list"))
           .andExpect(content().string(org.hamcrest.Matchers.containsString("22CS001")))
           // updatedBy is what previously threw: assert the rendered name is present.
           .andExpect(content().string(org.hamcrest.Matchers.containsString("Prof. Suresh Kumar")));
    }
}
