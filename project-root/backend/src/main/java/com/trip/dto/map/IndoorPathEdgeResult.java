package com.trip.dto.map;

import java.math.BigDecimal;

/**
 * 室内最短路径中的边结果，时间单位为分钟。
 */
public class IndoorPathEdgeResult {

    private Long edgeId;
    private Long fromNodeId;
    private Long toNodeId;
    private String edgeType;
    private BigDecimal distance;
    private BigDecimal timeCost;

    public Long getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(Long edgeId) {
        this.edgeId = edgeId;
    }

    public Long getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(Long fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public Long getToNodeId() {
        return toNodeId;
    }

    public void setToNodeId(Long toNodeId) {
        this.toNodeId = toNodeId;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal distance) {
        this.distance = distance;
    }

    public BigDecimal getTimeCost() {
        return timeCost;
    }

    public void setTimeCost(BigDecimal timeCost) {
        this.timeCost = timeCost;
    }
}
