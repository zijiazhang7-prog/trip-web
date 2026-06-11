package com.trip.service;

import com.trip.common.CommentTargetType;
import com.trip.dto.request.CommentCreateRequest;
import com.trip.dto.request.CommentPageQuery;
import com.trip.vo.response.CommentVO;
import com.trip.vo.response.PageResultVO;

/**
 * 统一评论业务服务。
 */
public interface CommentService {

    PageResultVO<CommentVO> listComments(
            CommentTargetType targetType,
            Long targetId,
            CommentPageQuery query);

    CommentVO createComment(
            CommentTargetType targetType,
            Long targetId,
            CommentCreateRequest request);

    boolean deleteComment(CommentTargetType targetType, Long commentId);
}
