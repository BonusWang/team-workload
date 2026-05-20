package com.teamworkload.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamworkload.common.BusinessException;
import com.teamworkload.entity.WorkDay;
import com.teamworkload.mapper.WorkDayMapper;
import com.teamworkload.service.WorkDayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WorkDayServiceImpl extends ServiceImpl<WorkDayMapper, WorkDay> implements WorkDayService {

    private static final String HOLIDAY_API_URL = "https://timor.tech/api/holiday/year/{year}";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WorkDayServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public void syncYear(Integer year) {
        LambdaQueryWrapper<WorkDay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkDay::getYear, year);
        remove(wrapper);

        java.util.Map<Integer, HolidayInfo> holidayMap = fetchHolidaysFromApi(year);

        List<WorkDay> workDays = new ArrayList<>();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            WorkDay wd = new WorkDay();
            wd.setDate(date);
            wd.setYear(year);

            HolidayInfo holidayInfo = holidayMap.get(date.getDayOfYear());

            if (holidayInfo != null) {
                wd.setIsWorkday(holidayInfo.isWorkday ? 1 : 0);
                wd.setType(holidayInfo.type);
                wd.setDescription(holidayInfo.name);
            } else {
                DayOfWeek dow = date.getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    wd.setIsWorkday(0);
                    wd.setType(WorkDay.TYPE_WEEKEND);
                    wd.setDescription("周末");
                } else {
                    wd.setIsWorkday(1);
                    wd.setType(WorkDay.TYPE_WORKDAY);
                    wd.setDescription("工作日");
                }
            }

            workDays.add(wd);
        }

        saveBatch(workDays);
        log.info("同步{}年工作日数据完成，共{}条", year, workDays.size());
    }

    private java.util.Map<Integer, HolidayInfo> fetchHolidaysFromApi(Integer year) {
        java.util.Map<Integer, HolidayInfo> holidayMap = new java.util.HashMap<>();
        try {
            String response = restTemplate.getForObject(HOLIDAY_API_URL, String.class, year);
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode holidayNode = root.get("holiday");
                if (holidayNode != null && holidayNode.isObject()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    holidayNode.fields().forEachRemaining(entry -> {
                        try {
                            String dateStr = entry.getKey();
                            JsonNode info = entry.getValue();
                            LocalDate date = LocalDate.parse(dateStr, formatter);
                            boolean isHoliday = info.has("holiday") && info.get("holiday").asBoolean();
                            String name = info.has("name") ? info.get("name").asText() : "";

                            HolidayInfo hi = new HolidayInfo();
                            hi.name = name;
                            if (isHoliday) {
                                hi.isWorkday = false;
                                hi.type = WorkDay.TYPE_HOLIDAY;
                            } else {
                                hi.isWorkday = true;
                                hi.type = WorkDay.TYPE_MAKEUP;
                            }
                            holidayMap.put(date.getDayOfYear(), hi);
                        } catch (Exception e) {
                            log.warn("解析节假日数据失败: {}", entry.getKey(), e);
                        }
                    });
                }
                log.info("从API获取{}年节假日数据成功，共{}条", year, holidayMap.size());
            }
        } catch (Exception e) {
            log.warn("调用节假日API失败，使用本地算法计算: {}", e.getMessage());
        }
        return holidayMap;
    }

    @Override
    public List<WorkDay> listByYearAndMonth(Integer year, Integer month) {
        if (month != null) {
            return baseMapper.selectByYearAndMonth(year, month);
        }
        LambdaQueryWrapper<WorkDay> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkDay::getYear, year).orderByAsc(WorkDay::getDate);
        return list(wrapper);
    }

    @Override
    public void updateWorkDay(Long id, Integer isWorkday, String type, String description) {
        WorkDay workDay = getById(id);
        if (workDay == null) {
            throw new BusinessException("工作日记录不存在");
        }
        workDay.setIsWorkday(isWorkday);
        workDay.setType(type);
        workDay.setDescription(description);
        updateById(workDay);
    }

    private static class HolidayInfo {
        boolean isWorkday;
        String type;
        String name;
    }
}
