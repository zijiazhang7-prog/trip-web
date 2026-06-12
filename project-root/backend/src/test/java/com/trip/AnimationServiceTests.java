package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import com.trip.model.ai.AnimationGenerationResult;
import com.trip.model.ai.AnimationScene;
import com.trip.model.ai.AnimationScript;
import com.trip.security.JwtClaims;
import com.trip.service.AIService;
import com.trip.service.impl.AnimationServiceImpl;
import com.trip.vo.response.DiaryAnimationVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class AnimationServiceTests {

    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final DiaryMediaMapper diaryMediaMapper = mock(DiaryMediaMapper.class);
    private final DestinationMapper destinationMapper = mock(DestinationMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final DiaryAnimationMapper diaryAnimationMapper = mock(DiaryAnimationMapper.class);
    private final AIService aiService = mock(AIService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AnimationServiceImpl service = new AnimationServiceImpl(
            diaryMapper,
            diaryMediaMapper,
            destinationMapper,
            userMapper,
            diaryAnimationMapper,
            aiService,
            objectMapper,
            transactionTemplate());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorShouldGenerateAndPersistAnimation() {
        prepareAuthorAndDiary();
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of(image(501L, 0)));
        when(destinationMapper.selectById(201L)).thenReturn(destination());
        when(aiService.generateDiaryAnimation(any())).thenReturn(validResult());
        when(diaryAnimationMapper.upsertByDiaryId(any(DiaryAnimation.class))).thenReturn(1);
        when(diaryAnimationMapper.selectOne(any(Wrapper.class))).thenReturn(storedAnimation());

        DiaryAnimationVO result = service.generate(101L);

        assertEquals(9001L, result.getId());
        assertEquals(101L, result.getDiaryId());
        assertEquals("mock-template", result.getProvider());
        assertEquals(1, result.getScript().getScenes().size());
        ArgumentCaptor<DiaryAnimation> captor = ArgumentCaptor.forClass(DiaryAnimation.class);
        verify(diaryAnimationMapper).upsertByDiaryId(captor.capture());
        assertEquals("ready", captor.getValue().getStatus());
        assertNotNull(captor.getValue().getScriptJson());
    }

    @Test
    void repeatedGenerationShouldUpdateExistingRow() {
        prepareAuthorAndDiary();
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of(image(501L, 0)));
        when(destinationMapper.selectById(201L)).thenReturn(destination());
        when(aiService.generateDiaryAnimation(any())).thenReturn(validResult());
        when(diaryAnimationMapper.upsertByDiaryId(any(DiaryAnimation.class))).thenReturn(0);
        when(diaryAnimationMapper.selectOne(any(Wrapper.class))).thenReturn(storedAnimation());

        DiaryAnimationVO result = service.generate(101L);

        assertEquals(9001L, result.getId());
        verify(diaryAnimationMapper).upsertByDiaryId(any(DiaryAnimation.class));
    }

    @Test
    void nonAuthorShouldNotGenerateForPublicDiary() {
        setCurrentUser(8L);
        when(userMapper.selectById(8L)).thenReturn(activeUser(8L));
        when(diaryMapper.selectByIdForUpdate(101L)).thenReturn(diary("public"));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.generate(101L));

        assertEquals(ErrorCode.AUTH_005, exception.getErrorCode());
        verifyNoInteractions(diaryMediaMapper, aiService, diaryAnimationMapper);
    }

    @Test
    void generationShouldRequireLogin() {
        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.generate(101L));

        assertEquals(ErrorCode.AUTH_003, exception.getErrorCode());
        verifyNoInteractions(diaryMapper, diaryMediaMapper, aiService, diaryAnimationMapper);
    }

    @Test
    void authorShouldGenerateForPrivateDiary() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(diaryMapper.selectByIdForUpdate(101L)).thenReturn(diary("private"));
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of(image(501L, 0)));
        when(destinationMapper.selectById(201L)).thenReturn(destination());
        when(aiService.generateDiaryAnimation(any())).thenReturn(validResult());
        when(diaryAnimationMapper.upsertByDiaryId(any(DiaryAnimation.class))).thenReturn(1);
        when(diaryAnimationMapper.selectOne(any(Wrapper.class))).thenReturn(storedAnimation());

        DiaryAnimationVO result = service.generate(101L);

        assertEquals(9001L, result.getId());
    }

    @Test
    void diaryWithoutImagesShouldBeRejected() {
        prepareAuthorAndDiary();
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.generate(101L));

        assertEquals(ErrorCode.AI_009, exception.getErrorCode());
        verifyNoInteractions(aiService, diaryAnimationMapper);
    }

    @Test
    void providerMustNotReferenceForeignMedia() {
        prepareAuthorAndDiary();
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of(image(501L, 0)));
        when(destinationMapper.selectById(201L)).thenReturn(destination());
        AnimationScene foreignScene = new AnimationScene(
                1,
                999L,
                "/files/diary/foreign.jpg",
                "foreign",
                4000,
                "zoom_in",
                "fade",
                "subtitle",
                "narration");
        when(aiService.generateDiaryAnimation(any())).thenReturn(new AnimationGenerationResult(
                "mock-template",
                "title",
                "narration",
                new AnimationScript("1.0", "16:9", 4000, "light-travel", List.of(foreignScene))));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.generate(101L));

        assertEquals(ErrorCode.AI_007, exception.getErrorCode());
        verifyNoInteractions(diaryAnimationMapper);
    }

    @Test
    void providerFailureShouldNotOverwriteExistingAnimation() {
        prepareAuthorAndDiary();
        when(diaryMediaMapper.selectList(any(Wrapper.class))).thenReturn(List.of(image(501L, 0)));
        when(destinationMapper.selectById(201L)).thenReturn(destination());
        when(aiService.generateDiaryAnimation(any())).thenThrow(new IllegalStateException("provider failed"));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.generate(101L));

        assertEquals(ErrorCode.AI_002, exception.getErrorCode());
        verifyNoInteractions(diaryAnimationMapper);
    }

    @Test
    void changedMediaSnapshotShouldNotPersistGeneratedAnimation() {
        prepareAuthorAndDiary();
        DiaryMedia original = image(501L, 0);
        DiaryMedia changed = image(502L, 0);
        when(diaryMediaMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(original), List.of(changed));
        when(destinationMapper.selectById(201L)).thenReturn(destination());
        when(aiService.generateDiaryAnimation(any())).thenReturn(validResult());

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.generate(101L));

        assertEquals(ErrorCode.AI_002, exception.getErrorCode());
        verify(diaryAnimationMapper, never()).upsertByDiaryId(any(DiaryAnimation.class));
    }

    @Test
    void publicAnimationShouldBeReadableWithoutLogin() {
        when(diaryMapper.selectById(101L)).thenReturn(diary("public"));
        when(diaryAnimationMapper.selectOne(any(Wrapper.class))).thenReturn(storedAnimation());

        DiaryAnimationVO result = service.getByDiaryId(101L);

        assertEquals(101L, result.getDiaryId());
        assertEquals(501L, result.getScript().getScenes().get(0).getMediaId());
        verifyNoInteractions(userMapper);
    }

    @Test
    void privateAnimationShouldRequireItsAuthor() {
        when(diaryMapper.selectById(101L)).thenReturn(diary("private"));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.getByDiaryId(101L));

        assertEquals(ErrorCode.AUTH_003, exception.getErrorCode());
        verifyNoInteractions(diaryAnimationMapper);
    }

    @Test
    void missingAnimationShouldReturnDedicatedError() {
        when(diaryMapper.selectById(101L)).thenReturn(diary("public"));
        when(diaryAnimationMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.getByDiaryId(101L));

        assertEquals(ErrorCode.AI_010, exception.getErrorCode());
    }

    private void prepareAuthorAndDiary() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(diaryMapper.selectByIdForUpdate(101L)).thenReturn(diary("public"));
    }

    private void setCurrentUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtClaims(userId, "animation_user_" + userId, "user", 1L, 2L),
                null,
                List.of()));
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setStatus(1);
        return user;
    }

    private Diary diary(String visibility) {
        Diary diary = new Diary();
        diary.setId(101L);
        diary.setUserId(7L);
        diary.setDestinationId(201L);
        diary.setTitle("校园漫步");
        diary.setContentText("今天沿着校园主路游览。");
        diary.setVisibility(visibility);
        diary.setStatus(1);
        return diary;
    }

    private DiaryMedia image(Long id, int sortNo) {
        DiaryMedia media = new DiaryMedia();
        media.setId(id);
        media.setDiaryId(101L);
        media.setMediaType("image");
        media.setFileUrl("/files/diary/photo-" + id + ".jpg");
        media.setSortNo(sortNo);
        return media;
    }

    private Destination destination() {
        Destination destination = new Destination();
        destination.setId(201L);
        destination.setName("北京邮电大学沙河校区");
        return destination;
    }

    private AnimationGenerationResult validResult() {
        AnimationScene scene = new AnimationScene(
                1,
                501L,
                "/files/diary/photo-501.jpg",
                "校园建筑和道路",
                4000,
                "zoom_in",
                "fade",
                "校园第一幕",
                "旅程从校园照片开始。");
        return new AnimationGenerationResult(
                "mock-template",
                "校园旅行动画",
                "一次校园旅行回顾。",
                new AnimationScript("1.0", "16:9", 4000, "light-travel", List.of(scene)));
    }

    private DiaryAnimation storedAnimation() {
        DiaryAnimation animation = new DiaryAnimation();
        animation.setId(9001L);
        animation.setDiaryId(101L);
        animation.setProvider("mock-template");
        animation.setAnimationTitle("校园旅行动画");
        animation.setNarrationText("一次校园旅行回顾。");
        animation.setScriptJson(writeScript(validResult().script()));
        animation.setStatus("ready");
        animation.setCreatedAt(LocalDateTime.now());
        animation.setUpdatedAt(LocalDateTime.now());
        return animation;
    }

    private String writeScript(AnimationScript script) {
        try {
            return objectMapper.writeValueAsString(script);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        });
    }
}
