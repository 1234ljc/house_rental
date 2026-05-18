package com.example.demo.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.repository.user.entity.RealnameAuth;
import com.example.demo.entity.Result;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.RealnameAuthMapper;
import com.example.demo.repository.user.UserMapper;
import com.example.demo.utils.PasswordUtil;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/profile")
public class TenantProfileController {

    private final UserMapper userMapper;
    private final RealnameAuthMapper realnameAuthMapper;

    public TenantProfileController(UserMapper userMapper, RealnameAuthMapper realnameAuthMapper) {
        this.userMapper = userMapper;
        this.realnameAuthMapper = realnameAuthMapper;
    }

    /**
     * 获取当前用户的实名认证信息
     */
    @GetMapping("/realname")
    public Result<Map<String, Object>> getRealnameInfo(@RequestAttribute("userId") Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取用户信息
        User user = userMapper.selectById(userId);
        result.put("realnameStatus", user.getRealnameStatus());
        result.put("realnameTime", user.getRealnameTime());
        result.put("auditReason", user.getRealnameAuditReason());
        // 返回用户表中的真实姓名和身份证（这是最终生效的数据）
        result.put("realName", user.getRealName());
        result.put("idCard", user.getIdCard());
        
        // 获取最新的实名认证申请
        LambdaQueryWrapper<RealnameAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RealnameAuth::getUserId, userId)
               .orderByDesc(RealnameAuth::getCreateTime)
               .last("LIMIT 1");
        RealnameAuth auth = realnameAuthMapper.selectOne(wrapper);
        
        if (auth != null) {
            result.put("authInfo", auth);
        }
        
        return Result.success(result);
    }

    /**
     * 提交实名认证申请
     */
    @PostMapping("/realname/submit")
    public Result<String> submitRealnameAuth(@RequestAttribute("userId") Long userId,
                                              @RequestBody RealnameAuthDTO dto) {
        // 检查是否已有待审核的申请
        LambdaQueryWrapper<RealnameAuth> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RealnameAuth::getUserId, userId)
               .eq(RealnameAuth::getAuthStatus, 0);
        Long count = realnameAuthMapper.selectCount(wrapper);
        if (count > 0) {
            return Result.failure("您已有待审核的实名认证申请，请等待审核结果");
        }
        
        // 创建新的实名认证申请
        RealnameAuth auth = new RealnameAuth();
        auth.setUserId(userId);
        auth.setRealName(dto.getRealName());
        auth.setIdCard(dto.getIdCard());
        auth.setIdCardFront(dto.getIdCardFront());
        auth.setIdCardBack(dto.getIdCardBack());
        auth.setAuthStatus(0); // 待审核
        auth.setCreateTime(LocalDateTime.now());
        
        realnameAuthMapper.insert(auth);
        
        // 更新用户实名状态为审核中
        User user = new User();
        user.setUserId(userId);
        user.setRealnameStatus(0); // 0-待审核
        user.setRealName(dto.getRealName());
        user.setIdCard(dto.getIdCard());
        userMapper.updateById(user);
        
        return Result.success("实名认证申请已提交，请等待审核");
    }

    /**
     * 获取个人信息
     */
    @GetMapping("/info")
    public Result<User> getPersonalInfo(@RequestAttribute("userId") Long userId) {
        User user = userMapper.selectById(userId);
        user.setPassword(null); // 不返回密码
        return Result.success(user);
    }

    /**
     * 更新个人信息（头像、昵称、真实姓名、手机号、邮箱）
     */
    @PutMapping("/info")
    public Result<String> updatePersonalInfo(@RequestAttribute("userId") Long userId,
                                              @RequestBody UpdateInfoDTO dto) {
        User user = new User();
        user.setUserId(userId);
        
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            // 检查用户名是否已存在
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, dto.getUsername())
                   .ne(User::getUserId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                return Result.failure("用户名已被使用");
            }
            user.setUsername(dto.getUsername());
        }
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            // 检查手机号是否已存在
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, dto.getPhone())
                   .ne(User::getUserId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                return Result.failure("手机号已被使用");
            }
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            // 检查邮箱是否已存在
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, dto.getEmail())
                   .ne(User::getUserId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                return Result.failure("邮箱已被使用");
            }
            user.setEmail(dto.getEmail());
        }
        
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        return Result.success("个人信息更新成功");
    }

    /**
     * 修改手机号（需验证）
     */
    @PutMapping("/phone")
    public Result<String> updatePhone(@RequestAttribute("userId") Long userId,
                                       @RequestBody UpdatePhoneDTO dto) {
        // 实际项目中需要验证短信验证码
        // 这里简化处理，直接更新
        
        // 检查手机号是否已被使用
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, dto.getPhone())
               .ne(User::getUserId, userId);
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.failure("该手机号已被其他账号使用");
        }
        
        User user = new User();
        user.setUserId(userId);
        user.setPhone(dto.getPhone());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        return Result.success("手机号修改成功");
    }

    /**
     * 修改邮箱（需验证）
     */
    @PutMapping("/email")
    public Result<String> updateEmail(@RequestAttribute("userId") Long userId,
                                       @RequestBody UpdateEmailDTO dto) {
        // 实际项目中需要验证邮箱验证码
        // 这里简化处理，直接更新
        
        // 检查邮箱是否已被使用
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, dto.getEmail())
                   .ne(User::getUserId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                return Result.failure("该邮箱已被其他账号使用");
            }
        }
        
        User user = new User();
        user.setUserId(userId);
        user.setEmail(dto.getEmail());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        return Result.success("邮箱修改成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<String> updatePassword(@RequestAttribute("userId") Long userId,
                                          @RequestBody UpdatePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        
        // 验证旧密码
        if (!PasswordUtil.matches(dto.getOldPassword(), user.getPassword())) {
            return Result.failure("原密码错误");
        }
        
        // 更新密码（BCrypt加密）
        User updateUser = new User();
        updateUser.setUserId(userId);
        updateUser.setPassword(PasswordUtil.encode(dto.getNewPassword()));
        updateUser.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(updateUser);
        
        return Result.success("密码修改成功");
    }

    /**
     * 修改身份证（同步更新用户表和实名认证表）
     */
    @PutMapping("/idcard")
    public Result<String> updateIdCard(@RequestAttribute("userId") Long userId,
                                        @RequestBody UpdateIdCardDTO dto) {
        User user = userMapper.selectById(userId);
        
        // 只有已认证的用户才能修改身份证
        if (user.getRealnameStatus() != 1) {
            return Result.failure("只有已完成实名认证的用户才能修改身份证");
        }
        
        // 检查身份证是否已被使用
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getIdCard, dto.getIdCard())
               .ne(User::getUserId, userId);
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.failure("该身份证号已被其他账号使用");
        }
        
        // 更新用户表身份证，并将实名状态改为待审核
        User updateUser = new User();
        updateUser.setUserId(userId);
        updateUser.setIdCard(dto.getIdCard());
        updateUser.setRealnameStatus(0); // 改为待审核
        updateUser.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(updateUser);
        
        // 更新实名认证表中最新的记录
        LambdaQueryWrapper<RealnameAuth> authWrapper = new LambdaQueryWrapper<>();
        authWrapper.eq(RealnameAuth::getUserId, userId)
                   .orderByDesc(RealnameAuth::getCreateTime)
                   .last("LIMIT 1");
        RealnameAuth auth = realnameAuthMapper.selectOne(authWrapper);
        if (auth != null) {
            RealnameAuth updateAuth = new RealnameAuth();
            updateAuth.setAuthId(auth.getAuthId());
            updateAuth.setIdCard(dto.getIdCard());
            updateAuth.setAuthStatus(0); // 改为待审核
            realnameAuthMapper.updateById(updateAuth);
        }
        
        return Result.success("身份证修改成功，需要重新审核");
    }

    /**
     * 获取自己的名片
     */
    @GetMapping("/card")
    public Result<Map<String, Object>> getMyCard(@RequestAttribute("userId") Long userId) {
        User user = userMapper.selectById(userId);
        return Result.success(buildUserCard(user));
    }

    /**
     * 获取他人名片
     */
    @GetMapping("/card/{targetUserId}")
    public Result<Map<String, Object>> getUserCard(@PathVariable Long targetUserId) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            return Result.failure("用户不存在");
        }
        return Result.success(buildUserCard(user));
    }

    /**
     * 构建用户名片信息
     */
    private Map<String, Object> buildUserCard(User user) {
        Map<String, Object> card = new HashMap<>();
        card.put("userId", user.getUserId());
        card.put("username", user.getUsername());
        card.put("avatar", user.getAvatar());
        card.put("realName", user.getRealName());
        card.put("phone", user.getPhone());
        card.put("email", user.getEmail());
        card.put("userType", user.getUserType());
        card.put("realnameStatus", user.getRealnameStatus());
        card.put("createTime", user.getCreateTime());
        return card;
    }

    // DTO classes
    @Data
    public static class RealnameAuthDTO {
        private String realName;
        private String idCard;
        private String idCardFront;
        private String idCardBack;
    }

    @Data
    public static class UpdateInfoDTO {
        private String avatar;
        private String username;
        private String realName;
        private String phone;
        private String email;
    }

    @Data
    public static class UpdatePhoneDTO {
        private String phone;
        private String verifyCode;
    }

    @Data
    public static class UpdateEmailDTO {
        private String email;
        private String verifyCode;
    }

    @Data
    public static class UpdatePasswordDTO {
        private String oldPassword;
        private String newPassword;
    }

    @Data
    public static class UpdateIdCardDTO {
        private String idCard;
    }
}
