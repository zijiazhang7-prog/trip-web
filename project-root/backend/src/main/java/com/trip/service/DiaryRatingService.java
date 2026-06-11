package com.trip.service;

import com.trip.dto.request.DiaryRatingRequest;
import com.trip.vo.response.DiaryRatingVO;

public interface DiaryRatingService {

    boolean rateDiary(Long diaryId, DiaryRatingRequest request);

    DiaryRatingVO getMyRating(Long diaryId);
}
