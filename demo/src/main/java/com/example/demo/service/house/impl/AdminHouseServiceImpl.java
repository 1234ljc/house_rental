package com.example.demo.service.house.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.house.AdminHouseService;
import com.example.demo.service.notification.NotificationService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminHouseServiceImpl implements AdminHouseService {

    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public AdminHouseServiceImpl(HouseMapper houseMapper, UserMapper userMapper, NotificationService notificationService) {
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    public Result getAuditList(Integer status, String keyword, Integer page, Integer size) {
        Page<House> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        if (status != null && status != -1) {
            wrapper.eq(House::getStatus, status);
        }
        if (StrUtil.isNotEmpty(keyword)) {
            wrapper.and(w -> w.like(House::getTitle, keyword).or().like(House::getAddress, keyword));
        }
        wrapper.orderByDesc(House::getCreateTime);
        Page<House> result = houseMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (House house : result.getRecords()) {
            records.add(buildHouseInfo(house));
        }
        return Result.success(Map.of("records", records, "total", result.getTotal(), "current", result.getCurrent(), "size", result.getSize()));
    }

    @Override
    public Result getHouseDetail(Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        return Result.success(buildHouseInfo(house));
    }

    @Override
    public Result approveHouse(Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        if (house.getStatus() != 0) {
            return Result.failure("该房源不是待审核状态");
        }
        LambdaUpdateWrapper<House> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(House::getHouseId, houseId).set(House::getStatus, 1).set(House::getAuditReason, null).set(House::getUpdateTime, LocalDateTime.now());
        houseMapper.update(null, wrapper);
        notificationService.notifyHouseApproved(house.getLandlordId(), house.getTitle(), houseId);
        return Result.success("审核通过");
    }

    @Override
    public Result rejectHouse(Long houseId, Map<String, String> body) {
        String reason = body.get("reason");
        if (StrUtil.isBlank(reason)) {
            return Result.failure("请填写驳回原因");
        }
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        if (house.getStatus() != 0) {
            return Result.failure("该房源不是待审核状态");
        }
        LambdaUpdateWrapper<House> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(House::getHouseId, houseId).set(House::getStatus, 4).set(House::getAuditReason, reason).set(House::getUpdateTime, LocalDateTime.now());
        houseMapper.update(null, wrapper);
        notificationService.notifyHouseRejected(house.getLandlordId(), house.getTitle(), reason, houseId);
        return Result.success("已驳回");
    }

    @Override
    public Result batchApprove(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Result.failure("请选择要审核的房源");
        }
        LambdaUpdateWrapper<House> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(House::getHouseId, ids).eq(House::getStatus, 0).set(House::getStatus, 1).set(House::getAuditReason, null).set(House::getUpdateTime, LocalDateTime.now());
        int count = houseMapper.update(null, wrapper);
        return Result.success("成功审核通过 " + count + " 条房源");
    }

    @Override
    public Result batchReject(List<Long> ids, String reason) {
        if (CollUtil.isEmpty(ids)) {
            return Result.failure("请选择要驳回的房源");
        }
        if (StrUtil.isBlank(reason)) {
            return Result.failure("请填写驳回原因");
        }
        LambdaUpdateWrapper<House> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(House::getHouseId, ids).eq(House::getStatus, 0).set(House::getStatus, 4).set(House::getAuditReason, reason).set(House::getUpdateTime, LocalDateTime.now());
        int count = houseMapper.update(null, wrapper);
        return Result.success("成功驳回 " + count + " 条房源");
    }

    @Override
    public Result getAuditStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 0)));
        stats.put("approved", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 1)));
        stats.put("rejected", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 4)));
        stats.put("total", houseMapper.selectCount(null));
        return Result.success(stats);
    }

    @Override
    public Result getMonitorList(Integer status, String city, String district, Integer minPrice, Integer maxPrice, String keyword, Integer page, Integer size) {
        Page<House> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        if (status != -1) {
            wrapper.eq(House::getStatus, status);
        }
        if (StrUtil.isNotEmpty(city)) {
            wrapper.eq(House::getCity, city);
        }
        if (StrUtil.isNotEmpty(district)) {
            wrapper.eq(House::getDistrict, district);
        }
        if (minPrice != null) {
            wrapper.ge(House::getRentPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(House::getRentPrice, maxPrice);
        }
        if (StrUtil.isNotEmpty(keyword)) {
            wrapper.and(w -> w.like(House::getTitle, keyword).or().like(House::getAddress, keyword));
        }
        wrapper.orderByDesc(House::getCreateTime);
        Page<House> result = houseMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (House house : result.getRecords()) {
            records.add(buildHouseInfo(house));
        }
        return Result.success(Map.of("records", records, "total", result.getTotal(), "current", result.getCurrent(), "size", result.getSize()));
    }

    @Override
    public Result getMonitorStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 0)));
        stats.put("available", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 1)));
        stats.put("rented", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 2)));
        stats.put("offline", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 3)));
        stats.put("rejected", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 4)));
        stats.put("total", houseMapper.selectCount(null));
        return Result.success(stats);
    }

    @Override
    public Result offlineHouse(Long houseId, String reason) {
        if (StrUtil.isBlank(reason)) {
            return Result.failure("请填写下架原因");
        }
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        if (house.getStatus() == 3) {
            return Result.failure("该房源已下架");
        }
        LambdaUpdateWrapper<House> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(House::getHouseId, houseId).set(House::getStatus, 3).set(House::getAuditReason, "管理员下架：" + reason).set(House::getUpdateTime, LocalDateTime.now());
        houseMapper.update(null, wrapper);
        notificationService.notifyHouseOffline(house.getLandlordId(), house.getTitle(), reason, houseId);
        return Result.success("房源已下架");
    }

    @Override
    public Result onlineHouse(Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        if (house.getStatus() != 3) {
            return Result.failure("只能恢复已下架的房源");
        }
        LambdaUpdateWrapper<House> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(House::getHouseId, houseId).set(House::getStatus, 1).set(House::getAuditReason, null).set(House::getUpdateTime, LocalDateTime.now());
        houseMapper.update(null, wrapper);
        return Result.success("房源已恢复上架");
    }

    @Override
    public Result editHouse(Long houseId, Map<String, Object> body) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        LambdaUpdateWrapper<House> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(House::getHouseId, houseId);
        if (body.containsKey("title")) {
            wrapper.set(House::getTitle, body.get("title").toString());
        }
        if (body.containsKey("description")) {
            wrapper.set(House::getDescription, body.get("description").toString());
        }
        if (body.containsKey("address")) {
            wrapper.set(House::getAddress, body.get("address").toString());
        }
        if (body.containsKey("rentPrice")) {
            wrapper.set(House::getRentPrice, new BigDecimal(body.get("rentPrice").toString()));
        }
        if (body.containsKey("area")) {
            wrapper.set(House::getArea, new BigDecimal(body.get("area").toString()));
        }
        wrapper.set(House::getUpdateTime, LocalDateTime.now());
        houseMapper.update(null, wrapper);
        return Result.success("房源信息已更新");
    }

    @Override
    public Result batchOffline(List<Integer> ids, String reason) {
        if (CollUtil.isEmpty(ids)) {
            return Result.failure("请选择要下架的房源");
        }
        if (StrUtil.isBlank(reason)) {
            return Result.failure("请填写下架原因");
        }
        List<Long> longIds = ids.stream().map(Integer::longValue).toList();
        LambdaUpdateWrapper<House> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(House::getHouseId, longIds).ne(House::getStatus, 3).set(House::getStatus, 3).set(House::getAuditReason, "管理员下架：" + reason).set(House::getUpdateTime, LocalDateTime.now());
        int count = houseMapper.update(null, wrapper);
        return Result.success("成功下架 " + count + " 条房源");
    }

    @Override
    public Result getAreaDistribution() {
        return Result.success(houseMapper.getHotCities());
    }

    @Override
    public Result getPriceDistribution() {
        List<House> houses = houseMapper.selectList(new LambdaQueryWrapper<House>().eq(House::getStatus, 1));
        Map<String, Long> buckets = new LinkedHashMap<>();
        buckets.put("0-1000", 0L);
        buckets.put("1000-2000", 0L);
        buckets.put("2000-3000", 0L);
        buckets.put("3000-5000", 0L);
        buckets.put("5000+", 0L);
        for (House house : houses) {
            BigDecimal price = house.getRentPrice();
            if (price == null) {
                continue;
            }
            if (price.compareTo(BigDecimal.valueOf(1000)) < 0) {
                buckets.compute("0-1000", (k, v) -> v + 1);
            } else if (price.compareTo(BigDecimal.valueOf(2000)) < 0) {
                buckets.compute("1000-2000", (k, v) -> v + 1);
            } else if (price.compareTo(BigDecimal.valueOf(3000)) < 0) {
                buckets.compute("2000-3000", (k, v) -> v + 1);
            } else if (price.compareTo(BigDecimal.valueOf(5000)) < 0) {
                buckets.compute("3000-5000", (k, v) -> v + 1);
            } else {
                buckets.compute("5000+", (k, v) -> v + 1);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : buckets.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result getHotRanking(Integer limit) {
        int size = limit == null || limit <= 0 ? 10 : limit;
        List<House> houses = houseMapper.selectList(new LambdaQueryWrapper<House>()
                .eq(House::getStatus, 1)
                .orderByDesc(House::getViewCount)
                .orderByDesc(House::getCollectCount));
        List<Map<String, Object>> records = new ArrayList<>();
        int rank = 1;
        for (House house : houses) {
            if (rank > size) {
                break;
            }
            Map<String, Object> info = buildHouseInfo(house);
            info.put("rank", rank++);
            records.add(info);
        }
        return Result.success(records);
    }

    @Override
    public Result getPublishTrend(Integer days) {
        int range = days == null || days <= 0 ? 30 : days;
        List<Map<String, Object>> trend = houseMapper.getLast30DaysTrend();
        if (range < 30 && trend.size() > range) {
            trend = trend.subList(Math.max(0, trend.size() - range), trend.size());
        }
        return Result.success(trend);
    }

    @Override
    public Result getTypeDistribution() {
        List<House> houses = houseMapper.selectList(new LambdaQueryWrapper<House>().eq(House::getStatus, 1));
        Map<String, Long> dist = new LinkedHashMap<>();
        for (House house : houses) {
            String type = house.getRentOption() == null ? "未知" : switch (house.getRentOption()) {
                case 1 -> "整租";
                case 2 -> "合租";
                case 3 -> "都支持";
                default -> "未知";
            };
            dist.put(type, dist.getOrDefault(type, 0L) + 1);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : dist.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result getAnalysisOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();
        Long total = houseMapper.selectCount(null);
        Long available = houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 1));
        Long rented = houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 2));
        Long offline = houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 3));
        Long rejected = houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getStatus, 4));
        Long todayNew = houseMapper.countTodayNew();
        Long totalViews = houseMapper.selectMaps(new LambdaQueryWrapper<House>().select(House::getViewCount)).stream()
                .mapToLong(row -> {
                    Object v = row.get("viewCount");
                    return v == null ? 0L : Long.parseLong(v.toString());
                }).sum();
        Long monthNew = houseMapper.selectCount(new LambdaQueryWrapper<House>().ge(House::getCreateTime, LocalDateTime.now().minusMonths(1)));
        List<Map<String, Object>> priceRows = houseMapper.selectMaps(new LambdaQueryWrapper<House>().select(House::getRentPrice));
        long priceCount = priceRows.stream()
                .map(row -> row.get("rentPrice"))
                .filter(Objects::nonNull)
                .count();
        Long avgPrice = priceCount == 0 ? 0L : (long) priceRows.stream()
                .map(row -> row.get("rentPrice"))
                .filter(Objects::nonNull)
                .mapToLong(v -> new BigDecimal(v.toString()).longValue())
                .average().orElse(0);

        stats.put("totalHouses", total == null ? 0 : total);
        stats.put("availableHouses", available == null ? 0 : available);
        stats.put("rentedHouses", rented == null ? 0 : rented);
        stats.put("offlineHouses", offline == null ? 0 : offline);
        stats.put("rejectedHouses", rejected == null ? 0 : rejected);
        stats.put("todayNew", todayNew == null ? 0 : todayNew);
        stats.put("monthNew", monthNew == null ? 0 : monthNew);
        stats.put("avgPrice", avgPrice == null ? 0 : avgPrice);
        stats.put("totalViews", totalViews == null ? 0 : totalViews);
        return Result.success(stats);
    }

    private Map<String, Object> buildHouseInfo(House house) {
        Map<String, Object> map = new HashMap<>();
        map.put("houseId", house.getHouseId());
        map.put("title", house.getTitle());
        map.put("description", house.getDescription());
        map.put("address", house.getAddress());
        map.put("province", house.getProvince());
        map.put("city", house.getCity());
        map.put("district", house.getDistrict());
        map.put("rentPrice", house.getRentPrice());
        map.put("area", house.getArea());
        map.put("houseType", house.getHouseType());
        map.put("floor", house.getFloor());
        map.put("orientation", house.getOrientation());
        map.put("status", house.getStatus());
        map.put("auditReason", house.getAuditReason());
        map.put("images", house.getImages());
        map.put("facilities", house.getFacilities());
        map.put("viewCount", house.getViewCount());
        map.put("collectCount", house.getCollectCount());
        map.put("createTime", house.getCreateTime());
        map.put("rentOption", house.getRentOption());
        User landlord = userMapper.selectById(house.getLandlordId());
        if (landlord != null) {
            map.put("landlordId", landlord.getUserId());
            map.put("landlordName", landlord.getRealName() != null ? landlord.getRealName() : landlord.getUsername());
            map.put("landlordPhone", landlord.getPhone());
        }
        return map;
    }
}
