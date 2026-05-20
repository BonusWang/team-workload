package com.teamworkload.controller;

import com.teamworkload.common.Result;
import com.teamworkload.entity.WorkDay;
import com.teamworkload.service.WorkDayService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/work-day")
public class WorkDayController {

    private final WorkDayService workDayService;

    public WorkDayController(WorkDayService workDayService) {
        this.workDayService = workDayService;
    }

    @PostMapping("/sync")
    public Result<Void> sync(@RequestParam Integer year) {
        workDayService.syncYear(year);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<WorkDay>> list(@RequestParam Integer year,
                                      @RequestParam(required = false) Integer month) {
        List<WorkDay> list = workDayService.listByYearAndMonth(year, month);
        return Result.success(list);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestParam Integer isWorkday,
                               @RequestParam String type,
                               @RequestParam(required = false) String description) {
        workDayService.updateWorkDay(id, isWorkday, type, description);
        return Result.success();
    }
}
