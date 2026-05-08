package com.trip.dto.map;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapService 单目标最短路径结果。
 */
public class ShortestPathResult {

    private Long destinationId;

    private Long startNodeId;

    private Long targetNodeId;

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

    public Long getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(Long targetNodeId) {
        this.targetNodeId = targetNodeId;
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
