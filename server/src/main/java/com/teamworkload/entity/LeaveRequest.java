package com.teamworkload.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("leave_request")
public class LeaveRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String leaveType;

    private LocalDate startDate;

    private String startTime;

    private LocalDate endDate;

    private String endTime;

    private BigDecimal days;

    private String reason;

    private Long approverId;

    private String status;

    private String rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime approveTime;
}
