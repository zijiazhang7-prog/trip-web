package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.ErrorCode;
import com.trip.dto.query.DestinationQuery;
import com.trip.dto.query.FacilityQuery;
import com.trip.dto.query.FoodQuery;
import com.trip.dto.query.PlaceQuery;
import com.trip.entity.Destination;
import com.trip.entity.Facility;
import com.trip.entity.Food;
import com.trip.entity.Place;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.FacilityMapper;
import com.trip.mapper.FoodMapper;
import com.trip.mapper.PlaceMapper;
import com.trip.service.QueryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QueryServiceImpl implements QueryService {

    private static final int ENABLED_STATUS = 1;
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final DestinationMapper destinationMapper;
    private final PlaceMapper placeMapper;
    private final FacilityMapper facilityMapper;
    private final FoodMapper foodMapper;

    public QueryServiceImpl(
            DestinationMapper destinationMapper,
            PlaceMapper placeMapper,
            FacilityMapper facilityMapper,
            FoodMapper foodMapper) {
        this.destinationMapper = destinationMapper;
        this.placeMapper = placeMapper;
        this.facilityMapper = facilityMapper;
        this.foodMapper = foodMapper;
    }

    @Override
    public IPage<Destination> queryDestinations(DestinationQuery query) {
        DestinationQuery safeQuery = query == null ? new DestinationQuery() : query;
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<Destination>()
                .eq(Destination::getStatus, ENABLED_STATUS);

        String keyword = normalize(safeQuery.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(Destination::getName, keyword)
                    .or()
                    .like(Destination::getCategory, keyword)
                    .or()
                    .like(Destination::getCity, keyword)
                    .or()
                    .like(Destination::getDescription, keyword));
        }

        String type = normalize(safeQuery.getType());
        if (StringUtils.hasText(type)) {
            wrapper.eq(Destination::getType, type);
        }

        String category = normalize(safeQuery.getCategory());
        if (StringUtils.hasText(category)) {
            wrapper.eq(Destination::getCategory, category);
        }

        String theme = normalize(safeQuery.getTheme());
        if (StringUtils.hasText(theme)) {
            wrapper.like(Destination::getTagJson, theme);
        }

        wrapper.orderByDesc(Destination::getCreatedAt).orderByAsc(Destination::getId);
        return destinationMapper.selectPage(new Page<>(pageNum(safeQuery.getPageNum()), pageSize(safeQuery.getPageSize())), wrapper);
    }

    @Override
    public Destination getDestinationById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        Destination destination = destinationMapper.selectById(id);
        if (destination == null || destination.getStatus() == null || destination.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        return destination;
    }

    @Override
    public List<Place> queryPlaces(PlaceQuery query) {
        if (query == null || query.getDestinationId() == null || query.getDestinationId() <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        LambdaQueryWrapper<Place> wrapper = new LambdaQueryWrapper<Place>()
                .eq(Place::getDestinationId, query.getDestinationId());
        String placeType = normalize(query.getPlaceType());
        if (StringUtils.hasText(placeType)) {
            wrapper.eq(Place::getPlaceType, placeType);
        }
        wrapper.orderByAsc(Place::getId);
        return placeMapper.selectList(wrapper);
    }

    @Override
    public List<Facility> queryFacilities(FacilityQuery query) {
        if (query == null || query.getDestinationId() == null || query.getDestinationId() <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        LambdaQueryWrapper<Facility> wrapper = new LambdaQueryWrapper<Facility>()
                .eq(Facility::getDestinationId, query.getDestinationId())
                .eq(Facility::getStatus, ENABLED_STATUS);

        String facilityType = normalize(query.getFacilityType());
        if (StringUtils.hasText(facilityType)) {
            wrapper.eq(Facility::getFacilityType, facilityType);
        }

        String keyword = normalize(query.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(Facility::getName, keyword)
                    .or()
                    .like(Facility::getDescription, keyword));
        }
        wrapper.orderByAsc(Facility::getId);
        return facilityMapper.selectList(wrapper);
    }

    @Override
    public List<Food> queryFoods(FoodQuery query) {
        if (query == null || query.getDestinationId() == null || query.getDestinationId() <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<Food>()
                .eq(Food::getDestinationId, query.getDestinationId());

        if (query.getFacilityId() != null) {
            if (query.getFacilityId() <= 0) {
                throw new BusinessException(ErrorCode.COMMON_001);
            }
            wrapper.eq(Food::getFacilityId, query.getFacilityId());
        }

        String foodType = normalize(query.getFoodType());
        if (StringUtils.hasText(foodType)) {
            wrapper.eq(Food::getFoodType, foodType);
        }

        String keyword = normalize(query.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like(Food::getName, keyword)
                    .or()
                    .like(Food::getFoodType, keyword)
                    .or()
                    .like(Food::getShopName, keyword)
                    .or()
                    .like(Food::getDescription, keyword));
        }
        wrapper.orderByAsc(Food::getId);
        return foodMapper.selectList(wrapper);
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
