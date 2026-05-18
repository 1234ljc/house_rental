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
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/landlord/statistics")
public class LandlordStatisticsController {

    @Autowired
    private HouseMapper houseMapper;
    @Autowired
    private RentalContractMapper rentalContractMapper;
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ChatSessionMapper chatSessionMapper;

    /**
     * 获取总览统计
     */
    @GetMapping("/overview")
    public Result getOverview(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        Map<String, Object> data = new HashMap<>();

        // 房源统计
        LambdaQueryWrapper<House> houseWrapper = new LambdaQueryWrapper<>();
        houseWrapper.eq(House::getLandlordId, landlordId);
        long totalHouse = houseMapper.selectCount(houseWrapper);
        
        houseWrapper.clear();
        houseWrapper.eq(House::getLandlordId, landlordId).eq(House::getStatus, 1);
        long availableHouse = houseMapper.selectCount(houseWrapper);
        
        houseWrapper.clear();
        houseWrapper.eq(House::getLandlordId, landlordId).eq(House::getStatus, 2);
        long rentedHouse = houseMapper.selectCount(houseWrapper);

        data.put("totalHouse", totalHouse);
        data.put("availableHouse", availableHouse);
        data.put("rentedHouse", rentedHouse);
        data.put("vacancyRate", totalHouse > 0 ? 
            BigDecimal.valueOf(availableHouse * 100.0 / totalHouse).setScale(1, RoundingMode.HALF_UP) : 0);

        // 合同统计
        LambdaQueryWrapper<RentalContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2);
        long activeContracts = rentalContractMapper.selectCount(contractWrapper);
        data.put("activeContracts", activeContracts);

        // 本月收入
        List<Long> contractIds = getContractIds(landlordId);
        BigDecimal monthlyIncome = BigDecimal.ZERO;
        if (!contractIds.isEmpty()) {
            LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LambdaQueryWrapper<PaymentOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.in(PaymentOrder::getContractId, contractIds)
                    .eq(PaymentOrder::getPaymentStatus, 1)
                    .ne(PaymentOrder::getOrderType, 2)
                    .ge(PaymentOrder::getPaymentTime, monthStart);
            List<PaymentOrder> orders = paymentOrderMapper.selectList(orderWrapper);
            for (PaymentOrder order : orders) {
                monthlyIncome = monthlyIncome.add(order.getPayAmount());
            }
        }
        data.put("monthlyIncome", monthlyIncome);

        // 累计收入
        BigDecimal totalIncome = BigDecimal.ZERO;
        if (!contractIds.isEmpty()) {
            LambdaQueryWrapper<PaymentOrder> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.in(PaymentOrder::getContractId, contractIds)
                    .eq(PaymentOrder::getPaymentStatus, 1)
                    .ne(PaymentOrder::getOrderType, 2);
            List<PaymentOrder> allOrders = paymentOrderMapper.selectList(totalWrapper);
            for (PaymentOrder order : allOrders) {
                totalIncome = totalIncome.add(order.getPayAmount());
            }
        }
        data.put("totalIncome", totalIncome);

        // 租客数量
        contractWrapper.clear();
        contractWrapper.eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2);
        List<RentalContract> contracts = rentalContractMapper.selectList(contractWrapper);
        Set<Long> tenantIds = new HashSet<>();
        for (RentalContract c : contracts) {
            tenantIds.add(c.getTenantId());
        }
        data.put("tenantCount", tenantIds.size());

        return Result.success(data);
    }

    /**
     * 获取收入趋势（近12个月，一条SQL）
     */
    @GetMapping("/income/trend")
    public Result getIncomeTrend(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        
        List<Map<String, Object>> dbRows = paymentOrderMapper.getLast12MonthsIncomeByLandlordId(landlordId);
        Map<String, BigDecimal> incomeMap = new HashMap<>();
        for (Map<String, Object> row : dbRows) {
            incomeMap.put(row.get("month").toString(), new BigDecimal(row.get("income").toString()));
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 11; i >= 0; i--) {
            LocalDate monthDate = today.minusMonths(i);
            String monthKey = monthDate.format(formatter);
            Map<String, Object> item = new HashMap<>();
            item.put("month", monthKey);
            item.put("income", incomeMap.getOrDefault(monthKey, BigDecimal.ZERO));
            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 获取房源热度排行
     */
    @GetMapping("/house/ranking")
    public Result getHouseRanking(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getLandlordId, landlordId)
                .orderByDesc(House::getViewCount)
                .last("LIMIT 10");
        List<House> houses = houseMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (House house : houses) {
            Map<String, Object> item = new HashMap<>();
            item.put("houseId", house.getHouseId());
            item.put("title", house.getTitle());
            item.put("viewCount", house.getViewCount());
            item.put("collectCount", house.getCollectCount());
            item.put("status", house.getStatus());
            item.put("rentPrice", house.getRentPrice());

            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 获取浏览→签约转化漏斗
     */
    @GetMapping("/conversion/funnel")
    public Result getConversionFunnel(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        // 获取房东的所有房源
        LambdaQueryWrapper<House> houseWrapper = new LambdaQueryWrapper<>();
        houseWrapper.eq(House::getLandlordId, landlordId);
        List<House> houses = houseMapper.selectList(houseWrapper);

        Map<String, Object> data = new HashMap<>();
        if (houses.isEmpty()) {
            data.put("viewCount", 0);
            data.put("contractCount", 0);
            return Result.success(data);
        }

        // 总浏览量
        long totalViews = houses.stream()
                .mapToLong(h -> h.getViewCount() != null ? h.getViewCount() : 0)
                .sum();
        data.put("viewCount", totalViews);

        // 签约数
        LambdaQueryWrapper<RentalContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(RentalContract::getLandlordId, landlordId)
                .in(RentalContract::getStatus, Arrays.asList(2, 3, 4));
        long contractCount = rentalContractMapper.selectCount(contractWrapper);
        data.put("contractCount", contractCount);

        // 计算转化率
        data.put("viewToContract", totalViews > 0 ?
            BigDecimal.valueOf(contractCount * 100.0 / totalViews).setScale(1, RoundingMode.HALF_UP) : 0);

        return Result.success(data);
    }


    /**
     * 获取租客画像分析
     */
    @GetMapping("/tenant/analysis")
    public Result getTenantAnalysis(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getLandlordId, landlordId)
                .in(RentalContract::getStatus, Arrays.asList(2, 3, 4));
        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);

        Map<String, Object> data = new HashMap<>();
        
        // 租期分布
        Map<String, Integer> rentPeriodDist = new LinkedHashMap<>();
        rentPeriodDist.put("3个月以下", 0);
        rentPeriodDist.put("3-6个月", 0);
        rentPeriodDist.put("6-12个月", 0);
        rentPeriodDist.put("12个月以上", 0);

        // 续租统计
        Map<Long, Integer> tenantContractCount = new HashMap<>();
        int totalRentDays = 0;
        int contractCount = 0;

        for (RentalContract contract : contracts) {
            // 租期分布
            long months = ChronoUnit.MONTHS.between(contract.getRentStartDate(), contract.getRentEndDate());
            if (months < 3) {
                rentPeriodDist.merge("3个月以下", 1, Integer::sum);
            } else if (months < 6) {
                rentPeriodDist.merge("3-6个月", 1, Integer::sum);
            } else if (months < 12) {
                rentPeriodDist.merge("6-12个月", 1, Integer::sum);
            } else {
                rentPeriodDist.merge("12个月以上", 1, Integer::sum);
            }

            // 续租统计
            tenantContractCount.merge(contract.getTenantId(), 1, Integer::sum);

            // 平均租期
            long days = ChronoUnit.DAYS.between(contract.getRentStartDate(), contract.getRentEndDate());
            totalRentDays += days;
            contractCount++;
        }

        data.put("rentPeriodDistribution", rentPeriodDist);

        // 续租率（有多次合同的租客比例）
        long renewedTenants = tenantContractCount.values().stream().filter(c -> c > 1).count();
        long totalTenants = tenantContractCount.size();
        data.put("renewalRate", totalTenants > 0 ? 
            BigDecimal.valueOf(renewedTenants * 100.0 / totalTenants).setScale(1, RoundingMode.HALF_UP) : 0);
        data.put("totalTenants", totalTenants);
        data.put("renewedTenants", renewedTenants);

        // 平均租期（天）
        data.put("avgRentDays", contractCount > 0 ? totalRentDays / contractCount : 0);

        return Result.success(data);
    }

    /**
     * 获取收入来源分布
     */
    @GetMapping("/income/distribution")
    public Result getIncomeDistribution(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        List<Long> contractIds = getContractIds(landlordId);

        List<Map<String, Object>> result = new ArrayList<>();
        if (contractIds.isEmpty()) {
            result.add(Map.of("name", "首期支付", "value", 0));
            result.add(Map.of("name", "租金收入", "value", 0));
            return Result.success(result);
        }

        // 首期支付
        LambdaQueryWrapper<PaymentOrder> firstWrapper = new LambdaQueryWrapper<>();
        firstWrapper.in(PaymentOrder::getContractId, contractIds)
                .eq(PaymentOrder::getPaymentStatus, 1)
                .eq(PaymentOrder::getOrderType, 0);
        List<PaymentOrder> firstOrders = paymentOrderMapper.selectList(firstWrapper);
        BigDecimal firstPayment = firstOrders.stream()
                .map(PaymentOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 租金收入
        LambdaQueryWrapper<PaymentOrder> rentWrapper = new LambdaQueryWrapper<>();
        rentWrapper.in(PaymentOrder::getContractId, contractIds)
                .eq(PaymentOrder::getPaymentStatus, 1)
                .eq(PaymentOrder::getOrderType, 1);
        List<PaymentOrder> rentOrders = paymentOrderMapper.selectList(rentWrapper);
        BigDecimal rentIncome = rentOrders.stream()
                .map(PaymentOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.add(Map.of("name", "首期支付", "value", firstPayment));
        result.add(Map.of("name", "租金收入", "value", rentIncome));

        return Result.success(result);
    }

    /**
     * 获取合同到期预警
     */
    @GetMapping("/contract/expiring")
    public Result getExpiringContracts(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        LocalDate today = LocalDate.now();

        Map<String, Object> data = new HashMap<>();

        // 7天内到期
        LambdaQueryWrapper<RentalContract> wrapper7 = new LambdaQueryWrapper<>();
        wrapper7.eq(RentalContract::getLandlordId, landlordId)
                .eq(RentalContract::getStatus, 2)
                .ge(RentalContract::getRentEndDate, today)
                .le(RentalContract::getRentEndDate, today.plusDays(7));
        long expiring7 = rentalContractMapper.selectCount(wrapper7);
        data.put("expiring7Days", expiring7);

        // 30天内到期
        LambdaQueryWrapper<RentalContract> wrapper30 = new LambdaQueryWrapper<>();
        wrapper30.eq(RentalContract::getLandlordId, landlordId)
                .eq(RentalContract::getStatus, 2)
                .ge(RentalContract::getRentEndDate, today)
                .le(RentalContract::getRentEndDate, today.plusDays(30));
        long expiring30 = rentalContractMapper.selectCount(wrapper30);
        data.put("expiring30Days", expiring30);

        // 即将到期合同列表
        LambdaQueryWrapper<RentalContract> listWrapper = new LambdaQueryWrapper<>();
        listWrapper.eq(RentalContract::getLandlordId, landlordId)
                .eq(RentalContract::getStatus, 2)
                .ge(RentalContract::getRentEndDate, today)
                .le(RentalContract::getRentEndDate, today.plusDays(30))
                .orderByAsc(RentalContract::getRentEndDate);
        List<RentalContract> contracts = rentalContractMapper.selectList(listWrapper);

        List<Map<String, Object>> list = new ArrayList<>();
        for (RentalContract contract : contracts) {
            Map<String, Object> item = new HashMap<>();
            item.put("contractId", contract.getContractId());
            item.put("contractNo", contract.getContractNo());
            item.put("rentEndDate", contract.getRentEndDate());
            item.put("daysLeft", ChronoUnit.DAYS.between(today, contract.getRentEndDate()));

            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null) {
                item.put("houseTitle", house.getTitle());
            }

            User tenant = userMapper.selectById(contract.getTenantId());
            if (tenant != null) {
                item.put("tenantName", tenant.getRealName() != null ? tenant.getRealName() : tenant.getUsername());
                item.put("tenantPhone", tenant.getPhone());
            }

            list.add(item);
        }
        data.put("expiringList", list);

        return Result.success(data);
    }

    // ==================== 辅助方法 ====================

    private List<Long> getContractIds(Long landlordId) {
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getLandlordId, landlordId).select(RentalContract::getContractId);
        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);
        return contracts.stream().map(RentalContract::getContractId).toList();
    }

    // ==================== 单个房源数据看板 ====================

    /**
     * 获取单个房源的数据看板
     * 包含：浏览量趋势（近30天）、咨询量、出租率
     */
    @GetMapping("/house/{houseId}/dashboard")
    public Result getHouseDashboard(@PathVariable Long houseId, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        House house = houseMapper.selectById(houseId);
        if (house == null || !house.getLandlordId().equals(landlordId)) {
            return Result.failure("房源不存在或无权查看");
        }

        Map<String, Object> data = new HashMap<>();

        // 基础数据
        data.put("houseId", house.getHouseId());
        data.put("title", house.getTitle());
        data.put("viewCount", house.getViewCount() != null ? house.getViewCount() : 0);
        data.put("collectCount", house.getCollectCount() != null ? house.getCollectCount() : 0);
        data.put("rentPrice", house.getRentPrice());
        data.put("status", house.getStatus());

        // 咨询量（chat_session 数量）
        Long consultCount = chatSessionMapper.countConsultByHouseId(houseId);
        data.put("consultCount", consultCount);

        // 出租率：该房源历史合同中已确认/到期/终止的合同数 / 总合同数
        LambdaQueryWrapper<RentalContract> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(RentalContract::getHouseId, houseId);
        long totalContracts = rentalContractMapper.selectCount(totalWrapper);

        LambdaQueryWrapper<RentalContract> signedWrapper = new LambdaQueryWrapper<>();
        signedWrapper.eq(RentalContract::getHouseId, houseId)
                .in(RentalContract::getStatus, Arrays.asList(2, 3, 4));
        long signedContracts = rentalContractMapper.selectCount(signedWrapper);

        double occupancyRate = totalContracts > 0 ?
                BigDecimal.valueOf(signedContracts * 100.0 / totalContracts)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue() : 0;
        data.put("occupancyRate", occupancyRate);
        data.put("totalContracts", totalContracts);
        data.put("signedContracts", signedContracts);

        // 当前是否出租中
        LambdaQueryWrapper<RentalContract> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(RentalContract::getHouseId, houseId).eq(RentalContract::getStatus, 2);
        boolean isRented = rentalContractMapper.selectCount(activeWrapper) > 0;
        data.put("isRented", isRented);

        // 近30天浏览量趋势（用 view_count 当前值模拟，实际项目可接入埋点表）
        // 这里生成近30天的模拟趋势数据（基于当前浏览量均匀分布+随机波动）
        List<Map<String, Object>> viewTrend = buildViewTrend(house.getViewCount());
        data.put("viewTrend", viewTrend);

        // 近6个月收入趋势
        List<Map<String, Object>> incomeTrend = buildHouseIncomeTrend(houseId);
        data.put("incomeTrend", incomeTrend);

        return Result.success(data);
    }

    /**
     * 生成近30天浏览量趋势（基于总浏览量的模拟分布）
     */
    private List<Map<String, Object>> buildViewTrend(Integer totalViewCount) {
        List<Map<String, Object>> trend = new ArrayList<>();
        int total = totalViewCount != null ? totalViewCount : 0;
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        // 简单分布：越近的日期权重越高
        int[] weights = new int[30];
        int weightSum = 0;
        for (int i = 0; i < 30; i++) {
            weights[i] = i + 1; // 权重递增，最近的权重最大
            weightSum += weights[i];
        }

        Random random = new Random(total); // 用总量作为种子，保证同一房源结果稳定
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            int base = total > 0 ? (int) Math.round(total * weights[29 - i] * 1.0 / weightSum) : 0;
            // 加一点随机波动（±20%）
            int fluctuation = base > 0 ? (int) (base * 0.2 * (random.nextDouble() - 0.5)) : 0;
            int count = Math.max(0, base + fluctuation);

            Map<String, Object> item = new HashMap<>();
            item.put("date", date.format(fmt));
            item.put("count", count);
            trend.add(item);
        }
        return trend;
    }

    /**
     * 近6个月该房源收入趋势
     */
    private List<Map<String, Object>> buildHouseIncomeTrend(Long houseId) {
        // 查该房源所有合同ID
        LambdaQueryWrapper<RentalContract> cw = new LambdaQueryWrapper<>();
        cw.eq(RentalContract::getHouseId, houseId).select(RentalContract::getContractId);
        List<RentalContract> contracts = rentalContractMapper.selectList(cw);
        List<Long> contractIds = contracts.stream().map(RentalContract::getContractId).toList();

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = today.minusMonths(i);
            String monthKey = monthDate.format(fmt);
            BigDecimal income = BigDecimal.ZERO;

            if (!contractIds.isEmpty()) {
                LocalDateTime monthStart = monthDate.withDayOfMonth(1).atStartOfDay();
                LocalDateTime monthEnd = monthDate.withDayOfMonth(monthDate.lengthOfMonth()).atTime(23, 59, 59);
                LambdaQueryWrapper<PaymentOrder> pw = new LambdaQueryWrapper<>();
                pw.in(PaymentOrder::getContractId, contractIds)
                        .eq(PaymentOrder::getPaymentStatus, 1)
                        .ne(PaymentOrder::getOrderType, 2)
                        .ge(PaymentOrder::getPaymentTime, monthStart)
                        .le(PaymentOrder::getPaymentTime, monthEnd);
                List<PaymentOrder> orders = paymentOrderMapper.selectList(pw);
                for (PaymentOrder o : orders) {
                    income = income.add(o.getPayAmount());
                }
            }

            Map<String, Object> item = new HashMap<>();
            item.put("month", monthKey);
            item.put("income", income);
            result.add(item);
        }
        return result;
    }
}