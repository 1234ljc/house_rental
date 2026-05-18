package com.example.demo.service.house.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.house.LandlordHouseService;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LandlordHouseServiceImpl implements LandlordHouseService {

    private final HouseMapper houseMapper;
    private final UserMapper userMapper;

    public LandlordHouseServiceImpl(HouseMapper houseMapper, UserMapper userMapper) {
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Result publishHouse(Long userId, Object dto) {
        House house = new House();
        house.setLandlordId(userId);
        copyDto(dto, house);
        house.setStatus(0);
        house.setViewCount(0);
        house.setCollectCount(0);
        house.setCreateTime(LocalDateTime.now());
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.insert(house);
        return Result.success(house.getHouseId());
    }

    @Override
    public Result getHouseList(Long userId, Integer status, String keyword, Integer page, Integer size) {
        Page<House> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getLandlordId, userId);
        if (status != null) {
            wrapper.eq(House::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(House::getTitle, keyword)
                    .or().like(House::getAddress, keyword)
                    .or().like(House::getCity, keyword)
                    .or().like(House::getDistrict, keyword));
        }
        wrapper.orderByDesc(House::getUpdateTime);
        Page<House> result = houseMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (House house : result.getRecords()) {
            records.add(buildHouseInfo(house));
        }
        return Result.success(Map.of("records", records, "total", result.getTotal(), "current", result.getCurrent(), "size", result.getSize()));
    }

    @Override
    public Result getHouseDetail(Long userId, Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        if (!Objects.equals(house.getLandlordId(), userId)) {
            return Result.failure("无权查看该房源");
        }
        return Result.success(buildHouseInfo(house));
    }

    @Override
    public Result updateHouse(Long userId, Long houseId, Object dto) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        if (!Objects.equals(house.getLandlordId(), userId)) {
            return Result.failure("无权操作该房源");
        }
        copyDto(dto, house);
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        return Result.success("房源已更新");
    }

    @Override
    public Result offlineHouse(Long userId, Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        if (!Objects.equals(house.getLandlordId(), userId)) {
            return Result.failure("无权操作该房源");
        }
        house.setStatus(3);
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        return Result.success("房源已下架");
    }

    @Override
    public Result onlineHouse(Long userId, Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        if (!Objects.equals(house.getLandlordId(), userId)) {
            return Result.failure("无权操作该房源");
        }
        if (house.getStatus() == 4) {
            return Result.failure("审核驳回的房源不能直接上架，请重新编辑后提交审核");
        }
        house.setStatus(1);
        house.setUpdateTime(LocalDateTime.now());
        houseMapper.updateById(house);
        return Result.success("房源已上架");
    }

    @Override
    public Result deleteHouse(Long userId, Long houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }
        if (!Objects.equals(house.getLandlordId(), userId)) {
            return Result.failure("无权操作该房源");
        }
        if (house.getStatus() == 2) {
            return Result.failure("已出租房源不能删除");
        }
        houseMapper.deleteById(houseId);
        return Result.success("房源已删除");
    }

    @Override
    public Result getHouseStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        LambdaQueryWrapper<House> base = new LambdaQueryWrapper<>();
        base.eq(House::getLandlordId, userId);
        stats.put("total", houseMapper.selectCount(base));
        stats.put("draft", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getLandlordId, userId).eq(House::getStatus, 0)));
        stats.put("online", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getLandlordId, userId).eq(House::getStatus, 1)));
        stats.put("rented", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getLandlordId, userId).eq(House::getStatus, 2)));
        stats.put("offline", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getLandlordId, userId).eq(House::getStatus, 3)));
        stats.put("rejected", houseMapper.selectCount(new LambdaQueryWrapper<House>().eq(House::getLandlordId, userId).eq(House::getStatus, 4)));
        return Result.success(stats);
    }

    @Override
    public Result batchOperate(Long userId, Map<String, Object> params) {
        Object idsObj = params.get("houseIds");
        Object actionObj = params.get("action");
        if (!(idsObj instanceof Collection<?> idsRaw) || actionObj == null) {
            return Result.failure("参数错误");
        }
        String action = actionObj.toString();
        int count = 0;
        for (Object idObj : idsRaw) {
            Long houseId = Long.valueOf(idObj.toString());
            House house = houseMapper.selectById(houseId);
            if (house == null || !Objects.equals(house.getLandlordId(), userId)) {
                continue;
            }
            switch (action) {
                case "offline" -> house.setStatus(3);
                case "online" -> {
                    if (house.getStatus() == 4) {
                        continue;
                    }
                    house.setStatus(1);
                }
                case "delete" -> {
                    if (house.getStatus() == 2) {
                        continue;
                    }
                    houseMapper.deleteById(houseId);
                    count++;
                    continue;
                }
                default -> {
                    return Result.failure("未知操作");
                }
            }
            house.setUpdateTime(LocalDateTime.now());
            houseMapper.updateById(house);
            count++;
        }
        return Result.success("已处理 " + count + " 个房源");
    }

    private void copyDto(Object dto, House house) {
        if (dto == null) {
            return;
        }
        setIfPresent(dto, "getTitle", v -> house.setTitle((String) v));
        setIfPresent(dto, "getDescription", v -> house.setDescription((String) v));
        setIfPresent(dto, "getAddress", v -> house.setAddress((String) v));
        setIfPresent(dto, "getProvince", v -> house.setProvince((String) v));
        setIfPresent(dto, "getCity", v -> house.setCity((String) v));
        setIfPresent(dto, "getDistrict", v -> house.setDistrict((String) v));
        setIfPresent(dto, "getLongitude", v -> house.setLongitude(toBigDecimal(v)));
        setIfPresent(dto, "getLatitude", v -> house.setLatitude(toBigDecimal(v)));
        setIfPresent(dto, "getRentPrice", v -> house.setRentPrice(toBigDecimal(v)));
        setIfPresent(dto, "getDepositType", v -> house.setDepositType((String) v));
        setIfPresent(dto, "getArea", v -> house.setArea(toBigDecimal(v)));
        setIfPresent(dto, "getHouseType", v -> house.setHouseType((String) v));
        setIfPresent(dto, "getFloor", v -> house.setFloor((String) v));
        setIfPresent(dto, "getOrientation", v -> house.setOrientation((String) v));
        setIfPresent(dto, "getFacilities", v -> house.setFacilities((String) v));
        setIfPresent(dto, "getImages", v -> house.setImages((String) v));
        setIfPresent(dto, "getRentOption", v -> house.setRentOption(toInteger(v)));
        setIfPresent(dto, "getPropertyLicenseFront", v -> house.setPropertyLicenseFront((String) v));
        setIfPresent(dto, "getPropertyLicenseBack", v -> house.setPropertyLicenseBack((String) v));
        setIfPresent(dto, "getPropertyLicenseOther", v -> house.setPropertyLicenseOther((String) v));
    }

    private void setIfPresent(Object dto, String getter, java.util.function.Consumer<Object> consumer) {
        try {
            Method method = dto.getClass().getMethod(getter);
            Object value = method.invoke(dto);
            if (value != null) {
                consumer.accept(value);
            }
        } catch (Exception ignored) {
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer i) {
            return i;
        }
        return Integer.valueOf(value.toString());
    }

    private Map<String, Object> buildHouseInfo(House house) {
        Map<String, Object> map = new HashMap<>();
        map.put("houseId", house.getHouseId());
        map.put("landlordId", house.getLandlordId());
        map.put("title", house.getTitle());
        map.put("description", house.getDescription());
        map.put("address", house.getAddress());
        map.put("province", house.getProvince());
        map.put("city", house.getCity());
        map.put("district", house.getDistrict());
        map.put("longitude", house.getLongitude());
        map.put("latitude", house.getLatitude());
        map.put("rentPrice", house.getRentPrice());
        map.put("depositType", house.getDepositType());
        map.put("area", house.getArea());
        map.put("houseType", house.getHouseType());
        map.put("floor", house.getFloor());
        map.put("orientation", house.getOrientation());
        map.put("facilities", house.getFacilities());
        map.put("images", house.getImages());
        map.put("rentOption", house.getRentOption());
        map.put("status", house.getStatus());
        map.put("auditReason", house.getAuditReason());
        map.put("viewCount", house.getViewCount());
        map.put("collectCount", house.getCollectCount());
        map.put("propertyLicenseFront", house.getPropertyLicenseFront());
        map.put("propertyLicenseBack", house.getPropertyLicenseBack());
        map.put("propertyLicenseOther", house.getPropertyLicenseOther());
        map.put("createTime", house.getCreateTime());
        map.put("updateTime", house.getUpdateTime());
        return map;
    }
}
