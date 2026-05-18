package com.example.demo.controller.house;

import com.example.demo.entity.Result;
import com.example.demo.service.house.AdminHouseService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/house")
public class AdminHouseController {

    private final AdminHouseService adminHouseService;

    public AdminHouseController(AdminHouseService adminHouseService) {
        this.adminHouseService = adminHouseService;
    }

    @GetMapping("/audit/list")
    public Result getAuditList(@RequestParam(defaultValue = "0") Integer status,
                               @RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size) {
        return adminHouseService.getAuditList(status, keyword, page, size);
    }

    @GetMapping("/audit/{houseId}")
    public Result getHouseDetail(@PathVariable Long houseId) {
        return adminHouseService.getHouseDetail(houseId);
    }

    @PutMapping("/audit/{houseId}/approve")
    public Result approveHouse(@PathVariable Long houseId) {
        return adminHouseService.approveHouse(houseId);
    }

    @PutMapping("/audit/{houseId}/reject")
    public Result rejectHouse(@PathVariable Long houseId, @RequestBody Map<String, String> body) {
        return adminHouseService.rejectHouse(houseId, body);
    }

    @PutMapping("/audit/batch/approve")
    public Result batchApprove(@RequestBody Map<String, List<Long>> body) {
        return adminHouseService.batchApprove(body.get("ids"));
    }

    @PutMapping("/audit/batch/reject")
    public Result batchReject(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<?>) body.get("ids")).stream().map(v -> Long.valueOf(v.toString())).toList();
        String reason = body.get("reason") == null ? null : body.get("reason").toString();
        return adminHouseService.batchReject(ids, reason);
    }

    @GetMapping("/audit/stats")
    public Result getAuditStats() {
        return adminHouseService.getAuditStats();
    }

    @GetMapping("/monitor/list")
    public Result getMonitorList(@RequestParam(defaultValue = "-1") Integer status,
                                 @RequestParam(required = false) String city,
                                 @RequestParam(required = false) String district,
                                 @RequestParam(required = false) Integer minPrice,
                                 @RequestParam(required = false) Integer maxPrice,
                                 @RequestParam(defaultValue = "") String keyword,
                                 @RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size) {
        return adminHouseService.getMonitorList(status, city, district, minPrice, maxPrice, keyword, page, size);
    }

    @GetMapping("/monitor/stats")
    public Result getMonitorStats() {
        return adminHouseService.getMonitorStats();
    }

    @PutMapping("/monitor/{houseId}/offline")
    public Result offlineHouse(@PathVariable Long houseId, @RequestBody Map<String, String> body) {
        return adminHouseService.offlineHouse(houseId, body.get("reason"));
    }

    @PutMapping("/monitor/{houseId}/online")
    public Result onlineHouse(@PathVariable Long houseId) {
        return adminHouseService.onlineHouse(houseId);
    }

    @PutMapping("/monitor/{houseId}/edit")
    public Result editHouse(@PathVariable Long houseId, @RequestBody Map<String, Object> body) {
        return adminHouseService.editHouse(houseId, body);
    }

    @PutMapping("/monitor/batch/offline")
    public Result batchOffline(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        String reason = body.get("reason") == null ? null : body.get("reason").toString();
        return adminHouseService.batchOffline(ids, reason);
    }

    @GetMapping("/analysis/area-distribution")
    public Result getAreaDistribution() { return adminHouseService.getAreaDistribution(); }
    @GetMapping("/analysis/price-distribution")
    public Result getPriceDistribution() { return adminHouseService.getPriceDistribution(); }
    @GetMapping("/analysis/hot-ranking")
    public Result getHotRanking(@RequestParam(defaultValue = "10") Integer limit) { return adminHouseService.getHotRanking(limit); }
    @GetMapping("/analysis/publish-trend")
    public Result getPublishTrend(@RequestParam(defaultValue = "30") Integer days) { return adminHouseService.getPublishTrend(days); }
    @GetMapping("/analysis/type-distribution")
    public Result getTypeDistribution() { return adminHouseService.getTypeDistribution(); }
    @GetMapping("/analysis/overview")
    public Result getAnalysisOverview() { return adminHouseService.getAnalysisOverview(); }
}
