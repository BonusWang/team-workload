package com.teamworkload.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.merge.LoopMergeStrategy;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.teamworkload.common.BusinessException;
import com.teamworkload.entity.DailyReport;
import com.teamworkload.entity.LeaveRequest;
import com.teamworkload.entity.MonthlySummary;
import com.teamworkload.entity.SysUser;
import com.teamworkload.entity.WeeklyReport;
import com.teamworkload.mapper.DailyReportMapper;
import com.teamworkload.mapper.LeaveRequestMapper;
import com.teamworkload.mapper.MonthlySummaryMapper;
import com.teamworkload.mapper.SysUserMapper;
import com.teamworkload.mapper.WeeklyReportMapper;
import com.teamworkload.service.MonthlySummaryService;
import com.teamworkload.util.WorkDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MonthlySummaryServiceImpl extends ServiceImpl<MonthlySummaryMapper, MonthlySummary> implements MonthlySummaryService {

    private final DailyReportMapper dailyReportMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final SysUserMapper sysUserMapper;
    private final WorkDayUtil workDayUtil;
    private final WeeklyReportMapper weeklyReportMapper;

    public MonthlySummaryServiceImpl(DailyReportMapper dailyReportMapper, LeaveRequestMapper leaveRequestMapper, SysUserMapper sysUserMapper, WorkDayUtil workDayUtil, WeeklyReportMapper weeklyReportMapper) {
        this.dailyReportMapper = dailyReportMapper;
        this.leaveRequestMapper = leaveRequestMapper;
        this.sysUserMapper = sysUserMapper;
        this.workDayUtil = workDayUtil;
        this.weeklyReportMapper = weeklyReportMapper;
    }

    @Override
    public MonthlySummary getPersonalSummary(Long userId, String yearMonth) {
        // 每次都重新计算，确保数据最新
        MonthlySummary summary = calculateSummary(userId, yearMonth);
        
        LambdaQueryWrapper<MonthlySummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MonthlySummary::getUserId, userId).eq(MonthlySummary::getYearMonth, yearMonth);
        MonthlySummary existing = getOne(wrapper);
        
        if (existing != null) {
            summary.setId(existing.getId());
            updateById(summary);
        } else {
            save(summary);
        }
        
        return summary;
    }

    @Override
    public List<MonthlySummary> getPersonalHistory(Long userId) {
        LambdaQueryWrapper<MonthlySummary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MonthlySummary::getUserId, userId).orderByDesc(MonthlySummary::getYearMonth);
        return list(wrapper);
    }

    @Override
    public Map<String, Object> getTeamSummary(Long leaderId, String yearMonth) {
        List<SysUser> members = getTeamMembers(leaderId);
        List<Map<String, Object>> memberSummaries = new ArrayList<>();
        BigDecimal teamTotalHours = BigDecimal.ZERO;

        for (SysUser member : members) {
            MonthlySummary ms = getPersonalSummary(member.getId(), yearMonth);
            Map<String, Object> item = new HashMap<>();
            item.put("userId", member.getId());
            item.put("name", member.getName());
            item.put("totalHours", ms.getTotalHours());
            item.put("workDays", ms.getWorkDays());
            item.put("leaveDays", ms.getLeaveDays());
            item.put("dailyAvg", ms.getDailyAvg());
            memberSummaries.add(item);
            teamTotalHours = teamTotalHours.add(ms.getTotalHours() != null ? ms.getTotalHours() : BigDecimal.ZERO);
        }

        memberSummaries.sort((a, b) -> ((BigDecimal) b.get("totalHours")).compareTo((BigDecimal) a.get("totalHours")));

        Map<String, Object> result = new HashMap<>();
        result.put("yearMonth", yearMonth);
        result.put("teamTotalHours", teamTotalHours);
        result.put("memberSummaries", memberSummaries);
        return result;
    }

    @Override
    public void exportTeamSummary(Long leaderId, String yearMonth, HttpServletResponse response) {
        Map<String, Object> data = getTeamSummary(leaderId, yearMonth);
        List<Map<String, Object>> summaries = (List<Map<String, Object>>) data.get("memberSummaries");

        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("姓名"));
        head.add(Collections.singletonList("总工时"));
        head.add(Collections.singletonList("应工作天数"));
        head.add(Collections.singletonList("请假天数"));
        head.add(Collections.singletonList("日均投入"));

        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> s : summaries) {
            List<Object> row = new ArrayList<>();
            row.add(s.get("name"));
            row.add(s.get("totalHours"));
            row.add(s.get("workDays"));
            row.add(s.get("leaveDays"));
            row.add(s.get("dailyAvg"));
            rows.add(row);
        }

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("月度汇总_" + yearMonth, "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream()).head(head).sheet("月度汇总").doWrite(rows);
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private MonthlySummary calculateSummary(Long userId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 从 weekly_report 汇总总工时 - 查找与当月有重叠的所有周
        LambdaQueryWrapper<WeeklyReport> weeklyWrapper = new LambdaQueryWrapper<>();
        weeklyWrapper.eq(WeeklyReport::getUserId, userId)
                // 周结束日期 >= 月初 且 周开始日期 <= 月底，即两周有重叠
                .le(WeeklyReport::getWeekStartDate, end)
                .ge(WeeklyReport::getWeekEndDate, start);
        List<WeeklyReport> weeklyReports = weeklyReportMapper.selectList(weeklyWrapper);

        BigDecimal totalHours = weeklyReports.stream()
                .map(WeeklyReport::getTotalHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算原始的应工作天数（包括节假日设置）
        int originalWorkDays = workDayUtil.calculateWorkDays(start, end);

        // 只统计状态为通过的请假记录
        LambdaQueryWrapper<LeaveRequest> leaveWrapper = new LambdaQueryWrapper<>();
        leaveWrapper.eq(LeaveRequest::getUserId, userId)
                .eq(LeaveRequest::getStatus, "APPROVED")
                .le(LeaveRequest::getStartDate, end)
                .ge(LeaveRequest::getEndDate, start);
        List<LeaveRequest> leaves = leaveRequestMapper.selectList(leaveWrapper);
        BigDecimal leaveDays = leaves.stream()
                .map(LeaveRequest::getDays)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 应工作天数 = 原始工作日 - 请假天数，不能小于0
        int workDays = Math.max(0, originalWorkDays - leaveDays.intValue());

        BigDecimal dailyAvg = workDays > 0 ? totalHours.divide(new BigDecimal(workDays), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // 项目分布还是从 daily_report 获取
        LambdaQueryWrapper<DailyReport> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.eq(DailyReport::getUserId, userId)
                .ge(DailyReport::getWorkDate, start)
                .le(DailyReport::getWorkDate, end);
        List<DailyReport> reports = dailyReportMapper.selectList(reportWrapper);

        Map<String, BigDecimal> projectDist = new LinkedHashMap<>();
        for (DailyReport dr : reports) {
            projectDist.merge(dr.getProjectName(), dr.getHours(), BigDecimal::add);
        }

        MonthlySummary summary = new MonthlySummary();
        summary.setUserId(userId);
        summary.setYearMonth(yearMonth);
        summary.setTotalHours(totalHours);
        summary.setWorkDays(workDays);
        summary.setLeaveDays(leaveDays);
        summary.setDailyAvg(dailyAvg);
        summary.setTaskTypeDist(JSON.toJSONString(projectDist));
        return summary;
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

    @Override
    public List<Map<String, Object>> getTeamMonthlyDetails(String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getStatus, 1).orderByAsc(SysUser::getId);
        List<SysUser> allUsers = sysUserMapper.selectList(userWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        BigDecimal totalActualHours = BigDecimal.ZERO;

        for (SysUser user : allUsers) {
            MonthlySummary summary = calculateSummary(user.getId(), yearMonth);
            
            BigDecimal actualHours = summary.getTotalHours() != null ? summary.getTotalHours() : BigDecimal.ZERO;
            int workDays = summary.getWorkDays() != null ? summary.getWorkDays() : 0;
            BigDecimal expectedHours = new BigDecimal(workDays * 8);
            BigDecimal diff = actualHours.subtract(expectedHours);

            String supplementaryNote = buildSupplementaryNote(user.getId(), monthStart, monthEnd);

            Map<String, Object> item = new HashMap<>();
            item.put("submitter", user.getName());
            item.put("actualHours", actualHours);
            item.put("workDays", workDays);
            item.put("expectedHours", expectedHours);
            item.put("diff", diff);
            item.put("supplementaryNote", supplementaryNote);

            result.add(item);
            totalActualHours = totalActualHours.add(actualHours);
        }

        Map<String, Object> totalItem = new HashMap<>();
        totalItem.put("submitter", "合计");
        totalItem.put("actualHours", totalActualHours);
        totalItem.put("workDays", "");
        totalItem.put("expectedHours", "");
        totalItem.put("diff", "");
        totalItem.put("supplementaryNote", "");
        result.add(totalItem);

        return result;
    }

    @Override
    public void exportTeamMonthlyDetails(String yearMonth, HttpServletResponse response) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getStatus, 1).orderByAsc(SysUser::getId);
        List<SysUser> allUsers = sysUserMapper.selectList(userWrapper);

        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("提交人"));
        head.add(Collections.singletonList("实际工时"));
        head.add(Collections.singletonList("工作天数"));
        head.add(Collections.singletonList("应工作工时"));
        head.add(Collections.singletonList("差值"));
        head.add(Collections.singletonList("补充说明"));

        List<List<Object>> data = new ArrayList<>();
        BigDecimal totalActualHours = BigDecimal.ZERO;

        for (SysUser user : allUsers) {
            MonthlySummary summary = calculateSummary(user.getId(), yearMonth);
            
            BigDecimal actualHours = summary.getTotalHours() != null ? summary.getTotalHours() : BigDecimal.ZERO;
            int workDays = summary.getWorkDays() != null ? summary.getWorkDays() : 0;
            BigDecimal expectedHours = new BigDecimal(workDays * 8);
            BigDecimal diff = actualHours.subtract(expectedHours);

            String supplementaryNote = buildSupplementaryNote(user.getId(), monthStart, monthEnd);

            List<Object> row = new ArrayList<>();
            row.add(user.getName());
            row.add(actualHours);
            row.add(workDays);
            row.add(expectedHours);
            row.add(diff);
            row.add(supplementaryNote);

            data.add(row);
            totalActualHours = totalActualHours.add(actualHours);
        }

        List<Object> totalRow = new ArrayList<>();
        totalRow.add("合计");
        totalRow.add(totalActualHours);
        totalRow.add("");
        totalRow.add("");
        totalRow.add("");
        totalRow.add("");
        data.add(totalRow);

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("月报详情_" + yearMonth, "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            
            EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("月报详情")
                .doWrite(data);
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }
    }

    private String buildSupplementaryNote(Long userId, LocalDate monthStart, LocalDate monthEnd) {
        LambdaQueryWrapper<LeaveRequest> leaveWrapper = new LambdaQueryWrapper<>();
        leaveWrapper.eq(LeaveRequest::getUserId, userId)
                .eq(LeaveRequest::getStatus, "APPROVED")
                .le(LeaveRequest::getStartDate, monthEnd)
                .ge(LeaveRequest::getEndDate, monthStart);
        List<LeaveRequest> leaves = leaveRequestMapper.selectList(leaveWrapper);

        List<String> notes = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M.d");

        for (LeaveRequest leave : leaves) {
            LocalDate startDate = leave.getStartDate().isBefore(monthStart) ? monthStart : leave.getStartDate();
            LocalDate endDate = leave.getEndDate().isAfter(monthEnd) ? monthEnd : leave.getEndDate();
            String leaveTypeCN = convertLeaveTypeToChinese(leave.getLeaveType());

            if (startDate.equals(endDate)) {
                notes.add(startDate.format(formatter) + "请" + leaveTypeCN + "假一天");
            } else {
                notes.add(startDate.format(formatter) + "-" + endDate.format(formatter) + "请" + leaveTypeCN + "假");
            }
        }

        return String.join("，", notes);
    }

    private String convertLeaveTypeToChinese(String leaveType) {
        if (leaveType == null) {
            return "";
        }
        switch (leaveType.toUpperCase()) {
            case "ANNUAL":
                return "年";
            case "SICK":
                return "病";
            case "PERSONAL":
                return "事";
            case "MARRIAGE":
                return "婚";
            case "MATERNITY":
                return "产";
            case "PATERNITY":
                return "陪产";
            case "BEREAVEMENT":
                return "丧";
            default:
                return leaveType;
        }
    }
}
