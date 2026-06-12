package com.trip.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.common.ErrorCode;
import com.trip.dto.map.MultiPathResult;
import com.trip.dto.map.PathEdgeResult;
import com.trip.dto.map.PathNodeResult;
import com.trip.dto.map.ShortestPathResult;
import com.trip.dto.request.MultiRoutePlanRequest;
import com.trip.dto.request.RouteHistoryPageQuery;
import com.trip.dto.request.SingleRoutePlanRequest;
import com.trip.entity.Destination;
import com.trip.entity.RouteHistory;
import com.trip.entity.User;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.RouteHistoryMapper;
import com.trip.mapper.UserMapper;
import com.trip.model.route.RouteTransportType;
import com.trip.security.JwtClaims;
import com.trip.service.MapService;
import com.trip.service.RouteService;
import com.trip.vo.response.PageResultVO;
import com.trip.vo.response.RouteHistoryVO;
import com.trip.vo.response.RoutePathEdgeVO;
import com.trip.vo.response.RoutePlanVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteServiceImpl.class);
    private static final int ENABLED_STATUS = 1;
    private static final int MAX_MULTI_TARGET_COUNT = 8;
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String STRATEGY_SHORTEST_DISTANCE = "shortest_distance";
    private static final String STRATEGY_SHORTEST_TIME = "shortest_time";

    private final MapService mapService;
    private final RouteHistoryMapper routeHistoryMapper;
    private final DestinationMapper destinationMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public RouteServiceImpl(
            MapService mapService,
            RouteHistoryMapper routeHistoryMapper,
            DestinationMapper destinationMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper) {
        this.mapService = mapService;
        this.routeHistoryMapper = routeHistoryMapper;
        this.destinationMapper = destinationMapper;
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
        RouteTransportType transportType = transportType(request.getTransportType());
        validateStrategyTransportCombination(strategyType, transportType);

        ShortestPathResult pathResult = mapService.shortestPath(
                request.getDestinationId(),
                request.getStartNodeId(),
                request.getTargetNodeId(),
                strategyType,
                transportType.value());
        Integer estimatedTime = normalizeEstimatedTime(pathResult.getEstimatedTime());

        RouteHistory history = toHistory(userId, pathResult, strategyType, transportType.value(), estimatedTime);
        int inserted = routeHistoryMapper.insert(history);
        if (inserted != 1 || history.getId() == null) {
            throw new BusinessException(ErrorCode.COMMON_006);
        }

        return toRoutePlanVO(pathResult, strategyType, transportType.value(), estimatedTime, history.getId());
    }

    @Override
    @Transactional
    public RoutePlanVO planMultiRoute(MultiRoutePlanRequest request) {
        validateMultiRequest(request);

        Long userId = currentUserId();
        ensureActiveUser(userId);
        String strategyType = strategyType(request.getStrategyType());
        RouteTransportType transportType = transportType(request.getTransportType());
        validateStrategyTransportCombination(strategyType, transportType);
        boolean returnToStart = Boolean.TRUE.equals(request.getReturnToStart());

        MultiPathResult pathResult = mapService.multiTargetPath(
                request.getDestinationId(),
                request.getStartNodeId(),
                request.getTargetNodeIds(),
                returnToStart,
                strategyType,
                transportType.value());
        Integer estimatedTime = normalizeEstimatedTime(pathResult.getEstimatedTime());

        RouteHistory history = toHistory(userId, pathResult, strategyType, transportType.value(), estimatedTime);
        int inserted = routeHistoryMapper.insert(history);
        if (inserted != 1 || history.getId() == null) {
            throw new BusinessException(ErrorCode.COMMON_006);
        }

        return toRoutePlanVO(pathResult, strategyType, transportType.value(), estimatedTime, history.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResultVO<RouteHistoryVO> listMyRouteHistories(RouteHistoryPageQuery query) {
        Long userId = currentUserId();
        ensureActiveUser(userId);
        int pageNum = query == null || query.getPageNum() == null ? DEFAULT_PAGE_NUM : query.getPageNum();
        int pageSize = query == null || query.getPageSize() == null ? DEFAULT_PAGE_SIZE : query.getPageSize();

        IPage<RouteHistory> page = routeHistoryMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<RouteHistory>lambdaQuery()
                        .eq(RouteHistory::getUserId, userId)
                        .orderByDesc(RouteHistory::getCreatedAt)
                        .orderByDesc(RouteHistory::getId));
        Map<Long, String> destinationNames = destinationNames(page.getRecords());
        List<RouteHistoryVO> records = page.getRecords().stream()
                .map(history -> toHistorySummaryVO(history, destinationNames.get(history.getDestinationId())))
                .toList();
        return PageResultVO.of(records, page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    @Override
    @Transactional(readOnly = true)
    public RouteHistoryVO getMyRouteHistory(Long historyId) {
        if (historyId == null || historyId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Long userId = currentUserId();
        ensureActiveUser(userId);
        RouteHistory history = routeHistoryMapper.selectOne(Wrappers.<RouteHistory>lambdaQuery()
                .eq(RouteHistory::getId, historyId)
                .eq(RouteHistory::getUserId, userId));
        if (history == null) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        Destination destination = destinationMapper.selectById(history.getDestinationId());
        return toHistoryDetailVO(history, destination == null ? null : destination.getName());
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

    private Map<Long, String> destinationNames(List<RouteHistory> histories) {
        Set<Long> destinationIds = histories.stream()
                .map(RouteHistory::getDestinationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (destinationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        destinationMapper.selectBatchIds(destinationIds)
                .forEach(destination -> names.put(destination.getId(), destination.getName()));
        return names;
    }

    private RouteHistoryVO toHistorySummaryVO(RouteHistory history, String destinationName) {
        List<PathNodeResult> pathNodes = readPathNodes(history);
        RouteHistoryVO vo = historyBaseVO(history, destinationName);
        vo.setStartNodeName(nodeName(pathNodes, history.getStartNodeId()));
        vo.setEndNodeName(nodeName(pathNodes, history.getEndNodeId()));
        return vo;
    }

    private RouteHistoryVO toHistoryDetailVO(RouteHistory history, String destinationName) {
        List<PathNodeResult> pathNodes = readPathNodes(history);
        RouteHistoryVO vo = historyBaseVO(history, destinationName);
        vo.setStartNodeName(nodeName(pathNodes, history.getStartNodeId()));
        vo.setEndNodeName(nodeName(pathNodes, history.getEndNodeId()));
        vo.setPathNodes(pathNodes);
        vo.setPathEdges(readPathEdges(history).stream().map(RoutePathEdgeVO::from).toList());
        vo.setOrderedTargetNodeIds(readOrderedTargetNodeIds(history));
        return vo;
    }

    private RouteHistoryVO historyBaseVO(RouteHistory history, String destinationName) {
        RouteHistoryVO vo = new RouteHistoryVO();
        vo.setId(history.getId());
        vo.setDestinationId(history.getDestinationId());
        vo.setDestinationName(destinationName);
        vo.setStartNodeId(history.getStartNodeId());
        vo.setEndNodeId(history.getEndNodeId());
        vo.setStrategyType(history.getStrategyType());
        vo.setTransportType(history.getTransportType());
        vo.setTotalDistance(history.getTotalDistance());
        vo.setEstimatedTime(history.getEstimatedTime());
        vo.setCreatedAt(history.getCreatedAt());
        return vo;
    }

    private List<PathNodeResult> readPathNodes(RouteHistory history) {
        return readJson(
                history.getId(),
                history.getPathNodeJson(),
                new TypeReference<List<PathNodeResult>>() {});
    }

    private List<PathEdgeResult> readPathEdges(RouteHistory history) {
        return readJson(
                history.getId(),
                history.getPathEdgeJson(),
                new TypeReference<List<PathEdgeResult>>() {});
    }

    private List<Long> readOrderedTargetNodeIds(RouteHistory history) {
        return readJson(
                history.getId(),
                history.getOrderedTargetNodeJson(),
                new TypeReference<List<Long>>() {});
    }

    private <T> List<T> readJson(Long historyId, String json, TypeReference<List<T>> typeReference) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<T> values = objectMapper.readValue(json, typeReference);
            return values == null ? List.of() : values;
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Failed to parse route history snapshot, historyId={}", historyId);
            throw new BusinessException(ErrorCode.ROUTE_010);
        }
    }

    private String nodeName(List<PathNodeResult> pathNodes, Long nodeId) {
        if (nodeId == null) {
            return null;
        }
        return pathNodes.stream()
                .filter(node -> nodeId.equals(node.getNodeId()))
                .map(PathNodeResult::getNodeName)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
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
        history.setOrderedTargetNodeJson(writeJson(result.getOrderedTargetNodeIds()));
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

    private Integer normalizeEstimatedTime(BigDecimal estimatedTime) {
        if (estimatedTime == null || estimatedTime.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return estimatedTime.setScale(0, RoundingMode.CEILING).intValue();
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

    private RouteTransportType transportType(String transportType) {
        if (!StringUtils.hasText(transportType)) {
            return RouteTransportType.WALK;
        }
        return RouteTransportType.fromValue(transportType)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_005));
    }

    private void validateStrategyTransportCombination(
            String strategyType,
            RouteTransportType transportType) {
        if (RouteTransportType.MIXED.equals(transportType)
                && !STRATEGY_SHORTEST_TIME.equals(strategyType)) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
    }
}
