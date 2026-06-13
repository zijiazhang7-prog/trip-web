package com.trip.dto.map;

import java.math.BigDecimal;

/**
 * 室内最短路径中的节点结果。
 */
public class IndoorPathNodeResult {

    private Long nodeId;
    private String nodeName;
    private String nodeType;
    private Integer floorNo;
    private BigDecimal indoorX;
    private BigDecimal indoorY;

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }

    public BigDecimal getIndoorX() {
        return indoorX;
    }

    public void setIndoorX(BigDecimal indoorX) {
        this.indoorX = indoorX;
    }

    public BigDecimal getIndoorY() {
        return indoorY;
    }

    public void setIndoorY(BigDecimal indoorY) {
        this.indoorY = indoorY;
    }
}
