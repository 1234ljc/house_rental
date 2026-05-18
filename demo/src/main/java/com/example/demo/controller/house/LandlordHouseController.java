package com.example.demo.controller.house;

import com.example.demo.entity.Result;
import com.example.demo.service.house.LandlordHouseService;

import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 房东端-房源管理接口
 */
@RestController
@RequestMapping("/api/landlord/house")
public class LandlordHouseController {

    private final LandlordHouseService landlordHouseService;

    public LandlordHouseController(LandlordHouseService landlordHouseService) {
        this.landlordHouseService = landlordHouseService;
    }

    @PostMapping("/publish")
    public Result<String> publishHouse(@RequestAttribute("userId") Long userId,
                                        @RequestBody HousePublishDTO dto) {
        return landlordHouseService.publishHouse(userId, dto);
    }

    @GetMapping("/list")
    public Result<?> getHouseList(@RequestAttribute("userId") Long userId,
                                  @RequestParam(required = false) Integer status,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size) {
        return landlordHouseService.getHouseList(userId, status, keyword, page, size);
    }

    @GetMapping("/{houseId}")
    public Result<?> getHouseDetail(@RequestAttribute("userId") Long userId,
                                    @PathVariable Long houseId) {
        return landlordHouseService.getHouseDetail(userId, houseId);
    }

    @PutMapping("/{houseId}")
    public Result<String> updateHouse(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long houseId,
                                      @RequestBody HousePublishDTO dto) {
        return landlordHouseService.updateHouse(userId, houseId, dto);
    }

    @PutMapping("/{houseId}/offline")
    public Result<String> offlineHouse(@RequestAttribute("userId") Long userId,
                                       @PathVariable Long houseId) {
        return landlordHouseService.offlineHouse(userId, houseId);
    }

    @PutMapping("/{houseId}/online")
    public Result<String> onlineHouse(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long houseId) {
        return landlordHouseService.onlineHouse(userId, houseId);
    }

    @DeleteMapping("/{houseId}")
    public Result<String> deleteHouse(@RequestAttribute("userId") Long userId,
                                      @PathVariable Long houseId) {
        return landlordHouseService.deleteHouse(userId, houseId);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getHouseStats(@RequestAttribute("userId") Long userId) {
        return landlordHouseService.getHouseStats(userId);
    }

    @PostMapping("/batch")
    public Result<String> batchOperate(@RequestAttribute("userId") Long userId,
                                       @RequestBody Map<String, Object> params) {
        return landlordHouseService.batchOperate(userId, params);
    }

    @Data
    public static class HousePublishDTO {
        private String title;
        private String description;
        private String address;
        private String province;
        private String city;
        private String district;
        private BigDecimal longitude;
        private BigDecimal latitude;
        private BigDecimal rentPrice;
        private String depositType;
        private BigDecimal area;
        private String houseType;
        private String floor;
        private String orientation;
        private String facilities;
        private String images;
        private Integer rentOption;
        private String propertyLicenseFront;
        private String propertyLicenseBack;
        private String propertyLicenseOther;
    }
}
