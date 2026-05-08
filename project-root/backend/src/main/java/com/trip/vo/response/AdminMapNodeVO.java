package com.trip.vo.response;

import com.trip.entity.MapNode;
import java.math.BigDecimal;

/**
 * Admin 地图节点响应对象。
 */
public class AdminMapNodeVO {

    private Long id;
    private Long destinationId;
    private String nodeName;
    private String nodeType;
    private Long refId;
    private BigDecimal lng;
    private BigDecimal lat;
    private Integer floorNo;

    public static AdminMapNodeVO from(MapNode node) {
        AdminMapNodeVO vo = new AdminMapNodeVO();
        vo.setId(node.getId());
        vo.setDestinationId(node.getDestinationId());
        vo.setNodeName(node.getNodeName());
        vo.setNodeType(node.getNodeType());
        vo.setRefId(node.getRefId());
        vo.setLng(node.getLng());
        vo.setLat(node.getLat());
        vo.setFloorNo(node.getFloorNo());
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

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public BigDecimal getLng() {
        return lng;
    }

    public void setLng(BigDecimal lng) {
        this.lng = lng;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public void setLat(BigDecimal lat) {
        this.lat = lat;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }
}
