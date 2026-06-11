package com.trip.service.comment;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trip.common.CommentTargetType;

/**
 * 不同评论表的数据访问和目标校验适配器。
 */
public interface CommentTargetHandler {

    CommentTargetType targetType();

    void validateTarget(Long targetId);

    IPage<CommentRecord> listTopLevelComments(Long targetId, long pageNum, long pageSize);

    CommentRecord createComment(Long targetId, Long userId, String contentText);

    CommentRecord getComment(Long commentId);

    boolean updateStatus(Long commentId, int expectedStatus, int newStatus);
}
