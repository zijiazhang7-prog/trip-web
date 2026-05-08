package com.trip.service;

import com.trip.dto.request.DestinationPlacesQuery;
import com.trip.dto.request.DestinationRecommendQuery;
import com.trip.dto.request.DestinationSearchQuery;
import com.trip.vo.response.DestinationVO;
import com.trip.vo.response.PageResultVO;
import com.trip.vo.response.PlaceVO;
import java.util.List;

/**
 * 推荐模块服务，负责目的地推荐、搜索和详情入口数据组织。
 */
public interface RecommendService {

    PageResultVO<DestinationVO> recommendDestinations(DestinationRecommendQuery query);

    PageResultVO<DestinationVO> searchDestinations(DestinationSearchQuery query);

    DestinationVO getDestinationDetail(Long id);

    List<PlaceVO> listDestinationPlaces(Long destinationId, DestinationPlacesQuery query);
}
