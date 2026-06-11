package com.trip.engine.graph;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.springframework.stereotype.Component;

@Component
public class GraphEngine {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final String STRATEGY_SHORTEST_DISTANCE = "shortest_distance";
    public static final String STRATEGY_SHORTEST_TIME = "shortest_time";

    /**
     * 在非负权有向图上运行 Dijkstra，返回从起点到所有可达节点的最短路径树。
     */
    public ShortestPathTree shortestPaths(Graph graph, Long startNodeId, String strategyType) {
        Map<Long, WeightDistance> weights = new LinkedHashMap<>();
        Map<Long, BigDecimal> distances = new LinkedHashMap<>();
        Map<Long, PreviousStep> previousMap = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
                Comparator.comparing(NodeDistance::weight).thenComparing(NodeDistance::nodeId));

        weights.put(startNodeId, new WeightDistance(ZERO, ZERO));
        distances.put(startNodeId, ZERO);
        queue.add(new NodeDistance(startNodeId, ZERO));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            WeightDistance knownWeight = weights.get(current.nodeId());
            if (knownWeight == null || current.weight().compareTo(knownWeight.weight()) > 0) {
                continue;
            }

            for (GraphEdge edge : graph.adjacency().getOrDefault(current.nodeId(), List.of())) {
                if (STRATEGY_SHORTEST_TIME.equals(strategyType) && edge.timeCost() == null) {
                    continue;
                }
                BigDecimal edgeWeight = STRATEGY_SHORTEST_TIME.equals(strategyType) ? edge.timeCost() : edge.distance();
                BigDecimal nextWeight = current.weight().add(edgeWeight);
                BigDecimal oldWeight = weights.containsKey(edge.toNodeId()) ? weights.get(edge.toNodeId()).weight() : null;
                if (oldWeight == null || nextWeight.compareTo(oldWeight) < 0) {
                    BigDecimal nextDistance = distances.get(current.nodeId()).add(edge.distance());
                    BigDecimal nextTime = STRATEGY_SHORTEST_TIME.equals(strategyType)
                            ? knownWeight.time().add(edge.timeCost())
                            : nextDistance;
                    weights.put(edge.toNodeId(), new WeightDistance(nextWeight, nextTime));
                    distances.put(edge.toNodeId(), nextDistance);
                    previousMap.put(edge.toNodeId(), new PreviousStep(current.nodeId(), edge));
                    queue.add(new NodeDistance(edge.toNodeId(), nextWeight));
                }
            }
        }

        return new ShortestPathTree(weights, distances, previousMap);
    }

    public Long nearestTarget(Iterable<Long> targetNodeIds, Map<Long, WeightDistance> weights) {
        Long nearestNodeId = null;
        BigDecimal nearestWeight = null;
        for (Long targetNodeId : targetNodeIds) {
            WeightDistance weightDistance = weights.get(targetNodeId);
            if (weightDistance == null) {
                continue;
            }
            BigDecimal weight = weightDistance.weight();
            if (nearestWeight == null
                    || weight.compareTo(nearestWeight) < 0
                    || (weight.compareTo(nearestWeight) == 0 && targetNodeId < nearestNodeId)) {
                nearestNodeId = targetNodeId;
                nearestWeight = weight;
            }
        }
        return nearestNodeId;
    }

    public List<Long> backtrackNodeIds(Long startNodeId, Long targetNodeId, Map<Long, PreviousStep> previousMap) {
        List<Long> nodeIds = new ArrayList<>();
        Long currentNodeId = targetNodeId;
        nodeIds.add(currentNodeId);
        while (!startNodeId.equals(currentNodeId)) {
            PreviousStep previousStep = previousMap.get(currentNodeId);
            if (previousStep == null) {
                return List.of();
            }
            currentNodeId = previousStep.previousNodeId();
            nodeIds.add(currentNodeId);
        }
        Collections.reverse(nodeIds);
        return nodeIds;
    }

    public List<GraphEdge> backtrackEdges(Long startNodeId, Long targetNodeId, Map<Long, PreviousStep> previousMap) {
        if (startNodeId.equals(targetNodeId)) {
            return List.of();
        }

        List<GraphEdge> pathEdges = new ArrayList<>();
        Long currentNodeId = targetNodeId;
        while (!startNodeId.equals(currentNodeId)) {
            PreviousStep previousStep = previousMap.get(currentNodeId);
            if (previousStep == null) {
                return List.of();
            }
            pathEdges.add(previousStep.edge());
            currentNodeId = previousStep.previousNodeId();
        }
        Collections.reverse(pathEdges);
        return pathEdges;
    }

    private record NodeDistance(Long nodeId, BigDecimal weight) {
    }

    public record Graph(Map<Long, List<GraphEdge>> adjacency) {
    }

    public record GraphEdge(Long edgeId, Long fromNodeId, Long toNodeId, BigDecimal distance, BigDecimal timeCost) {
    }

    public record PreviousStep(Long previousNodeId, GraphEdge edge) {
    }

    public record ShortestPathTree(
            Map<Long, WeightDistance> weights,
            Map<Long, BigDecimal> distances,
            Map<Long, PreviousStep> previousMap) {
    }

    public record WeightDistance(BigDecimal weight, BigDecimal time) {
    }
}
