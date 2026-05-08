package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.ErrorCode;
import com.trip.dto.request.DiaryFulltextSearchQuery;
import com.trip.dto.request.DiaryTitleSearchQuery;
import com.trip.entity.Diary;
import com.trip.exception.BusinessException;
import com.trip.mapper.DiaryMapper;
import com.trip.service.impl.SearchServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class SearchServiceTests {

    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final SearchServiceImpl searchService = new SearchServiceImpl(diaryMapper);

    @Test
    void searchDiaryByTitleShouldRejectBlankTitle() {
        DiaryTitleSearchQuery query = new DiaryTitleSearchQuery();
        query.setTitle(" ");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> searchService.searchDiaryByTitle(query));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void searchDiaryByTitleShouldRejectTooLongTitle() {
        DiaryTitleSearchQuery query = new DiaryTitleSearchQuery();
        query.setTitle("a".repeat(151));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> searchService.searchDiaryByTitle(query));

        assertEquals(ErrorCode.COMMON_002, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void searchDiaryFulltextShouldRejectBlankKeyword() {
        DiaryFulltextSearchQuery query = new DiaryFulltextSearchQuery();
        query.setKeyword("");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> searchService.searchDiaryFulltext(query));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void searchDiaryFulltextShouldRejectTooLongKeyword() {
        DiaryFulltextSearchQuery query = new DiaryFulltextSearchQuery();
        query.setKeyword("a".repeat(101));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> searchService.searchDiaryFulltext(query));

        assertEquals(ErrorCode.COMMON_002, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void searchDiaryFulltextShouldRejectInvalidDestinationId() {
        DiaryFulltextSearchQuery query = new DiaryFulltextSearchQuery();
        query.setKeyword("图书馆");
        query.setDestinationId(0L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> searchService.searchDiaryFulltext(query));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void searchDiaryByTitleShouldRejectUnsupportedSortBy() {
        DiaryTitleSearchQuery query = new DiaryTitleSearchQuery();
        query.setTitle("校园");
        query.setSortBy("unknown");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> searchService.searchDiaryByTitle(query));

        assertEquals(ErrorCode.COMMON_008, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void searchDiaryFulltextShouldRejectUnsupportedSortBy() {
        DiaryFulltextSearchQuery query = new DiaryFulltextSearchQuery();
        query.setKeyword("图书馆");
        query.setSortBy("unknown");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> searchService.searchDiaryFulltext(query));

        assertEquals(ErrorCode.COMMON_008, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void searchDiaryByTitleShouldReturnPublicDiaryPage() {
        DiaryTitleSearchQuery query = new DiaryTitleSearchQuery();
        query.setTitle("校园");
        query.setSortBy("heat");
        query.setPageSize(200);
        Page<Diary> page = new Page<>(1, 100);
        page.setRecords(List.of(new Diary()));
        page.setTotal(1);
        when(diaryMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        Page<Diary> result = (Page<Diary>) searchService.searchDiaryByTitle(query);

        assertEquals(100, result.getSize());
        assertEquals(1, result.getTotal());
        verify(diaryMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void searchDiaryFulltextShouldSupportDestinationFilter() {
        DiaryFulltextSearchQuery query = new DiaryFulltextSearchQuery();
        query.setKeyword("图书馆");
        query.setDestinationId(101L);
        query.setSortBy("rating");
        Page<Diary> page = new Page<>(2, 5);
        page.setRecords(List.of(new Diary()));
        page.setTotal(6);
        when(diaryMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        Page<Diary> result = (Page<Diary>) searchService.searchDiaryFulltext(query);

        assertEquals(2, result.getCurrent());
        assertEquals(6, result.getTotal());
        verify(diaryMapper).selectPage(any(Page.class), any(Wrapper.class));
    }
}
