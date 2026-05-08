package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.ErrorCode;
import com.trip.dto.request.DiaryFulltextSearchQuery;
import com.trip.dto.request.DiaryTitleSearchQuery;
import com.trip.entity.Diary;
import com.trip.exception.BusinessException;
import com.trip.mapper.DiaryMapper;
import com.trip.service.SearchService;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * SearchService 基础版：基于 MySQL LIKE 实现小规模日记文本检索。
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final int ENABLED_STATUS = 1;
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final String VISIBILITY_PUBLIC = "public";
    private static final String SORT_BY_LATEST = "latest";
    private static final String SORT_BY_HEAT = "heat";
    private static final String SORT_BY_RATING = "rating";

    private final DiaryMapper diaryMapper;

    public SearchServiceImpl(DiaryMapper diaryMapper) {
        this.diaryMapper = diaryMapper;
    }

    /**
     * 数据结构：数据库索引和分页结果集。
     * 算法：用标题关键词进行 LIKE 召回，再按白名单排序字段排序。
     * 复杂度：取决于 MySQL 执行计划；业务层只处理当前页，空间复杂度 O(pageSize)。
     * 适用范围：适合课程设计小规模数据，后续可替换为倒排索引或 FULLTEXT。
     */
    @Override
    public IPage<Diary> searchDiaryByTitle(DiaryTitleSearchQuery query) {
        DiaryTitleSearchQuery safeQuery = query == null ? new DiaryTitleSearchQuery() : query;
        String title = normalize(safeQuery.getTitle());
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }

        LambdaQueryWrapper<Diary> wrapper = publicDiaryWrapper()
                .like(Diary::getTitle, title);
        applySort(wrapper, normalizeSortBy(safeQuery.getSortBy()));
        return diaryMapper.selectPage(new Page<>(pageNum(safeQuery.getPageNum()), pageSize(safeQuery.getPageSize())), wrapper);
    }

    /**
     * 数据结构：数据库文本字段和分页结果集。
     * 算法：用正文关键词进行 LIKE 召回，可选目的地过滤，再按白名单排序字段排序。
     * 复杂度：取决于 MySQL 执行计划；业务层只处理当前页，空间复杂度 O(pageSize)。
     * 适用范围：适合 P1 基础全文检索演示，非大规模全文搜索最终方案。
     */
    @Override
    public IPage<Diary> searchDiaryFulltext(DiaryFulltextSearchQuery query) {
        DiaryFulltextSearchQuery safeQuery = query == null ? new DiaryFulltextSearchQuery() : query;
        String keyword = normalize(safeQuery.getKeyword());
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        if (safeQuery.getDestinationId() != null && safeQuery.getDestinationId() <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        LambdaQueryWrapper<Diary> wrapper = publicDiaryWrapper()
                .like(Diary::getContentText, keyword);
        if (safeQuery.getDestinationId() != null) {
            wrapper.eq(Diary::getDestinationId, safeQuery.getDestinationId());
        }
        applySort(wrapper, normalizeSortBy(safeQuery.getSortBy()));
        return diaryMapper.selectPage(new Page<>(pageNum(safeQuery.getPageNum()), pageSize(safeQuery.getPageSize())), wrapper);
    }

    private LambdaQueryWrapper<Diary> publicDiaryWrapper() {
        return new LambdaQueryWrapper<Diary>()
                .eq(Diary::getStatus, ENABLED_STATUS)
                .eq(Diary::getVisibility, VISIBILITY_PUBLIC);
    }

    private void applySort(LambdaQueryWrapper<Diary> wrapper, String sortBy) {
        if (SORT_BY_HEAT.equals(sortBy)) {
            wrapper.orderByDesc(Diary::getHeatScore).orderByDesc(Diary::getCreatedAt).orderByAsc(Diary::getId);
        } else if (SORT_BY_RATING.equals(sortBy)) {
            wrapper.orderByDesc(Diary::getRatingScore).orderByDesc(Diary::getCreatedAt).orderByAsc(Diary::getId);
        } else {
            wrapper.orderByDesc(Diary::getCreatedAt).orderByAsc(Diary::getId);
        }
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = normalize(sortBy);
        if (!StringUtils.hasText(normalized)) {
            return SORT_BY_LATEST;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!SORT_BY_LATEST.equals(normalized)
                && !SORT_BY_HEAT.equals(normalized)
                && !SORT_BY_RATING.equals(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_008);
        }
        return normalized;
    }

    private long pageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private long pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
