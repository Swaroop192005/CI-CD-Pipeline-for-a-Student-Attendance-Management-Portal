package com.samp.attendance;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Bootstraps the application when the WAR is deployed into a standalone servlet
 * container (Tomcat 10.1) rather than started through the embedded one.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AttendanceApplication.class);
    }
}
