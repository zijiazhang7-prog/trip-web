package com.trip.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trip.common.CommentTargetType;
import com.trip.common.ErrorCode;
import com.trip.dto.request.CommentCreateRequest;
import com.trip.dto.request.CommentPageQuery;
import com.trip.entity.User;
import com.trip.exception.BusinessException;
import com.trip.mapper.UserMapper;
import com.trip.security.JwtClaims;
import com.trip.service.CommentService;
import com.trip.service.comment.CommentRecord;
import com.trip.service.comment.CommentTargetHandler;
import com.trip.vo.response.CommentVO;
import com.trip.vo.response.PageResultVO;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 评论公共业务实现，统一处理校验、鉴权、分页组装和软删除。
 */
@Service
public class CommentServiceImpl implements CommentService {

    private static final int ENABLED_STATUS = 1;
    private static final int HIDDEN_STATUS = 0;
    private static final int DELETED_STATUS = 2;
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final String ADMIN_ROLE = "admin";

    private final Map<CommentTargetType, CommentTargetHandler> handlers;
    private final UserMapper userMapper;

    public CommentServiceImpl(List<CommentTargetHandler> handlers, UserMapper userMapper) {
        this.handlers = new EnumMap<>(CommentTargetType.class);
        for (CommentTargetHandler handler : handlers) {
            this.handlers.put(handler.targetType(), handler);
        }
        this.userMapper = userMapper;
    }

    @Override
    public PageResultVO<CommentVO> listComments(
            CommentTargetType targetType,
            Long targetId,
            CommentPageQuery query) {
        CommentTargetHandler handler = handler(targetType);
        validateId(targetId);
        handler.validateTarget(targetId);

        CommentPageQuery safeQuery = query == null ? new CommentPageQuery() : query;
        IPage<CommentRecord> page = handler.listTopLevelComments(
                targetId,
                pageNum(safeQuery.getPageNum()),
                pageSize(safeQuery.getPageSize()));
        Map<Long, User> users = usersById(page.getRecords().stream()
                .map(CommentRecord::userId)
                .collect(Collectors.toSet()));
        List<CommentVO> comments = page.getRecords().stream()
                .map(comment -> toVO(targetType, comment, users.get(comment.userId())))
                .toList();
        return PageResultVO.of(
                comments,
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages());
    }

    @Override
    @Transactional
    public CommentVO createComment(
            CommentTargetType targetType,
            Long targetId,
            CommentCreateRequest request) {
        CommentTargetHandler handler = handler(targetType);
        validateId(targetId);
        String contentText = normalizeContent(request);
        User currentUser = currentActiveUser();
        handler.validateTarget(targetId);
        CommentRecord comment = handler.createComment(targetId, currentUser.getId(), contentText);
        if (comment == null) {
            throw new BusinessException(ErrorCode.COMMENT_005);
        }
        return toVO(targetType, comment, currentUser);
    }

    @Override
    @Transactional
    public boolean deleteComment(CommentTargetType targetType, Long commentId) {
        CommentTargetHandler handler = handler(targetType);
        validateId(commentId);
        User currentUser = currentActiveUser();
        CommentRecord comment = handler.getComment(commentId);
        if (comment == null || comment.status() == null || comment.status() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.COMMENT_002);
        }

        boolean administrator = ADMIN_ROLE.equalsIgnoreCase(currentUser.getRole());
        if (!administrator && !currentUser.getId().equals(comment.userId())) {
            throw new BusinessException(ErrorCode.COMMENT_004);
        }
        int newStatus = administrator ? HIDDEN_STATUS : DELETED_STATUS;
        if (!handler.updateStatus(commentId, ENABLED_STATUS, newStatus)) {
            throw new BusinessException(ErrorCode.COMMENT_005);
        }
        return true;
    }

    private CommentTargetHandler handler(CommentTargetType targetType) {
        CommentTargetHandler handler = targetType == null ? null : handlers.get(targetType);
        if (handler == null) {
            throw new BusinessException(ErrorCode.COMMENT_006);
        }
        return handler;
    }

    private String normalizeContent(CommentCreateRequest request) {
        String contentText = request == null ? null : request.getContentText();
        contentText = contentText == null ? null : contentText.trim();
        if (!StringUtils.hasText(contentText) || contentText.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.COMMENT_003);
        }
        return contentText;
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

    private Map<Long, User> usersById(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> users = new HashMap<>();
        for (User user : userMapper.selectBatchIds(userIds)) {
            users.put(user.getId(), user);
        }
        return users;
    }

    private CommentVO toVO(CommentTargetType targetType, CommentRecord comment, User user) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.id());
        vo.setTargetType(targetType.pathValue());
        vo.setTargetId(comment.targetId());
        vo.setUserId(comment.userId());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setAvatarUrl(user == null ? null : user.getAvatarUrl());
        vo.setContentText(comment.contentText());
        vo.setCreatedAt(comment.createdAt());
        return vo;
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }
    }

    private long pageNum(Integer pageNum) {
        return pageNum == null ? DEFAULT_PAGE_NUM : pageNum;
    }

    private long pageSize(Integer pageSize) {
        return pageSize == null ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
