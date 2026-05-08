package com.trip.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.common.ErrorCode;
import com.trip.dto.map.MultiPathResult;
import com.trip.dto.map.PathNodeResult;
import com.trip.dto.map.ShortestPathResult;
import com.trip.dto.request.MultiRoutePlanRequest;
import com.trip.dto.request.SingleRoutePlanRequest;
import com.trip.entity.RouteHistory;
import com.trip.entity.User;
import com.trip.exception.BusinessException;
import com.trip.mapper.RouteHistoryMapper;
import com.trip.mapper.UserMapper;
import com.trip.security.JwtClaims;
import com.trip.service.MapService;
import com.trip.service.RouteService;
import com.trip.vo.response.RoutePathEdgeVO;
import com.trip.vo.response.RoutePlanVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 单目标路线规划业务编排，路径算法统一复用 MapService。
 */
@Service
public class RouteServiceImpl implements RouteService {

    private static final int ENABLED_STATUS = 1;
    private static final int MAX_MULTI_TARGET_COUNT = 8;
    private static final String STRATEGY_SHORTEST_DISTANCE = "shortest_distance";
    private static final String STRATEGY_SHORTEST_TIME = "shortest_time";
    private static final String DEFAULT_TRANSPORT_TYPE = "walk";
    private static final BigDecimal WALK_SPEED_METER_PER_MINUTE = new BigDecimal("80");

    private final MapService mapService;
    private final RouteHistoryMapper routeHistoryMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public RouteServiceImpl(
            MapService mapService,
            RouteHistoryMapper routeHistoryMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper) {
        this.mapService = mapService;
        this.routeHistoryMapper = routeHistoryMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RoutePlanVO planSingleRoute(SingleRoutePlanRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        Long userId = currentUserId();
        ensureActiveUser(userId);
        String strategyType = strategyType(request.getStrategyType());
        String transportType = transportType(request.getTransportType());

        ShortestPathResult pathResult = mapService.shortestPath(
                request.getDestinationId(),
                request.getStartNodeId(),
                request.getTargetNodeId(),
                strategyType);
        Integer estimatedTime = estimateTime(pathResult, strategyType);

        RouteHistory history = toHistory(userId, pathResult, strategyType, transportType, estimatedTime);
        int inserted = routeHistoryMapper.insert(history);
        if (inserted != 1 || history.getId() == null) {
            throw new BusinessException(ErrorCode.COMMON_006);
        }

        return toRoutePlanVO(pathResult, strategyType, transportType, estimatedTime, history.getId());
    }

    @Override
    @Transactional
    public RoutePlanVO planMultiRoute(MultiRoutePlanRequest request) {
        validateMultiRequest(request);

        Long userId = currentUserId();
        ensureActiveUser(userId);
        String strategyType = strategyType(request.getStrategyType());
        String transportType = transportType(request.getTransportType());
        boolean returnToStart = Boolean.TRUE.equals(request.getReturnToStart());

        MultiPathResult pathResult = mapService.multiTargetPath(
                request.getDestinationId(),
                request.getStartNodeId(),
                request.getTargetNodeIds(),
                returnToStart,
                strategyType);
        Integer estimatedTime = estimateTime(pathResult, strategyType);

        RouteHistory history = toHistory(userId, pathResult, strategyType, transportType, estimatedTime);
        int inserted = routeHistoryMapper.insert(history);
        if (inserted != 1 || history.getId() == null) {
            throw new BusinessException(ErrorCode.COMMON_006);
        }

        return toRoutePlanVO(pathResult, strategyType, transportType, estimatedTime, history.getId());
    }

    private RouteHistory toHistory(
            Long userId,
            ShortestPathResult result,
            String strategyType,
            String transportType,
            Integer estimatedTime) {
        RouteHistory history = new RouteHistory();
        history.setUserId(userId);
        history.setDestinationId(result.getDestinationId());
        history.setStartNodeId(result.getStartNodeId());
        history.setEndNodeId(result.getTargetNodeId());
        history.setPathNodeJson(writeJson(result.getPathNodes()));
        history.setPathEdgeJson(writeJson(result.getPathEdges()));
        history.setStrategyType(strategyType);
        history.setTransportType(transportType);
        history.setTotalDistance(result.getTotalDistance());
        history.setEstimatedTime(estimatedTime);
        return history;
    }

    private RouteHistory toHistory(
            Long userId,
            MultiPathResult result,
            String strategyType,
            String transportType,
            Integer estimatedTime) {
        RouteHistory history = new RouteHistory();
        history.setUserId(userId);
        history.setDestinationId(result.getDestinationId());
        history.setStartNodeId(result.getStartNodeId());
        history.setEndNodeId(result.getEndNodeId());
        history.setPathNodeJson(writeJson(result.getPathNodes()));
        history.setPathEdgeJson(writeJson(result.getPathEdges()));
        history.setStrategyType(strategyType);
        history.setTransportType(transportType);
        history.setTotalDistance(result.getTotalDistance());
        history.setEstimatedTime(estimatedTime);
        return history;
    }

    private RoutePlanVO toRoutePlanVO(
            ShortestPathResult result,
            String strategyType,
            String transportType,
            Integer estimatedTime,
            Long historyId) {
        RoutePlanVO vo = new RoutePlanVO();
        vo.setDestinationId(result.getDestinationId());
        vo.setStrategyType(strategyType);
        vo.setTransportType(transportType);
        vo.setTotalDistance(result.getTotalDistance());
        vo.setEstimatedTime(estimatedTime);
        vo.setPathNodes(result.getPathNodes());
        vo.setPathEdges(result.getPathEdges().stream().map(RoutePathEdgeVO::from).toList());
        vo.setRouteSummary(routeSummary(result.getPathNodes()));
        vo.setHistoryId(historyId);
        return vo;
    }

    private RoutePlanVO toRoutePlanVO(
            MultiPathResult result,
            String strategyType,
            String transportType,
            Integer estimatedTime,
            Long historyId) {
        RoutePlanVO vo = new RoutePlanVO();
        vo.setDestinationId(result.getDestinationId());
        vo.setStrategyType(strategyType);
        vo.setTransportType(transportType);
        vo.setTotalDistance(result.getTotalDistance());
        vo.setEstimatedTime(estimatedTime);
        vo.setPathNodes(result.getPathNodes());
        vo.setPathEdges(result.getPathEdges().stream().map(RoutePathEdgeVO::from).toList());
        vo.setRouteSummary(routeSummary(result.getPathNodes()));
        vo.setHistoryId(historyId);
        return vo;
    }

    private String routeSummary(List<PathNodeResult> pathNodes) {
        if (pathNodes == null || pathNodes.isEmpty()) {
            return "";
        }
        return String.join(" -> ", pathNodes.stream()
                .map(node -> StringUtils.hasText(node.getNodeName())
                        ? node.getNodeName()
                        : String.valueOf(node.getNodeId()))
                .toList());
    }

    private Integer estimateTime(ShortestPathResult pathResult, String strategyType) {
        if (STRATEGY_SHORTEST_TIME.equals(strategyType)) {
            return normalizeEstimatedTime(pathResult.getEstimatedTime());
        }
        return estimateTimeByDistance(pathResult.getTotalDistance());
    }

    private Integer estimateTime(MultiPathResult pathResult, String strategyType) {
        if (STRATEGY_SHORTEST_TIME.equals(strategyType)) {
            return normalizeEstimatedTime(pathResult.getEstimatedTime());
        }
        return estimateTimeByDistance(pathResult.getTotalDistance());
    }

    private Integer normalizeEstimatedTime(BigDecimal estimatedTime) {
        if (estimatedTime == null || estimatedTime.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return estimatedTime.setScale(0, RoundingMode.CEILING).intValue();
    }

    private Integer estimateTimeByDistance(BigDecimal totalDistance) {
        if (totalDistance == null || totalDistance.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return totalDistance.divide(WALK_SPEED_METER_PER_MINUTE, 0, RoundingMode.CEILING).intValue();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.COMMON_006);
        }
    }

    private void validateMultiRequest(MultiRoutePlanRequest request) {
        if (request == null
                || request.getTargetNodeIds() == null
                || request.getTargetNodeIds().isEmpty()
                || request.getTargetNodeIds().size() > MAX_MULTI_TARGET_COUNT) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Set<Long> uniqueTargetIds = new LinkedHashSet<>();
        for (Long targetNodeId : request.getTargetNodeIds()) {
            if (targetNodeId == null || targetNodeId <= 0 || !uniqueTargetIds.add(targetNodeId)) {
                throw new BusinessException(ErrorCode.COMMON_001);
            }
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtClaims claims)) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }
        return claims.getUserId();
    }

    private void ensureActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_009);
        }
        if (user.getStatus() == null || user.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.AUTH_006);
        }
    }

    private String strategyType(String strategyType) {
        if (!StringUtils.hasText(strategyType)) {
            return STRATEGY_SHORTEST_DISTANCE;
        }
        String normalized = strategyType.trim();
        if (!STRATEGY_SHORTEST_DISTANCE.equals(normalized)
                && !STRATEGY_SHORTEST_TIME.equals(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        return normalized;
    }

    private String transportType(String transportType) {
        if (!StringUtils.hasText(transportType)) {
            return DEFAULT_TRANSPORT_TYPE;
        }
        return transportType.trim();
    }
}
