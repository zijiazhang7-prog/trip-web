package com.trip.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Admin 场所维护请求。
 */
public class AdminPlaceRequest {

    @NotNull
    @Min(1)
    private Long destinationId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String placeType;

    @Size(max = 2000)
    private String description;

    @DecimalMin("-180.000000")
    @DecimalMax("180.000000")
    private BigDecimal lng;

    @DecimalMin("-90.000000")
    @DecimalMax("90.000000")
    private BigDecimal lat;

    @Size(max = 100)
    private String floorInfo;

    @DecimalMin("0.00")
    @DecimalMax("999.99")
    private BigDecimal heatScore;

    @DecimalMin("0.00")
    @DecimalMax("5.00")
    private BigDecimal ratingScore;

    @Size(max = 255)
    private String openTimeRule;

    @Min(0)
    @Max(10080)
    private Integer suggestedDurationMin;

    @Min(0)
    @Max(10)
    private Integer costLevel;

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
