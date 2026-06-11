package com.trip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日记压缩维护任务配置。
 */
@Component
@ConfigurationProperties(prefix = "trip.compression")
public class CompressionProperties {

    private boolean backfillEnabled;

    private int batchSize = 100;

    private boolean verifyExisting;

    public boolean isBackfillEnabled() {
        return backfillEnabled;
    }

    public void setBackfillEnabled(boolean backfillEnabled) {
        this.backfillEnabled = backfillEnabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isVerifyExisting() {
        return verifyExisting;
    }

    public void setVerifyExisting(boolean verifyExisting) {
        this.verifyExisting = verifyExisting;
    }
}
