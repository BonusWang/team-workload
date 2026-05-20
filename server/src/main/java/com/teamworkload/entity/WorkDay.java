package com.teamworkload.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("work_day")
public class WorkDay {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("`date`")
    private LocalDate date;

    private Integer isWorkday;

    private String type;

    private String description;

    private Integer year;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public static final String TYPE_WORKDAY = "WORKDAY";
    public static final String TYPE_WEEKEND = "WEEKEND";
    public static final String TYPE_HOLIDAY = "HOLIDAY";
    public static final String TYPE_MAKEUP = "MAKEUP";
}
