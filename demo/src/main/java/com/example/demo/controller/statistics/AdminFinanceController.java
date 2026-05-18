package com.example.demo.controller.statistics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.*;

@RestController
@RequestMapping("/api/admin/finance")
public class AdminFinanceController {

    @Autowired
    private PaymentOrderMapper orderMapper;
    
    @Autowired
    private RentalContractMapper contractMapper;
    
    @Autowired
    private HouseMapper houseMapper;
    
    @Autowired
    private UserMapper userMapper;

    // ==================== 订单管理 ====================

    /**
     * 获取订单列表
     */
    @GetMapping("/order/list")
    public Result getOrderList(
            @RequestParam(defaultValue = "-1") Integer paymentStatus,
            @RequestParam(defaultValue = "-1") Integer orderType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        Page<PaymentOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        
        if (paymentStatus != -1) {
            wrapper.eq(PaymentOrder::getPaymentStatus, paymentStatus);
        }
        if (orderType != -1) {
            wrapper.eq(PaymentOrder::getOrderType, orderType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(PaymentOrder::getOrderNo, keyword);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(PaymentOrder::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(PaymentOrder::getCreateTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        
        wrapper.orderByDesc(PaymentOrder::getCreateTime);
        Page<PaymentOrder> result = orderMapper.selectPage(pageParam, wrapper);
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (PaymentOrder order : result.getRecords()) {
            records.add(buildOrderInfo(order));
        }
        
        return Result.success(Map.of(
                "records", records,
                "total", result.getTotal(),
                "current", result.getCurrent(),
                "size", result.getSize()
        ));
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/order/{id}")
    public Result getOrderDetail(@PathVariable Long id) {
        PaymentOrder order = orderMapper.selectById(id);
        if (order == null) {
            return Result.failure("订单不存在");
        }
        return Result.success(buildOrderInfo(order));
    }

    /**
     * 获取订单统计
     */
    @GetMapping("/order/stats")
    public Result getOrderStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总订单数
        long total = orderMapper.selectCount(null);
        stats.put("total", total);
        
        // 各状态数量
        long pending = orderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getPaymentStatus, 0));
        long paid = orderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getPaymentStatus, 1));
        long failed = orderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getPaymentStatus, 2));
        
        stats.put("pending", pending);
        stats.put("paid", paid);
        stats.put("failed", failed);
        
        // 成功率和失败率
        if (total > 0) {
            stats.put("successRate", BigDecimal.valueOf(paid * 100.0 / total).setScale(1, RoundingMode.HALF_UP));
            stats.put("failRate", BigDecimal.valueOf(failed * 100.0 / total).setScale(1, RoundingMode.HALF_UP));
        } else {
            stats.put("successRate", 0);
            stats.put("failRate", 0);
        }
        
        // 总交易额
        BigDecimal totalAmount = orderMapper.sumTotalAmount();
        stats.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
        
        // 今日订单数和金额
        stats.put("todayOrders", orderMapper.countTodayOrders());
        BigDecimal todayAmount = orderMapper.sumTodayAmount();
        stats.put("todayAmount", todayAmount != null ? todayAmount : BigDecimal.ZERO);
        
        return Result.success(stats);
    }


    // ==================== 收入分析 ====================

    /**
     * 获取收入概览
     */
    @GetMapping("/income/overview")
    public Result getIncomeOverview() {
        Map<String, Object> data = new HashMap<>();
        
        // 总收入
        BigDecimal totalIncome = orderMapper.sumTotalAmount();
        data.put("totalIncome", totalIncome != null ? totalIncome : BigDecimal.ZERO);
        
        // 本月收入
        LambdaQueryWrapper<PaymentOrder> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(PaymentOrder::getPaymentStatus, 1)
                .apply("YEAR(payment_time) = YEAR(CURDATE()) AND MONTH(payment_time) = MONTH(CURDATE())");
        List<PaymentOrder> monthOrders = orderMapper.selectList(monthWrapper);
        BigDecimal monthIncome = monthOrders.stream()
                .map(PaymentOrder::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("monthIncome", monthIncome);
        
        // 今日收入
        BigDecimal todayIncome = orderMapper.sumTodayAmount();
        data.put("todayIncome", todayIncome != null ? todayIncome : BigDecimal.ZERO);
        
        // 订单总数
        data.put("totalOrders", orderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getPaymentStatus, 1)));
        
        // 本月订单数
        data.put("monthOrders", monthOrders.size());
        
        // 今日订单数
        data.put("todayOrders", orderMapper.countTodayOrders());
        
        return Result.success(data);
    }

    /**
     * 获取收入趋势（按日/月/年）
     */
    @GetMapping("/income/trend")
    public Result getIncomeTrend(
            @RequestParam(defaultValue = "day") String type,
            @RequestParam(defaultValue = "30") Integer days) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter;
        
        if ("month".equals(type)) {
            // 按月统计（近12个月）
            formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            for (int i = 11; i >= 0; i--) {
                LocalDate month = today.minusMonths(i).withDayOfMonth(1);
                String monthStr = month.format(formatter);
                
                LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(PaymentOrder::getPaymentStatus, 1)
                        .apply("DATE_FORMAT(payment_time, '%Y-%m') = {0}", monthStr);
                List<PaymentOrder> orders = orderMapper.selectList(wrapper);
                
                BigDecimal amount = orders.stream()
                        .map(PaymentOrder::getPayAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                Map<String, Object> item = new HashMap<>();
                item.put("date", monthStr);
                item.put("amount", amount);
                item.put("count", orders.size());
                result.add(item);
            }
        } else if ("year".equals(type)) {
            // 按年统计（近5年）
            for (int i = 4; i >= 0; i--) {
                int year = today.getYear() - i;
                
                LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(PaymentOrder::getPaymentStatus, 1)
                        .apply("YEAR(payment_time) = {0}", year);
                List<PaymentOrder> orders = orderMapper.selectList(wrapper);
                
                BigDecimal amount = orders.stream()
                        .map(PaymentOrder::getPayAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                Map<String, Object> item = new HashMap<>();
                item.put("date", String.valueOf(year));
                item.put("amount", amount);
                item.put("count", orders.size());
                result.add(item);
            }
        } else {
            // 按日统计（默认近30天）
            formatter = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                
                LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(PaymentOrder::getPaymentStatus, 1)
                        .apply("DATE(payment_time) = {0}", date.toString());
                List<PaymentOrder> orders = orderMapper.selectList(wrapper);
                
                BigDecimal amount = orders.stream()
                        .map(PaymentOrder::getPayAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                Map<String, Object> item = new HashMap<>();
                item.put("date", date.format(formatter));
                item.put("amount", amount);
                item.put("count", orders.size());
                result.add(item);
            }
        }
        
        return Result.success(result);
    }

    /**
     * 获取收入构成分析
     */
    @GetMapping("/income/composition")
    public Result getIncomeComposition() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        String[] types = {"首期支付", "租金支付", "押金退还"};
        for (int i = 0; i < types.length; i++) {
            LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentOrder::getPaymentStatus, 1)
                    .eq(PaymentOrder::getOrderType, i);
            List<PaymentOrder> orders = orderMapper.selectList(wrapper);
            
            BigDecimal amount = orders.stream()
                    .map(PaymentOrder::getPayAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", types[i]);
            item.put("value", amount);
            item.put("count", orders.size());
            result.add(item);
        }
        
        return Result.success(result);
    }

    /**
     * 获取支付方式分布
     */
    @GetMapping("/income/payment-method")
    public Result getPaymentMethodDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        String[] methods = {"未知", "支付宝", "微信", "银行卡"};
        for (int i = 1; i <= 3; i++) {
            LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentOrder::getPaymentStatus, 1)
                    .eq(PaymentOrder::getPaymentMethod, i);
            List<PaymentOrder> orders = orderMapper.selectList(wrapper);
            
            BigDecimal amount = orders.stream()
                    .map(PaymentOrder::getPayAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", methods[i]);
            item.put("value", amount);
            item.put("count", orders.size());
            result.add(item);
        }
        
        return Result.success(result);
    }

    /**
     * 导出财务报表数据
     */
    @GetMapping("/income/export")
    public Result exportFinanceReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getPaymentStatus, 1);
        
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(PaymentOrder::getPaymentTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(PaymentOrder::getPaymentTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        
        wrapper.orderByDesc(PaymentOrder::getPaymentTime);
        List<PaymentOrder> orders = orderMapper.selectList(wrapper);
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (PaymentOrder order : orders) {
            records.add(buildOrderInfo(order));
        }
        
        // 统计汇总
        BigDecimal totalAmount = orders.stream()
                .map(PaymentOrder::getPayAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return Result.success(Map.of(
                "records", records,
                "total", orders.size(),
                "totalAmount", totalAmount
        ));
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> buildOrderInfo(PaymentOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("orderNo", order.getOrderNo());
        map.put("contractId", order.getContractId());
        map.put("orderType", order.getOrderType());
        map.put("totalAmount", order.getTotalAmount());
        map.put("payAmount", order.getPayAmount());
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("paymentStatus", order.getPaymentStatus());
        map.put("paymentTime", order.getPaymentTime());
        map.put("createTime", order.getCreateTime());
        
        // 合同和房源信息
        RentalContract contract = contractMapper.selectById(order.getContractId());
        if (contract != null) {
            map.put("contractNo", contract.getContractNo());
            
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null) {
                map.put("houseTitle", house.getTitle());
                map.put("houseAddress", house.getAddress());
            }
            
            // 房东信息
            User landlord = userMapper.selectById(contract.getLandlordId());
            if (landlord != null) {
                map.put("landlordName", landlord.getRealName() != null ? landlord.getRealName() : landlord.getUsername());
                map.put("landlordPhone", landlord.getPhone());
            }
            
            // 租客信息
            User tenant = userMapper.selectById(contract.getTenantId());
            if (tenant != null) {
                map.put("tenantName", tenant.getRealName() != null ? tenant.getRealName() : tenant.getUsername());
                map.put("tenantPhone", tenant.getPhone());
            }
        }
        
        return map;
    }
}
