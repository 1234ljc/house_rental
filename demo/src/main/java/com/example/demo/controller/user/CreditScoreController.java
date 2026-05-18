package com.example.demo.controller.user;

import com.example.demo.entity.Result;
import com.example.demo.service.user.CreditScoreService;

import com.example.demo.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 信用评分控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/credit")
public class CreditScoreController {

    @Autowired
    private CreditScoreService creditScoreService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前用户的信用报告
     */
    @GetMapping("/report")
    public Result getCreditReport(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);

            Map<String, Object> report = creditScoreService.getCreditReport(userId);
            if (report == null) {
                return Result.failure("用户不存在");
            }

            return Result.success(report);
        } catch (Exception e) {
            log.error("获取信用报告失败", e);
            return Result.failure("获取信用报告失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定用户的信用分（用于查看其他用户的信用分）
     */
    @GetMapping("/score/{userId}")
    public Result getUserCreditScore(@PathVariable Long userId) {
        try {
            int score = creditScoreService.calculateCreditScore(userId);
            String level = creditScoreService.getCreditLevel(score);
            
            Map<String, Object> result = new HashMap<>();
            result.put("score", score);
            result.put("level", level);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户信用分失败", e);
            return Result.failure("获取用户信用分失败：" + e.getMessage());
        }
    }

    /**
     * 手动更新信用分（用于测试或管理员操作）
     */
    @PostMapping("/update")
    public Result updateCreditScore(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            Long userId = jwtUtil.getUserIdFromToken(token);

            creditScoreService.updateCreditScore(userId);
            return Result.success("信用分更新成功");
        } catch (Exception e) {
            log.error("更新信用分失败", e);
            return Result.failure("更新信用分失败：" + e.getMessage());
        }
    }
}
