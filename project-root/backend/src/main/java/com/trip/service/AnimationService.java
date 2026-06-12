package com.trip.service;

import com.trip.vo.response.DiaryAnimationVO;

public interface AnimationService {

    DiaryAnimationVO generate(Long diaryId);

    DiaryAnimationVO getByDiaryId(Long diaryId);
}
