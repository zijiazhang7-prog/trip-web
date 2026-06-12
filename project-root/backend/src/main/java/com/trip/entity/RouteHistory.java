package com.trip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("route_history")
public class RouteHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long destinationId;

    private Long startNodeId;

    private Long endNodeId;

    private String pathNodeJson;

    private String pathEdgeJson;

    private String orderedTargetNodeJson;

    private String strategyType;

    private String transportType;

    private BigDecimal totalDistance;

    private Integer estimatedTime;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public String getPathNodeJson() {
        return pathNodeJson;
    }

    public void setPathNodeJson(String pathNodeJson) {
        this.pathNodeJson = pathNodeJson;
    }

    public String getPathEdgeJson() {
        return pathEdgeJson;
    }

    public void setPathEdgeJson(String pathEdgeJson) {
        this.pathEdgeJson = pathEdgeJson;
    }

    public String getOrderedTargetNodeJson() {
        return orderedTargetNodeJson;
    }

    public void setOrderedTargetNodeJson(String orderedTargetNodeJson) {
        this.orderedTargetNodeJson = orderedTargetNodeJson;
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
}
