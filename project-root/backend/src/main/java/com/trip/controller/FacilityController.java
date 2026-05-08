package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.dto.request.NearbyFacilityQuery;
import com.trip.service.FacilityService;
import com.trip.vo.response.NearbyFacilityVO;
import com.trip.vo.response.PageResultVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 周边设施查询接口。
 */
@Validated
@RestController
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping("/nearby")
    public ApiResponse<PageResultVO<NearbyFacilityVO>> nearby(@Valid @ModelAttribute NearbyFacilityQuery query) {
        return ApiResponse.success(facilityService.findNearbyFacilities(query));
    }
}
