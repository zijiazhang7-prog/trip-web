package com.trip.service;

import com.trip.dto.request.DiaryCreateRequest;
import com.trip.dto.request.DiaryFulltextSearchQuery;
import com.trip.dto.request.DiaryListQuery;
import com.trip.dto.request.DiaryTitleSearchQuery;
import com.trip.vo.response.DiaryCreateResponse;
import com.trip.vo.response.DiaryVO;
import com.trip.vo.response.PageResultVO;

public interface DiaryService {

    DiaryCreateResponse createDiary(DiaryCreateRequest request);

    PageResultVO<DiaryVO> listDiaries(DiaryListQuery query);

    DiaryVO getDiaryDetail(Long id);

    PageResultVO<DiaryVO> listDestinationDiaries(Long destinationId, DiaryListQuery query);

    PageResultVO<DiaryVO> searchByTitle(DiaryTitleSearchQuery query);

    PageResultVO<DiaryVO> searchFulltext(DiaryFulltextSearchQuery query);
}
