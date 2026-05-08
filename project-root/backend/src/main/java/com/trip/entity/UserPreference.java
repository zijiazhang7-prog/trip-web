package com.trip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_preference")
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer preferHotLevel;

    private String preferTheme;

    private String preferFoodType;

    private Integer preferCrowdLevel;

    private String travelStyle;

    private String customPreferenceText;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPreferHotLevel() {
        return preferHotLevel;
    }

    public void setPreferHotLevel(Integer preferHotLevel) {
        this.preferHotLevel = preferHotLevel;
    }

    public String getPreferTheme() {
        return preferTheme;
    }

    public void setPreferTheme(String preferTheme) {
        this.preferTheme = preferTheme;
    }

    public String getPreferFoodType() {
        return preferFoodType;
    }

    public void setPreferFoodType(String preferFoodType) {
        this.preferFoodType = preferFoodType;
    }

    public Integer getPreferCrowdLevel() {
        return preferCrowdLevel;
    }

    public void setPreferCrowdLevel(Integer preferCrowdLevel) {
        this.preferCrowdLevel = preferCrowdLevel;
    }

    public String getTravelStyle() {
        return travelStyle;
    }

    public void setTravelStyle(String travelStyle) {
        this.travelStyle = travelStyle;
    }

    public String getCustomPreferenceText() {
        return customPreferenceText;
    }

    public void setCustomPreferenceText(String customPreferenceText) {
        this.customPreferenceText = customPreferenceText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
