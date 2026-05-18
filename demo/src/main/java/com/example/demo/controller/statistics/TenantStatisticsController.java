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
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/tenant/statistics")
public class TenantStatisticsController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RentalContractMapper rentalContractMapper;
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    @Autowired
    private HouseMapper houseMapper;
    @Autowired
    private FavoriteMapper favoriteMapper;

    @GetMapping("/overview")
    public Result getOverview(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        Map<String, Object> data = new HashMap<>();

        User user = userMapper.selectById(tenantId);
        
        int beans = user != null && user.getBeans() != null ? user.getBeans() : 0;
        data.put("beans", beans);
        data.put("beansValue", new BigDecimal(beans).divide(new BigDecimal(1000), 2, RoundingMode.DOWN));

        LambdaQueryWrapper<RentalContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(RentalContract::getTenantId, tenantId);
        List<RentalContract> contracts = rentalContractMapper.selectList(contractWrapper);
        
        int totalContracts = contracts.size();
        int activeContracts = 0;
        int completedContracts = 0;
        long totalRentDays = 0;
        
        for (RentalContract c : contracts) {
            if (c.getStatus() == 2) {
                activeContracts++;
                if (c.getRentStartDate() != null) {
                    totalRentDays += ChronoUnit.DAYS.between(c.getRentStartDate(), LocalDate.now());
                }
            } else if (c.getStatus() == 3 || c.getStatus() == 4) {
                completedContracts++;
                if (c.getRentStartDate() != null && c.getRentEndDate() != null) {
                    totalRentDays += ChronoUnit.DAYS.between(c.getRentStartDate(), c.getRentEndDate());
                }
            }
        }
        
        data.put("totalContracts", totalContracts);
        data.put("activeContracts", activeContracts);
        data.put("completedContracts", completedContracts);
        data.put("totalRentDays", totalRentDays);
        data.put("totalRentMonths", totalRentDays / 30);

        List<Long> contractIds = contracts.stream().map(RentalContract::getContractId).toList();
        if (!contractIds.isEmpty()) {
            LambdaQueryWrapper<PaymentOrder> paidWrapper = new LambdaQueryWrapper<>();
            paidWrapper.in(PaymentOrder::getContractId, contractIds)
                    .eq(PaymentOrder::getPaymentStatus, 1)
                    .ne(PaymentOrder::getOrderType, 2);
            List<PaymentOrder> paidOrders = paymentOrderMapper.selectList(paidWrapper);
            
            BigDecimal totalPaid = paidOrders.stream()
                    .map(PaymentOrder::getPayAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            data.put("totalPaid", totalPaid);
            data.put("totalOrders", paidOrders.size());
            
            LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            BigDecimal monthPaid = paidOrders.stream()
                    .filter(o -> o.getPaymentTime() != null && o.getPaymentTime().isAfter(monthStart))
                    .map(PaymentOrder::getPayAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            data.put("monthPaid", monthPaid);
        } else {
            data.put("totalPaid", BigDecimal.ZERO);
            data.put("totalOrders", 0);
            data.put("monthPaid", BigDecimal.ZERO);
        }

        LambdaQueryWrapper<Favorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(Favorite::getUserId, tenantId);
        data.put("favoriteCount", favoriteMapper.selectCount(favWrapper));

        return Result.success(data);
    }

    @GetMapping("/expense-trend")
    public Result getExpenseTrend(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<RentalContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(RentalContract::getTenantId, tenantId);
        List<RentalContract> contracts = rentalContractMapper.selectList(contractWrapper);
        List<Long> contractIds = contracts.stream().map(RentalContract::getContractId).toList();

        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        
        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            LocalDateTime monthStart = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime monthEnd = month.plusMonths(1).withDayOfMonth(1).atStartOfDay();
            
            BigDecimal amount = BigDecimal.ZERO;
            if (!contractIds.isEmpty()) {
                LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
                wrapper.in(PaymentOrder::getContractId, contractIds)
                        .eq(PaymentOrder::getPaymentStatus, 1)
                        .ne(PaymentOrder::getOrderType, 2)
                        .ge(PaymentOrder::getPaymentTime, monthStart)
                        .lt(PaymentOrder::getPaymentTime, monthEnd);
                List<PaymentOrder> orders = paymentOrderMapper.selectList(wrapper);
                amount = orders.stream()
                        .map(PaymentOrder::getPayAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("month", month.getYear() + "-" + String.format("%02d", month.getMonthValue()));
            item.put("monthLabel", month.getMonthValue() + "月");
            item.put("amount", amount);
            trend.add(item);
        }

        return Result.success(trend);
    }

    @GetMapping("/expense-category")
    public Result getExpenseCategory(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<RentalContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(RentalContract::getTenantId, tenantId);
        List<RentalContract> contracts = rentalContractMapper.selectList(contractWrapper);
        List<Long> contractIds = contracts.stream().map(RentalContract::getContractId).toList();

        List<Map<String, Object>> categories = new ArrayList<>();
        
        if (!contractIds.isEmpty()) {
            LambdaQueryWrapper<PaymentOrder> firstWrapper = new LambdaQueryWrapper<>();
            firstWrapper.in(PaymentOrder::getContractId, contractIds)
                    .eq(PaymentOrder::getPaymentStatus, 1)
                    .eq(PaymentOrder::getOrderType, 0);
            List<PaymentOrder> firstOrders = paymentOrderMapper.selectList(firstWrapper);
            BigDecimal firstAmount = firstOrders.stream()
                    .map(PaymentOrder::getPayAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            LambdaQueryWrapper<PaymentOrder> rentWrapper = new LambdaQueryWrapper<>();
            rentWrapper.in(PaymentOrder::getContractId, contractIds)
                    .eq(PaymentOrder::getPaymentStatus, 1)
                    .eq(PaymentOrder::getOrderType, 1);
            List<PaymentOrder> rentOrders = paymentOrderMapper.selectList(rentWrapper);
            BigDecimal rentAmount = rentOrders.stream()
                    .map(PaymentOrder::getPayAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            categories.add(Map.of("name", "首期支付", "value", firstAmount));
            categories.add(Map.of("name", "租金支付", "value", rentAmount));
        }

        return Result.success(categories);
    }


    @GetMapping("/rent-history")
    public Result getRentHistory(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");

        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getTenantId, tenantId)
                .orderByDesc(RentalContract::getCreateTime);
        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);

        List<Map<String, Object>> history = new ArrayList<>();
        for (RentalContract c : contracts) {
            Map<String, Object> item = new HashMap<>();
            item.put("contractId", c.getContractId());
            item.put("contractNo", c.getContractNo());
            item.put("status", c.getStatus());
            item.put("statusName", getStatusName(c.getStatus()));
            item.put("startDate", c.getRentStartDate());
            item.put("endDate", c.getRentEndDate());
            item.put("monthlyRent", c.getMonthlyRent());
            item.put("depositAmount", c.getDepositAmount());
            
            House house = houseMapper.selectById(c.getHouseId());
            if (house != null) {
                item.put("houseTitle", house.getTitle());
                item.put("houseAddress", house.getAddress());
            }
            
            if (c.getRentStartDate() != null && c.getRentEndDate() != null) {
                long months = ChronoUnit.MONTHS.between(c.getRentStartDate(), c.getRentEndDate());
                item.put("rentMonths", months);
            }
            
            history.add(item);
        }

        return Result.success(history);
    }

    @GetMapping("/behavior")
    public Result getBehaviorStats(HttpServletRequest request) {
        Long tenantId = (Long) request.getAttribute("userId");
        Map<String, Object> data = new HashMap<>();

        LambdaQueryWrapper<Favorite> favWrapper2 = new LambdaQueryWrapper<>();
        favWrapper2.eq(Favorite::getUserId, tenantId);
        long totalFavorites = favoriteMapper.selectCount(favWrapper2);
        data.put("totalFavorites", totalFavorites);

        LambdaQueryWrapper<RentalContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(RentalContract::getTenantId, tenantId);
        List<RentalContract> contracts = rentalContractMapper.selectList(contractWrapper);
        
        data.put("totalApplications", contracts.size());
        data.put("successApplications", contracts.stream().filter(c -> c.getStatus() >= 2).count());
        data.put("contractRate", 0);

        return Result.success(data);
    }

    private String getStatusName(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "待确认";
            case 2 -> "生效中";
            case 3 -> "已到期";
            case 4 -> "已终止";
            case 5 -> "已拒绝";
            default -> "未知";
        };
    }
}
