package com.example.demo.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import com.example.demo.utils.PasswordUtil;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理端-用户管理接口
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final UserMapper userMapper;

    public AdminUserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 分页查询用户列表
     * @param userType 用户类型：1租客 2房东 3管理员
     * @param status 状态：0禁用 1正常
     * @param startDate 注册开始日期
     * @param endDate 注册结束日期
     * @param keyword 关键词（用户名/手机号）
     * @param page 页码
     * @param size 每页数量
     */
    @GetMapping("/list")
    public Result<IPage<User>> getUserList(
            @RequestParam Integer userType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserType, userType);

        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(User::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }

        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(User::getCreateTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getRealName, keyword));
        }

        wrapper.orderByDesc(User::getCreateTime);

        IPage<User> pageResult = userMapper.selectPage(new Page<>(page, size), wrapper);

        // 隐藏密码
        pageResult.getRecords().forEach(user -> user.setPassword(null));

        return Result.success(pageResult);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    public Result<User> getUserDetail(@PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.failure(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 禁用/启用用户
     */
    @PutMapping("/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.failure(400, "状态参数错误");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.failure(404, "用户不存在");
        }

        user.setStatus(status);
        userMapper.updateById(user);

        return Result.success("操作成功", null);
    }

    /**
     * 重置密码
     */
    @PutMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.failure(404, "用户不存在");
        }

        // 重置为默认密码 123456（BCrypt加密）
        user.setPassword(PasswordUtil.encode("123456"));
        userMapper.updateById(user);

        return Result.success("密码已重置为：123456", null);
    }

    /**
     * 添加管理员
     */
    @PostMapping("/admin")
    public Result<Void> addAdmin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("realName");
        String phone = body.get("phone");
        String email = body.get("email");

        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            return Result.failure(400, "用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.failure(400, "密码不能为空");
        }
        if (password.length() < 6) {
            return Result.failure(400, "密码长度不能少于6位");
        }

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.failure(400, "用户名已存在");
        }

        // 检查手机号是否已存在
        if (phone != null && !phone.trim().isEmpty()) {
            LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(User::getPhone, phone);
            if (userMapper.selectCount(phoneWrapper) > 0) {
                return Result.failure(400, "手机号已被使用");
            }
        }

        // 检查邮箱是否已存在
        if (email != null && !email.trim().isEmpty()) {
            LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(User::getEmail, email);
            if (userMapper.selectCount(emailWrapper) > 0) {
                return Result.failure(400, "邮箱已被使用");
            }
        }

        // 创建管理员
        User admin = new User();
        admin.setUsername(username.trim());
        admin.setPassword(PasswordUtil.encode(password));
        admin.setRealName(realName != null ? realName.trim() : null);
        admin.setPhone(phone != null ? phone.trim() : null);
        admin.setEmail(email != null ? email.trim() : null);
        admin.setUserType(3); // 管理员
        admin.setRealnameStatus(1); // 管理员默认已认证
        admin.setStatus(1); // 正常状态
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        userMapper.insert(admin);

        return Result.success("添加管理员成功", null);
    }
}
