package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.trip.common.ErrorCode;
import com.trip.dto.map.IndoorPathEdgeResult;
import com.trip.dto.map.IndoorPathNodeResult;
import com.trip.dto.map.IndoorPathResult;
import com.trip.dto.request.IndoorRoutePlanRequest;
import com.trip.entity.MapNode;
import com.trip.entity.Place;
import com.trip.exception.BusinessException;
import com.trip.mapper.MapEdgeMapper;
import com.trip.mapper.MapNodeMapper;
import com.trip.mapper.PlaceMapper;
import com.trip.service.MapService;
import com.trip.service.impl.IndoorRouteServiceImpl;
import com.trip.vo.response.IndoorRoutePlanVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class IndoorRouteServiceTests {

    private final PlaceMapper placeMapper = mock(PlaceMapper.class);
    private final MapNodeMapper mapNodeMapper = mock(MapNodeMapper.class);
    private final MapEdgeMapper mapEdgeMapper = mock(MapEdgeMapper.class);
    private final MapService mapService = mock(MapService.class);
    private final IndoorRouteServiceImpl service = new IndoorRouteServiceImpl(
            placeMapper, mapNodeMapper, mapEdgeMapper, mapService);

    @Test
    void listBuildingsShouldReturnOnlyBuildingsWithIndoorNodes() {
        Place building = building(201L, 101L, "教学楼A");
        when(mapNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                node(1L, 101L, 201L, "一层大厅", "hall", 1),
                node(2L, 101L, 201L, "二层走廊", "corridor", 2)));
        when(placeMapper.selectBatchIds(any())).thenReturn(List.of(building));

        assertEquals(List.of(1, 2), service.listBuildings(101L).get(0).getFloorNos());
    }

    @Test
    void planRouteShouldReturnSecondsAndReadableSteps() {
        Place building = building(201L, 101L, "教学楼A");
        when(placeMapper.selectById(201L)).thenReturn(building);
        when(mapService.indoorShortestPath(
                101L, 201L, 1L, 3L, "shortest_time", "elevator"))
                .thenReturn(pathResult());

        IndoorRoutePlanRequest request = new IndoorRoutePlanRequest();
        request.setDestinationId(101L);
        request.setBuildingId(201L);
        request.setStartNodeId(1L);
        request.setTargetNodeId(3L);
        request.setStrategyType("shortest_time");
        request.setVerticalMode("elevator");

        IndoorRoutePlanVO result = service.planRoute(request);

        assertEquals(45, result.getTotalTime());
        assertEquals("second", result.getTimeUnit());
        assertEquals("乘电梯从1层到2层", result.getSteps().get(0));
        assertEquals("elevator", result.getPathEdges().get(0).getEdgeType());
        verify(mapService).indoorShortestPath(
                101L, 201L, 1L, 3L, "shortest_time", "elevator");
    }

    @Test
    void planRouteShouldRejectBuildingFromAnotherDestination() {
        when(placeMapper.selectById(201L)).thenReturn(building(201L, 102L, "其他建筑"));
        IndoorRoutePlanRequest request = new IndoorRoutePlanRequest();
        request.setDestinationId(101L);
        request.setBuildingId(201L);
        request.setStartNodeId(1L);
        request.setTargetNodeId(3L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.planRoute(request));

        assertEquals(ErrorCode.ROUTE_011, exception.getErrorCode());
    }

    private IndoorPathResult pathResult() {
        IndoorPathResult result = new IndoorPathResult();
        result.setDestinationId(101L);
        result.setBuildingId(201L);
        result.setStartNodeId(1L);
        result.setTargetNodeId(3L);
        result.setTotalDistance(new BigDecimal("18.00"));
        result.setEstimatedTime(new BigDecimal("0.75"));
        result.setPathNodes(List.of(
                pathNode(1L, "一层电梯口", "elevator", 1),
                pathNode(3L, "二层电梯口", "elevator", 2)));
        IndoorPathEdgeResult edge = new IndoorPathEdgeResult();
        edge.setEdgeId(11L);
        edge.setFromNodeId(1L);
        edge.setToNodeId(3L);
        edge.setEdgeType("elevator");
        edge.setDistance(new BigDecimal("18.00"));
        edge.setTimeCost(new BigDecimal("0.75"));
        result.setPathEdges(List.of(edge));
        return result;
    }

    private IndoorPathNodeResult pathNode(Long id, String name, String type, Integer floorNo) {
        IndoorPathNodeResult node = new IndoorPathNodeResult();
        node.setNodeId(id);
        node.setNodeName(name);
        node.setNodeType(type);
        node.setFloorNo(floorNo);
        node.setIndoorX(new BigDecimal("100"));
        node.setIndoorY(new BigDecimal("200"));
        return node;
    }

    private Place building(Long id, Long destinationId, String name) {
        Place place = new Place();
        place.setId(id);
        place.setDestinationId(destinationId);
        place.setName(name);
        place.setPlaceType("教学楼");
        place.setFloorInfo("1F-3F");
        return place;
    }

    private MapNode node(
            Long id,
            Long destinationId,
            Long placeId,
            String name,
            String type,
            Integer floorNo) {
        MapNode node = new MapNode();
        node.setId(id);
        node.setDestinationId(destinationId);
        node.setPlaceId(placeId);
        node.setNodeName(name);
        node.setNodeType(type);
        node.setFloorNo(floorNo);
        return node;
    }
}
