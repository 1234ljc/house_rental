package com.example.demo.service.statistics.impl;

import com.example.demo.entity.Result;
import com.example.demo.repository.house.HouseMapper;
import com.example.demo.service.statistics.TenantDashboardService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TenantDashboardServiceImpl implements TenantDashboardService {

    private final HouseMapper houseMapper;

    public TenantDashboardServiceImpl(HouseMapper houseMapper) {
        this.houseMapper = houseMapper;
    }

    @Override
    public Result getHotHouses(String city) {
        List<Map<String, Object>> houses = houseMapper.selectHotHouses(city, 8);
        return Result.success(houses);
    }

    @Override
    public Result getCheapHouses(String city) {
        List<Map<String, Object>> houses = houseMapper.selectCheapHouses(city, 8);
        return Result.success(houses);
    }

    @Override
    public Result getRecommendHouses(String city) {
        List<Map<String, Object>> houses = houseMapper.selectRecommendHouses(city, 6);
        return Result.success(houses);
    }

    @Override
    public Result searchHouses(String keyword, String city, Integer minPrice, Integer maxPrice) {
        List<Map<String, Object>> houses = houseMapper.searchHouses(keyword, city, minPrice, maxPrice);
        return Result.success(houses);
    }
}
