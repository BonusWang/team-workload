package com.teamworkload.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class DailyReportImportDTO {

    @ExcelProperty("所属项目/产品")
    private String projectName;

    @ExcelProperty("任务号")
    private String taskNo;

    @ExcelProperty("任务名称")
    private String taskName;

    @ExcelProperty("工作日期")
    private String workDateStr;

    @ExcelProperty("实际工时")
    private String hoursStr;

    @ExcelProperty("工作描述")
    private String workDescription;

    @ExcelProperty("提交人")
    private String submitterName;
}
