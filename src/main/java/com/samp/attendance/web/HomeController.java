package com.samp.attendance.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing page. Its only job at this stage is to prove the full request path is
 * wired end to end (Tomcat to Spring MVC to Thymeleaf to externalised config)
 * and to surface the settings that the pipeline parameterises.
 */
@Controller
public class HomeController {

    private final String applicationName;
    private final int minimumAttendancePercent;
    private final String activeProfile;

    public HomeController(
            @Value("${spring.application.name}") String applicationName,
            @Value("${samp.attendance.minimum-percent}") int minimumAttendancePercent,
            @Value("${spring.profiles.active:default}") String activeProfile) {
        this.applicationName = applicationName;
        this.minimumAttendancePercent = minimumAttendancePercent;
        this.activeProfile = activeProfile;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("minimumAttendancePercent", minimumAttendancePercent);
        model.addAttribute("activeProfile", activeProfile);
        return "index";
    }
}
