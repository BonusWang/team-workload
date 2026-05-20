package com.teamworkload.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class QuankaiImportDTO {

    @ExcelProperty(index = 0)
    private String projectName;

    @ExcelProperty(index = 6)
    private String taskNo;

    @ExcelProperty(index = 7)
    private String taskName;

    @ExcelProperty(index = 15)
    private String workDateStr;

    @ExcelProperty(index = 14)
    private String hoursStr;

    @ExcelProperty(index = 16)
    private String workDescription;

    @ExcelProperty(index = 22)
    private String submitterName;
}
