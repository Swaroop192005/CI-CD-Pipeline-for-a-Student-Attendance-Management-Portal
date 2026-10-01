package com.samp.attendance.web;

import com.samp.attendance.repository.StudentRepository;
import com.samp.attendance.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.Principal;

/**
 * A student's own attendance (US-10).
 *
 * <p>The roll number is resolved from the authenticated principal, never from a
 * request parameter, so one student cannot read another's record (AC-02.3).
 */
@Controller
public class StudentController {

    private final DashboardService dashboard;
    private final StudentRepository students;

    public StudentController(DashboardService dashboard, StudentRepository students) {
        this.dashboard = dashboard;
        this.students = students;
    }

    @GetMapping("/my-attendance")
    public String myAttendance(Principal principal, Model model) {
        String rollNumber = students.findByAppUserUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No student record for this login"))
                .getRollNumber();

        model.addAttribute("rows", dashboard.summaryForStudent(rollNumber));
        model.addAttribute("records", dashboard.recordsForStudent(rollNumber));
        model.addAttribute("threshold", dashboard.thresholdPercent());
        model.addAttribute("rollNumber", rollNumber);
        return "my-attendance";
    }
}
