package com.trip.vo.response;

import com.trip.entity.UserPreference;
import java.time.LocalDateTime;
import java.util.List;

public class UserPreferenceVO {

    private Long userId;

    private Integer preferHotLevel;

    private List<String> preferThemeList;

    private String preferFoodType;

    private Integer preferCrowdLevel;

    private String travelStyle;

    private String customPreferenceText;

    private LocalDateTime updatedAt;

    public static UserPreferenceVO empty(Long userId) {
        UserPreferenceVO vo = new UserPreferenceVO();
        vo.setUserId(userId);
        vo.setPreferThemeList(List.of());
        return vo;
    }

    public static UserPreferenceVO from(UserPreference preference, List<String> preferThemeList) {
        UserPreferenceVO vo = new UserPreferenceVO();
        vo.setUserId(preference.getUserId());
        vo.setPreferHotLevel(preference.getPreferHotLevel());
        vo.setPreferThemeList(preferThemeList);
        vo.setPreferFoodType(preference.getPreferFoodType());
        vo.setPreferCrowdLevel(preference.getPreferCrowdLevel());
        vo.setTravelStyle(preference.getTravelStyle());
        vo.setCustomPreferenceText(preference.getCustomPreferenceText());
        vo.setUpdatedAt(preference.getUpdatedAt());
        return vo;
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

    public List<String> getPreferThemeList() {
        return preferThemeList;
    }

    public void setPreferThemeList(List<String> preferThemeList) {
        this.preferThemeList = preferThemeList;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
