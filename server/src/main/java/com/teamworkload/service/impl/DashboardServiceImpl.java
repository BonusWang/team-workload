package com.teamworkload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teamworkload.entity.DailyReport;
import com.teamworkload.entity.LeaveRequest;
import com.teamworkload.entity.SysUser;
import com.teamworkload.entity.WeeklyReport;
import com.teamworkload.mapper.DailyReportMapper;
import com.teamworkload.mapper.LeaveRequestMapper;
import com.teamworkload.mapper.SysUserMapper;
import com.teamworkload.mapper.WeeklyReportMapper;
import com.teamworkload.service.DashboardService;
import com.teamworkload.util.WorkDayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private final DailyReportMapper dailyReportMapper;
    private final WeeklyReportMapper weeklyReportMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final SysUserMapper sysUserMapper;
    private final WorkDayUtil workDayUtil;

    public DashboardServiceImpl(DailyReportMapper dailyReportMapper, WeeklyReportMapper weeklyReportMapper,
                                LeaveRequestMapper leaveRequestMapper, SysUserMapper sysUserMapper, WorkDayUtil workDayUtil) {
        this.dailyReportMapper = dailyReportMapper;
        this.weeklyReportMapper = weeklyReportMapper;
        this.leaveRequestMapper = leaveRequestMapper;
        this.sysUserMapper = sysUserMapper;
        this.workDayUtil = workDayUtil;
    }

    @Override
    public Map<String, Object> getPersonalDashboard(Long userId) {
        LocalDate[] weekRange = workDayUtil.getWeekRange(LocalDate.now());
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        LambdaQueryWrapper<DailyReport> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(DailyReport::getUserId, userId)
                .ge(DailyReport::getWorkDate, weekRange[0])
                .le(DailyReport::getWorkDate, weekRange[1]);
        List<DailyReport> weekReports = dailyReportMapper.selectList(weekWrapper);
        BigDecimal weekHours = weekReports.stream()
                .map(DailyReport::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<DailyReport> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(DailyReport::getUserId, userId)
                .ge(DailyReport::getWorkDate, monthStart)
                .le(DailyReport::getWorkDate, monthEnd);
        List<DailyReport> monthReports = dailyReportMapper.selectList(monthWrapper);
        BigDecimal monthHours = monthReports.stream()
                .map(DailyReport::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LambdaQueryWrapper<LeaveRequest> leaveWrapper = new LambdaQueryWrapper<>();
        leaveWrapper.eq(LeaveRequest::getUserId, userId)
                .eq(LeaveRequest::getStatus, "APPROVED")
                .le(LeaveRequest::getStartDate, monthEnd)
                .ge(LeaveRequest::getEndDate, monthStart);
        List<LeaveRequest> leaves = leaveRequestMapper.selectList(leaveWrapper);
        BigDecimal leaveDays = leaves.stream()
                .map(LeaveRequest::getDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String saturation = "MEDIUM";
        if (weekHours.compareTo(new BigDecimal("50")) > 0) saturation = "HIGH";
        else if (weekHours.compareTo(new BigDecimal("30")) < 0) saturation = "LOW";

        LambdaQueryWrapper<WeeklyReport> historyWrapper = new LambdaQueryWrapper<>();
        historyWrapper.eq(WeeklyReport::getUserId, userId)
                .orderByDesc(WeeklyReport::getWeekEndDate)
                .last("LIMIT 3");
        List<WeeklyReport> recentReports = weeklyReportMapper.selectList(historyWrapper);

        Map<String, BigDecimal> projectDist = new LinkedHashMap<>();
        for (DailyReport dr : monthReports) {
            projectDist.merge(dr.getProjectName(), dr.getHours(), BigDecimal::add);
        }

        SysUser user = sysUserMapper.selectById(userId);
        BigDecimal annualBalance = user != null && user.getAnnualLeaveBalance() != null
                ? user.getAnnualLeaveBalance() : BigDecimal.ZERO;

        Map<String, Object> result = new HashMap<>();
        result.put("weekHours", weekHours);
        result.put("monthHours", monthHours);
        result.put("leaveDays", leaveDays);
        result.put("annualLeaveBalance", annualBalance);
        result.put("saturation", saturation);
        result.put("recentReports", recentReports);
        result.put("projectDistribution", projectDist);
        result.put("weekStartDate", weekRange[0].toString());
        result.put("weekEndDate", weekRange[1].toString());
        return result;
    }

    @Override
    public List<Map<String, Object>> getCalendarData(Long userId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        LambdaQueryWrapper<DailyReport> reportWrapper = new LambdaQueryWrapper<>();
        reportWrapper.eq(DailyReport::getUserId, userId)
                .ge(DailyReport::getWorkDate, start)
                .le(DailyReport::getWorkDate, end);
        List<DailyReport> reports = dailyReportMapper.selectList(reportWrapper);
        Map<LocalDate, BigDecimal> hoursByDate = new HashMap<>();
        for (DailyReport dr : reports) {
            hoursByDate.merge(dr.getWorkDate(), dr.getHours(), BigDecimal::add);
        }

        LambdaQueryWrapper<LeaveRequest> leaveWrapper = new LambdaQueryWrapper<>();
        leaveWrapper.eq(LeaveRequest::getUserId, userId)
                .eq(LeaveRequest::getStatus, "APPROVED")
                .le(LeaveRequest::getStartDate, end)
                .ge(LeaveRequest::getEndDate, start);
        List<LeaveRequest> leaves = leaveRequestMapper.selectList(leaveWrapper);
        Set<LocalDate> leaveDates = new HashSet<>();
        for (LeaveRequest lr : leaves) {
            LocalDate d = lr.getStartDate();
            while (!d.isAfter(lr.getEndDate())) {
                leaveDates.add(d);
                d = d.plusDays(1);
            }
        }

        List<Map<String, Object>> calendar = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", date.toString());
            day.put("hours", hoursByDate.getOrDefault(date, BigDecimal.ZERO));
            day.put("isWorkday", workDayUtil.isWorkDay(date));
            day.put("isLeave", leaveDates.contains(date));
            day.put("isToday", date.equals(LocalDate.now()));

            String status = "NORMAL";
            if (!workDayUtil.isWorkDay(date)) {
                status = "NONWORKDAY";
            } else if (leaveDates.contains(date)) {
                status = "LEAVE";
            } else if (hoursByDate.getOrDefault(date, BigDecimal.ZERO).compareTo(BigDecimal.ZERO) == 0) {
                status = "UNFILLED";
            } else if (hoursByDate.getOrDefault(date, BigDecimal.ZERO).compareTo(new BigDecimal("8")) < 0) {
                status = "INSUFFICIENT";
            }
            day.put("status", status);
            calendar.add(day);
        }
        return calendar;
    }
}
