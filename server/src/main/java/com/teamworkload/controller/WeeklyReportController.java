package com.teamworkload.controller;

import com.teamworkload.common.Result;
import com.teamworkload.entity.WeeklyReport;
import com.teamworkload.service.WeeklyReportService;
import com.teamworkload.util.SecurityUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report/weekly")
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    public WeeklyReportController(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }

    @GetMapping("/current")
    public Result<WeeklyReport> current() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(weeklyReportService.getCurrentWeekly(userId));
    }

    @GetMapping("/history")
    public Result<List<WeeklyReport>> history() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(weeklyReportService.getHistoryWeekly(userId));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> params) {
        Long userId = SecurityUtil.getCurrentUserId();
        String nextWeekPlans = (String) params.get("nextWeekPlans");
        String notes = (String) params.get("notes");
        return Result.success(weeklyReportService.submitWeekly(userId, nextWeekPlans, notes));
    }

    @PostMapping("/submit/{targetUserId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
    public Result<Map<String, Object>> submitForUser(@PathVariable Long targetUserId, @RequestBody Map<String, Object> params) {
        Long operatorId = SecurityUtil.getCurrentUserId();
        String nextWeekPlans = (String) params.get("nextWeekPlans");
        String notes = (String) params.get("notes");
        return Result.success(weeklyReportService.submitWeeklyForUser(operatorId, targetUserId, nextWeekPlans, notes));
    }

    @PutMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
    public Result<Void> updateDetails(@PathVariable Long id, @RequestParam String field, @RequestParam(required = false) String value) {
        weeklyReportService.updateWeeklyDetails(id, field, value);
        return Result.success();
    }

    @PutMapping("/user/{userId}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
    public Result<Void> updateDetailsByUserId(@PathVariable Long userId, @RequestParam String field, @RequestParam(required = false) String value) {
        Long operatorId = SecurityUtil.getCurrentUserId();
        weeklyReportService.updateWeeklyDetailsByUserId(operatorId, userId, field, value);
        return Result.success();
    }

    @PutMapping("/current/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER', 'MEMBER')")
    public Result<Void> updateCurrentDetails(@RequestParam String field, @RequestParam(required = false) String value) {
        Long userId = SecurityUtil.getCurrentUserId();
        weeklyReportService.updateCurrentWeeklyDetails(userId, field, value);
        return Result.success();
    }

    @PutMapping("/{id}/revoke")
    public Result<WeeklyReport> revoke(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(weeklyReportService.revokeWeekly(userId, id));
    }

    @GetMapping("/team/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public Result<Map<String, Object>> teamOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            return Result.success(weeklyReportService.getTeamWeeklyOverview(userId, startDate, endDate));
        }
        return Result.success(weeklyReportService.getTeamWeeklyOverview(userId));
    }

    @GetMapping("/team/ranking")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public Result<List<Map<String, Object>>> teamRanking(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            return Result.success(weeklyReportService.getTeamWeeklyRanking(userId, startDate, endDate));
        }
        return Result.success(weeklyReportService.getTeamWeeklyRanking(userId));
    }

    @GetMapping("/team/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEADER')")
    public void teamExport(HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        weeklyReportService.exportTeamWeekly(userId, response);
    }

    @GetMapping("/team/details")
    public Result<List<Map<String, Object>>> teamDetails(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (startDate != null && endDate != null) {
            return Result.success(weeklyReportService.getTeamWeeklyDetails(userId, startDate, endDate));
        }
        return Result.success(weeklyReportService.getTeamWeeklyDetails(userId));
    }

    @GetMapping("/team/details/export")
    public void exportTeamWeeklyDetails(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (startDate != null && endDate != null) {
            weeklyReportService.exportTeamWeeklyDetails(userId, startDate, endDate, response);
        } else {
            weeklyReportService.exportTeamWeeklyDetails(userId, response);
        }
    }
}
