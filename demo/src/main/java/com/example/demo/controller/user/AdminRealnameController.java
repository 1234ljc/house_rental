package com.example.demo.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.repository.user.entity.RealnameAuth;
import com.example.demo.entity.Result;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.RealnameAuthMapper;
import com.example.demo.repository.user.UserMapper;
import com.example.demo.service.notification.NotificationService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端-实名认证审核接口
 */
@RestController
@RequestMapping("/api/admin/realname")
public class AdminRealnameController {

    private final RealnameAuthMapper realnameAuthMapper;
    private final UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    public AdminRealnameController(RealnameAuthMapper realnameAuthMapper, UserMapper userMapper) {
        this.realnameAuthMapper = realnameAuthMapper;
        this.userMapper = userMapper;
    }

    /**
     * 分页查询实名认证申请列表
     */
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> getRealnameList(
            @RequestParam(required = false) Integer authStatus,
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        LambdaQueryWrapper<RealnameAuth> wrapper = new LambdaQueryWrapper<>();

        if (authStatus != null) {
            wrapper.eq(RealnameAuth::getAuthStatus, authStatus);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(RealnameAuth::getRealName, keyword)
                    .or().like(RealnameAuth::getIdCard, keyword));
        }

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(RealnameAuth::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }

        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(RealnameAuth::getCreateTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }

        wrapper.orderByDesc(RealnameAuth::getCreateTime);

        IPage<RealnameAuth> authPage = realnameAuthMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为包含用户信息的结果
        IPage<Map<String, Object>> resultPage = new Page<>(page, size, authPage.getTotal());
        resultPage.setRecords(authPage.getRecords().stream().map(auth -> {
            Map<String, Object> map = new HashMap<>();
            map.put("authId", auth.getAuthId());
            map.put("userId", auth.getUserId());
            map.put("realName", auth.getRealName());
            map.put("idCard", auth.getIdCard());
            map.put("idCardFront", auth.getIdCardFront());
            map.put("idCardBack", auth.getIdCardBack());
            map.put("authStatus", auth.getAuthStatus());
            map.put("auditReason", auth.getAuditReason());
            map.put("auditTime", auth.getAuditTime());
            map.put("createTime", auth.getCreateTime());

            // 获取用户信息
            User user = userMapper.selectById(auth.getUserId());
            if (user != null) {
                map.put("username", user.getUsername());
                map.put("phone", user.getPhone());
                map.put("userType", user.getUserType());
                // 根据userType过滤
                if (userType != null && !userType.equals(user.getUserType())) {
                    return null;
                }
            }
            return map;
        }).filter(m -> m != null).toList());

        return Result.success(resultPage);
    }

    /**
     * 获取实名认证详情
     */
    @GetMapping("/{authId}")
    public Result<Map<String, Object>> getRealnameDetail(@PathVariable Long authId) {
        RealnameAuth auth = realnameAuthMapper.selectById(authId);
        if (auth == null) {
            return Result.failure(404, "认证记录不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("authId", auth.getAuthId());
        result.put("userId", auth.getUserId());
        result.put("realName", auth.getRealName());
        result.put("idCard", auth.getIdCard());
        result.put("idCardFront", auth.getIdCardFront());
        result.put("idCardBack", auth.getIdCardBack());
        result.put("authStatus", auth.getAuthStatus());
        result.put("auditReason", auth.getAuditReason());
        result.put("auditTime", auth.getAuditTime());
        result.put("createTime", auth.getCreateTime());

        // 获取用户信息
        User user = userMapper.selectById(auth.getUserId());
        if (user != null) {
            result.put("username", user.getUsername());
            result.put("phone", user.getPhone());
            result.put("email", user.getEmail());
            result.put("userType", user.getUserType());
            result.put("userCreateTime", user.getCreateTime());
        }

        return Result.success(result);
    }

    /**
     * 审核实名认证
     */
    @PutMapping("/{authId}/audit")
    public Result<String> auditRealname(@PathVariable Long authId,
                                         @RequestAttribute("userId") Long auditorId,
                                         @RequestBody AuditDTO dto) {
        RealnameAuth auth = realnameAuthMapper.selectById(authId);
        if (auth == null) {
            return Result.failure(404, "认证记录不存在");
        }

        if (auth.getAuthStatus() != 0) {
            return Result.failure(400, "该认证申请已审核");
        }

        // 更新认证记录
        auth.setAuthStatus(dto.getAuthStatus());
        auth.setAuditReason(dto.getAuditReason());
        auth.setAuditorId(auditorId);
        auth.setAuditTime(LocalDateTime.now());
        realnameAuthMapper.updateById(auth);

        // 更新用户实名状态
        User user = new User();
        user.setUserId(auth.getUserId());
        user.setRealnameStatus(dto.getAuthStatus());
        user.setRealnameTime(LocalDateTime.now());
        user.setRealnameAuditReason(dto.getAuditReason());
        
        if (dto.getAuthStatus() == 1) {
            // 审核通过，更新用户真实姓名和身份证号
            user.setRealName(auth.getRealName());
            user.setIdCard(auth.getIdCard());
        }
        
        userMapper.updateById(user);

        // 发送通知给用户
        if (dto.getAuthStatus() == 1) {
            notificationService.notifyRealnameApproved(auth.getUserId());
        } else {
            notificationService.notifyRealnameRejected(auth.getUserId(), dto.getAuditReason());
        }

        return Result.success(dto.getAuthStatus() == 1 ? "审核通过" : "审核已驳回");
    }

    /**
     * 获取待审核数量统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        
        // 待审核数量
        LambdaQueryWrapper<RealnameAuth> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(RealnameAuth::getAuthStatus, 0);
        stats.put("pending", realnameAuthMapper.selectCount(pendingWrapper));
        
        // 已通过数量
        LambdaQueryWrapper<RealnameAuth> passedWrapper = new LambdaQueryWrapper<>();
        passedWrapper.eq(RealnameAuth::getAuthStatus, 1);
        stats.put("passed", realnameAuthMapper.selectCount(passedWrapper));
        
        // 已驳回数量
        LambdaQueryWrapper<RealnameAuth> rejectedWrapper = new LambdaQueryWrapper<>();
        rejectedWrapper.eq(RealnameAuth::getAuthStatus, 2);
        stats.put("rejected", realnameAuthMapper.selectCount(rejectedWrapper));
        
        // 总数
        stats.put("total", realnameAuthMapper.selectCount(null));
        
        return Result.success(stats);
    }

    @Data
    public static class AuditDTO {
        private Integer authStatus; // 1通过 2驳回
        private String auditReason;
    }
}
