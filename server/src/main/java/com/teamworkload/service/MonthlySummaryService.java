package com.teamworkload.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teamworkload.entity.MonthlySummary;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface MonthlySummaryService extends IService<MonthlySummary> {

    MonthlySummary getPersonalSummary(Long userId, String yearMonth);

    List<MonthlySummary> getPersonalHistory(Long userId);

    Map<String, Object> getTeamSummary(Long leaderId, String yearMonth);

    void exportTeamSummary(Long leaderId, String yearMonth, HttpServletResponse response);

    List<Map<String, Object>> getTeamMonthlyDetails(String yearMonth);

    void exportTeamMonthlyDetails(String yearMonth, HttpServletResponse response);
}
