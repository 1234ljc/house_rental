package com.example.demo.controller.statistics;

import com.example.demo.repository.house.entity.House;
import com.example.demo.entity.Result;
import com.example.demo.repository.chat.*;
import com.example.demo.repository.comment.*;
import com.example.demo.repository.house.*;
import com.example.demo.repository.notification.*;
import com.example.demo.repository.rental.*;
import com.example.demo.repository.user.*;
import com.example.demo.utils.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 房东端首页仪表盘接口
 */
@RestController
@RequestMapping("/api/landlord/dashboard")
public class LandlordDashboardController {

    private final JwtUtil jwtUtil;
    private final HouseMapper houseMapper;
    private final RentalContractMapper contractMapper;
    private final PaymentOrderMapper paymentOrderMapper;

    public LandlordDashboardController(JwtUtil jwtUtil,
                                       HouseMapper houseMapper,
                                       RentalContractMapper contractMapper,
                                       PaymentOrderMapper paymentOrderMapper) {
        this.jwtUtil = jwtUtil;
        this.houseMapper = houseMapper;
        this.contractMapper = contractMapper;
        this.paymentOrderMapper = paymentOrderMapper;
    }

    /**
     * 获取统计卡片数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestHeader("Authorization") String authHeader) {
        Long landlordId = getUserIdFromToken(authHeader);
        if (landlordId == null) {
            return Result.failure(401, "无效的token");
        }

        Map<String, Object> stats = new HashMap<>();

        // 房源统计
        stats.put("totalHouses", houseMapper.countByLandlordId(landlordId));
        stats.put("availableHouses", houseMapper.countByLandlordIdAndStatus(landlordId, 1));

        // 合同统计
        stats.put("pendingContracts", contractMapper.countPendingSignByLandlordId(landlordId));

        // 租金统计
        stats.put("pendingRents", paymentOrderMapper.countPendingByLandlordId(landlordId));
        stats.put("monthlyReceived", paymentOrderMapper.sumMonthlyReceivedByLandlordId(landlordId));
        stats.put("monthlyExpected", paymentOrderMapper.sumMonthlyExpectedByLandlordId(landlordId));

        return Result.success(stats);
    }

    /**
     * 获取待办事项
     */
    @GetMapping("/todos")
    public Result<Map<String, Object>> getTodos(@RequestHeader("Authorization") String authHeader) {
        Long landlordId = getUserIdFromToken(authHeader);
        if (landlordId == null) {
            return Result.failure(401, "无效的token");
        }

        Map<String, Object> todos = new HashMap<>();

        // 紧急事项
        List<Map<String, Object>> urgentList = new ArrayList<>();
        
        // 逾期租金
        Long overdueRents = paymentOrderMapper.countOverdueByLandlordId(landlordId);
        if (overdueRents > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", overdueRents + "笔租金已逾期");
            item.put("subTitle", "请及时催缴");
            item.put("type", "overdue");
            urgentList.add(item);
        }

        todos.put("urgent", urgentList);

        // 普通事项
        List<Map<String, Object>> normalList = new ArrayList<>();

        // 待签署合同
        Long pendingContracts = contractMapper.countPendingSignByLandlordId(landlordId);
        if (pendingContracts > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", pendingContracts + "份合同待确认");
            item.put("subTitle", "本周内");
            item.put("type", "contract");
            normalList.add(item);
        }

        // 续租申请待处理
        Long pendingRenewals = contractMapper.countPendingRenewalByLandlordId(landlordId);
        if (pendingRenewals > 0) {
            Map<String, Object> renewalItem = new HashMap<>();
            renewalItem.put("title", pendingRenewals + "条续租申请待处理");
            renewalItem.put("subTitle", "请尽快处理");
            renewalItem.put("type", "renewal");
            // 续租申请放到紧急事项
            urgentList.add(renewalItem);
        }

        todos.put("normal", normalList);

        return Result.success(todos);
    }

    /**
     * 获取近30天收入趋势
     */
    @GetMapping("/income-trend")
    public Result<List<Map<String, Object>>> getIncomeTrend(@RequestHeader("Authorization") String authHeader) {
        Long landlordId = getUserIdFromToken(authHeader);
        if (landlordId == null) {
            return Result.failure(401, "无效的token");
        }

        List<Map<String, Object>> trend = paymentOrderMapper.getLast30DaysIncomeByLandlordId(landlordId);
        return Result.success(trend);
    }

    /**
     * 获取房源收入排行TOP5
     */
    @GetMapping("/house-income-rank")
    public Result<List<Map<String, Object>>> getHouseIncomeRank(@RequestHeader("Authorization") String authHeader) {
        Long landlordId = getUserIdFromToken(authHeader);
        if (landlordId == null) {
            return Result.failure(401, "无效的token");
        }

        List<Map<String, Object>> rank = paymentOrderMapper.getHouseIncomeRankByLandlordId(landlordId);
        return Result.success(rank);
    }

    /**
     * 获取房源运营数据（出租率、空置天数、浏览转化等）
     */
    @GetMapping("/house-analytics")
    public Result<Map<String, Object>> getHouseAnalytics(@RequestHeader("Authorization") String authHeader) {
        Long landlordId = getUserIdFromToken(authHeader);
        if (landlordId == null) {
            return Result.failure(401, "无效的token");
        }

        Map<String, Object> analytics = new HashMap<>();

        // 房源总数和各状态数
        Long totalHouses = houseMapper.countByLandlordId(landlordId);
        Long rentedHouses = houseMapper.countByLandlordIdAndStatus(landlordId, 2);
        Long availableHouses = houseMapper.countByLandlordIdAndStatus(landlordId, 1);

        // 出租率
        double occupancyRate = totalHouses > 0 ? (rentedHouses * 100.0 / totalHouses) : 0;
        analytics.put("occupancyRate", Math.round(occupancyRate * 10) / 10.0);
        analytics.put("totalHouses", totalHouses);
        analytics.put("rentedHouses", rentedHouses);
        analytics.put("availableHouses", availableHouses);

        // 总浏览量和总收藏量
        List<House> houses = houseMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<House>()
                .eq(House::getLandlordId, landlordId)
        );
        long totalViews = houses.stream().mapToLong(h -> h.getViewCount() != null ? h.getViewCount() : 0).sum();
        long totalCollects = houses.stream().mapToLong(h -> h.getCollectCount() != null ? h.getCollectCount() : 0).sum();
        analytics.put("totalViews", totalViews);
        analytics.put("totalCollects", totalCollects);

        // 签约转化率（总签约数 / 总浏览量）
        long contractCount = contractMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.demo.repository.rental.entity.RentalContract>()
                .eq(com.example.demo.repository.rental.entity.RentalContract::getLandlordId, landlordId)
                .in(com.example.demo.repository.rental.entity.RentalContract::getStatus, java.util.Arrays.asList(2, 3, 4))
        );
        double conversionRate = totalViews > 0 ? (contractCount * 100.0 / totalViews) : 0;
        analytics.put("conversionRate", Math.round(conversionRate * 100) / 100.0);
        analytics.put("contractCount", contractCount);

        // 各房源浏览排行TOP5
        List<Map<String, Object>> houseViewRank = new ArrayList<>();
        houses.stream()
            .sorted((a, b) -> Long.compare(
                b.getViewCount() != null ? b.getViewCount() : 0,
                a.getViewCount() != null ? a.getViewCount() : 0))
            .limit(5)
            .forEach(h -> {
                Map<String, Object> item = new HashMap<>();
                item.put("houseId", h.getHouseId());
                item.put("title", h.getTitle());
                item.put("viewCount", h.getViewCount() != null ? h.getViewCount() : 0);
                item.put("collectCount", h.getCollectCount() != null ? h.getCollectCount() : 0);
                item.put("rentPrice", h.getRentPrice());
                item.put("status", h.getStatus());
                houseViewRank.add(item);
            });
        analytics.put("houseViewRank", houseViewRank);

        return Result.success(analytics);
    }

    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token);
    }
}
