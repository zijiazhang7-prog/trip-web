package com.trip.dto.map;

import java.math.BigDecimal;

/**
 * 最短路径中的边结果，保留路径展示和路线历史写入所需的基础字段。
 */
public class PathEdgeResult {

    private Long edgeId;

    private Long fromNodeId;

    private Long toNodeId;

    private BigDecimal distance;

    private String transportType;

    public PathEdgeResult() {
    }

    public PathEdgeResult(
            Long edgeId,
            Long fromNodeId,
            Long toNodeId,
            BigDecimal distance,
            String transportType) {
        this.edgeId = edgeId;
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.distance = distance;
        this.transportType = transportType;
    }

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

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal distance) {
        this.distance = distance;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }
}
