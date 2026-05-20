package com.teamworkload.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.teamworkload.common.Result;
import com.teamworkload.dto.MonthlyLeaveStatDTO;
import com.teamworkload.entity.LeaveRequest;
import com.teamworkload.service.LeaveRequestService;
import com.teamworkload.util.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/leave")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    public Result<LeaveRequest> submit(@RequestBody LeaveRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(leaveRequestService.submitLeave(userId, request));
    }

    @GetMapping("/my")
    public Result<List<LeaveRequest>> myLeaves() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(leaveRequestService.getMyLeaves(userId));
    }

    @PutMapping("/{id}/approve")
    public Result<LeaveRequest> approve(@PathVariable Long id) {
        Long approverId = SecurityUtil.getCurrentUserId();
        return Result.success(leaveRequestService.approveLeave(approverId, id));
    }

    @PutMapping("/{id}/reject")
    public Result<LeaveRequest> reject(@PathVariable Long id, @RequestParam String reason) {
        Long approverId = SecurityUtil.getCurrentUserId();
        return Result.success(leaveRequestService.rejectLeave(approverId, id, reason));
    }

    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> pending() {
        Long approverId = SecurityUtil.getCurrentUserId();
        return Result.success(leaveRequestService.getPendingLeaves(approverId));
    }

    @GetMapping("/history")
    public Result<Page<Map<String, Object>>> history(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String leaveType) {
        Long approverId = SecurityUtil.getCurrentUserId();
        return Result.success(leaveRequestService.getHistoryLeaves(approverId, page, size, status, leaveType));
    }

    @GetMapping("/balance")
    public Result<Map<String, Object>> balance() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(leaveRequestService.getLeaveBalance(userId));
    }

    @PutMapping("/{id}/revoke")
    public Result<LeaveRequest> revoke(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.success(leaveRequestService.revokeLeave(userId, id));
    }

    @GetMapping("/stats/monthly")
    public Result<List<MonthlyLeaveStatDTO>> getMonthlyStats(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return Result.success(leaveRequestService.getMonthlyLeaveStats(year, month));
    }
}
