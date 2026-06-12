package com.trip.model.ai;

import java.util.List;

public record AnimationScript(
        String schemaVersion,
        String aspectRatio,
        int totalDurationMs,
        String backgroundMusic,
        List<AnimationScene> scenes) {
}
