package com.teamworkload.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyReportQueryDTO {

    private Long userId;

    private LocalDate startDate;

    private LocalDate endDate;

    private String projectName;
    
    private String source;
}
