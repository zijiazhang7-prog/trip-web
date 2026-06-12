package com.trip.integration.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.config.AiAnimationProperties;
import com.trip.model.ai.AnimationGenerationInput;
import com.trip.model.ai.AnimationGenerationResult;
import com.trip.model.ai.AnimationMediaInput;
import com.trip.model.ai.AnimationScene;
import com.trip.model.ai.AnimationScript;
import com.trip.service.AnimationScriptProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * OpenAI-compatible 多模态动画脚本 Provider。
 */
@Component
public class OpenAiCompatibleAnimationProvider implements AnimationScriptProvider {

    public static final String PROVIDER_NAME = "openai-compatible";
    private static final String CHAT_COMPLETIONS_PATH = "chat/completions";
    private static final String SCHEMA_VERSION = "1.0";
    private static final String ASPECT_RATIO = "16:9";
    private static final String BACKGROUND_MUSIC = "light-travel";
    private static final Set<String> MOTIONS =
            Set.of("zoom_in", "zoom_out", "pan_left", "pan_right");
    private static final Set<String> TRANSITIONS =
            Set.of("fade", "dissolve", "slide");
    private static final String SYSTEM_PROMPT = """
            你是旅游照片动画分镜生成器。请结合日记标题、正文、目的地和实际图片内容，
            为每张图片生成一个动画场景。分析可见的建筑、道路、天空、人物、食物、校园或景区场景，
            但不得猜测人物身份、不可见事实或无法确认的具体建筑名称。
            每张图片必须且只能出现一次，顺序必须与输入一致。
            motion 只能是 zoom_in、zoom_out、pan_left、pan_right。
            transition 只能是 fade、dissolve、slide。
            durationMs 必须在 1000 到 10000 之间。
            只输出一个合法 JSON 对象，不要输出 Markdown、代码围栏或解释文字。
            JSON 格式：
            {
              "title": "动画标题",
              "narration": "整段动画总旁白",
              "scenes": [{
                "order": 1,
                "mediaId": 1,
                "visualDescription": "对画面可见内容的客观描述",
                "durationMs": 4000,
                "motion": "zoom_in",
                "transition": "fade",
                "subtitle": "简短字幕",
                "narration": "该场景旁白"
              }]
            }
            """;

    private final AiAnimationProperties properties;
    private final LocalAnimationImageLoader imageLoader;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleAnimationProvider(
            AiAnimationProperties properties,
            LocalAnimationImageLoader imageLoader,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.imageLoader = imageLoader;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public AnimationGenerationResult generate(AnimationGenerationInput input) {
        validateConfiguration();
        validateInput(input);
        List<LoadedAnimationImage> images = imageLoader.load(input.mediaList());
        JsonNode response = callProvider(buildRequest(input, images));
        return parseResponse(input, response);
    }

    private Map<String, Object> buildRequest(
            AnimationGenerationInput input,
            List<LoadedAnimationImage> images) {
        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of(
                "type", "text",
                "text", buildContextText(input)));
        for (LoadedAnimationImage image : images) {
            userContent.add(Map.of(
                    "type", "text",
                    "text", "下面图片对应 mediaId=" + image.mediaId()));
            userContent.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of(
                            "url", "data:" + image.mimeType() + ";base64," + image.base64Data())));
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getModel().trim());
        request.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userContent)));
        request.put("response_format", Map.of("type", "json_object"));
        request.put("temperature", 0.2);
        request.put("max_tokens", properties.getMaxOutputTokens());
        return request;
    }

    private String buildContextText(AnimationGenerationInput input) {
        String content = normalize(input.contentText());
        if (content.length() > properties.getMaxContentChars()) {
            content = content.substring(0, properties.getMaxContentChars());
        }
        return """
                日记标题：%s
                目的地：%s
                日记正文：%s
                图片数量：%d
                请严格按图片输入顺序生成 scenes，并原样使用每张图片前标注的 mediaId。
                """.formatted(
                fallback(input.diaryTitle(), "旅行日记"),
                fallback(input.destinationName(), "旅行目的地"),
                fallback(content, "用户未填写正文"),
                input.mediaList().size());
    }

    private JsonNode callProvider(Map<String, Object> request) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());
        RestClient restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()) + "/")
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey().trim())
                .build();
        try {
            JsonNode response = restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new AIProviderException("EMPTY_PROVIDER_RESPONSE");
            }
            return response;
        } catch (AIProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AIProviderException("PROVIDER_REQUEST_FAILED", exception);
        }
    }

    private AnimationGenerationResult parseResponse(
            AnimationGenerationInput input,
            JsonNode response) {
        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual() || !StringUtils.hasText(contentNode.asText())) {
            throw new AIProviderException("PROVIDER_CONTENT_MISSING");
        }

        JsonNode generated;
        try {
            generated = objectMapper.readTree(contentNode.asText());
        } catch (JsonProcessingException exception) {
            throw new AIProviderException("PROVIDER_JSON_INVALID", exception);
        }

        String title = requiredText(generated, "title", 150);
        String narration = requiredText(generated, "narration", 2000);
        JsonNode scenesNode = generated.path("scenes");
        if (!scenesNode.isArray() || scenesNode.size() != input.mediaList().size()) {
            throw new AIProviderException("PROVIDER_SCENES_INVALID");
        }

        Map<Long, AnimationMediaInput> allowedMedia = new HashMap<>();
        for (AnimationMediaInput media : input.mediaList()) {
            allowedMedia.put(media.mediaId(), media);
        }

        List<AnimationScene> scenes = new ArrayList<>(scenesNode.size());
        Set<Long> usedMedia = new java.util.HashSet<>();
        int totalDurationMs = 0;
        for (int index = 0; index < scenesNode.size(); index++) {
            JsonNode sceneNode = scenesNode.get(index);
            int order = sceneNode.path("order").asInt(-1);
            Long mediaId = sceneNode.path("mediaId").canConvertToLong()
                    ? sceneNode.path("mediaId").longValue()
                    : null;
            Long expectedMediaId = input.mediaList().get(index).mediaId();
            int durationMs = sceneNode.path("durationMs").asInt(-1);
            String motion = requiredText(sceneNode, "motion", 30);
            String transition = requiredText(sceneNode, "transition", 30);
            if (order != index + 1
                    || mediaId == null
                    || !mediaId.equals(expectedMediaId)
                    || !allowedMedia.containsKey(mediaId)
                    || !usedMedia.add(mediaId)
                    || durationMs < 1000
                    || durationMs > 10000
                    || !MOTIONS.contains(motion)
                    || !TRANSITIONS.contains(transition)) {
                throw new AIProviderException("PROVIDER_SCENE_INVALID");
            }

            AnimationMediaInput media = allowedMedia.get(mediaId);
            scenes.add(new AnimationScene(
                    order,
                    mediaId,
                    media.fileUrl(),
                    requiredText(sceneNode, "visualDescription", 500),
                    durationMs,
                    motion,
                    transition,
                    requiredText(sceneNode, "subtitle", 200),
                    requiredText(sceneNode, "narration", 500)));
            totalDurationMs += durationMs;
        }

        AnimationScript script = new AnimationScript(
                SCHEMA_VERSION,
                ASPECT_RATIO,
                totalDurationMs,
                BACKGROUND_MUSIC,
                List.copyOf(scenes));
        return new AnimationGenerationResult(PROVIDER_NAME, title, narration, script);
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getBaseUrl())
                || !StringUtils.hasText(properties.getApiKey())
                || !StringUtils.hasText(properties.getModel())) {
            throw new AIProviderException("PROVIDER_NOT_CONFIGURED");
        }
        if (properties.getConnectTimeout() == null
                || properties.getConnectTimeout().isNegative()
                || properties.getReadTimeout() == null
                || properties.getReadTimeout().isNegative()
                || properties.getMaxImages() <= 0
                || properties.getMaxImageSize() == null
                || properties.getMaxImageSize().isNegative()
                || properties.getMaxTotalImageSize() == null
                || properties.getMaxTotalImageSize().isNegative()
                || properties.getMaxContentChars() <= 0
                || properties.getMaxOutputTokens() <= 0) {
            throw new AIProviderException("PROVIDER_CONFIG_INVALID");
        }
    }

    private void validateInput(AnimationGenerationInput input) {
        if (input == null
                || input.diaryId() == null
                || input.mediaList() == null
                || input.mediaList().isEmpty()) {
            throw new AIProviderException("PROVIDER_INPUT_INVALID");
        }
    }

    private String requiredText(JsonNode node, String field, int maxLength) {
        JsonNode value = node.path(field);
        if (!value.isTextual()) {
            throw new AIProviderException("PROVIDER_FIELD_INVALID");
        }
        String normalized = normalize(value.asText());
        if (!StringUtils.hasText(normalized) || normalized.length() > maxLength) {
            throw new AIProviderException("PROVIDER_FIELD_INVALID");
        }
        return normalized;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String fallback(String value, String fallback) {
        String normalized = normalize(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
