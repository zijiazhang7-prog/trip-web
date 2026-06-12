package com.trip.model.ai;

public record AnimationScene(
        int order,
        Long mediaId,
        String fileUrl,
        String visualDescription,
        int durationMs,
        String motion,
        String transition,
        String subtitle,
        String narration) {
}
