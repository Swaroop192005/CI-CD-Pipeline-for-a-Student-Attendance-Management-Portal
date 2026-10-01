package com.samp.attendance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role restrictions (US-02, success criterion S4).
 *
 * <p>These exist because a hidden link is not access control: the server must
 * refuse the request regardless of what the UI offers.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:acl;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class AccessControlTest {

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("AC-01.3 — an unauthenticated request is redirected to login")
    void unauthenticatedIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/attendance")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    @DisplayName("a FACULTY user may reach the attendance screens")
    void facultyMayReachAttendance() throws Exception {
        mvc.perform(get("/attendance")).andExpect(status().isOk());
        mvc.perform(get("/attendance/new")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "22cs001", roles = "STUDENT")
    @DisplayName("AC-02.1 — a STUDENT is refused the attendance entry screen")
    void studentIsRefusedAttendanceEntry() throws Exception {
        mvc.perform(get("/attendance/new")).andExpect(status().isForbidden());
        mvc.perform(get("/attendance")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    @DisplayName("AC-02.2 — a FACULTY user is refused the admin dashboard")
    void facultyIsRefusedDashboard() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isForbidden());
    }
}
