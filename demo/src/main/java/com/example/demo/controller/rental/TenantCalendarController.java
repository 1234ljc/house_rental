package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.TenantCalendarService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/calendar")
@RequiredArgsConstructor
public class TenantCalendarController {

    private final TenantCalendarService tenantCalendarService;

    @GetMapping("/events")
    public Result<?> getCalendarEvents(HttpServletRequest request) {
        return tenantCalendarService.getCalendarEvents(request);
    }

    @GetMapping("/timeline")
    public Result<?> getContractTimeline(HttpServletRequest request) {
        return tenantCalendarService.getContractTimeline(request);
    }

    @GetMapping("/reminders")
    public Result<?> getReminders(HttpServletRequest request) {
        return tenantCalendarService.getReminders(request);
    }

    @GetMapping("/stats")
    public Result<?> getCalendarStats(HttpServletRequest request) {
        return tenantCalendarService.getCalendarStats(request);
    }
}
