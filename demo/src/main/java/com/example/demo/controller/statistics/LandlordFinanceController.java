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
import com.example.demo.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/landlord/finance")
public class LandlordFinanceController {

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    @Autowired
    private RentalContractMapper rentalContractMapper;
    @Autowired
    private HouseMapper houseMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private NotificationService notificationService;

    /**
     * 获取财务统计
     */
    @GetMapping("/stats")
    public Result getFinanceStats(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        Map<String, Object> stats = new HashMap<>();

        // 待收租金数
        Long pending = paymentOrderMapper.countPendingByLandlordId(landlordId);
        stats.put("pendingCount", pending != null ? pending : 0);

        // 逾期租金数
        Long overdue = paymentOrderMapper.countOverdueByLandlordId(landlordId);
        stats.put("overdueCount", overdue != null ? overdue : 0);

        // 本月已收
        BigDecimal monthlyReceived = paymentOrderMapper.sumMonthlyReceivedByLandlordId(landlordId);
        stats.put("monthlyReceived", monthlyReceived != null ? monthlyReceived : BigDecimal.ZERO);

        // 本月预计
        BigDecimal monthlyExpected = paymentOrderMapper.sumMonthlyExpectedByLandlordId(landlordId);
        stats.put("monthlyExpected", monthlyExpected != null ? monthlyExpected : BigDecimal.ZERO);

        // 待处理押金退还
        List<Long> contractIds = getContractIdsByLandlord(landlordId);
        if (!contractIds.isEmpty()) {
            LambdaQueryWrapper<PaymentOrder> refundWrapper = new LambdaQueryWrapper<>();
            refundWrapper.in(PaymentOrder::getContractId, contractIds)
                    .eq(PaymentOrder::getOrderType, 2)
                    .eq(PaymentOrder::getPaymentStatus, 0);
            stats.put("pendingRefund", paymentOrderMapper.selectCount(refundWrapper));
        } else {
            stats.put("pendingRefund", 0);
        }

        return Result.success(stats);
    }

    /**
     * 获取租金收取列表
     */
    @GetMapping("/rent/list")
    public Result getRentList(
            @RequestParam(required = false) Integer status, // 0待支付 1已支付 2逾期
            @RequestParam(required = false) Long houseId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        List<Long> contractIds = getContractIdsByLandlord(landlordId);
        if (contractIds.isEmpty()) {
            return Result.success(Map.of("records", new ArrayList<>(), "total", 0));
        }

        // 如果指定了房源，过滤合同
        if (houseId != null) {
            LambdaQueryWrapper<RentalContract> cw = new LambdaQueryWrapper<>();
            cw.eq(RentalContract::getLandlordId, landlordId)
                    .eq(RentalContract::getHouseId, houseId)
                    .select(RentalContract::getContractId);
            List<RentalContract> filtered = rentalContractMapper.selectList(cw);
            contractIds = filtered.stream().map(RentalContract::getContractId).toList();
            if (contractIds.isEmpty()) {
                return Result.success(Map.of("records", new ArrayList<>(), "total", 0));
            }
        }

        Page<PaymentOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PaymentOrder::getContractId, contractIds);
        wrapper.in(PaymentOrder::getOrderType, Arrays.asList(0, 1)); // 首期和租金

        if (status != null) {
            if (status == 2) {
                // 逾期：待支付且创建超过3天
                wrapper.eq(PaymentOrder::getPaymentStatus, 0);
                wrapper.lt(PaymentOrder::getCreateTime, LocalDateTime.now().minusDays(3));
            } else {
                wrapper.eq(PaymentOrder::getPaymentStatus, status);
            }
        }
        wrapper.orderByDesc(PaymentOrder::getCreateTime);

        Page<PaymentOrder> result = paymentOrderMapper.selectPage(pageParam, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (PaymentOrder order : result.getRecords()) {
            Map<String, Object> map = buildOrderInfo(order);
            // 判断是否逾期
            if (order.getPaymentStatus() == 0 && 
                order.getCreateTime().isBefore(LocalDateTime.now().minusDays(3))) {
                map.put("isOverdue", true);
            } else {
                map.put("isOverdue", false);
            }
            records.add(map);
        }

        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    /**
     * 发送催缴提醒（模拟）
     */
    @PostMapping("/rent/remind/{orderId}")
    public Result sendReminder(@PathVariable Long orderId, HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) {
            return Result.failure("订单不存在");
        }

        RentalContract contract = rentalContractMapper.selectById(order.getContractId());
        if (contract == null || !contract.getLandlordId().equals(landlordId)) {
            return Result.failure("无权操作");
        }

        if (order.getPaymentStatus() != 0) {
            return Result.failure("该订单已支付");
        }

        // 发送催缴通知给租客
        House house = houseMapper.selectById(contract.getHouseId());
        String houseTitle = house != null ? house.getTitle() : "房源";
        notificationService.notifyPaymentOverdue(contract.getTenantId(), houseTitle, orderId);

        // 模拟发送提醒（实际可发短信/推送）
        return Result.success("催缴提醒已发送");
    }


    /**
     * 获取押金管理列表
     */
    @GetMapping("/deposit/list")
    public Result getDepositList(
            @RequestParam(required = false) Integer status, // 0待退还 1已退还 2已扣除
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        List<Long> contractIds = getContractIdsByLandlord(landlordId);
        if (contractIds.isEmpty()) {
            return Result.success(Map.of("records", new ArrayList<>(), "total", 0));
        }

        Page<PaymentOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PaymentOrder::getContractId, contractIds);
        wrapper.eq(PaymentOrder::getOrderType, 2); // 押金退还

        if (status != null) {
            wrapper.eq(PaymentOrder::getPaymentStatus, status);
        }
        wrapper.orderByDesc(PaymentOrder::getCreateTime);

        Page<PaymentOrder> result = paymentOrderMapper.selectPage(pageParam, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (PaymentOrder order : result.getRecords()) {
            records.add(buildOrderInfo(order));
        }

        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    /**
     * 处理押金退还
     */
    @PostMapping("/deposit/process/{orderId}")
    public Result processDepositRefund(@PathVariable Long orderId, 
                                       @RequestBody Map<String, Object> params,
                                       HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null || order.getOrderType() != 2) {
            return Result.failure("订单不存在或类型错误");
        }

        RentalContract contract = rentalContractMapper.selectById(order.getContractId());
        if (contract == null || !contract.getLandlordId().equals(landlordId)) {
            return Result.failure("无权操作");
        }

        if (order.getPaymentStatus() != 0) {
            return Result.failure("该申请已处理");
        }

        Integer action = Integer.valueOf(params.get("action").toString()); // 1同意 2拒绝/扣除
        BigDecimal refundAmount = params.get("refundAmount") != null 
                ? new BigDecimal(params.get("refundAmount").toString()) 
                : order.getTotalAmount();
        String remark = (String) params.get("remark");

        if (action == 1) {
            // 同意退还
            order.setPaymentStatus(1);
            order.setPayAmount(refundAmount);
            order.setPaymentTime(LocalDateTime.now());
        } else {
            // 拒绝或部分扣除
            order.setPaymentStatus(2);
            order.setPayAmount(refundAmount);
            order.setPaymentTime(LocalDateTime.now());
        }
        paymentOrderMapper.updateById(order);

        // 发送通知给租客
        House house = houseMapper.selectById(contract.getHouseId());
        String houseTitle = house != null ? house.getTitle() : "房源";
        if (action == 1) {
            notificationService.notifyDepositRefunded(contract.getTenantId(), houseTitle, refundAmount.toString(), orderId);
        }

        return Result.success(action == 1 ? "押金已退还" : "押金已处理");
    }

    /**
     * 获取收入趋势（近30天）
     */
    @GetMapping("/income/trend")
    public Result getIncomeTrend(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> trend = paymentOrderMapper.getLast30DaysIncomeByLandlordId(landlordId);
        
        // 补全30天数据
        Map<String, BigDecimal> trendMap = new HashMap<>();
        for (Map<String, Object> item : trend) {
            String date = item.get("date").toString();
            BigDecimal amount = new BigDecimal(item.get("amount").toString());
            trendMap.put(date, amount);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(formatter);
            Map<String, Object> item = new HashMap<>();
            item.put("date", dateStr);
            item.put("amount", trendMap.getOrDefault(dateStr, BigDecimal.ZERO));
            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 获取房源收入排行
     */
    @GetMapping("/income/rank")
    public Result getHouseIncomeRank(HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> rank = paymentOrderMapper.getHouseIncomeRankByLandlordId(landlordId);
        return Result.success(rank);
    }

    /**
     * 获取收入统计（按月/季度/年）
     */
    @GetMapping("/income/summary")
    public Result getIncomeSummary(@RequestParam(defaultValue = "month") String type,
                                   HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        List<Long> contractIds = getContractIdsByLandlord(landlordId);
        if (contractIds.isEmpty()) {
            return Result.success(Map.of(
                "totalIncome", BigDecimal.ZERO,
                "rentIncome", BigDecimal.ZERO,
                "depositIncome", BigDecimal.ZERO,
                "orderCount", 0
            ));
        }

        LocalDateTime startTime;
        LocalDateTime endTime = LocalDateTime.now();

        switch (type) {
            case "quarter":
                int currentMonth = LocalDate.now().getMonthValue();
                int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
                startTime = LocalDate.now().withMonth(quarterStartMonth).withDayOfMonth(1).atStartOfDay();
                break;
            case "year":
                startTime = LocalDate.now().withMonth(1).withDayOfMonth(1).atStartOfDay();
                break;
            default: // month
                startTime = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }

        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PaymentOrder::getContractId, contractIds)
                .eq(PaymentOrder::getPaymentStatus, 1)
                .ge(PaymentOrder::getPaymentTime, startTime)
                .le(PaymentOrder::getPaymentTime, endTime);

        List<PaymentOrder> orders = paymentOrderMapper.selectList(wrapper);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal rentIncome = BigDecimal.ZERO;
        BigDecimal depositIncome = BigDecimal.ZERO;

        for (PaymentOrder order : orders) {
            if (order.getOrderType() == 2) {
                // 押金退还是支出，不计入收入
                continue;
            }
            totalIncome = totalIncome.add(order.getPayAmount());
            if (order.getOrderType() == 0) {
                // 首期支付包含租金和押金
                RentalContract contract = rentalContractMapper.selectById(order.getContractId());
                if (contract != null) {
                    rentIncome = rentIncome.add(contract.getMonthlyRent());
                    depositIncome = depositIncome.add(contract.getDepositAmount());
                }
            } else if (order.getOrderType() == 1) {
                rentIncome = rentIncome.add(order.getPayAmount());
            }
        }

        return Result.success(Map.of(
            "totalIncome", totalIncome,
            "rentIncome", rentIncome,
            "depositIncome", depositIncome,
            "orderCount", orders.size()
        ));
    }

    /**
     * 导出财务报表
     */
    @GetMapping("/export")
    public Result exportFinanceReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long landlordId = (Long) request.getAttribute("userId");

        List<Long> contractIds = getContractIdsByLandlord(landlordId);
        if (contractIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PaymentOrder::getContractId, contractIds)
                .eq(PaymentOrder::getPaymentStatus, 1);

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(PaymentOrder::getPaymentTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(PaymentOrder::getPaymentTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(PaymentOrder::getPaymentTime);

        List<PaymentOrder> orders = paymentOrderMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();

        for (PaymentOrder order : orders) {
            Map<String, Object> row = new HashMap<>();
            row.put("orderNo", order.getOrderNo());
            row.put("orderType", getOrderTypeName(order.getOrderType()));
            row.put("totalAmount", order.getTotalAmount());
            row.put("payAmount", order.getPayAmount());
            row.put("paymentMethod", getPaymentMethodName(order.getPaymentMethod()));
            row.put("paymentTime", order.getPaymentTime());

            RentalContract contract = rentalContractMapper.selectById(order.getContractId());
            if (contract != null) {
                row.put("contractNo", contract.getContractNo());
                House house = houseMapper.selectById(contract.getHouseId());
                if (house != null) {
                    row.put("houseName", house.getTitle());
                }
                User tenant = userMapper.selectById(contract.getTenantId());
                if (tenant != null) {
                    row.put("tenantName", tenant.getRealName() != null ? tenant.getRealName() : tenant.getUsername());
                }
            }
            result.add(row);
        }

        return Result.success(result);
    }

    // ==================== 辅助方法 ====================

    private List<Long> getContractIdsByLandlord(Long landlordId) {
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getLandlordId, landlordId).select(RentalContract::getContractId);
        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);
        return contracts.stream().map(RentalContract::getContractId).toList();
    }

    private Map<String, Object> buildOrderInfo(PaymentOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getOrderId());
        map.put("orderNo", order.getOrderNo());
        map.put("orderType", order.getOrderType());
        map.put("totalAmount", order.getTotalAmount());
        map.put("payAmount", order.getPayAmount());
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("paymentStatus", order.getPaymentStatus());
        map.put("paymentTime", order.getPaymentTime());
        map.put("createTime", order.getCreateTime());

        RentalContract contract = rentalContractMapper.selectById(order.getContractId());
        if (contract != null) {
            Map<String, Object> contractInfo = new HashMap<>();
            contractInfo.put("contractId", contract.getContractId());
            contractInfo.put("contractNo", contract.getContractNo());
            map.put("contract", contractInfo);

            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null) {
                Map<String, Object> houseInfo = new HashMap<>();
                houseInfo.put("houseId", house.getHouseId());
                houseInfo.put("title", house.getTitle());
                houseInfo.put("address", house.getAddress());
                houseInfo.put("images", house.getImages());
                map.put("house", houseInfo);
            }

            User tenant = userMapper.selectById(contract.getTenantId());
            if (tenant != null) {
                Map<String, Object> tenantInfo = new HashMap<>();
                tenantInfo.put("userId", tenant.getUserId());
                tenantInfo.put("realName", tenant.getRealName());
                tenantInfo.put("phone", tenant.getPhone());
                map.put("tenant", tenantInfo);
            }
        }

        return map;
    }

    private String getOrderTypeName(Integer orderType) {
        return switch (orderType) {
            case 0 -> "首期支付";
            case 1 -> "租金支付";
            case 2 -> "押金退还";
            default -> "未知";
        };
    }

    private String getPaymentMethodName(Integer method) {
        if (method == null) return "未知";
        return switch (method) {
            case 1 -> "支付宝";
            case 2 -> "微信";
            case 3 -> "银行卡";
            default -> "未知";
        };
    }
}
