package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.trip.service.DiaryRatingService;
import com.trip.vo.response.DiaryRatingVO;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 日记评分服务，负责评分明细去重和日记评分聚合维护。
 */
@Service
public class DiaryRatingServiceImpl implements DiaryRatingService {

    private static final int ENABLED_STATUS = 1;
    private static final String VISIBILITY_PUBLIC = "public";
    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;

    private final DiaryMapper diaryMapper;
    private final DiaryRatingMapper diaryRatingMapper;
    private final UserMapper userMapper;

    public DiaryRatingServiceImpl(
            DiaryMapper diaryMapper,
            DiaryRatingMapper diaryRatingMapper,
            UserMapper userMapper) {
        this.diaryMapper = diaryMapper;
        this.diaryRatingMapper = diaryRatingMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public boolean rateDiary(Long diaryId, DiaryRatingRequest request) {
        validateDiaryId(diaryId);
        int score = validateScore(request);
        User currentUser = currentActiveUser();
        Diary diary = diaryMapper.selectByIdForUpdate(diaryId);
        validateRateableDiary(diary);

        DiaryRating rating = selectRating(diaryId, currentUser.getId());
        int affected;
        if (rating == null) {
            rating = new DiaryRating();
            rating.setDiaryId(diaryId);
            rating.setUserId(currentUser.getId());
            rating.setScore(score);
            affected = diaryRatingMapper.insert(rating);
        } else {
            rating.setScore(score);
            affected = diaryRatingMapper.updateById(rating);
        }
        if (affected != 1 || diaryMapper.recalculateRating(diaryId) != 1) {
            throw new BusinessException(ErrorCode.DIARY_006);
        }
        return true;
    }

    @Override
    public DiaryRatingVO getMyRating(Long diaryId) {
        validateDiaryId(diaryId);
        User currentUser = currentActiveUser();
        Diary diary = diaryMapper.selectById(diaryId);
        validateRateableDiary(diary);
        DiaryRating rating = selectRating(diaryId, currentUser.getId());
        return new DiaryRatingVO(
                diaryId,
                rating == null ? null : rating.getScore(),
                scoreOrZero(diary.getRatingScore()),
                countOrZero(diary.getRatingCount()));
    }

    private DiaryRating selectRating(Long diaryId, Long userId) {
        return diaryRatingMapper.selectOne(new LambdaQueryWrapper<DiaryRating>()
                .eq(DiaryRating::getDiaryId, diaryId)
                .eq(DiaryRating::getUserId, userId));
    }

    private void validateDiaryId(Long diaryId) {
        if (diaryId == null || diaryId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
    }

    private int validateScore(DiaryRatingRequest request) {
        if (request == null
                || request.getScore() == null
                || request.getScore() < MIN_SCORE
                || request.getScore() > MAX_SCORE) {
            throw new BusinessException(ErrorCode.DIARY_008);
        }
        return request.getScore();
    }

    private void validateRateableDiary(Diary diary) {
        if (diary == null) {
            throw new BusinessException(ErrorCode.DIARY_003);
        }
        if (diary.getStatus() == null
                || diary.getStatus() != ENABLED_STATUS
                || !VISIBILITY_PUBLIC.equals(diary.getVisibility())) {
            throw new BusinessException(ErrorCode.DIARY_011);
        }
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

    private BigDecimal scoreOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int countOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
