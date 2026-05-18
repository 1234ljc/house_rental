package com.example.demo.service.house;

import com.example.demo.entity.Result;

import java.util.List;
import java.util.Map;

public interface AdminHouseService{
    Result getAuditList(Integer status, String keyword, Integer page, Integer size);
    Result getHouseDetail(Long houseId);
    Result approveHouse(Long houseId);
    Result rejectHouse(Long houseId, Map<String, String> body);
    Result batchApprove(List<Long> ids);
    Result batchReject(List<Long> ids, String reason);
    Result getAuditStats();
    Result getMonitorList(Integer status, String city, String district, Integer minPrice, Integer maxPrice, String keyword, Integer page, Integer size);
    Result getMonitorStats();
    Result offlineHouse(Long houseId, String reason);
    Result onlineHouse(Long houseId);
    Result editHouse(Long houseId, Map<String, Object> body);
    Result batchOffline(List<Integer> ids, String reason);
    Result getAreaDistribution();
    Result getPriceDistribution();
    Result getHotRanking(Integer limit);
    Result getPublishTrend(Integer days);
    Result getTypeDistribution();
    Result getAnalysisOverview();
}
