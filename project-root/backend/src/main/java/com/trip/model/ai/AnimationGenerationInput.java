package com.trip.model.ai;

import java.util.List;

public record AnimationGenerationInput(
        Long diaryId,
        String diaryTitle,
        String contentText,
        String destinationName,
        List<AnimationMediaInput> mediaList) {
}
