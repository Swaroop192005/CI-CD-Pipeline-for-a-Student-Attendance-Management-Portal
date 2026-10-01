package com.samp.attendance.web;

import com.samp.attendance.domain.WorkflowState;
import com.samp.attendance.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Summary dashboard and pending-approval queue (US-15, US-16). */
@Controller
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var rows = dashboard.summary();
        model.addAttribute("rows", rows);
        model.addAttribute("counts", dashboard.countsByState());
        model.addAttribute("threshold", dashboard.thresholdPercent());
        model.addAttribute("pendingCount", dashboard.countsByState().get(WorkflowState.SUBMITTED));
        model.addAttribute("belowCount", rows.stream().filter(r -> r.belowThreshold()).count());
        return "dashboard";
    }
}
