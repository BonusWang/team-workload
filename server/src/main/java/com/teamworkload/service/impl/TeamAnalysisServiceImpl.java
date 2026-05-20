package com.teamworkload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamworkload.entity.DailyReport;
import com.teamworkload.entity.LeaveRequest;
import com.teamworkload.entity.SysUser;
import com.teamworkload.mapper.DailyReportMapper;
import com.teamworkload.mapper.LeaveRequestMapper;
import com.teamworkload.mapper.SysUserMapper;
import com.teamworkload.service.TeamAnalysisService;
import com.teamworkload.util.WorkDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class TeamAnalysisServiceImpl implements TeamAnalysisService {

    private final SysUserMapper sysUserMapper;
    private final DailyReportMapper dailyReportMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final WorkDayUtil workDayUtil;

    public TeamAnalysisServiceImpl(SysUserMapper sysUserMapper, DailyReportMapper dailyReportMapper,
                                   LeaveRequestMapper leaveRequestMapper, WorkDayUtil workDayUtil) {
        this.sysUserMapper = sysUserMapper;
        this.dailyReportMapper = dailyReportMapper;
        this.leaveRequestMapper = leaveRequestMapper;
        this.workDayUtil = workDayUtil;
    }

    @Override
    public Map<String, Object> analyzeGap(Long leaderId) {
        List<SysUser> members = getTeamMembers(leaderId);
        LocalDate[] weekRange = workDayUtil.getWeekRange(LocalDate.now());

        List<Map<String, Object>> onLeave = new ArrayList<>();
        List<Map<String, Object>> unfilled = new ArrayList<>();
        List<Map<String, Object>> lowWorkload = new ArrayList<>();

        for (SysUser member : members) {
            LambdaQueryWrapper<LeaveRequest> leaveWrapper = new LambdaQueryWrapper<>();
            leaveWrapper.eq(LeaveRequest::getUserId, member.getId())
                    .eq(LeaveRequest::getStatus, "APPROVED")
                    .le(LeaveRequest::getStartDate, weekRange[1])
                    .ge(LeaveRequest::getEndDate, weekRange[0]);
            long leaveCount = leaveRequestMapper.selectCount(leaveWrapper);
            if (leaveCount > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", member.getId());
                item.put("name", member.getName());
                item.put("type", "LEAVE");
                onLeave.add(item);
                continue;
            }

            LambdaQueryWrapper<DailyReport> reportWrapper = new LambdaQueryWrapper<>();
            reportWrapper.eq(DailyReport::getUserId, member.getId())
                    .ge(DailyReport::getWorkDate, weekRange[0])
                    .le(DailyReport::getWorkDate, weekRange[1]);
            List<DailyReport> reports = dailyReportMapper.selectList(reportWrapper);
            BigDecimal totalHours = reports.stream()
                    .map(DailyReport::getHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalHours.compareTo(BigDecimal.ZERO) == 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", member.getId());
                item.put("name", member.getName());
                item.put("type", "UNFILLED");
                unfilled.add(item);
            } else if (totalHours.compareTo(new BigDecimal("20")) < 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", member.getId());
                item.put("name", member.getName());
                item.put("hours", totalHours);
                item.put("type", "LOW");
                lowWorkload.add(item);
            }
        }

        List<Map<String, Object>> allGaps = new ArrayList<>();
        allGaps.addAll(onLeave);
        allGaps.addAll(unfilled);
        allGaps.addAll(lowWorkload);

        Map<String, Object> result = new HashMap<>();
        result.put("weekStartDate", weekRange[0].toString());
        result.put("weekEndDate", weekRange[1].toString());
        result.put("totalMembers", members.size());
        result.put("gapCount", allGaps.size());
        result.put("gaps", allGaps);
        result.put("suggestion", generateSuggestion(allGaps.size(), members.size()));
        return result;
    }

    private List<SysUser> getTeamMembers(Long leaderId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getLeaderId, leaderId).eq(SysUser::getStatus, 1).ne(SysUser::getRole, "ADMIN");
        List<SysUser> members = new ArrayList<>(sysUserMapper.selectList(wrapper));
        // 添加当前用户，但是如果是管理员也排除
        SysUser leader = sysUserMapper.selectById(leaderId);
        if (leader != null && !"ADMIN".equals(leader.getRole())) {
            members.add(leader);
        }
        return members;
    }

    private String generateSuggestion(int gapCount, int totalCount) {
        if (gapCount == 0) {
            return "团队工作量饱满，无人力空缺";
        }
        double ratio = (double) gapCount / totalCount;
        if (ratio > 0.3) {
            return "团队人力空缺较大，建议协调资源或调整任务分配";
        } else if (ratio > 0.1) {
            return "存在部分人力空缺，建议关注成员工作状态";
        } else {
            return "人力空缺较小，属正常范围";
        }
    }
}
