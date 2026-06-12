package com.trip.vo.response;

import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DiaryVO {

    private Long id;
    private Long userId;
    private String username;
    private Long destinationId;
    private String destinationName;
    private Long routeHistoryId;
    private String title;
    private String contentText;
    private Long heatScore;
    private BigDecimal ratingScore;
    private Integer ratingCount;
    private String visibility;
    private List<DiaryMediaVO> mediaList;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DiaryVO from(Diary diary, User user, Destination destination, List<DiaryMediaVO> mediaList) {
        DiaryVO vo = new DiaryVO();
        vo.setId(diary.getId());
        vo.setUserId(diary.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setDestinationId(diary.getDestinationId());
        vo.setDestinationName(destination == null ? null : destination.getName());
        vo.setRouteHistoryId(diary.getRouteHistoryId());
        vo.setTitle(diary.getTitle());
        vo.setContentText(diary.getContentText());
        vo.setHeatScore(diary.getHeatScore());
        vo.setRatingScore(diary.getRatingScore());
        vo.setRatingCount(diary.getRatingCount());
        vo.setVisibility(diary.getVisibility());
        vo.setMediaList(mediaList);
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
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

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public Long getHeatScore() {
        return heatScore;
    }

    public void setHeatScore(Long heatScore) {
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

    public List<DiaryMediaVO> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<DiaryMediaVO> mediaList) {
        this.mediaList = mediaList;
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
