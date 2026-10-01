package com.samp.attendance.selenium;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The five critical user journeys through the portal (US-20).
 *
 * <p>These are the journeys whose breakage would make the portal useless, so they
 * are the ones worth gating deployment on:
 *
 * <ol>
 *   <li>J1 — a faculty member signs in and records a session</li>
 *   <li>J2 — a future session date is refused</li>
 *   <li>J3 — records are found by search</li>
 *   <li>J4 — the approval workflow, including a faculty member being denied approval</li>
 *   <li>J5 — the dashboard flags a student below the threshold</li>
 * </ol>
 *
 * <p>Ordered, because J4 and J5 need data that J1 creates. A suite of journeys
 * that each re-seed from scratch is slower and tests less: this order is itself
 * the realistic path through the product.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Critical user journeys")
class CriticalJourneysIT extends SeleniumJourneySupport {

    private static final String SESSION_DATE = LocalDate.now().minusDays(1).toString();

    // --- Test data, stated once so a reader can check the assertions ---------
    private static final String FACULTY_USER = "faculty1", FACULTY_PASS = "faculty123";
    private static final String ADMIN_USER   = "admin",    ADMIN_PASS   = "admin123";
    private static final String STUDENT_USER = "22cs003",  STUDENT_PASS = "student123";
    private static final String ABSENT_ROLL  = "22CS003";

    @Test @Order(1)
    @DisplayName("J1 — a faculty member signs in and records a full session")
    void j1_recordSession() {
        signIn(FACULTY_USER, FACULTY_PASS, "records-table");

        assertThat(visible("current-user").getText()).isEqualTo(FACULTY_USER);
        assertThat(visible("current-role").getText()).isEqualTo("FACULTY");

        driver.get(url("/attendance/new?courseId=1&sessionDate=" + SESSION_DATE));
        wait.until(ExpectedConditions.visibilityOfElementLocated(testId("roster-table")));

        List<WebElement> rows = driver.findElements(By.cssSelector("[data-testid='roster-table'] tbody tr"));
        assertThat(rows).as("every enrolled student must appear on the roster").hasSize(8);

        // AC-06.1: the selector defaults to PRESENT for an unrecorded session.
        assertThat(new Select(visible("status-22CS001")).getFirstSelectedOption().getText())
                .isEqualTo("PRESENT");

        // Mark one absent so later journeys have a below-threshold student.
        new Select(visible("status-" + ABSENT_ROLL)).selectByValue("ABSENT");
        clickable("save-attendance").click();

        String confirmation = visible("save-confirmation").getText();
        assertThat(confirmation).isEqualTo("Attendance saved for 8 students");

        assertThat(driver.findElements(By.cssSelector("[data-testid='records-table'] tbody tr")))
                .as("eight records must now be listed").hasSize(8);
    }

    @Test @Order(2)
    @DisplayName("J2 — a future session date is refused and nothing is saved")
    void j2_futureDateRefused() {
        signIn(FACULTY_USER, FACULTY_PASS, "records-table");

        String future = LocalDate.now().plusDays(7).toString();
        driver.get(url("/attendance/new?courseId=1&sessionDate=" + future));
        wait.until(ExpectedConditions.visibilityOfElementLocated(testId("roster-table")));
        clickable("save-attendance").click();

        assertThat(visible("form-error").getText())
                .as("AC-06.4 — the user must be told why the save was refused")
                .contains("Session date cannot be in the future");

        // Nothing for that date may have been persisted.
        driver.get(url("/attendance/search?from=" + future + "&to=" + future));
        assertThat(exists("search-empty"))
                .as("no record may exist for a refused future date").isTrue();
    }

    @Test @Order(3)
    @DisplayName("J3 — records are found by roll number and by workflow state")
    void j3_search() {
        signIn(FACULTY_USER, FACULTY_PASS, "records-table");

        driver.get(url("/attendance/search"));
        visible("search-roll").sendKeys(ABSENT_ROLL);
        clickable("search-submit").click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(testId("search-results")));

        List<WebElement> found = driver.findElements(By.cssSelector("[data-testid='search-results'] tbody tr"));
        assertThat(found).as("exactly the one record for this student").hasSize(1);
        assertThat(found.get(0).getText()).contains(ABSENT_ROLL).contains("ABSENT");

        // AC-09.4 — no matches is an empty state, never an error page.
        driver.get(url("/attendance/search?rollNumber=NO-SUCH-ROLL"));
        assertThat(visible("search-empty").getText()).isEqualTo("No matching records");
    }

    @Test @Order(4)
    @DisplayName("J4 — faculty submits, is denied approval, and an admin approves")
    void j4_approvalWorkflow() {
        // Faculty submits every draft.
        signIn(FACULTY_USER, FACULTY_PASS, "records-table");
        int submitted = clickAllMatching("submit-", "records-table", 20);
        assertThat(submitted).as("all eight records should have been submitted").isEqualTo(8);

        // AC-12.2 — a FACULTY user must not even be offered approval, and the
        // server must refuse it. The absence of the control is checked here; the
        // service-level refusal is asserted by WorkflowServiceTest.
        assertThat(driver.findElements(By.cssSelector("button[data-testid^='approve-']")))
                .as("a FACULTY user must not be offered an approve control")
                .isEmpty();

        // Admin approves.
        signOut();
        signIn(ADMIN_USER, ADMIN_PASS, "records-table");

        assertThat(driver.findElements(By.cssSelector("button[data-testid^='approve-']")))
                .as("an ADMIN must be offered approve controls").isNotEmpty();

        int approved = clickAllMatching("approve-", "records-table", 20);
        assertThat(approved).isEqualTo(8);
    }

    @Test @Order(5)
    @DisplayName("J5 — the dashboard flags the student below the threshold")
    void j5_dashboardFlagsShortage() {
        signIn(ADMIN_USER, ADMIN_PASS, "records-table");

        driver.get(url("/dashboard"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(testId("dashboard")));

        assertThat(visible("threshold").getText()).isEqualTo("75%");
        assertThat(visible("state-count-APPROVED").getText()).isEqualTo("8");

        // The absent student attended 0 of 1 approved session = 0%, below 75%.
        assertThat(visible("pct-" + ABSENT_ROLL + "-CS301").getText()).isEqualTo("0%");
        assertThat(driver.findElements(testId("below-threshold-row")))
                .as("exactly the student below the threshold is flagged").hasSize(1);
        assertThat(visible("below-threshold-count").getText()).isEqualTo("1");

        // A student who attended shows 100% and is not flagged.
        assertThat(visible("pct-22CS001-CS301").getText()).isEqualTo("100%");
    }

    @Test @Order(6)
    @DisplayName("J6 — a student sees only their own attendance")
    void j6_studentSeesOnlyOwn() {
        signIn(STUDENT_USER, STUDENT_PASS, "/my-attendance", "my-summary");

        assertThat(visible("my-roll").getText()).isEqualTo(ABSENT_ROLL);
        assertThat(visible("my-pct-CS301").getText()).isEqualTo("0%");
        assertThat(exists("my-shortage-CS301")).as("the shortage must be visible to the student").isTrue();

        // AC-02.1 — the entry screen is refused outright for a STUDENT.
        driver.get(url("/attendance/new"));
        assertThat(driver.getPageSource())
                .as("a STUDENT must not reach the attendance entry screen")
                .doesNotContain("data-testid=\"roster-table\"");
    }
}
