package com.trip.service.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.CommentTargetType;
import com.trip.common.ErrorCode;
import com.trip.entity.Destination;
import com.trip.entity.DestinationComment;
import com.trip.exception.BusinessException;
import com.trip.mapper.DestinationCommentMapper;
import com.trip.mapper.DestinationMapper;
import org.springframework.stereotype.Component;

/**
 * 目的地评论表适配器。
 */
@Component
public class DestinationCommentHandler implements CommentTargetHandler {

    private static final int ENABLED_STATUS = 1;

    private final DestinationMapper destinationMapper;
    private final DestinationCommentMapper commentMapper;

    public DestinationCommentHandler(
            DestinationMapper destinationMapper,
            DestinationCommentMapper commentMapper) {
        this.destinationMapper = destinationMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public CommentTargetType targetType() {
        return CommentTargetType.DESTINATION;
    }

    @Override
    public void validateTarget(Long targetId) {
        Destination destination = destinationMapper.selectById(targetId);
        if (destination == null
                || destination.getStatus() == null
                || destination.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.COMMENT_001);
        }
    }

    @Override
    public IPage<CommentRecord> listTopLevelComments(Long targetId, long pageNum, long pageSize) {
        IPage<DestinationComment> page = commentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<DestinationComment>()
                        .eq(DestinationComment::getDestinationId, targetId)
                        .isNull(DestinationComment::getParentCommentId)
                        .eq(DestinationComment::getStatus, ENABLED_STATUS)
                        .orderByDesc(DestinationComment::getCreatedAt)
                        .orderByDesc(DestinationComment::getId));
        return mapPage(page);
    }

    @Override
    public CommentRecord createComment(Long targetId, Long userId, String contentText) {
        DestinationComment comment = new DestinationComment();
        comment.setDestinationId(targetId);
        comment.setUserId(userId);
        comment.setContentText(contentText);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(ENABLED_STATUS);
        if (commentMapper.insert(comment) != 1 || comment.getId() == null) {
            throw new BusinessException(ErrorCode.COMMENT_005);
        }
        return toRecord(commentMapper.selectById(comment.getId()));
    }

    @Override
    public CommentRecord getComment(Long commentId) {
        return toRecord(commentMapper.selectById(commentId));
    }

    @Override
    public boolean updateStatus(Long commentId, int expectedStatus, int newStatus) {
        return commentMapper.update(
                        null,
                        new LambdaUpdateWrapper<DestinationComment>()
                                .eq(DestinationComment::getId, commentId)
                                .eq(DestinationComment::getStatus, expectedStatus)
                                .set(DestinationComment::getStatus, newStatus))
                == 1;
    }

    private IPage<CommentRecord> mapPage(IPage<DestinationComment> source) {
        Page<CommentRecord> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toRecord).toList());
        return result;
    }

    private CommentRecord toRecord(DestinationComment comment) {
        if (comment == null) {
            return null;
        }
        return new CommentRecord(
                comment.getId(),
                comment.getDestinationId(),
                comment.getUserId(),
                comment.getContentText(),
                comment.getStatus(),
                comment.getCreatedAt());
    }
}
