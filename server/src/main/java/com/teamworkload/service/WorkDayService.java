package com.teamworkload.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.teamworkload.entity.WorkDay;

import java.util.List;

public interface WorkDayService extends IService<WorkDay> {

    void syncYear(Integer year);

    List<WorkDay> listByYearAndMonth(Integer year, Integer month);

    void updateWorkDay(Long id, Integer isWorkday, String type, String description);
}
