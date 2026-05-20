package com.teamworkload.dto;

import lombok.Data;

@Data
public class UserQueryDTO {

    private String name;

    private String role;

    private Integer status;

    private Long leaderId;
}
