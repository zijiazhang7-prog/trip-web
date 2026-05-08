package com.trip.vo.response;

import com.trip.entity.Facility;
import java.math.BigDecimal;

/**
 * Admin 设施维护响应对象。
 */
public class AdminFacilityVO {

    private Long id;
    private Long destinationId;
    private Long placeId;
    private String name;
    private String facilityType;
    private String description;
    private BigDecimal lng;
    private BigDecimal lat;
    private Integer status;

    public static AdminFacilityVO from(Facility facility) {
        AdminFacilityVO vo = new AdminFacilityVO();
        vo.setId(facility.getId());
        vo.setDestinationId(facility.getDestinationId());
        vo.setPlaceId(facility.getPlaceId());
        vo.setName(facility.getName());
        vo.setFacilityType(facility.getFacilityType());
        vo.setDescription(facility.getDescription());
        vo.setLng(facility.getLng());
        vo.setLat(facility.getLat());
        vo.setStatus(facility.getStatus());
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

    public Long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(String facilityType) {
        this.facilityType = facilityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
