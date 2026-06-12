package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.dto.request.DiaryRecommendQuery;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.DiaryMediaMapper;
import com.trip.mapper.UserMapper;
import com.trip.service.UserPreferenceService;
import com.trip.service.impl.DiaryRecommendServiceImpl;
import com.trip.service.impl.RankServiceImpl;
import com.trip.vo.response.DiaryVO;
import com.trip.vo.response.PageResultVO;
import com.trip.vo.response.UserPreferenceVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DiaryRecommendServiceTests {

    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final DiaryMediaMapper diaryMediaMapper = mock(DiaryMediaMapper.class);
    private final DestinationMapper destinationMapper = mock(DestinationMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserPreferenceService userPreferenceService = mock(UserPreferenceService.class);
    private final DiaryRecommendServiceImpl service = new DiaryRecommendServiceImpl(
            diaryMapper,
            diaryMediaMapper,
            destinationMapper,
            userMapper,
            new RankServiceImpl(),
            userPreferenceService,
            new ObjectMapper());

    @BeforeEach
    void setUpAssociations() {
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectBatchIds(anyCollection())).thenReturn(List.of());
    }

    @Test
    void interestRecommendationShouldPreferMatchingDiary() {
        Diary historyDiary = diary(1L, 10L, 101L, "校园历史建筑", "参观校史馆", 0L, "0");
        Diary popularDiary = diary(2L, 11L, 102L, "热门散步", "操场散步", 100L, "4");
        when(diaryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(historyDiary, popularDiary));
        when(destinationMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                destination(101L, "历史校园", "campus", "历史文化"),
                destination(102L, "运动校园", "campus", "运动")));
        UserPreferenceVO preference = UserPreferenceVO.empty(9L);
        preference.setPreferThemeList(List.of("历史"));
        preference.setPreferHotLevel(5);
        when(userPreferenceService.getCurrentPreference()).thenReturn(preference);

        PageResultVO<DiaryVO> result = service.recommendDiaries(query("interest", 2));

        assertEquals(List.of(1L, 2L), result.getList().stream().map(DiaryVO::getId).toList());
        assertEquals(2, result.getTotal());
    }

    @Test
    void interestRecommendationShouldFallBackToHeatWithoutPreferenceKeywords() {
        Diary lowHeat = diary(1L, 10L, 101L, "低热度", "正文", 3L, "5");
        Diary highHeat = diary(2L, 11L, 102L, "高热度", "正文", 50L, "1");
        when(diaryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(lowHeat, highHeat));
        when(destinationMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                destination(101L, "A", "campus", "校园"),
                destination(102L, "B", "campus", "校园")));
        when(userPreferenceService.getCurrentPreference()).thenReturn(UserPreferenceVO.empty(9L));

        PageResultVO<DiaryVO> result = service.recommendDiaries(query("interest", 1));

        assertEquals(List.of(2L), result.getList().stream().map(DiaryVO::getId).toList());
        assertEquals(1, result.getPageSize());
        assertEquals(2, result.getPages());
    }

    @Test
    void ratingRecommendationShouldUseTopKAndKeepStableTieOrder() {
        Diary first = diary(1L, 10L, 101L, "第一篇", "正文", 1L, "4.8");
        Diary second = diary(2L, 11L, 102L, "第二篇", "正文", 100L, "4.8");
        Diary third = diary(3L, 12L, 103L, "第三篇", "正文", 200L, "3.0");
        when(diaryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(first, second, third));
        when(destinationMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                destination(101L, "A", "campus", "校园"),
                destination(102L, "B", "campus", "校园"),
                destination(103L, "C", "campus", "校园")));

        PageResultVO<DiaryVO> result = service.recommendDiaries(query("rating", 2));

        assertEquals(List.of(1L, 2L), result.getList().stream().map(DiaryVO::getId).toList());
    }

    @Test
    void interestRecommendationShouldHandleZeroHeatAndCustomPreferenceText() {
        Diary matched = diary(1L, 10L, 101L, null, "适合安静散步", 0L, null);
        Diary unmatched = diary(2L, 11L, 102L, null, null, 0L, null);
        when(diaryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(matched, unmatched));
        when(destinationMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                destination(101L, "湖边", null, null),
                destination(102L, "操场", null, null)));
        UserPreferenceVO preference = UserPreferenceVO.empty(9L);
        preference.setCustomPreferenceText("安静，散步");
        when(userPreferenceService.getCurrentPreference()).thenReturn(preference);

        PageResultVO<DiaryVO> result = service.recommendDiaries(query(null, 2));

        assertEquals(List.of(1L, 2L), result.getList().stream().map(DiaryVO::getId).toList());
    }

    @Test
    void recommendationShouldReturnEmptyPageWithoutCandidates() {
        when(diaryMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        PageResultVO<DiaryVO> result = service.recommendDiaries(query("interest", null));

        assertEquals(List.of(), result.getList());
        assertEquals(10, result.getPageSize());
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPages());
    }

    private DiaryRecommendQuery query(String sortBy, Integer pageSize) {
        DiaryRecommendQuery query = new DiaryRecommendQuery();
        query.setSortBy(sortBy);
        query.setPageSize(pageSize);
        return query;
    }

    private Diary diary(
            Long id,
            Long userId,
            Long destinationId,
            String title,
            String content,
            Long heat,
            String rating) {
        Diary diary = new Diary();
        diary.setId(id);
        diary.setUserId(userId);
        diary.setDestinationId(destinationId);
        diary.setTitle(title);
        diary.setContentText(content);
        diary.setHeatScore(heat);
        diary.setRatingScore(rating == null ? null : new BigDecimal(rating));
        diary.setRatingCount(0);
        diary.setVisibility("public");
        diary.setStatus(1);
        diary.setCreatedAt(LocalDateTime.of(2026, 6, 12, 12, 0).minusMinutes(id));
        return diary;
    }

    private Destination destination(Long id, String name, String type, String category) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        destination.setType(type);
        destination.setCategory(category);
        destination.setStatus(1);
        return destination;
    }
}
