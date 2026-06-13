package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.dto.request.IndoorRoutePlanRequest;
import com.trip.service.IndoorRouteService;
import com.trip.vo.response.IndoorBuildingVO;
import com.trip.vo.response.IndoorMapVO;
import com.trip.vo.response.IndoorRoutePlanVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 室内建筑、楼层图和路径规划接口。
 */
@RestController
@RequestMapping("/api/v1/indoor")
@Validated
public class IndoorRouteController {

    private final IndoorRouteService indoorRouteService;

    public IndoorRouteController(IndoorRouteService indoorRouteService) {
        this.indoorRouteService = indoorRouteService;
    }

    @GetMapping("/buildings")
    public ApiResponse<List<IndoorBuildingVO>> buildings(
            @RequestParam @Min(1) Long destinationId) {
        return ApiResponse.success(indoorRouteService.listBuildings(destinationId));
    }

    @GetMapping("/buildings/{buildingId}/map")
    public ApiResponse<IndoorMapVO> buildingMap(
            @PathVariable @Min(1) Long buildingId) {
        return ApiResponse.success(indoorRouteService.getBuildingMap(buildingId));
    }

    @PostMapping("/routes/plan")
    public ApiResponse<IndoorRoutePlanVO> plan(
            @Valid @RequestBody IndoorRoutePlanRequest request) {
        return ApiResponse.success(indoorRouteService.planRoute(request));
    }
}
