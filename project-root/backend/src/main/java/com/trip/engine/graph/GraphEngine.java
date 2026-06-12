package com.trip.engine.graph;

import com.trip.model.route.EdgeTransportAccess;
import com.trip.model.route.RouteTransportType;
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
    private final EdgeCostCalculator edgeCostCalculator;

    public GraphEngine() {
        this(new EdgeCostCalculator());
    }

    public GraphEngine(EdgeCostCalculator edgeCostCalculator) {
        this.edgeCostCalculator = edgeCostCalculator;
    }

    /**
     * 在非负权有向图上运行 Dijkstra，返回从起点到所有可达节点的最短路径树。
     */
    public ShortestPathTree shortestPaths(Graph graph, Long startNodeId, RouteConstraint constraint) {
        Map<PathState, WeightDistance> stateWeights = new HashMap<>();
        Map<PathState, BigDecimal> stateDistances = new HashMap<>();
        Map<PathState, PreviousStep> previousMap = new HashMap<>();
        PriorityQueue<StateDistance> queue = new PriorityQueue<>(
                Comparator.comparing(StateDistance::weight)
                        .thenComparing(item -> item.state().nodeId())
                        .thenComparing(item -> item.state().transportType().value()));

        for (RouteTransportType initialType : availableTypes(constraint.transportType())) {
            PathState state = new PathState(startNodeId, initialType);
            stateWeights.put(state, new WeightDistance(ZERO, ZERO));
            stateDistances.put(state, ZERO);
            queue.add(new StateDistance(state, ZERO));
        }

        while (!queue.isEmpty()) {
            StateDistance current = queue.poll();
            WeightDistance knownWeight = stateWeights.get(current.state());
            if (knownWeight == null || current.weight().compareTo(knownWeight.weight()) > 0) {
                continue;
            }

            for (GraphEdge edge : graph.adjacency().getOrDefault(current.state().nodeId(), List.of())) {
                for (RouteTransportType actualType : transitionTypes(edge, constraint.transportType())) {
                    BigDecimal timeCost = edgeCostCalculator.timeCost(edge, actualType);
                    BigDecimal edgeWeight = STRATEGY_SHORTEST_TIME.equals(constraint.strategyType())
                            ? timeCost
                            : edgeCostCalculator.distanceCost(edge);
                    if (edgeWeight == null || timeCost == null) {
                        continue;
                    }

                    PathState nextState = new PathState(edge.toNodeId(), actualType);
                    BigDecimal nextWeight = current.weight().add(edgeWeight);
                    WeightDistance oldWeightDistance = stateWeights.get(nextState);
                    if (oldWeightDistance == null || nextWeight.compareTo(oldWeightDistance.weight()) < 0) {
                        BigDecimal nextDistance = stateDistances.get(current.state()).add(edge.distance());
                        BigDecimal nextTime = knownWeight.time().add(timeCost);
                        stateWeights.put(nextState, new WeightDistance(nextWeight, nextTime));
                        stateDistances.put(nextState, nextDistance);
                        previousMap.put(nextState, new PreviousStep(current.state(), edge, actualType));
                        queue.add(new StateDistance(nextState, nextWeight));
                    }
                }
            }
        }

        return collapseStates(stateWeights, stateDistances, previousMap);
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

    public List<Long> backtrackNodeIds(Long startNodeId, Long targetNodeId, ShortestPathTree tree) {
        List<Long> nodeIds = new ArrayList<>();
        PathState currentState = tree.bestStates().get(targetNodeId);
        if (currentState == null) {
            return List.of();
        }
        nodeIds.add(currentState.nodeId());
        while (!startNodeId.equals(currentState.nodeId())) {
            PreviousStep previousStep = tree.previousMap().get(currentState);
            if (previousStep == null) {
                return List.of();
            }
            currentState = previousStep.previousState();
            nodeIds.add(currentState.nodeId());
        }
        Collections.reverse(nodeIds);
        return nodeIds;
    }

    public List<PathEdge> backtrackEdges(Long startNodeId, Long targetNodeId, ShortestPathTree tree) {
        if (startNodeId.equals(targetNodeId)) {
            return List.of();
        }

        List<PathEdge> pathEdges = new ArrayList<>();
        PathState currentState = tree.bestStates().get(targetNodeId);
        if (currentState == null) {
            return List.of();
        }
        while (!startNodeId.equals(currentState.nodeId())) {
            PreviousStep previousStep = tree.previousMap().get(currentState);
            if (previousStep == null) {
                return List.of();
            }
            pathEdges.add(new PathEdge(previousStep.edge(), previousStep.transportType()));
            currentState = previousStep.previousState();
        }
        Collections.reverse(pathEdges);
        return pathEdges;
    }

    private List<RouteTransportType> availableTypes(RouteTransportType requestedType) {
        if (RouteTransportType.MIXED.equals(requestedType)) {
            return List.of(RouteTransportType.WALK, RouteTransportType.BIKE, RouteTransportType.CART);
        }
        return List.of(requestedType);
    }

    private List<RouteTransportType> transitionTypes(GraphEdge edge, RouteTransportType requestedType) {
        if (!RouteTransportType.MIXED.equals(requestedType)) {
            return edgeCostCalculator.isAccessible(edge, requestedType) ? List.of(requestedType) : List.of();
        }
        return edge.transportAccess().allowedTypes().stream()
                .sorted(Comparator.comparing(RouteTransportType::value))
                .toList();
    }

    private ShortestPathTree collapseStates(
            Map<PathState, WeightDistance> stateWeights,
            Map<PathState, BigDecimal> stateDistances,
            Map<PathState, PreviousStep> previousMap) {
        Map<Long, WeightDistance> weights = new LinkedHashMap<>();
        Map<Long, BigDecimal> distances = new LinkedHashMap<>();
        Map<Long, PathState> bestStates = new HashMap<>();
        for (Map.Entry<PathState, WeightDistance> entry : stateWeights.entrySet()) {
            PathState state = entry.getKey();
            WeightDistance candidate = entry.getValue();
            WeightDistance current = weights.get(state.nodeId());
            PathState currentState = bestStates.get(state.nodeId());
            if (current == null
                    || candidate.weight().compareTo(current.weight()) < 0
                    || (candidate.weight().compareTo(current.weight()) == 0
                    && state.transportType().value().compareTo(currentState.transportType().value()) < 0)) {
                weights.put(state.nodeId(), candidate);
                distances.put(state.nodeId(), stateDistances.get(state));
                bestStates.put(state.nodeId(), state);
            }
        }
        return new ShortestPathTree(weights, distances, bestStates, previousMap);
    }

    private record StateDistance(PathState state, BigDecimal weight) {
    }

    public record Graph(Map<Long, List<GraphEdge>> adjacency) {
    }

    public record GraphEdge(
            Long edgeId,
            Long fromNodeId,
            Long toNodeId,
            BigDecimal distance,
            BigDecimal idealSpeed,
            BigDecimal crowdFactor,
            EdgeTransportAccess transportAccess) {
    }

    public record PathState(Long nodeId, RouteTransportType transportType) {
    }

    public record PathEdge(GraphEdge edge, RouteTransportType transportType) {
    }

    public record PreviousStep(PathState previousState, GraphEdge edge, RouteTransportType transportType) {
    }

    public record ShortestPathTree(
            Map<Long, WeightDistance> weights,
            Map<Long, BigDecimal> distances,
            Map<Long, PathState> bestStates,
            Map<PathState, PreviousStep> previousMap) {
    }

    public record WeightDistance(BigDecimal weight, BigDecimal time) {
    }
}
