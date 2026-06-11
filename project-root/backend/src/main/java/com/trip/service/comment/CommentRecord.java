package com.trip.service.comment;

import java.time.LocalDateTime;

/**
 * 评论服务内部统一记录，不暴露数据库实体。
 */
public record CommentRecord(
        Long id,
        Long targetId,
        Long userId,
        String contentText,
        Integer status,
        LocalDateTime createdAt) {
}
