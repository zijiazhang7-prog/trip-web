package com.trip.vo.response;

import com.trip.dto.map.PathNodeResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 路线历史摘要与详情。
 */
public class RouteHistoryVO {

    private Long id;

    private Long destinationId;

    private String destinationName;

    private Long startNodeId;

    private String startNodeName;

    private Long endNodeId;

    private String endNodeName;

    private String strategyType;

    private String transportType;

    private BigDecimal totalDistance;

    private Integer estimatedTime;

    private LocalDateTime createdAt;

    private List<PathNodeResult> pathNodes;

    private List<RoutePathEdgeVO> pathEdges;

    private List<Long> orderedTargetNodeIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public Long getStartNodeId() {
        return startNodeId;
    }

    public void setStartNodeId(Long startNodeId) {
        this.startNodeId = startNodeId;
    }

    public String getStartNodeName() {
        return startNodeName;
    }

    public void setStartNodeName(String startNodeName) {
        this.startNodeName = startNodeName;
    }

    public Long getEndNodeId() {
        return endNodeId;
    }

    public void setEndNodeId(Long endNodeId) {
        this.endNodeId = endNodeId;
    }

    public String getEndNodeName() {
        return endNodeName;
    }

    public void setEndNodeName(String endNodeName) {
        this.endNodeName = endNodeName;
    }

    public String getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public BigDecimal getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(BigDecimal totalDistance) {
        this.totalDistance = totalDistance;
    }

    public Integer getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(Integer estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<PathNodeResult> getPathNodes() {
        return pathNodes;
    }

    public void setPathNodes(List<PathNodeResult> pathNodes) {
        this.pathNodes = pathNodes;
    }

    public List<RoutePathEdgeVO> getPathEdges() {
        return pathEdges;
    }

    public void setPathEdges(List<RoutePathEdgeVO> pathEdges) {
        this.pathEdges = pathEdges;
    }

    public List<Long> getOrderedTargetNodeIds() {
        return orderedTargetNodeIds;
    }

    public void setOrderedTargetNodeIds(List<Long> orderedTargetNodeIds) {
        this.orderedTargetNodeIds = orderedTargetNodeIds;
    }
}
