package com.trip.service;

import com.trip.model.ai.AnimationGenerationInput;
import com.trip.model.ai.AnimationGenerationResult;

public interface AnimationScriptProvider {

    String name();

    AnimationGenerationResult generate(AnimationGenerationInput input);
}
