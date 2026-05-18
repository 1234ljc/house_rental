package com.example.demo.service.rental;

import com.example.demo.entity.Result;
import jakarta.servlet.http.HttpServletRequest;

public interface TenantCalendarService{
    Result getCalendarEvents(HttpServletRequest request);

    Result getContractTimeline(HttpServletRequest request);

    Result getReminders(HttpServletRequest request);

    Result getCalendarStats(HttpServletRequest request);
}
