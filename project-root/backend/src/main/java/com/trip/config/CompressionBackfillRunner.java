package com.trip.config;

import com.trip.service.CompressionMaintenanceService;
import com.trip.service.model.CompressionMaintenanceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 在显式开启配置时执行一次历史日记压缩维护。
 */
@Component
@ConditionalOnProperty(prefix = "trip.compression", name = "backfill-enabled", havingValue = "true")
public class CompressionBackfillRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionBackfillRunner.class);

    private final CompressionMaintenanceService compressionMaintenanceService;

    public CompressionBackfillRunner(CompressionMaintenanceService compressionMaintenanceService) {
        this.compressionMaintenanceService = compressionMaintenanceService;
    }

    @Override
    public void run(ApplicationArguments args) {
        CompressionMaintenanceResult result = compressionMaintenanceService.maintain();
        LOGGER.info(
                "Diary compression maintenance completed: scanned={}, backfilled={}, verified={}, repaired={}, "
                        + "skipped={}, failed={}, originalBytes={}, compressedBytes={}, compressionRatio={}",
                result.scannedCount(),
                result.backfilledCount(),
                result.verifiedCount(),
                result.repairedCount(),
                result.skippedCount(),
                result.failedCount(),
                result.originalByteLength(),
                result.compressedByteLength(),
                String.format(java.util.Locale.ROOT, "%.4f", result.compressionRatio()));
    }
}
