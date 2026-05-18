package com.example.demo.controller.statistics;

import com.example.demo.entity.Result;
import com.example.demo.service.statistics.TenantDashboardService;

import org.springframework.web.bind.annotation.*;

/**
 * 租客端首页接口
 */
@RestController
@RequestMapping("/api/tenant/dashboard")
public class TenantDashboardController {

    private final TenantDashboardService tenantDashboardService;

    public TenantDashboardController(TenantDashboardService tenantDashboardService) {
        this.tenantDashboardService = tenantDashboardService;
    }

    /**
     * 获取热门房源
     */
    @GetMapping("/hot-houses")
    public Result getHotHouses(@RequestParam(required = false) String city) {
        return tenantDashboardService.getHotHouses(city);
    }

    /**
     * 获取低价房源
     */
    @GetMapping("/cheap-houses")
    public Result getCheapHouses(@RequestParam(required = false) String city) {
        return tenantDashboardService.getCheapHouses(city);
    }

    /**
     * 获取推荐房源
     */
    @GetMapping("/recommend-houses")
    public Result getRecommendHouses(@RequestParam(required = false) String city) {
        return tenantDashboardService.getRecommendHouses(city);
    }

    /**
     * 搜索房源
     */
    @GetMapping("/search")
    public Result searchHouses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice) {
        return tenantDashboardService.searchHouses(keyword, city, minPrice, maxPrice);
    }
}
