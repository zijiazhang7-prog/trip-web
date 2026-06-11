package com.trip.service.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.CommentTargetType;
import com.trip.common.ErrorCode;
import com.trip.entity.FoodComment;
import com.trip.exception.BusinessException;
import com.trip.mapper.FoodCommentMapper;
import com.trip.mapper.FoodMapper;
import org.springframework.stereotype.Component;

/**
 * 美食评论表适配器。
 */
@Component
public class FoodCommentHandler implements CommentTargetHandler {

    private static final int ENABLED_STATUS = 1;

    private final FoodMapper foodMapper;
    private final FoodCommentMapper commentMapper;

    public FoodCommentHandler(FoodMapper foodMapper, FoodCommentMapper commentMapper) {
        this.foodMapper = foodMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public CommentTargetType targetType() {
        return CommentTargetType.FOOD;
    }

    @Override
    public void validateTarget(Long targetId) {
        if (foodMapper.selectById(targetId) == null) {
            throw new BusinessException(ErrorCode.COMMENT_001);
        }
    }

    @Override
    public IPage<CommentRecord> listTopLevelComments(Long targetId, long pageNum, long pageSize) {
        IPage<FoodComment> page = commentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<FoodComment>()
                        .eq(FoodComment::getFoodId, targetId)
                        .isNull(FoodComment::getParentCommentId)
                        .eq(FoodComment::getStatus, ENABLED_STATUS)
                        .orderByDesc(FoodComment::getCreatedAt)
                        .orderByDesc(FoodComment::getId));
        return mapPage(page);
    }

    @Override
    public CommentRecord createComment(Long targetId, Long userId, String contentText) {
        FoodComment comment = new FoodComment();
        comment.setFoodId(targetId);
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
                        new LambdaUpdateWrapper<FoodComment>()
                                .eq(FoodComment::getId, commentId)
                                .eq(FoodComment::getStatus, expectedStatus)
                                .set(FoodComment::getStatus, newStatus))
                == 1;
    }

    private IPage<CommentRecord> mapPage(IPage<FoodComment> source) {
        Page<CommentRecord> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toRecord).toList());
        return result;
    }

    private CommentRecord toRecord(FoodComment comment) {
        if (comment == null) {
            return null;
        }
        return new CommentRecord(
                comment.getId(),
                comment.getFoodId(),
                comment.getUserId(),
                comment.getContentText(),
                comment.getStatus(),
                comment.getCreatedAt());
    }
}
