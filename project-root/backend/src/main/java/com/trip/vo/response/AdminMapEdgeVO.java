package com.trip.vo.response;

import com.trip.entity.MapEdge;
import java.math.BigDecimal;

/**
 * Admin 地图边响应对象。
 */
public class AdminMapEdgeVO {

    private Long id;
    private Long destinationId;
    private Long fromNodeId;
    private Long toNodeId;
    private BigDecimal distance;
    private BigDecimal idealSpeed;
    private BigDecimal crowdFactor;
    private String transportType;
    private String edgeType;
    private Integer bidirectionalFlag;

    public static AdminMapEdgeVO from(MapEdge edge) {
        AdminMapEdgeVO vo = new AdminMapEdgeVO();
        vo.setId(edge.getId());
        vo.setDestinationId(edge.getDestinationId());
        vo.setFromNodeId(edge.getFromNodeId());
        vo.setToNodeId(edge.getToNodeId());
        vo.setDistance(edge.getDistance());
        vo.setIdealSpeed(edge.getIdealSpeed());
        vo.setCrowdFactor(edge.getCrowdFactor());
        vo.setTransportType(edge.getTransportType());
        vo.setEdgeType(edge.getEdgeType());
        vo.setBidirectionalFlag(edge.getBidirectionalFlag());
        return vo;
    }

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

    public BigDecimal getIdealSpeed() {
        return idealSpeed;
    }

    public void setIdealSpeed(BigDecimal idealSpeed) {
        this.idealSpeed = idealSpeed;
    }

    public BigDecimal getCrowdFactor() {
        return crowdFactor;
    }

    public void setCrowdFactor(BigDecimal crowdFactor) {
        this.crowdFactor = crowdFactor;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType;
    }

    public Integer getBidirectionalFlag() {
        return bidirectionalFlag;
    }

    public void setBidirectionalFlag(Integer bidirectionalFlag) {
        this.bidirectionalFlag = bidirectionalFlag;
    }
}
