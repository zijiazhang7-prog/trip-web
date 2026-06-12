package com.trip.service;

import com.trip.dto.map.MultiPathResult;
import com.trip.dto.map.ShortestPathResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MapService {

    ShortestPathResult shortestPath(Long destinationId, Long startNodeId, Long targetNodeId);

    ShortestPathResult shortestPath(Long destinationId, Long startNodeId, Long targetNodeId, String strategyType);

    ShortestPathResult shortestPath(
            Long destinationId,
            Long startNodeId,
            Long targetNodeId,
            String strategyType,
            String transportType);

    MultiPathResult multiTargetPath(
            Long destinationId,
            Long startNodeId,
            List<Long> targetNodeIds,
            boolean returnToStart);

    MultiPathResult multiTargetPath(
            Long destinationId,
            Long startNodeId,
            List<Long> targetNodeIds,
            boolean returnToStart,
            String strategyType);

    MultiPathResult multiTargetPath(
            Long destinationId,
            Long startNodeId,
            List<Long> targetNodeIds,
            boolean returnToStart,
            String strategyType,
            String transportType);

    Map<Long, BigDecimal> shortestDistances(Long destinationId, Long startNodeId);
}
