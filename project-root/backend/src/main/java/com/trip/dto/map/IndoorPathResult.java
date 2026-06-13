package com.trip.dto.map;

import java.math.BigDecimal;
import java.util.List;

/**
 * MapService 输出的室内最短路径结果，时间单位为分钟。
 */
public class IndoorPathResult {

    private Long destinationId;
    private Long buildingId;
    private Long startNodeId;
    private Long targetNodeId;
    private BigDecimal totalDistance;
    private BigDecimal estimatedTime;
    private List<IndoorPathNodeResult> pathNodes;
    private List<IndoorPathEdgeResult> pathEdges;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
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

    public List<IndoorPathNodeResult> getPathNodes() {
        return pathNodes;
    }

    public void setPathNodes(List<IndoorPathNodeResult> pathNodes) {
        this.pathNodes = pathNodes;
    }

    public List<IndoorPathEdgeResult> getPathEdges() {
        return pathEdges;
    }

    public void setPathEdges(List<IndoorPathEdgeResult> pathEdges) {
        this.pathEdges = pathEdges;
    }
}
