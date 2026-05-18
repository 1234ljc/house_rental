package com.example.demo.service.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.repository.user.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 信用评分服务
 */
@Slf4j
@Service
public class CreditScoreService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RentalContractMapper rentalContractMapper;

    /**
     * 计算用户信用分
     */
    public int calculateCreditScore(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return 600; // 默认分数
        }

        // 管理员不需要信用分
        if (user.getUserType() != null && user.getUserType() == 3) {
            return 0;
        }

        int score = 600; // 基础分

        // 1. 实名认证 +100分
        if (user.getRealnameStatus() != null && user.getRealnameStatus() == 1) {
            score += 100;
        }

        // 2. 账号使用时长（每月+2分，最高50分）
        if (user.getCreateTime() != null) {
            long months = ChronoUnit.MONTHS.between(user.getCreateTime(), LocalDateTime.now());
            int timeScore = (int) Math.min(months * 2, 50);
            score += timeScore;
        }

        // 3. 租客：完成的租赁合同（每个+50分，最高200分）
        if (user.getUserType() != null && user.getUserType() == 1) {
            QueryWrapper<RentalContract> wrapper = new QueryWrapper<>();
            wrapper.eq("tenant_id", userId);
            wrapper.eq("status", 2); // 已完成的合同
            long completedContracts = rentalContractMapper.selectCount(wrapper);
            int contractScore = (int) Math.min(completedContracts * 50, 200);
            score += contractScore;
        }

        // 4. 房东：成功出租的房源（每个+30分，最高150分）
        if (user.getUserType() != null && user.getUserType() == 2) {
            QueryWrapper<RentalContract> wrapper = new QueryWrapper<>();
            wrapper.eq("landlord_id", userId);
            wrapper.eq("status", 2); // 已完成的合同
            long completedContracts = rentalContractMapper.selectCount(wrapper);
            int contractScore = (int) Math.min(completedContracts * 30, 150);
            score += contractScore;
        }

        // 5. 确保分数在0-1000范围内
        score = Math.max(0, Math.min(1000, score));

        return score;
    }

    /**
     * 更新用户信用分
     */
    public void updateCreditScore(Long userId) {
        int newScore = calculateCreditScore(userId);
        User user = new User();
        user.setUserId(userId);
        user.setCreditScore(newScore);
        userMapper.updateById(user);
        log.info("更新用户{}信用分: {}", userId, newScore);
    }

    /**
     * 获取信用等级
     */
    public String getCreditLevel(int score) {
        if (score >= 900) return "极好";
        if (score >= 800) return "优秀";
        if (score >= 700) return "良好";
        if (score >= 600) return "一般";
        if (score >= 500) return "较差";
        return "差";
    }

    /**
     * 获取信用报告
     */
    public Map<String, Object> getCreditReport(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        // 管理员不需要信用分
        if (user.getUserType() != null && user.getUserType() == 3) {
            Map<String, Object> adminReport = new HashMap<>();
            adminReport.put("score", 0);
            adminReport.put("level", "管理员");
            adminReport.put("message", "管理员账号无需信用评分");
            return adminReport;
        }

        Map<String, Object> report = new HashMap<>();
        
        // 重新计算信用分（确保数据是最新的）
        int score = calculateCreditScore(userId);
        report.put("score", score);
        report.put("level", getCreditLevel(score));
        
        // 分数明细
        Map<String, Object> details = new HashMap<>();
        details.put("baseScore", 600);
        
        int realnameScore = (user.getRealnameStatus() != null && user.getRealnameStatus() == 1) ? 100 : 0;
        details.put("realnameScore", realnameScore);
        
        int timeScore = 0;
        if (user.getCreateTime() != null) {
            long months = ChronoUnit.MONTHS.between(user.getCreateTime(), LocalDateTime.now());
            timeScore = (int) Math.min(months * 2, 50);
        }
        details.put("timeScore", timeScore);
        
        // 根据用户类型显示不同的合同信息
        if (user.getUserType() != null && user.getUserType() == 1) {
            // 租客
            QueryWrapper<RentalContract> wrapper = new QueryWrapper<>();
            wrapper.eq("tenant_id", userId);
            wrapper.eq("status", 2);
            long completedContracts = rentalContractMapper.selectCount(wrapper);
            int contractScore = (int) Math.min(completedContracts * 50, 200);
            details.put("contractScore", contractScore);
            details.put("completedContracts", completedContracts);
            details.put("contractType", "租赁合同");
        } else if (user.getUserType() != null && user.getUserType() == 2) {
            // 房东
            QueryWrapper<RentalContract> wrapper = new QueryWrapper<>();
            wrapper.eq("landlord_id", userId);
            wrapper.eq("status", 2);
            long completedContracts = rentalContractMapper.selectCount(wrapper);
            int contractScore = (int) Math.min(completedContracts * 30, 150);
            details.put("contractScore", contractScore);
            details.put("completedContracts", completedContracts);
            details.put("contractType", "出租合同");
        }
        
        report.put("details", details);
        
        // 提升建议
        java.util.List<String> suggestions = new java.util.ArrayList<>();
        if (realnameScore == 0) {
            suggestions.add("完成实名认证可获得100分");
        }
        
        if (user.getUserType() != null && user.getUserType() == 1) {
            // 租客建议
            int contractScore = details.get("contractScore") != null ? (int) details.get("contractScore") : 0;
            if (contractScore < 200) {
                suggestions.add("完成更多租赁合同可提升信用分（每个+50分）");
            }
        } else if (user.getUserType() != null && user.getUserType() == 2) {
            // 房东建议
            int contractScore = details.get("contractScore") != null ? (int) details.get("contractScore") : 0;
            if (contractScore < 150) {
                suggestions.add("成功出租更多房源可提升信用分（每个+30分）");
            }
        }
        
        if (timeScore < 50) {
            suggestions.add("保持账号活跃，时间越长信用分越高");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("您的信用分已经很高了，继续保持良好的行为");
        }
        report.put("suggestions", suggestions);
        
        return report;
    }

    /**
     * 实名认证后增加信用分
     */
    public void onRealnameVerified(Long userId) {
        updateCreditScore(userId);
    }

    /**
     * 完成合同后增加信用分
     */
    public void onContractCompleted(Long userId) {
        updateCreditScore(userId);
    }
}
