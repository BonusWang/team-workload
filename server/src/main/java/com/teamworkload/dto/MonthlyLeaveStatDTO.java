
package com.teamworkload.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyLeaveStatDTO {
    private String month;
    private String employee;
    private BigDecimal days;
}

