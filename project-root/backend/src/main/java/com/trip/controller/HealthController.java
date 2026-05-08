package com.trip.controller;

import com.trip.common.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工程健康检查接口。
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    /**
     * 返回后端基础启动状态。
     *
     * @return 统一返回结构
     */
    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "ok"));
    }
}
