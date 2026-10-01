package com.samp.attendance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Regression guard: after signing in, every role must land on a page it is
 * allowed to open.
 *
 * <p>Before the role-based success handler, all three roles were sent to
 * {@code /attendance}. A STUDENT authenticated successfully and was then refused
 * with HTTP 403 by the very next request — a working login that presented to the
 * user as a broken application.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:landing;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class LoginLandingPageTest {

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("a STUDENT lands on their own attendance page, not a 403")
    void studentLandsOnOwnAttendance() throws Exception {
        mvc.perform(formLogin("/login").user("22cs001").password("student123"))
           .andExpect(authenticated())
           .andExpect(redirectedUrl("/my-attendance"));
    }

    @Test
    @DisplayName("a FACULTY user lands on the records list")
    void facultyLandsOnRecords() throws Exception {
        mvc.perform(formLogin("/login").user("faculty1").password("faculty123"))
           .andExpect(authenticated())
           .andExpect(redirectedUrl("/attendance"));
    }

    @Test
    @DisplayName("an ADMIN lands on the dashboard")
    void adminLandsOnDashboard() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("admin123"))
           .andExpect(authenticated())
           .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("bad credentials still fail to the login page")
    void badCredentialsFail() throws Exception {
        mvc.perform(formLogin("/login").user("22cs001").password("wrong"))
           .andExpect(unauthenticated())
           .andExpect(redirectedUrl("/login?error"));
    }
}
