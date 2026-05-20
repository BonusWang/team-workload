package com.teamworkload.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.RowWriteHandlerContext;
import com.alibaba.excel.write.handler.context.SheetWriteHandlerContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamworkload.common.BusinessException;
import com.teamworkload.entity.DailyReport;
import com.teamworkload.entity.SysUser;
import com.teamworkload.entity.WeeklyReport;
import com.teamworkload.mapper.DailyReportMapper;
import com.teamworkload.mapper.SysUserMapper;
import com.teamworkload.mapper.WeeklyReportMapper;
import com.teamworkload.service.WeeklyReportService;
import com.teamworkload.util.WorkDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WeeklyReportServiceImpl extends ServiceImpl<WeeklyReportMapper, WeeklyReport> implements WeeklyReportService {

    private final DailyReportMapper dailyReportMapper;
    private final SysUserMapper sysUserMapper;
    private final WorkDayUtil workDayUtil;

    public WeeklyReportServiceImpl(DailyReportMapper dailyReportMapper, SysUserMapper sysUserMapper, WorkDayUtil workDayUtil) {
        this.dailyReportMapper = dailyReportMapper;
        this.sysUserMapper = sysUserMapper;
        this.workDayUtil = workDayUtil;
    }

    @Override
    public WeeklyReport getCurrentWeekly(Long userId) {
        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());
        return getOrCreateWeekly(userId, range[0], range[1]);
    }

    @Override
    public List<WeeklyReport> getHistoryWeekly(Long userId) {
        LambdaQueryWrapper<WeeklyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeeklyReport::getUserId, userId)
                .orderByDesc(WeeklyReport::getWeekEndDate);
        return list(wrapper);
    }

    @Override
    @Transactional
    public Map<String, Object> submitWeekly(Long userId, String nextWeekPlans, String notes) {
        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());
        List<Long> submittedUserIds = new ArrayList<>();

        WeeklyReport report = getOrCreateWeekly(userId, range[0], range[1]);

        if (!"SUBMITTED".equals(report.getStatus())) {
            BigDecimal totalHours = calculateTotalHours(userId, range[0], range[1]);
            report.setTotalHours(totalHours);
            report.setNextWeekPlans(nextWeekPlans);
            report.setNotes(notes);
            report.setStatus("SUBMITTED");
            report.setSubmittedAt(LocalDateTime.now());
            updateById(report);
            submittedUserIds.add(userId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("submittedUsers", submittedUserIds.size());
        result.put("message", "周报提交成功");
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> submitWeeklyForUser(Long operatorId, Long targetUserId, String nextWeekPlans, String notes) {
        SysUser operator = sysUserMapper.selectById(operatorId);
        if (operator == null) {
            throw new BusinessException("操作用户不存在");
        }
        boolean isAdminOrLeader = "ADMIN".equals(operator.getRole()) || "LEADER".equals(operator.getRole());
        boolean isSelf = operatorId.equals(targetUserId);

        if (!isSelf && !isAdminOrLeader) {
            throw new BusinessException("没有权限代提交该成员的周报");
        }

        if (!isSelf) {
            List<SysUser> teamMembers = getTeamMembers(operatorId);
            boolean isTeamMember = teamMembers.stream().anyMatch(m -> m.getId().equals(targetUserId));
            if (!isTeamMember) {
                throw new BusinessException("只能代提交团队成员的周报");
            }
        }

        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());
        WeeklyReport report = getOrCreateWeekly(targetUserId, range[0], range[1]);

        if (!"SUBMITTED".equals(report.getStatus())) {
            BigDecimal totalHours = calculateTotalHours(targetUserId, range[0], range[1]);
            report.setTotalHours(totalHours);
            report.setNextWeekPlans(nextWeekPlans);
            report.setNotes(notes);
            report.setStatus("SUBMITTED");
            report.setSubmittedAt(LocalDateTime.now());
            updateById(report);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("submittedUsers", 1);
        result.put("message", "周报提交成功");
        return result;
    }

    @Override
    public void updateWeeklyDetails(Long reportId, String field, String value) {
        WeeklyReport report = getById(reportId);
        if (report == null) {
            throw new BusinessException("周报不存在");
        }
        if ("SUBMITTED".equals(report.getStatus())) {
            throw new BusinessException("已提交的周报无法编辑");
        }
        
        switch (field) {
            case "thisWeekTasks":
                report.setThisWeekTasks(value != null ? value : "");
                break;
            case "nextWeekPlans":
                report.setNextWeekPlans(value != null ? value : "");
                break;
            case "notes":
                report.setNotes(value != null ? value : "");
                break;
            default:
                throw new BusinessException("不支持更新的字段: " + field);
        }
        
        updateById(report);
    }

    @Override
    public void updateWeeklyDetailsByUserId(Long operatorId, Long targetUserId, String field, String value) {
        SysUser operator = sysUserMapper.selectById(operatorId);
        if (operator == null) {
            throw new BusinessException("操作用户不存在");
        }
        boolean isAdminOrLeader = "ADMIN".equals(operator.getRole()) || "LEADER".equals(operator.getRole());
        boolean isSelf = operatorId.equals(targetUserId);

        if (!isSelf && !isAdminOrLeader) {
            throw new BusinessException("没有权限编辑该成员的周报");
        }

        if (!isSelf) {
            List<SysUser> teamMembers = getTeamMembers(operatorId);
            boolean isTeamMember = teamMembers.stream().anyMatch(m -> m.getId().equals(targetUserId));
            if (!isTeamMember) {
                throw new BusinessException("只能编辑团队成员的周报");
            }
        }

        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());
        WeeklyReport report = getOrCreateWeekly(targetUserId, range[0], range[1]);

        if ("SUBMITTED".equals(report.getStatus())) {
            throw new BusinessException("已提交的周报无法编辑");
        }

        switch (field) {
            case "thisWeekTasks":
                report.setThisWeekTasks(value != null ? value : "");
                break;
            case "nextWeekPlans":
                report.setNextWeekPlans(value != null ? value : "");
                break;
            case "notes":
                report.setNotes(value != null ? value : "");
                break;
            default:
                throw new BusinessException("不支持更新的字段: " + field);
        }

        updateById(report);
    }

    @Override
    public void updateCurrentWeeklyDetails(Long userId, String field, String value) {
        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());
        WeeklyReport report = getOrCreateWeekly(userId, range[0], range[1]);
        
        if ("SUBMITTED".equals(report.getStatus())) {
            throw new BusinessException("已提交的周报无法编辑");
        }
        
        switch (field) {
            case "thisWeekTasks":
                report.setThisWeekTasks(value != null ? value : "");
                break;
            case "nextWeekPlans":
                report.setNextWeekPlans(value != null ? value : "");
                break;
            case "notes":
                report.setNotes(value != null ? value : "");
                break;
            default:
                throw new BusinessException("不支持更新的字段: " + field);
        }
        updateById(report);
    }

    @Override
    @Transactional
    public WeeklyReport revokeWeekly(Long userId, Long reportId) {
        WeeklyReport report = getById(reportId);
        if (report == null) {
            throw new BusinessException("周报不存在");
        }
        
        // 检查权限：只能撤回自己的周报，或者管理员/领导者可以撤回团队成员的周报
        SysUser currentUser = sysUserMapper.selectById(userId);
        boolean isAdminOrLeader = "ADMIN".equals(currentUser.getRole()) || "LEADER".equals(currentUser.getRole());
        boolean isOwner = report.getUserId().equals(userId);
        boolean isTeamMember = false;
        
        if (isAdminOrLeader) {
            List<SysUser> teamMembers = getTeamMembers(userId);
            isTeamMember = teamMembers.stream().anyMatch(member -> member.getId().equals(report.getUserId()));
        }
        
        if (!isOwner && !(isAdminOrLeader && isTeamMember)) {
            throw new BusinessException("没有权限撤回该周报");
        }
        
        if (!"SUBMITTED".equals(report.getStatus())) {
            throw new BusinessException("只能撤回已提交的周报");
        }
        
        report.setStatus("DRAFT");
        report.setSubmittedAt(null);
        updateById(report);
        return report;
    }

    @Override
    public Map<String, Object> getTeamWeeklyOverview(Long leaderId) {
        List<SysUser> members = getTeamMembers(leaderId);
        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());

        BigDecimal teamTotalHours = BigDecimal.ZERO;
        List<Map<String, Object>> memberStats = new ArrayList<>();
        Map<String, BigDecimal> projectHours = new LinkedHashMap<>();

        for (SysUser member : members) {
            BigDecimal memberHours = calculateTotalHours(member.getId(), range[0], range[1]);
            teamTotalHours = teamTotalHours.add(memberHours);

            Map<String, Object> stat = new HashMap<>();
            stat.put("userId", member.getId());
            stat.put("name", member.getName());
            stat.put("totalHours", memberHours);
            stat.put("level", member.getLevel());
            stat.put("alert", getAlertLevel(memberHours));
            memberStats.add(stat);

            LambdaQueryWrapper<DailyReport> reportWrapper = new LambdaQueryWrapper<>();
            reportWrapper.eq(DailyReport::getUserId, member.getId())
                    .ge(DailyReport::getWorkDate, range[0])
                    .le(DailyReport::getWorkDate, range[1]);
            List<DailyReport> reports = dailyReportMapper.selectList(reportWrapper);
            for (DailyReport dr : reports) {
                projectHours.merge(dr.getProjectName(), dr.getHours(), BigDecimal::add);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("teamTotalHours", teamTotalHours);
        result.put("memberCount", members.size());
        result.put("memberStats", memberStats);
        result.put("projectDistribution", projectHours);
        result.put("weekStartDate", range[0].toString());
        result.put("weekEndDate", range[1].toString());
        return result;
    }

    @Override
    public Map<String, Object> getTeamWeeklyOverview(Long leaderId, String startDate, String endDate) {
        List<SysUser> members = getTeamMembers(leaderId);
        LocalDate rangeStart = LocalDate.parse(startDate);
        LocalDate rangeEnd = LocalDate.parse(endDate);

        BigDecimal teamTotalHours = BigDecimal.ZERO;
        List<Map<String, Object>> memberStats = new ArrayList<>();
        Map<String, BigDecimal> projectHours = new LinkedHashMap<>();

        for (SysUser member : members) {
            BigDecimal memberHours = calculateTotalHours(member.getId(), rangeStart, rangeEnd);
            teamTotalHours = teamTotalHours.add(memberHours);

            Map<String, Object> stat = new HashMap<>();
            stat.put("userId", member.getId());
            stat.put("name", member.getName());
            stat.put("totalHours", memberHours);
            stat.put("level", member.getLevel());
            stat.put("alert", getAlertLevel(memberHours));
            memberStats.add(stat);

            LambdaQueryWrapper<DailyReport> reportWrapper = new LambdaQueryWrapper<>();
            reportWrapper.eq(DailyReport::getUserId, member.getId())
                    .ge(DailyReport::getWorkDate, rangeStart)
                    .le(DailyReport::getWorkDate, rangeEnd);
            List<DailyReport> reports = dailyReportMapper.selectList(reportWrapper);
            for (DailyReport dr : reports) {
                projectHours.merge(dr.getProjectName(), dr.getHours(), BigDecimal::add);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("teamTotalHours", teamTotalHours);
        result.put("memberCount", members.size());
        result.put("memberStats", memberStats);
        result.put("projectDistribution", projectHours);
        result.put("weekStartDate", rangeStart.toString());
        result.put("weekEndDate", rangeEnd.toString());
        return result;
    }

    @Override
    public List<Map<String, Object>> getTeamWeeklyRanking(Long leaderId) {
        List<SysUser> members = getTeamMembers(leaderId);
        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (SysUser member : members) {
            BigDecimal hours = calculateTotalHours(member.getId(), range[0], range[1]);
            Map<String, Object> item = new HashMap<>();
            item.put("userId", member.getId());
            item.put("name", member.getName());
            item.put("totalHours", hours);
            item.put("alert", getAlertLevel(hours));
            ranking.add(item);
        }

        ranking.sort((a, b) -> ((BigDecimal) b.get("totalHours")).compareTo((BigDecimal) a.get("totalHours")));
        return ranking;
    }

    @Override
    public List<Map<String, Object>> getTeamWeeklyRanking(Long leaderId, String startDate, String endDate) {
        List<SysUser> members = getTeamMembers(leaderId);
        LocalDate rangeStart = LocalDate.parse(startDate);
        LocalDate rangeEnd = LocalDate.parse(endDate);

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (SysUser member : members) {
            BigDecimal hours = calculateTotalHours(member.getId(), rangeStart, rangeEnd);
            Map<String, Object> item = new HashMap<>();
            item.put("userId", member.getId());
            item.put("name", member.getName());
            item.put("totalHours", hours);
            item.put("alert", getAlertLevel(hours));
            ranking.add(item);
        }

        ranking.sort((a, b) -> ((BigDecimal) b.get("totalHours")).compareTo((BigDecimal) a.get("totalHours")));
        return ranking;
    }

    @Override
    public void exportTeamWeekly(Long leaderId, HttpServletResponse response) {
        List<SysUser> members = getTeamMembers(leaderId);
        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());
        SysUser leader = sysUserMapper.selectById(leaderId);

        List<List<String>> head = buildExportHead();
        List<List<Object>> data = new ArrayList<>();

        for (SysUser member : members) {
            BigDecimal memberHours = calculateTotalHours(member.getId(), range[0], range[1]);
            LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DailyReport::getUserId, member.getId())
                    .ge(DailyReport::getWorkDate, range[0])
                    .le(DailyReport::getWorkDate, range[1]);
            List<DailyReport> reports = dailyReportMapper.selectList(wrapper);

            BigDecimal projectHours = BigDecimal.ZERO;
            BigDecimal softHours = BigDecimal.ZERO;
            for (DailyReport dr : reports) {
                if (dr.getProjectName() != null && dr.getProjectName().contains("运维")) {
                    softHours = softHours.add(dr.getHours());
                } else {
                    projectHours = projectHours.add(dr.getHours());
                }
            }

            BigDecimal unitPrice = member.getUnitPrice() != null ? member.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal capacity = memberHours.multiply(unitPrice);

            List<Object> row = new ArrayList<>();
            row.add(leader != null ? leader.getName() : "");
            row.add(member.getName());
            row.add(member.getDept() != null ? member.getDept() : "");
            row.add(member.getLevel() != null ? member.getLevel() : "");
            row.add(softHours);
            row.add(projectHours);
            row.add(memberHours);
            row.add(unitPrice);
            row.add(capacity);
            row.add("");
            row.add("");
            row.add("");
            data.add(row);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("团队周报_" + range[0] + "_" + range[1], "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream()).head(head).sheet("团队周报").doWrite(data);
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private WeeklyReport getOrCreateWeekly(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        LambdaQueryWrapper<WeeklyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeeklyReport::getUserId, userId)
                .eq(WeeklyReport::getWeekEndDate, weekEnd);
        WeeklyReport report = getOne(wrapper);

        if (report == null) {
            report = new WeeklyReport();
            report.setUserId(userId);
            report.setWeekStartDate(weekStart);
            report.setWeekEndDate(weekEnd);
            report.setTotalHours(calculateTotalHours(userId, weekStart, weekEnd));
            report.setStatus("DRAFT");
            save(report);
        } else {
            BigDecimal totalHours = calculateTotalHours(userId, weekStart, weekEnd);
            report.setTotalHours(totalHours);
            if ("DRAFT".equals(report.getStatus())) {
                updateById(report);
            }
        }
        return report;
    }

    private BigDecimal calculateTotalHours(Long userId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyReport::getUserId, userId)
                .ge(DailyReport::getWorkDate, start)
                .le(DailyReport::getWorkDate, end);
        List<DailyReport> reports = dailyReportMapper.selectList(wrapper);
        return reports.stream()
                .map(DailyReport::getHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateRegularHours(Long userId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyReport::getUserId, userId)
                .ge(DailyReport::getWorkDate, start)
                .le(DailyReport::getWorkDate, end)
                .eq(DailyReport::getSource, "QUANKAI");
        List<DailyReport> reports = dailyReportMapper.selectList(wrapper);
        return reports.stream()
                .map(DailyReport::getHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateProjectHours(Long userId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<DailyReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyReport::getUserId, userId)
                .ge(DailyReport::getWorkDate, start)
                .le(DailyReport::getWorkDate, end)
                .and(w -> w.isNull(DailyReport::getSource).or().eq(DailyReport::getSource, "").or().eq(DailyReport::getSource, "OTHER"));
        List<DailyReport> reports = dailyReportMapper.selectList(wrapper);
        return reports.stream()
                .map(DailyReport::getHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<SysUser> getTeamMembers(Long leaderId) {
        List<SysUser> allMembers = new ArrayList<>();
        
        // 添加当前用户，但是如果是管理员就排除
        SysUser currentUser = sysUserMapper.selectById(leaderId);
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            allMembers.add(currentUser);
        }
        
        // 递归获取所有下属，并排除管理员
        getAllSubordinates(leaderId, allMembers);
        
        return allMembers;
    }
    
    private void getAllSubordinates(Long leaderId, List<SysUser> result) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getLeaderId, leaderId).eq(SysUser::getStatus, 1).ne(SysUser::getRole, "ADMIN");
        List<SysUser> directSubordinates = sysUserMapper.selectList(wrapper);
        
        for (SysUser subordinate : directSubordinates) {
            result.add(subordinate);
            // 递归获取下属的下属
            getAllSubordinates(subordinate.getId(), result);
        }
    }

    private String getAlertLevel(BigDecimal hours) {
        if (hours.compareTo(new BigDecimal("50")) > 0) return "HIGH";
        if (hours.compareTo(new BigDecimal("20")) < 0) return "LOW";
        return "NORMAL";
    }

    private List<List<String>> buildExportHead() {
        String[] headers = {"组员上级", "组员", "所属框架", "级别", "软开工时", "项目工时", "总工时", "结算单价", "产能", "本周事项主要*", "下周预计*", "补充说明"};
        List<List<String>> head = new ArrayList<>();
        for (String h : headers) {
            head.add(Collections.singletonList(h));
        }
        return head;
    }

    @Override
    public List<Map<String, Object>> getTeamWeeklyDetails(Long leaderId) {
        List<SysUser> members = getTeamMembers(leaderId);
        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());

        List<Map<String, Object>> result = new ArrayList<>();

        for (SysUser member : members) {
            // 获取本周的周报数据
            LambdaQueryWrapper<WeeklyReport> reportWrapper = new LambdaQueryWrapper<>();
            reportWrapper.eq(WeeklyReport::getUserId, member.getId())
                    .eq(WeeklyReport::getWeekEndDate, range[1]);
            WeeklyReport weeklyReport = getOne(reportWrapper);
            
            BigDecimal totalHours = BigDecimal.ZERO;
            String thisWeekTasks = "";
            String nextWeekPlans = "";
            String notes = "";
            String status = "DRAFT";

            totalHours = calculateTotalHours(member.getId(), range[0], range[1]);
            BigDecimal regularHours = calculateRegularHours(member.getId(), range[0], range[1]);
            BigDecimal projectHours = calculateProjectHours(member.getId(), range[0], range[1]);

            if (weeklyReport != null) {
                thisWeekTasks = weeklyReport.getThisWeekTasks() != null ? weeklyReport.getThisWeekTasks() : "";
                nextWeekPlans = weeklyReport.getNextWeekPlans() != null ? weeklyReport.getNextWeekPlans() : "";
                notes = weeklyReport.getNotes() != null ? weeklyReport.getNotes() : "";
                status = weeklyReport.getStatus() != null ? weeklyReport.getStatus() : "DRAFT";
            }
            
            // 获取直接上级
            String directLeaderName = "";
            if (member.getLeaderId() != null) {
                SysUser directLeader = sysUserMapper.selectById(member.getLeaderId());
                if (directLeader != null) {
                    // 如果上级是管理员，则负责人为自己
                    if ("ADMIN".equals(directLeader.getRole())) {
                        directLeaderName = member.getName();
                    } else {
                        directLeaderName = directLeader.getName();
                    }
                }
            }
            
            // 构建返回数据
            Map<String, Object> item = new HashMap<>();
            item.put("id", weeklyReport != null ? weeklyReport.getId() : null);
            item.put("userId", member.getId());
            item.put("leader", directLeaderName);
            item.put("member", member.getName());
            item.put("totalHours", totalHours);
            item.put("regularHours", regularHours);
            item.put("projectHours", projectHours);
            item.put("thisWeekTasks", thisWeekTasks);
            item.put("nextWeekPlans", nextWeekPlans);
            item.put("notes", notes);
            item.put("status", status);
            
            result.add(item);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getTeamWeeklyDetails(Long leaderId, String startDate, String endDate) {
        List<SysUser> members = getTeamMembers(leaderId);
        LocalDate rangeStart = LocalDate.parse(startDate);
        LocalDate rangeEnd = LocalDate.parse(endDate);

        List<Map<String, Object>> result = new ArrayList<>();

        for (SysUser member : members) {
            if ("ADMIN".equals(member.getRole())) {
                continue;
            }

            LambdaQueryWrapper<WeeklyReport> reportWrapper = new LambdaQueryWrapper<>();
            reportWrapper.eq(WeeklyReport::getUserId, member.getId())
                    .eq(WeeklyReport::getWeekEndDate, rangeEnd);
            WeeklyReport weeklyReport = getOne(reportWrapper);

            BigDecimal totalHours = BigDecimal.ZERO;
            BigDecimal regularHours = BigDecimal.ZERO;
            BigDecimal projectHours = BigDecimal.ZERO;
            String thisWeekTasks = "";
            String nextWeekPlans = "";
            String notes = "";
            String status = "DRAFT";

            totalHours = calculateTotalHours(member.getId(), rangeStart, rangeEnd);
            regularHours = calculateRegularHours(member.getId(), rangeStart, rangeEnd);
            projectHours = calculateProjectHours(member.getId(), rangeStart, rangeEnd);

            if (weeklyReport != null) {
                thisWeekTasks = weeklyReport.getThisWeekTasks() != null ? weeklyReport.getThisWeekTasks() : "";
                nextWeekPlans = weeklyReport.getNextWeekPlans() != null ? weeklyReport.getNextWeekPlans() : "";
                notes = weeklyReport.getNotes() != null ? weeklyReport.getNotes() : "";
                status = weeklyReport.getStatus() != null ? weeklyReport.getStatus() : "DRAFT";
            }

            String directLeaderName = "";
            if (member.getLeaderId() != null) {
                SysUser directLeader = sysUserMapper.selectById(member.getLeaderId());
                if (directLeader != null) {
                    if ("ADMIN".equals(directLeader.getRole())) {
                        directLeaderName = member.getName();
                    } else {
                        directLeaderName = directLeader.getName();
                    }
                }
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", weeklyReport != null ? weeklyReport.getId() : null);
            item.put("userId", member.getId());
            item.put("leader", directLeaderName);
            item.put("member", member.getName());
            item.put("totalHours", totalHours);
            item.put("regularHours", regularHours);
            item.put("projectHours", projectHours);
            item.put("thisWeekTasks", thisWeekTasks);
            item.put("nextWeekPlans", nextWeekPlans);
            item.put("notes", notes);
            item.put("status", status);

            result.add(item);
        }

        return result;
    }

    @Override
    public void exportTeamWeeklyDetails(Long leaderId, HttpServletResponse response) {
        LocalDate[] range = workDayUtil.getWeekRange(LocalDate.now());
        exportTeamWeeklyDetailsInternal(leaderId, range[0], range[1], response,
                "周报详情_" + range[0] + "_" + range[1]);
    }

    private void exportTeamWeeklyDetailsInternal(Long leaderId, LocalDate rangeStart, LocalDate rangeEnd,
                                                  HttpServletResponse response, String fileNamePrefix) {
        List<SysUser> members = getTeamMembers(leaderId);

        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("负责人"));
        head.add(Collections.singletonList("人员"));
        head.add(Collections.singletonList("总工时"));
        head.add(Collections.singletonList("本周事项"));
        head.add(Collections.singletonList("下周预计事项"));
        head.add(Collections.singletonList("补充说明"));

        List<List<Object>> data = new ArrayList<>();
        List<MergeRange> mergeRanges = new ArrayList<>();

        String currentLeader = null;
        int leaderStartRow = -1;
        int dataRowIndex = 0;

        for (SysUser member : members) {
            if ("ADMIN".equals(member.getRole())) {
                continue;
            }

            LambdaQueryWrapper<WeeklyReport> reportWrapper = new LambdaQueryWrapper<>();
            reportWrapper.eq(WeeklyReport::getUserId, member.getId())
                    .eq(WeeklyReport::getWeekEndDate, rangeEnd);
            WeeklyReport weeklyReport = getOne(reportWrapper);

            BigDecimal totalHours = calculateTotalHours(member.getId(), rangeStart, rangeEnd);
            String thisWeekTasks = "";
            String nextWeekPlans = "";
            String notes = "";

            if (weeklyReport != null) {
                thisWeekTasks = weeklyReport.getThisWeekTasks() != null ? weeklyReport.getThisWeekTasks() : "";
                nextWeekPlans = weeklyReport.getNextWeekPlans() != null ? weeklyReport.getNextWeekPlans() : "";
                notes = weeklyReport.getNotes() != null ? weeklyReport.getNotes() : "";
            }

            String directLeaderName = "";
            if (member.getLeaderId() != null) {
                SysUser directLeader = sysUserMapper.selectById(member.getLeaderId());
                if (directLeader != null) {
                    if ("ADMIN".equals(directLeader.getRole())) {
                        directLeaderName = member.getName();
                    } else {
                        directLeaderName = directLeader.getName();
                    }
                }
            }

            List<Object> row = new ArrayList<>();
            row.add(directLeaderName);
            row.add(member.getName());
            row.add(totalHours);
            row.add(thisWeekTasks);
            row.add(nextWeekPlans);
            row.add(notes);
            data.add(row);

            if (!directLeaderName.equals(currentLeader)) {
                if (currentLeader != null && leaderStartRow >= 0) {
                    mergeRanges.add(new MergeRange(leaderStartRow, dataRowIndex - 1));
                }
                currentLeader = directLeaderName;
                leaderStartRow = dataRowIndex;
            }
            dataRowIndex++;
        }
        if (currentLeader != null && leaderStartRow >= 0 && leaderStartRow < dataRowIndex - 1) {
            mergeRanges.add(new MergeRange(leaderStartRow, dataRowIndex - 1));
        }

        String titleText = "统计日报时间：" + rangeStart + " ~ " + rangeEnd;

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode(fileNamePrefix, "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .registerWriteHandler(new TitleRowWriteHandler(titleText, 6))
                    .registerWriteHandler(new LeaderMergeWriteHandler(mergeRanges))
                    .registerWriteHandler(new BorderWriteHandler())
                    .sheet("周报详情")
                    .doWrite(data);
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private static class MergeRange {
        final int startRow;
        final int endRow;
        MergeRange(int startRow, int endRow) {
            this.startRow = startRow;
            this.endRow = endRow;
        }
    }

    private static class TitleRowWriteHandler implements SheetWriteHandler {
        private final String titleText;
        private final int columnCount;

        TitleRowWriteHandler(String titleText, int columnCount) {
            this.titleText = titleText;
            this.columnCount = columnCount;
        }

        @Override
        public void afterSheetCreate(SheetWriteHandlerContext context) {
            Sheet sheet = context.getWriteSheetHolder().getSheet();
            Workbook workbook = sheet.getWorkbook();

            Row titleRow = sheet.createRow(1);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setFontName("微软雅黑");
            titleFont.setFontHeightInPoints((short) 12);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setBorderTop(BorderStyle.THIN);
            titleStyle.setBorderBottom(BorderStyle.THIN);
            titleStyle.setBorderLeft(BorderStyle.THIN);
            titleStyle.setBorderRight(BorderStyle.THIN);
            titleStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
            titleStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            titleStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            titleStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());

            for (int j = 0; j < columnCount; j++) {
                Cell cell = titleRow.createCell(j);
                if (j == 0) {
                    cell.setCellValue(titleText);
                }
                cell.setCellStyle(titleStyle);
            }

            if (columnCount > 1) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, columnCount - 1));
            }

            titleRow.setHeightInPoints(28);
        }
    }

    private static class LeaderMergeWriteHandler implements SheetWriteHandler {
        private final List<MergeRange> mergeRanges;

        LeaderMergeWriteHandler(List<MergeRange> mergeRanges) {
            this.mergeRanges = mergeRanges;
        }

        @Override
        public void afterSheetCreate(SheetWriteHandlerContext context) {
            Sheet sheet = context.getWriteSheetHolder().getSheet();
            Workbook workbook = sheet.getWorkbook();

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerStyle.setBorderTop(BorderStyle.THIN);
            centerStyle.setBorderBottom(BorderStyle.THIN);
            centerStyle.setBorderLeft(BorderStyle.THIN);
            centerStyle.setBorderRight(BorderStyle.THIN);
            centerStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
            centerStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            centerStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            centerStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());

            for (MergeRange range : mergeRanges) {
                int startRow = range.startRow + 2;
                int endRow = range.endRow + 2;
                if (startRow < endRow) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 0, 0));
                    for (int r = startRow; r <= endRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null) {
                            Cell cell = row.getCell(0);
                            if (cell != null) {
                                cell.setCellStyle(centerStyle);
                            }
                        }
                    }
                }
            }
        }
    }

    private static class BorderWriteHandler implements RowWriteHandler {
        private int maxCol = 0;
        private final Set<Integer> leaderRows = new HashSet<>();

        @Override
        public void afterRowDispose(RowWriteHandlerContext context) {
            Row row = context.getRow();
            if (row == null) return;
            Workbook workbook = row.getSheet().getWorkbook();
            int rowIndex = row.getRowNum();

            if (row.getLastCellNum() > maxCol) {
                maxCol = row.getLastCellNum();
            }

            for (int j = 0; j < maxCol; j++) {
                Cell cell = row.getCell(j);
                if (cell == null) {
                    cell = row.createCell(j);
                }
                CellStyle existingStyle = cell.getCellStyle();
                CellStyle newStyle = workbook.createCellStyle();
                newStyle.cloneStyleFrom(existingStyle);
                newStyle.setBorderTop(BorderStyle.THIN);
                newStyle.setBorderBottom(BorderStyle.THIN);
                newStyle.setBorderLeft(BorderStyle.THIN);
                newStyle.setBorderRight(BorderStyle.THIN);
                newStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
                newStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
                newStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
                newStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());

                if (j == 0 && cell.getStringCellValue() != null && !cell.getStringCellValue().isEmpty()) {
                    newStyle.setAlignment(HorizontalAlignment.CENTER);
                    newStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                }

                cell.setCellStyle(newStyle);
            }
        }
    }

    @Override
    public void exportTeamWeeklyDetails(Long leaderId, String startDate, String endDate, HttpServletResponse response) {
        LocalDate rangeStart = LocalDate.parse(startDate);
        LocalDate rangeEnd = LocalDate.parse(endDate);
        exportTeamWeeklyDetailsInternal(leaderId, rangeStart, rangeEnd, response,
                "团队周报详情_" + startDate + "~" + endDate);
    }
}
