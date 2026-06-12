package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.service.AnimationService;
import com.trip.vo.response.DiaryAnimationVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diaries/{diaryId}/animation")
public class DiaryAnimationController {

    private final AnimationService animationService;

    public DiaryAnimationController(AnimationService animationService) {
        this.animationService = animationService;
    }

    @PostMapping
    public ApiResponse<DiaryAnimationVO> generate(@PathVariable Long diaryId) {
        return ApiResponse.success("generated", animationService.generate(diaryId));
    }

    @GetMapping
    public ApiResponse<DiaryAnimationVO> detail(@PathVariable Long diaryId) {
        return ApiResponse.success(animationService.getByDiaryId(diaryId));
    }
}
