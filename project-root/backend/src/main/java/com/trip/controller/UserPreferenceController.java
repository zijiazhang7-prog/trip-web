package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.dto.request.UserPreferenceRequest;
import com.trip.service.UserPreferenceService;
import com.trip.vo.response.UserPreferenceVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户偏好接口。
 */
@RestController
@RequestMapping("/api/v1/user-preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping("/me")
    public ApiResponse<UserPreferenceVO> getCurrentPreference() {
        return ApiResponse.success(userPreferenceService.getCurrentPreference());
    }

    @PutMapping("/me")
    public ApiResponse<UserPreferenceVO> saveCurrentPreference(
            @Valid @RequestBody UserPreferenceRequest request) {
        return ApiResponse.success(userPreferenceService.saveCurrentPreference(request));
    }
}
