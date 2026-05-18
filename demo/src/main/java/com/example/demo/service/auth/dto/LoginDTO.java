package com.example.demo.service.auth.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
    private Integer userType; // 1租客 2房东 3管理员
}