package com.example.demo.controller.statistics;

import com.example.demo.entity.Result;
import com.example.demo.repository.chat.*;
import com.example.demo.repository.comment.*;
import com.example.demo.repository.house.*;
import com.example.demo.repository.notification.*;
import com.example.demo.repository.rental.*;
import com.example.demo.repository.user.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 管理端首页仪表盘接口
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final UserMapper userMapper;
    private final HouseMapper houseMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final RealnameAuthMapper realnameAuthMapper;

    public AdminDashboardController(UserMapper userMapper,
                                    HouseMapper houseMapper,
                                    PaymentOrderMapper paymentOrderMapper,
                                    RealnameAuthMapper realnameAuthMapper) {
        this.userMapper = userMapper;
        this.houseMapper = houseMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.realnameAuthMapper = realnameAuthMapper;
    }

    /**
     * 获取统计卡片数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        List<Map<String, Object>> userByType = userMapper.countByUserType();
        long tenantCount = 0, landlordCount = 0, adminCount = 0;
        for (Map<String, Object> item : userByType) {
            Integer type = ((Number) item.get("userType")).intValue();
            Long count = ((Number) item.get("count")).longValue();
            if (type == 1) tenantCount = count;
            else if (type == 2) landlordCount = count;
            else if (type == 3) adminCount = count;
        }
        stats.put("tenantCount", tenantCount);
        stats.put("landlordCount", landlordCount);
        stats.put("adminCount", adminCount);
        stats.put("totalUserCount", tenantCount + landlordCount + adminCount);

        // 房源统计
        List<Map<String, Object>> houseByStatus = houseMapper.countByStatus();
        long totalHouse = 0, pendingHouse = 0, availableHouse = 0, rentedHouse = 0;
        for (Map<String, Object> item : houseByStatus) {
            Integer status = ((Number) item.get("status")).intValue();
            Long count = ((Number) item.get("count")).longValue();
            totalHouse += count;
            if (status == 0) pendingHouse = count;
            else if (status == 1) availableHouse = count;
            else if (status == 2) rentedHouse = count;
        }
        stats.put("totalHouseCount", totalHouse);
        stats.put("pendingHouseCount", pendingHouse);
        stats.put("availableHouseCount", availableHouse);
        stats.put("rentedHouseCount", rentedHouse);

        // 今日数据
        stats.put("todayNewUsers", userMapper.countTodayNew());
        stats.put("todayNewHouses", houseMapper.countTodayNew());
        stats.put("todayOrders", paymentOrderMapper.countTodayOrders());
        stats.put("todayAmount", paymentOrderMapper.sumTodayAmount());

        // 平台总交易额
        stats.put("totalAmount", paymentOrderMapper.sumTotalAmount());

        return Result.success(stats);
    }

    /**
     * 获取实时监控数据
     */
    @GetMapping("/monitor")
    public Result<Map<String, Object>> getMonitor() {
        Map<String, Object> monitor = new HashMap<>();

        // 模拟在线用户数（实际应从Redis获取）
        monitor.put("onlineUsers", new Random().nextInt(100) + 50);

        // 今日新增房源
        monitor.put("todayNewHouses", houseMapper.countTodayNew());

        // 异常操作报警数（模拟）
        monitor.put("alertCount", new Random().nextInt(5));

        // 系统资源（模拟）
        monitor.put("cpuUsage", 30 + new Random().nextInt(30));
        monitor.put("memoryUsage", 50 + new Random().nextInt(20));

        return Result.success(monitor);
    }

    /**
     * 获取趋势图数据
     */
    @GetMapping("/trends")
    public Result<Map<String, Object>> getTrends() {
        Map<String, Object> trends = new HashMap<>();

        // 用户增长趋势
        trends.put("userTrend", userMapper.getLast7DaysTrend());

        // 房源增长趋势
        trends.put("houseTrend", houseMapper.getLast7DaysTrend());

        // 交易额趋势
        trends.put("amountTrend", paymentOrderMapper.getLast7DaysAmountTrend());

        return Result.success(trends);
    }

    /**
     * 获取热门城市排行
     */
    @GetMapping("/hot-cities")
    public Result<List<Map<String, Object>>> getHotCities() {
        return Result.success(houseMapper.getHotCities());
    }

    /**
     * 获取待办事项
     */
    @GetMapping("/todos")
    public Result<List<Map<String, Object>>> getTodos() {
        List<Map<String, Object>> todos = new ArrayList<>();

        // 待审核房源
        Long pendingHouses = houseMapper.countPendingAudit();
        if (pendingHouses > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "urgent");
            item.put("content", pendingHouses + "条房源待审核");
            item.put("count", pendingHouses);
            todos.add(item);
        }

        // 待审核实名认证
        Long pendingAuth = realnameAuthMapper.countPendingAudit();
        if (pendingAuth > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "important");
            item.put("content", pendingAuth + "名用户实名认证待审核");
            item.put("count", pendingAuth);
            todos.add(item);
        }

        return Result.success(todos);
    }
}
