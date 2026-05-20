package com.teamworkload.controller;

import com.teamworkload.common.Result;
import com.teamworkload.entity.MonthlySummary;
import com.teamworkload.service.MonthlySummaryService;
import com.teamworkload.util.SecurityUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report/monthly")
public class MonthlySummaryController {

    private final MonthlySummaryService monthlySummaryService;

    public MonthlySummaryController(MonthlySummaryService monthlySummaryService) {
        this.monthlySummaryService = monthlySummaryService;
    }

    @GetMapping("/summary")
    public Result<MonthlySummary> summary(@RequestParam(defaultValue = "") String yearMonth) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (yearMonth.isEmpty()) {
            yearMonth = java.time.YearMonth.now().toString();
        }
        return Result.success(monthlySummaryService.getPersonalSummary(userId, yearMonth));
    }

    @GetMapping("/history")
    public Result<List<MonthlySummary>> history() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(monthlySummaryService.getPersonalHistory(userId));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public void export(@RequestParam String yearMonth, HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        monthlySummaryService.exportTeamSummary(userId, yearMonth, response);
    }

    @GetMapping("/team/details")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> teamDetails(@RequestParam(defaultValue = "") String yearMonth) {
        if (yearMonth.isEmpty()) {
            yearMonth = java.time.YearMonth.now().toString();
        }
        return Result.success(monthlySummaryService.getTeamMonthlyDetails(yearMonth));
    }

    @GetMapping("/team/details/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportTeamMonthlyDetails(@RequestParam(defaultValue = "") String yearMonth, HttpServletResponse response) {
        if (yearMonth.isEmpty()) {
            yearMonth = java.time.YearMonth.now().toString();
        }
        monthlySummaryService.exportTeamMonthlyDetails(yearMonth, response);
    }
}
