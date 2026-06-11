package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.common.CommentTargetType;
import com.trip.service.CommentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论统一删除入口。
 */
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @DeleteMapping("/{commentType}/{commentId}")
    public ApiResponse<Boolean> deleteComment(
            @PathVariable String commentType,
            @PathVariable Long commentId) {
        return ApiResponse.success(commentService.deleteComment(
                CommentTargetType.fromPath(commentType),
                commentId));
    }
}
