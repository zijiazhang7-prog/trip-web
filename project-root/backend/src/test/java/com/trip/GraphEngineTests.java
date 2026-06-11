package com.trip;

import com.trip.engine.graph.GraphEngine;
import com.trip.engine.graph.GraphEngine.Graph;
import com.trip.engine.graph.GraphEngine.GraphEdge;
import com.trip.engine.graph.GraphEngine.ShortestPathTree;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GraphEngineTests {

    private final GraphEngine graphEngine = new GraphEngine();

    @Test
    void shortestPathsShouldUseDistanceWeight() {
        Graph graph = graph(Map.of(
                1L, List.of(edge(11L, 1L, 2L, "2.00", null), edge(13L, 1L, 3L, "10.00", null)),
                2L, List.of(edge(12L, 2L, 3L, "3.00", null))));

        ShortestPathTree result = graphEngine.shortestPaths(graph, 1L, GraphEngine.STRATEGY_SHORTEST_DISTANCE);

        assertEquals(new BigDecimal("5.00"), result.distances().get(3L));
        assertEquals(List.of(1L, 2L, 3L), graphEngine.backtrackNodeIds(1L, 3L, result.previousMap()));
        assertEquals(List.of(11L, 12L), graphEngine.backtrackEdges(1L, 3L, result.previousMap())
                .stream()
                .map(GraphEdge::edgeId)
                .toList());
    }

    @Test
    void shortestPathsShouldUseTimeWeightAndKeepDistanceTotal() {
        Graph graph = graph(Map.of(
                1L, List.of(
                        edge(11L, 1L, 2L, "100.00", "10.00000000"),
                        edge(13L, 1L, 3L, "300.00", "3.00000000")),
                2L, List.of(edge(12L, 2L, 3L, "100.00", "10.00000000"))));

        ShortestPathTree result = graphEngine.shortestPaths(graph, 1L, GraphEngine.STRATEGY_SHORTEST_TIME);

        assertEquals(new BigDecimal("300.00"), result.distances().get(3L));
        assertEquals(new BigDecimal("3.00000000"), result.weights().get(3L).time());
        assertEquals(List.of(1L, 3L), graphEngine.backtrackNodeIds(1L, 3L, result.previousMap()));
    }

    @Test
    void shortestPathsShouldSkipInvalidTimeEdgeForTimeStrategy() {
        Graph graph = graph(Map.of(1L, List.of(edge(11L, 1L, 2L, "100.00", null))));

        ShortestPathTree result = graphEngine.shortestPaths(graph, 1L, GraphEngine.STRATEGY_SHORTEST_TIME);

        assertFalse(result.distances().containsKey(2L));
        assertEquals(List.of(), graphEngine.backtrackNodeIds(1L, 2L, result.previousMap()));
    }

    @Test
    void nearestTargetShouldIgnoreUnreachableAndUseNodeIdTieBreak() {
        Graph graph = graph(Map.of(
                1L, List.of(edge(11L, 1L, 3L, "5.00", null), edge(12L, 1L, 2L, "5.00", null))));

        ShortestPathTree result = graphEngine.shortestPaths(graph, 1L, GraphEngine.STRATEGY_SHORTEST_DISTANCE);

        assertEquals(2L, graphEngine.nearestTarget(List.of(4L, 3L, 2L), result.weights()));
    }

    @Test
    void shortestPathsShouldReturnStartOnlyWhenStartEqualsTarget() {
        Graph graph = graph(Map.of());

        ShortestPathTree result = graphEngine.shortestPaths(graph, 1L, GraphEngine.STRATEGY_SHORTEST_DISTANCE);

        assertEquals(new BigDecimal("0"), result.distances().get(1L));
        assertEquals(List.of(1L), graphEngine.backtrackNodeIds(1L, 1L, result.previousMap()));
        assertEquals(List.of(), graphEngine.backtrackEdges(1L, 1L, result.previousMap()));
    }

    private Graph graph(Map<Long, List<GraphEdge>> adjacency) {
        return new Graph(adjacency);
    }

    private GraphEdge edge(Long id, Long fromNodeId, Long toNodeId, String distance, String timeCost) {
        return new GraphEdge(
                id,
                fromNodeId,
                toNodeId,
                new BigDecimal(distance),
                timeCost == null ? null : new BigDecimal(timeCost));
    }
}
