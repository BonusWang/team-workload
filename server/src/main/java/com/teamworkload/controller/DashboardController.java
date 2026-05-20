package com.teamworkload.controller;

import com.teamworkload.common.Result;
import com.teamworkload.service.DashboardService;
import com.teamworkload.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/personal")
    public Result<Map<String, Object>> personal() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(dashboardService.getPersonalDashboard(userId));
    }

    @GetMapping("/calendar")
    public Result<List<Map<String, Object>>> calendar(@RequestParam(defaultValue = "") String yearMonth) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (yearMonth.isEmpty()) {
            yearMonth = java.time.YearMonth.now().toString();
        }
        return Result.success(dashboardService.getCalendarData(userId, yearMonth));
    }
}
