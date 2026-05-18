package com.example.demo.service.house;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface LandlordHouseService{
    Result publishHouse(Long userId, Object dto);
    Result getHouseList(Long userId, Integer status, String keyword, Integer page, Integer size);
    Result getHouseDetail(Long userId, Long houseId);
    Result updateHouse(Long userId, Long houseId, Object dto);
    Result offlineHouse(Long userId, Long houseId);
    Result onlineHouse(Long userId, Long houseId);
    Result deleteHouse(Long userId, Long houseId);
    Result getHouseStats(Long userId);
    Result batchOperate(Long userId, Map<String, Object> params);
}
