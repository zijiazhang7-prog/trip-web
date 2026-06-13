package com.trip.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Admin 地图节点维护请求。
 */
public class AdminMapNodeRequest {

    @NotNull
    @Min(1)
    private Long destinationId;

    @NotBlank
    @Size(max = 100)
    private String nodeName;

    @NotBlank
    @Size(max = 50)
    private String nodeType;

    @Min(1)
    private Long refId;

    @Min(1)
    private Long placeId;

    private BigDecimal lng;

    private BigDecimal lat;

    private Integer floorNo;

    private BigDecimal indoorX;

    private BigDecimal indoorY;

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

    public Long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
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
