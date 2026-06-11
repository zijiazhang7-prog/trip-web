package com.trip;

import com.trip.common.ErrorCode;
import com.trip.dto.query.FoodQuery;
import com.trip.dto.request.FoodRecommendQuery;
import com.trip.dto.request.FoodSearchQuery;
import com.trip.entity.Food;
import com.trip.exception.BusinessException;
import com.trip.service.QueryService;
import com.trip.service.impl.FoodServiceImpl;
import com.trip.service.impl.RankServiceImpl;
import com.trip.vo.response.FoodVO;
import com.trip.vo.response.PageResultVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FoodServiceTests {

    private final QueryService queryService = mock(QueryService.class);
    private final FoodServiceImpl foodService = new FoodServiceImpl(queryService, new RankServiceImpl());

    @Test
    void recommendShouldReturnTopKByHeat() {
        when(queryService.queryFoods(any(FoodQuery.class))).thenReturn(List.of(
                food(1L, "低热度面", "面食", 30, 4.8),
                food(2L, "高热度饭", "快餐", 90, 4.1),
                food(3L, "中热度汤", "汤品", 60, 4.5)));

        FoodRecommendQuery query = new FoodRecommendQuery();
        query.setDestinationId(101L);
        query.setSortBy("heat");
        query.setTopK(2);

        PageResultVO<FoodVO> result = foodService.recommendFoods(query);

        assertEquals(List.of(2L, 3L), result.getList().stream().map(FoodVO::getId).toList());
        assertEquals(2, result.getPageSize());
        assertEquals(3, result.getTotal());
        assertEquals(new BigDecimal("116.123456"), result.getList().get(0).getLng());
        assertEquals(new BigDecimal("40.123456"), result.getList().get(0).getLat());
    }

    @Test
    void recommendShouldPassFoodTypeAndFacilityIdToQueryService() {
        when(queryService.queryFoods(any(FoodQuery.class))).thenReturn(List.of());

        FoodRecommendQuery query = new FoodRecommendQuery();
        query.setDestinationId(101L);
        query.setFacilityId(20L);
        query.setFoodType(" 面食 ");

        foodService.recommendFoods(query);

        ArgumentCaptor<FoodQuery> captor = ArgumentCaptor.forClass(FoodQuery.class);
        verify(queryService).queryFoods(captor.capture());
        assertEquals(101L, captor.getValue().getDestinationId());
        assertEquals(20L, captor.getValue().getFacilityId());
        assertEquals("面食", captor.getValue().getFoodType());
    }

    @Test
    void searchShouldSortByRatingAndPage() {
        when(queryService.queryFoods(any(FoodQuery.class))).thenReturn(List.of(
                food(1L, "牛肉面", "面食", 80, 4.2),
                food(2L, "番茄面", "面食", 70, 4.9),
                food(3L, "炒面", "面食", 90, 4.5)));

        FoodSearchQuery query = new FoodSearchQuery();
        query.setDestinationId(101L);
        query.setKeyword("面");
        query.setSortBy("rating");
        query.setPageNum(2);
        query.setPageSize(1);

        PageResultVO<FoodVO> result = foodService.searchFoods(query);

        assertEquals(List.of(3L), result.getList().stream().map(FoodVO::getId).toList());
        assertEquals(3, result.getTotal());
        assertEquals(3, result.getPages());
    }

    @Test
    void searchShouldRequireKeywordOrFoodType() {
        FoodSearchQuery query = new FoodSearchQuery();
        query.setDestinationId(101L);

        BusinessException exception = assertThrows(BusinessException.class, () -> foodService.searchFoods(query));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
    }

    @Test
    void shouldRejectUnsupportedSortBy() {
        when(queryService.queryFoods(any(FoodQuery.class))).thenReturn(List.of(food(1L, "牛肉面", "面食", 80, 4.2)));
        FoodRecommendQuery query = new FoodRecommendQuery();
        query.setDestinationId(101L);
        query.setSortBy("distance");

        BusinessException exception = assertThrows(BusinessException.class, () -> foodService.recommendFoods(query));

        assertEquals(ErrorCode.COMMON_008, exception.getErrorCode());
    }

    private Food food(Long id, String name, String foodType, int heatScore, double ratingScore) {
        Food food = new Food();
        food.setId(id);
        food.setDestinationId(101L);
        food.setFacilityId(20L);
        food.setName(name);
        food.setFoodType(foodType);
        food.setShopName("第一食堂");
        food.setDescription("测试美食");
        food.setHeatScore(BigDecimal.valueOf(heatScore));
        food.setRatingScore(BigDecimal.valueOf(ratingScore));
        food.setAvgPrice(new BigDecimal("18.00"));
        food.setCoverUrl("/files/food/" + id + ".jpg");
        food.setLng(new BigDecimal("116.123456"));
        food.setLat(new BigDecimal("40.123456"));
        return food;
    }
}
