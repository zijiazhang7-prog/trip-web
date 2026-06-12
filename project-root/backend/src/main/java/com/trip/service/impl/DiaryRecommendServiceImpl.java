package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.dto.request.DiaryRecommendQuery;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.DiaryMedia;
import com.trip.entity.User;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.DiaryMediaMapper;
import com.trip.mapper.UserMapper;
import com.trip.service.DiaryRecommendService;
import com.trip.service.RankService;
import com.trip.service.UserPreferenceService;
import com.trip.taxonomy.TagJsonParser;
import com.trip.vo.response.DiaryMediaVO;
import com.trip.vo.response.DiaryVO;
import com.trip.vo.response.PageResultVO;
import com.trip.vo.response.UserPreferenceVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于用户偏好、日记文本、目的地特征、浏览量和评分的日记 Top-K 推荐。
 */
@Service
public class DiaryRecommendServiceImpl implements DiaryRecommendService {

    private static final int ENABLED_STATUS = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_CANDIDATE_SIZE = 200;
    private static final int MAX_KEYWORD_LENGTH = 30;
    private static final String VISIBILITY_PUBLIC = "public";
    private static final String SORT_BY_INTEREST = "interest";
    private static final String SORT_BY_HEAT = "heat";
    private static final String SORT_BY_RATING = "rating";
    private static final BigDecimal INTEREST_WEIGHT = new BigDecimal("0.50");
    private static final BigDecimal HEAT_WEIGHT = new BigDecimal("0.30");
    private static final BigDecimal RATING_WEIGHT = new BigDecimal("0.20");
    private static final BigDecimal MAX_RATING = new BigDecimal("5");
    private static final BigDecimal MAX_PREFERENCE_LEVEL = new BigDecimal("5");
    private static final Pattern KEYWORD_SEPARATOR = Pattern.compile("[\\s,，、;；。.!！?？:：]+");
    private final DiaryMapper diaryMapper;
    private final DiaryMediaMapper diaryMediaMapper;
    private final DestinationMapper destinationMapper;
    private final UserMapper userMapper;
    private final RankService rankService;
    private final UserPreferenceService userPreferenceService;
    private final ObjectMapper objectMapper;

    public DiaryRecommendServiceImpl(
            DiaryMapper diaryMapper,
            DiaryMediaMapper diaryMediaMapper,
            DestinationMapper destinationMapper,
            UserMapper userMapper,
            RankService rankService,
            UserPreferenceService userPreferenceService,
            ObjectMapper objectMapper) {
        this.diaryMapper = diaryMapper;
        this.diaryMediaMapper = diaryMediaMapper;
        this.destinationMapper = destinationMapper;
        this.userMapper = userMapper;
        this.rankService = rankService;
        this.userPreferenceService = userPreferenceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResultVO<DiaryVO> recommendDiaries(DiaryRecommendQuery query) {
        DiaryRecommendQuery safeQuery = query == null ? new DiaryRecommendQuery() : query;
        String sortBy = normalizeSortBy(safeQuery.getSortBy());
        int topK = pageSize(safeQuery.getPageSize());
        List<Diary> diaries = loadCandidates();
        if (diaries.isEmpty()) {
            return PageResultVO.of(List.of(), 1, topK, 0, 0);
        }

        Map<Long, Destination> destinations = destinationsById(
                diaries.stream().map(Diary::getDestinationId).collect(Collectors.toSet()));
        List<RecommendationCandidate> candidates = diaries.stream()
                .map(diary -> new RecommendationCandidate(diary, destinations.get(diary.getDestinationId())))
                .toList();
        List<RecommendationCandidate> ranked = rankCandidates(candidates, sortBy, topK);
        List<Diary> rankedDiaries = ranked.stream().map(RecommendationCandidate::diary).toList();
        long total = diaries.size();
        long pages = (total + topK - 1L) / topK;
        return PageResultVO.of(assembleDiaryVOs(rankedDiaries, destinations), 1, topK, total, pages);
    }

    private List<Diary> loadCandidates() {
        return diaryMapper.selectList(new LambdaQueryWrapper<Diary>()
                .eq(Diary::getStatus, ENABLED_STATUS)
                .eq(Diary::getVisibility, VISIBILITY_PUBLIC)
                .orderByDesc(Diary::getCreatedAt)
                .orderByDesc(Diary::getId)
                .last("LIMIT " + MAX_CANDIDATE_SIZE));
    }

    private List<RecommendationCandidate> rankCandidates(
            List<RecommendationCandidate> candidates,
            String sortBy,
            int topK) {
        if (SORT_BY_HEAT.equals(sortBy)) {
            return rankService.topK(
                    candidates,
                    candidate -> BigDecimal.valueOf(nonNegativeHeat(candidate.diary())),
                    topK,
                    true);
        }
        if (SORT_BY_RATING.equals(sortBy)) {
            return rankService.topK(
                    candidates,
                    candidate -> normalizedRating(candidate.diary()),
                    topK,
                    true);
        }

        UserPreferenceVO preference = userPreferenceService.getCurrentPreference();
        Set<String> preferenceKeywords = preferenceKeywords(preference);
        if (preferenceKeywords.isEmpty()) {
            return rankService.topK(
                    candidates,
                    candidate -> BigDecimal.valueOf(nonNegativeHeat(candidate.diary())),
                    topK,
                    true);
        }

        long maxHeat = candidates.stream()
                .map(RecommendationCandidate::diary)
                .mapToLong(this::nonNegativeHeat)
                .max()
                .orElse(0L);
        return rankService.topK(
                candidates,
                candidate -> recommendationScore(candidate, preference, preferenceKeywords, maxHeat),
                topK,
                true);
    }

    /**
     * 数据结构：偏好词使用 Set 去重，候选列表交给 RankService 的 PriorityQueue 取 Top-K。
     * 复杂度：文本匹配约 O(n*p*L)，Top-K 为 O(n log k)，空间复杂度 O(n+p+k)。
     */
    private BigDecimal recommendationScore(
            RecommendationCandidate candidate,
            UserPreferenceVO preference,
            Set<String> preferenceKeywords,
            long maxHeat) {
        String searchableText = searchableText(candidate).toLowerCase(Locale.ROOT);
        long matched = preferenceKeywords.stream().filter(searchableText::contains).count();
        BigDecimal interestScore = BigDecimal.valueOf(matched)
                .divide(BigDecimal.valueOf(preferenceKeywords.size()), 8, RoundingMode.HALF_UP);
        BigDecimal heatScore = normalizedHeat(candidate.diary(), maxHeat)
                .multiply(hotPreferenceFactor(preference));
        BigDecimal ratingScore = normalizedRating(candidate.diary());
        return interestScore.multiply(INTEREST_WEIGHT)
                .add(heatScore.multiply(HEAT_WEIGHT))
                .add(ratingScore.multiply(RATING_WEIGHT));
    }

    private BigDecimal normalizedHeat(Diary diary, long maxHeat) {
        if (maxHeat <= 0) {
            return BigDecimal.ZERO;
        }
        double normalized = Math.log1p(nonNegativeHeat(diary)) / Math.log1p(maxHeat);
        return BigDecimal.valueOf(normalized);
    }

    private BigDecimal normalizedRating(Diary diary) {
        BigDecimal rating = diary.getRatingScore() == null ? BigDecimal.ZERO : diary.getRatingScore();
        if (rating.signum() < 0) {
            rating = BigDecimal.ZERO;
        } else if (rating.compareTo(MAX_RATING) > 0) {
            rating = MAX_RATING;
        }
        return rating.divide(MAX_RATING, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal hotPreferenceFactor(UserPreferenceVO preference) {
        if (preference == null || preference.getPreferHotLevel() == null) {
            return BigDecimal.ONE;
        }
        int level = Math.max(1, Math.min(5, preference.getPreferHotLevel()));
        return BigDecimal.valueOf(level).divide(MAX_PREFERENCE_LEVEL, 8, RoundingMode.HALF_UP);
    }

    private Set<String> preferenceKeywords(UserPreferenceVO preference) {
        if (preference == null) {
            return Set.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        if (preference.getPreferThemeList() != null) {
            preference.getPreferThemeList().forEach(value -> addKeyword(keywords, value));
        }
        addKeyword(keywords, preference.getTravelStyle());
        addKeyword(keywords, preference.getPreferFoodType());
        if (StringUtils.hasText(preference.getCustomPreferenceText())) {
            for (String value : KEYWORD_SEPARATOR.split(preference.getCustomPreferenceText())) {
                addKeyword(keywords, value);
            }
        }
        return keywords;
    }

    private void addKeyword(Set<String> keywords, String value) {
        String normalized = normalize(value);
        if (StringUtils.hasText(normalized) && normalized.length() <= MAX_KEYWORD_LENGTH) {
            keywords.add(normalized.toLowerCase(Locale.ROOT));
        }
    }

    private String searchableText(RecommendationCandidate candidate) {
        Diary diary = candidate.diary();
        Destination destination = candidate.destination();
        StringBuilder builder = new StringBuilder();
        appendText(builder, diary.getTitle());
        appendText(builder, diary.getContentText());
        if (destination != null) {
            appendText(builder, destination.getName());
            appendText(builder, destination.getType());
            appendText(builder, destination.getCategory());
            appendText(builder, destination.getCity());
            appendText(builder, destination.getDescription());
            for (String tag : TagJsonParser.parse(destination.getTagJson(), objectMapper)) {
                appendText(builder, tag);
            }
        }
        return builder.toString();
    }

    private void appendText(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(' ').append(value.trim());
        }
    }

    private long nonNegativeHeat(Diary diary) {
        return diary.getHeatScore() == null ? 0L : Math.max(0L, diary.getHeatScore());
    }

    private List<DiaryVO> assembleDiaryVOs(
            List<Diary> diaries,
            Map<Long, Destination> destinationMap) {
        if (diaries.isEmpty()) {
            return List.of();
        }
        List<Long> diaryIds = diaries.stream().map(Diary::getId).toList();
        Map<Long, List<DiaryMediaVO>> mediaMap = mediaByDiaryId(diaryIds);
        Map<Long, User> userMap = usersById(diaries.stream().map(Diary::getUserId).collect(Collectors.toSet()));
        return diaries.stream()
                .map(diary -> DiaryVO.from(
                        diary,
                        userMap.get(diary.getUserId()),
                        destinationMap.get(diary.getDestinationId()),
                        mediaMap.getOrDefault(diary.getId(), List.of())))
                .toList();
    }

    private Map<Long, List<DiaryMediaVO>> mediaByDiaryId(List<Long> diaryIds) {
        return diaryMediaMapper.selectList(new LambdaQueryWrapper<DiaryMedia>()
                        .in(DiaryMedia::getDiaryId, diaryIds)
                        .orderByAsc(DiaryMedia::getSortNo)
                        .orderByAsc(DiaryMedia::getId))
                .stream()
                .sorted(Comparator
                        .comparing((DiaryMedia media) -> media.getSortNo() == null ? 0 : media.getSortNo())
                        .thenComparing(media -> media.getId() == null ? 0L : media.getId()))
                .collect(Collectors.groupingBy(
                        DiaryMedia::getDiaryId,
                        Collectors.mapping(DiaryMediaVO::from, Collectors.toList())));
    }

    private Map<Long, Destination> destinationsById(Collection<Long> destinationIds) {
        if (destinationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Destination> destinationMap = new HashMap<>();
        for (Destination destination : destinationMapper.selectBatchIds(destinationIds)) {
            destinationMap.put(destination.getId(), destination);
        }
        return destinationMap;
    }

    private Map<Long, User> usersById(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> userMap = new HashMap<>();
        for (User user : userMapper.selectBatchIds(userIds)) {
            userMap.put(user.getId(), user);
        }
        return userMap;
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = normalize(sortBy);
        return StringUtils.hasText(normalized) ? normalized.toLowerCase(Locale.ROOT) : SORT_BY_INTEREST;
    }

    private int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private record RecommendationCandidate(Diary diary, Destination destination) {
    }
}
