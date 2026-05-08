package com.trip.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trip.dto.request.DiaryFulltextSearchQuery;
import com.trip.dto.request.DiaryTitleSearchQuery;
import com.trip.entity.Diary;

/**
 * 公共文本检索能力，当前 P1 基础版先支撑日记标题与正文检索。
 */
public interface SearchService {

    IPage<Diary> searchDiaryByTitle(DiaryTitleSearchQuery query);

    IPage<Diary> searchDiaryFulltext(DiaryFulltextSearchQuery query);
}
