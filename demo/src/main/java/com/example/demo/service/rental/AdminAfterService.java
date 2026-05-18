package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface AdminAfterService{
    Result getStats();

    Result getTrend();

    Result getTypeDistribution();

    Result getList(Integer manageType, Integer status, Boolean overtime, String keyword, Integer page, Integer size);

    Result getDetail(Long manageId);

    Result urge(Long manageId);

    Result forceComplete(Long manageId, Map<String, String> params);

    Result getLandlordRanking(String sortBy);

    Result getOvertimeList();
}
