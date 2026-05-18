package com.example.demo.service.rental.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.service.rental.AdminContractAlertService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AdminContractAlertServiceImpl implements AdminContractAlertService {

    private final RentalContractMapper rentalContractMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public AdminContractAlertServiceImpl(RentalContractMapper rentalContractMapper, HouseMapper houseMapper, UserMapper userMapper, NotificationService notificationService) {
        this.rentalContractMapper = rentalContractMapper;
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    public Result getStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDate today = LocalDate.now();
        stats.put("totalActive", rentalContractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2)));
        stats.put("expiring7Days", rentalContractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(7))));
        stats.put("expiring30Days", rentalContractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(30))));
        stats.put("expiring90Days", rentalContractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(90))));
        stats.put("expired", rentalContractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2).lt(RentalContract::getRentEndDate, today)));
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        stats.put("expiringThisMonth", rentalContractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, monthEnd)));
        long completedCount = rentalContractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 3));
        long terminatedCount = rentalContractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 4));
        long totalEnded = completedCount + terminatedCount;
        stats.put("renewRate", totalEnded > 0 ? new BigDecimal((double) completedCount / totalEnded * 100).setScale(1, RoundingMode.HALF_UP) : 0);
        return Result.success(stats);
    }

    @Override
    public Result getList(Integer alertType, String keyword, Integer page, Integer size) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getStatus, 2);
        if (alertType != null) {
            switch (alertType) {
                case 1 -> wrapper.ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(7));
                case 2 -> wrapper.ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(30));
                case 3 -> wrapper.ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(90));
                case 4 -> wrapper.lt(RentalContract::getRentEndDate, today);
            }
        }
        wrapper.orderByAsc(RentalContract::getRentEndDate);
        List<RentalContract> allContracts = rentalContractMapper.selectList(wrapper);
        if (keyword != null && !keyword.isEmpty()) {
            allContracts = allContracts.stream().filter(c -> {
                House house = houseMapper.selectById(c.getHouseId());
                User tenant = userMapper.selectById(c.getTenantId());
                User landlord = userMapper.selectById(c.getLandlordId());
                String searchText = (house != null ? house.getTitle() + house.getAddress() : "") + (tenant != null ? tenant.getRealName() + tenant.getUsername() : "") + (landlord != null ? landlord.getRealName() + landlord.getUsername() : "") + c.getContractNo();
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
    public Result getCalendar(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int y = year != null ? year : today.getYear();
        int m = month != null ? month : today.getMonthValue();
        LocalDate monthStart = LocalDate.of(y, m, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        List<RentalContract> contracts = rentalContractMapper.selectList(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, monthStart).le(RentalContract::getRentEndDate, monthEnd));
        Map<Integer, List<Map<String, Object>>> calendarData = new HashMap<>();
        for (RentalContract c : contracts) calendarData.computeIfAbsent(c.getRentEndDate().getDayOfMonth(), k -> new ArrayList<>()).add(buildContractInfo(c, today));
        return Result.success(Map.of("year", y, "month", m, "data", calendarData, "totalCount", contracts.size()));
    }

    @Override
    public Result getTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            LocalDate monthStart = today.plusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            long count = rentalContractMapper.selectCount(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2).ge(RentalContract::getRentEndDate, monthStart).le(RentalContract::getRentEndDate, monthEnd));
            trend.add(Map.of("month", monthStart.getYear() + "-" + String.format("%02d", monthStart.getMonthValue()), "monthLabel", monthStart.getMonthValue() + "月", "count", count));
        }
        return Result.success(trend);
    }

    @Override
    public Result sendNotify(Long contractId) {
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null) {
            return Result.failure("合同不存在");
        }
        House house = houseMapper.selectById(contract.getHouseId());
        notificationService.notifyContractExpiring(contract.getTenantId(), contract.getLandlordId(), house != null ? house.getTitle() : "房源", contract.getRentEndDate().toString(), contract.getContractId());
        return Result.success("已发送到期提醒通知");
    }

    @Override
    public Result batchNotify(Map<String, Object> params) {
        Integer alertType = (Integer) params.get("alertType");
        if (alertType == null) {
            return Result.failure("请选择预警类型");
        }
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<RentalContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalContract::getStatus, 2);
        switch (alertType) {
            case 1 -> wrapper.ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(7));
            case 2 -> wrapper.ge(RentalContract::getRentEndDate, today).le(RentalContract::getRentEndDate, today.plusDays(30));
            default -> { return Result.failure("无效的预警类型"); }
        }
        List<RentalContract> contracts = rentalContractMapper.selectList(wrapper);
        int count = 0;
        for (RentalContract c : contracts) {
            House house = houseMapper.selectById(c.getHouseId());
            notificationService.notifyContractExpiring(c.getTenantId(), c.getLandlordId(), house != null ? house.getTitle() : "房源", c.getRentEndDate().toString(), c.getContractId());
            count++;
        }
        return Result.success("已发送 " + count + " 条到期提醒通知");
    }

    @Override
    public Result markExpired(Long contractId) {
        RentalContract contract = rentalContractMapper.selectById(contractId);
        if (contract == null) {
            return Result.failure("合同不存在");
        }
        if (contract.getStatus() != 2) {
            return Result.failure("只能标记生效中的合同");
        }
        contract.setStatus(3);
        rentalContractMapper.updateById(contract);
        House house = houseMapper.selectById(contract.getHouseId());
        if (house != null && house.getStatus() == 2) {
            house.setStatus(1);
            houseMapper.updateById(house);
        }
        return Result.success("合同已标记为到期");
    }

    @Override
    public Result batchMarkExpired() {
        LocalDate today = LocalDate.now();
        List<RentalContract> expiredContracts = rentalContractMapper.selectList(new LambdaQueryWrapper<RentalContract>().eq(RentalContract::getStatus, 2).lt(RentalContract::getRentEndDate, today));
        int count = 0;
        for (RentalContract c : expiredContracts) {
            c.setStatus(3);
            rentalContractMapper.updateById(c);
            House house = houseMapper.selectById(c.getHouseId());
            if (house != null && house.getStatus() == 2) {
                house.setStatus(1);
                houseMapper.updateById(house);
            }
            count++;
        }
        return Result.success("已处理 " + count + " 份过期合同");
    }

    private Map<String, Object> buildContractInfo(RentalContract c, LocalDate today) {
        Map<String, Object> map = new HashMap<>();
        map.put("contractId", c.getContractId());
        map.put("contractNo", c.getContractNo());
        map.put("rentStartDate", c.getRentStartDate());
        map.put("rentEndDate", c.getRentEndDate());
        map.put("monthlyRent", c.getMonthlyRent());
        map.put("depositAmount", c.getDepositAmount());
        map.put("status", c.getStatus());
        map.put("daysRemaining", ChronoUnit.DAYS.between(today, c.getRentEndDate()));
        long daysRemaining = ChronoUnit.DAYS.between(today, c.getRentEndDate());
        map.put("alertLevel", daysRemaining < 0 ? "expired" : daysRemaining <= 7 ? "danger" : daysRemaining <= 30 ? "warning" : "info");
        map.put("rentMonths", ChronoUnit.MONTHS.between(c.getRentStartDate(), c.getRentEndDate()));
        House house = houseMapper.selectById(c.getHouseId());
        if (house != null) {
            map.put("houseId", house.getHouseId());
            map.put("houseTitle", house.getTitle());
            map.put("houseAddress", house.getAddress());
            map.put("houseImages", house.getImages());
        }
        User tenant = userMapper.selectById(c.getTenantId());
        if (tenant != null) {
            map.put("tenantId", tenant.getUserId());
            map.put("tenantName", tenant.getRealName());
            map.put("tenantPhone", tenant.getPhone());
            map.put("tenantAvatar", tenant.getAvatar());
        }
        User landlord = userMapper.selectById(c.getLandlordId());
        if (landlord != null) {
            map.put("landlordId", landlord.getUserId());
            map.put("landlordName", landlord.getRealName());
            map.put("landlordPhone", landlord.getPhone());
            map.put("landlordAvatar", landlord.getAvatar());
        }
        return map;
    }
}
