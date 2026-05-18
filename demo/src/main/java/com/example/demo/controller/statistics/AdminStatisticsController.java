package com.example.demo.controller.statistics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Result;
import com.example.demo.repository.chat.entity.*;
import com.example.demo.repository.comment.entity.*;
import com.example.demo.repository.house.entity.*;
import com.example.demo.repository.notification.entity.*;
import com.example.demo.repository.rental.entity.*;
import com.example.demo.repository.user.entity.*;
import com.example.demo.repository.chat.*;
import com.example.demo.repository.comment.*;
import com.example.demo.repository.house.*;
import com.example.demo.repository.notification.*;
import com.example.demo.repository.rental.*;
import com.example.demo.repository.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private HouseMapper houseMapper;
    @Autowired
    private RentalContractMapper contractMapper;
    @Autowired
    private PaymentOrderMapper orderMapper;

    // ==================== 业务数据统计 ====================

    /**
     * 用户统计
     */
    @GetMapping("/business/user")
    public Result getUserStats() {
        Map<String, Object> data = new HashMap<>();
        
        // 总用户数
        long total = userMapper.selectCount(null);
        data.put("total", total);
        
        // 各类型用户数
        long tenants = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserType, 1));
        long landlords = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserType, 2));
        long admins = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUserType, 3));
        data.put("tenants", tenants);
        data.put("landlords", landlords);
        data.put("admins", admins);
        
        // 今日新增
        long todayNew = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .apply("DATE(create_time) = CURDATE()"));
        data.put("todayNew", todayNew);
        
        // 本月新增
        long monthNew = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .apply("YEAR(create_time) = YEAR(CURDATE()) AND MONTH(create_time) = MONTH(CURDATE())"));
        data.put("monthNew", monthNew);
        
        // 实名认证率
        long verified = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRealnameStatus, 1));
        data.put("verified", verified);
        data.put("verifyRate", total > 0 ? BigDecimal.valueOf(verified * 100.0 / total).setScale(1, RoundingMode.HALF_UP) : 0);
        
        // 近30天注册趋势（一条SQL）
        List<Map<String, Object>> dbTrend = userMapper.getLast30DaysTrend();
        Map<String, Long> trendMap = new HashMap<>();
        for (Map<String, Object> row : dbTrend) {
            trendMap.put(row.get("date").toString(), ((Number) row.get("count")).longValue());
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.add(Map.of("date", date.format(fmt), "count", trendMap.getOrDefault(date.toString(), 0L)));
        }
        data.put("trend", trend);
        
        // 用户类型分布
        data.put("typeDistribution", List.of(
                Map.of("name", "租客", "value", tenants),
                Map.of("name", "房东", "value", landlords),
                Map.of("name", "管理员", "value", admins)
        ));
        
        return Result.success(data);
    }

    /**
     * 房源统计
     */
    @GetMapping("/business/house")
    public Result getHouseStats() {
        Map<String, Object> data = new HashMap<>();
        
        // 各状态数量（一条SQL）
        List<Map<String, Object>> statusRows = houseMapper.countByStatus();
        Map<Integer, Long> statusMap = new HashMap<>();
        long total = 0;
        for (Map<String, Object> row : statusRows) {
            Integer status = ((Number) row.get("status")).intValue();
            Long cnt = ((Number) row.get("count")).longValue();
            statusMap.put(status, cnt);
            total += cnt;
        }
        
        long pending = statusMap.getOrDefault(0, 0L);
        long available = statusMap.getOrDefault(1, 0L);
        long rented = statusMap.getOrDefault(2, 0L);
        long offline = statusMap.getOrDefault(3, 0L);
        long rejected = statusMap.getOrDefault(4, 0L);
        
        data.put("total", total);
        data.put("pending", pending);
        data.put("available", available);
        data.put("rented", rented);
        data.put("offline", offline);
        data.put("rejected", rejected);
        
        // 审核通过率
        long reviewed = available + rented + offline + rejected;
        long passed = available + rented + offline;
        data.put("passRate", reviewed > 0 ? BigDecimal.valueOf(passed * 100.0 / reviewed).setScale(1, RoundingMode.HALF_UP) : 0);
        
        // 今日/本月新增
        data.put("todayNew", houseMapper.countTodayNew());
        long monthNew = houseMapper.selectCount(new LambdaQueryWrapper<House>()
                .apply("YEAR(create_time) = YEAR(CURDATE()) AND MONTH(create_time) = MONTH(CURDATE())"));
        data.put("monthNew", monthNew);
        
        // 区域分布TOP10
        data.put("cityDistribution", houseMapper.getHotCities());
        
        // 发布趋势（一条SQL）
        List<Map<String, Object>> dbTrend = houseMapper.getLast30DaysTrend();
        Map<String, Long> trendMap = new HashMap<>();
        for (Map<String, Object> row : dbTrend) {
            trendMap.put(row.get("date").toString(), ((Number) row.get("count")).longValue());
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.add(Map.of("date", date.format(fmt), "count", trendMap.getOrDefault(date.toString(), 0L)));
        }
        data.put("trend", trend);
        
        return Result.success(data);
    }


    /**
     * 租赁统计
     */
    @GetMapping("/business/rental")
    public Result getRentalStats() {
        Map<String, Object> data = new HashMap<>();
        
        // 申请相关已移除，返回0兼容前端
        data.put("totalApply", 0);
        data.put("pendingApply", 0);
        data.put("acceptedApply", 0);
        data.put("rejectedApply", 0);
        data.put("successRate", 0);
        
        // 合同统计
        long totalContract = contractMapper.selectCount(null);
        long activeContract = contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2));
        long expiredContract = contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 3));
        
        data.put("totalContract", totalContract);
        data.put("activeContract", activeContract);
        data.put("expiredContract", expiredContract);
        
        // 平均租期（月）
        List<RentalContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<RentalContract>()
                .isNotNull(RentalContract::getRentStartDate)
                .isNotNull(RentalContract::getRentEndDate));
        if (!contracts.isEmpty()) {
            double avgMonths = contracts.stream()
                    .mapToLong(c -> ChronoUnit.MONTHS.between(c.getRentStartDate(), c.getRentEndDate()))
                    .average().orElse(0);
            data.put("avgRentMonth", BigDecimal.valueOf(avgMonths).setScale(1, RoundingMode.HALF_UP));
        } else {
            data.put("avgRentMonth", 0);
        }
        
        // 合同创建趋势（近30天，一条SQL）
        List<Map<String, Object>> dbTrend = contractMapper.getLast30DaysTrend();
        Map<String, Long> trendMap = new HashMap<>();
        for (Map<String, Object> row : dbTrend) {
            trendMap.put(row.get("date").toString(), ((Number) row.get("count")).longValue());
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.add(Map.of("date", date.format(fmt), "count", trendMap.getOrDefault(date.toString(), 0L)));
        }
        data.put("trend", trend);
        
        return Result.success(data);
    }

    /**
     * 财务统计
     */
    @GetMapping("/business/finance")
    public Result getFinanceStats() {
        Map<String, Object> data = new HashMap<>();
        
        // 总交易额
        BigDecimal totalAmount = orderMapper.sumTotalAmount();
        data.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        
        // 订单统计
        long totalOrders = orderMapper.selectCount(null);
        long paidOrders = orderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getPaymentStatus, 1));
        long failedOrders = orderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getPaymentStatus, 2));
        
        data.put("totalOrders", totalOrders);
        data.put("paidOrders", paidOrders);
        data.put("failedOrders", failedOrders);
        
        // 成功率/失败率
        data.put("successRate", totalOrders > 0 ? BigDecimal.valueOf(paidOrders * 100.0 / totalOrders).setScale(1, RoundingMode.HALF_UP) : 0);
        data.put("failRate", totalOrders > 0 ? BigDecimal.valueOf(failedOrders * 100.0 / totalOrders).setScale(1, RoundingMode.HALF_UP) : 0);
        
        // 今日/本月交易额
        BigDecimal todayAmount = orderMapper.sumTodayAmount();
        data.put("todayAmount", todayAmount != null ? todayAmount : BigDecimal.ZERO);
        
        List<PaymentOrder> monthOrders = orderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPaymentStatus, 1)
                .apply("YEAR(payment_time) = YEAR(CURDATE()) AND MONTH(payment_time) = MONTH(CURDATE())"));
        BigDecimal monthAmount = monthOrders.stream()
                .map(PaymentOrder::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("monthAmount", monthAmount);
        
        // 收入趋势（一条SQL）
        List<Map<String, Object>> dbTrend = orderMapper.getLast30DaysAmountTrend();
        Map<String, BigDecimal> trendMap = new HashMap<>();
        for (Map<String, Object> row : dbTrend) {
            trendMap.put(row.get("date").toString(), new BigDecimal(row.get("amount").toString()));
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.add(Map.of("date", date.format(fmt), "amount", trendMap.getOrDefault(date.toString(), BigDecimal.ZERO)));
        }
        data.put("trend", trend);
        
        // 订单类型分布
        List<Map<String, Object>> typeDistribution = new ArrayList<>();
        String[] types = {"首期支付", "租金支付", "押金退还"};
        for (int i = 0; i < 3; i++) {
            List<PaymentOrder> typeOrders = orderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getPaymentStatus, 1)
                    .eq(PaymentOrder::getOrderType, i));
            BigDecimal amount = typeOrders.stream()
                    .map(PaymentOrder::getPayAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            typeDistribution.add(Map.of("name", types[i], "value", amount));
        }
        data.put("typeDistribution", typeDistribution);
        
        return Result.success(data);
    }


    // ==================== 数据分析报表 ====================

    /**
     * 用户画像分析
     */
    @GetMapping("/report/user-profile")
    public Result getUserProfile() {
        Map<String, Object> data = new HashMap<>();
        
        // 租客分析
        List<User> tenants = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getUserType, 1));
        data.put("tenantCount", tenants.size());
        long tenantVerified = tenants.stream().filter(u -> u.getRealnameStatus() == 1).count();
        data.put("tenantVerifyRate", tenants.size() > 0 ? BigDecimal.valueOf(tenantVerified * 100.0 / tenants.size()).setScale(1, RoundingMode.HALF_UP) : 0);
        
        // 房东分析
        List<User> landlords = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getUserType, 2));
        data.put("landlordCount", landlords.size());
        long landlordVerified = landlords.stream().filter(u -> u.getRealnameStatus() == 1).count();
        data.put("landlordVerifyRate", landlords.size() > 0 ? BigDecimal.valueOf(landlordVerified * 100.0 / landlords.size()).setScale(1, RoundingMode.HALF_UP) : 0);
        
        // 活跃房东（有房源的）
        List<House> houses = houseMapper.selectList(null);
        Set<Long> activeLandlords = houses.stream().map(House::getLandlordId).collect(Collectors.toSet());
        data.put("activeLandlords", activeLandlords.size());
        
        // 活跃租客（有合同的）
        List<RentalContract> allContracts = contractMapper.selectList(null);
        Set<Long> activeTenants = allContracts.stream().map(RentalContract::getTenantId).collect(Collectors.toSet());
        data.put("activeTenants", activeTenants.size());
        
        // 用户注册时间分布（按月）
        List<Map<String, Object>> registerTrend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = today.minusMonths(i).withDayOfMonth(1);
            String monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .apply("DATE_FORMAT(create_time, '%Y-%m') = {0}", monthStr));
            registerTrend.add(Map.of("month", monthStr, "count", count));
        }
        data.put("registerTrend", registerTrend);
        
        return Result.success(data);
    }

    /**
     * 房源质量分析
     */
    @GetMapping("/report/house-quality")
    public Result getHouseQuality() {
        Map<String, Object> data = new HashMap<>();
        
        List<House> houses = houseMapper.selectList(null);
        data.put("total", houses.size());
        
        // 信息完整性分析
        long hasDescription = houses.stream().filter(h -> h.getDescription() != null && !h.getDescription().isEmpty()).count();
        long hasFacilities = houses.stream().filter(h -> h.getFacilities() != null && !h.getFacilities().isEmpty()).count();
        long hasImages = houses.stream().filter(h -> h.getImages() != null && !h.getImages().isEmpty() && !"[]".equals(h.getImages())).count();
        
        data.put("hasDescription", hasDescription);
        data.put("hasFacilities", hasFacilities);
        data.put("hasImages", hasImages);
        
        data.put("descriptionRate", houses.size() > 0 ? BigDecimal.valueOf(hasDescription * 100.0 / houses.size()).setScale(1, RoundingMode.HALF_UP) : 0);
        data.put("facilitiesRate", houses.size() > 0 ? BigDecimal.valueOf(hasFacilities * 100.0 / houses.size()).setScale(1, RoundingMode.HALF_UP) : 0);
        data.put("imagesRate", houses.size() > 0 ? BigDecimal.valueOf(hasImages * 100.0 / houses.size()).setScale(1, RoundingMode.HALF_UP) : 0);
        
        // 价格区间分布
        List<Map<String, Object>> priceDistribution = new ArrayList<>();
        int[][] ranges = {{0, 1000}, {1000, 2000}, {2000, 3000}, {3000, 5000}, {5000, 8000}, {8000, Integer.MAX_VALUE}};
        String[] labels = {"1000以下", "1000-2000", "2000-3000", "3000-5000", "5000-8000", "8000以上"};
        for (int i = 0; i < ranges.length; i++) {
            int min = ranges[i][0], max = ranges[i][1];
            long count = houses.stream()
                    .filter(h -> h.getRentPrice() != null)
                    .filter(h -> h.getRentPrice().doubleValue() >= min && (max == Integer.MAX_VALUE || h.getRentPrice().doubleValue() < max))
                    .count();
            priceDistribution.add(Map.of("name", labels[i], "value", count));
        }
        data.put("priceDistribution", priceDistribution);
        
        // 户型分布
        Map<String, Long> houseTypeMap = houses.stream()
                .filter(h -> h.getHouseType() != null)
                .collect(Collectors.groupingBy(House::getHouseType, Collectors.counting()));
        List<Map<String, Object>> houseTypeDistribution = houseTypeMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .collect(Collectors.toList());
        data.put("houseTypeDistribution", houseTypeDistribution);
        
        return Result.success(data);
    }

    /**
     * 租赁行为分析
     */
    @GetMapping("/report/rental-behavior")
    public Result getRentalBehavior() {
        Map<String, Object> data = new HashMap<>();
        
        // 热门房源TOP10
        List<House> hotHouses = houseMapper.selectList(new LambdaQueryWrapper<House>()
                .orderByDesc(House::getViewCount)
                .last("LIMIT 10"));
        List<Map<String, Object>> hotList = new ArrayList<>();
        int rank = 1;
        for (House h : hotHouses) {
            hotList.add(Map.of(
                    "rank", rank++,
                    "title", h.getTitle(),
                    "city", h.getCity() != null ? h.getCity() : "",
                    "rentPrice", h.getRentPrice(),
                    "viewCount", h.getViewCount() != null ? h.getViewCount() : 0,
                    "collectCount", h.getCollectCount() != null ? h.getCollectCount() : 0
            ));
        }
        data.put("hotHouses", hotList);
        
        // 租期偏好分布（从合同数据获取）
        List<RentalContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<RentalContract>()
                .isNotNull(RentalContract::getRentStartDate)
                .isNotNull(RentalContract::getRentEndDate));
        Map<String, Long> rentMonthMap = new HashMap<>();
        for (RentalContract c : contracts) {
            long months = ChronoUnit.MONTHS.between(c.getRentStartDate(), c.getRentEndDate());
            String key;
            if (months <= 3) key = "3个月以内";
            else if (months <= 6) key = "3-6个月";
            else if (months <= 12) key = "6-12个月";
            else key = "12个月以上";
            rentMonthMap.merge(key, 1L, Long::sum);
        }
        data.put("rentMonthDistribution", rentMonthMap.entrySet().stream()
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .collect(Collectors.toList()));
        
        return Result.success(data);
    }

    /**
     * 收入趋势预测
     */
    @GetMapping("/report/income-forecast")
    public Result getIncomeForecast() {
        Map<String, Object> data = new HashMap<>();
        
        // 历史月收入数据
        List<Map<String, Object>> history = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate month = today.minusMonths(i).withDayOfMonth(1);
            String monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            List<PaymentOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                    .eq(PaymentOrder::getPaymentStatus, 1)
                    .apply("DATE_FORMAT(payment_time, '%Y-%m') = {0}", monthStr));
            BigDecimal amount = orders.stream()
                    .map(PaymentOrder::getPayAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            history.add(Map.of("month", monthStr, "amount", amount, "type", "历史"));
        }
        data.put("history", history);
        
        // 简单预测（基于近3个月平均值）
        List<BigDecimal> recent = history.subList(Math.max(0, history.size() - 3), history.size())
                .stream()
                .map(m -> (BigDecimal) m.get("amount"))
                .collect(Collectors.toList());
        BigDecimal avgRecent = recent.isEmpty() ? BigDecimal.ZERO :
                recent.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(recent.size()), 2, RoundingMode.HALF_UP);
        
        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            LocalDate futureMonth = today.plusMonths(i).withDayOfMonth(1);
            String monthStr = futureMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            // 简单增长预测（每月增长5%）
            BigDecimal predicted = avgRecent.multiply(BigDecimal.valueOf(1 + 0.05 * i)).setScale(2, RoundingMode.HALF_UP);
            forecast.add(Map.of("month", monthStr, "amount", predicted, "type", "预测"));
        }
        data.put("forecast", forecast);
        
        // 预测汇总
        data.put("avgMonthlyIncome", avgRecent);
        data.put("predictedNextMonth", forecast.isEmpty() ? BigDecimal.ZERO : forecast.get(0).get("amount"));
        
        return Result.success(data);
    }

    /**
     * 数据导出
     */
    @GetMapping("/report/export")
    public Result exportData(
            @RequestParam String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        Map<String, Object> data = new HashMap<>();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        switch (type) {
            case "user":
                List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .ge(User::getCreateTime, start.atStartOfDay())
                        .le(User::getCreateTime, end.plusDays(1).atStartOfDay()));
                data.put("records", users.stream().map(this::buildUserExport).collect(Collectors.toList()));
                data.put("total", users.size());
                break;
            case "house":
                List<House> houses = houseMapper.selectList(new LambdaQueryWrapper<House>()
                        .ge(House::getCreateTime, start.atStartOfDay())
                        .le(House::getCreateTime, end.plusDays(1).atStartOfDay()));
                data.put("records", houses.stream().map(this::buildHouseExport).collect(Collectors.toList()));
                data.put("total", houses.size());
                break;
            case "order":
                List<PaymentOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>()
                        .ge(PaymentOrder::getCreateTime, start.atStartOfDay())
                        .le(PaymentOrder::getCreateTime, end.plusDays(1).atStartOfDay()));
                data.put("records", orders.stream().map(this::buildOrderExport).collect(Collectors.toList()));
                data.put("total", orders.size());
                BigDecimal totalAmount = orders.stream()
                        .filter(o -> o.getPaymentStatus() == 1)
                        .map(PaymentOrder::getPayAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                data.put("totalAmount", totalAmount);
                break;
            case "contract":
                List<RentalContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<RentalContract>()
                        .ge(RentalContract::getCreateTime, start.atStartOfDay())
                        .le(RentalContract::getCreateTime, end.plusDays(1).atStartOfDay()));
                data.put("records", contracts.stream().map(this::buildContractExport).collect(Collectors.toList()));
                data.put("total", contracts.size());
                break;
            default:
                return Result.failure("不支持的导出类型");
        }
        
        data.put("exportTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        data.put("dateRange", start + " ~ " + end);
        
        return Result.success(data);
    }

    // 辅助方法
    private Map<String, Object> buildUserExport(User u) {
        return Map.of(
                "userId", u.getUserId(),
                "username", u.getUsername(),
                "phone", u.getPhone() != null ? u.getPhone() : "",
                "userType", u.getUserType() == 1 ? "租客" : u.getUserType() == 2 ? "房东" : "管理员",
                "realnameStatus", u.getRealnameStatus() == 1 ? "已认证" : "未认证",
                "createTime", u.getCreateTime()
        );
    }

    private Map<String, Object> buildHouseExport(House h) {
        return Map.of(
                "houseId", h.getHouseId(),
                "title", h.getTitle(),
                "city", h.getCity() != null ? h.getCity() : "",
                "rentPrice", h.getRentPrice(),
                "status", getHouseStatusText(h.getStatus()),
                "createTime", h.getCreateTime()
        );
    }

    private Map<String, Object> buildOrderExport(PaymentOrder o) {
        return Map.of(
                "orderId", o.getOrderId(),
                "orderNo", o.getOrderNo(),
                "payAmount", o.getPayAmount(),
                "paymentStatus", o.getPaymentStatus() == 1 ? "已支付" : o.getPaymentStatus() == 0 ? "待支付" : "失败",
                "paymentTime", o.getPaymentTime() != null ? o.getPaymentTime().toString() : "",
                "createTime", o.getCreateTime()
        );
    }

    private Map<String, Object> buildContractExport(RentalContract c) {
        return Map.of(
                "contractId", c.getContractId(),
                "contractNo", c.getContractNo(),
                "monthlyRent", c.getMonthlyRent(),
                "status", getContractStatusText(c.getStatus()),
                "rentStartDate", c.getRentStartDate(),
                "rentEndDate", c.getRentEndDate()
        );
    }

    private String getHouseStatusText(int status) {
        String[] texts = {"待审核", "可出租", "已出租", "已下架", "审核驳回"};
        return status >= 0 && status < texts.length ? texts[status] : "未知";
    }

    private String getContractStatusText(int status) {
        String[] texts = {"草稿", "待确认", "已确认", "已到期", "已终止"};
        return status >= 0 && status < texts.length ? texts[status] : "未知";
    }
}
