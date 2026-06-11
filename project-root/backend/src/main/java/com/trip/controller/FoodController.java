package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.common.CommentTargetType;
import com.trip.dto.request.CommentCreateRequest;
import com.trip.dto.request.CommentPageQuery;
import com.trip.dto.request.FoodRecommendQuery;
import com.trip.dto.request.FoodSearchQuery;
import com.trip.service.CommentService;
import com.trip.service.FoodService;
import com.trip.vo.response.CommentVO;
import com.trip.vo.response.FoodVO;
import com.trip.vo.response.PageResultVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final CommentService commentService;

    public FoodController(FoodService foodService, CommentService commentService) {
        this.foodService = foodService;
        this.commentService = commentService;
    }

    @GetMapping("/recommend")
    public ApiResponse<PageResultVO<FoodVO>> recommend(@Valid @ModelAttribute FoodRecommendQuery query) {
        return ApiResponse.success(foodService.recommendFoods(query));
    }

    @GetMapping("/search")
    public ApiResponse<PageResultVO<FoodVO>> search(@Valid @ModelAttribute FoodSearchQuery query) {
        return ApiResponse.success(foodService.searchFoods(query));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<PageResultVO<CommentVO>> comments(
            @PathVariable Long id,
            @Valid @ModelAttribute CommentPageQuery query) {
        return ApiResponse.success(commentService.listComments(CommentTargetType.FOOD, id, query));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<CommentVO> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(
                "created",
                commentService.createComment(CommentTargetType.FOOD, id, request));
    }
}
