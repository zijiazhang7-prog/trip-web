package com.trip.service.impl;

import com.trip.common.ErrorCode;
import com.trip.dto.query.FoodQuery;
import com.trip.dto.request.FoodRecommendQuery;
import com.trip.dto.request.FoodSearchQuery;
import com.trip.entity.Food;
import com.trip.exception.BusinessException;
import com.trip.service.FoodService;
import com.trip.service.QueryService;
import com.trip.service.RankService;
import com.trip.vo.response.FoodVO;
import com.trip.vo.response.PageResultVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Food 基础版：候选召回后按热度或评分排序，输出 Top-K 或分页结果。
 */
@Service
public class FoodServiceImpl implements FoodService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String SORT_BY_HEAT = "heat";
    private static final String SORT_BY_RATING = "rating";

    private final QueryService queryService;
    private final RankService rankService;

    public FoodServiceImpl(QueryService queryService, RankService rankService) {
        this.queryService = queryService;
        this.rankService = rankService;
    }

    @Override
    public PageResultVO<FoodVO> recommendFoods(FoodRecommendQuery query) {
        if (query == null || query.getDestinationId() == null || query.getDestinationId() <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        List<Food> candidates = queryService.queryFoods(toFoodQuery(query));
        int limit = topK(query.getTopK());
        List<Food> ranked = rankFoods(candidates, query.getSortBy(), limit);
        return PageResultVO.of(toFoodVOs(ranked), DEFAULT_PAGE_NUM, limit, candidates.size(), pages(candidates.size(), limit));
    }

    @Override
    public PageResultVO<FoodVO> searchFoods(FoodSearchQuery query) {
        if (query == null || query.getDestinationId() == null || query.getDestinationId() <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        if (!StringUtils.hasText(normalize(query.getKeyword())) && !StringUtils.hasText(normalize(query.getFoodType()))) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        List<Food> candidates = queryService.queryFoods(toFoodQuery(query));
        List<Food> ranked = rankFoods(candidates, query.getSortBy(), null);
        return page(ranked, pageNum(query.getPageNum()), pageSize(query.getPageSize()));
    }

    /**
     * 数据结构：候选美食列表承接召回结果，排序阶段复用 RankService 的比较器 / Top-K 堆。
     * 算法：先 O(n) 过滤召回，再按热度或评分排序；Top-K 场景由 RankService 控制堆大小。
     * 复杂度：全量排序 O(n log n)，Top-K 为 O(n log k)，空间复杂度 O(n) 或 O(k)。
     * 适用范围：适合 P1 基础演示和中小规模样例数据，全文检索后续交给 SearchService。
     */
    private List<Food> rankFoods(List<Food> candidates, String sortBy, Integer topK) {
        Function<Food, BigDecimal> scoreExtractor = switch (normalizeSortBy(sortBy)) {
            case SORT_BY_HEAT -> Food::getHeatScore;
            case SORT_BY_RATING -> Food::getRatingScore;
            default -> throw new BusinessException(ErrorCode.COMMON_008);
        };
        if (topK == null || topK <= 0) {
            return rankService.sortByScore(candidates, scoreExtractor, true);
        }
        return rankService.topK(candidates, scoreExtractor, topK, true);
    }

    private FoodQuery toFoodQuery(FoodRecommendQuery query) {
        FoodQuery foodQuery = new FoodQuery();
        foodQuery.setDestinationId(query.getDestinationId());
        foodQuery.setFacilityId(query.getFacilityId());
        foodQuery.setFoodType(normalize(query.getFoodType()));
        return foodQuery;
    }

    private FoodQuery toFoodQuery(FoodSearchQuery query) {
        FoodQuery foodQuery = new FoodQuery();
        foodQuery.setDestinationId(query.getDestinationId());
        foodQuery.setFoodType(normalize(query.getFoodType()));
        foodQuery.setKeyword(normalize(query.getKeyword()));
        return foodQuery;
    }

    private PageResultVO<FoodVO> page(List<Food> ranked, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, ranked.size());
        int toIndex = Math.min(fromIndex + pageSize, ranked.size());
        return PageResultVO.of(
                toFoodVOs(ranked.subList(fromIndex, toIndex)),
                pageNum,
                pageSize,
                ranked.size(),
                pages(ranked.size(), pageSize));
    }

    private List<FoodVO> toFoodVOs(List<Food> foods) {
        return foods.stream().map(FoodVO::from).toList();
    }

    private long pages(long total, long pageSize) {
        return total == 0 ? 0 : (total + pageSize - 1) / pageSize;
    }

    private int pageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int topK(Integer topK) {
        if (topK == null || topK < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(topK, MAX_PAGE_SIZE);
    }

    private String normalizeSortBy(String sortBy) {
        String normalizedSortBy = normalize(sortBy);
        if (!StringUtils.hasText(normalizedSortBy)) {
            return SORT_BY_HEAT;
        }
        normalizedSortBy = normalizedSortBy.toLowerCase(Locale.ROOT);
        if (SORT_BY_HEAT.equals(normalizedSortBy) || SORT_BY_RATING.equals(normalizedSortBy)) {
            return normalizedSortBy;
        }
        throw new BusinessException(ErrorCode.COMMON_008);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
