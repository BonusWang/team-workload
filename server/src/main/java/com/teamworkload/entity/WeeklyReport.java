package com.teamworkload.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("weekly_report")
public class WeeklyReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate weekStartDate;

    private LocalDate weekEndDate;

    private BigDecimal totalHours;

    private String status;

    private String thisWeekTasks;

    private String nextWeekPlans;

    private String notes;

    private LocalDateTime submittedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
