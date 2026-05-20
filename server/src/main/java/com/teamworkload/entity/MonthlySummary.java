package com.teamworkload.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("monthly_summary")
public class MonthlySummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField("`year_month`")
    private String yearMonth;

    private BigDecimal totalHours;

    private Integer workDays;

    private BigDecimal leaveDays;

    private BigDecimal dailyAvg;

    private String taskTypeDist;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
