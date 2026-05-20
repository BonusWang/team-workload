package com.teamworkload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamworkload.common.BusinessException;
import com.teamworkload.dto.MonthlyLeaveStatDTO;
import com.teamworkload.entity.LeaveRequest;
import com.teamworkload.entity.SysUser;
import com.teamworkload.entity.WorkDay;
import com.teamworkload.mapper.LeaveRequestMapper;
import com.teamworkload.mapper.SysUserMapper;
import com.teamworkload.mapper.WorkDayMapper;
import com.teamworkload.service.LeaveRequestService;
import com.teamworkload.util.SecurityUtil;
import com.teamworkload.util.WorkDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LeaveRequestServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest> implements LeaveRequestService {

    private final SysUserMapper sysUserMapper;
    private final WorkDayUtil workDayUtil;
    private final WorkDayMapper workDayMapper;

    public LeaveRequestServiceImpl(SysUserMapper sysUserMapper, WorkDayUtil workDayUtil, WorkDayMapper workDayMapper) {
        this.sysUserMapper = sysUserMapper;
        this.workDayUtil = workDayUtil;
        this.workDayMapper = workDayMapper;
    }

    @Override
    @Transactional
    public LeaveRequest submitLeave(Long userId, LeaveRequest request) {
        log.info("提交请假申请，用户ID: {}, 开始日期: {}, 结束日期: {}", 
            userId, request.getStartDate(), request.getEndDate());
        
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 验证必填字段
        if (request.getLeaveType() == null || request.getLeaveType().trim().isEmpty()) {
            throw new BusinessException("请假类型不能为空");
        }
        if (request.getStartDate() == null) {
            throw new BusinessException("开始日期不能为空");
        }
        if (request.getEndDate() == null) {
            throw new BusinessException("结束日期不能为空");
        }
        if (request.getStartTime() == null || request.getStartTime().trim().isEmpty()) {
            throw new BusinessException("开始时间不能为空");
        }
        if (request.getEndTime() == null || request.getEndTime().trim().isEmpty()) {
            throw new BusinessException("结束时间不能为空");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException("请假原因不能为空");
        }

        Long leaderId = user.getLeaderId();
        if (leaderId == null) {
            throw new BusinessException("未设置直属上级，请联系管理员");
        }

        // 检查结束时间是否大于开始时间
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        
        // 如果是同一天，检查结束时间是否大于开始时间
        if (request.getStartDate().equals(request.getEndDate())) {
            int startHour = request.getStartTime() != null ? Integer.parseInt(request.getStartTime().substring(0, 2)) : 9;
            int endHour = request.getEndTime() != null ? Integer.parseInt(request.getEndTime().substring(0, 2)) : 18;
            int startMinute = request.getStartTime() != null ? Integer.parseInt(request.getStartTime().substring(3, 5)) : 0;
            int endMinute = request.getEndTime() != null ? Integer.parseInt(request.getEndTime().substring(3, 5)) : 30;
            
            if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
                throw new BusinessException("结束时间不能早于或等于开始时间");
            }
        }



        // 检查是否有重叠的请假申请（精确到时间段）
        List<LeaveRequest> activeLeaves = list(new LambdaQueryWrapper<LeaveRequest>()
                .eq(LeaveRequest::getUserId, userId)
                .in(LeaveRequest::getStatus, "PENDING", "APPROVED"));
        
        for (LeaveRequest leave : activeLeaves) {
            if (isTimeOverlap(leave, request)) {
                String status = "APPROVED".equals(leave.getStatus()) ? "已通过" : "待审批";
                throw new BusinessException("该时间段内已有" + status + "的请假申请");
            }
        }

        // 计算请假天数（精确到半天，跳过节假日
        BigDecimal leaveDays;
        
        // 解析开始和结束时间，判断是上午还是下午
        int startHour = request.getStartTime() != null ? Integer.parseInt(request.getStartTime().substring(0, 2)) : 9;
        int endHour = request.getEndTime() != null ? Integer.parseInt(request.getEndTime().substring(0, 2)) : 18;
        
        boolean startIsMorning = startHour < 12;
        boolean endIsAfternoon = endHour >= 13;
        
        double totalDays = 0.0;
        LocalDate currentDate = request.getStartDate();
        
        while (!currentDate.isAfter(request.getEndDate())) {
            // 查询当天是否为工作日
            WorkDay workDay = workDayMapper.selectByDate(currentDate);
            
            boolean isWorkDay = true;
            if (workDay != null && workDay.getIsWorkday() != null) {
                isWorkDay = workDay.getIsWorkday() == 1;
            }
            
            if (isWorkDay) {
                if (currentDate.equals(request.getStartDate()) && currentDate.equals(request.getEndDate())) {
                    // 同一天：计算开始到结束
                    if (startIsMorning && !endIsAfternoon) {
                        // 上午请假，算0.5天
                        totalDays += 0.5;
                    } else if (startIsMorning && endIsAfternoon) {
                        // 上午到下午，算1天
                        totalDays += 1.0;
                    } else if (!startIsMorning && endIsAfternoon) {
                        // 下午请假，算0.5天
                        totalDays += 0.5;
                    }
                } else if (currentDate.equals(request.getStartDate())) {
                    // 开始日期
                    totalDays += startIsMorning ? 1.0 : 0.5;
                } else if (currentDate.equals(request.getEndDate())) {
                    // 结束日期
                    totalDays += endIsAfternoon ? 1.0 : 0.5;
                } else {
                    // 中间完整工作日，算1天
                    totalDays += 1.0;
                }
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        leaveDays = BigDecimal.valueOf(totalDays).setScale(1, BigDecimal.ROUND_HALF_UP);

        // 检查年假余额（如果是年假）
        if ("ANNUAL".equals(request.getLeaveType())) {
            // 获取当前年假余额（包括待审批的年假）
            BigDecimal currentBalance = user.getAnnualLeaveBalance();
            if (currentBalance == null) {
                currentBalance = BigDecimal.ZERO;
            }
            
            // 计算待审批的年假天数
            LambdaQueryWrapper<LeaveRequest> pendingAnnualWrapper = new LambdaQueryWrapper<>();
            pendingAnnualWrapper.eq(LeaveRequest::getUserId, userId)
                    .eq(LeaveRequest::getStatus, "PENDING")
                    .eq(LeaveRequest::getLeaveType, "ANNUAL");
            
            List<LeaveRequest> pendingAnnualLeaves = list(pendingAnnualWrapper);
            BigDecimal pendingAnnualDays = BigDecimal.ZERO;
            for (LeaveRequest pendingLeave : pendingAnnualLeaves) {
                pendingAnnualDays = pendingAnnualDays.add(pendingLeave.getDays());
            }
            
            // 计算剩余可用年假
            BigDecimal availableAnnualLeave = currentBalance.subtract(pendingAnnualDays);
            
            // 检查是否有足够的年假
            if (availableAnnualLeave.compareTo(leaveDays) < 0) {
                throw new BusinessException("年假余额不足");
            }
            
            // 提交申请后，减少年假余额
            user.setAnnualLeaveBalance(currentBalance.subtract(leaveDays));
            sysUserMapper.updateById(user);
        }

        request.setUserId(userId);
        request.setDays(leaveDays);
        request.setApproverId(leaderId);
        request.setStatus("PENDING");
        save(request);
        return request;
    }

    @Override
    @Transactional
    public LeaveRequest approveLeave(Long approverId, Long leaveId) {
        LeaveRequest leave = getById(leaveId);
        if (leave == null) {
            throw new BusinessException("请假记录不存在");
        }
        if (!approverId.equals(leave.getApproverId())) {
            throw new BusinessException("无权审批此请假申请");
        }
        if (!"PENDING".equals(leave.getStatus())) {
            throw new BusinessException("该请假申请已处理");
        }

        leave.setStatus("APPROVED");
        leave.setApproveTime(LocalDateTime.now());
        updateById(leave);

        // 审批通过时不需要再扣除年假，因为在提交申请时已经扣除了

        return leave;
    }

    @Override
    @Transactional
    public LeaveRequest rejectLeave(Long approverId, Long leaveId, String reason) {
        LeaveRequest leave = getById(leaveId);
        if (leave == null) {
            throw new BusinessException("请假记录不存在");
        }
        if (!approverId.equals(leave.getApproverId())) {
            throw new BusinessException("无权审批此请假申请");
        }
        if (!"PENDING".equals(leave.getStatus())) {
            throw new BusinessException("该请假申请已处理");
        }

        // 如果是年假，拒绝后归还年假余额
        if ("ANNUAL".equals(leave.getLeaveType())) {
            SysUser user = sysUserMapper.selectById(leave.getUserId());
            if (user != null && user.getAnnualLeaveBalance() != null) {
                // 归还年假余额
                user.setAnnualLeaveBalance(user.getAnnualLeaveBalance().add(leave.getDays()));
                sysUserMapper.updateById(user);
            }
        }

        leave.setStatus("REJECTED");
        leave.setRejectReason(reason);
        leave.setApproveTime(LocalDateTime.now());
        updateById(leave);
        return leave;
    }

    @Override
    public List<LeaveRequest> getMyLeaves(Long userId) {
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaveRequest::getUserId, userId).orderByDesc(LeaveRequest::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<Map<String, Object>> getPendingLeaves(Long approverId) {
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaveRequest::getApproverId, approverId)
                .eq(LeaveRequest::getStatus, "PENDING")
                .orderByAsc(LeaveRequest::getCreateTime);
        List<LeaveRequest> leaves = list(wrapper);
        
        // 转换为包含申请人姓名的Map列表，并排除管理员用户的申请和已撤回的请假
        return leaves.stream()
                .filter(leave -> {
                    // 查询申请人信息
                    SysUser user = sysUserMapper.selectById(leave.getUserId());
                    // 排除管理员用户的申请和已撤回的请假
                    return (user == null || !"ADMIN".equals(user.getRole())) && !"REVOKED".equals(leave.getStatus());
                })
                .map(leave -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", leave.getId());
                    result.put("userId", leave.getUserId());
                    
                    // 查询申请人信息
                    SysUser user = sysUserMapper.selectById(leave.getUserId());
                    result.put("userName", user != null ? user.getName() : "未知用户");
                    
                    result.put("leaveType", leave.getLeaveType());
                    result.put("startDate", leave.getStartDate());
                    result.put("startTime", leave.getStartTime());
                    result.put("endDate", leave.getEndDate());
                    result.put("endTime", leave.getEndTime());
                    result.put("days", leave.getDays());
                    result.put("reason", leave.getReason());
                    result.put("createTime", leave.getCreateTime());
                    result.put("status", leave.getStatus());
                    
                    return result;
                }).collect(Collectors.toList());
    }

    @Override
    public Page<Map<String, Object>> getHistoryLeaves(Long approverId, Integer page, Integer size, String status, String leaveType) {
        // 创建分页查询
        Page<LeaveRequest> leavePage = new Page<>(page, size);

        // 构建查询条件，查询当前用户审批过的记录或自己申请的记录
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(LeaveRequest::getApproverId, approverId)
                .or()
                .eq(LeaveRequest::getUserId, approverId)
        );

        // 状态过滤（非必填，不传则查所有状态）
        if (status != null && !status.isEmpty()) {
            wrapper.eq(LeaveRequest::getStatus, status);
        }

        // 请假类型过滤（非必填）
        if (leaveType != null && !leaveType.isEmpty()) {
            wrapper.eq(LeaveRequest::getLeaveType, leaveType);
        }

        wrapper.orderByDesc(LeaveRequest::getApproveTime);

        // 执行分页查询
        Page<LeaveRequest> resultPage = page(leavePage, wrapper);

        // 转换为包含申请人姓名的Map列表
        List<Map<String, Object>> records = resultPage.getRecords().stream().map(leave -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", leave.getId());
            result.put("userId", leave.getUserId());

            // 查询申请人信息
            SysUser user = sysUserMapper.selectById(leave.getUserId());
            result.put("userName", user != null ? user.getName() : "未知用户");

            result.put("leaveType", leave.getLeaveType());
            result.put("startDate", leave.getStartDate());
            result.put("startTime", leave.getStartTime());
            result.put("endDate", leave.getEndDate());
            result.put("endTime", leave.getEndTime());
            result.put("days", leave.getDays());
            result.put("reason", leave.getReason());
            result.put("status", leave.getStatus());
            result.put("rejectReason", leave.getRejectReason());
            result.put("approveTime", leave.getApproveTime());

            return result;
        }).collect(Collectors.toList());

        // 创建新的分页结果
        Page<Map<String, Object>> responsePage = new Page<>();
        responsePage.setCurrent(resultPage.getCurrent());
        responsePage.setSize(resultPage.getSize());
        responsePage.setTotal(resultPage.getTotal());
        responsePage.setRecords(records);

        return responsePage;
    }

    @Override
    public Map<String, Object> getLeaveBalance(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        Map<String, Object> result = new HashMap<>();
        
        // 获取当前年假余额
        BigDecimal annualLeaveBalance = user != null ? user.getAnnualLeaveBalance() : BigDecimal.ZERO;
        if (annualLeaveBalance == null) {
            annualLeaveBalance = BigDecimal.ZERO;
        }
        
        // 计算待审批的年假天数
        LambdaQueryWrapper<LeaveRequest> pendingAnnualWrapper = new LambdaQueryWrapper<>();
        pendingAnnualWrapper.eq(LeaveRequest::getUserId, userId)
                .eq(LeaveRequest::getStatus, "PENDING")
                .eq(LeaveRequest::getLeaveType, "ANNUAL");
        
        List<LeaveRequest> pendingAnnualLeaves = list(pendingAnnualWrapper);
        BigDecimal pendingAnnualDays = BigDecimal.ZERO;
        for (LeaveRequest pendingLeave : pendingAnnualLeaves) {
            pendingAnnualDays = pendingAnnualDays.add(pendingLeave.getDays());
        }
        
        // 计算可用年假余额
        BigDecimal availableAnnualLeave = annualLeaveBalance.subtract(pendingAnnualDays);
        
        result.put("annualLeaveBalance", annualLeaveBalance);
        result.put("pendingAnnualDays", pendingAnnualDays);
        result.put("availableAnnualLeave", availableAnnualLeave);

        // 计算本月已请假天数
        String currentMonth = YearMonth.now().toString();
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaveRequest::getUserId, userId)
                .eq(LeaveRequest::getStatus, "APPROVED")
                .ge(LeaveRequest::getStartDate, YearMonth.now().atDay(1))
                .le(LeaveRequest::getEndDate, YearMonth.now().atEndOfMonth());
        List<LeaveRequest> leaves = list(wrapper);
        BigDecimal monthLeaveDays = leaves.stream()
                .map(LeaveRequest::getDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("monthLeaveDays", monthLeaveDays);

        return result;
    }

    /**
     * 检查两个请假申请的时间段是否重叠
     * @param leave1 已有的请假申请
     * @param leave2 新的请假申请
     * @return 是否重叠
     */
    private boolean isTimeOverlap(LeaveRequest leave1, LeaveRequest leave2) {
        LocalDate start1 = leave1.getStartDate();
        LocalDate end1 = leave1.getEndDate();
        LocalDate start2 = leave2.getStartDate();
        LocalDate end2 = leave2.getEndDate();
        
        // 如果日期范围完全不重叠，直接返回false
        if (end1.isBefore(start2) || end2.isBefore(start1)) {
            return false;
        }
        
        // 获取时间段的开始和结束（上午=true，下午=false）
        boolean start1IsMorning = isMorning(leave1.getStartTime());
        boolean end1IsAfternoon = isAfternoon(leave1.getEndTime());
        boolean start2IsMorning = isMorning(leave2.getStartTime());
        boolean end2IsAfternoon = isAfternoon(leave2.getEndTime());
        
        // 遍历重叠的日期
        LocalDate current = start1.isBefore(start2) ? start2 : start1;
        LocalDate overlapEnd = end1.isBefore(end2) ? end1 : end2;
        
        while (!current.isAfter(overlapEnd)) {
            boolean isOverlap = checkDayOverlap(current, start1, end1, start1IsMorning, end1IsAfternoon, 
                                               start2, end2, start2IsMorning, end2IsAfternoon);
            if (isOverlap) {
                return true;
            }
            current = current.plusDays(1);
        }
        
        return false;
    }
    
    /**
     * 检查某一天的时间段是否重叠
     */
    private boolean checkDayOverlap(LocalDate day, 
                                   LocalDate start1, LocalDate end1, boolean start1M, boolean end1A,
                                   LocalDate start2, LocalDate end2, boolean start2M, boolean end2A) {
        // 获取leave1在这一天的时间段
        int leave1StartPart = 0; // 0=不包含这一天，1=上午，2=下午，3=全天
        int leave1EndPart = 0;
        
        if (day.isEqual(start1) && day.isEqual(end1)) {
            // 同一天
            if (start1M && !end1A) leave1StartPart = 1;
            else if (start1M && end1A) leave1StartPart = 3;
            else if (!start1M && end1A) leave1StartPart = 2;
        } else if (day.isEqual(start1)) {
            // 开始日
            leave1StartPart = start1M ? 3 : 2;
        } else if (day.isEqual(end1)) {
            // 结束日
            leave1StartPart = end1A ? 3 : 1;
        } else if (day.isAfter(start1) && day.isBefore(end1)) {
            // 中间日（全天）
            leave1StartPart = 3;
        }
        
        // 获取leave2在这一天的时间段
        int leave2StartPart = 0;
        
        if (day.isEqual(start2) && day.isEqual(end2)) {
            if (start2M && !end2A) leave2StartPart = 1;
            else if (start2M && end2A) leave2StartPart = 3;
            else if (!start2M && end2A) leave2StartPart = 2;
        } else if (day.isEqual(start2)) {
            leave2StartPart = start2M ? 3 : 2;
        } else if (day.isEqual(end2)) {
            leave2StartPart = end2A ? 3 : 1;
        } else if (day.isAfter(start2) && day.isBefore(end2)) {
            leave2StartPart = 3;
        }
        
        // 检查是否有重叠
        return (leave1StartPart == 1 && (leave2StartPart == 1 || leave2StartPart == 3)) ||
               (leave1StartPart == 2 && (leave2StartPart == 2 || leave2StartPart == 3)) ||
               (leave1StartPart == 3 && leave2StartPart > 0);
    }
    
    /**
     * 判断是否为上午
     */
    private boolean isMorning(String time) {
        if (time == null) return true;
        int hour = Integer.parseInt(time.substring(0, 2));
        return hour < 12;
    }
    
    /**
     * 判断是否为下午
     */
    private boolean isAfternoon(String time) {
        if (time == null) return true;
        int hour = Integer.parseInt(time.substring(0, 2));
        return hour >= 13;
    }

    @Override
    @Transactional
    public LeaveRequest revokeLeave(Long userId, Long leaveId) {
        log.info("撤回请假申请，用户ID: {}, 请假ID: {}", userId, leaveId);
        
        LeaveRequest leave = getById(leaveId);
        if (leave == null) {
            throw new BusinessException("请假记录不存在");
        }
        
        if (!leave.getUserId().equals(userId)) {
            throw new BusinessException("只能撤回自己的请假申请");
        }
        
        if (!"PENDING".equals(leave.getStatus())) {
            throw new BusinessException("只有待审批状态的请假可以撤回");
        }
        
        // 如果是年假，恢复年假余额
        if ("ANNUAL".equals(leave.getLeaveType()) && leave.getDays() != null) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user != null) {
                BigDecimal currentBalance = user.getAnnualLeaveBalance();
                if (currentBalance == null) {
                    currentBalance = BigDecimal.ZERO;
                }
                user.setAnnualLeaveBalance(currentBalance.add(leave.getDays()));
                sysUserMapper.updateById(user);
            }
        }
        
        // 更新状态为已撤回
        leave.setStatus("REVOKED");
        leave.setRejectReason("用户撤回");
        leave.setApproveTime(java.time.LocalDateTime.now());
        updateById(leave);
        
        return leave;
    }

    @Override
    public List<MonthlyLeaveStatDTO> getMonthlyLeaveStats(Integer year, Integer month) {
        log.info("获取月度请假统计，年份: {}, 月份: {}", year, month);

        // 获取当前登录用户ID
        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 查询当前用户和下属的用户ID列表
        List<Long> userIds = new ArrayList<>();
        userIds.add(currentUserId);

        // 获取下属ID列表
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getLeaderId, currentUserId);
        List<SysUser> subordinates = sysUserMapper.selectList(userWrapper);
        if (subordinates != null && !subordinates.isEmpty()) {
            userIds.addAll(subordinates.stream().map(SysUser::getId).collect(Collectors.toList()));
        }

        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        // 查询当前用户和下属可能与该月份有重叠的已通过请假记录
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaveRequest::getStatus, "APPROVED")
                .le(LeaveRequest::getStartDate, monthEnd)
                .ge(LeaveRequest::getEndDate, monthStart)
                .in(LeaveRequest::getUserId, userIds);
        List<LeaveRequest> leaves = list(wrapper);
        
        // 按用户分组统计
        Map<String, BigDecimal> userDaysMap = new HashMap<>();
        
        for (LeaveRequest leave : leaves) {
            BigDecimal daysInMonth = calculateDaysInMonth(leave, year, month);
            if (daysInMonth.compareTo(BigDecimal.ZERO) > 0) {
                SysUser user = sysUserMapper.selectById(leave.getUserId());
                String userName = user != null ? user.getName() : "未知用户";
                userDaysMap.merge(userName, daysInMonth, BigDecimal::add);
            }
        }
        
        // 转换为DTO列表
        List<MonthlyLeaveStatDTO> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : userDaysMap.entrySet()) {
            result.add(new MonthlyLeaveStatDTO(
                    String.format("%04d-%02d", year, month),
                    entry.getKey(),
                    entry.getValue().setScale(1, BigDecimal.ROUND_HALF_UP)
            ));
        }
        
        log.info("月度请假统计结果，共 {} 条记录", result.size());
        return result;
    }
    
    /**
     * 计算请假记录在指定月份的天数
     */
    private BigDecimal calculateDaysInMonth(LeaveRequest leave, Integer year, Integer month) {
        LocalDate leaveStart = leave.getStartDate();
        LocalDate leaveEnd = leave.getEndDate();
        
        YearMonth targetMonth = YearMonth.of(year, month);
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        
        // 确定实际在目标月份内的起止日期
        LocalDate actualStart = leaveStart.isBefore(monthStart) ? monthStart : leaveStart;
        LocalDate actualEnd = leaveEnd.isAfter(monthEnd) ? monthEnd : leaveEnd;
        
        // 如果实际结束日期在开始日期之前，说明不在该月
        if (actualEnd.isBefore(actualStart)) {
            return BigDecimal.ZERO;
        }
        
        // 如果请假完全在该月内，直接返回全部天数
        if (leaveStart.isAfter(monthStart.minusDays(1)) && leaveEnd.isBefore(monthEnd.plusDays(1))) {
            return leave.getDays();
        }
        
        // 跨月情况：逐天计算在目标月内的请假天数
        BigDecimal result = BigDecimal.ZERO;
        LocalDate currentDate = actualStart;
        
        // 解析时间段
        boolean startIsMorning = isMorning(leave.getStartTime());
        boolean endIsAfternoon = isAfternoon(leave.getEndTime());
        
        while (!currentDate.isAfter(actualEnd)) {
            // 检查当天是否为工作日
            WorkDay workDay = workDayMapper.selectByDate(currentDate);
            boolean isWorkDay = true;
            if (workDay != null && workDay.getIsWorkday() != null) {
                isWorkDay = workDay.getIsWorkday() == 1;
            }
            
            if (isWorkDay) {
                if (currentDate.equals(leaveStart) && currentDate.equals(leaveEnd)) {
                    // 请假开始和结束在同一天
                    if (startIsMorning && !endIsAfternoon) {
                        // 上午请假
                        result = result.add(BigDecimal.valueOf(0.5));
                    } else if (startIsMorning && endIsAfternoon) {
                        // 全天请假
                        result = result.add(BigDecimal.ONE);
                    } else if (!startIsMorning && endIsAfternoon) {
                        // 下午请假
                        result = result.add(BigDecimal.valueOf(0.5));
                    }
                } else if (currentDate.equals(leaveStart)) {
                    // 请假开始日
                    result = result.add(startIsMorning ? BigDecimal.ONE : BigDecimal.valueOf(0.5));
                } else if (currentDate.equals(leaveEnd)) {
                    // 请假结束日
                    result = result.add(endIsAfternoon ? BigDecimal.ONE : BigDecimal.valueOf(0.5));
                } else {
                    // 中间完整工作日
                    result = result.add(BigDecimal.ONE);
                }
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return result;
    }
}
