package com.trip.integration.ai;

/**
 * 外部 AI Provider 的可降级异常，不携带请求正文、图片或密钥。
 */
public class AIProviderException extends RuntimeException {

    public AIProviderException(String message) {
        super(message);
    }

    public AIProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
