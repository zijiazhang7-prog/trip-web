package com.trip;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.common.ErrorCode;
import com.trip.dto.query.DestinationQuery;
import com.trip.dto.query.PlaceQuery;
import com.trip.dto.request.DestinationPlacesQuery;
import com.trip.dto.request.DestinationRecommendQuery;
import com.trip.dto.request.DestinationSearchQuery;
import com.trip.entity.Destination;
import com.trip.entity.Place;
import com.trip.exception.BusinessException;
import com.trip.security.JwtClaims;
import com.trip.service.QueryService;
import com.trip.service.RankService;
import com.trip.service.UserPreferenceService;
import com.trip.service.impl.RankServiceImpl;
import com.trip.service.impl.RecommendServiceImpl;
import com.trip.taxonomy.TaxonomyConfiguration;
import com.trip.taxonomy.TaxonomyService;
import com.trip.vo.response.PageResultVO;
import com.trip.vo.response.UserPreferenceVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendServiceTests {

    private final QueryService queryService = mock(QueryService.class);
    private final RankService rankService = new RankServiceImpl();
    private final UserPreferenceService userPreferenceService = mock(UserPreferenceService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TaxonomyService taxonomyService = new TaxonomyService(
            new TaxonomyConfiguration().taxonomyCatalog(objectMapper),
            objectMapper);
    private final RecommendServiceImpl recommendService = new RecommendServiceImpl(
            queryService,
            rankService,
            userPreferenceService,
            objectMapper,
            taxonomyService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recommendShouldUseHeatWhenAnonymousAndSortByRecommend() {
        when(queryService.queryAllDestinations(any(DestinationQuery.class))).thenReturn(List.of(
                destination(1L, "文化校园", 80, 4.6, "[\"校园\"]"),
                destination(2L, "湖边景区", 95, 4.2, "[\"自然\"]"),
                destination(3L, "历史街区", 70, 4.9, "[\"人文\"]")));

        DestinationRecommendQuery query = new DestinationRecommendQuery();
        query.setSortBy("recommend");
        query.setTopK(2);

        PageResultVO<?> result = recommendService.recommendDestinations(query);

        assertEquals(List.of(2L, 1L), result.getList().stream()
                .map(item -> ((com.trip.vo.response.DestinationVO) item).getId())
                .toList());
        assertEquals(2, result.getPageSize());
    }

    @Test
    void recommendShouldUsePreferenceThemesWhenAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtClaims(9L, "alice", "user", 1L, 2L),
                null,
                List.of()));

        UserPreferenceVO preference = UserPreferenceVO.empty(9L);
        preference.setPreferThemeList(List.of("人文建筑型"));
        when(userPreferenceService.getCurrentPreference()).thenReturn(preference);
        when(queryService.queryAllDestinations(any(DestinationQuery.class))).thenReturn(List.of(
                destination(1L, "人文建筑馆", 70, 4.0, "[\"人文建筑型\"]"),
                destination(2L, "高热度广场", 80, 4.0, "[\"运动\"]")));

        DestinationRecommendQuery query = new DestinationRecommendQuery();
        query.setSortBy("recommend");
        query.setTopK(1);

        PageResultVO<?> result = recommendService.recommendDestinations(query);

        assertEquals(1L, ((com.trip.vo.response.DestinationVO) result.getList().get(0)).getId());
        verify(userPreferenceService).getCurrentPreference();
    }

    @Test
    void anonymousRequestTagsShouldAffectRecommendOrder() {
        when(queryService.queryAllDestinations(any(DestinationQuery.class))).thenReturn(List.of(
                destinationWithCategory(1L, "历史馆", "历史古迹", 20, 4.0, "历史建筑|网红打卡"),
                destinationWithCategory(2L, "热门广场", "城市公园", 90, 4.0, "休闲娱乐")));

        DestinationRecommendQuery query = new DestinationRecommendQuery();
        query.setSortBy("recommend");
        query.setDestType("历史人文");
        query.setInterestTags(List.of("历史文化"));
        query.setTopK(2);

        PageResultVO<?> result = recommendService.recommendDestinations(query);

        assertEquals(List.of(1L, 2L), destinationIds(result));
        com.trip.vo.response.DestinationVO first =
                (com.trip.vo.response.DestinationVO) result.getList().get(0);
        assertEquals("历史人文", first.getDestType());
        assertEquals(true, first.getInterestTags().contains("历史文化"));
        assertEquals(List.of("历史建筑", "网红打卡"), first.getTags());
    }

    @Test
    void heatSortShouldIgnoreRequestTags() {
        when(queryService.queryAllDestinations(any(DestinationQuery.class))).thenReturn(List.of(
                destinationWithCategory(1L, "历史馆", "历史古迹", 20, 4.0, "历史建筑"),
                destinationWithCategory(2L, "热门广场", "城市公园", 90, 4.0, "休闲")));

        DestinationRecommendQuery query = new DestinationRecommendQuery();
        query.setSortBy("heat");
        query.setInterestTags(List.of("历史文化"));
        query.setTopK(2);

        assertEquals(List.of(2L, 1L), destinationIds(recommendService.recommendDestinations(query)));
    }

    @Test
    void personalizedRecommendShouldSupportPagingWithoutTopK() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtClaims(9L, "alice", "user", 1L, 2L),
                null,
                List.of()));

        UserPreferenceVO preference = UserPreferenceVO.empty(9L);
        preference.setPreferThemeList(List.of("人文"));
        when(userPreferenceService.getCurrentPreference()).thenReturn(preference);
        when(queryService.queryAllDestinations(any(DestinationQuery.class))).thenReturn(List.of(
                destination(1L, "自然景区", 20, 4.0, "[\"自然\"]"),
                destination(2L, "人文校园", 10, 4.0, "[\"人文\"]"),
                destination(3L, "历史街区", 5, 4.0, "[\"历史\"]")));

        DestinationRecommendQuery query = new DestinationRecommendQuery();
        query.setSortBy("recommend");
        query.setPageNum(1);
        query.setPageSize(2);

        PageResultVO<?> result = recommendService.recommendDestinations(query);

        assertEquals(List.of(2L, 3L), destinationIds(result));
        assertEquals(3, result.getTotal());
        assertEquals(2, result.getPages());
    }

    @Test
    void recommendShouldPaginateWithoutDuplicateIds() {
        when(queryService.queryAllDestinations(any(DestinationQuery.class))).thenReturn(List.of(
                destination(1L, "A", 50, 4.0, "[]"),
                destination(2L, "B", 40, 4.0, "[]"),
                destination(3L, "C", 30, 4.0, "[]"),
                destination(4L, "D", 20, 4.0, "[]"),
                destination(5L, "E", 10, 4.0, "[]")));

        DestinationRecommendQuery firstQuery = new DestinationRecommendQuery();
        firstQuery.setSortBy("heat");
        firstQuery.setPageNum(1);
        firstQuery.setPageSize(2);
        DestinationRecommendQuery secondQuery = new DestinationRecommendQuery();
        secondQuery.setSortBy("heat");
        secondQuery.setPageNum(2);
        secondQuery.setPageSize(2);

        PageResultVO<?> first = recommendService.recommendDestinations(firstQuery);
        PageResultVO<?> second = recommendService.recommendDestinations(secondQuery);

        assertEquals(List.of(1L, 2L), destinationIds(first));
        assertEquals(List.of(3L, 4L), destinationIds(second));
        assertEquals(1, first.getPageNum());
        assertEquals(2, second.getPageNum());
        assertEquals(2, first.getPageSize());
        assertEquals(5, first.getTotal());
        assertEquals(3, first.getPages());
    }

    @Test
    void recommendTopKShouldTakePriorityOverPaging() {
        when(queryService.queryAllDestinations(any(DestinationQuery.class))).thenReturn(List.of(
                destination(1L, "A", 10, 4.0, "[]"),
                destination(2L, "B", 30, 4.0, "[]"),
                destination(3L, "C", 20, 4.0, "[]")));

        DestinationRecommendQuery query = new DestinationRecommendQuery();
        query.setSortBy("heat");
        query.setTopK(2);
        query.setPageNum(3);
        query.setPageSize(1);

        PageResultVO<?> result = recommendService.recommendDestinations(query);

        assertEquals(List.of(2L, 3L), destinationIds(result));
        assertEquals(1, result.getPageNum());
        assertEquals(2, result.getPageSize());
        assertEquals(3, result.getTotal());
        assertEquals(2, result.getPages());
    }

    @Test
    void searchShouldRequireKeyword() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> recommendService.searchDestinations(new DestinationSearchQuery()));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
    }

    @Test
    void searchShouldRankCurrentPageByRating() {
        when(queryService.queryDestinations(any(DestinationQuery.class))).thenReturn(page(List.of(
                destination(1L, "A", 90, 4.1, "[]"),
                destination(2L, "B", 80, 4.9, "[]"))));

        DestinationSearchQuery query = new DestinationSearchQuery();
        query.setKeyword("校园");
        query.setSortBy("rating");

        PageResultVO<?> result = recommendService.searchDestinations(query);

        assertEquals(List.of(2L, 1L), result.getList().stream()
                .map(item -> ((com.trip.vo.response.DestinationVO) item).getId())
                .toList());
    }

    @Test
    void listDestinationPlacesShouldDelegateToQueryService() {
        Place place = new Place();
        place.setId(5L);
        place.setDestinationId(1L);
        place.setName("图书馆");
        place.setPlaceType("building");
        when(queryService.queryPlaces(any(PlaceQuery.class))).thenReturn(List.of(place));

        DestinationPlacesQuery query = new DestinationPlacesQuery();
        query.setPlaceType("building");

        assertEquals(1, recommendService.listDestinationPlaces(1L, query).size());

        ArgumentCaptor<PlaceQuery> captor = ArgumentCaptor.forClass(PlaceQuery.class);
        verify(queryService).queryPlaces(captor.capture());
        assertEquals(1L, captor.getValue().getDestinationId());
        assertEquals("building", captor.getValue().getPlaceType());
    }

    private Page<Destination> page(List<Destination> records) {
        Page<Destination> page = new Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(records.size());
        return page;
    }

    private Destination destination(Long id, String name, int heatScore, double ratingScore, String tagJson) {
        return destinationWithCategory(id, name, "校园", heatScore, ratingScore, tagJson);
    }

    private Destination destinationWithCategory(
            Long id,
            String name,
            String category,
            int heatScore,
            double ratingScore,
            String tagJson) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        destination.setType("campus");
        destination.setCategory(category);
        destination.setCity("北京");
        destination.setDescription("测试目的地");
        destination.setHeatScore(BigDecimal.valueOf(heatScore));
        destination.setRatingScore(BigDecimal.valueOf(ratingScore));
        destination.setTagJson(tagJson);
        destination.setCoverUrl("/files/destination/" + id + ".jpg");
        destination.setStatus(1);
        return destination;
    }

    private List<Long> destinationIds(PageResultVO<?> result) {
        return result.getList().stream()
                .map(item -> ((com.trip.vo.response.DestinationVO) item).getId())
                .toList();
    }
}
