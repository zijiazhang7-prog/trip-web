package com.trip.vo.response;

public class DiaryCreateResponse {

    private Long diaryId;

    public DiaryCreateResponse(Long diaryId) {
        this.diaryId = diaryId;
    }

    public Long getDiaryId() {
        return diaryId;
    }

    public void setDiaryId(Long diaryId) {
        this.diaryId = diaryId;
    }
}
