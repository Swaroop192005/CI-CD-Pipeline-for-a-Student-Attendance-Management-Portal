package com.samp.attendance.web;

import com.samp.attendance.domain.AttendanceStatus;
import com.samp.attendance.service.AttendanceService;
import com.samp.attendance.service.RosterEntry;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attendance capture (US-06) and listing (US-07).
 *
 * <p>Binds HTTP to {@link AttendanceService}. All rules about what may be saved
 * live in the service, so this class stays free of business logic.
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    private static final int PAGE_SIZE = 20; // AC-07.2

    private final AttendanceService attendance;

    public AttendanceController(AttendanceService attendance) {
        this.attendance = attendance;
    }

    /** US-07 — paged list, newest session first. */
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        var records = attendance.list(PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        model.addAttribute("records", records);
        model.addAttribute("currentPage", records.getNumber());
        model.addAttribute("totalPages", records.getTotalPages());
        return "attendance/list";
    }

    /** US-06 — the entry screen for one course and session date. */
    @GetMapping("/new")
    public String entryForm(@RequestParam(required = false) Long courseId,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
                            Model model) {

        model.addAttribute("courses", attendance.allCourses());
        LocalDate date = sessionDate != null ? sessionDate : LocalDate.now();
        model.addAttribute("sessionDate", date);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("statuses", AttendanceStatus.values());

        if (courseId != null) {
            List<RosterEntry> roster = attendance.roster(courseId, date);
            model.addAttribute("roster", roster);
            model.addAttribute("course", attendance.requireCourse(courseId));
        }
        return "attendance/new";
    }

    /** US-06 — save the session. */
    @PostMapping
    public String save(@RequestParam Long courseId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
                       @RequestParam Map<String, String> allParams,
                       Principal principal,
                       RedirectAttributes redirect) {

        Map<Long, AttendanceStatus> statusByStudentId = extractStatuses(allParams);

        try {
            int saved = attendance.saveSession(courseId, sessionDate, statusByStudentId, principal.getName());
            redirect.addFlashAttribute("message", "Attendance saved for " + saved + " students"); // AC-06.2
            return "redirect:/attendance";
        } catch (AttendanceService.FutureSessionDateException e) {
            // AC-06.4 — redisplay the form with the error rather than saving.
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addAttribute("courseId", courseId);
            return "redirect:/attendance/new";
        }
    }

    /**
     * Form fields are named {@code status-<studentId>}; everything else posted
     * (course, date, CSRF token) is ignored here.
     */
    private Map<Long, AttendanceStatus> extractStatuses(Map<String, String> params) {
        Map<Long, AttendanceStatus> result = new HashMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith("status-")) {
                try {
                    result.put(Long.valueOf(key.substring("status-".length())),
                               AttendanceStatus.valueOf(value));
                } catch (IllegalArgumentException ignored) {
                    // An unparseable field is skipped rather than failing the whole save.
                }
            }
        });
        return result;
    }
}
