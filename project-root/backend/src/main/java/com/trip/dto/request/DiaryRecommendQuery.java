package com.trip.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 日记个性化推荐查询参数。
 */
public class DiaryRecommendQuery {

    @Pattern(regexp = "^(interest|heat|rating)$", message = "必须是 interest/heat/rating")
    private String sortBy;

    @Min(1)
    @Max(100)
    private Integer pageSize;

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
