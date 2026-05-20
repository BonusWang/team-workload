package com.teamworkload.dto;

import lombok.Data;

@Data
public class TokenDTO {

    private String token;

    private Long userId;

    private String username;

    private String name;

    private String role;

    public TokenDTO(String token, Long userId, String username, String name, String role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.role = role;
    }
}
