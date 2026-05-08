package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.common.ErrorCode;
import com.trip.dto.query.FacilityQuery;
import com.trip.dto.request.NearbyFacilityQuery;
import com.trip.entity.Facility;
import com.trip.entity.MapNode;
import com.trip.exception.BusinessException;
import com.trip.mapper.MapNodeMapper;
import com.trip.service.FacilityService;
import com.trip.service.MapService;
import com.trip.service.QueryService;
import com.trip.service.RankService;
import com.trip.vo.response.NearbyFacilityVO;
import com.trip.vo.response.PageResultVO;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 周边设施基础版：设施召回后按图上可达距离排序。
 */
@Service
public class FacilityServiceImpl implements FacilityService {

    private static final String MAP_NODE_TYPE_FACILITY = "facility";
    private static final String SORT_BY_DISTANCE = "distance";
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final QueryService queryService;
    private final RankService rankService;
    private final MapService mapService;
    private final MapNodeMapper mapNodeMapper;

    public FacilityServiceImpl(
            QueryService queryService,
            RankService rankService,
            MapService mapService,
            MapNodeMapper mapNodeMapper) {
        this.queryService = queryService;
        this.rankService = rankService;
        this.mapService = mapService;
        this.mapNodeMapper = mapNodeMapper;
    }

    @Override
    public PageResultVO<NearbyFacilityVO> findNearbyFacilities(NearbyFacilityQuery query) {
        if (query == null || query.getDestinationId() == null || query.getSourceNodeId() == null) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        validateSortBy(query.getSortBy());

        FacilityQuery facilityQuery = new FacilityQuery();
        facilityQuery.setDestinationId(query.getDestinationId());
        facilityQuery.setFacilityType(normalize(query.getFacilityType()));
        List<Facility> candidates = queryService.queryFacilities(facilityQuery);
        if (candidates.isEmpty()) {
            return emptyPage(query);
        }

        Map<Long, BigDecimal> distances = mapService.shortestDistances(query.getDestinationId(), query.getSourceNodeId());
        Map<Long, Long> facilityNodeMap = facilityNodeMap(query.getDestinationId(), candidates);
        List<NearbyFacilityVO> reachableFacilities = candidates.stream()
                .map(facility -> toNearbyFacility(facility, facilityNodeMap, distances, query))
                .filter(item -> item != null && withinRadius(item, query.getRadius()))
                .toList();

        List<NearbyFacilityVO> sorted = rankService.sortByScore(
                reachableFacilities,
                NearbyFacilityVO::getReachableDistance,
                false);
        return page(sorted, query);
    }

    /**
     * 数据结构：候选设施列表、facilityId 到 nodeId 的 HashMap、nodeId 到距离的 HashMap。
     * 算法：一次单源最短路后按设施节点取距离，避免对每个设施重复跑 Dijkstra。
     * 复杂度：最短路由 MapService 完成 O((V+E)logV)，设施过滤与映射 O(m)，排序 O(m log m)。
     */
    private NearbyFacilityVO toNearbyFacility(
            Facility facility,
            Map<Long, Long> facilityNodeMap,
            Map<Long, BigDecimal> distances,
            NearbyFacilityQuery query) {
        Long targetNodeId = facilityNodeMap.get(facility.getId());
        if (targetNodeId == null) {
            return null;
        }
        BigDecimal reachableDistance = distances.get(targetNodeId);
        if (reachableDistance == null) {
            return null;
        }
        return NearbyFacilityVO.from(facility, reachableDistance, query.getSourceNodeId(), targetNodeId);
    }

    private Map<Long, Long> facilityNodeMap(Long destinationId, List<Facility> facilities) {
        List<Long> facilityIds = facilities.stream()
                .map(Facility::getId)
                .filter(id -> id != null)
                .toList();
        if (facilityIds.isEmpty()) {
            return Map.of();
        }

        List<MapNode> nodes = mapNodeMapper.selectList(new LambdaQueryWrapper<MapNode>()
                .eq(MapNode::getDestinationId, destinationId)
                .eq(MapNode::getNodeType, MAP_NODE_TYPE_FACILITY)
                .in(MapNode::getRefId, facilityIds));
        Map<Long, Long> result = new HashMap<>();
        for (MapNode node : nodes) {
            if (node.getRefId() != null && node.getId() != null && !result.containsKey(node.getRefId())) {
                result.put(node.getRefId(), node.getId());
            }
        }
        return result;
    }

    private boolean withinRadius(NearbyFacilityVO facility, Integer radius) {
        return radius == null || facility.getReachableDistance().compareTo(BigDecimal.valueOf(radius)) <= 0;
    }

    private PageResultVO<NearbyFacilityVO> page(List<NearbyFacilityVO> sorted, NearbyFacilityQuery query) {
        int pageNum = pageNum(query.getPageNum());
        int pageSize = pageSize(query.getPageSize());
        int fromIndex = Math.min((pageNum - 1) * pageSize, sorted.size());
        int toIndex = Math.min(fromIndex + pageSize, sorted.size());
        long pages = sorted.isEmpty() ? 0 : (sorted.size() + pageSize - 1L) / pageSize;
        return PageResultVO.of(sorted.subList(fromIndex, toIndex), pageNum, pageSize, sorted.size(), pages);
    }

    private PageResultVO<NearbyFacilityVO> emptyPage(NearbyFacilityQuery query) {
        int pageNum = pageNum(query.getPageNum());
        int pageSize = pageSize(query.getPageSize());
        return PageResultVO.of(List.of(), pageNum, pageSize, 0, 0);
    }

    private void validateSortBy(String sortBy) {
        if (StringUtils.hasText(sortBy) && !SORT_BY_DISTANCE.equals(sortBy.trim())) {
            throw new BusinessException(ErrorCode.COMMON_008);
        }
    }

    private int pageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
