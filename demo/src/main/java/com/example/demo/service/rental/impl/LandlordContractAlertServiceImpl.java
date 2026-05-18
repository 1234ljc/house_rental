package com.example.demo.service.rental.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.service.rental.LandlordContractAlertService;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LandlordContractAlertServiceImpl implements LandlordContractAlertService {

    private final RentalContractMapper contractMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public LandlordContractAlertServiceImpl(RentalContractMapper contractMapper, HouseMapper houseMapper, UserMapper userMapper, NotificationService notificationService) {
        this.contractMapper = contractMapper;
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId == null ? null : Long.valueOf(userId.toString());
    }

    @Override
    public Result getStats(HttpServletRequest request) {
        Long landlordId = getUserId(request);
        Map<String, Object> stats = new HashMap<>();
        LocalDate today = LocalDate.now();
        stats.put("totalActive", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2)));
        stats.put("expiring7Days", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(7))));
        stats.put("expiring30Days", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(30))));
        stats.put("expiring90Days", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(90))));
        stats.put("expired", contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2).lt(RentalContract::getRentEndDate, today)));
        return Result.success(stats);
    }

    @Override
    public Result getList(HttpServletRequest request, Integer alertType, String keyword, Integer page, Integer size) {
        Long landlordId = getUserId(request);
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2);
        if (alertType != null) {
            switch (alertType) {
                case 1 -> wrapper.ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(7));
                case 2 -> wrapper.ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(30));
                case 3 -> wrapper.ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(90));
                case 4 -> wrapper.lt(RentalContract::getRentEndDate, today);
            }
        }
        wrapper.orderByAsc(RentalContract::getRentEndDate);
        List<RentalContract> allContracts = contractMapper.selectList(wrapper);
        if (keyword != null && !keyword.isEmpty()) {
            allContracts = allContracts.stream().filter(c -> {
                House house = houseMapper.selectById(c.getHouseId());
                User tenant = userMapper.selectById(c.getTenantId());
                String searchText = (house != null ? house.getTitle() + house.getAddress() : "") + (tenant != null ? tenant.getRealName() + tenant.getUsername() : "") + c.getContractNo();
                return searchText.toLowerCase().contains(keyword.toLowerCase());
            }).toList();
        }
        int total = allContracts.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<RentalContract> pageContracts = start < total ? allContracts.subList(start, end) : new ArrayList<>();
        List<Map<String, Object>> records = new ArrayList<>();
        for (RentalContract c : pageContracts) records.add(buildContractInfo(c, today));
        return Result.success(Map.of("records", records, "total", total));
    }

    @Override
    public Result getCalendar(HttpServletRequest request, Integer year, Integer month) {
        Long landlordId = getUserId(request);
        LocalDate today = LocalDate.now();
        int y = year != null ? year : today.getYear();
        int m = month != null ? month : today.getMonthValue();
        LocalDate monthStart = LocalDate.of(y, m, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        List<RentalContract> contracts = contractMapper.selectList(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, monthStart).le(RentalContract::getRentEndDate, monthEnd));
        Map<Integer, List<Map<String, Object>>> calendarData = new HashMap<>();
        for (RentalContract c : contracts) calendarData.computeIfAbsent(c.getRentEndDate().getDayOfMonth(), k -> new ArrayList<>()).add(buildContractInfo(c, today));
        return Result.success(Map.of("year", y, "month", m, "data", calendarData, "totalCount", contracts.size()));
    }

    @Override
    public Result getTrend(HttpServletRequest request) {
        Long landlordId = getUserId(request);
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            LocalDate monthStart = today.plusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            long count = contractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getLandlordId, landlordId).eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, monthStart).le(RentalContract::getRentEndDate, monthEnd));
            trend.add(Map.of("month", monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue()), "monthLabel", monthStart.getMonthValue() + "月", "count", count));
        }
        return Result.success(trend);
    }

    @Override
    public Result sendNotify(HttpServletRequest request, Long contractId) {
        Long landlordId = getUserId(request);
        RentalContract contract = contractMapper.selectById(contractId);
        if (contract == null || !contract.getLandlordId().equals(landlordId)) return Result.failure("合同不存在或无权操作");
        House house = houseMapper.selectById(contract.getHouseId());
        notificationService.notifyContractExpiring(contract.getTenantId(), contract.getLandlordId(), house != null ? house.getTitle() : "房源", contract.getRentEndDate().toString(), contract.getContractId());
        return Result.success("已发送到期提醒通知");
    }

    private Map<String, Object> buildContractInfo(RentalContract c, LocalDate today) {
        Map<String, Object> map = new HashMap<>();
        map.put("contractId", c.getContractId());
        map.put("contractNo", c.getContractNo());
        map.put("rentStartDate", c.getRentStartDate());
        map.put("rentEndDate", c.getRentEndDate());
        map.put("monthlyRent", c.getMonthlyRent());
        map.put("depositAmount", c.getDepositAmount());
        long daysRemaining = ChronoUnit.DAYS.between(today, c.getRentEndDate());
        map.put("daysRemaining", daysRemaining);
        map.put("alertLevel", daysRemaining < 0 ? "expired" : daysRemaining <= 7 ? "danger" : daysRemaining <= 30 ? "warning" : "info");
        map.put("rentMonths", ChronoUnit.MONTHS.between(c.getRentStartDate(), c.getRentEndDate()));
        House house = houseMapper.selectById(c.getHouseId());
        if (house != null) {
            map.put("houseId", house.getHouseId());
            map.put("houseTitle", house.getTitle());
            map.put("houseAddress", house.getAddress());
        }
        User tenant = userMapper.selectById(c.getTenantId());
        if (tenant != null) {
            map.put("tenantId", tenant.getUserId());
            map.put("tenantName", tenant.getRealName());
            map.put("tenantPhone", tenant.getPhone());
            map.put("tenantAvatar", tenant.getAvatar());
        }
        return map;
    }
}
