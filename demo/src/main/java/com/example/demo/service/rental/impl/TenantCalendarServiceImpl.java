package com.example.demo.service.rental.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.repository.rental.entity.PaymentOrder;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.rental.PaymentOrderMapper;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.service.rental.TenantCalendarService;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class TenantCalendarServiceImpl implements TenantCalendarService {

    private final RentalContractMapper contractMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final HouseMapper houseMapper;

    public TenantCalendarServiceImpl(RentalContractMapper contractMapper,
                                     PaymentOrderMapper paymentOrderMapper,
                                     HouseMapper houseMapper) {
        this.contractMapper = contractMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.houseMapper = houseMapper;
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId == null ? null : Long.valueOf(userId.toString());
    }

    @Override
    public Result getCalendarEvents(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<Map<String, Object>> events = new ArrayList<>();
        List<RentalContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getTenantId, userId).in(RentalContract::getStatus, 2, 3));
        for (RentalContract c : contracts) {
            House house = houseMapper.selectById(c.getHouseId());
            String title = house != null ? house.getTitle() : "房源";
            if (c.getRentStartDate() != null) {
                Map<String, Object> ev = new HashMap<>();
                ev.put("id", "contract_start_" + c.getContractId());
                ev.put("type", "contract_start");
                ev.put("title", "租期开始: " + title);
                ev.put("date", c.getRentStartDate().toString());
                ev.put("color", "#67c23a");
                ev.put("contractId", c.getContractId());
                events.add(ev);
            }
            if (c.getRentEndDate() != null) {
                Map<String, Object> ev = new HashMap<>();
                ev.put("id", "contract_end_" + c.getContractId());
                ev.put("type", "contract_end");
                ev.put("title", "租期结束: " + title);
                ev.put("date", c.getRentEndDate().toString());
                ev.put("color", "#f56c6c");
                ev.put("contractId", c.getContractId());
                events.add(ev);
            }
        }
        for (RentalContract c : contracts) {
            List<PaymentOrder> orders = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getContractId, c.getContractId()).eq(PaymentOrder::getPaymentStatus, 0));
            House house = houseMapper.selectById(c.getHouseId());
            String houseTitle = house != null ? house.getTitle() : "房源";
            for (PaymentOrder o : orders) {
                Map<String, Object> ev = new HashMap<>();
                ev.put("id", "payment_" + o.getOrderId());
                ev.put("type", "payment");
                ev.put("title", "待付款: " + houseTitle);
                ev.put("date", o.getCreateTime().toLocalDate().toString());
                ev.put("amount", o.getTotalAmount());
                ev.put("color", "#e6a23c");
                events.add(ev);
            }
        }
        return Result.success(events);
    }

    @Override
    public Result getContractTimeline(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<Map<String, Object>> timeline = new ArrayList<>();
        List<RentalContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getTenantId, userId).orderByDesc(RentalContract::getCreateTime));
        for (RentalContract c : contracts) {
            House house = houseMapper.selectById(c.getHouseId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getContractId());
            item.put("houseTitle", house != null ? house.getTitle() : "未知房源");
            item.put("houseAddress", house != null ? house.getAddress() : "");
            item.put("startDate", c.getRentStartDate());
            item.put("endDate", c.getRentEndDate());
            item.put("monthlyRent", c.getMonthlyRent());
            item.put("status", c.getStatus());
            item.put("statusText", getContractStatusText(c.getStatus()));
            if (c.getStatus() == 2 && c.getRentEndDate() != null) item.put("daysLeft", Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), c.getRentEndDate())));
            timeline.add(item);
        }
        return Result.success(timeline);
    }

    @Override
    public Result getReminders(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<Map<String, Object>> reminders = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate next30Days = today.plusDays(30);
        List<RentalContract> expiringContracts = contractMapper.selectList(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getTenantId, userId).eq(RentalContract::getStatus, 2).le(RentalContract::getRentEndDate, next30Days).ge(RentalContract::getRentEndDate, today));
        for (RentalContract c : expiringContracts) {
            House house = houseMapper.selectById(c.getHouseId());
            long days = ChronoUnit.DAYS.between(today, c.getRentEndDate());
            reminders.add(Map.of("type", "contract_expiry", "icon", "Warning", "title", "合同即将到期", "content", (house != null ? house.getTitle() : "房源") + " 还有 " + days + " 天到期", "date", c.getRentEndDate(), "daysLeft", days, "level", days <= 7 ? "danger" : "warning"));
        }
        List<RentalContract> activeContracts = contractMapper.selectList(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getTenantId, userId).eq(RentalContract::getStatus, 2));
        for (RentalContract c : activeContracts) {
            List<PaymentOrder> pendingPayments = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getContractId, c.getContractId()).eq(PaymentOrder::getPaymentStatus, 0));
            House house = houseMapper.selectById(c.getHouseId());
            for (PaymentOrder o : pendingPayments) {
                reminders.add(Map.of("type", "payment", "icon", "Money", "title", "待付款", "content", (house != null ? house.getTitle() : "房源") + " ¥" + o.getTotalAmount(), "amount", o.getTotalAmount(), "level", "warning"));
            }
        }
        reminders.sort((a, b) -> {
            int levelA = "danger".equals(a.get("level")) ? 0 : ("warning".equals(a.get("level")) ? 1 : 2);
            int levelB = "danger".equals(b.get("level")) ? 0 : ("warning".equals(b.get("level")) ? 1 : 2);
            return levelA - levelB;
        });
        return Result.success(reminders);
    }

    @Override
    public Result getCalendarStats(HttpServletRequest request) {
        Long userId = getUserId(request);
        Map<String, Object> stats = new HashMap<>();
        long activeContracts = contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getTenantId, userId).eq(RentalContract::getStatus, 2));
        stats.put("activeContracts", activeContracts);
        List<RentalContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getTenantId, userId).eq(RentalContract::getStatus, 2));
        long pendingPayments = 0;
        for (RentalContract c : contracts) pendingPayments += paymentOrderMapper.selectCount(new LambdaQueryWrapper<PaymentOrder>().eq(PaymentOrder::getContractId, c.getContractId()).eq(PaymentOrder::getPaymentStatus, 0));
        stats.put("pendingPayments", pendingPayments);
        long expiringContracts = contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getTenantId, userId).eq(RentalContract::getStatus, 2).le(RentalContract::getRentEndDate, LocalDate.now().plusDays(30)).ge(RentalContract::getRentEndDate, LocalDate.now()));
        stats.put("expiringContracts", expiringContracts);
        long totalRentals = contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getTenantId, userId).in(RentalContract::getStatus, 2, 3));
        stats.put("totalRentals", totalRentals);
        return Result.success(stats);
    }

    private String getContractStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待确认";
            case 1 -> "待生效";
            case 2 -> "生效中";
            case 3 -> "已完成";
            case 4 -> "已终止";
            default -> "未知";
        };
    }
}
