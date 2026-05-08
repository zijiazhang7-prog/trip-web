package com.trip.vo.response;

import com.trip.dto.map.PathEdgeResult;
import java.math.BigDecimal;

/**
 * 路线结果中的路径边。
 */
public class RoutePathEdgeVO {

    private Long fromNodeId;

    private Long toNodeId;

    private BigDecimal distance;

    public static RoutePathEdgeVO from(PathEdgeResult edge) {
        RoutePathEdgeVO vo = new RoutePathEdgeVO();
        vo.setFromNodeId(edge.getFromNodeId());
        vo.setToNodeId(edge.getToNodeId());
        vo.setDistance(edge.getDistance());
        return vo;
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
}
