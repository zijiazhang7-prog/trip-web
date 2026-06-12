package com.trip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.trip.config.AiAnimationProperties;
import com.trip.integration.ai.AIProviderException;
import com.trip.integration.ai.LocalAnimationImageLoader;
import com.trip.integration.ai.OpenAiCompatibleAnimationProvider;
import com.trip.model.ai.AnimationGenerationInput;
import com.trip.model.ai.AnimationGenerationResult;
import com.trip.model.ai.AnimationMediaInput;
import com.trip.service.AnimationScriptProvider;
import com.trip.service.impl.AIServiceImpl;
import com.trip.service.impl.MockTemplateAnimationProvider;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleAnimationProviderTests {

    private static final byte[] MINIMAL_PNG = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
    };

    @TempDir
    Path uploadRoot;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void providerShouldSendLocalImageAndParseStrictJson() throws Exception {
        Path image = createDiaryImage("photo.png");
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        startServer(exchange -> {
            requestBody.set(readBody(exchange));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sendJson(exchange, 200, successfulResponse());
        });

        OpenAiCompatibleAnimationProvider provider = provider(properties(Duration.ofSeconds(2)));
        AnimationGenerationResult result = provider.generate(input(image));

        assertEquals("openai-compatible", result.provider());
        assertEquals("校园建筑漫游", result.title());
        assertEquals(501L, result.script().scenes().get(0).mediaId());
        assertEquals("/files/diary/photo.png", result.script().scenes().get(0).fileUrl());
        assertEquals("画面中有校园建筑、道路和蓝天", result.script().scenes().get(0).visualDescription());
        assertEquals("Bearer test-key", authorization.get());

        JsonNode sent = new ObjectMapper().readTree(requestBody.get());
        assertEquals("vision-model", sent.path("model").asText());
        assertTrue(requestBody.get().contains("data:image/png;base64,"));
        assertTrue(requestBody.get().contains("mediaId=501"));
    }

    @Test
    void invalidProviderJsonShouldBeRejected() throws Exception {
        Path image = createDiaryImage("invalid.png");
        startServer(exchange -> sendJson(exchange, 200, """
                {"choices":[{"message":{"content":"not-json"}}]}
                """));

        OpenAiCompatibleAnimationProvider provider = provider(properties(Duration.ofSeconds(2)));

        assertThrows(AIProviderException.class, () -> provider.generate(input(image)));
    }

    @Test
    void timeoutShouldFallBackToMockProvider() throws Exception {
        Path image = createDiaryImage("slow.png");
        startServer(exchange -> {
            try {
                Thread.sleep(300);
                sendJson(exchange, 200, successfulResponse());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // Client timeout can close the connection before the mock response is written.
            }
        });
        AiAnimationProperties properties = properties(Duration.ofMillis(50));
        properties.setProvider(OpenAiCompatibleAnimationProvider.PROVIDER_NAME);
        AnimationScriptProvider realProvider = provider(properties);
        AIServiceImpl aiService = new AIServiceImpl(
                List.of(realProvider, new MockTemplateAnimationProvider()),
                properties);

        AnimationGenerationResult result = aiService.generateDiaryAnimation(input(image));

        assertEquals("mock-template", result.provider());
        assertEquals(1, result.script().scenes().size());
    }

    @Test
    void providerServerErrorShouldFallBackToMockProvider() throws Exception {
        Path image = createDiaryImage("server-error.png");
        startServer(exchange -> sendJson(exchange, 500, """
                {"error":{"message":"temporary unavailable"}}
                """));
        AiAnimationProperties properties = properties(Duration.ofSeconds(1));
        properties.setProvider(OpenAiCompatibleAnimationProvider.PROVIDER_NAME);
        AIServiceImpl aiService = new AIServiceImpl(
                List.of(provider(properties), new MockTemplateAnimationProvider()),
                properties);

        AnimationGenerationResult result = aiService.generateDiaryAnimation(input(image));

        assertEquals("mock-template", result.provider());
    }

    @Test
    void missingApiKeyShouldFallBackWithoutCallingServer() throws Exception {
        Path image = createDiaryImage("no-key.png");
        AiAnimationProperties properties = properties(Duration.ofSeconds(1));
        properties.setApiKey("");
        properties.setProvider(OpenAiCompatibleAnimationProvider.PROVIDER_NAME);
        AnimationScriptProvider realProvider = provider(properties);
        AIServiceImpl aiService = new AIServiceImpl(
                List.of(realProvider, new MockTemplateAnimationProvider()),
                properties);

        AnimationGenerationResult result = aiService.generateDiaryAnimation(input(image));

        assertEquals("mock-template", result.provider());
    }

    @Test
    void nonDiaryFileUrlShouldBeRejected() throws Exception {
        createDiaryImage("safe.png");
        OpenAiCompatibleAnimationProvider provider = provider(properties(Duration.ofSeconds(1)));
        AnimationGenerationInput invalidInput = new AnimationGenerationInput(
                101L,
                "title",
                "content",
                "destination",
                List.of(new AnimationMediaInput(501L, "/files/avatar/safe.png", 0)));

        assertThrows(AIProviderException.class, () -> provider.generate(invalidInput));
    }

    private OpenAiCompatibleAnimationProvider provider(AiAnimationProperties properties) {
        return new OpenAiCompatibleAnimationProvider(
                properties,
                new LocalAnimationImageLoader(
                        uploadRoot.toString(),
                        "/files",
                        properties),
                new ObjectMapper());
    }

    private AiAnimationProperties properties(Duration readTimeout) {
        AiAnimationProperties properties = new AiAnimationProperties();
        properties.setBaseUrl("http://127.0.0.1:" + (server == null ? 1 : server.getAddress().getPort()) + "/v1");
        properties.setApiKey("test-key");
        properties.setModel("vision-model");
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(readTimeout);
        return properties;
    }

    private AnimationGenerationInput input(Path ignoredImagePath) {
        return new AnimationGenerationInput(
                101L,
                "校园春日漫步",
                "今天沿着校园主路参观了图书馆。",
                "北京邮电大学沙河校区",
                List.of(new AnimationMediaInput(501L, "/files/diary/photo.png", 0)));
    }

    private Path createDiaryImage(String fileName) throws IOException {
        Path diaryDirectory = uploadRoot.resolve("diary");
        Files.createDirectories(diaryDirectory);
        Path image = diaryDirectory.resolve(fileName);
        Files.write(image, MINIMAL_PNG);
        if (!"photo.png".equals(fileName)) {
            Files.copy(image, diaryDirectory.resolve("photo.png"));
        }
        return image;
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> handler.handle(exchange));
        server.start();
    }

    private String successfulResponse() throws IOException {
        String generated = """
                {
                  "title":"校园建筑漫游",
                  "narration":"跟随照片回顾校园旅程。",
                  "scenes":[{
                    "order":1,
                    "mediaId":501,
                    "visualDescription":"画面中有校园建筑、道路和蓝天",
                    "durationMs":4000,
                    "motion":"zoom_in",
                    "transition":"fade",
                    "subtitle":"校园主路",
                    "narration":"从开阔的校园道路开始。"
                  }]
                }
                """;
        return new ObjectMapper().writeValueAsString(java.util.Map.of(
                "choices", List.of(java.util.Map.of(
                        "message", java.util.Map.of("content", generated)))));
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
