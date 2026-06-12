package com.trip.model.ai;

public record AnimationGenerationResult(
        String provider,
        String title,
        String narration,
        AnimationScript script) {
}
