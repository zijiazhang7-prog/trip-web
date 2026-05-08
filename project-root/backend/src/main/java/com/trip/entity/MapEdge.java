package com.trip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("map_edge")
public class MapEdge {

    @TableId(type = IdType.AUTO)
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
