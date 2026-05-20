package com.teamworkload.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailyReportSubmitDTO {

    private String projectName;

    private String taskNo;

    private String taskName;

    private String workDate;

    private BigDecimal hours;

    private String workDescription;
}
