package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.common.ErrorCode;
import com.trip.dto.map.IndoorPathEdgeResult;
import com.trip.dto.map.IndoorPathNodeResult;
import com.trip.dto.map.IndoorPathResult;
import com.trip.dto.request.IndoorRoutePlanRequest;
import com.trip.entity.MapEdge;
import com.trip.entity.MapNode;
import com.trip.entity.Place;
import com.trip.exception.BusinessException;
import com.trip.mapper.MapEdgeMapper;
import com.trip.mapper.MapNodeMapper;
import com.trip.mapper.PlaceMapper;
import com.trip.model.route.IndoorVerticalMode;
import com.trip.service.IndoorRouteService;
import com.trip.service.MapService;
import com.trip.vo.response.IndoorBuildingVO;
import com.trip.vo.response.IndoorEdgeVO;
import com.trip.vo.response.IndoorMapVO;
import com.trip.vo.response.IndoorNodeVO;
import com.trip.vo.response.IndoorRoutePlanVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 室内导航业务编排，不直接实现图算法。
 */
@Service
public class IndoorRouteServiceImpl implements IndoorRouteService {

    private static final String DEFAULT_STRATEGY = "shortest_time";
    private static final String TIME_UNIT_SECOND = "second";
    private static final BigDecimal SECONDS_PER_MINUTE = new BigDecimal("60");

    private final PlaceMapper placeMapper;
    private final MapNodeMapper mapNodeMapper;
    private final MapEdgeMapper mapEdgeMapper;
    private final MapService mapService;

    public IndoorRouteServiceImpl(
            PlaceMapper placeMapper,
            MapNodeMapper mapNodeMapper,
            MapEdgeMapper mapEdgeMapper,
            MapService mapService) {
        this.placeMapper = placeMapper;
        this.mapNodeMapper = mapNodeMapper;
        this.mapEdgeMapper = mapEdgeMapper;
        this.mapService = mapService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndoorBuildingVO> listBuildings(Long destinationId) {
        requirePositive(destinationId);
        List<MapNode> nodes = mapNodeMapper.selectList(new LambdaQueryWrapper<MapNode>()
                .eq(MapNode::getDestinationId, destinationId)
                .isNotNull(MapNode::getPlaceId)
                .isNotNull(MapNode::getFloorNo)
                .orderByAsc(MapNode::getPlaceId)
                .orderByAsc(MapNode::getFloorNo)
                .orderByAsc(MapNode::getId));
        if (nodes.isEmpty()) {
            return List.of();
        }

        Map<Long, Set<Integer>> floorsByBuilding = new LinkedHashMap<>();
        for (MapNode node : nodes) {
            floorsByBuilding.computeIfAbsent(node.getPlaceId(), key -> new LinkedHashSet<>())
                    .add(node.getFloorNo());
        }
        Map<Long, Place> places = placesById(floorsByBuilding.keySet());
        List<IndoorBuildingVO> results = new ArrayList<>();
        for (Map.Entry<Long, Set<Integer>> entry : floorsByBuilding.entrySet()) {
            Place place = places.get(entry.getKey());
            if (place != null && destinationId.equals(place.getDestinationId())) {
                results.add(toBuildingVO(place, entry.getValue()));
            }
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public IndoorMapVO getBuildingMap(Long buildingId) {
        Place building = requireBuilding(buildingId, null);
        List<MapNode> nodes = buildingNodes(building);
        if (nodes.isEmpty()) {
            throw new BusinessException(ErrorCode.ROUTE_011);
        }
        Map<Long, MapNode> nodeMap = nodesById(nodes);

        IndoorMapVO vo = new IndoorMapVO();
        vo.setBuilding(toBuildingVO(building, floors(nodes)));
        vo.setNodes(nodes.stream().map(IndoorNodeVO::from).toList());
        vo.setEdges(buildingEdges(building.getDestinationId(), nodeMap).stream()
                .map(edge -> toIndoorEdgeVO(edge, nodeMap))
                .toList());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public IndoorRoutePlanVO planRoute(IndoorRoutePlanRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Place building = requireBuilding(request.getBuildingId(), request.getDestinationId());
        String strategyType = request.getStrategyType() == null
                ? DEFAULT_STRATEGY
                : request.getStrategyType();
        String verticalMode = request.getVerticalMode() == null
                ? IndoorVerticalMode.ANY.value()
                : request.getVerticalMode();

        IndoorPathResult path = mapService.indoorShortestPath(
                request.getDestinationId(),
                request.getBuildingId(),
                request.getStartNodeId(),
                request.getTargetNodeId(),
                strategyType,
                verticalMode);
        return toRoutePlanVO(building, strategyType, verticalMode, path);
    }

    private IndoorRoutePlanVO toRoutePlanVO(
            Place building,
            String strategyType,
            String verticalMode,
            IndoorPathResult path) {
        Map<Long, IndoorPathNodeResult> nodesById = new HashMap<>();
        List<IndoorNodeVO> pathNodes = new ArrayList<>();
        for (IndoorPathNodeResult node : path.getPathNodes()) {
            nodesById.put(node.getNodeId(), node);
            pathNodes.add(toIndoorNodeVO(node));
        }

        List<IndoorEdgeVO> pathEdges = new ArrayList<>();
        List<String> steps = new ArrayList<>();
        for (IndoorPathEdgeResult edge : path.getPathEdges()) {
            IndoorPathNodeResult fromNode = nodesById.get(edge.getFromNodeId());
            IndoorPathNodeResult toNode = nodesById.get(edge.getToNodeId());
            pathEdges.add(toIndoorEdgeVO(edge, fromNode, toNode));
            steps.add(routeStep(edge, fromNode, toNode));
        }

        IndoorRoutePlanVO vo = new IndoorRoutePlanVO();
        vo.setDestinationId(path.getDestinationId());
        vo.setBuildingId(path.getBuildingId());
        vo.setBuildingName(building.getName());
        vo.setStrategyType(strategyType);
        vo.setVerticalMode(verticalMode);
        vo.setTotalDistance(path.getTotalDistance());
        vo.setTotalTime(toSeconds(path.getEstimatedTime()));
        vo.setTimeUnit(TIME_UNIT_SECOND);
        vo.setPathNodes(pathNodes);
        vo.setPathEdges(pathEdges);
        vo.setSteps(steps);
        return vo;
    }

    private IndoorNodeVO toIndoorNodeVO(IndoorPathNodeResult node) {
        IndoorNodeVO vo = new IndoorNodeVO();
        vo.setNodeId(node.getNodeId());
        vo.setNodeName(node.getNodeName());
        vo.setNodeType(node.getNodeType());
        vo.setFloorNo(node.getFloorNo());
        vo.setX(node.getIndoorX());
        vo.setY(node.getIndoorY());
        return vo;
    }

    private IndoorEdgeVO toIndoorEdgeVO(
            IndoorPathEdgeResult edge,
            IndoorPathNodeResult fromNode,
            IndoorPathNodeResult toNode) {
        IndoorEdgeVO vo = new IndoorEdgeVO();
        vo.setEdgeId(edge.getEdgeId());
        vo.setFromNodeId(edge.getFromNodeId());
        vo.setToNodeId(edge.getToNodeId());
        vo.setFromNodeName(fromNode == null ? null : fromNode.getNodeName());
        vo.setToNodeName(toNode == null ? null : toNode.getNodeName());
        vo.setFromFloorNo(fromNode == null ? null : fromNode.getFloorNo());
        vo.setToFloorNo(toNode == null ? null : toNode.getFloorNo());
        vo.setEdgeType(edge.getEdgeType());
        vo.setDistance(edge.getDistance());
        vo.setTimeCost(toSeconds(edge.getTimeCost()));
        return vo;
    }

    private IndoorEdgeVO toIndoorEdgeVO(MapEdge edge, Map<Long, MapNode> nodeMap) {
        MapNode fromNode = nodeMap.get(edge.getFromNodeId());
        MapNode toNode = nodeMap.get(edge.getToNodeId());
        IndoorEdgeVO vo = new IndoorEdgeVO();
        vo.setEdgeId(edge.getId());
        vo.setFromNodeId(edge.getFromNodeId());
        vo.setToNodeId(edge.getToNodeId());
        vo.setFromNodeName(fromNode == null ? null : fromNode.getNodeName());
        vo.setToNodeName(toNode == null ? null : toNode.getNodeName());
        vo.setFromFloorNo(fromNode == null ? null : fromNode.getFloorNo());
        vo.setToFloorNo(toNode == null ? null : toNode.getFloorNo());
        vo.setEdgeType(edge.getEdgeType());
        vo.setDistance(edge.getDistance());
        return vo;
    }

    private String routeStep(
            IndoorPathEdgeResult edge,
            IndoorPathNodeResult fromNode,
            IndoorPathNodeResult toNode) {
        String fromName = fromNode == null ? String.valueOf(edge.getFromNodeId()) : fromNode.getNodeName();
        String toName = toNode == null ? String.valueOf(edge.getToNodeId()) : toNode.getNodeName();
        if ("elevator".equals(edge.getEdgeType())) {
            return "乘电梯从" + floorLabel(fromNode) + "到" + floorLabel(toNode);
        }
        if ("stair".equals(edge.getEdgeType())) {
            return "经楼梯从" + floorLabel(fromNode) + "到" + floorLabel(toNode);
        }
        return "从" + fromName + "沿走廊前往" + toName + "，约" + distanceLabel(edge.getDistance()) + "米";
    }

    private String floorLabel(IndoorPathNodeResult node) {
        return node == null || node.getFloorNo() == null ? "未知楼层" : node.getFloorNo() + "层";
    }

    private String distanceLabel(BigDecimal distance) {
        return distance == null ? "0" : distance.stripTrailingZeros().toPlainString();
    }

    private Integer toSeconds(BigDecimal minutes) {
        if (minutes == null || minutes.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return minutes.multiply(SECONDS_PER_MINUTE)
                .setScale(0, RoundingMode.CEILING)
                .intValue();
    }

    private Place requireBuilding(Long buildingId, Long destinationId) {
        requirePositive(buildingId);
        Place place = placeMapper.selectById(buildingId);
        if (place == null || destinationId != null && !destinationId.equals(place.getDestinationId())) {
            throw new BusinessException(ErrorCode.ROUTE_011);
        }
        return place;
    }

    private List<MapNode> buildingNodes(Place building) {
        return mapNodeMapper.selectList(new LambdaQueryWrapper<MapNode>()
                .eq(MapNode::getDestinationId, building.getDestinationId())
                .eq(MapNode::getPlaceId, building.getId())
                .orderByAsc(MapNode::getFloorNo)
                .orderByAsc(MapNode::getId));
    }

    private List<MapEdge> buildingEdges(Long destinationId, Map<Long, MapNode> nodeMap) {
        if (nodeMap.isEmpty()) {
            return List.of();
        }
        return mapEdgeMapper.selectList(new LambdaQueryWrapper<MapEdge>()
                        .eq(MapEdge::getDestinationId, destinationId)
                        .orderByAsc(MapEdge::getId))
                .stream()
                .filter(edge -> nodeMap.containsKey(edge.getFromNodeId())
                        && nodeMap.containsKey(edge.getToNodeId()))
                .toList();
    }

    private Map<Long, MapNode> nodesById(List<MapNode> nodes) {
        Map<Long, MapNode> nodeMap = new LinkedHashMap<>();
        for (MapNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    private Set<Integer> floors(List<MapNode> nodes) {
        Set<Integer> floors = new LinkedHashSet<>();
        nodes.stream()
                .map(MapNode::getFloorNo)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .forEach(floors::add);
        return floors;
    }

    private Map<Long, Place> placesById(Set<Long> ids) {
        Map<Long, Place> results = new HashMap<>();
        for (Place place : placeMapper.selectBatchIds(ids)) {
            results.put(place.getId(), place);
        }
        return results;
    }

    private IndoorBuildingVO toBuildingVO(Place place, Set<Integer> floors) {
        IndoorBuildingVO vo = new IndoorBuildingVO();
        vo.setBuildingId(place.getId());
        vo.setDestinationId(place.getDestinationId());
        vo.setBuildingName(place.getName());
        vo.setPlaceType(place.getPlaceType());
        vo.setFloorInfo(place.getFloorInfo());
        vo.setFloorNos(floors.stream().sorted(Comparator.naturalOrder()).toList());
        return vo;
    }

    private void requirePositive(Long value) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
    }
}
