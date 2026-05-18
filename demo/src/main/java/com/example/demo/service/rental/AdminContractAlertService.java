package com.example.demo.service.rental;

import com.example.demo.entity.Result;

import java.util.Map;

public interface AdminContractAlertService{
    Result getStats();

    Result getList(Integer alertType, String keyword, Integer page, Integer size);

    Result getCalendar(Integer year, Integer month);

    Result getTrend();

    Result sendNotify(Long contractId);

    Result batchNotify(Map<String, Object> params);

    Result markExpired(Long contractId);

    Result batchMarkExpired();
}
