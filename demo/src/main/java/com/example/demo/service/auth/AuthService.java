package com.example.demo.service.auth;

import com.example.demo.entity.Result;
import com.example.demo.service.auth.dto.LoginDTO;
import com.example.demo.service.auth.dto.RegisterDTO;

import java.util.Map;

public interface AuthService{
    Result<Map<String, Object>> login(LoginDTO loginDTO);

    Result register(RegisterDTO registerDTO);

    Result<Map<String, Object>> getCurrentUser(String token);

    Result logout(String token);

    Result<Map<String, String>> preCheck(LoginDTO loginDTO);
}
