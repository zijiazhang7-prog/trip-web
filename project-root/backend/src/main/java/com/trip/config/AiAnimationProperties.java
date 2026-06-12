package com.trip.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * 日记动画 AI Provider 配置。密钥只能由环境变量或本地配置注入。
 */
@Component
@ConfigurationProperties(prefix = "trip.ai.animation")
public class AiAnimationProperties {

    private String provider = "mock-template";
    private boolean fallbackEnabled = true;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(45);
    private int maxImages = 6;
    private DataSize maxImageSize = DataSize.ofMegabytes(2);
    private DataSize maxTotalImageSize = DataSize.ofMegabytes(8);
    private int maxContentChars = 3000;
    private int maxOutputTokens = 2500;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxImages() {
        return maxImages;
    }

    public void setMaxImages(int maxImages) {
        this.maxImages = maxImages;
    }

    public DataSize getMaxImageSize() {
        return maxImageSize;
    }

    public void setMaxImageSize(DataSize maxImageSize) {
        this.maxImageSize = maxImageSize;
    }

    public DataSize getMaxTotalImageSize() {
        return maxTotalImageSize;
    }

    public void setMaxTotalImageSize(DataSize maxTotalImageSize) {
        this.maxTotalImageSize = maxTotalImageSize;
    }

    public int getMaxContentChars() {
        return maxContentChars;
    }

    public void setMaxContentChars(int maxContentChars) {
        this.maxContentChars = maxContentChars;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }
}
