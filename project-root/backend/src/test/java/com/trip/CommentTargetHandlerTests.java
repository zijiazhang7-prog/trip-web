package com.trip;

import com.trip.common.ErrorCode;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationCommentMapper;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryCommentMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.FoodCommentMapper;
import com.trip.mapper.FoodMapper;
import com.trip.service.comment.DestinationCommentHandler;
import com.trip.service.comment.DiaryCommentHandler;
import com.trip.service.comment.FoodCommentHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentTargetHandlerTests {

    @Test
    void disabledDestinationShouldNotAcceptComments() {
        DestinationMapper destinationMapper = mock(DestinationMapper.class);
        Destination destination = new Destination();
        destination.setStatus(0);
        when(destinationMapper.selectById(1L)).thenReturn(destination);
        DestinationCommentHandler handler =
                new DestinationCommentHandler(destinationMapper, mock(DestinationCommentMapper.class));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> handler.validateTarget(1L));

        assertEquals(ErrorCode.COMMENT_001, exception.getErrorCode());
    }

    @Test
    void missingFoodShouldNotAcceptComments() {
        FoodMapper foodMapper = mock(FoodMapper.class);
        FoodCommentHandler handler = new FoodCommentHandler(foodMapper, mock(FoodCommentMapper.class));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> handler.validateTarget(2L));

        assertEquals(ErrorCode.COMMENT_001, exception.getErrorCode());
    }

    @Test
    void privateOrDisabledDiaryShouldNotAcceptComments() {
        DiaryMapper diaryMapper = mock(DiaryMapper.class);
        Diary privateDiary = new Diary();
        privateDiary.setStatus(1);
        privateDiary.setVisibility("private");
        Diary disabledDiary = new Diary();
        disabledDiary.setStatus(0);
        disabledDiary.setVisibility("public");
        when(diaryMapper.selectById(3L)).thenReturn(privateDiary);
        when(diaryMapper.selectById(4L)).thenReturn(disabledDiary);
        DiaryCommentHandler handler =
                new DiaryCommentHandler(diaryMapper, mock(DiaryCommentMapper.class));

        BusinessException privateException =
                assertThrows(BusinessException.class, () -> handler.validateTarget(3L));
        BusinessException disabledException =
                assertThrows(BusinessException.class, () -> handler.validateTarget(4L));

        assertEquals(ErrorCode.COMMENT_001, privateException.getErrorCode());
        assertEquals(ErrorCode.COMMENT_001, disabledException.getErrorCode());
    }
}
