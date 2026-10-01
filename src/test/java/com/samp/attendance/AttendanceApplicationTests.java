package com.samp.attendance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for the local setup delivered in Task 3.
 *
 * <p>These assert the two things the rest of the pipeline depends on: the
 * application context starts, and the health endpoint that the Docker
 * HEALTHCHECK and the Ansible post-deploy verification call actually answers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
class AttendanceApplicationTests {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    @DisplayName("application context loads")
    void contextLoads() {
        assertThat(port).isGreaterThan(0);
    }

    @Test
    @DisplayName("health endpoint reports UP without authentication")
    void healthEndpointIsUpAndPublic() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("landing page renders and exposes the configured threshold")
    void landingPageRendersConfiguredThreshold() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("data-testid=\"status-card\"");
        assertThat(response.getBody()).contains("75%");
    }

    @Test
    @DisplayName("a protected URL is intercepted and the login form is served instead")
    void protectedUrlIsInterceptedBySecurity() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/attendance", String.class);

        // Spring Security redirects the unauthenticated request to the login page,
        // and TestRestTemplate follows that redirect. Asserting on the delivered
        // body is therefore what proves interception: a 3xx never reaches us, and
        // a bare 200 check would pass even if the protected resource were served.
        assertThat(response.getBody())
                .as("expected the login form to be served in place of the protected resource")
                .contains("name=\"username\"")
                .contains("name=\"password\"");
    }
}
