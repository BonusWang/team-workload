package com.teamworkload.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    Map<String, Object> getPersonalDashboard(Long userId);

    List<Map<String, Object>> getCalendarData(Long userId, String yearMonth);
}
