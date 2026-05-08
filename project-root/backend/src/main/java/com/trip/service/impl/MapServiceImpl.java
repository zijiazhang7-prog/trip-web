package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.common.ErrorCode;
import com.trip.dto.map.MultiPathResult;
import com.trip.dto.map.PathEdgeResult;
import com.trip.dto.map.PathNodeResult;
import com.trip.dto.map.ShortestPathResult;
import com.trip.entity.MapEdge;
import com.trip.entity.MapNode;
import com.trip.exception.BusinessException;
import com.trip.mapper.MapEdgeMapper;
import com.trip.mapper.MapNodeMapper;
import com.trip.service.MapService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MapServiceImpl implements MapService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int BIDIRECTIONAL = 1;
    private static final String STRATEGY_SHORTEST_DISTANCE = "shortest_distance";
    private static final String STRATEGY_SHORTEST_TIME = "shortest_time";

    private final MapNodeMapper mapNodeMapper;
    private final MapEdgeMapper mapEdgeMapper;

    public MapServiceImpl(MapNodeMapper mapNodeMapper, MapEdgeMapper mapEdgeMapper) {
        this.mapNodeMapper = mapNodeMapper;
        this.mapEdgeMapper = mapEdgeMapper;
    }

    /**
     * 计算单目标最短距离路径。
     *
     * <p>数据结构：使用邻接表表示有向带权图，使用优先队列保存当前候选最短节点，使用哈希表保存距离和前驱。
     * 算法思想：在目的地子图内以 distance 为边权运行 Dijkstra，再从终点按前驱回溯路径。
     * 时间复杂度：建图 O(V + E)，最短路 O((V + E) log V)，回溯 O(P)。
     * 适用范围：非负边权的目的地内部单源单目标最短距离查询。</p>
     */
    @Override
    public ShortestPathResult shortestPath(Long destinationId, Long startNodeId, Long targetNodeId) {
        return shortestPath(destinationId, startNodeId, targetNodeId, STRATEGY_SHORTEST_DISTANCE);
    }

    @Override
    public ShortestPathResult shortestPath(Long destinationId, Long startNodeId, Long targetNodeId, String strategyType) {
        validateId(destinationId);
        validateId(startNodeId);
        validateId(targetNodeId);
        String normalizedStrategyType = normalizeStrategyType(strategyType);

        Graph graph = buildGraph(destinationId);
        MapNode startNode = graph.nodeMap().get(startNodeId);
        if (startNode == null) {
            throw new BusinessException(ErrorCode.ROUTE_001);
        }
        MapNode targetNode = graph.nodeMap().get(targetNodeId);
        if (targetNode == null) {
            throw new BusinessException(ErrorCode.ROUTE_002);
        }
        if (!destinationId.equals(startNode.getDestinationId()) || !destinationId.equals(targetNode.getDestinationId())) {
            throw new BusinessException(ErrorCode.ROUTE_009);
        }

        DijkstraResult dijkstraResult = dijkstra(graph, startNodeId, normalizedStrategyType);
        WeightDistance totalWeight = dijkstraResult.weights().get(targetNodeId);
        BigDecimal totalDistance = dijkstraResult.distances().get(targetNodeId);
        if (totalDistance == null) {
            throw new BusinessException(ErrorCode.ROUTE_003);
        }

        ShortestPathResult result = new ShortestPathResult();
        result.setDestinationId(destinationId);
        result.setStartNodeId(startNodeId);
        result.setTargetNodeId(targetNodeId);
        result.setTotalDistance(totalDistance);
        result.setEstimatedTime(totalWeight == null ? ZERO : totalWeight.time());
        result.setPathNodes(buildPathNodes(graph, startNodeId, targetNodeId, dijkstraResult.previousMap()));
        result.setPathEdges(buildPathEdges(startNodeId, targetNodeId, dijkstraResult.previousMap()));
        return result;
    }

    /**
     * 计算多目标最近邻启发式路径。
     *
     * <p>数据结构：复用目的地内部有向带权图邻接表，使用集合保存未访问目标点，使用列表拼接路径结果。
     * 算法思想：从当前节点出发运行 Dijkstra，选择未访问目标中当前最短的一点作为下一站，追加该段路径后继续；
     * 如需要回到起点，则最后再追加一段当前点到起点的最短路径。
     * 时间复杂度：目标点数量为 k 时最多运行 k+1 次 Dijkstra，约 O(k * (V + E) log V)。
     * 适用范围：目标点数量较少的景区 / 校园多点游览路线演示；该启发式不保证 TSP 全局最优。</p>
     */
    @Override
    public MultiPathResult multiTargetPath(
            Long destinationId,
            Long startNodeId,
            List<Long> targetNodeIds,
            boolean returnToStart) {
        return multiTargetPath(destinationId, startNodeId, targetNodeIds, returnToStart, STRATEGY_SHORTEST_DISTANCE);
    }

    @Override
    public MultiPathResult multiTargetPath(
            Long destinationId,
            Long startNodeId,
            List<Long> targetNodeIds,
            boolean returnToStart,
            String strategyType) {
        validateId(destinationId);
        validateId(startNodeId);
        validateTargetNodeIds(targetNodeIds);
        String normalizedStrategyType = normalizeStrategyType(strategyType);

        Graph graph = buildGraph(destinationId);
        if (!graph.nodeMap().containsKey(startNodeId)) {
            throw new BusinessException(ErrorCode.ROUTE_001);
        }
        for (Long targetNodeId : targetNodeIds) {
            MapNode targetNode = graph.nodeMap().get(targetNodeId);
            if (targetNode == null) {
                throw new BusinessException(ErrorCode.ROUTE_002);
            }
            if (!destinationId.equals(targetNode.getDestinationId())) {
                throw new BusinessException(ErrorCode.ROUTE_009);
            }
        }

        Set<Long> unvisitedTargetIds = new LinkedHashSet<>(targetNodeIds);
        List<Long> orderedTargetIds = new ArrayList<>();
        List<PathNodeResult> allPathNodes = new ArrayList<>();
        List<PathEdgeResult> allPathEdges = new ArrayList<>();
        BigDecimal totalDistance = ZERO;
        BigDecimal totalEstimatedTime = ZERO;
        Long currentNodeId = startNodeId;

        while (!unvisitedTargetIds.isEmpty()) {
            DijkstraResult dijkstraResult = dijkstra(graph, currentNodeId, normalizedStrategyType);
            Long nextTargetId = nearestTarget(unvisitedTargetIds, dijkstraResult.weights());
            if (nextTargetId == null) {
                throw new BusinessException(ErrorCode.ROUTE_003);
            }
            ShortestPathResult segment = buildSegment(graph, currentNodeId, nextTargetId, dijkstraResult);
            appendSegment(allPathNodes, allPathEdges, segment);
            totalDistance = totalDistance.add(segment.getTotalDistance());
            totalEstimatedTime = totalEstimatedTime.add(segment.getEstimatedTime() == null ? ZERO : segment.getEstimatedTime());
            orderedTargetIds.add(nextTargetId);
            unvisitedTargetIds.remove(nextTargetId);
            currentNodeId = nextTargetId;
        }

        if (returnToStart && !currentNodeId.equals(startNodeId)) {
            DijkstraResult dijkstraResult = dijkstra(graph, currentNodeId, normalizedStrategyType);
            BigDecimal returnDistance = dijkstraResult.distances().get(startNodeId);
            if (returnDistance == null) {
                throw new BusinessException(ErrorCode.ROUTE_003);
            }
            ShortestPathResult segment = buildSegment(graph, currentNodeId, startNodeId, dijkstraResult);
            appendSegment(allPathNodes, allPathEdges, segment);
            totalDistance = totalDistance.add(segment.getTotalDistance());
            totalEstimatedTime = totalEstimatedTime.add(segment.getEstimatedTime() == null ? ZERO : segment.getEstimatedTime());
            currentNodeId = startNodeId;
        }

        MultiPathResult result = new MultiPathResult();
        result.setDestinationId(destinationId);
        result.setStartNodeId(startNodeId);
        result.setEndNodeId(currentNodeId);
        result.setOrderedTargetNodeIds(orderedTargetIds);
        result.setTotalDistance(totalDistance);
        result.setEstimatedTime(totalEstimatedTime);
        result.setPathNodes(allPathNodes);
        result.setPathEdges(allPathEdges);
        return result;
    }

    /**
     * 计算从起点出发到同一目的地下所有可达节点的最短距离。
     *
     * <p>该方法为 Facility 的“图上可达距离排序”打底。它只返回可达节点距离，不把不可达节点放入结果。</p>
     */
    @Override
    public Map<Long, BigDecimal> shortestDistances(Long destinationId, Long startNodeId) {
        validateId(destinationId);
        validateId(startNodeId);

        Graph graph = buildGraph(destinationId);
        if (!graph.nodeMap().containsKey(startNodeId)) {
            throw new BusinessException(ErrorCode.ROUTE_001);
        }
        return dijkstra(graph, startNodeId, STRATEGY_SHORTEST_DISTANCE).distances();
    }

    private Graph buildGraph(Long destinationId) {
        List<MapNode> nodes = mapNodeMapper.selectList(new LambdaQueryWrapper<MapNode>()
                .eq(MapNode::getDestinationId, destinationId));
        List<MapEdge> edges = mapEdgeMapper.selectList(new LambdaQueryWrapper<MapEdge>()
                .eq(MapEdge::getDestinationId, destinationId));

        Map<Long, MapNode> nodeMap = new HashMap<>();
        for (MapNode node : nodes) {
            if (node.getId() != null) {
                nodeMap.put(node.getId(), node);
            }
        }

        Map<Long, List<GraphEdge>> adjacency = new HashMap<>();
        for (MapEdge edge : edges) {
            addDirectedEdge(nodeMap, adjacency, edge, edge.getFromNodeId(), edge.getToNodeId());
            if (Integer.valueOf(BIDIRECTIONAL).equals(edge.getBidirectionalFlag())) {
                addDirectedEdge(nodeMap, adjacency, edge, edge.getToNodeId(), edge.getFromNodeId());
            }
        }

        return new Graph(nodeMap, adjacency);
    }

    private void addDirectedEdge(
            Map<Long, MapNode> nodeMap,
            Map<Long, List<GraphEdge>> adjacency,
            MapEdge edge,
            Long fromNodeId,
            Long toNodeId) {
        if (fromNodeId == null
                || toNodeId == null
                || !nodeMap.containsKey(fromNodeId)
                || !nodeMap.containsKey(toNodeId)
                || edge.getDistance() == null
                || edge.getDistance().compareTo(ZERO) <= 0) {
            return;
        }
        BigDecimal idealSpeed = edge.getIdealSpeed();
        BigDecimal crowdFactor = edge.getCrowdFactor();
        BigDecimal timeCost = null;
        if (idealSpeed != null
                && crowdFactor != null
                && idealSpeed.compareTo(ZERO) > 0
                && crowdFactor.compareTo(ZERO) > 0) {
            timeCost = edge.getDistance().divide(idealSpeed.multiply(crowdFactor), 8, java.math.RoundingMode.HALF_UP);
        }
        adjacency.computeIfAbsent(fromNodeId, key -> new ArrayList<>())
                .add(new GraphEdge(edge.getId(), fromNodeId, toNodeId, edge.getDistance(), timeCost));
    }

    private DijkstraResult dijkstra(Graph graph, Long startNodeId, String strategyType) {
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

        return new DijkstraResult(weights, distances, previousMap);
    }

    private Long nearestTarget(Set<Long> targetNodeIds, Map<Long, WeightDistance> weights) {
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

    private ShortestPathResult buildSegment(
            Graph graph,
            Long startNodeId,
            Long targetNodeId,
            DijkstraResult dijkstraResult) {
        BigDecimal totalDistance = dijkstraResult.distances().get(targetNodeId);
        if (totalDistance == null) {
            throw new BusinessException(ErrorCode.ROUTE_003);
        }

        ShortestPathResult result = new ShortestPathResult();
        result.setDestinationId(graph.nodeMap().get(startNodeId).getDestinationId());
        result.setStartNodeId(startNodeId);
        result.setTargetNodeId(targetNodeId);
        result.setTotalDistance(totalDistance);
        WeightDistance totalWeight = dijkstraResult.weights().get(targetNodeId);
        result.setEstimatedTime(totalWeight == null ? ZERO : totalWeight.time());
        result.setPathNodes(buildPathNodes(graph, startNodeId, targetNodeId, dijkstraResult.previousMap()));
        result.setPathEdges(buildPathEdges(startNodeId, targetNodeId, dijkstraResult.previousMap()));
        return result;
    }

    private void appendSegment(
            List<PathNodeResult> allPathNodes,
            List<PathEdgeResult> allPathEdges,
            ShortestPathResult segment) {
        List<PathNodeResult> segmentNodes = segment.getPathNodes();
        if (segmentNodes != null && !segmentNodes.isEmpty()) {
            int fromIndex = allPathNodes.isEmpty() ? 0 : 1;
            for (int index = fromIndex; index < segmentNodes.size(); index++) {
                allPathNodes.add(segmentNodes.get(index));
            }
        }
        if (segment.getPathEdges() != null) {
            allPathEdges.addAll(segment.getPathEdges());
        }
    }

    private List<PathNodeResult> buildPathNodes(
            Graph graph,
            Long startNodeId,
            Long targetNodeId,
            Map<Long, PreviousStep> previousMap) {
        List<Long> nodeIds = backtrackNodeIds(startNodeId, targetNodeId, previousMap);
        List<PathNodeResult> pathNodes = new ArrayList<>();
        for (Long nodeId : nodeIds) {
            MapNode node = graph.nodeMap().get(nodeId);
            pathNodes.add(new PathNodeResult(nodeId, node == null ? null : node.getNodeName()));
        }
        return pathNodes;
    }

    private List<PathEdgeResult> buildPathEdges(
            Long startNodeId,
            Long targetNodeId,
            Map<Long, PreviousStep> previousMap) {
        if (startNodeId.equals(targetNodeId)) {
            return List.of();
        }

        List<PathEdgeResult> pathEdges = new ArrayList<>();
        Long currentNodeId = targetNodeId;
        while (!startNodeId.equals(currentNodeId)) {
            PreviousStep previousStep = previousMap.get(currentNodeId);
            if (previousStep == null) {
                throw new BusinessException(ErrorCode.ROUTE_003);
            }
            GraphEdge edge = previousStep.edge();
            pathEdges.add(new PathEdgeResult(edge.edgeId(), edge.fromNodeId(), edge.toNodeId(), edge.distance()));
            currentNodeId = previousStep.previousNodeId();
        }
        Collections.reverse(pathEdges);
        return pathEdges;
    }

    private List<Long> backtrackNodeIds(
            Long startNodeId,
            Long targetNodeId,
            Map<Long, PreviousStep> previousMap) {
        List<Long> nodeIds = new ArrayList<>();
        Long currentNodeId = targetNodeId;
        nodeIds.add(currentNodeId);
        while (!startNodeId.equals(currentNodeId)) {
            PreviousStep previousStep = previousMap.get(currentNodeId);
            if (previousStep == null) {
                throw new BusinessException(ErrorCode.ROUTE_003);
            }
            currentNodeId = previousStep.previousNodeId();
            nodeIds.add(currentNodeId);
        }
        Collections.reverse(nodeIds);
        return nodeIds;
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
    }

    private void validateTargetNodeIds(List<Long> targetNodeIds) {
        if (targetNodeIds == null || targetNodeIds.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Set<Long> uniqueNodeIds = new LinkedHashSet<>();
        for (Long targetNodeId : targetNodeIds) {
            validateId(targetNodeId);
            if (!uniqueNodeIds.add(targetNodeId)) {
                throw new BusinessException(ErrorCode.COMMON_001);
            }
        }
    }

    private String normalizeStrategyType(String strategyType) {
        if (STRATEGY_SHORTEST_TIME.equals(strategyType)) {
            return STRATEGY_SHORTEST_TIME;
        }
        return STRATEGY_SHORTEST_DISTANCE;
    }

    private record Graph(Map<Long, MapNode> nodeMap, Map<Long, List<GraphEdge>> adjacency) {
    }

    private record GraphEdge(Long edgeId, Long fromNodeId, Long toNodeId, BigDecimal distance, BigDecimal timeCost) {
    }

    private record PreviousStep(Long previousNodeId, GraphEdge edge) {
    }

    private record DijkstraResult(
            Map<Long, WeightDistance> weights,
            Map<Long, BigDecimal> distances,
            Map<Long, PreviousStep> previousMap) {
    }

    private record WeightDistance(BigDecimal weight, BigDecimal time) {
    }

    private record NodeDistance(Long nodeId, BigDecimal weight) {
    }
}
