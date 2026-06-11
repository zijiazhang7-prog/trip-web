package com.trip.vo.response;

import java.math.BigDecimal;

public class DiaryRatingVO {

    private Long diaryId;
    private Integer userScore;
    private BigDecimal ratingScore;
    private Integer ratingCount;

    public DiaryRatingVO() {
    }

    public DiaryRatingVO(Long diaryId, Integer userScore, BigDecimal ratingScore, Integer ratingCount) {
        this.diaryId = diaryId;
        this.userScore = userScore;
        this.ratingScore = ratingScore;
        this.ratingCount = ratingCount;
    }

    public Long getDiaryId() {
        return diaryId;
    }

    public void setDiaryId(Long diaryId) {
        this.diaryId = diaryId;
    }

    public Integer getUserScore() {
        return userScore;
    }

    public void setUserScore(Integer userScore) {
        this.userScore = userScore;
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
}
