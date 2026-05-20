package com.teamworkload.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.teamworkload.dto.MonthlyLeaveStatDTO;
import com.teamworkload.entity.LeaveRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface LeaveRequestService extends IService<LeaveRequest> {

    LeaveRequest submitLeave(Long userId, LeaveRequest request);

    LeaveRequest approveLeave(Long approverId, Long leaveId);

    LeaveRequest rejectLeave(Long approverId, Long leaveId, String reason);

    List<LeaveRequest> getMyLeaves(Long userId);

    List<Map<String, Object>> getPendingLeaves(Long approverId);

    Page<Map<String, Object>> getHistoryLeaves(Long approverId, Integer page, Integer size, String status, String leaveType);

    Map<String, Object> getLeaveBalance(Long userId);

    LeaveRequest revokeLeave(Long userId, Long leaveId);
    
    List<MonthlyLeaveStatDTO> getMonthlyLeaveStats(Integer year, Integer month);
}
