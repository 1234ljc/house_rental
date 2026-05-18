package com.example.demo.service.rental.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.repository.rental.entity.RentalContract;
import com.example.demo.repository.rental.entity.RentalManage;
import com.example.demo.repository.rental.RentalContractMapper;
import com.example.demo.repository.rental.RentalManageMapper;
import com.example.demo.service.rental.AdminAfterService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
// 负责平台售后工单统计、催办、强制处理和房东排名监管功能实现。
public class AdminAfterServiceImpl implements AdminAfterService {

    private final RentalManageMapper rentalManageMapper;
    private final RentalContractMapper rentalContractMapper;
    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public AdminAfterServiceImpl(RentalManageMapper rentalManageMapper,
                                 RentalContractMapper rentalContractMapper,
                                 HouseMapper houseMapper,
                                 UserMapper userMapper,
                                 NotificationService notificationService) {
        this.rentalManageMapper = rentalManageMapper;
        this.rentalContractMapper = rentalContractMapper;
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    // 平台售后概览：统计工单总量、处理率、工单类型和超时情况。
    @Override
    public Result getStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = rentalManageMapper.selectCount(null);
        stats.put("total", total);
        stats.put("pending", rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getStatus, 0)));
        stats.put("processing", rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getStatus, 1)));
        long completed = rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getStatus, 2));
        stats.put("completed", completed);
        stats.put("completeRate", total > 0 ? new BigDecimal((double) completed / total * 100).setScale(1, RoundingMode.HALF_UP) : 0);
        stats.put("repairCount", rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getManageType, 0)));
        stats.put("otherCount", rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getManageType, 1)));
        List<RentalManage> completedList = rentalManageMapper.selectList(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getStatus, 2).isNotNull(RentalManage::getCompleteTime));
        if (!completedList.isEmpty()) {
            double avgHours = completedList.stream().mapToLong(rm -> ChronoUnit.HOURS.between(rm.getCreateTime(), rm.getCompleteTime())).average().orElse(0);
            stats.put("avgProcessHours", new BigDecimal(avgHours).setScale(1, RoundingMode.HALF_UP));
            List<RentalManage> ratedList = completedList.stream().filter(rm -> rm.getRating() != null && rm.getRating() > 0).toList();
            double avgRating = ratedList.isEmpty() ? 0 : ratedList.stream().mapToInt(RentalManage::getRating).average().orElse(0);
            stats.put("avgRating", new BigDecimal(avgRating).setScale(1, RoundingMode.HALF_UP));
        } else {
            stats.put("avgProcessHours", 0);
            stats.put("avgRating", 0);
        }
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);
        stats.put("overtime", rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getStatus, 0).lt(RentalManage::getCreateTime, threshold)));
        return Result.success(stats);
    }

    // 售后趋势：按月统计新增工单和已完成工单数量，便于看平台售后压力变化。
    @Override
    public Result getTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 11; i >= 0; i--) {



            LocalDate month = now.minusMonths(i);
            LocalDateTime monthStart = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime monthEnd = month.plusMonths(1).withDayOfMonth(1).atStartOfDay();
            long newCount = rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().ge(RentalManage::getCreateTime, monthStart).lt(RentalManage::getCreateTime, monthEnd));
            long completeCount = rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getStatus, 2).ge(RentalManage::getCompleteTime, monthStart).lt(RentalManage::getCompleteTime, monthEnd));
            trend.add(Map.of("month", month.getYear() + "-" + String.format("%02d", month.getMonthValue()), "monthLabel", month.getMonthValue() + "月", "newCount", newCount, "completeCount", completeCount));
        }
        return Result.success(trend);
    }

    // 工单类型分布：区分维修申请和其他问题，方便判断售后侧重点。
    @Override
    public Result getTypeDistribution() {
        return Result.success(List.of(
                Map.of("name", "维修申请", "value", rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getManageType, 0))),
                Map.of("name", "其他问题", "value", rentalManageMapper.selectCount(new LambdaQueryWrapper<RentalManage>().eq(RentalManage::getManageType, 1)))
        ));
    }

    // 工单列表：按类型、状态、超时和关键字筛选具体售后记录。
    @Override
    public Result getList(Integer manageType, Integer status, Boolean overtime, String keyword, Integer page, Integer size) {
        Page<RentalManage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RentalManage> wrapper = new LambdaQueryWrapper<>();
        if (manageType != null) {
            wrapper.eq(RentalManage::getManageType, manageType);
        }
        if (status != null) {
            wrapper.eq(RentalManage::getStatus, status);
        }
        if (Boolean.TRUE.equals(overtime)) {
            wrapper.eq(RentalManage::getStatus, 0).lt(RentalManage::getCreateTime, LocalDateTime.now().minusHours(48));
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(RentalManage::getContent, keyword);
        }
        wrapper.orderByDesc(RentalManage::getCreateTime);
        Page<RentalManage> result = rentalManageMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (RentalManage rm : result.getRecords()) {
            records.add(buildManageInfo(rm));
        }
        return Result.success(Map.of("records", records, "total", result.getTotal()));
    }

    // 工单详情：读取单条售后记录并拼装房源、租客和合同信息。
    @Override
    public Result getDetail(Long manageId) {
        RentalManage rm = rentalManageMapper.selectById(manageId);
        if (rm == null) {
            return Result.failure("工单不存在");
        }
        return Result.success(buildManageInfo(rm));
    }

    // 催办流程：对超时未处理工单发送提醒给房东。
    @Override
    public Result urge(Long manageId) {
        RentalManage rm = rentalManageMapper.selectById(manageId);
        if (rm == null) {
            return Result.failure("工单不存在");
        }
        if (rm.getStatus() != 0) {
            return Result.failure("该工单已在处理中或已完成");
        }
        RentalContract contract = rentalContractMapper.selectById(rm.getContractId());
        if (contract == null) {
            return Result.failure("合同不存在");
        }
        House house = houseMapper.selectById(contract.getHouseId());
        String houseTitle = house != null ? house.getTitle() : "房源";
        String typeName = rm.getManageType() == 0 ? "维修申请" : "问题反馈";
        notificationService.send(contract.getLandlordId(), 5, "租后服务催促提醒", "您有一条" + typeName + "工单（房源：" + houseTitle + "）已超时未处理，请尽快处理，以免影响您的服务评分。", rm.getManageId());
        return Result.success("已发送催促通知给房东");
    }

    // 强制完成流程：管理员直接结单，并把处理结果通知给租客。
    @Override
    public Result forceComplete(Long manageId, Map<String, String> params) {
        RentalManage rm = rentalManageMapper.selectById(manageId);
        if (rm == null) {
            return Result.failure("工单不存在");
        }
        String reason = params.get("reason");
        if (reason == null || reason.isEmpty()) {
            return Result.failure("请填写处理说明");
        }
        rm.setStatus(2);
        rm.setResponseContent("[管理员处理] " + reason);
        rm.setCompleteTime(LocalDateTime.now());
        rentalManageMapper.updateById(rm);
        notificationService.send(rm.getUserId(), 5, "工单已处理", "您的工单已由平台管理员处理完成：" + reason, rm.getManageId());
        return Result.success("工单已强制完成");
    }

    // 房东售后排名：按处理效率、完成率和评分对房东进行对比展示。
    @Override
    public Result getLandlordRanking(String sortBy) {
        List<RentalManage> allManages = rentalManageMapper.selectList(null);
        Map<Long, List<RentalManage>> groupByLandlord = new HashMap<>();
        for (RentalManage rm : allManages) {
            RentalContract contract = rentalContractMapper.selectById(rm.getContractId());
            if (contract != null) {
                groupByLandlord.computeIfAbsent(contract.getLandlordId(), k -> new ArrayList<>()).add(rm);
            }
        }
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map.Entry<Long, List<RentalManage>> entry : groupByLandlord.entrySet()) {
            Long landlordId = entry.getKey();
            List<RentalManage> manages = entry.getValue();
            User landlord = userMapper.selectById(landlordId);
            if (landlord == null) {
                continue;
            }
            long totalCount = manages.size();
            long completedCount = manages.stream().filter(m -> m.getStatus() == 2).count();
            long pendingCount = manages.stream().filter(m -> m.getStatus() == 0).count();
            double completeRate = totalCount > 0 ? (double) completedCount / totalCount * 100 : 0;
            List<RentalManage> completedManages = manages.stream().filter(m -> m.getStatus() == 2 && m.getCompleteTime() != null).toList();
            double avgHours = completedManages.isEmpty() ? 0 : completedManages.stream().mapToLong(m -> ChronoUnit.HOURS.between(m.getCreateTime(), m.getCompleteTime())).average().orElse(0);
            List<RentalManage> ratedManages = completedManages.stream().filter(m -> m.getRating() != null && m.getRating() > 0).toList();
            double avgRating = ratedManages.isEmpty() ? 0 : ratedManages.stream().mapToInt(RentalManage::getRating).average().orElse(0);
            Map<String, Object> item = new HashMap<>();
            item.put("landlordId", landlordId);
            item.put("username", landlord.getUsername());
            item.put("realName", landlord.getRealName());
            item.put("avatar", landlord.getAvatar());
            item.put("totalCount", totalCount);
            item.put("completedCount", completedCount);
            item.put("pendingCount", pendingCount);
            item.put("completeRate", new BigDecimal(completeRate).setScale(1, RoundingMode.HALF_UP));
            item.put("avgHours", new BigDecimal(avgHours).setScale(1, RoundingMode.HALF_UP));
            item.put("avgRating", new BigDecimal(avgRating).setScale(1, RoundingMode.HALF_UP));
            ranking.add(item);
        }
        if ("rating".equals(sortBy)) {
            ranking.sort((a, b) -> ((BigDecimal) b.get("avgRating")).compareTo((BigDecimal) a.get("avgRating")));
        } else if ("complete".equals(sortBy)) {
            ranking.sort((a, b) -> ((BigDecimal) b.get("completeRate")).compareTo((BigDecimal) a.get("completeRate")));
        } else {
            ranking.sort((a, b) -> ((BigDecimal) a.get("avgHours")).compareTo((BigDecimal) b.get("avgHours")));
        }
        return Result.success(ranking.stream().limit(20).toList());
    }

    // 超时工单列表：找出超过 48 小时未处理的工单，供平台重点督办。
    @Override
    public Result getOvertimeList() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);
        LambdaQueryWrapper<RentalManage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentalManage::getStatus, 0).lt(RentalManage::getCreateTime, threshold).orderByAsc(RentalManage::getCreateTime);
        List<RentalManage> list = rentalManageMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RentalManage rm : list) {
            Map<String, Object> info = buildManageInfo(rm);
            long overtimeHours = ChronoUnit.HOURS.between(rm.getCreateTime(), LocalDateTime.now()) - 48;
            info.put("overtimeHours", overtimeHours);
            result.add(info);
        }
        return Result.success(result);
    }

    private Map<String, Object> buildManageInfo(RentalManage rm) {
        Map<String, Object> map = new HashMap<>();
        map.put("manageId", rm.getManageId());
        map.put("contractId", rm.getContractId());
        map.put("manageType", rm.getManageType());
        map.put("manageTypeName", rm.getManageType() == 0 ? "维修申请" : "其他问题");
        map.put("content", rm.getContent());
        map.put("images", rm.getImages());
        map.put("status", rm.getStatus());
        map.put("statusName", switch (rm.getStatus()) { case 0 -> "待处理"; case 1 -> "处理中"; case 2 -> "已完成"; default -> "未知"; });
        map.put("responseContent", rm.getResponseContent());
        map.put("rating", rm.getRating());
        map.put("createTime", rm.getCreateTime());
        map.put("completeTime", rm.getCompleteTime());
        if (rm.getStatus() == 2 && rm.getCompleteTime() != null) map.put("processHours", ChronoUnit.HOURS.between(rm.getCreateTime(), rm.getCompleteTime()));
        else if (rm.getStatus() == 0) {
            long hours = ChronoUnit.HOURS.between(rm.getCreateTime(), LocalDateTime.now());
            map.put("waitingHours", hours);
            map.put("isOvertime", hours > 48);
        }
        User user = userMapper.selectById(rm.getUserId());
        if (user != null) {
            map.put("userId", user.getUserId());
            map.put("username", user.getUsername());
            map.put("userRealName", user.getRealName());
            map.put("userAvatar", user.getAvatar());
            map.put("userPhone", user.getPhone());
        }
        RentalContract contract = rentalContractMapper.selectById(rm.getContractId());
        if (contract != null) {
            map.put("contractNo", contract.getContractNo());
            User landlord = userMapper.selectById(contract.getLandlordId());
            if (landlord != null) {
                map.put("landlordId", landlord.getUserId());
                map.put("landlordName", landlord.getRealName());
                map.put("landlordPhone", landlord.getPhone());
                map.put("landlordAvatar", landlord.getAvatar());
            }
            House house = houseMapper.selectById(contract.getHouseId());
            if (house != null) {
                map.put("houseId", house.getHouseId());
                map.put("houseTitle", house.getTitle());
                map.put("houseAddress", house.getAddress());
            }
        }
        return map;
    }
}
