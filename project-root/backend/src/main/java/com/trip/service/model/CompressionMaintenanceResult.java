package com.trip.service.model;

/**
 * 日记压缩维护任务的汇总结果。
 */
public record CompressionMaintenanceResult(
        long scannedCount,
        long backfilledCount,
        long verifiedCount,
        long repairedCount,
        long skippedCount,
        long failedCount,
        long originalByteLength,
        long compressedByteLength) {

    public double compressionRatio() {
        if (originalByteLength == 0) {
            return 0D;
        }
        return (double) compressedByteLength / originalByteLength;
    }
}
