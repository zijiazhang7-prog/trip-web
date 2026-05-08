package com.trip.dto.map;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapService 多目标路径规划结果。
 */
public class MultiPathResult {

    private Long destinationId;

    private Long startNodeId;

    private Long endNodeId;

    private List<Long> orderedTargetNodeIds;

    private BigDecimal totalDistance;

    private BigDecimal estimatedTime;

    private List<PathNodeResult> pathNodes;

    private List<PathEdgeResult> pathEdges;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Long getStartNodeId() {
        return startNodeId;
    }

    public void setStartNodeId(Long startNodeId) {
        this.startNodeId = startNodeId;
    }

    public Long getEndNodeId() {
        return endNodeId;
    }

    public void setEndNodeId(Long endNodeId) {
        this.endNodeId = endNodeId;
    }

    public List<Long> getOrderedTargetNodeIds() {
        return orderedTargetNodeIds;
    }

    public void setOrderedTargetNodeIds(List<Long> orderedTargetNodeIds) {
        this.orderedTargetNodeIds = orderedTargetNodeIds;
    }

    public BigDecimal getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(BigDecimal totalDistance) {
        this.totalDistance = totalDistance;
    }

    public BigDecimal getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(BigDecimal estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public List<PathNodeResult> getPathNodes() {
        return pathNodes;
    }

    public void setPathNodes(List<PathNodeResult> pathNodes) {
        this.pathNodes = pathNodes;
    }

    public List<PathEdgeResult> getPathEdges() {
        return pathEdges;
    }

    public void setPathEdges(List<PathEdgeResult> pathEdges) {
        this.pathEdges = pathEdges;
    }
}
