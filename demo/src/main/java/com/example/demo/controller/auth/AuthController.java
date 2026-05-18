package com.example.demo.controller.auth;

import com.example.demo.service.auth.dto.LoginDTO;
import com.example.demo.service.auth.dto.RegisterDTO;
import com.example.demo.entity.Result;
import com.example.demo.service.auth.AuthService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录接口 - 明文密码验证
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    /**
     * 注册接口 - 明文密码存储
     */
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    /**
     * 获取当前用户信息（根据token）
     */
    @GetMapping("/current")
    public Result getCurrentUser(@RequestHeader("Authorization") String token) {
        return authService.getCurrentUser(token);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "Authorization", required = false) String token) {
        return authService.logout(token);
    }

    /**
     * 预检查用户信息（用于头像显示）- 明文密码验证
     */
    @PostMapping("/pre-check")
    public Result<Map<String, String>> preCheck(@RequestBody LoginDTO loginDTO) {
        return authService.preCheck(loginDTO);
    }
}