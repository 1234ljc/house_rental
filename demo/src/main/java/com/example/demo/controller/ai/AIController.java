package com.example.demo.controller.ai;



import com.example.demo.entity.Result;

import com.example.demo.service.ai.AIService;

import lombok.Data;

import org.springframework.web.bind.annotation.*;



/**

 * AI功能控制器（仅保留：智能解析房源描述、房源对比分析）

 */

@RestController

@RequestMapping("/api/ai")

public class AIController {



    private final AIService aiService;



    public AIController(AIService aiService) {

        this.aiService = aiService;

    }



    /**

     * 智能解析房源描述（AI智能发布使用）

     */

    @PostMapping("/parse-house-description")

    public Result<String> parseHouseDescription(@RequestBody HouseDescriptionParseRequest request) {

        try {

            String result = aiService.parseHouseDescription(request.getDescription());

            if (result == null || result.isEmpty()) {

                return Result.failure("AI解析失败，请稍后重试");

            }

            return Result.success(result);

        } catch (Exception e) {

            return Result.failure("解析失败：" + e.getMessage());

        }

    }



    /**

     * AI房源对比分析（房源对比弹窗使用）

     */

    @PostMapping("/compare-houses")

    public Result<String> compareHouses(@RequestBody CompareHousesRequest request) {

        try {

            if (request.getHouses() == null || request.getHouses().isEmpty()) {

                return Result.failure("请提供需要对比的房源");

            }

            if (request.getHouses().size() < 2) {

                return Result.failure("至少需要2个房源才能进行对比");

            }

            if (request.getHouses().size() > 3) {

                return Result.failure("最多支持对比3个房源");

            }

            String analysis = aiService.compareHouses(request.getHouses());

            if (analysis == null || analysis.isEmpty()) {

                return Result.failure("AI分析失败，请稍后重试");

            }

            return Result.success(analysis);

        } catch (Exception e) {

            return Result.failure("对比分析失败：" + e.getMessage());

        }

    }



    @Data

    public static class HouseDescriptionParseRequest {

        private String description;

    }



    @Data

    public static class CompareHousesRequest {

        private java.util.List<java.util.Map<String, Object>> houses;

    }

}

