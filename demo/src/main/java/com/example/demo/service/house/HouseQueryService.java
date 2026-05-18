package com.example.demo.service.house;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface HouseQueryService{
    Result searchHouses(String keyword,
                        String province,
                        String city,
                        String district,
                        Integer minPrice,
                        Integer maxPrice,
                        String houseType,
                        Integer minArea,
                        Integer maxArea,
                        String orientation,
                        String sort,
                        Integer page,
                        Integer size);

    Result getHouseDetail(Long houseId, HttpServletRequest request);

    Result getHotCities();

    Result getRecommendHouses(Integer limit, String city);

    Result getHotHouses(Integer limit, String city);

    Result addFavorite(Long houseId, HttpServletRequest request);

    Result removeFavorite(Long houseId, HttpServletRequest request);

    Result checkFavorite(Long houseId, HttpServletRequest request);

    Result getFavoriteList(Integer page, Integer size, HttpServletRequest request);
}
