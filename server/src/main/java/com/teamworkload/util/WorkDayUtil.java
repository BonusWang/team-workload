package com.teamworkload.util;

import com.teamworkload.entity.WorkDay;
import com.teamworkload.mapper.WorkDayMapper;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class WorkDayUtil {

    private static WorkDayMapper workDayMapper;

    public WorkDayUtil(WorkDayMapper workDayMapper) {
        WorkDayUtil.workDayMapper = workDayMapper;
    }

    public static int calculateWorkDays(LocalDate startDate, LocalDate endDate) {
        WorkDay wd = workDayMapper.selectByDate(startDate);
        if (wd != null) {
            return workDayMapper.countWorkDaysBetween(startDate, endDate);
        }
        int count = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    public static boolean isWorkDay(LocalDate date) {
        WorkDay wd = workDayMapper.selectByDate(date);
        if (wd != null) {
            return wd.getIsWorkday() == 1;
        }
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    public static LocalDate[] getWeekRange(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        int diff = dow.getValue() - DayOfWeek.THURSDAY.getValue();
        LocalDate thisThursday = date.minusDays(diff);
        LocalDate lastThursday = thisThursday.minusWeeks(1);
        LocalDate thisWednesday = thisThursday.minusDays(1);
        return new LocalDate[]{lastThursday, thisWednesday};
    }
}
