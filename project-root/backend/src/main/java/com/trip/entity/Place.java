package com.trip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("place")
public class Place {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long destinationId;

    private String name;

    private String placeType;

    private String description;

    private BigDecimal lng;

    private BigDecimal lat;

    private String floorInfo;

    private BigDecimal heatScore;

    private BigDecimal ratingScore;

    private String openTimeRule;

    private Integer suggestedDurationMin;

    private Integer costLevel;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaceType() {
        return placeType;
    }

    public void setPlaceType(String placeType) {
        this.placeType = placeType;
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

    public String getFloorInfo() {
        return floorInfo;
    }

    public void setFloorInfo(String floorInfo) {
        this.floorInfo = floorInfo;
    }

    public BigDecimal getHeatScore() {
        return heatScore;
    }

    public void setHeatScore(BigDecimal heatScore) {
        this.heatScore = heatScore;
    }

    public BigDecimal getRatingScore() {
        return ratingScore;
    }

    public void setRatingScore(BigDecimal ratingScore) {
        this.ratingScore = ratingScore;
    }

    public String getOpenTimeRule() {
        return openTimeRule;
    }

    public void setOpenTimeRule(String openTimeRule) {
        this.openTimeRule = openTimeRule;
    }

    public Integer getSuggestedDurationMin() {
        return suggestedDurationMin;
    }

    public void setSuggestedDurationMin(Integer suggestedDurationMin) {
        this.suggestedDurationMin = suggestedDurationMin;
    }

    public Integer getCostLevel() {
        return costLevel;
    }

    public void setCostLevel(Integer costLevel) {
        this.costLevel = costLevel;
    }
}
