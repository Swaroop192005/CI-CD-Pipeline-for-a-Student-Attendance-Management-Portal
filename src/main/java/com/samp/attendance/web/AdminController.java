package com.samp.attendance.web;

import com.samp.attendance.repository.CourseRepository;
import com.samp.attendance.repository.StudentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Master-data screens (US-04, US-05).
 *
 * <p>ADMIN-only; the role rule is in the filter chain for {@code /admin/**}.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CourseRepository courses;
    private final StudentRepository students;

    public AdminController(CourseRepository courses, StudentRepository students) {
        this.courses = courses;
        this.students = students;
    }

    @GetMapping("/courses")
    public String courses(Model model) {
        model.addAttribute("courses", courses.findAll());
        return "admin/courses";
    }

    @GetMapping("/students")
    public String students(Model model) {
        model.addAttribute("students", students.findAll());
        return "admin/students";
    }
}
