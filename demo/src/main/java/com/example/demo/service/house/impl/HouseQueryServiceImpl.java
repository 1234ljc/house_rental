package com.example.demo.service.house.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Result;
import com.example.demo.repository.house.entity.Favorite;
import com.example.demo.repository.house.entity.House;
import com.example.demo.repository.house.FavoriteMapper;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.house.HouseQueryService;
import com.example.demo.repository.user.entity.User;
import com.example.demo.repository.user.UserMapper;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
// 负责租客侧房源搜索、详情查看和收藏管理功能实现。
public class HouseQueryServiceImpl implements HouseQueryService {

    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final FavoriteMapper favoriteMapper;

    public HouseQueryServiceImpl(HouseMapper houseMapper,
                                 UserMapper userMapper,
                                 FavoriteMapper favoriteMapper) {
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.favoriteMapper = favoriteMapper;
    }

    // 租客看房主流程：按关键词、区域、价格、面积和朝向组合筛选房源，并支持排序和分页。
    @Override
    public Result searchHouses(String keyword, String province, String city, String district, Integer minPrice, Integer maxPrice, String houseType, Integer minArea, Integer maxArea, String orientation, String sort, Integer page, Integer size) {
        Page<House> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<House> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(House::getStatus, 1);

        if (StrUtil.isNotEmpty(keyword)) {
            wrapper.and(w -> w.like(House::getTitle, keyword)
                    .or().like(House::getDescription, keyword)
                    .or().like(House::getAddress, keyword)
                    .or().like(House::getFacilities, keyword));
        }
        if (StrUtil.isNotEmpty(province)) {
            wrapper.like(House::getProvince, province);
        }
        if (StrUtil.isNotEmpty(city)) {
            wrapper.like(House::getCity, city);
        }
        if (StrUtil.isNotEmpty(district)) {
            wrapper.like(House::getDistrict, district);
        }
        if (minPrice != null) {
            wrapper.ge(House::getRentPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(House::getRentPrice, maxPrice);
        }
        if (StrUtil.isNotEmpty(houseType)) {
            wrapper.eq(House::getHouseType, houseType);
        }
        if (minArea != null) {
            wrapper.ge(House::getArea, minArea);
        }
        if (maxArea != null) {
            wrapper.le(House::getArea, maxArea);
        }
        if (StrUtil.isNotEmpty(orientation)) {
            wrapper.eq(House::getOrientation, orientation);
        }

        switch (sort != null ? sort : "latest") {
            case "price_asc" -> wrapper.orderByAsc(House::getRentPrice);
            case "price_desc" -> wrapper.orderByDesc(House::getRentPrice);
            case "area_asc" -> wrapper.orderByAsc(House::getArea);
            case "area_desc" -> wrapper.orderByDesc(House::getArea);
            case "hot" -> wrapper.orderByDesc(House::getViewCount);
            case "newest" -> wrapper.orderByDesc(House::getCreateTime);
            default -> wrapper.orderByDesc(House::getViewCount).orderByDesc(House::getCreateTime);
        }

        Page<House> result = houseMapper.selectPage(pageParam, wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (House house : result.getRecords()) {
            records.add(buildHouseCard(house));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        return Result.success(data);
    }

    // 房源详情流程：查询房源并累加浏览量，同时返回房东信息和展示详情。
    @Override
    public Result getHouseDetail(Long houseId, HttpServletRequest request) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }

        house.setViewCount((house.getViewCount() == null ? 0 : house.getViewCount()) + 1);
        houseMapper.updateById(house);

        Map<String, Object> data = buildHouseDetail(house);
        return Result.success(data);
    }

    @Override
    public Result getHotCities() {
        return Result.success(houseMapper.getHotCities());
    }

    @Override
    public Result getRecommendHouses(Integer limit, String city) {
        return Result.success(houseMapper.selectRecommendHouses(city, limit));
    }

    @Override
    public Result getHotHouses(Integer limit, String city) {
        return Result.success(houseMapper.selectHotHouses(city, limit));
    }

    // 收藏流程：校验登录和重复收藏后写入收藏记录，并同步更新收藏数。
    @Override
    public Result addFavorite(Long houseId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.failure("请先登录");
        }

        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return Result.failure("房源不存在");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getHouseId, houseId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            return Result.failure("已收藏该房源");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setHouseId(houseId);
        favoriteMapper.insert(favorite);

        house.setCollectCount((house.getCollectCount() == null ? 0 : house.getCollectCount()) + 1);
        houseMapper.updateById(house);

        return Result.success("收藏成功");
    }

    // 取消收藏流程：删除收藏记录，并在成功后回减房源收藏数。
    @Override
    public Result removeFavorite(Long houseId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.failure("请先登录");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getHouseId, houseId);
        int deleted = favoriteMapper.delete(wrapper);
        if (deleted > 0) {
            House house = houseMapper.selectById(houseId);
            if (house != null && house.getCollectCount() != null && house.getCollectCount() > 0) {
                house.setCollectCount(house.getCollectCount() - 1);
                houseMapper.updateById(house);
            }
            return Result.success("取消收藏成功");
        }
        return Result.failure("未收藏该房源");
    }

    // 收藏状态查询流程：用于前端判断当前房源是否已收藏。
    @Override
    public Result checkFavorite(Long houseId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.success(false);
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getHouseId, houseId);
        return Result.success(favoriteMapper.selectCount(wrapper) > 0);
    }

    // 收藏列表流程：按用户维度读取收藏记录，并拼装房源展示卡片。
    @Override
    public Result getFavoriteList(Integer page, Integer size, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.failure("请先登录");
        }

        LambdaQueryWrapper<Favorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreateTime);
        List<Favorite> favorites = favoriteMapper.selectList(favWrapper);
        if (CollUtil.isEmpty(favorites)) {
            Map<String, Object> data = new HashMap<>();
            data.put("records", new ArrayList<>());
            data.put("total", 0);
            return Result.success(data);
        }

        List<Long> houseIds = favorites.stream().map(Favorite::getHouseId).toList();
        Page<House> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<House> houseWrapper = new LambdaQueryWrapper<>();
        houseWrapper.in(House::getHouseId, houseIds);
        Page<House> result = houseMapper.selectPage(pageParam, houseWrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (House house : result.getRecords()) {
            records.add(buildHouseCard(house));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", favorites.size());
        return Result.success(data);
    }

    private Map<String, Object> buildHouseCard(House house) {
        Map<String, Object> map = new HashMap<>();
        map.put("houseId", house.getHouseId());
        map.put("title", house.getTitle());
        map.put("address", house.getAddress());
        map.put("province", house.getProvince());
        map.put("city", house.getCity());
        map.put("district", house.getDistrict());
        map.put("rentPrice", house.getRentPrice());
        map.put("depositType", house.getDepositType());
        map.put("area", house.getArea());
        map.put("houseType", house.getHouseType());
        map.put("floor", house.getFloor());
        map.put("orientation", house.getOrientation());
        map.put("facilities", house.getFacilities());
        map.put("images", house.getImages());
        map.put("rentOption", house.getRentOption());
        map.put("viewCount", house.getViewCount());
        map.put("collectCount", house.getCollectCount());
        map.put("createTime", house.getCreateTime());
        return map;
    }

    private Map<String, Object> buildHouseDetail(House house) {
        Map<String, Object> data = buildHouseCard(house);
        data.put("description", house.getDescription());
        data.put("status", house.getStatus());
        data.put("landlordId", house.getLandlordId());

        if (house.getLandlordId() != null) {
            User landlord = userMapper.selectById(house.getLandlordId());
            if (landlord != null) {
                Map<String, Object> landlordInfo = new HashMap<>();
                landlordInfo.put("userId", landlord.getUserId());
                landlordInfo.put("username", landlord.getUsername());
                landlordInfo.put("realName", landlord.getRealName());
                landlordInfo.put("phone", landlord.getPhone());
                landlordInfo.put("avatar", landlord.getAvatar());
                landlordInfo.put("realnameStatus", landlord.getRealnameStatus());
                data.put("landlord", landlordInfo);
            }
        }
        return data;
    }
}
