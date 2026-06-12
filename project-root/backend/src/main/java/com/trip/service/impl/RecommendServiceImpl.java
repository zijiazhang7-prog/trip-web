package com.trip.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
import com.trip.service.RecommendService;
import com.trip.service.UserPreferenceService;
import com.trip.taxonomy.ResolvedDestinationTags;
import com.trip.taxonomy.TagJsonParser;
import com.trip.taxonomy.TaxonomyService;
import com.trip.taxonomy.UserTagSelection;
import com.trip.vo.response.DestinationVO;
import com.trip.vo.response.PageResultVO;
import com.trip.vo.response.PlaceVO;
import com.trip.vo.response.UserPreferenceVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基础推荐实现：候选召回后使用排序或小根堆 Top-K 输出结果。
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String SORT_BY_HEAT = "heat";
    private static final String SORT_BY_RATING = "rating";
    private static final String SORT_BY_RECOMMEND = "recommend";
    private static final BigDecimal HEAT_WEIGHT = new BigDecimal("0.6");
    private static final BigDecimal RATING_WEIGHT = new BigDecimal("3.0");
    private static final BigDecimal PREFERENCE_MATCH_WEIGHT = new BigDecimal("10.0");
    private final QueryService queryService;
    private final RankService rankService;
    private final UserPreferenceService userPreferenceService;
    private final ObjectMapper objectMapper;
    private final TaxonomyService taxonomyService;

    public RecommendServiceImpl(
            QueryService queryService,
            RankService rankService,
            UserPreferenceService userPreferenceService,
            ObjectMapper objectMapper,
            TaxonomyService taxonomyService) {
        this.queryService = queryService;
        this.rankService = rankService;
        this.userPreferenceService = userPreferenceService;
        this.objectMapper = objectMapper;
        this.taxonomyService = taxonomyService;
    }

    @Override
    public PageResultVO<DestinationVO> recommendDestinations(DestinationRecommendQuery query) {
        DestinationRecommendQuery safeQuery = query == null ? new DestinationRecommendQuery() : query;

        DestinationQuery destinationQuery = new DestinationQuery();
        destinationQuery.setType(normalize(safeQuery.getType()));
        destinationQuery.setTheme(normalize(safeQuery.getTheme()));

        List<Destination> candidates = queryService.queryAllDestinations(destinationQuery);
        if (safeQuery.getTopK() != null && safeQuery.getTopK() > 0) {
            int limit = topK(safeQuery.getTopK());
            List<Destination> ranked = rankForRecommend(candidates, safeQuery, limit);
            return PageResultVO.of(
                    toDestinationVOs(ranked),
                    DEFAULT_PAGE_NUM,
                    limit,
                    candidates.size(),
                    pages(candidates.size(), limit));
        }

        int requestedPageNum = pageNum(safeQuery.getPageNum());
        int requestedPageSize = pageSize(safeQuery.getPageSize());
        List<Destination> ranked = rankForRecommend(candidates, safeQuery, null);
        return paginateDestinations(ranked, requestedPageNum, requestedPageSize);
    }

    @Override
    public PageResultVO<DestinationVO> searchDestinations(DestinationSearchQuery query) {
        if (query == null || !StringUtils.hasText(normalize(query.getKeyword()))) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        DestinationQuery destinationQuery = new DestinationQuery();
        destinationQuery.setKeyword(normalize(query.getKeyword()));
        destinationQuery.setType(normalize(query.getType()));
        destinationQuery.setCategory(normalize(query.getCategory()));
        destinationQuery.setPageNum(pageNum(query.getPageNum()));
        destinationQuery.setPageSize(pageSize(query.getPageSize()));

        IPage<Destination> page = queryService.queryDestinations(destinationQuery);
        List<Destination> ranked = rankService.rankDestinations(page.getRecords(), normalizeSortBy(query.getSortBy()), null);
        return PageResultVO.of(toDestinationVOs(ranked), page.getCurrent(), page.getSize(), page.getTotal(), page.getPages());
    }

    @Override
    public DestinationVO getDestinationDetail(Long id) {
        return toDestinationVO(queryService.getDestinationById(id));
    }

    @Override
    public List<PlaceVO> listDestinationPlaces(Long destinationId, DestinationPlacesQuery query) {
        if (destinationId == null || destinationId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        PlaceQuery placeQuery = new PlaceQuery();
        placeQuery.setDestinationId(destinationId);
        placeQuery.setPlaceType(query == null ? null : normalize(query.getPlaceType()));
        return queryService.queryPlaces(placeQuery).stream().map(PlaceVO::from).toList();
    }

    private List<Destination> rankForRecommend(
            List<Destination> candidates,
            DestinationRecommendQuery query,
            Integer topK) {
        String normalizedSortBy = normalizeRecommendSortBy(query.getSortBy());
        if (SORT_BY_RECOMMEND.equals(normalizedSortBy)) {
            UserTagSelection selection = taxonomyService.mergeSelection(query, currentPreference());
            if (selection.isEmpty()) {
                return rankService.rankDestinations(candidates, SORT_BY_HEAT, topK);
            }
            if (topK == null || topK <= 0) {
                return rankService.sortByScore(
                        candidates,
                        destination -> recommendScore(destination, selection),
                        true);
            }
            return rankService.topK(
                    candidates,
                    destination -> recommendScore(destination, selection),
                    topK,
                    true);
        }
        return rankService.rankDestinations(candidates, normalizedSortBy, topK);
    }

    private PageResultVO<DestinationVO> paginateDestinations(
            List<Destination> ranked,
            int pageNum,
            int pageSize) {
        int fromIndex = (int) Math.min((long) (pageNum - 1) * pageSize, ranked.size());
        int toIndex = Math.min(fromIndex + pageSize, ranked.size());
        return PageResultVO.of(
                toDestinationVOs(ranked.subList(fromIndex, toIndex)),
                pageNum,
                pageSize,
                ranked.size(),
                pages(ranked.size(), pageSize));
    }

    /**
     * 数据结构：候选列表保存目的地，偏好集合使用 HashSet，Top-K 由 RankService 的 PriorityQueue 完成。
     * 复杂度：每个目的地做一次固定字段和标签匹配，整体约 O(n*m)，Top-K 为 O(n log k)。
     */
    private BigDecimal recommendScore(Destination destination, UserTagSelection selection) {
        BigDecimal heatScore = scoreOf(destination.getHeatScore()).multiply(HEAT_WEIGHT);
        BigDecimal ratingScore = scoreOf(destination.getRatingScore()).multiply(RATING_WEIGHT);
        BigDecimal preferenceScore = BigDecimal.valueOf(
                        taxonomyService.scoreDestination(taxonomyService.resolve(destination), selection))
                .multiply(PREFERENCE_MATCH_WEIGHT);
        return heatScore.add(ratingScore).add(preferenceScore);
    }

    private UserPreferenceVO currentPreference() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtClaims)) {
            return null;
        }

        try {
            return userPreferenceService.getCurrentPreference();
        } catch (BusinessException exception) {
            return null;
        }
    }

    private List<DestinationVO> toDestinationVOs(List<Destination> destinations) {
        return destinations.stream().map(this::toDestinationVO).toList();
    }

    private DestinationVO toDestinationVO(Destination destination) {
        ResolvedDestinationTags resolved = taxonomyService.resolve(destination);
        DestinationVO vo = DestinationVO.from(
                destination,
                TagJsonParser.parse(destination.getTagJson(), objectMapper));
        vo.setDestType(resolved.destType());
        vo.setInterestTags(List.copyOf(resolved.interests()));
        return vo;
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
        return Math.min(topK, MAX_PAGE_SIZE);
    }

    private long pages(long total, long pageSize) {
        return total == 0 ? 0 : (total + pageSize - 1) / pageSize;
    }

    private String normalizeRecommendSortBy(String sortBy) {
        String normalizedSortBy = normalize(sortBy);
        if (!StringUtils.hasText(normalizedSortBy)) {
            return SORT_BY_RECOMMEND;
        }
        normalizedSortBy = normalizedSortBy.toLowerCase(Locale.ROOT);
        if (SORT_BY_HEAT.equals(normalizedSortBy)
                || SORT_BY_RATING.equals(normalizedSortBy)
                || SORT_BY_RECOMMEND.equals(normalizedSortBy)) {
            return normalizedSortBy;
        }
        throw new BusinessException(ErrorCode.COMMON_008);
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

    private BigDecimal scoreOf(BigDecimal score) {
        return score == null ? BigDecimal.ZERO : score;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
