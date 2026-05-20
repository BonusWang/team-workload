package com.teamworkload.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_report")
public class DailyReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate workDate;

    private String projectName;

    private String taskNo;

    private String taskName;

    private BigDecimal hours;

    private String workDescription;

    private String source;

    private String importBatchNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
