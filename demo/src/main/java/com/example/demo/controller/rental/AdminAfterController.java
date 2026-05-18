package com.example.demo.controller.rental;

import com.example.demo.entity.Result;
import com.example.demo.service.rental.AdminAfterService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/after")
public class AdminAfterController {

    private final AdminAfterService adminAfterService;

    public AdminAfterController(AdminAfterService adminAfterService) {
        this.adminAfterService = adminAfterService;
    }

    @GetMapping("/stats")
    public Result getStats() {
        return adminAfterService.getStats();
    }

    @GetMapping("/trend")
    public Result getTrend() {
        return adminAfterService.getTrend();
    }

    @GetMapping("/type-distribution")
    public Result getTypeDistribution() {
        return adminAfterService.getTypeDistribution();
    }

    @GetMapping("/list")
    public Result getList(@RequestParam(required = false) Integer manageType,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) Boolean overtime,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size) {
        return adminAfterService.getList(manageType, status, overtime, keyword, page, size);
    }

    @GetMapping("/{manageId}")
    public Result getDetail(@PathVariable Long manageId) {
        return adminAfterService.getDetail(manageId);
    }

    @PostMapping("/urge/{manageId}")
    public Result urge(@PathVariable Long manageId) {
        return adminAfterService.urge(manageId);
    }

    @PostMapping("/force-complete/{manageId}")
    public Result forceComplete(@PathVariable Long manageId, @RequestBody Map<String, String> params) {
        return adminAfterService.forceComplete(manageId, params);
    }

    @GetMapping("/landlord-ranking")
    public Result getLandlordRanking(@RequestParam(defaultValue = "response") String sortBy) {
        return adminAfterService.getLandlordRanking(sortBy);
    }

    @GetMapping("/overtime-list")
    public Result getOvertimeList() {
        return adminAfterService.getOvertimeList();
    }
}
