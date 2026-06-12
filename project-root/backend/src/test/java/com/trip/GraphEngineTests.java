package com.trip;

import com.trip.engine.graph.GraphEngine;
import com.trip.engine.graph.GraphEngine.Graph;
import com.trip.engine.graph.GraphEngine.GraphEdge;
import com.trip.engine.graph.GraphEngine.PathEdge;
import com.trip.engine.graph.GraphEngine.ShortestPathTree;
import com.trip.engine.graph.RouteConstraint;
import com.trip.model.route.EdgeTransportAccess;
import com.trip.model.route.RouteTransportType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GraphEngineTests {

    private final GraphEngine graphEngine = new GraphEngine();

    @Test
    void shortestPathsShouldUseDistanceWeightAndTransportFilter() {
        Graph graph = graph(Map.of(
                1L, List.of(
                        edge(11L, 1L, 2L, "2.00", "80", "1.00", EdgeTransportAccess.WALK),
                        edge(13L, 1L, 3L, "10.00", "80", "1.00", EdgeTransportAccess.WALK)),
                2L, List.of(edge(12L, 2L, 3L, "3.00", "80", "1.00", EdgeTransportAccess.WALK))));

        ShortestPathTree result = graphEngine.shortestPaths(
                graph,
                1L,
                constraint(GraphEngine.STRATEGY_SHORTEST_DISTANCE, RouteTransportType.WALK));

        assertEquals(new BigDecimal("5.00"), result.distances().get(3L));
        assertEquals(List.of(1L, 2L, 3L), graphEngine.backtrackNodeIds(1L, 3L, result));
        assertEquals(List.of(11L, 12L), graphEngine.backtrackEdges(1L, 3L, result)
                .stream()
                .map(PathEdge::edge)
                .map(GraphEdge::edgeId)
                .toList());
    }

    @Test
    void shortestTimeShouldUseTransportSpeedRoadLimitAndCrowdFactor() {
        Graph graph = graph(Map.of(
                1L, List.of(
                        edge(11L, 1L, 2L, "100.00", "80", "1.00", EdgeTransportAccess.WALK_BIKE),
                        edge(13L, 1L, 3L, "300.00", "300", "1.00", EdgeTransportAccess.WALK_BIKE)),
                2L, List.of(edge(12L, 2L, 3L, "100.00", "80", "1.00", EdgeTransportAccess.WALK_BIKE))));

        ShortestPathTree walk = graphEngine.shortestPaths(
                graph,
                1L,
                constraint(GraphEngine.STRATEGY_SHORTEST_TIME, RouteTransportType.WALK));
        ShortestPathTree bike = graphEngine.shortestPaths(
                graph,
                1L,
                constraint(GraphEngine.STRATEGY_SHORTEST_TIME, RouteTransportType.BIKE));

        assertEquals(List.of(1L, 2L, 3L), graphEngine.backtrackNodeIds(1L, 3L, walk));
        assertEquals(List.of(1L, 3L), graphEngine.backtrackNodeIds(1L, 3L, bike));
        assertEquals(new BigDecimal("1.20000000"), bike.weights().get(3L).time());
    }

    @Test
    void singleTransportShouldRejectInaccessibleEdges() {
        Graph graph = graph(Map.of(
                1L, List.of(edge(11L, 1L, 2L, "100.00", null, "1.00", EdgeTransportAccess.BIKE))));

        ShortestPathTree walk = graphEngine.shortestPaths(
                graph,
                1L,
                constraint(GraphEngine.STRATEGY_SHORTEST_TIME, RouteTransportType.WALK));
        ShortestPathTree bike = graphEngine.shortestPaths(
                graph,
                1L,
                constraint(GraphEngine.STRATEGY_SHORTEST_TIME, RouteTransportType.BIKE));

        assertFalse(walk.distances().containsKey(2L));
        assertEquals(new BigDecimal("0.40000000"), bike.weights().get(2L).time());
    }

    @Test
    void shortestTimeShouldSkipInvalidCrowdFactor() {
        Graph graph = graph(Map.of(
                1L, List.of(edge(11L, 1L, 2L, "100.00", "80", "0.00", EdgeTransportAccess.WALK))));

        ShortestPathTree result = graphEngine.shortestPaths(
                graph,
                1L,
                constraint(GraphEngine.STRATEGY_SHORTEST_TIME, RouteTransportType.WALK));

        assertFalse(result.distances().containsKey(2L));
    }

    @Test
    void mixedShouldChooseDifferentTransportForDifferentEdges() {
        Graph graph = graph(Map.of(
                1L, List.of(
                        edge(11L, 1L, 2L, "80.00", null, "1.00", EdgeTransportAccess.WALK),
                        edge(13L, 1L, 3L, "400.00", null, "1.00", EdgeTransportAccess.WALK)),
                2L, List.of(edge(12L, 2L, 3L, "250.00", null, "1.00", EdgeTransportAccess.BIKE))));

        ShortestPathTree result = graphEngine.shortestPaths(
                graph,
                1L,
                constraint(GraphEngine.STRATEGY_SHORTEST_TIME, RouteTransportType.MIXED));
        List<PathEdge> pathEdges = graphEngine.backtrackEdges(1L, 3L, result);

        assertEquals(List.of(1L, 2L, 3L), graphEngine.backtrackNodeIds(1L, 3L, result));
        assertEquals(
                List.of(RouteTransportType.WALK, RouteTransportType.BIKE),
                pathEdges.stream().map(PathEdge::transportType).toList());
        assertEquals(new BigDecimal("2.00000000"), result.weights().get(3L).time());
    }

    @Test
    void nearestTargetShouldIgnoreUnreachableAndUseNodeIdTieBreak() {
        Graph graph = graph(Map.of(
                1L, List.of(
                        edge(11L, 1L, 3L, "5.00", "80", "1.00", EdgeTransportAccess.WALK),
                        edge(12L, 1L, 2L, "5.00", "80", "1.00", EdgeTransportAccess.WALK))));

        ShortestPathTree result = graphEngine.shortestPaths(
                graph,
                1L,
                constraint(GraphEngine.STRATEGY_SHORTEST_DISTANCE, RouteTransportType.WALK));

        assertEquals(2L, graphEngine.nearestTarget(List.of(4L, 3L, 2L), result.weights()));
    }

    private RouteConstraint constraint(String strategyType, RouteTransportType transportType) {
        return new RouteConstraint(strategyType, transportType);
    }

    private Graph graph(Map<Long, List<GraphEdge>> adjacency) {
        return new Graph(adjacency);
    }

    private GraphEdge edge(
            Long id,
            Long fromNodeId,
            Long toNodeId,
            String distance,
            String idealSpeed,
            String crowdFactor,
            EdgeTransportAccess access) {
        return new GraphEdge(
                id,
                fromNodeId,
                toNodeId,
                new BigDecimal(distance),
                idealSpeed == null ? null : new BigDecimal(idealSpeed),
                new BigDecimal(crowdFactor),
                access);
    }
}
