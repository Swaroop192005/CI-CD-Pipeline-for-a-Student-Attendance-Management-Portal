package com.samp.attendance.selenium;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures a screenshot, the page source and the failure details whenever a
 * journey fails.
 *
 * <p>A failing UI test usually reports only "element not found". The screenshot
 * is what turns that into a diagnosis, so it is written for failures and
 * <em>only</em> for failures — a folder of passing screenshots is noise that
 * hides the one image that matters.
 *
 * <p>This implements {@link AfterTestExecutionCallback}, not {@code TestWatcher}.
 * That distinction is the whole mechanism: {@code AfterTestExecutionCallback}
 * runs immediately after the test body and <em>before</em> any {@code @AfterEach}
 * method, so the browser is still alive. A {@code TestWatcher} fires after
 * {@code @AfterEach} has already called {@code driver.quit()}, by which point
 * there is nothing left to photograph — which is exactly how the first version
 * of this class silently captured nothing.
 */
public class ScreenshotOnFailure implements AfterTestExecutionCallback {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    static void bind(WebDriver driver) { DRIVER.set(driver); }
    static void unbind() { DRIVER.remove(); }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isEmpty()) {
            return; // passed - nothing to capture
        }
        Throwable cause = context.getExecutionException().get();

        WebDriver driver = DRIVER.get();
        if (!(driver instanceof TakesScreenshot shooter)) {
            System.err.println("[screenshot] no live driver; cannot capture " + context.getDisplayName());
            return;
        }

        String dir = System.getProperty("selenium.screenshot.dir", "target/selenium-failures");
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String name = context.getRequiredTestClass().getSimpleName() + "."
                    + context.getRequiredTestMethod().getName() + "-" + stamp;

        try {
            Path target = Paths.get(dir);
            Files.createDirectories(target);

            Files.copy(shooter.getScreenshotAs(OutputType.FILE).toPath(),
                       target.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);

            // The DOM at the moment of failure answers what a picture cannot,
            // such as whether an element was present but not visible.
            Files.writeString(target.resolve(name + ".html"), driver.getPageSource());

            Files.writeString(target.resolve(name + ".txt"),
                    "test   : " + context.getDisplayName() + "\n"
                  + "url    : " + driver.getCurrentUrl() + "\n"
                  + "failure: " + cause + "\n");

            System.err.println("[screenshot] FAILURE captured -> " + target.resolve(name + ".png"));
        } catch (Exception e) {
            System.err.println("[screenshot] could not capture failure: " + e.getMessage());
        }
    }
}
