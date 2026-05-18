package com.example.demo.config;

import com.example.demo.utils.JwtUtil;
import com.example.demo.utils.RedisCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RedisCache redisCache;

    public JwtInterceptor(JwtUtil jwtUtil, RedisCache redisCache) {
        this.jwtUtil = jwtUtil;
        this.redisCache = redisCache;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取token - 优先从Header获取，其次从URL参数获取（用于文件下载）
        String token = request.getHeader("Authorization");
        
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else {
            // 从URL参数获取token（用于window.open下载文件场景）
            token = request.getParameter("token");
        }
        
        if (token != null && !token.isEmpty()) {
            // 从Redis获取用户信息
            Object userInfoObj = redisCache.getToken(token);
            if (userInfoObj != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userInfo = (Map<String, Object>) userInfoObj;
                Object userIdObj = userInfo.get("userId");
                Object userTypeObj = userInfo.get("userType");
                if (userIdObj != null) {
                    Long userId;
                    if (userIdObj instanceof Integer) {
                        userId = ((Integer) userIdObj).longValue();
                    } else if (userIdObj instanceof Long) {
                        userId = (Long) userIdObj;
                    } else {
                        userId = Long.valueOf(userIdObj.toString());
                    }
                    request.setAttribute("userId", userId);
                    
                    // 设置用户类型
                    if (userTypeObj != null) {
                        Integer userType;
                        if (userTypeObj instanceof Integer) {
                            userType = (Integer) userTypeObj;
                        } else {
                            userType = Integer.valueOf(userTypeObj.toString());
                        }
                        request.setAttribute("userType", userType);
                    }
                }
            }
        }
        
        return true;
    }
}
