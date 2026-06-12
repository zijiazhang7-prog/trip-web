package com.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.config.AiAnimationProperties;
import com.trip.model.ai.AnimationGenerationInput;
import com.trip.model.ai.AnimationGenerationResult;
import com.trip.model.ai.AnimationMediaInput;
import com.trip.service.impl.AIServiceImpl;
import com.trip.service.impl.MockTemplateAnimationProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIServiceTests {

    private final AIServiceImpl aiService =
            new AIServiceImpl(
                    List.of(new MockTemplateAnimationProvider()),
                    new AiAnimationProperties());

    @Test
    void mockProviderShouldCreateOrderedPlayableScript() throws Exception {
        AnimationGenerationInput input = new AnimationGenerationInput(
                101L,
                "校园春日漫步",
                "今天沿着校园主路游览了图书馆和教学楼。",
                "北京邮电大学沙河校区",
                List.of(
                        new AnimationMediaInput(501L, "/files/diary/a.jpg", 0),
                        new AnimationMediaInput(502L, "/files/diary/b.jpg", 1)));

        AnimationGenerationResult result = aiService.generateDiaryAnimation(input);

        assertEquals("mock-template", result.provider());
        assertEquals(2, result.script().scenes().size());
        assertEquals(8000, result.script().totalDurationMs());
        assertEquals(501L, result.script().scenes().get(0).mediaId());
        assertEquals(1, result.script().scenes().get(0).order());
        assertEquals(502L, result.script().scenes().get(1).mediaId());
        assertTrue(result.title().contains("北京邮电大学沙河校区"));

        String json = new ObjectMapper().writeValueAsString(result.script());
        assertEquals("1.0", new ObjectMapper().readTree(json).path("schemaVersion").asText());
        assertEquals(2, new ObjectMapper().readTree(json).path("scenes").size());
    }
}
