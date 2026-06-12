package com.trip.integration.ai;

public record LoadedAnimationImage(
        Long mediaId,
        String fileUrl,
        String mimeType,
        String base64Data) {
}
