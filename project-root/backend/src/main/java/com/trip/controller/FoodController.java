package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.dto.request.FoodRecommendQuery;
import com.trip.dto.request.FoodSearchQuery;
import com.trip.service.FoodService;
import com.trip.vo.response.FoodVO;
import com.trip.vo.response.PageResultVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 美食推荐与搜索接口。
 */
@Validated
@RestController
@RequestMapping("/api/v1/foods")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping("/recommend")
    public ApiResponse<PageResultVO<FoodVO>> recommend(@Valid @ModelAttribute FoodRecommendQuery query) {
        return ApiResponse.success(foodService.recommendFoods(query));
    }

    @GetMapping("/search")
    public ApiResponse<PageResultVO<FoodVO>> search(@Valid @ModelAttribute FoodSearchQuery query) {
        return ApiResponse.success(foodService.searchFoods(query));
    }
}
