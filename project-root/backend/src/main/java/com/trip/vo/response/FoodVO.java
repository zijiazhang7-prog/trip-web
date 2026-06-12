package com.trip.vo.response;

import com.trip.entity.Food;
import java.math.BigDecimal;
import java.util.List;

/**
 * 美食查询结果。
 */
public class FoodVO {

    private Long id;
    private Long destinationId;
    private Long facilityId;
    private String name;
    private String foodType;
    private String shopName;
    private String description;
    private BigDecimal heatScore;
    private BigDecimal ratingScore;
    private BigDecimal avgPrice;
    private String coverUrl;
    private BigDecimal lng;
    private BigDecimal lat;
    private List<String> cuisineTags;

    public static FoodVO from(Food food) {
        FoodVO vo = new FoodVO();
        vo.setId(food.getId());
        vo.setDestinationId(food.getDestinationId());
        vo.setFacilityId(food.getFacilityId());
        vo.setName(food.getName());
        vo.setFoodType(food.getFoodType());
        vo.setShopName(food.getShopName());
        vo.setDescription(food.getDescription());
        vo.setHeatScore(food.getHeatScore());
        vo.setRatingScore(food.getRatingScore());
        vo.setAvgPrice(food.getAvgPrice());
        vo.setCoverUrl(food.getCoverUrl());
        vo.setLng(food.getLng());
        vo.setLat(food.getLat());
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

    public List<String> getCuisineTags() {
        return cuisineTags;
    }

    public void setCuisineTags(List<String> cuisineTags) {
        this.cuisineTags = cuisineTags;
    }
}
