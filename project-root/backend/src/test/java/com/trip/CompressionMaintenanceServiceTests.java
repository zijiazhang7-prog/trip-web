package com.trip;

import com.trip.config.CompressionProperties;
import com.trip.engine.compression.CompressionEngine;
import com.trip.entity.Diary;
import com.trip.mapper.DiaryMapper;
import com.trip.service.impl.CompressionMaintenanceServiceImpl;
import com.trip.service.model.CompressionMaintenanceResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompressionMaintenanceServiceTests {

    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final CompressionEngine compressionEngine = new CompressionEngine();
    private final CompressionProperties properties = new CompressionProperties();

    @Test
    void shouldBackfillMissingCompressedContentByIdCursor() {
        properties.setBatchSize(2);
        Diary first = diary(10L, "第一篇历史日记", null);
        Diary second = diary(20L, "第二篇历史日记", null);
        when(diaryMapper.selectCompressionBatchAfterId(0L, 2, false)).thenReturn(List.of(first, second));
        when(diaryMapper.selectCompressionBatchAfterId(20L, 2, false)).thenReturn(List.of());
        when(diaryMapper.updateCompressedIfMissing(any(), anyString(), any(byte[].class))).thenReturn(1);

        CompressionMaintenanceResult result = service().maintain();

        assertEquals(2, result.scannedCount());
        assertEquals(2, result.backfilledCount());
        assertEquals(0, result.failedCount());
        assertEquals(
                "第一篇历史日记".getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                        + "第二篇历史日记".getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                result.originalByteLength());
        InOrder order = inOrder(diaryMapper);
        order.verify(diaryMapper).selectCompressionBatchAfterId(0L, 2, false);
        order.verify(diaryMapper).updateCompressedIfMissing(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq("第一篇历史日记"),
                any(byte[].class));
        order.verify(diaryMapper).updateCompressedIfMissing(
                org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq("第二篇历史日记"),
                any(byte[].class));
        order.verify(diaryMapper).selectCompressionBatchAfterId(20L, 2, false);
    }

    @Test
    void shouldVerifyValidContentAndRepairDamagedOrMismatchedContent() {
        properties.setVerifyExisting(true);
        Diary valid = diary(
                1L,
                "完整压缩内容",
                compressionEngine.compress("完整压缩内容").data());
        Diary damaged = diary(2L, "需要修复的内容", new byte[] {1, 2, 3});
        Diary mismatched = diary(
                3L,
                "数据库中的新正文",
                compressionEngine.compress("压缩包中的旧正文").data());
        when(diaryMapper.selectCompressionBatchAfterId(0L, 100, true))
                .thenReturn(List.of(valid, damaged, mismatched));
        when(diaryMapper.selectCompressionBatchAfterId(3L, 100, true)).thenReturn(List.of());
        when(diaryMapper.updateCompressedIfContentUnchanged(any(), anyString(), any(byte[].class)))
                .thenReturn(1);

        CompressionMaintenanceResult result = service().maintain();

        assertEquals(3, result.scannedCount());
        assertEquals(1, result.verifiedCount());
        assertEquals(2, result.repairedCount());
        assertEquals(0, result.failedCount());
        verify(diaryMapper, never()).updateCompressedIfMissing(any(), anyString(), any(byte[].class));
        verify(diaryMapper).updateCompressedIfContentUnchanged(
                org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq("需要修复的内容"),
                any(byte[].class));
        verify(diaryMapper).updateCompressedIfContentUnchanged(
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq("数据库中的新正文"),
                any(byte[].class));
    }

    @Test
    void shouldContinueAfterSingleDiaryCompressionFailure() {
        CompressionEngine failingEngine = mock(CompressionEngine.class);
        when(failingEngine.compress("失败正文")).thenThrow(new IllegalStateException("test failure"));
        when(failingEngine.compress("正常正文")).thenReturn(compressionEngine.compress("正常正文"));
        when(diaryMapper.selectCompressionBatchAfterId(0L, 100, false))
                .thenReturn(List.of(diary(1L, "失败正文", null), diary(2L, "正常正文", null)));
        when(diaryMapper.selectCompressionBatchAfterId(2L, 100, false)).thenReturn(List.of());
        when(diaryMapper.updateCompressedIfMissing(2L, "正常正文", compressionEngine.compress("正常正文").data()))
                .thenReturn(1);

        CompressionMaintenanceResult result =
                new CompressionMaintenanceServiceImpl(diaryMapper, failingEngine, properties).maintain();

        assertEquals(2, result.scannedCount());
        assertEquals(1, result.backfilledCount());
        assertEquals(1, result.failedCount());
        verify(diaryMapper, never()).updateCompressedIfMissing(
                org.mockito.ArgumentMatchers.eq(1L),
                anyString(),
                any(byte[].class));
    }

    @Test
    void shouldSkipEmptyTextAndConcurrentUpdateConflict() {
        Diary empty = diary(1L, "", null);
        Diary conflicted = diary(2L, "并发修改前正文", null);
        when(diaryMapper.selectCompressionBatchAfterId(0L, 100, false))
                .thenReturn(List.of(empty, conflicted));
        when(diaryMapper.selectCompressionBatchAfterId(2L, 100, false)).thenReturn(List.of());
        when(diaryMapper.updateCompressedIfMissing(any(), anyString(), any(byte[].class))).thenReturn(0);

        CompressionMaintenanceResult result = service().maintain();

        assertEquals(2, result.scannedCount());
        assertEquals(0, result.backfilledCount());
        assertEquals(2, result.skippedCount());
    }

    private CompressionMaintenanceServiceImpl service() {
        return new CompressionMaintenanceServiceImpl(diaryMapper, compressionEngine, properties);
    }

    private Diary diary(Long id, String contentText, byte[] contentCompressed) {
        Diary diary = new Diary();
        diary.setId(id);
        diary.setContentText(contentText);
        diary.setContentCompressed(contentCompressed);
        return diary;
    }
}
