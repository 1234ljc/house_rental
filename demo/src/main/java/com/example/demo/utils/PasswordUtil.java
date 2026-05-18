package com.example.demo.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类，统一使用 BCrypt 加密
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 加密密码 */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /** 验证密码 */
    public static boolean matches(String rawPassword, String encodedPassword) {
        // 兼容旧明文密码（迁移期间）
        if (encodedPassword != null && !encodedPassword.startsWith("$2a$") && !encodedPassword.startsWith("$2b$")) {
            return rawPassword.equals(encodedPassword);
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}
