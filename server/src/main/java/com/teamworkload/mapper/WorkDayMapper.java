package com.teamworkload.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamworkload.entity.WorkDay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WorkDayMapper extends BaseMapper<WorkDay> {

    @Select("SELECT * FROM work_day WHERE year = #{year} AND MONTH(date) = #{month} ORDER BY date")
    List<WorkDay> selectByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    @Select("SELECT * FROM work_day WHERE date BETWEEN #{startDate} AND #{endDate} AND is_workday = 1")
    List<WorkDay> selectWorkDaysBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM work_day WHERE date = #{date}")
    WorkDay selectByDate(@Param("date") LocalDate date);

    @Select("SELECT COUNT(*) FROM work_day WHERE date BETWEEN #{startDate} AND #{endDate} AND is_workday = 1")
    int countWorkDaysBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("DELETE FROM work_day WHERE year = #{year}")
    void deleteByYear(@Param("year") Integer year);
}
