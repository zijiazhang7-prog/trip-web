package com.trip.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Admin 美食维护请求。
 */
public class AdminFoodRequest {

    @NotNull
    @Min(1)
    private Long destinationId;

    @Min(1)
    private Long facilityId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String foodType;

    @Size(max = 100)
    private String shopName;

    @Size(max = 2000)
    private String description;

    @DecimalMin("0.00")
    @DecimalMax("999.99")
    private BigDecimal heatScore;

    @DecimalMin("0.00")
    @DecimalMax("5.00")
    private BigDecimal ratingScore;

    @DecimalMin("0.00")
    @DecimalMax("999999.99")
    private BigDecimal avgPrice;

    @Size(max = 255)
    private String coverUrl;

    @DecimalMin("-180.000000")
    @DecimalMax("180.000000")
    private BigDecimal lng;

    @DecimalMin("-90.000000")
    @DecimalMax("90.000000")
    private BigDecimal lat;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFoodType() {
        return foodType;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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
}
