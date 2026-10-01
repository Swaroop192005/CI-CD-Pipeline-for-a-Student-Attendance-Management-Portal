package com.samp.attendance.web;

import com.samp.attendance.domain.AttendanceStatus;
import com.samp.attendance.domain.WorkflowState;
import com.samp.attendance.service.AttendanceService;
import com.samp.attendance.service.WorkflowService;
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
    private static final String STATUS_FIELD_PREFIX = "status-";

    private final AttendanceService attendance;
    private final WorkflowService workflow;

    public AttendanceController(AttendanceService attendance, WorkflowService workflow) {
        this.attendance = attendance;
        this.workflow = workflow;
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
     *
     * <p>A malformed {@code status-*} field is rejected rather than skipped.
     * Skipping it would leave that student's attendance unrecorded while the
     * confirmation still reported success — a silent data loss, which is the very
     * problem this portal exists to remove. Such a field can only come from a
     * broken or tampered form, so failing loudly is correct.
     */
    private Map<Long, AttendanceStatus> extractStatuses(Map<String, String> params) {
        Map<Long, AttendanceStatus> result = new HashMap<>();
        params.forEach((key, value) -> {
            if (!key.startsWith(STATUS_FIELD_PREFIX)) {
                return;
            }
            String rawStudentId = key.substring(STATUS_FIELD_PREFIX.length());
            try {
                result.put(Long.valueOf(rawStudentId), AttendanceStatus.valueOf(value));
            } catch (IllegalArgumentException e) {
                throw new MalformedAttendanceFieldException(key, value, e);
            }
        });
        return result;
    }

    /** Thrown when a posted {@code status-*} field cannot be interpreted. */
    static class MalformedAttendanceFieldException extends RuntimeException {
        MalformedAttendanceFieldException(String field, String value, Throwable cause) {
            super("Malformed attendance field '" + field + "' with value '" + value + "'", cause);
        }
    }

    /** US-09 — search and filter, every criterion optional. */
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String rollNumber,
                         @RequestParam(required = false) Long courseId,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(required = false) WorkflowState state,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {

        var results = attendance.search(rollNumber, courseId, from, to, state,
                                        PageRequest.of(Math.max(page, 0), PAGE_SIZE));

        model.addAttribute("records", results);
        model.addAttribute("courses", attendance.allCourses());
        model.addAttribute("states", WorkflowState.values());
        model.addAttribute("currentPage", results.getNumber());
        model.addAttribute("totalPages", results.getTotalPages());
        model.addAttribute("rollNumber", rollNumber);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("selectedState", state);
        model.addAttribute("searched", true);
        return "attendance/search";
    }

    /** US-11 — submit a draft for approval. */
    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return runTransition(() -> workflow.submit(id, principal.getName()),
                             "Record submitted for approval", redirect);
    }

    /** US-12 — approve (ADMIN only; the service enforces it). */
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return runTransition(() -> workflow.approve(id, principal.getName()),
                             "Record approved", redirect);
    }

    /** US-13 — reject with a mandatory remark (ADMIN only). */
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String remark,
                         Principal principal, RedirectAttributes redirect) {
        return runTransition(() -> workflow.reject(id, principal.getName(), remark),
                             "Record rejected", redirect);
    }

    /** Send a rejected record back to DRAFT for rework. */
    @PostMapping("/{id}/revise")
    public String revise(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return runTransition(() -> workflow.revise(id, principal.getName()),
                             "Record returned to draft", redirect);
    }

    /**
     * Turns a refused transition into a message on the records page instead of an
     * error page. The refusal itself still happens in the service — this only
     * decides how it is presented.
     */
    private String runTransition(Runnable action, String successMessage, RedirectAttributes redirect) {
        try {
            action.run();
            redirect.addFlashAttribute("message", successMessage);
        } catch (WorkflowService.IllegalTransitionException | WorkflowService.RemarkRequiredException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/attendance";
    }
}
