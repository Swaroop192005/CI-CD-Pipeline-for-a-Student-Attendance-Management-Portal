package com.samp.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Student Attendance Management Portal.
 *
 * <p>The application is packaged as a WAR so that one artefact serves both
 * deployment targets required by the project: {@code java -jar} / {@code
 * spring-boot:run} during development, and a standalone Tomcat 10.1 in the
 * deployment stage of the pipeline.
 */
@SpringBootApplication
public class AttendanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendanceApplication.class, args);
    }
}
