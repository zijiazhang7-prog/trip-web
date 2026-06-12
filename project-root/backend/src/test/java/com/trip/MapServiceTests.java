package com.trip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.common.ErrorCode;
import com.trip.dto.map.MultiPathResult;
import com.trip.dto.map.ShortestPathResult;
import com.trip.entity.MapEdge;
import com.trip.entity.MapNode;
import com.trip.exception.BusinessException;
import com.trip.mapper.MapEdgeMapper;
import com.trip.mapper.MapNodeMapper;
import com.trip.service.impl.MapServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MapServiceTests {

    private final MapNodeMapper mapNodeMapper = mock(MapNodeMapper.class);
    private final MapEdgeMapper mapEdgeMapper = mock(MapEdgeMapper.class);
    private final MapServiceImpl mapService = new MapServiceImpl(mapNodeMapper, mapEdgeMapper);

    @Test
    void shortestPathShouldReturnShortestDirectedPath() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B"), node(3L, 101L, "C")), List.of(
                edge(11L, 101L, 1L, 2L, "2.00", 0),
                edge(12L, 101L, 2L, 3L, "3.00", 0),
                edge(13L, 101L, 1L, 3L, "10.00", 0)));

        ShortestPathResult result = mapService.shortestPath(101L, 1L, 3L);

        assertEquals(new BigDecimal("5.00"), result.getTotalDistance());
        assertEquals(List.of(1L, 2L, 3L), result.getPathNodes().stream().map(item -> item.getNodeId()).toList());
        assertEquals(List.of(11L, 12L), result.getPathEdges().stream().map(item -> item.getEdgeId()).toList());
    }

    @Test
    void shortestPathShouldSupportBidirectionalEdge() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B")), List.of(
                edge(11L, 101L, 1L, 2L, "2.00", 1)));

        ShortestPathResult result = mapService.shortestPath(101L, 2L, 1L);

        assertEquals(new BigDecimal("2.00"), result.getTotalDistance());
        assertEquals(List.of(2L, 1L), result.getPathNodes().stream().map(item -> item.getNodeId()).toList());
        assertEquals(List.of(11L), result.getPathEdges().stream().map(item -> item.getEdgeId()).toList());
    }

    @Test
    void shortestPathShouldReturnZeroWhenStartEqualsTarget() {
        mockGraph(List.of(node(1L, 101L, "A")), List.of());

        ShortestPathResult result = mapService.shortestPath(101L, 1L, 1L);

        assertEquals(BigDecimal.ZERO, result.getTotalDistance());
        assertEquals(List.of(1L), result.getPathNodes().stream().map(item -> item.getNodeId()).toList());
        assertEquals(List.of(), result.getPathEdges());
    }

    @Test
    void shortestDistancesShouldReturnReachableNodeDistancesOnly() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B"), node(3L, 101L, "C"), node(4L, 101L, "D")), List.of(
                edge(11L, 101L, 1L, 2L, "2.00", 0),
                edge(12L, 101L, 2L, 3L, "3.00", 0)));

        Map<Long, BigDecimal> distances = mapService.shortestDistances(101L, 1L);

        assertEquals(new BigDecimal("0"), distances.get(1L));
        assertEquals(new BigDecimal("2.00"), distances.get(2L));
        assertEquals(new BigDecimal("5.00"), distances.get(3L));
        assertEquals(false, distances.containsKey(4L));
    }

    @Test
    void multiTargetPathShouldUseNearestNeighborAndMergeSegments() {
        mockGraph(List.of(
                node(1L, 101L, "A"),
                node(2L, 101L, "B"),
                node(3L, 101L, "C")), List.of(
                edge(11L, 101L, 1L, 2L, "1.00", 0),
                edge(12L, 101L, 2L, 3L, "2.00", 0),
                edge(13L, 101L, 1L, 3L, "5.00", 0)));

        MultiPathResult result = mapService.multiTargetPath(101L, 1L, List.of(3L, 2L), false);

        assertEquals(List.of(2L, 3L), result.getOrderedTargetNodeIds());
        assertEquals(new BigDecimal("3.00"), result.getTotalDistance());
        assertEquals(3L, result.getEndNodeId());
        assertEquals(List.of(1L, 2L, 3L), result.getPathNodes().stream().map(item -> item.getNodeId()).toList());
        assertEquals(List.of(11L, 12L), result.getPathEdges().stream().map(item -> item.getEdgeId()).toList());
    }

    @Test
    void multiTargetPathShouldAppendReturnPathWhenNeeded() {
        mockGraph(List.of(
                node(1L, 101L, "A"),
                node(2L, 101L, "B"),
                node(3L, 101L, "C")), List.of(
                edge(11L, 101L, 1L, 2L, "1.00", 0),
                edge(12L, 101L, 2L, 3L, "2.00", 0),
                edge(13L, 101L, 3L, 1L, "4.00", 0)));

        MultiPathResult result = mapService.multiTargetPath(101L, 1L, List.of(2L, 3L), true);

        assertEquals(new BigDecimal("7.00"), result.getTotalDistance());
        assertEquals(1L, result.getEndNodeId());
        assertEquals(List.of(1L, 2L, 3L, 1L), result.getPathNodes().stream().map(item -> item.getNodeId()).toList());
        assertEquals(List.of(11L, 12L, 13L), result.getPathEdges().stream().map(item -> item.getEdgeId()).toList());
    }

    @Test
    void multiTargetPathShouldRejectDuplicateTargets() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> mapService.multiTargetPath(101L, 1L, List.of(2L, 2L), false));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
    }

    @Test
    void multiTargetPathShouldRejectUnreachableTarget() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B")), List.of());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> mapService.multiTargetPath(101L, 1L, List.of(2L), false));

        assertEquals(ErrorCode.ROUTE_003, exception.getErrorCode());
    }

    @Test
    void shortestPathShouldRejectMissingStartNode() {
        mockGraph(List.of(node(2L, 101L, "B")), List.of());

        BusinessException exception = assertThrows(
                BusinessException.class, () -> mapService.shortestPath(101L, 1L, 2L));

        assertEquals(ErrorCode.ROUTE_001, exception.getErrorCode());
    }

    @Test
    void shortestPathShouldRejectMissingTargetNode() {
        mockGraph(List.of(node(1L, 101L, "A")), List.of());

        BusinessException exception = assertThrows(
                BusinessException.class, () -> mapService.shortestPath(101L, 1L, 2L));

        assertEquals(ErrorCode.ROUTE_002, exception.getErrorCode());
    }

    @Test
    void shortestPathShouldRejectCrossDestinationNodes() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 102L, "B")), List.of());

        BusinessException exception = assertThrows(
                BusinessException.class, () -> mapService.shortestPath(101L, 1L, 2L));

        assertEquals(ErrorCode.ROUTE_009, exception.getErrorCode());
    }

    @Test
    void shortestPathShouldRejectUnreachableTarget() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B")), List.of());

        BusinessException exception = assertThrows(
                BusinessException.class, () -> mapService.shortestPath(101L, 1L, 2L));

        assertEquals(ErrorCode.ROUTE_003, exception.getErrorCode());
    }

    @Test
    void shortestPathShouldIgnoreNonPositiveDistanceEdge() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B")), List.of(
                edge(11L, 101L, 1L, 2L, "0.00", 0)));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> mapService.shortestPath(101L, 1L, 2L));

        assertEquals(ErrorCode.ROUTE_003, exception.getErrorCode());
    }

    @Test
    void shortestPathShouldRejectInvalidId() {
        BusinessException exception = assertThrows(
                BusinessException.class, () -> mapService.shortestPath(101L, null, 2L));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
    }

    @Test
    void shortestPathShouldUseTimeWeightWhenStrategyIsShortestTime() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B"), node(3L, 101L, "C")), List.of(
                edgeWithTime(11L, 101L, 1L, 2L, "100.00", "10.00", "1.00", 0),
                edgeWithTime(12L, 101L, 2L, 3L, "100.00", "10.00", "1.00", 0),
                edgeWithTime(13L, 101L, 1L, 3L, "300.00", "100.00", "1.00", 0)));

        ShortestPathResult result = mapService.shortestPath(101L, 1L, 3L, "shortest_time");

        assertEquals(new BigDecimal("300.00"), result.getTotalDistance());
        assertEquals(List.of(1L, 3L), result.getPathNodes().stream().map(item -> item.getNodeId()).toList());
        assertEquals(List.of(13L), result.getPathEdges().stream().map(item -> item.getEdgeId()).toList());
        assertEquals(new BigDecimal("3.75000000"), result.getEstimatedTime());
    }

    @Test
    void shortestPathShouldIgnoreInvalidTimeEdgeInShortestTimeStrategy() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B")), List.of(
                edgeWithTime(11L, 101L, 1L, 2L, "100.00", "0.00", "1.00", 0)));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> mapService.shortestPath(101L, 1L, 2L, "shortest_time"));

        assertEquals(ErrorCode.ROUTE_003, exception.getErrorCode());
    }

    @Test
    void multiTargetPathShouldChooseNearestByTimeWhenStrategyIsShortestTime() {
        mockGraph(List.of(
                node(1L, 101L, "A"),
                node(2L, 101L, "B"),
                node(3L, 101L, "C")), List.of(
                edgeWithTime(11L, 101L, 1L, 2L, "100.00", "10.00", "1.00", 0),
                edgeWithTime(12L, 101L, 1L, 3L, "200.00", "100.00", "1.00", 0),
                edgeWithTime(13L, 101L, 3L, 2L, "100.00", "100.00", "1.00", 0)));

        MultiPathResult result = mapService.multiTargetPath(101L, 1L, List.of(2L, 3L), false, "shortest_time");

        assertEquals(List.of(3L, 2L), result.getOrderedTargetNodeIds());
        assertEquals(new BigDecimal("300.00"), result.getTotalDistance());
        assertEquals(new BigDecimal("3.75000000"), result.getEstimatedTime());
    }

    @Test
    void shortestPathShouldFilterBySingleTransportAndReturnActualTransport() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B"), node(3L, 101L, "C")), List.of(
                edgeWithTransport(11L, 101L, 1L, 2L, "80.00", "80", "1.00", "walk", 0),
                edgeWithTransport(12L, 101L, 1L, 3L, "250.00", "250", "1.00", "walk_bike", 0),
                edgeWithTransport(13L, 101L, 3L, 2L, "250.00", "250", "1.00", "bike", 0)));

        ShortestPathResult walk = mapService.shortestPath(
                101L, 1L, 2L, "shortest_time", "walk");
        ShortestPathResult bike = mapService.shortestPath(
                101L, 1L, 2L, "shortest_time", "bike");

        assertEquals(List.of(11L), walk.getPathEdges().stream().map(item -> item.getEdgeId()).toList());
        assertEquals(List.of(12L, 13L), bike.getPathEdges().stream().map(item -> item.getEdgeId()).toList());
        assertEquals(List.of("bike", "bike"), bike.getPathEdges().stream()
                .map(item -> item.getTransportType())
                .toList());
    }

    @Test
    void shortestPathShouldSupportMixedTransport() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B"), node(3L, 101L, "C")), List.of(
                edgeWithTransport(11L, 101L, 1L, 2L, "80.00", null, "1.00", "walk", 0),
                edgeWithTransport(12L, 101L, 2L, 3L, "250.00", null, "1.00", "bike", 0),
                edgeWithTransport(13L, 101L, 1L, 3L, "400.00", null, "1.00", "walk", 0)));

        ShortestPathResult result = mapService.shortestPath(
                101L, 1L, 3L, "shortest_time", "mixed");

        assertEquals(List.of("walk", "bike"), result.getPathEdges().stream()
                .map(item -> item.getTransportType())
                .toList());
        assertEquals(new BigDecimal("2.00000000"), result.getEstimatedTime());
    }

    @Test
    void shortestPathShouldReportTransportRestrictedPath() {
        mockGraph(List.of(node(1L, 101L, "A"), node(2L, 101L, "B")), List.of(
                edgeWithTransport(11L, 101L, 1L, 2L, "100.00", null, "1.00", "bike", 0)));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> mapService.shortestPath(101L, 1L, 2L, "shortest_time", "walk"));

        assertEquals(ErrorCode.ROUTE_008, exception.getErrorCode());
    }

    private void mockGraph(List<MapNode> nodes, List<MapEdge> edges) {
        when(mapNodeMapper.selectList(anyNodeQuery())).thenReturn(nodes);
        when(mapEdgeMapper.selectList(anyEdgeQuery())).thenReturn(edges);
    }

    private LambdaQueryWrapper<MapNode> anyNodeQuery() {
        return any();
    }

    private LambdaQueryWrapper<MapEdge> anyEdgeQuery() {
        return any();
    }

    private MapNode node(Long id, Long destinationId, String name) {
        MapNode node = new MapNode();
        node.setId(id);
        node.setDestinationId(destinationId);
        node.setNodeName(name);
        node.setNodeType("intersection");
        return node;
    }

    private MapEdge edge(Long id, Long destinationId, Long fromNodeId, Long toNodeId, String distance, Integer bidirectional) {
        MapEdge edge = new MapEdge();
        edge.setId(id);
        edge.setDestinationId(destinationId);
        edge.setFromNodeId(fromNodeId);
        edge.setToNodeId(toNodeId);
        edge.setDistance(new BigDecimal(distance));
        edge.setIdealSpeed(new BigDecimal("80"));
        edge.setCrowdFactor(BigDecimal.ONE);
        edge.setTransportType("walk");
        edge.setBidirectionalFlag(bidirectional);
        return edge;
    }

    private MapEdge edgeWithTime(
            Long id,
            Long destinationId,
            Long fromNodeId,
            Long toNodeId,
            String distance,
            String idealSpeed,
            String crowdFactor,
            Integer bidirectional) {
        MapEdge edge = edge(id, destinationId, fromNodeId, toNodeId, distance, bidirectional);
        edge.setIdealSpeed(new BigDecimal(idealSpeed));
        edge.setCrowdFactor(new BigDecimal(crowdFactor));
        return edge;
    }

    private MapEdge edgeWithTransport(
            Long id,
            Long destinationId,
            Long fromNodeId,
            Long toNodeId,
            String distance,
            String idealSpeed,
            String crowdFactor,
            String transportType,
            Integer bidirectional) {
        MapEdge edge = edge(id, destinationId, fromNodeId, toNodeId, distance, bidirectional);
        edge.setIdealSpeed(idealSpeed == null ? null : new BigDecimal(idealSpeed));
        edge.setCrowdFactor(new BigDecimal(crowdFactor));
        edge.setTransportType(transportType);
        return edge;
    }
}
