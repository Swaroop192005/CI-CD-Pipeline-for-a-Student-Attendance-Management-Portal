package com.samp.attendance.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

/**
 * Shared setup for the Selenium user journeys.
 *
 * <p>The application is started in-process on a random port, so the suite needs
 * no externally running server and cannot accidentally test a stale deployment.
 *
 * <p>Waiting is done with explicit {@link WebDriverWait} conditions only. There is
 * no {@code Thread.sleep} anywhere in this suite: fixed sleeps are the single
 * largest source of flaky UI tests (risk R1), and a gate that fails at random is
 * worse than no gate, because the team learns to re-run it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {
                    "spring.datasource.url=jdbc:h2:mem:selenium;DB_CLOSE_DELAY=-1",
                    "samp.attendance.minimum-percent=75"
                })
@ExtendWith(ScreenshotOnFailure.class)
abstract class SeleniumJourneySupport {

    @LocalServerPort
    protected int port;

    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (!"false".equals(System.getProperty("selenium.headless"))) {
            options.addArguments("--headless=new");
        }
        // Required in a container: no sandbox namespaces, and /dev/shm is small.
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage",
                             "--window-size=1366,900", "--disable-gpu");

        String remote = System.getProperty("selenium.remote.url", "");
        if (!remote.isBlank()) {
            // CI path: the browser runs in a separate Selenium container.
            // Jenkins' own image has neither Chrome nor its shared libraries, and
            // this environment blocks apt, so driving a remote browser is the only
            // way to run these journeys inside the pipeline. Both containers use
            // host networking, so the browser can reach the app's random port.
            try {
                driver = new RemoteWebDriver(new URL(remote), options);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot reach remote WebDriver at " + remote, e);
            }
        } else {
            // Local path: ChromeDriver and Chromium on this machine.
            String driverPath = System.getProperty("webdriver.chrome.driver");
            if (driverPath != null && !driverPath.isBlank()) {
                System.setProperty("webdriver.chrome.driver", driverPath);
            }
            String binary = System.getProperty("selenium.chrome.binary");
            if (binary != null && !binary.isBlank()) {
                options.setBinary(binary);
            }
            driver = new ChromeDriver(options);
        }

        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        ScreenshotOnFailure.bind(driver);
    }

    @AfterEach
    void stopBrowser() {
        ScreenshotOnFailure.unbind();
        if (driver != null) {
            driver.quit();
        }
    }

    // ---- helpers -----------------------------------------------------------

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }

    protected WebElement visible(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(testId(id)));
    }

    protected WebElement clickable(String id) {
        return wait.until(ExpectedConditions.elementToBeClickable(testId(id)));
    }

    /**
     * Signs in, then navigates to {@code landingPath} and waits for a landmark.
     *
     * <p>The destination is explicit because the post-login redirect is the same
     * for every role, and a STUDENT has no access to it: assuming the redirect
     * lands somewhere useful is what made the student journey fail first time.
     */
    protected void signIn(String username, String password, String landingPath, String landmarkTestId) {
        driver.get(url("/login"));
        visible("username").sendKeys(username);
        visible("password").sendKeys(password);
        clickable("login-submit").click();
        wait.until(ExpectedConditions.urlContains("/"));
        driver.get(url(landingPath));
        wait.until(ExpectedConditions.visibilityOfElementLocated(testId(landmarkTestId)));
    }

    protected void signIn(String username, String password, String landmarkTestId) {
        signIn(username, password, "/attendance", landmarkTestId);
    }

    /**
     * Ends the session.
     *
     * <p>Clearing cookies rather than requesting {@code /logout}: Spring Security
     * only accepts logout as a POST, so a GET leaves the session intact and the
     * next sign-in silently continues as the previous user — which is how the
     * admin half of the approval journey first ran as the faculty member.
     */
    protected void signOut() {
        driver.manage().deleteAllCookies();
        driver.get(url("/login"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(testId("username")));
    }

    /**
     * Clicks every button matching a prefix until none remain, re-finding the
     * element each time.
     *
     * <p>Each click navigates, so any element reference captured before it is
     * stale. Re-finding inside the loop and tolerating a stale reference is what
     * makes this reliable; holding a list across a navigation is what produced
     * "Node with given id does not belong to the document".
     */
    protected int clickAllMatching(String testIdPrefix, String landmarkTestId, int limit) {
        By selector = By.cssSelector("button[data-testid^='" + testIdPrefix + "']");
        int clicks = 0;
        while (clicks < limit) {
            wait.until(ExpectedConditions.visibilityOfElementLocated(testId(landmarkTestId)));
            if (driver.findElements(selector).isEmpty()) {
                break;
            }
            try {
                wait.until(ExpectedConditions.elementToBeClickable(
                        driver.findElements(selector).get(0))).click();
            } catch (StaleElementReferenceException retry) {
                continue; // the page re-rendered underneath us; find it again
            }
            clicks++;
        }
        return clicks;
    }

    protected boolean exists(String id) {
        return !driver.findElements(testId(id)).isEmpty();
    }
}
