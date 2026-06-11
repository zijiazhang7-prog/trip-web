package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.ErrorCode;
import com.trip.dto.query.DestinationQuery;
import com.trip.dto.query.FacilityQuery;
import com.trip.dto.query.FoodQuery;
import com.trip.dto.query.PlaceQuery;
import com.trip.engine.index.IndexEngine;
import com.trip.engine.index.IndexSearchResult;
import com.trip.engine.index.IndexNamespace;
import com.trip.entity.Destination;
import com.trip.entity.Facility;
import com.trip.entity.Food;
import com.trip.entity.Place;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.FacilityMapper;
import com.trip.mapper.FoodMapper;
import com.trip.mapper.PlaceMapper;
import com.trip.service.impl.QueryServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class QueryServiceTests {

    private final DestinationMapper destinationMapper = mock(DestinationMapper.class);
    private final PlaceMapper placeMapper = mock(PlaceMapper.class);
    private final FacilityMapper facilityMapper = mock(FacilityMapper.class);
    private final FoodMapper foodMapper = mock(FoodMapper.class);
    private final QueryServiceImpl queryService = new QueryServiceImpl(
            new IndexEngine(),
            destinationMapper,
            placeMapper,
            facilityMapper,
            foodMapper);

    @Test
    void queryDestinationsShouldUseDefaultPageWhenQueryIsNull() {
        when(destinationMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<Destination>());

        queryService.queryDestinations(null);

        ArgumentCaptor<Page<Destination>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(destinationMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertEquals(1, pageCaptor.getValue().getCurrent());
        assertEquals(10, pageCaptor.getValue().getSize());
    }

    @Test
    void queryDestinationsShouldLimitPageSize() {
        DestinationQuery query = new DestinationQuery();
        query.setPageNum(2);
        query.setPageSize(500);
        when(destinationMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<Destination>());

        queryService.queryDestinations(query);

        ArgumentCaptor<Page<Destination>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(destinationMapper).selectPage(pageCaptor.capture(), any(Wrapper.class));
        assertEquals(2, pageCaptor.getValue().getCurrent());
        assertEquals(100, pageCaptor.getValue().getSize());
    }

    @Test
    void queryDestinationsShouldTryNameIndexAndKeepMapperPagination() {
        IndexEngine mockedIndexEngine = mock(IndexEngine.class);
        when(mockedIndexEngine.findExact(IndexNamespace.DESTINATION_NAME, "西湖"))
                .thenReturn(IndexSearchResult.available(List.of(1L)));
        when(mockedIndexEngine.findByPrefix(IndexNamespace.DESTINATION_NAME, "西湖", 1000))
                .thenReturn(IndexSearchResult.available(List.of(1L, 2L)));
        when(destinationMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(new Page<Destination>());
        QueryServiceImpl indexedQueryService = new QueryServiceImpl(
                mockedIndexEngine,
                destinationMapper,
                placeMapper,
                facilityMapper,
                foodMapper);
        DestinationQuery query = new DestinationQuery();
        query.setKeyword("西湖");

        indexedQueryService.queryDestinations(query);

        verify(mockedIndexEngine).findExact(IndexNamespace.DESTINATION_NAME, "西湖");
        verify(mockedIndexEngine).findByPrefix(IndexNamespace.DESTINATION_NAME, "西湖", 1000);
        verify(destinationMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void getDestinationByIdShouldRejectInvalidId() {
        BusinessException exception = assertThrows(BusinessException.class, () -> queryService.getDestinationById(0L));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
    }

    @Test
    void getDestinationByIdShouldRejectMissingDestination() {
        when(destinationMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> queryService.getDestinationById(1L));

        assertEquals(ErrorCode.COMMON_003, exception.getErrorCode());
    }

    @Test
    void getDestinationByIdShouldRejectDisabledDestination() {
        Destination destination = new Destination();
        destination.setId(1L);
        destination.setStatus(0);
        when(destinationMapper.selectById(1L)).thenReturn(destination);

        BusinessException exception = assertThrows(BusinessException.class, () -> queryService.getDestinationById(1L));

        assertEquals(ErrorCode.COMMON_003, exception.getErrorCode());
    }

    @Test
    void queryPlacesShouldRequireDestinationId() {
        BusinessException exception = assertThrows(BusinessException.class, () -> queryService.queryPlaces(new PlaceQuery()));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
    }

    @Test
    void queryPlacesShouldCallMapperForValidQuery() {
        PlaceQuery query = new PlaceQuery();
        query.setDestinationId(1L);
        query.setPlaceType("building");
        when(placeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(new Place()));

        List<Place> places = queryService.queryPlaces(query);

        assertEquals(1, places.size());
        verify(placeMapper).selectList(any(Wrapper.class));
    }

    @Test
    void queryFacilitiesShouldRequireDestinationId() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> queryService.queryFacilities(new FacilityQuery()));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
    }

    @Test
    void queryFacilitiesShouldCallMapperForValidQuery() {
        FacilityQuery query = new FacilityQuery();
        query.setDestinationId(1L);
        query.setFacilityType("toilet");
        query.setKeyword("一层");
        when(facilityMapper.selectList(any(Wrapper.class))).thenReturn(List.of(new Facility()));

        List<Facility> facilities = queryService.queryFacilities(query);

        assertEquals(1, facilities.size());
        verify(facilityMapper).selectList(any(Wrapper.class));
    }

    @Test
    void queryFoodsShouldRequireDestinationId() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> queryService.queryFoods(new FoodQuery()));

        assertEquals(ErrorCode.COMMON_001, exception.getErrorCode());
    }

    @Test
    void queryFoodsShouldCallMapperForValidQuery() {
        FoodQuery query = new FoodQuery();
        query.setDestinationId(1L);
        query.setFacilityId(2L);
        query.setFoodType("面食");
        query.setKeyword("牛肉面");
        when(foodMapper.selectList(any(Wrapper.class))).thenReturn(List.of(new Food()));

        List<Food> foods = queryService.queryFoods(query);

        assertEquals(1, foods.size());
        verify(foodMapper).selectList(any(Wrapper.class));
    }
}
