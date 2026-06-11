package com.trip.vo.response;

import com.trip.entity.Diary;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin 日记列表响应对象。
 */
public class AdminDiaryVO {

    private Long id;
    private Long userId;
    private Long destinationId;
    private Long routeHistoryId;
    private String title;
    private BigDecimal heatScore;
    private BigDecimal ratingScore;
    private Integer ratingCount;
    private String visibility;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminDiaryVO from(Diary diary) {
        AdminDiaryVO vo = new AdminDiaryVO();
        vo.setId(diary.getId());
        vo.setUserId(diary.getUserId());
        vo.setDestinationId(diary.getDestinationId());
        vo.setRouteHistoryId(diary.getRouteHistoryId());
        vo.setTitle(diary.getTitle());
        vo.setHeatScore(diary.getHeatScore());
        vo.setRatingScore(diary.getRatingScore());
        vo.setRatingCount(diary.getRatingCount());
        vo.setVisibility(diary.getVisibility());
        vo.setStatus(diary.getStatus());
        vo.setCreatedAt(diary.getCreatedAt());
        vo.setUpdatedAt(diary.getUpdatedAt());
        return vo;
    }

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

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Long getRouteHistoryId() {
        return routeHistoryId;
    }

    public void setRouteHistoryId(Long routeHistoryId) {
        this.routeHistoryId = routeHistoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
