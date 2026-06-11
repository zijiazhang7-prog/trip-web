package com.trip.engine.index;

/**
 * 索引构建所需的最小文档，只保存业务记录 ID 和文本。
 */
public record IndexDocument(Long id, String text) {
}
