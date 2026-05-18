package com.example.demo.service.auth.impl;

import com.example.demo.entity.Result;
import com.example.demo.service.auth.dto.LoginDTO;
import com.example.demo.service.auth.dto.RegisterDTO;
import com.example.demo.service.auth.AuthService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import com.example.demo.utils.JwtUtil;
import com.example.demo.utils.PasswordUtil;
import com.example.demo.utils.RedisCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
// 负责系统登录、注册和当前用户信息等认证相关功能实现。
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisCache redisCache;

    public AuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil, RedisCache redisCache) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.redisCache = redisCache;
    }



    // 登录主流程：校验账号密码和账号状态，签发 JWT，并将用户信息缓存到 Redis。
    @Override
    public Result<Map<String, Object>> login(LoginDTO loginDTO) {
        User user = userMapper.selectByUsernameAndType(loginDTO.getUsername(), loginDTO.getUserType());
        if (user == null) {
            return Result.failure(400, "用户名或密码错误");
        }
        if (!PasswordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            return Result.failure(400, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.failure(400, "账号已被禁用");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType());
        String token = jwtUtil.generateToken(claims);

        Map<String, Object> userInfo = buildUserInfo(user);
        redisCache.setToken(token, userInfo);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return Result.success("登录成功", result);
    }

    // 注册主流程：做重复校验和密码一致性校验，通过后创建用户并加密入库。
    @Override
    public Result register(RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return Result.failure(400, "两次输入密码不一致");
        }
        if (userMapper.selectByUsernameAndType(registerDTO.getUsername(), registerDTO.getUserType()) != null) {
            return Result.failure(400, "用户名已存在");
        }
        if (registerDTO.getPhone() != null && userMapper.selectByPhone(registerDTO.getPhone()) != null) {
            return Result.failure(400, "手机号已被注册");
        }
        if (registerDTO.getEmail() != null && userMapper.selectByEmail(registerDTO.getEmail()) != null) {
            return Result.failure(400, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(PasswordUtil.encode(registerDTO.getPassword()));
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setUserType(registerDTO.getUserType());
        user.setRealnameStatus(-1);
        user.setStatus(1);
        userMapper.insert(user);
        return Result.success("注册成功");
    }
    // 当前用户查询流程：先验证 token，再读取缓存，最后回表获取最新用户信息。
    @Override
    public Result<Map<String, Object>> getCurrentUser(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.failure(401, "未登录");
        }
        token = token.substring(7);
        Object cachedInfo = redisCache.getToken(token);
        if (cachedInfo == null) {
            return Result.failure(401, "登录已过期");
        }
        Map<String, Object> cachedMap = (Map<String, Object>) cachedInfo;
        Long userId = Long.valueOf(cachedMap.get("userId").toString());
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.failure(401, "用户不存在");
        }
        return Result.success(buildUserInfo(user));
    }
    // 退出登录流程：删除 Redis 中的 token 缓存，使 token 立即失效。
    @Override
    public Result logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            redisCache.deleteToken(token.substring(7));
        }
        return Result.success("退出成功");
    }
    // 登录预检查流程：验证账号密码，返回基础信息供前端登录前展示。
    @Override
    public Result<Map<String, String>> preCheck(LoginDTO loginDTO) {
        User user = userMapper.selectByUsernameAndType(loginDTO.getUsername(), loginDTO.getUserType());
        if (user == null) {
            return Result.failure(400, "用户不存在");
        }
        if (!PasswordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            return Result.failure(400, "密码错误");
        }
        Map<String, String> result = new HashMap<>();
        result.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        return Result.success(result);
    }

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("username", user.getUsername());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("realName", user.getRealName());
        userInfo.put("idCard", user.getIdCard());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("userType", user.getUserType());
        userInfo.put("realnameStatus", user.getRealnameStatus());
        userInfo.put("status", user.getStatus());
        return userInfo;
    }
}
