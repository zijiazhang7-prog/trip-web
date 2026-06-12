package com.trip.service;

import com.trip.dto.request.MultiRoutePlanRequest;
import com.trip.dto.request.RouteHistoryPageQuery;
import com.trip.dto.request.SingleRoutePlanRequest;
import com.trip.vo.response.PageResultVO;
import com.trip.vo.response.RouteHistoryVO;
import com.trip.vo.response.RoutePlanVO;

/**
 * 路线规划业务服务。
 */
public interface RouteService {

    RoutePlanVO planSingleRoute(SingleRoutePlanRequest request);

    RoutePlanVO planMultiRoute(MultiRoutePlanRequest request);

    PageResultVO<RouteHistoryVO> listMyRouteHistories(RouteHistoryPageQuery query);

    RouteHistoryVO getMyRouteHistory(Long historyId);
}
