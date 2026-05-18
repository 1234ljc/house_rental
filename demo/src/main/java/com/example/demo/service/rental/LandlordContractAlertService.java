package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;

public interface LandlordContractAlertService{
    Result getStats(HttpServletRequest request);
    Result getList(HttpServletRequest request, Integer alertType, String keyword, Integer page, Integer size);
    Result getCalendar(HttpServletRequest request, Integer year, Integer month);
    Result getTrend(HttpServletRequest request);
    Result sendNotify(HttpServletRequest request, Long contractId);
}
