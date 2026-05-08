package com.trip.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UserPreferenceRequest {

    @Min(value = 1, message = "必须大于等于1")
    @Max(value = 5, message = "必须小于等于5")
    private Integer preferHotLevel;

    @Size(max = 10, message = "最多选择10个")
    private List<@Size(max = 30, message = "长度不能超过30") String> preferThemeList;

    @Size(max = 100, message = "长度不能超过100")
    private String preferFoodType;

    @Min(value = 1, message = "必须大于等于1")
    @Max(value = 5, message = "必须小于等于5")
    private Integer preferCrowdLevel;

    @Size(max = 50, message = "长度不能超过50")
    private String travelStyle;

    @Size(max = 500, message = "长度不能超过500")
    private String customPreferenceText;

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
}
