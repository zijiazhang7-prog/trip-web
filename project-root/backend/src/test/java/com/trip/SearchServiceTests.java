package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.ErrorCode;
import com.trip.dto.request.DiaryFulltextSearchQuery;
import com.trip.dto.request.DiaryTitleSearchQuery;
import com.trip.engine.index.IndexEngine;
import com.trip.engine.index.IndexNamespace;
import com.trip.engine.index.IndexSearchResult;
import com.trip.entity.Diary;
import com.trip.exception.BusinessException;
import com.trip.mapper.DiaryMapper;
import com.trip.service.impl.SearchServiceImpl;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class SearchServiceTests {

    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final SearchServiceImpl searchService = new SearchServiceImpl(new IndexEngine(), diaryMapper);

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Diary.class);
    }

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
    void searchDiaryByTitleShouldTryIndexAndStillUseLikeMapper() {
        IndexEngine mockedIndexEngine = mock(IndexEngine.class);
        when(mockedIndexEngine.findExact(IndexNamespace.DIARY_TITLE, "校园"))
                .thenReturn(IndexSearchResult.available(List.of()));
        when(mockedIndexEngine.findByPrefix(IndexNamespace.DIARY_TITLE, "校园", 1000))
                .thenReturn(IndexSearchResult.available(List.of(1L)));
        SearchServiceImpl indexedSearchService = new SearchServiceImpl(mockedIndexEngine, diaryMapper);
        DiaryTitleSearchQuery query = new DiaryTitleSearchQuery();
        query.setTitle("校园");
        when(diaryMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<Diary>());

        indexedSearchService.searchDiaryByTitle(query);

        verify(mockedIndexEngine).findExact(IndexNamespace.DIARY_TITLE, "校园");
        verify(mockedIndexEngine).findByPrefix(IndexNamespace.DIARY_TITLE, "校园", 1000);
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

    @Test
    void searchDiaryFulltextShouldUseIndexedIdsWithoutLikeWhenIndexHits() {
        IndexEngine mockedIndexEngine = mock(IndexEngine.class);
        when(mockedIndexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "图书馆"))
                .thenReturn(IndexSearchResult.available(List.of(11L, 12L)));
        SearchServiceImpl indexedSearchService = new SearchServiceImpl(mockedIndexEngine, diaryMapper);
        DiaryFulltextSearchQuery query = new DiaryFulltextSearchQuery();
        query.setKeyword("图书馆");
        query.setDestinationId(101L);
        when(diaryMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<Diary>());

        indexedSearchService.searchDiaryFulltext(query);

        ArgumentCaptor<LambdaQueryWrapper<Diary>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(diaryMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment().toLowerCase();
        assertTrue(sqlSegment.contains("id in"));
        assertTrue(sqlSegment.contains("destination_id"));
        assertTrue(!sqlSegment.contains("content_text"));
        verify(mockedIndexEngine).findByContent(IndexNamespace.DIARY_CONTENT, "图书馆");
    }

    @Test
    void searchDiaryFulltextShouldReturnEmptyPageWithoutMapperWhenIndexMisses() {
        IndexEngine mockedIndexEngine = mock(IndexEngine.class);
        when(mockedIndexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "不存在"))
                .thenReturn(IndexSearchResult.available(List.of()));
        SearchServiceImpl indexedSearchService = new SearchServiceImpl(mockedIndexEngine, diaryMapper);
        DiaryFulltextSearchQuery query = new DiaryFulltextSearchQuery();
        query.setKeyword("不存在");
        query.setPageNum(2);
        query.setPageSize(5);

        Page<Diary> result = (Page<Diary>) indexedSearchService.searchDiaryFulltext(query);

        assertEquals(2, result.getCurrent());
        assertEquals(5, result.getSize());
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void searchDiaryFulltextShouldUseLikeWhenIndexIsUnavailable() {
        IndexEngine mockedIndexEngine = mock(IndexEngine.class);
        when(mockedIndexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "图书馆"))
                .thenReturn(IndexSearchResult.unavailable());
        SearchServiceImpl indexedSearchService = new SearchServiceImpl(mockedIndexEngine, diaryMapper);
        DiaryFulltextSearchQuery query = new DiaryFulltextSearchQuery();
        query.setKeyword("图书馆");
        when(diaryMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<Diary>());

        indexedSearchService.searchDiaryFulltext(query);

        ArgumentCaptor<LambdaQueryWrapper<Diary>> wrapperCaptor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(diaryMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().toLowerCase().contains("content_text"));
        verify(mockedIndexEngine).findByContent(IndexNamespace.DIARY_CONTENT, "图书馆");
    }
}
