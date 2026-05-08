package com.trip.service;

import com.trip.dto.request.NearbyFacilityQuery;
import com.trip.vo.response.NearbyFacilityVO;
import com.trip.vo.response.PageResultVO;

/**
 * 周边设施查询业务服务。
 */
public interface FacilityService {

    PageResultVO<NearbyFacilityVO> findNearbyFacilities(NearbyFacilityQuery query);
}
