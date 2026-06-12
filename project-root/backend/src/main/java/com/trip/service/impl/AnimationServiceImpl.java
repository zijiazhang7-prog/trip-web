package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.common.ErrorCode;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.DiaryAnimation;
import com.trip.entity.DiaryMedia;
import com.trip.entity.User;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryAnimationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.DiaryMediaMapper;
import com.trip.mapper.UserMapper;
import com.trip.model.ai.AnimationGenerationInput;
import com.trip.model.ai.AnimationGenerationResult;
import com.trip.model.ai.AnimationMediaInput;
import com.trip.model.ai.AnimationScene;
import com.trip.model.ai.AnimationScript;
import com.trip.security.JwtClaims;
import com.trip.service.AIService;
import com.trip.service.AnimationService;
import com.trip.vo.response.DiaryAnimationVO;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class AnimationServiceImpl implements AnimationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnimationServiceImpl.class);
    private static final int ENABLED_STATUS = 1;
    private static final String VISIBILITY_PRIVATE = "private";
    private static final String MEDIA_TYPE_IMAGE = "image";
    private static final String STATUS_READY = "ready";
    private static final Set<String> MOTIONS =
            Set.of("zoom_in", "zoom_out", "pan_left", "pan_right");
    private static final Set<String> TRANSITIONS =
            Set.of("fade", "dissolve", "slide");

    private final DiaryMapper diaryMapper;
    private final DiaryMediaMapper diaryMediaMapper;
    private final DestinationMapper destinationMapper;
    private final UserMapper userMapper;
    private final DiaryAnimationMapper diaryAnimationMapper;
    private final AIService aiService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public AnimationServiceImpl(
            DiaryMapper diaryMapper,
            DiaryMediaMapper diaryMediaMapper,
            DestinationMapper destinationMapper,
            UserMapper userMapper,
            DiaryAnimationMapper diaryAnimationMapper,
            AIService aiService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate) {
        this.diaryMapper = diaryMapper;
        this.diaryMediaMapper = diaryMediaMapper;
        this.destinationMapper = destinationMapper;
        this.userMapper = userMapper;
        this.diaryAnimationMapper = diaryAnimationMapper;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public DiaryAnimationVO generate(Long diaryId) {
        validateDiaryId(diaryId);
        User currentUser = currentActiveUser();
        GenerationContext context = transactionTemplate.execute(
                status -> loadGenerationContext(diaryId, currentUser.getId()));
        if (context == null) {
            throw new BusinessException(ErrorCode.AI_002);
        }

        AnimationGenerationResult result;
        try {
            result = aiService.generateDiaryAnimation(context.input());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Diary animation generation failed for diaryId={}, provider error type={}",
                    diaryId,
                    exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.AI_002);
        }
        validateResult(result, context.imageMedia());
        DiaryAnimationVO saved = transactionTemplate.execute(
                status -> persistGeneratedAnimation(
                        diaryId,
                        currentUser.getId(),
                        context.imageMedia(),
                        result));
        if (saved == null) {
            throw new BusinessException(ErrorCode.AI_002);
        }
        return saved;
    }

    private GenerationContext loadGenerationContext(Long diaryId, Long userId) {
        Diary diary = diaryMapper.selectByIdForUpdate(diaryId);
        validateReadableDiary(diary);
        validateAuthor(diary, userId);
        List<DiaryMedia> imageMedia = selectImageMedia(diaryId);
        if (imageMedia.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_009);
        }
        Destination destination = destinationMapper.selectById(diary.getDestinationId());
        AnimationGenerationInput input = new AnimationGenerationInput(
                diary.getId(),
                diary.getTitle(),
                diary.getContentText(),
                destination == null ? null : destination.getName(),
                imageMedia.stream()
                        .map(media -> new AnimationMediaInput(
                                media.getId(),
                                media.getFileUrl(),
                                media.getSortNo()))
                        .toList());
        return new GenerationContext(input, List.copyOf(imageMedia));
    }

    private DiaryAnimationVO persistGeneratedAnimation(
            Long diaryId,
            Long userId,
            List<DiaryMedia> originalMedia,
            AnimationGenerationResult result) {
        Diary diary = diaryMapper.selectByIdForUpdate(diaryId);
        validateReadableDiary(diary);
        validateAuthor(diary, userId);
        List<DiaryMedia> currentMedia = selectImageMedia(diaryId);
        if (!sameMediaSnapshot(originalMedia, currentMedia)) {
            throw new BusinessException(ErrorCode.AI_002);
        }

        DiaryAnimation animation = new DiaryAnimation();
        animation.setDiaryId(diaryId);
        animation.setProvider(result.provider());
        animation.setAnimationTitle(result.title());
        animation.setNarrationText(result.narration());
        animation.setScriptJson(writeScript(result.script()));
        animation.setStatus(STATUS_READY);

        diaryAnimationMapper.upsertByDiaryId(animation);
        DiaryAnimation saved = selectAnimation(diaryId);
        if (saved == null) {
            throw new BusinessException(ErrorCode.AI_002);
        }
        return toVO(saved);
    }

    @Override
    public DiaryAnimationVO getByDiaryId(Long diaryId) {
        validateDiaryId(diaryId);
        Diary diary = diaryMapper.selectById(diaryId);
        validateReadableDiary(diary);
        if (VISIBILITY_PRIVATE.equals(diary.getVisibility())) {
            User currentUser = currentActiveUser();
            if (!currentUser.getId().equals(diary.getUserId())) {
                throw new BusinessException(ErrorCode.AUTH_005);
            }
        }
        DiaryAnimation animation = selectAnimation(diaryId);
        if (animation == null) {
            throw new BusinessException(ErrorCode.AI_010);
        }
        return toVO(animation);
    }

    private List<DiaryMedia> selectImageMedia(Long diaryId) {
        return diaryMediaMapper.selectList(new LambdaQueryWrapper<DiaryMedia>()
                .eq(DiaryMedia::getDiaryId, diaryId)
                .eq(DiaryMedia::getMediaType, MEDIA_TYPE_IMAGE)
                .orderByAsc(DiaryMedia::getSortNo)
                .orderByAsc(DiaryMedia::getId));
    }

    private DiaryAnimation selectAnimation(Long diaryId) {
        return diaryAnimationMapper.selectOne(new LambdaQueryWrapper<DiaryAnimation>()
                .eq(DiaryAnimation::getDiaryId, diaryId));
    }

    private void validateResult(AnimationGenerationResult result, List<DiaryMedia> imageMedia) {
        if (result == null
                || !StringUtils.hasText(result.provider())
                || !StringUtils.hasText(result.title())
                || result.title().length() > 150
                || !StringUtils.hasText(result.narration())
                || result.script() == null
                || result.script().scenes() == null
                || result.script().scenes().isEmpty()) {
            throw new BusinessException(ErrorCode.AI_007);
        }

        Map<Long, String> allowedMedia = new HashMap<>();
        for (DiaryMedia media : imageMedia) {
            allowedMedia.put(media.getId(), media.getFileUrl());
        }
        Set<Long> usedMedia = new HashSet<>();
        int totalDuration = 0;
        for (int index = 0; index < result.script().scenes().size(); index++) {
            AnimationScene scene = result.script().scenes().get(index);
            if (scene == null
                    || scene.order() != index + 1
                    || !imageMedia.get(index).getId().equals(scene.mediaId())
                    || !allowedMedia.containsKey(scene.mediaId())
                    || !allowedMedia.get(scene.mediaId()).equals(scene.fileUrl())
                    || !usedMedia.add(scene.mediaId())
                    || !StringUtils.hasText(scene.visualDescription())
                    || scene.durationMs() < 1000
                    || scene.durationMs() > 10000
                    || !MOTIONS.contains(scene.motion())
                    || !TRANSITIONS.contains(scene.transition())
                    || !StringUtils.hasText(scene.subtitle())
                    || !StringUtils.hasText(scene.narration())) {
                throw new BusinessException(ErrorCode.AI_007);
            }
            totalDuration += scene.durationMs();
        }
        if (!"1.0".equals(result.script().schemaVersion())
                || !"16:9".equals(result.script().aspectRatio())
                || result.script().totalDurationMs() != totalDuration
                || usedMedia.size() != allowedMedia.size()) {
            throw new BusinessException(ErrorCode.AI_007);
        }
    }

    private String writeScript(AnimationScript script) {
        try {
            return objectMapper.writeValueAsString(script);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.AI_007);
        }
    }

    private DiaryAnimationVO toVO(DiaryAnimation animation) {
        try {
            AnimationScript script = objectMapper.readValue(animation.getScriptJson(), AnimationScript.class);
            return DiaryAnimationVO.from(animation, script);
        } catch (JsonProcessingException | RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.AI_007);
        }
    }

    private void validateDiaryId(Long diaryId) {
        if (diaryId == null || diaryId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
    }

    private void validateReadableDiary(Diary diary) {
        if (diary == null) {
            throw new BusinessException(ErrorCode.DIARY_003);
        }
        if (diary.getStatus() == null || diary.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.DIARY_011);
        }
    }

    private void validateAuthor(Diary diary, Long userId) {
        if (!userId.equals(diary.getUserId())) {
            throw new BusinessException(ErrorCode.AUTH_005);
        }
    }

    private boolean sameMediaSnapshot(
            List<DiaryMedia> originalMedia,
            List<DiaryMedia> currentMedia) {
        if (originalMedia.size() != currentMedia.size()) {
            return false;
        }
        for (int index = 0; index < originalMedia.size(); index++) {
            DiaryMedia original = originalMedia.get(index);
            DiaryMedia current = currentMedia.get(index);
            if (!original.getId().equals(current.getId())
                    || !original.getFileUrl().equals(current.getFileUrl())
                    || !java.util.Objects.equals(original.getSortNo(), current.getSortNo())) {
                return false;
            }
        }
        return true;
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

    private record GenerationContext(
            AnimationGenerationInput input,
            List<DiaryMedia> imageMedia) {
    }
}
