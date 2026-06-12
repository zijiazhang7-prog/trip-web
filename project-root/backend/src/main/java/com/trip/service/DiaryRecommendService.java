package com.trip.service;

import com.trip.dto.request.DiaryRecommendQuery;
import com.trip.vo.response.DiaryVO;
import com.trip.vo.response.PageResultVO;

/**
 * 当前登录用户的旅游日记推荐服务。
 */
public interface DiaryRecommendService {

    PageResultVO<DiaryVO> recommendDiaries(DiaryRecommendQuery query);
}
