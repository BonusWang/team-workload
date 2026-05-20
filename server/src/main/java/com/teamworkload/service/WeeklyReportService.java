package com.teamworkload.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teamworkload.entity.WeeklyReport;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface WeeklyReportService extends IService<WeeklyReport> {

    WeeklyReport getCurrentWeekly(Long userId);

    List<WeeklyReport> getHistoryWeekly(Long userId);

    Map<String, Object> submitWeekly(Long userId, String nextWeekPlans, String notes);

    Map<String, Object> submitWeeklyForUser(Long operatorId, Long targetUserId, String nextWeekPlans, String notes);

    void updateWeeklyDetails(Long reportId, String field, String value);

    void updateWeeklyDetailsByUserId(Long operatorId, Long targetUserId, String field, String value);

    void updateCurrentWeeklyDetails(Long userId, String field, String value);

    WeeklyReport revokeWeekly(Long userId, Long reportId);

    Map<String, Object> getTeamWeeklyOverview(Long leaderId);

    Map<String, Object> getTeamWeeklyOverview(Long leaderId, String startDate, String endDate);

    List<Map<String, Object>> getTeamWeeklyRanking(Long leaderId);

    List<Map<String, Object>> getTeamWeeklyRanking(Long leaderId, String startDate, String endDate);

    void exportTeamWeekly(Long leaderId, HttpServletResponse response);

    List<Map<String, Object>> getTeamWeeklyDetails(Long leaderId);

    List<Map<String, Object>> getTeamWeeklyDetails(Long leaderId, String startDate, String endDate);

    void exportTeamWeeklyDetails(Long leaderId, HttpServletResponse response);

    void exportTeamWeeklyDetails(Long leaderId, String startDate, String endDate, HttpServletResponse response);
}
