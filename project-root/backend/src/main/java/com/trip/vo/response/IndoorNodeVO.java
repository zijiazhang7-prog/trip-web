package com.trip.vo.response;

import com.trip.entity.MapNode;
import java.math.BigDecimal;

/**
 * 室内楼层图节点。
 */
public class IndoorNodeVO {

    private Long nodeId;
    private String nodeName;
    private String nodeType;
    private Integer floorNo;
    private BigDecimal x;
    private BigDecimal y;

    public static IndoorNodeVO from(MapNode node) {
        IndoorNodeVO vo = new IndoorNodeVO();
        vo.setNodeId(node.getId());
        vo.setNodeName(node.getNodeName());
        vo.setNodeType(node.getNodeType());
        vo.setFloorNo(node.getFloorNo());
        vo.setX(node.getIndoorX());
        vo.setY(node.getIndoorY());
        return vo;
    }

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

    public BigDecimal getX() {
        return x;
    }

    public void setX(BigDecimal x) {
        this.x = x;
    }

    public BigDecimal getY() {
        return y;
    }

    public void setY(BigDecimal y) {
        this.y = y;
    }
}
