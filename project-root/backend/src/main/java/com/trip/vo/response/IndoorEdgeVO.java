package com.trip.vo.response;

import java.math.BigDecimal;

/**
 * 室内楼层图或路线中的边。
 */
public class IndoorEdgeVO {

    private Long edgeId;
    private Long fromNodeId;
    private Long toNodeId;
    private String fromNodeName;
    private String toNodeName;
    private Integer fromFloorNo;
    private Integer toFloorNo;
    private String edgeType;
    private BigDecimal distance;
    private Integer timeCost;

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

    public String getFromNodeName() {
        return fromNodeName;
    }

    public void setFromNodeName(String fromNodeName) {
        this.fromNodeName = fromNodeName;
    }

    public String getToNodeName() {
        return toNodeName;
    }

    public void setToNodeName(String toNodeName) {
        this.toNodeName = toNodeName;
    }

    public Integer getFromFloorNo() {
        return fromFloorNo;
    }

    public void setFromFloorNo(Integer fromFloorNo) {
        this.fromFloorNo = fromFloorNo;
    }

    public Integer getToFloorNo() {
        return toFloorNo;
    }

    public void setToFloorNo(Integer toFloorNo) {
        this.toFloorNo = toFloorNo;
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

    public Integer getTimeCost() {
        return timeCost;
    }

    public void setTimeCost(Integer timeCost) {
        this.timeCost = timeCost;
    }
}
