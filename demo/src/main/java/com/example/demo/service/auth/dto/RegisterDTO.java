package com.example.demo.service.auth.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String confirmPassword;
    private String phone;
    private String email;
    private Integer userType; // 1租客 2房东
}