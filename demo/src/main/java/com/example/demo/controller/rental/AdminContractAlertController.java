package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.AdminContractAlertService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/contract-alert")
public class AdminContractAlertController {

    private final AdminContractAlertService adminContractAlertService;

    public AdminContractAlertController(AdminContractAlertService adminContractAlertService) {
        this.adminContractAlertService = adminContractAlertService;
    }

    @GetMapping("/stats")
    public Result getStats() {
        return adminContractAlertService.getStats();
    }

    @GetMapping("/list")
    public Result getList(@RequestParam(required = false) Integer alertType,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size) {
        return adminContractAlertService.getList(alertType, keyword, page, size);
    }

    @GetMapping("/calendar")
    public Result getCalendar(@RequestParam(required = false) Integer year,
                              @RequestParam(required = false) Integer month) {
        return adminContractAlertService.getCalendar(year, month);
    }

    @GetMapping("/trend")
    public Result getTrend() {
        return adminContractAlertService.getTrend();
    }

    @PostMapping("/notify/{contractId}")
    public Result sendNotify(@PathVariable Long contractId) {
        return adminContractAlertService.sendNotify(contractId);
    }

    @PostMapping("/batch-notify")
    public Result batchNotify(@RequestBody Map<String, Object> params) {
        return adminContractAlertService.batchNotify(params);
    }

    @PostMapping("/mark-expired/{contractId}")
    public Result markExpired(@PathVariable Long contractId) {
        return adminContractAlertService.markExpired(contractId);
    }

    @PostMapping("/batch-mark-expired")
    public Result batchMarkExpired() {
        return adminContractAlertService.batchMarkExpired();
    }
}
