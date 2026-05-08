package com.trip.vo.response;

import com.trip.entity.Destination;
import java.math.BigDecimal;
import java.util.List;

/**
 * 目的地接口响应对象。
 */
public class DestinationVO {

    private Long id;
    private String name;
    private String type;
    private String category;
    private String city;
    private String description;
    private BigDecimal heatScore;
    private BigDecimal ratingScore;
    private String coverUrl;
    private List<String> tags;

    public static DestinationVO from(Destination destination, List<String> tags) {
        DestinationVO vo = new DestinationVO();
        vo.setId(destination.getId());
        vo.setName(destination.getName());
        vo.setType(destination.getType());
        vo.setCategory(destination.getCategory());
        vo.setCity(destination.getCity());
        vo.setDescription(destination.getDescription());
        vo.setHeatScore(destination.getHeatScore());
        vo.setRatingScore(destination.getRatingScore());
        vo.setCoverUrl(destination.getCoverUrl());
        vo.setTags(tags);
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
