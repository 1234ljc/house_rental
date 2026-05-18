package com.example.demo.controller.house;

import com.example.demo.entity.Result;
import com.example.demo.service.house.HouseQueryService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/house")
public class TenantHouseController {

    private final HouseQueryService houseQueryService;

    public TenantHouseController(HouseQueryService houseQueryService) {
        this.houseQueryService = houseQueryService;
    }

    @GetMapping("/search")
    public Result searchHouses(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String houseType,
            @RequestParam(required = false) Integer minArea,
            @RequestParam(required = false) Integer maxArea,
            @RequestParam(required = false) String orientation,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size) {
        return houseQueryService.searchHouses(keyword, province, city, district, minPrice, maxPrice, houseType, minArea, maxArea, orientation, sort, page, size);
    }

    @GetMapping("/{houseId}")
    public Result getHouseDetail(@PathVariable Long houseId, HttpServletRequest request) {
        return houseQueryService.getHouseDetail(houseId, request);
    }

    @GetMapping("/hot-cities")
    public Result getHotCities() {
        return houseQueryService.getHotCities();
    }

    @GetMapping("/recommend")
    public Result getRecommendHouses(@RequestParam(defaultValue = "8") Integer limit,
                                     @RequestParam(required = false) String city) {
        return houseQueryService.getRecommendHouses(limit, city);
    }

    @GetMapping("/hot")
    public Result getHotHouses(@RequestParam(defaultValue = "8") Integer limit,
                               @RequestParam(required = false) String city) {
        return houseQueryService.getHotHouses(limit, city);
    }

    @PostMapping("/favorite/{houseId}")
    public Result addFavorite(@PathVariable Long houseId, HttpServletRequest request) {
        return houseQueryService.addFavorite(houseId, request);
    }

    @DeleteMapping("/favorite/{houseId}")
    public Result removeFavorite(@PathVariable Long houseId, HttpServletRequest request) {
        return houseQueryService.removeFavorite(houseId, request);
    }

    @GetMapping("/favorite/check/{houseId}")
    public Result checkFavorite(@PathVariable Long houseId, HttpServletRequest request) {
        return houseQueryService.checkFavorite(houseId, request);
    }

    @GetMapping("/favorite/list")
    public Result getFavoriteList(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "12") Integer size,
                                  HttpServletRequest request) {
        return houseQueryService.getFavoriteList(page, size, request);
    }
}
