package com.trip.service;

import com.trip.model.ai.AnimationGenerationInput;
import com.trip.model.ai.AnimationGenerationResult;

public interface AIService {

    AnimationGenerationResult generateDiaryAnimation(AnimationGenerationInput input);
}
