package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.trip.common.ErrorCode;
import com.trip.dto.request.DiaryRatingRequest;
import com.trip.entity.Diary;
import com.trip.entity.DiaryRating;
import com.trip.entity.User;
import com.trip.exception.BusinessException;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.DiaryRatingMapper;
import com.trip.mapper.UserMapper;
import com.trip.security.JwtClaims;
import com.trip.service.impl.DiaryRatingServiceImpl;
import com.trip.vo.response.DiaryRatingVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DiaryRatingServiceTests {

    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final DiaryRatingMapper diaryRatingMapper = mock(DiaryRatingMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final DiaryRatingServiceImpl service =
            new DiaryRatingServiceImpl(diaryMapper, diaryRatingMapper, userMapper);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void firstRatingShouldInsertAndRecalculateAggregate() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(diaryMapper.selectByIdForUpdate(101L)).thenReturn(publicDiary(101L));
        when(diaryRatingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(diaryRatingMapper.insert(any(DiaryRating.class))).thenReturn(1);
        when(diaryMapper.recalculateRating(101L)).thenReturn(1);

        boolean result = service.rateDiary(101L, request(5));

        assertTrue(result);
        ArgumentCaptor<DiaryRating> captor = ArgumentCaptor.forClass(DiaryRating.class);
        verify(diaryRatingMapper).insert(captor.capture());
        assertEquals(101L, captor.getValue().getDiaryId());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals(5, captor.getValue().getScore());
        verify(diaryMapper).recalculateRating(101L);
    }

    @Test
    void repeatedRatingShouldUpdateExistingRecord() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(diaryMapper.selectByIdForUpdate(101L)).thenReturn(publicDiary(101L));
        DiaryRating existing = rating(9001L, 101L, 7L, 5);
        when(diaryRatingMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(diaryRatingMapper.updateById(existing)).thenReturn(1);
        when(diaryMapper.recalculateRating(101L)).thenReturn(1);

        service.rateDiary(101L, request(3));

        assertEquals(3, existing.getScore());
        verify(diaryRatingMapper).updateById(existing);
        verify(diaryRatingMapper, never()).insert(any(DiaryRating.class));
    }

    @Test
    void aggregateFailureShouldRaiseDiaryRatingError() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(diaryMapper.selectByIdForUpdate(101L)).thenReturn(publicDiary(101L));
        when(diaryRatingMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(diaryRatingMapper.insert(any(DiaryRating.class))).thenReturn(1);
        when(diaryMapper.recalculateRating(101L)).thenReturn(0);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.rateDiary(101L, request(5)));

        assertEquals(ErrorCode.DIARY_006, exception.getErrorCode());
    }

    @Test
    void invalidScoreShouldBeRejectedBeforeDatabaseAccess() {
        BusinessException tooHigh =
                assertThrows(BusinessException.class, () -> service.rateDiary(101L, request(6)));
        BusinessException tooLow =
                assertThrows(BusinessException.class, () -> service.rateDiary(101L, request(0)));

        assertEquals(ErrorCode.DIARY_008, tooHigh.getErrorCode());
        assertEquals(ErrorCode.DIARY_008, tooLow.getErrorCode());
        verifyNoInteractions(diaryMapper, diaryRatingMapper, userMapper);
    }

    @Test
    void missingDiaryShouldBeRejected() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(diaryMapper.selectByIdForUpdate(101L)).thenReturn(null);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.rateDiary(101L, request(4)));

        assertEquals(ErrorCode.DIARY_003, exception.getErrorCode());
        verifyNoInteractions(diaryRatingMapper);
    }

    @Test
    void privateDiaryShouldBeRejected() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        Diary diary = publicDiary(101L);
        diary.setVisibility("private");
        when(diaryMapper.selectByIdForUpdate(101L)).thenReturn(diary);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.rateDiary(101L, request(4)));

        assertEquals(ErrorCode.DIARY_011, exception.getErrorCode());
        verifyNoInteractions(diaryRatingMapper);
    }

    @Test
    void getMyRatingShouldReturnAggregateAndNullWhenUserHasNotRated() {
        setCurrentUser(7L);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        Diary diary = publicDiary(101L);
        diary.setRatingScore(new BigDecimal("4.25"));
        diary.setRatingCount(4);
        when(diaryMapper.selectById(101L)).thenReturn(diary);
        when(diaryRatingMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        DiaryRatingVO result = service.getMyRating(101L);

        assertEquals(101L, result.getDiaryId());
        assertNull(result.getUserScore());
        assertEquals(new BigDecimal("4.25"), result.getRatingScore());
        assertEquals(4, result.getRatingCount());
    }

    private DiaryRatingRequest request(int score) {
        DiaryRatingRequest request = new DiaryRatingRequest();
        request.setScore(score);
        return request;
    }

    private Diary publicDiary(Long id) {
        Diary diary = new Diary();
        diary.setId(id);
        diary.setStatus(1);
        diary.setVisibility("public");
        diary.setRatingScore(BigDecimal.ZERO);
        diary.setRatingCount(0);
        return diary;
    }

    private DiaryRating rating(Long id, Long diaryId, Long userId, int score) {
        DiaryRating rating = new DiaryRating();
        rating.setId(id);
        rating.setDiaryId(diaryId);
        rating.setUserId(userId);
        rating.setScore(score);
        return rating;
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setStatus(1);
        return user;
    }

    private void setCurrentUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtClaims(userId, "rating_user_" + userId, "user", 1L, 2L),
                null,
                List.of()));
    }
}
