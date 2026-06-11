package com.trip.service.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trip.common.CommentTargetType;
import com.trip.common.ErrorCode;
import com.trip.entity.Diary;
import com.trip.entity.DiaryComment;
import com.trip.exception.BusinessException;
import com.trip.mapper.DiaryCommentMapper;
import com.trip.mapper.DiaryMapper;
import org.springframework.stereotype.Component;

/**
 * 日记评论表适配器。
 */
@Component
public class DiaryCommentHandler implements CommentTargetHandler {

    private static final int ENABLED_STATUS = 1;
    private static final String PUBLIC_VISIBILITY = "public";

    private final DiaryMapper diaryMapper;
    private final DiaryCommentMapper commentMapper;

    public DiaryCommentHandler(DiaryMapper diaryMapper, DiaryCommentMapper commentMapper) {
        this.diaryMapper = diaryMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public CommentTargetType targetType() {
        return CommentTargetType.DIARY;
    }

    @Override
    public void validateTarget(Long targetId) {
        Diary diary = diaryMapper.selectById(targetId);
        if (diary == null
                || diary.getStatus() == null
                || diary.getStatus() != ENABLED_STATUS
                || !PUBLIC_VISIBILITY.equals(diary.getVisibility())) {
            throw new BusinessException(ErrorCode.COMMENT_001);
        }
    }

    @Override
    public IPage<CommentRecord> listTopLevelComments(Long targetId, long pageNum, long pageSize) {
        IPage<DiaryComment> page = commentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<DiaryComment>()
                        .eq(DiaryComment::getDiaryId, targetId)
                        .isNull(DiaryComment::getParentCommentId)
                        .eq(DiaryComment::getStatus, ENABLED_STATUS)
                        .orderByDesc(DiaryComment::getCreatedAt)
                        .orderByDesc(DiaryComment::getId));
        return mapPage(page);
    }

    @Override
    public CommentRecord createComment(Long targetId, Long userId, String contentText) {
        DiaryComment comment = new DiaryComment();
        comment.setDiaryId(targetId);
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
                        new LambdaUpdateWrapper<DiaryComment>()
                                .eq(DiaryComment::getId, commentId)
                                .eq(DiaryComment::getStatus, expectedStatus)
                                .set(DiaryComment::getStatus, newStatus))
                == 1;
    }

    private IPage<CommentRecord> mapPage(IPage<DiaryComment> source) {
        Page<CommentRecord> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toRecord).toList());
        return result;
    }

    private CommentRecord toRecord(DiaryComment comment) {
        if (comment == null) {
            return null;
        }
        return new CommentRecord(
                comment.getId(),
                comment.getDiaryId(),
                comment.getUserId(),
                comment.getContentText(),
                comment.getStatus(),
                comment.getCreatedAt());
    }
}
