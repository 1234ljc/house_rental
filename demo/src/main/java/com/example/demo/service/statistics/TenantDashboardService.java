package com.example.demo.service.statistics;

import com.example.demo.entity.Result;

public interface TenantDashboardService{
    Result getHotHouses(String city);

    Result getCheapHouses(String city);

    Result getRecommendHouses(String city);

    Result searchHouses(String keyword, String city, Integer minPrice, Integer maxPrice);
}
