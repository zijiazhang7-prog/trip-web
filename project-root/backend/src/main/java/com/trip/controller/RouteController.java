package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.dto.request.MultiRoutePlanRequest;
import com.trip.dto.request.SingleRoutePlanRequest;
import com.trip.service.RouteService;
import com.trip.vo.response.RoutePlanVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路线规划接口入口。
 */
@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/plan/single")
    public ApiResponse<RoutePlanVO> planSingle(@Valid @RequestBody SingleRoutePlanRequest request) {
        return ApiResponse.success(routeService.planSingleRoute(request));
    }

    @PostMapping("/plan/multi")
    public ApiResponse<RoutePlanVO> planMulti(@Valid @RequestBody MultiRoutePlanRequest request) {
        return ApiResponse.success(routeService.planMultiRoute(request));
    }
}
