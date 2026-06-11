package com.trip.service.impl;

import com.trip.config.CompressionProperties;
import com.trip.engine.compression.CompressionEngine;
import com.trip.engine.compression.CompressionResult;
import com.trip.entity.Diary;
import com.trip.mapper.DiaryMapper;
import com.trip.service.CompressionMaintenanceService;
import com.trip.service.model.CompressionMaintenanceResult;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 使用主键游标分批维护历史日记的 Huffman 压缩副本。
 */
@Service
public class CompressionMaintenanceServiceImpl implements CompressionMaintenanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionMaintenanceServiceImpl.class);
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 1000;

    private final DiaryMapper diaryMapper;
    private final CompressionEngine compressionEngine;
    private final CompressionProperties properties;

    public CompressionMaintenanceServiceImpl(
            DiaryMapper diaryMapper,
            CompressionEngine compressionEngine,
            CompressionProperties properties) {
        this.diaryMapper = diaryMapper;
        this.compressionEngine = compressionEngine;
        this.properties = properties;
    }

    /**
     * 数据结构：按递增主键维护游标，每批仅保留有限数量日记。
     * 复杂度：总正文长度为 C、字符种类上限为 k 时，约为 O(C + n * k log k)。
     */
    @Override
    public CompressionMaintenanceResult maintain() {
        Statistics statistics = new Statistics();
        long lastId = 0L;
        int batchSize = normalizedBatchSize(properties.getBatchSize());

        while (true) {
            List<Diary> diaries = diaryMapper.selectCompressionBatchAfterId(
                    lastId,
                    batchSize,
                    properties.isVerifyExisting());
            if (diaries == null || diaries.isEmpty()) {
                break;
            }

            for (Diary diary : diaries) {
                if (diary == null || diary.getId() == null) {
                    statistics.failedCount++;
                    continue;
                }
                lastId = Math.max(lastId, diary.getId());
                statistics.scannedCount++;
                maintainDiary(diary, statistics);
            }
        }

        return statistics.toResult();
    }

    private void maintainDiary(Diary diary, Statistics statistics) {
        String contentText = diary.getContentText();
        if (contentText == null || contentText.isEmpty()) {
            statistics.skippedCount++;
            return;
        }

        byte[] existing = diary.getContentCompressed();
        if (existing == null || existing.length == 0) {
            backfill(diary, statistics);
            return;
        }
        if (!properties.isVerifyExisting()) {
            statistics.skippedCount++;
            return;
        }
        verifyOrRepair(diary, existing, statistics);
    }

    private void backfill(Diary diary, Statistics statistics) {
        try {
            CompressionResult compressed = compressionEngine.compress(diary.getContentText());
            int updated = diaryMapper.updateCompressedIfMissing(
                    diary.getId(),
                    diary.getContentText(),
                    compressed.data());
            if (updated == 1) {
                statistics.backfilledCount++;
                statistics.addPayload(diary.getContentText(), compressed.compressedByteLength());
            } else {
                statistics.skippedCount++;
            }
        } catch (RuntimeException exception) {
            recordFailure(diary.getId(), exception, statistics);
        }
    }

    private void verifyOrRepair(Diary diary, byte[] existing, Statistics statistics) {
        try {
            String decompressed = compressionEngine.decompress(existing);
            if (diary.getContentText().equals(decompressed)) {
                statistics.verifiedCount++;
                statistics.addPayload(diary.getContentText(), existing.length);
                return;
            }
        } catch (RuntimeException exception) {
            LOGGER.debug(
                    "Diary compressed content verification failed and will be repaired: diaryId={}, errorType={}",
                    diary.getId(),
                    exception.getClass().getSimpleName());
        }
        repair(diary, statistics);
    }

    private void repair(Diary diary, Statistics statistics) {
        try {
            CompressionResult compressed = compressionEngine.compress(diary.getContentText());
            int updated = diaryMapper.updateCompressedIfContentUnchanged(
                    diary.getId(),
                    diary.getContentText(),
                    compressed.data());
            if (updated == 1) {
                statistics.repairedCount++;
                statistics.addPayload(diary.getContentText(), compressed.compressedByteLength());
            } else {
                statistics.skippedCount++;
            }
        } catch (RuntimeException exception) {
            recordFailure(diary.getId(), exception, statistics);
        }
    }

    private void recordFailure(Long diaryId, RuntimeException exception, Statistics statistics) {
        statistics.failedCount++;
        LOGGER.warn(
                "Diary compression maintenance failed: diaryId={}, errorType={}",
                diaryId,
                exception.getClass().getSimpleName());
    }

    private int normalizedBatchSize(int configuredBatchSize) {
        if (configuredBatchSize <= 0) {
            return DEFAULT_BATCH_SIZE;
        }
        return Math.min(configuredBatchSize, MAX_BATCH_SIZE);
    }

    private static final class Statistics {

        private long scannedCount;
        private long backfilledCount;
        private long verifiedCount;
        private long repairedCount;
        private long skippedCount;
        private long failedCount;
        private long originalByteLength;
        private long compressedByteLength;

        private void addPayload(String contentText, long compressedLength) {
            originalByteLength += contentText.getBytes(StandardCharsets.UTF_8).length;
            compressedByteLength += compressedLength;
        }

        private CompressionMaintenanceResult toResult() {
            return new CompressionMaintenanceResult(
                    scannedCount,
                    backfilledCount,
                    verifiedCount,
                    repairedCount,
                    skippedCount,
                    failedCount,
                    originalByteLength,
                    compressedByteLength);
        }
    }
}
