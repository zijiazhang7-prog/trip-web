package com.trip;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.trip.security.JwtClaims;
import com.trip.service.MapService;
import com.trip.service.impl.RouteServiceImpl;
import com.trip.vo.response.PageResultVO;
import com.trip.vo.response.RouteHistoryVO;
import com.trip.vo.response.RoutePlanVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RouteServiceTests {

    private final MapService mapService = mock(MapService.class);
    private final RouteHistoryMapper routeHistoryMapper = mock(RouteHistoryMapper.class);
    private final DestinationMapper destinationMapper = mock(DestinationMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final RouteServiceImpl routeService = new RouteServiceImpl(
            mapService,
            routeHistoryMapper,
            destinationMapper,
            userMapper,
            new ObjectMapper());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void planSingleRouteShouldSaveHistoryAndReturnPlan() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(mapService.shortestPath(101L, 1L, 3L, "shortest_distance", "walk")).thenReturn(pathResult());
        when(routeHistoryMapper.insert(any(RouteHistory.class))).thenAnswer(invocation -> {
            RouteHistory history = invocation.getArgument(0);
            history.setId(9001L);
            return 1;
        });

        RoutePlanVO result = routeService.planSingleRoute(validRequest());

        assertEquals(101L, result.getDestinationId());
        assertEquals("shortest_distance", result.getStrategyType());
        assertEquals("walk", result.getTransportType());
        assertEquals(new BigDecimal("160.00"), result.getTotalDistance());
        assertEquals(2, result.getEstimatedTime());
        assertEquals(9001L, result.getHistoryId());
        assertEquals("校门 -> 图书馆 -> 食堂", result.getRouteSummary());
        assertEquals(List.of(1L, 2L, 3L), result.getPathNodes().stream().map(PathNodeResult::getNodeId).toList());
        assertEquals(2, result.getPathEdges().size());

        ArgumentCaptor<RouteHistory> captor = ArgumentCaptor.forClass(RouteHistory.class);
        verify(routeHistoryMapper).insert(captor.capture());
        RouteHistory history = captor.getValue();
        assertEquals(7L, history.getUserId());
        assertEquals(101L, history.getDestinationId());
        assertEquals(1L, history.getStartNodeId());
        assertEquals(3L, history.getEndNodeId());
        assertEquals("shortest_distance", history.getStrategyType());
        assertEquals("walk", history.getTransportType());
        assertEquals(new BigDecimal("160.00"), history.getTotalDistance());
        assertEquals(2, history.getEstimatedTime());
        assertTrue(history.getPathNodeJson().contains("校门"));
        assertTrue(history.getPathEdgeJson().contains("fromNodeId"));
    }

    @Test
    void planSingleRouteShouldKeepProvidedTransportTypeAsRecordOnly() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(mapService.shortestPath(101L, 1L, 3L, "shortest_distance", "bike")).thenReturn(pathResult());
        when(routeHistoryMapper.insert(any(RouteHistory.class))).thenAnswer(invocation -> {
            RouteHistory history = invocation.getArgument(0);
            history.setId(9002L);
            return 1;
        });

        SingleRoutePlanRequest request = validRequest();
        request.setTransportType("bike");

        RoutePlanVO result = routeService.planSingleRoute(request);

        assertEquals("bike", result.getTransportType());
        verify(mapService).shortestPath(101L, 1L, 3L, "shortest_distance", "bike");
    }

    @Test
    void planMultiRouteShouldSaveHistoryAndReturnMergedPlan() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(mapService.multiTargetPath(101L, 1L, List.of(2L, 3L), true, "shortest_distance", "walk"))
                .thenReturn(multiPathResult());
        when(routeHistoryMapper.insert(any(RouteHistory.class))).thenAnswer(invocation -> {
            RouteHistory history = invocation.getArgument(0);
            history.setId(9010L);
            return 1;
        });

        RoutePlanVO result = routeService.planMultiRoute(validMultiRequest());

        assertEquals(101L, result.getDestinationId());
        assertEquals("shortest_distance", result.getStrategyType());
        assertEquals("walk", result.getTransportType());
        assertEquals(new BigDecimal("240.00"), result.getTotalDistance());
        assertEquals(3, result.getEstimatedTime());
        assertEquals(9010L, result.getHistoryId());
        assertEquals("校门 -> 图书馆 -> 食堂 -> 校门", result.getRouteSummary());

        ArgumentCaptor<RouteHistory> captor = ArgumentCaptor.forClass(RouteHistory.class);
        verify(routeHistoryMapper).insert(captor.capture());
        RouteHistory history = captor.getValue();
        assertEquals(7L, history.getUserId());
        assertEquals(101L, history.getDestinationId());
        assertEquals(1L, history.getStartNodeId());
        assertEquals(1L, history.getEndNodeId());
        assertEquals(new BigDecimal("240.00"), history.getTotalDistance());
        assertTrue(history.getPathNodeJson().contains("图书馆"));
        assertTrue(history.getPathEdgeJson().contains("edgeId"));
        assertEquals("[2,3]", history.getOrderedTargetNodeJson());
    }

    @Test
    void listMyRouteHistoriesShouldReturnOnlyPagedCurrentUserSummaries() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        RouteHistory history = routeHistory(9001L, 7L);
        Page<RouteHistory> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(history));
        when(routeHistoryMapper.selectPage(any(Page.class), any())).thenReturn(page);
        Destination destination = new Destination();
        destination.setId(101L);
        destination.setName("北邮沙河校区");
        when(destinationMapper.selectBatchIds(anyCollection())).thenReturn(List.of(destination));

        PageResultVO<RouteHistoryVO> result = routeService.listMyRouteHistories(new RouteHistoryPageQuery());

        assertEquals(1, result.getList().size());
        RouteHistoryVO item = result.getList().get(0);
        assertEquals("北邮沙河校区", item.getDestinationName());
        assertEquals("校门", item.getStartNodeName());
        assertEquals("食堂", item.getEndNodeName());
        assertEquals(1, result.getTotal());
        assertNull(item.getPathNodes());
        verifyNoInteractions(mapService);
    }

    @Test
    void getMyRouteHistoryShouldRestoreSavedPathWithoutReplanning() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        RouteHistory history = routeHistory(9001L, 7L);
        history.setOrderedTargetNodeJson("[2,3]");
        when(routeHistoryMapper.selectOne(any())).thenReturn(history);
        Destination destination = new Destination();
        destination.setId(101L);
        destination.setName("北邮沙河校区");
        when(destinationMapper.selectById(101L)).thenReturn(destination);

        RouteHistoryVO result = routeService.getMyRouteHistory(9001L);

        assertEquals(List.of(1L, 2L, 3L), result.getPathNodes().stream()
                .map(PathNodeResult::getNodeId)
                .toList());
        assertEquals(2, result.getPathEdges().size());
        assertEquals(List.of(2L, 3L), result.getOrderedTargetNodeIds());
        assertEquals("walk", result.getPathEdges().get(0).getTransportType());
        verifyNoInteractions(mapService);
    }

    @Test
    void getMyRouteHistoryShouldHideMissingOrOtherUsersHistory() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(routeHistoryMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.getMyRouteHistory(9999L));

        assertEquals(ErrorCode.COMMON_003, exception.getErrorCode());
        verify(destinationMapper, never()).selectById(any());
        verifyNoInteractions(mapService);
    }

    @Test
    void getMyRouteHistoryShouldSupportLegacyRecordWithoutOrderedTargets() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        RouteHistory history = routeHistory(9001L, 7L);
        when(routeHistoryMapper.selectOne(any())).thenReturn(history);

        RouteHistoryVO result = routeService.getMyRouteHistory(9001L);

        assertTrue(result.getOrderedTargetNodeIds().isEmpty());
    }

    @Test
    void getMyRouteHistoryShouldRejectBrokenSnapshot() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        RouteHistory history = routeHistory(9001L, 7L);
        history.setPathNodeJson("{broken");
        when(routeHistoryMapper.selectOne(any())).thenReturn(history);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.getMyRouteHistory(9001L));

        assertEquals(ErrorCode.ROUTE_010, exception.getErrorCode());
        verifyNoInteractions(mapService);
    }

    @Test
    void planSingleRouteShouldRejectMissingLogin() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.planSingleRoute(validRequest()));

        assertEquals(ErrorCode.AUTH_003, exception.getErrorCode());
        verifyNoInteractions(mapService);
    }

    @Test
    void planSingleRouteShouldRejectDisabledUser() {
        setCurrentUser(7L);
        User user = activeUser(7L);
        user.setStatus(0);
        when(userMapper.selectById(7L)).thenReturn(user);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.planSingleRoute(validRequest()));

        assertEquals(ErrorCode.AUTH_006, exception.getErrorCode());
        verifyNoInteractions(mapService);
    }

    @Test
    void planSingleRouteShouldRejectUnknownStrategy() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        SingleRoutePlanRequest request = validRequest();
        request.setStrategyType("fastest");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.planSingleRoute(request));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
        verifyNoInteractions(mapService);
    }

    @Test
    void planMultiRouteShouldRejectDuplicateTargets() {
        MultiRoutePlanRequest request = validMultiRequest();
        request.setTargetNodeIds(List.of(2L, 2L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.planMultiRoute(request));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
        verifyNoInteractions(mapService);
    }

    @Test
    void planMultiRouteShouldRejectTooManyTargets() {
        MultiRoutePlanRequest request = validMultiRequest();
        request.setTargetNodeIds(List.of(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.planMultiRoute(request));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
        verifyNoInteractions(mapService);
    }

    @Test
    void planSingleRouteShouldPropagateMapServiceBusinessError() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(mapService.shortestPath(101L, 1L, 3L, "shortest_distance", "walk"))
                .thenThrow(new BusinessException(ErrorCode.ROUTE_003));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.planSingleRoute(validRequest()));

        assertEquals(ErrorCode.ROUTE_003, exception.getErrorCode());
    }

    private SingleRoutePlanRequest validRequest() {
        SingleRoutePlanRequest request = new SingleRoutePlanRequest();
        request.setDestinationId(101L);
        request.setStartNodeId(1L);
        request.setTargetNodeId(3L);
        return request;
    }

    @Test
    void planSingleRouteShouldUseShortestTimeStrategyAndEstimatedTime() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(mapService.shortestPath(101L, 1L, 3L, "shortest_time", "walk")).thenReturn(pathResultForTime());
        when(routeHistoryMapper.insert(any(RouteHistory.class))).thenAnswer(invocation -> {
            RouteHistory history = invocation.getArgument(0);
            history.setId(9003L);
            return 1;
        });

        SingleRoutePlanRequest request = validRequest();
        request.setStrategyType("shortest_time");

        RoutePlanVO result = routeService.planSingleRoute(request);

        assertEquals("shortest_time", result.getStrategyType());
        assertEquals(4, result.getEstimatedTime());
        verify(mapService).shortestPath(101L, 1L, 3L, "shortest_time", "walk");
    }

    @Test
    void planSingleRouteShouldRejectUnknownTransportType() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        SingleRoutePlanRequest request = validRequest();
        request.setTransportType("fly");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.planSingleRoute(request));

        assertEquals(ErrorCode.ROUTE_005, exception.getErrorCode());
        verifyNoInteractions(mapService);
    }

    @Test
    void planSingleRouteShouldRejectMixedDistanceStrategy() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        SingleRoutePlanRequest request = validRequest();
        request.setTransportType("mixed");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> routeService.planSingleRoute(request));

        assertEquals(ErrorCode.COMMON_002, exception.getErrorCode());
        verifyNoInteractions(mapService);
    }

    @Test
    void planSingleRouteShouldSaveMixedPathEdgeTransports() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        ShortestPathResult pathResult = pathResultForTime();
        pathResult.getPathEdges().get(1).setTransportType("bike");
        when(mapService.shortestPath(101L, 1L, 3L, "shortest_time", "mixed")).thenReturn(pathResult);
        when(routeHistoryMapper.insert(any(RouteHistory.class))).thenAnswer(invocation -> {
            RouteHistory history = invocation.getArgument(0);
            history.setId(9004L);
            return 1;
        });

        SingleRoutePlanRequest request = validRequest();
        request.setStrategyType("shortest_time");
        request.setTransportType("mixed");
        RoutePlanVO result = routeService.planSingleRoute(request);

        assertEquals("mixed", result.getTransportType());
        assertEquals(List.of("walk", "bike"), result.getPathEdges().stream()
                .map(item -> item.getTransportType())
                .toList());
        ArgumentCaptor<RouteHistory> captor = ArgumentCaptor.forClass(RouteHistory.class);
        verify(routeHistoryMapper).insert(captor.capture());
        assertTrue(captor.getValue().getPathEdgeJson().contains("\"transportType\":\"bike\""));
    }

    private MultiRoutePlanRequest validMultiRequest() {
        MultiRoutePlanRequest request = new MultiRoutePlanRequest();
        request.setDestinationId(101L);
        request.setStartNodeId(1L);
        request.setTargetNodeIds(List.of(2L, 3L));
        request.setReturnToStart(true);
        return request;
    }

    private ShortestPathResult pathResult() {
        ShortestPathResult result = new ShortestPathResult();
        result.setDestinationId(101L);
        result.setStartNodeId(1L);
        result.setTargetNodeId(3L);
        result.setTotalDistance(new BigDecimal("160.00"));
        result.setEstimatedTime(new BigDecimal("2.00"));
        result.setPathNodes(List.of(
                new PathNodeResult(1L, "校门"),
                new PathNodeResult(2L, "图书馆"),
                new PathNodeResult(3L, "食堂")));
        result.setPathEdges(List.of(
                new PathEdgeResult(11L, 1L, 2L, new BigDecimal("80.00"), "walk"),
                new PathEdgeResult(12L, 2L, 3L, new BigDecimal("80.00"), "walk")));
        return result;
    }

    private MultiPathResult multiPathResult() {
        MultiPathResult result = new MultiPathResult();
        result.setDestinationId(101L);
        result.setStartNodeId(1L);
        result.setEndNodeId(1L);
        result.setOrderedTargetNodeIds(List.of(2L, 3L));
        result.setTotalDistance(new BigDecimal("240.00"));
        result.setEstimatedTime(new BigDecimal("3.00"));
        result.setPathNodes(List.of(
                new PathNodeResult(1L, "校门"),
                new PathNodeResult(2L, "图书馆"),
                new PathNodeResult(3L, "食堂"),
                new PathNodeResult(1L, "校门")));
        result.setPathEdges(List.of(
                new PathEdgeResult(11L, 1L, 2L, new BigDecimal("80.00"), "walk"),
                new PathEdgeResult(12L, 2L, 3L, new BigDecimal("80.00"), "walk"),
                new PathEdgeResult(13L, 3L, 1L, new BigDecimal("80.00"), "walk")));
        return result;
    }

    private ShortestPathResult pathResultForTime() {
        ShortestPathResult result = pathResult();
        result.setEstimatedTime(new BigDecimal("3.10"));
        return result;
    }

    private void setCurrentUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtClaims(userId, "route_user", "user", 1L, 2L),
                null,
                List.of()));
    }

    private User activeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("route_user");
        user.setStatus(1);
        user.setRole("user");
        return user;
    }

    private RouteHistory routeHistory(Long id, Long userId) {
        RouteHistory history = new RouteHistory();
        history.setId(id);
        history.setUserId(userId);
        history.setDestinationId(101L);
        history.setStartNodeId(1L);
        history.setEndNodeId(3L);
        history.setPathNodeJson(
                "[{\"nodeId\":1,\"nodeName\":\"校门\"},"
                        + "{\"nodeId\":2,\"nodeName\":\"图书馆\"},"
                        + "{\"nodeId\":3,\"nodeName\":\"食堂\"}]");
        history.setPathEdgeJson(
                "[{\"edgeId\":11,\"fromNodeId\":1,\"toNodeId\":2,\"distance\":80.00,"
                        + "\"transportType\":\"walk\"},"
                        + "{\"edgeId\":12,\"fromNodeId\":2,\"toNodeId\":3,\"distance\":80.00,"
                        + "\"transportType\":\"walk\"}]");
        history.setStrategyType("shortest_distance");
        history.setTransportType("walk");
        history.setTotalDistance(new BigDecimal("160.00"));
        history.setEstimatedTime(2);
        history.setCreatedAt(LocalDateTime.of(2026, 6, 12, 10, 0));
        return history;
    }
}
