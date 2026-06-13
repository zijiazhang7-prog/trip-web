package com.trip.service;

import com.trip.dto.request.IndoorRoutePlanRequest;
import com.trip.vo.response.IndoorBuildingVO;
import com.trip.vo.response.IndoorMapVO;
import com.trip.vo.response.IndoorRoutePlanVO;
import java.util.List;

/**
 * 室内导航业务编排服务。
 */
public interface IndoorRouteService {

    List<IndoorBuildingVO> listBuildings(Long destinationId);

    IndoorMapVO getBuildingMap(Long buildingId);

    IndoorRoutePlanVO planRoute(IndoorRoutePlanRequest request);
}
