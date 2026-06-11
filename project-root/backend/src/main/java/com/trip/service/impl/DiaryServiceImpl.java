package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import com.trip.service.DiaryService;
import com.trip.service.IndexMaintenanceService;
import com.trip.service.SearchService;
import com.trip.vo.response.DiaryCreateResponse;
import com.trip.vo.response.DiaryMediaVO;
import com.trip.vo.response.DiaryVO;
import com.trip.vo.response.PageResultVO;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 图文日记基础实现，负责发布、列表、详情和目的地关联查询。
 */
@Service
public class DiaryServiceImpl implements DiaryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiaryServiceImpl.class);
    private static final int ENABLED_STATUS = 1;
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MEDIA_COUNT = 9;
    private static final String VISIBILITY_PUBLIC = "public";
    private static final String VISIBILITY_PRIVATE = "private";
    private static final String MEDIA_TYPE_IMAGE = "image";
    private static final String MEDIA_TYPE_VIDEO = "video";
    private static final String SORT_BY_LATEST = "latest";
    private static final String SORT_BY_HEAT = "heat";
    private static final String SORT_BY_RATING = "rating";

    private final DiaryMapper diaryMapper;
    private final DiaryMediaMapper diaryMediaMapper;
    private final DestinationMapper destinationMapper;
    private final UserMapper userMapper;
    private final RouteHistoryMapper routeHistoryMapper;
    private final SearchService searchService;
    private final IndexMaintenanceService indexMaintenanceService;
    private final CompressionEngine compressionEngine;

    public DiaryServiceImpl(
            DiaryMapper diaryMapper,
            DiaryMediaMapper diaryMediaMapper,
            DestinationMapper destinationMapper,
            UserMapper userMapper,
            RouteHistoryMapper routeHistoryMapper,
            SearchService searchService,
            IndexMaintenanceService indexMaintenanceService,
            CompressionEngine compressionEngine) {
        this.diaryMapper = diaryMapper;
        this.diaryMediaMapper = diaryMediaMapper;
        this.destinationMapper = destinationMapper;
        this.userMapper = userMapper;
        this.routeHistoryMapper = routeHistoryMapper;
        this.searchService = searchService;
        this.indexMaintenanceService = indexMaintenanceService;
        this.compressionEngine = compressionEngine;
    }

    @Override
    @Transactional
    public DiaryCreateResponse createDiary(DiaryCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        User currentUser = currentActiveUser();
        validateDestination(request.getDestinationId());
        validateRouteHistory(request.getRouteHistoryId(), currentUser.getId(), request.getDestinationId());

        String title = normalize(request.getTitle());
        String contentText = normalize(request.getContentText());
        if (!StringUtils.hasText(title) || !StringUtils.hasText(contentText)) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        String visibility = normalizeVisibility(request.getVisibility());
        List<DiaryMediaRequest> mediaRequests = validateMediaList(request.getMediaList());

        Diary diary = new Diary();
        diary.setUserId(currentUser.getId());
        diary.setDestinationId(request.getDestinationId());
        diary.setRouteHistoryId(request.getRouteHistoryId());
        diary.setTitle(title);
        diary.setContentText(contentText);
        diary.setContentCompressed(compressSafely(contentText));
        diary.setHeatScore(BigDecimal.ZERO);
        diary.setRatingScore(BigDecimal.ZERO);
        diary.setRatingCount(0);
        diary.setVisibility(visibility);
        diary.setStatus(ENABLED_STATUS);

        int inserted = diaryMapper.insert(diary);
        if (inserted != 1 || diary.getId() == null) {
            throw new BusinessException(ErrorCode.COMMON_006);
        }

        for (DiaryMediaRequest mediaRequest : mediaRequests) {
            DiaryMedia media = new DiaryMedia();
            media.setDiaryId(diary.getId());
            media.setMediaType(normalizeMediaType(mediaRequest.getMediaType()));
            media.setFileUrl(normalizeFileUrl(mediaRequest.getFileUrl()));
            media.setFileName(normalize(mediaRequest.getFileName()));
            media.setSortNo(mediaRequest.getSortNo() == null ? 0 : mediaRequest.getSortNo());
            diaryMediaMapper.insert(media);
        }

        indexMaintenanceService.invalidateAfterCommit(IndexNamespace.DIARY_TITLE);
        if (VISIBILITY_PUBLIC.equals(visibility)) {
            indexMaintenanceService.upsertAfterCommit(
                    IndexNamespace.DIARY_CONTENT,
                    new IndexDocument(diary.getId(), contentText));
        } else {
            indexMaintenanceService.removeAfterCommit(IndexNamespace.DIARY_CONTENT, diary.getId());
        }
        return new DiaryCreateResponse(diary.getId());
    }

    private byte[] compressSafely(String contentText) {
        try {
            return compressionEngine.compress(contentText).data();
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Diary content compression failed; original text will still be stored: {}",
                    exception.getClass().getSimpleName());
            return null;
        }
    }

    @Override
    public PageResultVO<DiaryVO> listDiaries(DiaryListQuery query) {
        DiaryListQuery safeQuery = query == null ? new DiaryListQuery() : query;
        if (safeQuery.getDestinationId() != null) {
            validateDestination(safeQuery.getDestinationId());
        }
        return pagePublicDiaries(safeQuery.getDestinationId(), safeQuery);
    }

    @Override
    public DiaryVO getDiaryDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        Diary diary = diaryMapper.selectById(id);
        if (diary == null || diary.getStatus() == null || diary.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
        if (VISIBILITY_PRIVATE.equals(diary.getVisibility())) {
            User currentUser = currentActiveUser();
            if (!currentUser.getId().equals(diary.getUserId())) {
                throw new BusinessException(ErrorCode.AUTH_005);
            }
        }
        return assembleDiaryVO(diary);
    }

    @Override
    public PageResultVO<DiaryVO> listDestinationDiaries(Long destinationId, DiaryListQuery query) {
        if (destinationId == null || destinationId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        validateDestination(destinationId);
        DiaryListQuery safeQuery = query == null ? new DiaryListQuery() : query;
        return pagePublicDiaries(destinationId, safeQuery);
    }

    @Override
    public PageResultVO<DiaryVO> searchByTitle(DiaryTitleSearchQuery query) {
        IPage<Diary> page = searchService.searchDiaryByTitle(query);
        return PageResultVO.of(
                assembleDiaryVOs(page.getRecords()),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    public PageResultVO<DiaryVO> searchFulltext(DiaryFulltextSearchQuery query) {
        IPage<Diary> page = searchService.searchDiaryFulltext(query);
        return PageResultVO.of(
                assembleDiaryVOs(page.getRecords()),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    private PageResultVO<DiaryVO> pagePublicDiaries(Long destinationId, DiaryListQuery query) {
        int pageNum = pageNum(query.getPageNum());
        int pageSize = pageSize(query.getPageSize());
        String sortBy = normalizeSortBy(query.getSortBy());

        LambdaQueryWrapper<Diary> wrapper = new LambdaQueryWrapper<Diary>()
                .eq(Diary::getStatus, ENABLED_STATUS)
                .eq(Diary::getVisibility, VISIBILITY_PUBLIC);
        if (destinationId != null) {
            wrapper.eq(Diary::getDestinationId, destinationId);
        }
        applySort(wrapper, sortBy);

        IPage<Diary> page = diaryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResultVO.of(
                assembleDiaryVOs(page.getRecords()),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    private void applySort(LambdaQueryWrapper<Diary> wrapper, String sortBy) {
        if (SORT_BY_HEAT.equals(sortBy)) {
            wrapper.orderByDesc(Diary::getHeatScore).orderByDesc(Diary::getCreatedAt);
        } else if (SORT_BY_RATING.equals(sortBy)) {
            wrapper.orderByDesc(Diary::getRatingScore).orderByDesc(Diary::getCreatedAt);
        } else {
            wrapper.orderByDesc(Diary::getCreatedAt);
        }
    }

    /**
     * 数据结构：用 Map 按 diaryId、userId、destinationId 分组关联数据。
     * 复杂度：当前页 n 篇日记、m 个媒体时，批量组装复杂度为 O(n + m)，空间复杂度 O(n + m)。
     */
    private List<DiaryVO> assembleDiaryVOs(List<Diary> diaries) {
        if (diaries == null || diaries.isEmpty()) {
            return List.of();
        }

        List<Long> diaryIds = diaries.stream().map(Diary::getId).toList();
        Map<Long, List<DiaryMediaVO>> mediaMap = mediaByDiaryId(diaryIds);
        Map<Long, User> userMap = usersById(diaries.stream().map(Diary::getUserId).collect(Collectors.toSet()));
        Map<Long, Destination> destinationMap = destinationsById(
                diaries.stream().map(Diary::getDestinationId).collect(Collectors.toSet()));

        return diaries.stream()
                .map(diary -> DiaryVO.from(
                        diary,
                        userMap.get(diary.getUserId()),
                        destinationMap.get(diary.getDestinationId()),
                        mediaMap.getOrDefault(diary.getId(), List.of())))
                .toList();
    }

    private DiaryVO assembleDiaryVO(Diary diary) {
        return DiaryVO.from(
                diary,
                userMapper.selectById(diary.getUserId()),
                destinationMapper.selectById(diary.getDestinationId()),
                mediaByDiaryId(List.of(diary.getId())).getOrDefault(diary.getId(), List.of()));
    }

    private Map<Long, List<DiaryMediaVO>> mediaByDiaryId(List<Long> diaryIds) {
        if (diaryIds.isEmpty()) {
            return Map.of();
        }
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

    private User currentActiveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtClaims claims)) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }
        User user = userMapper.selectById(claims.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_009);
        }
        if (user.getStatus() == null || user.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.AUTH_006);
        }
        return user;
    }

    private void validateDestination(Long destinationId) {
        if (destinationId == null || destinationId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        Destination destination = destinationMapper.selectById(destinationId);
        if (destination == null || destination.getStatus() == null || destination.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
    }

    private void validateRouteHistory(Long routeHistoryId, Long userId, Long destinationId) {
        if (routeHistoryId == null) {
            return;
        }
        RouteHistory history = routeHistoryMapper.selectById(routeHistoryId);
        if (history == null
                || !userId.equals(history.getUserId())
                || !destinationId.equals(history.getDestinationId())) {
            throw new BusinessException(ErrorCode.COMMON_003);
        }
    }

    private List<DiaryMediaRequest> validateMediaList(List<DiaryMediaRequest> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            return List.of();
        }
        if (mediaList.size() > MAX_MEDIA_COUNT) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        for (DiaryMediaRequest media : mediaList) {
            if (media == null) {
                throw new BusinessException(ErrorCode.COMMON_001);
            }
            normalizeMediaType(media.getMediaType());
            normalizeFileUrl(media.getFileUrl());
        }
        return mediaList;
    }

    private String normalizeMediaType(String mediaType) {
        String normalized = normalize(mediaType);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!Set.of(MEDIA_TYPE_IMAGE, MEDIA_TYPE_VIDEO).contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        return normalized;
    }

    private String normalizeFileUrl(String fileUrl) {
        String normalized = normalize(fileUrl);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
        if (!normalized.startsWith("/files/")) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        return normalized;
    }

    private String normalizeVisibility(String visibility) {
        String normalized = normalize(visibility);
        if (!StringUtils.hasText(normalized)) {
            return VISIBILITY_PUBLIC;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!Set.of(VISIBILITY_PUBLIC, VISIBILITY_PRIVATE).contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
        return normalized;
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = normalize(sortBy);
        if (!StringUtils.hasText(normalized)) {
            return SORT_BY_LATEST;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!Set.of(SORT_BY_LATEST, SORT_BY_HEAT, SORT_BY_RATING).contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_008);
        }
        return normalized;
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

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
