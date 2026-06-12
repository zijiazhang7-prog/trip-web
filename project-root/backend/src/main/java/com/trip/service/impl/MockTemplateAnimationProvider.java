package com.trip.service.impl;

import com.trip.model.ai.AnimationGenerationInput;
import com.trip.model.ai.AnimationGenerationResult;
import com.trip.model.ai.AnimationMediaInput;
import com.trip.model.ai.AnimationScene;
import com.trip.model.ai.AnimationScript;
import com.trip.service.AnimationScriptProvider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 离线模板提供方。只组织现有日记文本和图片顺序，不声明具备图片语义理解能力。
 */
@Component
public class MockTemplateAnimationProvider implements AnimationScriptProvider {

    static final String PROVIDER_NAME = "mock-template";
    private static final int SCENE_DURATION_MS = 4000;
    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_NARRATION_LENGTH = 300;
    private static final List<String> MOTIONS =
            List.of("zoom_in", "pan_left", "zoom_out", "pan_right");
    private static final List<String> TRANSITIONS = List.of("fade", "dissolve", "slide");

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public AnimationGenerationResult generate(AnimationGenerationInput input) {
        validateInput(input);

        String destinationName = fallback(input.destinationName(), "旅行目的地");
        String diaryTitle = fallback(input.diaryTitle(), "旅行日记");
        String title = truncate(destinationName + " · " + diaryTitle + " 动画回顾", MAX_TITLE_LENGTH);
        String contentSummary = truncate(normalize(input.contentText()), MAX_NARRATION_LENGTH);
        String narration = StringUtils.hasText(contentSummary)
                ? contentSummary
                : "跟随照片回顾这次 " + destinationName + " 之旅。";

        List<AnimationScene> scenes = new ArrayList<>(input.mediaList().size());
        for (int index = 0; index < input.mediaList().size(); index++) {
            AnimationMediaInput media = input.mediaList().get(index);
            int order = index + 1;
            scenes.add(new AnimationScene(
                    order,
                    media.mediaId(),
                    media.fileUrl(),
                    "第 " + order + " 张旅行照片，等待多模态模型补充画面语义。",
                    SCENE_DURATION_MS,
                    MOTIONS.get(index % MOTIONS.size()),
                    TRANSITIONS.get(index % TRANSITIONS.size()),
                    "第 " + order + " 幕 · " + destinationName,
                    sceneNarration(order, input.mediaList().size(), destinationName)));
        }

        AnimationScript script = new AnimationScript(
                "1.0",
                "16:9",
                scenes.size() * SCENE_DURATION_MS,
                "light-travel",
                List.copyOf(scenes));
        return new AnimationGenerationResult(PROVIDER_NAME, title, narration, script);
    }

    private void validateInput(AnimationGenerationInput input) {
        if (input == null || input.diaryId() == null || input.mediaList() == null || input.mediaList().isEmpty()) {
            throw new IllegalArgumentException("Animation input is incomplete");
        }
        for (AnimationMediaInput media : input.mediaList()) {
            if (media == null
                    || media.mediaId() == null
                    || !StringUtils.hasText(media.fileUrl())
                    || !media.fileUrl().startsWith("/files/")) {
                throw new IllegalArgumentException("Animation media input is invalid");
            }
        }
    }

    private String sceneNarration(int order, int total, String destinationName) {
        if (order == 1) {
            return "旅程从 " + destinationName + " 的第一张照片开始。";
        }
        if (order == total) {
            return "用最后一张照片结束这次旅行回顾。";
        }
        return "继续浏览旅途中的第 " + order + " 个画面。";
    }

    private String fallback(String value, String fallback) {
        String normalized = normalize(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
