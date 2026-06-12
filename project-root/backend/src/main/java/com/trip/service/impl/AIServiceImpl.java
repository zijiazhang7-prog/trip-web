package com.trip.service.impl;

import com.trip.config.AiAnimationProperties;
import com.trip.model.ai.AnimationGenerationInput;
import com.trip.model.ai.AnimationGenerationResult;
import com.trip.service.AIService;
import com.trip.service.AnimationScriptProvider;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIServiceImpl.class);
    private static final String MOCK_PROVIDER = "mock-template";

    private final Map<String, AnimationScriptProvider> providers;
    private final AiAnimationProperties properties;

    public AIServiceImpl(
            List<AnimationScriptProvider> providers,
            AiAnimationProperties properties) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(
                AnimationScriptProvider::name,
                Function.identity()));
        this.properties = properties;
    }

    @Override
    public AnimationGenerationResult generateDiaryAnimation(AnimationGenerationInput input) {
        String selectedName = StringUtils.hasText(properties.getProvider())
                ? properties.getProvider().trim()
                : MOCK_PROVIDER;
        AnimationScriptProvider selected = providers.get(selectedName);
        if (selected == null) {
            return fallback(input, selectedName, new IllegalStateException("Provider is not registered"));
        }

        long startedAt = System.nanoTime();
        try {
            AnimationGenerationResult result = selected.generate(input);
            LOGGER.info(
                    "Diary animation provider succeeded: diaryId={}, provider={}, durationMs={}",
                    input == null ? null : input.diaryId(),
                    selected.name(),
                    elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException exception) {
            return fallback(input, selectedName, exception);
        }
    }

    private AnimationGenerationResult fallback(
            AnimationGenerationInput input,
            String selectedName,
            RuntimeException exception) {
        LOGGER.warn(
                "Diary animation provider failed: diaryId={}, provider={}, errorType={}",
                input == null ? null : input.diaryId(),
                selectedName,
                exception.getClass().getSimpleName());
        if (!properties.isFallbackEnabled() || MOCK_PROVIDER.equals(selectedName)) {
            throw exception;
        }
        AnimationScriptProvider mockProvider = providers.get(MOCK_PROVIDER);
        if (mockProvider == null) {
            throw exception;
        }
        return mockProvider.generate(input);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
