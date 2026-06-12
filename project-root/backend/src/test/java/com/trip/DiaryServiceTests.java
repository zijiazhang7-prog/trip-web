package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.ErrorCode;
import com.trip.dto.request.DiaryCreateRequest;
import com.trip.dto.request.DiaryFulltextSearchQuery;
import com.trip.dto.request.DiaryListQuery;
import com.trip.dto.request.DiaryMediaRequest;
import com.trip.dto.request.DiaryTitleSearchQuery;
import com.trip.engine.compression.CompressionEngine;
import com.trip.engine.index.IndexDocument;
import com.trip.engine.index.IndexNamespace;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.DiaryMedia;
import com.trip.entity.RouteHistory;
import com.trip.entity.User;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.DiaryMediaMapper;
import com.trip.mapper.RouteHistoryMapper;
import com.trip.mapper.UserMapper;
import com.trip.security.JwtClaims;
import com.trip.service.SearchService;
import com.trip.service.IndexMaintenanceService;
import com.trip.service.QueryService;
import com.trip.service.impl.DiaryServiceImpl;
import com.trip.vo.response.DiaryCreateResponse;
import com.trip.vo.response.DiaryVO;
import com.trip.vo.response.PageResultVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DiaryServiceTests {

    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final DiaryMediaMapper diaryMediaMapper = mock(DiaryMediaMapper.class);
    private final DestinationMapper destinationMapper = mock(DestinationMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final RouteHistoryMapper routeHistoryMapper = mock(RouteHistoryMapper.class);
    private final QueryService queryService = mock(QueryService.class);
    private final SearchService searchService = mock(SearchService.class);
    private final IndexMaintenanceService indexMaintenanceService = mock(IndexMaintenanceService.class);
    private final CompressionEngine compressionEngine = new CompressionEngine();
    private final DiaryServiceImpl diaryService = new DiaryServiceImpl(
            diaryMapper,
            diaryMediaMapper,
            destinationMapper,
            userMapper,
            routeHistoryMapper,
            queryService,
            searchService,
            indexMaintenanceService,
            compressionEngine);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDiaryShouldSaveDiaryAndMedia() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(destinationMapper.selectById(101L)).thenReturn(destination(101L));
        when(routeHistoryMapper.selectById(9001L)).thenReturn(routeHistory(9001L, 7L, 101L));
        when(diaryMapper.insert(any(Diary.class))).thenAnswer(invocation -> {
            Diary diary = invocation.getArgument(0);
            diary.setId(4001L);
            return 1;
        });
        when(diaryMediaMapper.insert(any(DiaryMedia.class))).thenReturn(1);

        DiaryCreateResponse response = diaryService.createDiary(createRequest());

        assertEquals(4001L, response.getDiaryId());

        ArgumentCaptor<Diary> diaryCaptor = ArgumentCaptor.forClass(Diary.class);
        verify(diaryMapper).insert(diaryCaptor.capture());
        Diary savedDiary = diaryCaptor.getValue();
        assertEquals(7L, savedDiary.getUserId());
        assertEquals(101L, savedDiary.getDestinationId());
        assertEquals(9001L, savedDiary.getRouteHistoryId());
        assertEquals("校园散步", savedDiary.getTitle());
        assertEquals("今天去了图书馆。", savedDiary.getContentText());
        assertNotNull(savedDiary.getContentCompressed());
        assertEquals(
                savedDiary.getContentText(),
                compressionEngine.decompress(savedDiary.getContentCompressed()));
        assertEquals("private", savedDiary.getVisibility());
        assertEquals(0L, savedDiary.getHeatScore());
        assertEquals(0, savedDiary.getRatingCount());

        ArgumentCaptor<DiaryMedia> mediaCaptor = ArgumentCaptor.forClass(DiaryMedia.class);
        verify(diaryMediaMapper).insert(mediaCaptor.capture());
        DiaryMedia savedMedia = mediaCaptor.getValue();
        assertEquals(4001L, savedMedia.getDiaryId());
        assertEquals("image", savedMedia.getMediaType());
        assertEquals("/files/diary/20260505/photo.jpg", savedMedia.getFileUrl());
        assertEquals("photo.jpg", savedMedia.getFileName());
        assertEquals(0, savedMedia.getSortNo());
        verify(indexMaintenanceService).invalidateAfterCommit(IndexNamespace.DIARY_TITLE);
        verify(indexMaintenanceService).removeAfterCommit(IndexNamespace.DIARY_CONTENT, 4001L);
    }

    @Test
    void createPublicDiaryShouldIncrementallyAddContentAfterCommit() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(destinationMapper.selectById(101L)).thenReturn(destination(101L));
        when(routeHistoryMapper.selectById(9001L)).thenReturn(routeHistory(9001L, 7L, 101L));
        when(diaryMapper.insert(any(Diary.class))).thenAnswer(invocation -> {
            Diary diary = invocation.getArgument(0);
            diary.setId(4002L);
            return 1;
        });
        when(diaryMediaMapper.insert(any(DiaryMedia.class))).thenReturn(1);
        DiaryCreateRequest request = createRequest();
        request.setVisibility("public");

        diaryService.createDiary(request);

        verify(indexMaintenanceService).invalidateAfterCommit(IndexNamespace.DIARY_TITLE);
        verify(indexMaintenanceService).upsertAfterCommit(
                IndexNamespace.DIARY_CONTENT,
                new IndexDocument(4002L, "今天去了图书馆。"));
    }

    @Test
    void createDiaryShouldKeepOriginalTextWhenCompressionFails() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(destinationMapper.selectById(101L)).thenReturn(destination(101L));
        when(routeHistoryMapper.selectById(9001L)).thenReturn(routeHistory(9001L, 7L, 101L));
        when(diaryMapper.insert(any(Diary.class))).thenAnswer(invocation -> {
            Diary diary = invocation.getArgument(0);
            diary.setId(4003L);
            return 1;
        });
        when(diaryMediaMapper.insert(any(DiaryMedia.class))).thenReturn(1);
        CompressionEngine failingEngine = mock(CompressionEngine.class);
        when(failingEngine.compress(any(String.class))).thenThrow(new IllegalStateException("test failure"));
        DiaryServiceImpl service = new DiaryServiceImpl(
                diaryMapper,
                diaryMediaMapper,
                destinationMapper,
                userMapper,
                routeHistoryMapper,
                queryService,
                searchService,
                indexMaintenanceService,
                failingEngine);

        service.createDiary(createRequest());

        ArgumentCaptor<Diary> diaryCaptor = ArgumentCaptor.forClass(Diary.class);
        verify(diaryMapper).insert(diaryCaptor.capture());
        assertEquals("今天去了图书馆。", diaryCaptor.getValue().getContentText());
        assertNull(diaryCaptor.getValue().getContentCompressed());
    }

    @Test
    void createDiaryShouldRejectMissingLogin() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> diaryService.createDiary(createRequest()));

        assertEquals(ErrorCode.AUTH_003, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void createDiaryShouldRejectMissingDestination() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(destinationMapper.selectById(101L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> diaryService.createDiary(createRequest()));

        assertEquals(ErrorCode.COMMON_003, exception.getErrorCode());
        verifyNoInteractions(routeHistoryMapper);
    }

    @Test
    void createDiaryShouldRejectRouteHistoryOwnedByAnotherUser() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(destinationMapper.selectById(101L)).thenReturn(destination(101L));
        when(routeHistoryMapper.selectById(9001L)).thenReturn(routeHistory(9001L, 8L, 101L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> diaryService.createDiary(createRequest()));

        assertEquals(ErrorCode.COMMON_003, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void createDiaryShouldRejectInvalidMediaType() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(destinationMapper.selectById(101L)).thenReturn(destination(101L));
        when(routeHistoryMapper.selectById(9001L)).thenReturn(routeHistory(9001L, 7L, 101L));

        DiaryCreateRequest request = createRequest();
        request.getMediaList().get(0).setMediaType("pdf");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> diaryService.createDiary(request));

        assertEquals(ErrorCode.COMMON_002, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void listDiariesShouldReturnPublicDiariesWithMedia() {
        Diary first = publicDiary(4001L, 7L, 101L, "第一篇", 20L);
        Diary second = publicDiary(4002L, 8L, 101L, "第二篇", 10L);
        Page<Diary> page = new Page<>(1, 10);
        page.setRecords(List.of(first, second));
        page.setTotal(2);
        when(diaryMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                media(5002L, 4002L, 1),
                media(5001L, 4001L, 0)));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeUser(7L), activeUser(8L)));
        when(destinationMapper.selectBatchIds(anyCollection())).thenReturn(List.of(destination(101L)));

        DiaryListQuery query = new DiaryListQuery();
        query.setSortBy("heat");
        PageResultVO<DiaryVO> result = diaryService.listDiaries(query);

        assertEquals(2, result.getTotal());
        assertEquals(List.of(4001L, 4002L), result.getList().stream().map(DiaryVO::getId).toList());
        assertEquals("diary_user_7", result.getList().get(0).getUsername());
        assertEquals("测试目的地", result.getList().get(0).getDestinationName());
        assertEquals(1, result.getList().get(0).getMediaList().size());
    }

    @Test
    void listDestinationDiariesShouldValidateDestinationAndPageSizeCap() {
        when(destinationMapper.selectById(101L)).thenReturn(destination(101L));
        Page<Diary> page = new Page<>(1, 100);
        page.setRecords(List.of());
        page.setTotal(0);
        when(diaryMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        DiaryListQuery query = new DiaryListQuery();
        query.setPageSize(200);

        PageResultVO<DiaryVO> result = diaryService.listDestinationDiaries(101L, query);

        assertEquals(100, result.getPageSize());
    }

    @Test
    void listDiariesShouldResolveDestinationKeywordAndKeepPagination() {
        when(queryService.queryDestinationIdsByNameKeyword("北邮")).thenReturn(List.of(101L, 102L));
        Page<Diary> page = new Page<>(2, 5);
        page.setRecords(List.of());
        page.setTotal(0);
        when(diaryMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        DiaryListQuery query = new DiaryListQuery();
        query.setDestinationKeyword(" 北邮 ");
        query.setSortBy("rating");
        query.setPageNum(2);
        query.setPageSize(5);

        PageResultVO<DiaryVO> result = diaryService.listDiaries(query);

        assertEquals(2, result.getPageNum());
        assertEquals(5, result.getPageSize());
        verify(queryService).queryDestinationIdsByNameKeyword("北邮");
        verify(diaryMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void listDiariesShouldReturnEmptyPageWhenDestinationKeywordHasNoMatch() {
        when(queryService.queryDestinationIdsByNameKeyword("不存在")).thenReturn(List.of());
        DiaryListQuery query = new DiaryListQuery();
        query.setDestinationKeyword("不存在");
        query.setPageNum(2);
        query.setPageSize(5);

        PageResultVO<DiaryVO> result = diaryService.listDiaries(query);

        assertEquals(0, result.getTotal());
        assertEquals(2, result.getPageNum());
        assertEquals(5, result.getPageSize());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void listDiariesShouldIntersectDestinationIdAndKeyword() {
        when(destinationMapper.selectById(101L)).thenReturn(destination(101L));
        when(queryService.queryDestinationIdsByNameKeyword("西湖")).thenReturn(List.of(102L));
        DiaryListQuery query = new DiaryListQuery();
        query.setDestinationId(101L);
        query.setDestinationKeyword("西湖");

        PageResultVO<DiaryVO> result = diaryService.listDiaries(query);

        assertEquals(0, result.getTotal());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void listDiariesShouldRejectUnsupportedSortBy() {
        DiaryListQuery query = new DiaryListQuery();
        query.setSortBy("unknown");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> diaryService.listDiaries(query));

        assertEquals(ErrorCode.COMMON_008, exception.getErrorCode());
        verifyNoInteractions(diaryMapper);
    }

    @Test
    void getDetailShouldIncrementHeatAndReturnRefreshedPublicDiary() {
        Diary diary = publicDiary(4001L, 7L, 101L, "公开日记", 20L);
        Diary refreshedDiary = publicDiary(4001L, 7L, 101L, "公开日记", 21L);
        when(diaryMapper.selectById(4001L)).thenReturn(diary, refreshedDiary);
        when(diaryMapper.incrementHeatScore(4001L)).thenReturn(1);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(destinationMapper.selectById(101L)).thenReturn(destination(101L));
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of(media(5001L, 4001L, 0)));

        DiaryVO result = diaryService.getDiaryDetail(4001L);

        assertEquals(4001L, result.getId());
        assertEquals("公开日记", result.getTitle());
        assertEquals(21L, result.getHeatScore());
        assertEquals(1, result.getMediaList().size());
        verify(diaryMapper).incrementHeatScore(4001L);
    }

    @Test
    void getDetailShouldAllowPrivateDiaryOwner() {
        setCurrentUser(7L);
        Diary diary = publicDiary(4001L, 7L, 101L, "私有日记", 3L);
        diary.setVisibility("private");
        Diary refreshedDiary = publicDiary(4001L, 7L, 101L, "私有日记", 4L);
        refreshedDiary.setVisibility("private");
        when(diaryMapper.selectById(4001L)).thenReturn(diary, refreshedDiary);
        when(diaryMapper.incrementHeatScore(4001L)).thenReturn(1);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(destinationMapper.selectById(101L)).thenReturn(destination(101L));
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        DiaryVO result = diaryService.getDiaryDetail(4001L);

        assertEquals("private", result.getVisibility());
        assertEquals(4L, result.getHeatScore());
    }

    @Test
    void getDetailShouldRejectPrivateDiaryOtherUser() {
        setCurrentUser(8L);
        Diary diary = publicDiary(4001L, 7L, 101L, "私有日记", 3L);
        diary.setVisibility("private");
        when(diaryMapper.selectById(4001L)).thenReturn(diary);
        when(userMapper.selectById(8L)).thenReturn(activeUser(8L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> diaryService.getDiaryDetail(4001L));

        assertEquals(ErrorCode.AUTH_005, exception.getErrorCode());
        verify(diaryMapper, org.mockito.Mockito.never()).incrementHeatScore(any());
    }

    @Test
    void getDetailShouldNotIncrementDisabledDiary() {
        Diary diary = publicDiary(4001L, 7L, 101L, "禁用日记", 3L);
        diary.setStatus(0);
        when(diaryMapper.selectById(4001L)).thenReturn(diary);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> diaryService.getDiaryDetail(4001L));

        assertEquals(ErrorCode.COMMON_003, exception.getErrorCode());
        verify(diaryMapper, org.mockito.Mockito.never()).incrementHeatScore(any());
    }

    @Test
    void getDetailShouldFailWhenAtomicIncrementDoesNotUpdateRow() {
        Diary diary = publicDiary(4001L, 7L, 101L, "公开日记", 3L);
        when(diaryMapper.selectById(4001L)).thenReturn(diary);
        when(diaryMapper.incrementHeatScore(4001L)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> diaryService.getDiaryDetail(4001L));

        assertEquals(ErrorCode.COMMON_006, exception.getErrorCode());
    }

    @Test
    void searchByTitleShouldReuseSearchServiceAndAssembleVOs() {
        Diary diary = publicDiary(4001L, 7L, 101L, "校园散步", 0L);
        Page<Diary> page = new Page<>(1, 10);
        page.setRecords(List.of(diary));
        page.setTotal(1);
        DiaryTitleSearchQuery query = new DiaryTitleSearchQuery();
        query.setTitle("校园");
        when(searchService.searchDiaryByTitle(query)).thenReturn(page);
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of(media(5001L, 4001L, 0)));
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeUser(7L)));
        when(destinationMapper.selectBatchIds(anyCollection())).thenReturn(List.of(destination(101L)));

        PageResultVO<DiaryVO> result = diaryService.searchByTitle(query);

        assertEquals(1, result.getTotal());
        assertEquals("校园散步", result.getList().get(0).getTitle());
        verify(searchService).searchDiaryByTitle(query);
    }

    @Test
    void searchFulltextShouldReuseSearchServiceAndAssembleVOs() {
        Diary diary = publicDiary(4002L, 8L, 101L, "图书馆", 0L);
        Page<Diary> page = new Page<>(1, 10);
        page.setRecords(List.of(diary));
        page.setTotal(1);
        DiaryFulltextSearchQuery query = new DiaryFulltextSearchQuery();
        query.setKeyword("图书馆");
        when(searchService.searchDiaryFulltext(query)).thenReturn(page);
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of(activeUser(8L)));
        when(destinationMapper.selectBatchIds(anyCollection())).thenReturn(List.of(destination(101L)));

        PageResultVO<DiaryVO> result = diaryService.searchFulltext(query);

        assertEquals(1, result.getTotal());
        assertEquals("图书馆", result.getList().get(0).getTitle());
        verify(searchService).searchDiaryFulltext(query);
    }

    private DiaryCreateRequest createRequest() {
        DiaryCreateRequest request = new DiaryCreateRequest();
        request.setDestinationId(101L);
        request.setRouteHistoryId(9001L);
        request.setTitle(" 校园散步 ");
        request.setContentText(" 今天去了图书馆。 ");
        request.setVisibility("PRIVATE");
        request.setMediaList(List.of(mediaRequest()));
        return request;
    }

    private DiaryMediaRequest mediaRequest() {
        DiaryMediaRequest request = new DiaryMediaRequest();
        request.setMediaType("IMAGE");
        request.setFileUrl("/files/diary/20260505/photo.jpg");
        request.setFileName("photo.jpg");
        request.setSortNo(null);
        return request;
    }

    private Diary publicDiary(Long id, Long userId, Long destinationId, String title, Long heatScore) {
        Diary diary = new Diary();
        diary.setId(id);
        diary.setUserId(userId);
        diary.setDestinationId(destinationId);
        diary.setTitle(title);
        diary.setContentText("正文");
        diary.setHeatScore(heatScore);
        diary.setRatingScore(new BigDecimal("4.50"));
        diary.setRatingCount(2);
        diary.setVisibility("public");
        diary.setStatus(1);
        diary.setCreatedAt(LocalDateTime.of(2026, 5, 5, 10, 0));
        return diary;
    }

    private DiaryMedia media(Long id, Long diaryId, Integer sortNo) {
        DiaryMedia media = new DiaryMedia();
        media.setId(id);
        media.setDiaryId(diaryId);
        media.setMediaType("image");
        media.setFileUrl("/files/diary/photo-" + id + ".jpg");
        media.setFileName("photo-" + id + ".jpg");
        media.setSortNo(sortNo);
        return media;
    }

    private Destination destination(Long id) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName("测试目的地");
        destination.setStatus(1);
        return destination;
    }

    private RouteHistory routeHistory(Long id, Long userId, Long destinationId) {
        RouteHistory history = new RouteHistory();
        history.setId(id);
        history.setUserId(userId);
        history.setDestinationId(destinationId);
        return history;
    }

    private User activeUser(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("diary_user_" + userId);
        user.setStatus(1);
        user.setRole("user");
        return user;
    }

    private void setCurrentUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtClaims(userId, "diary_user_" + userId, "user", 1L, 2L),
                null,
                List.of()));
    }
}
