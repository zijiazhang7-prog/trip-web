package com.trip;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.CommentTargetType;
import com.trip.common.ErrorCode;
import com.trip.dto.request.CommentCreateRequest;
import com.trip.dto.request.CommentPageQuery;
import com.trip.entity.User;
import com.trip.exception.BusinessException;
import com.trip.mapper.UserMapper;
import com.trip.security.JwtClaims;
import com.trip.service.comment.CommentRecord;
import com.trip.service.comment.CommentTargetHandler;
import com.trip.service.impl.CommentServiceImpl;
import com.trip.vo.response.CommentVO;
import com.trip.vo.response.PageResultVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTests {

    private final CommentTargetHandler destinationHandler = handler(CommentTargetType.DESTINATION);
    private final CommentTargetHandler foodHandler = handler(CommentTargetType.FOOD);
    private final CommentTargetHandler diaryHandler = handler(CommentTargetType.DIARY);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final CommentServiceImpl service = new CommentServiceImpl(
            List.of(destinationHandler, foodHandler, diaryHandler),
            userMapper);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listShouldUseTargetHandlerAndAssembleUserFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 11, 10, 0);
        CommentRecord record = new CommentRecord(10L, 3L, 7L, "评论", 1, createdAt);
        IPage<CommentRecord> page = new Page<CommentRecord>(1, 10, 1).setRecords(List.of(record));
        when(destinationHandler.listTopLevelComments(3L, 1, 10)).thenReturn(page);
        when(userMapper.selectBatchIds(java.util.Set.of(7L))).thenReturn(List.of(activeUser(7L, "user")));

        PageResultVO<CommentVO> result =
                service.listComments(CommentTargetType.DESTINATION, 3L, new CommentPageQuery());

        assertEquals(1, result.getTotal());
        assertEquals("destination", result.getList().get(0).getTargetType());
        assertEquals("用户7", result.getList().get(0).getNickname());
        verify(destinationHandler).validateTarget(3L);
    }

    @Test
    void createShouldTrimContentAndUseCurrentUser() {
        setCurrentUser(7L, "user");
        User user = activeUser(7L, "user");
        when(userMapper.selectById(7L)).thenReturn(user);
        when(diaryHandler.createComment(9L, 7L, "正文"))
                .thenReturn(new CommentRecord(20L, 9L, 7L, "正文", 1, LocalDateTime.now()));

        CommentVO result = service.createComment(CommentTargetType.DIARY, 9L, request("  正文  "));

        assertEquals(20L, result.getId());
        assertEquals("正文", result.getContentText());
        verify(diaryHandler).validateTarget(9L);
    }

    @Test
    void blankOrTooLongContentShouldBeRejectedBeforeTargetAccess() {
        BusinessException blank = assertThrows(
                BusinessException.class,
                () -> service.createComment(CommentTargetType.FOOD, 2L, request("   ")));
        BusinessException tooLong = assertThrows(
                BusinessException.class,
                () -> service.createComment(CommentTargetType.FOOD, 2L, request("a".repeat(501))));

        assertEquals(ErrorCode.COMMENT_003, blank.getErrorCode());
        assertEquals(ErrorCode.COMMENT_003, tooLong.getErrorCode());
        verify(foodHandler, never()).validateTarget(2L);
    }

    @Test
    void ownerDeleteShouldSetDeletedStatus() {
        setCurrentUser(7L, "user");
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L, "user"));
        when(foodHandler.getComment(30L))
                .thenReturn(new CommentRecord(30L, 2L, 7L, "正文", 1, LocalDateTime.now()));
        when(foodHandler.updateStatus(30L, 1, 2)).thenReturn(true);

        assertTrue(service.deleteComment(CommentTargetType.FOOD, 30L));
        verify(foodHandler).updateStatus(30L, 1, 2);
    }

    @Test
    void administratorDeleteShouldHideAnyComment() {
        setCurrentUser(8L, "admin");
        when(userMapper.selectById(8L)).thenReturn(activeUser(8L, "admin"));
        when(diaryHandler.getComment(31L))
                .thenReturn(new CommentRecord(31L, 9L, 7L, "正文", 1, LocalDateTime.now()));
        when(diaryHandler.updateStatus(31L, 1, 0)).thenReturn(true);

        assertTrue(service.deleteComment(CommentTargetType.DIARY, 31L));
        verify(diaryHandler).updateStatus(31L, 1, 0);
    }

    @Test
    void normalUserCannotDeleteAnotherUsersComment() {
        setCurrentUser(8L, "user");
        when(userMapper.selectById(8L)).thenReturn(activeUser(8L, "user"));
        when(destinationHandler.getComment(32L))
                .thenReturn(new CommentRecord(32L, 3L, 7L, "正文", 1, LocalDateTime.now()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.deleteComment(CommentTargetType.DESTINATION, 32L));

        assertEquals(ErrorCode.COMMENT_004, exception.getErrorCode());
        verify(destinationHandler, never()).updateStatus(32L, 1, 2);
    }

    @Test
    void unknownCommentTypeShouldBeRejected() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.listComments(null, 1L, new CommentPageQuery()));

        assertEquals(ErrorCode.COMMENT_006, exception.getErrorCode());
    }

    private CommentTargetHandler handler(CommentTargetType targetType) {
        CommentTargetHandler handler = mock(CommentTargetHandler.class);
        when(handler.targetType()).thenReturn(targetType);
        return handler;
    }

    private CommentCreateRequest request(String content) {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContentText(content);
        return request;
    }

    private User activeUser(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setNickname("用户" + id);
        user.setRole(role);
        user.setStatus(1);
        return user;
    }

    private void setCurrentUser(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtClaims(userId, "comment_user_" + userId, role, 1L, 2L),
                null,
                List.of()));
    }
}
