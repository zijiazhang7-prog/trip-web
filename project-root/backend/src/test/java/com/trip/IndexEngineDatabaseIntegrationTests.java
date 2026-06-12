package com.trip;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.engine.index.IndexEngine;
import com.trip.engine.index.IndexNamespace;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.Food;
import com.trip.entity.User;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.FoodMapper;
import com.trip.mapper.UserMapper;
import com.trip.service.IndexMaintenanceService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 使用 MySQL 实库和真实 HTTP 入口验证索引启用、失效时的查询契约一致性。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IndexEngineDatabaseIntegrationTests {

    private static final EnumSet<IndexNamespace> TEST_NAMESPACES = EnumSet.of(
            IndexNamespace.DESTINATION_NAME,
            IndexNamespace.FOOD_NAME,
            IndexNamespace.FOOD_SHOP_NAME,
            IndexNamespace.DIARY_TITLE,
            IndexNamespace.DIARY_CONTENT);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IndexEngine indexEngine;

    @Autowired
    private IndexMaintenanceService indexMaintenanceService;

    @Autowired
    private DestinationMapper destinationMapper;

    @Autowired
    private FoodMapper foodMapper;

    @Autowired
    private DiaryMapper diaryMapper;

    @Autowired
    private UserMapper userMapper;

    private final List<Long> diaryIdsToClean = new ArrayList<>();
    private final List<Long> foodIdsToClean = new ArrayList<>();
    private final List<Long> destinationIdsToClean = new ArrayList<>();
    private final List<Long> userIdsToClean = new ArrayList<>();

    @AfterEach
    void cleanFixturesAndRestoreIndexes() {
        diaryIdsToClean.forEach(diaryMapper::deleteById);
        foodIdsToClean.forEach(foodMapper::deleteById);
        destinationIdsToClean.forEach(destinationMapper::deleteById);
        userIdsToClean.forEach(userMapper::deleteById);
        TEST_NAMESPACES.forEach(this::restoreIndex);
    }

    @Test
    void searchResponsesShouldMatchWhenIndexesAreReadyAndInvalidated() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");

        String marker = "idx" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Fixture fixture = createFixture(marker);
        TEST_NAMESPACES.forEach(indexMaintenanceService::rebuild);

        assertIndexReadyAndContainsFixture(fixture);

        Map<String, JsonNode> indexedResponses = requestScenarios(fixture);

        TEST_NAMESPACES.forEach(indexMaintenanceService::invalidate);
        TEST_NAMESPACES.forEach(namespace -> assertThat(indexEngine.isReady(namespace)).isFalse());

        Map<String, JsonNode> fallbackResponses = requestScenarios(fixture);

        assertThat(fallbackResponses)
                .usingRecursiveComparison()
                .isEqualTo(indexedResponses);
    }

    private Fixture createFixture(String marker) {
        User user = new User();
        user.setUsername(marker);
        user.setPasswordHash("integration-test-password-not-used");
        user.setNickname("Index Integration");
        user.setRole("user");
        user.setStatus(1);
        userMapper.insert(user);
        userIdsToClean.add(user.getId());

        List<Destination> destinations = List.of(
                destination(marker + "Exact", "41.00", "4.10"),
                destination(marker + "Alpha", "94.00", "4.90"),
                destination(marker + "Beta", "73.00", "4.60"),
                destination(marker + "Gamma", "52.00", "4.30"));
        destinations.forEach(destination -> {
            destinationMapper.insert(destination);
            destinationIdsToClean.add(destination.getId());
        });
        Long destinationId = destinations.get(0).getId();

        List<Food> foods = List.of(
                food(destinationId, marker + "Noodle", marker + "ShopOne", "65.00", "4.20"),
                food(destinationId, marker + "Rice", marker + "ShopTwo", "82.00", "4.80"),
                food(destinationId, marker + "Soup", marker + "ShopThree", "49.00", "4.50"),
                food(destinationId, marker + "Cake", marker + "ShopFour", "91.00", "4.70"));
        foods.forEach(food -> {
            foodMapper.insert(food);
            foodIdsToClean.add(food.getId());
        });

        List<Diary> diaries = List.of(
                diary(user.getId(), destinationId, marker + "Exact",
                        "今天参观" + marker + "图书馆", "31.00", "4.10"),
                diary(user.getId(), destinationId, marker + "PrefixAlpha",
                        "English" + marker + "Token", "88.00", "4.90"),
                diary(user.getId(), destinationId, marker + "PrefixBeta",
                        "单字旅" + marker + "体验", "67.00", "4.60"),
                diary(user.getId(), destinations.get(1).getId(), "Head" + marker + "InsideTail",
                        "异地参观" + marker + "图书馆", "95.00", "4.70"));
        diaries.forEach(diary -> {
            diaryMapper.insert(diary);
            diaryIdsToClean.add(diary.getId());
        });

        return new Fixture(marker, destinations, foods, diaries);
    }

    private Map<String, JsonNode> requestScenarios(Fixture fixture) throws Exception {
        Map<String, JsonNode> responses = new LinkedHashMap<>();
        String marker = fixture.marker();
        Long destinationId = fixture.destinations().get(0).getId();

        responses.put("destination-exact", getData("/api/v1/destinations/search",
                params("keyword", marker + "Exact", "sortBy", "rating", "pageNum", "1", "pageSize", "2")));
        responses.put("destination-prefix-page-1", getData("/api/v1/destinations/search",
                params("keyword", marker, "sortBy", "heat", "pageNum", "1", "pageSize", "2")));
        responses.put("destination-prefix-page-2", getData("/api/v1/destinations/search",
                params("keyword", marker, "sortBy", "heat", "pageNum", "2", "pageSize", "2")));

        responses.put("food-name-page-1", getData("/api/v1/foods/search",
                params("destinationId", destinationId.toString(), "keyword", marker,
                        "sortBy", "rating", "pageNum", "1", "pageSize", "2")));
        responses.put("food-name-page-2", getData("/api/v1/foods/search",
                params("destinationId", destinationId.toString(), "keyword", marker,
                        "sortBy", "rating", "pageNum", "2", "pageSize", "2")));
        responses.put("food-shop-exact", getData("/api/v1/foods/search",
                params("destinationId", destinationId.toString(), "keyword", marker + "ShopTwo",
                        "sortBy", "heat", "pageNum", "1", "pageSize", "2")));

        responses.put("diary-title-exact", getData("/api/v1/diaries/search/title",
                params("title", marker + "Exact", "sortBy", "latest", "pageNum", "1", "pageSize", "2")));
        responses.put("diary-title-prefix-page-1", getData("/api/v1/diaries/search/title",
                params("title", marker, "sortBy", "heat", "pageNum", "1", "pageSize", "2")));
        responses.put("diary-title-prefix-page-2", getData("/api/v1/diaries/search/title",
                params("title", marker, "sortBy", "heat", "pageNum", "2", "pageSize", "2")));
        responses.put("diary-title-infix", getData("/api/v1/diaries/search/title",
                params("title", marker + "Inside", "sortBy", "rating", "pageNum", "1", "pageSize", "2")));
        responses.put("diary-destination-prefix-page-1", getData("/api/v1/diaries",
                params("destinationKeyword", marker, "sortBy", "heat", "pageNum", "1", "pageSize", "2")));
        responses.put("diary-destination-prefix-page-2", getData("/api/v1/diaries",
                params("destinationKeyword", marker, "sortBy", "heat", "pageNum", "2", "pageSize", "2")));
        responses.put("diary-destination-infix", getData("/api/v1/diaries",
                params("destinationKeyword", "Exact", "sortBy", "rating", "pageNum", "1", "pageSize", "5")));
        responses.put("diary-destination-miss", getData("/api/v1/diaries",
                params("destinationKeyword", marker + "不存在", "sortBy", "latest", "pageNum", "1", "pageSize", "5")));
        responses.put("diary-content-page-1", getData("/api/v1/diaries/search/fulltext",
                params("keyword", marker, "sortBy", "heat", "pageNum", "1", "pageSize", "2")));
        responses.put("diary-content-page-2", getData("/api/v1/diaries/search/fulltext",
                params("keyword", marker, "sortBy", "heat", "pageNum", "2", "pageSize", "2")));
        responses.put("diary-content-chinese", getData("/api/v1/diaries/search/fulltext",
                params("keyword", marker + "图书馆", "sortBy", "rating", "pageNum", "1", "pageSize", "5")));
        responses.put("diary-content-destination", getData("/api/v1/diaries/search/fulltext",
                params("keyword", marker + "图书馆", "destinationId", destinationId.toString(),
                        "sortBy", "latest", "pageNum", "1", "pageSize", "5")));
        responses.put("diary-content-case", getData("/api/v1/diaries/search/fulltext",
                params("keyword", ("ENGLISH" + marker + "TOKEN").toUpperCase(),
                        "sortBy", "latest", "pageNum", "1", "pageSize", "5")));
        responses.put("diary-content-miss", getData("/api/v1/diaries/search/fulltext",
                params("keyword", marker + "不存在", "sortBy", "latest", "pageNum", "1", "pageSize", "5")));

        assertPage(responses.get("destination-prefix-page-1"), 1, 2, 4, 2);
        assertPage(responses.get("destination-prefix-page-2"), 2, 2, 4, 2);
        assertPage(responses.get("food-name-page-1"), 1, 2, 4, 2);
        assertPage(responses.get("food-name-page-2"), 2, 2, 4, 2);
        assertPage(responses.get("diary-title-prefix-page-1"), 1, 2, 4, 2);
        assertPage(responses.get("diary-title-prefix-page-2"), 2, 2, 4, 2);
        assertPage(responses.get("diary-title-infix"), 1, 2, 1, 1);
        assertPage(responses.get("diary-destination-prefix-page-1"), 1, 2, 4, 2);
        assertPage(responses.get("diary-destination-prefix-page-2"), 2, 2, 4, 2);
        assertPage(responses.get("diary-destination-infix"), 1, 5, 3, 1);
        assertPage(responses.get("diary-destination-miss"), 1, 5, 0, 0);
        assertPage(responses.get("diary-content-page-1"), 1, 2, 4, 2);
        assertPage(responses.get("diary-content-page-2"), 2, 2, 4, 2);
        assertPage(responses.get("diary-content-chinese"), 1, 5, 2, 1);
        assertPage(responses.get("diary-content-destination"), 1, 5, 1, 1);
        assertPage(responses.get("diary-content-case"), 1, 5, 1, 1);
        assertPage(responses.get("diary-content-miss"), 1, 5, 0, 0);
        assertThat(responses.get("diary-title-infix").path("list").get(0).path("id").asLong())
                .isEqualTo(fixture.diaries().get(3).getId());
        return responses;
    }

    private JsonNode getData(String path, Map<String, String> params) throws Exception {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        params.forEach(builder::queryParam);
        ResponseEntity<String> response =
                restTemplate.getForEntity(builder.build().encode().toUri(), String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.path("code").asText()).isEqualTo("SUCCESS");
        return body.path("data");
    }

    private void assertIndexReadyAndContainsFixture(Fixture fixture) {
        TEST_NAMESPACES.forEach(namespace -> assertThat(indexEngine.isReady(namespace)).isTrue());
        assertThat(indexEngine.findExact(
                        IndexNamespace.DESTINATION_NAME,
                        fixture.marker() + "Exact").ids())
                .contains(fixture.destinations().get(0).getId());
        assertThat(indexEngine.findByPrefix(
                        IndexNamespace.FOOD_NAME,
                        fixture.marker(),
                        100).ids())
                .containsAll(fixture.foods().stream().map(Food::getId).toList());
        assertThat(indexEngine.findExact(
                        IndexNamespace.FOOD_SHOP_NAME,
                        fixture.marker() + "ShopTwo").ids())
                .contains(fixture.foods().get(1).getId());
        assertThat(indexEngine.findByPrefix(
                        IndexNamespace.DIARY_TITLE,
                        fixture.marker(),
                        100).ids())
                .containsExactlyInAnyOrderElementsOf(
                        fixture.diaries().subList(0, 3).stream().map(Diary::getId).toList());
        assertThat(indexEngine.findByContent(
                        IndexNamespace.DIARY_CONTENT,
                        fixture.marker() + "图书馆").ids())
                .containsExactlyInAnyOrder(
                        fixture.diaries().get(0).getId(),
                        fixture.diaries().get(3).getId());
    }

    private void assertPage(JsonNode data, long pageNum, long pageSize, long total, long pages) {
        assertThat(data.path("pageNum").asLong()).isEqualTo(pageNum);
        assertThat(data.path("pageSize").asLong()).isEqualTo(pageSize);
        assertThat(data.path("total").asLong()).isEqualTo(total);
        assertThat(data.path("pages").asLong()).isEqualTo(pages);
    }

    private Destination destination(String name, String heatScore, String ratingScore) {
        Destination destination = new Destination();
        destination.setName(name);
        destination.setType("scenic");
        destination.setCategory("index-test");
        destination.setCity("IndexCity");
        destination.setDescription("IndexEngine integration fixture");
        destination.setHeatScore(new BigDecimal(heatScore));
        destination.setRatingScore(new BigDecimal(ratingScore));
        destination.setTagJson("[]");
        destination.setStatus(1);
        return destination;
    }

    private Food food(
            Long destinationId,
            String name,
            String shopName,
            String heatScore,
            String ratingScore) {
        Food food = new Food();
        food.setDestinationId(destinationId);
        food.setName(name);
        food.setFoodType("index-test");
        food.setShopName(shopName);
        food.setDescription("IndexEngine integration fixture");
        food.setHeatScore(new BigDecimal(heatScore));
        food.setRatingScore(new BigDecimal(ratingScore));
        food.setAvgPrice(new BigDecimal("20.00"));
        return food;
    }

    private Diary diary(
            Long userId,
            Long destinationId,
            String title,
            String contentText,
            String heatScore,
            String ratingScore) {
        Diary diary = new Diary();
        diary.setUserId(userId);
        diary.setDestinationId(destinationId);
        diary.setTitle(title);
        diary.setContentText(contentText);
        diary.setHeatScore(new BigDecimal(heatScore).longValueExact());
        diary.setRatingScore(new BigDecimal(ratingScore));
        diary.setVisibility("public");
        diary.setStatus(1);
        return diary;
    }

    private Map<String, String> params(String... values) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            params.put(values[index], values[index + 1]);
        }
        return params;
    }

    private void restoreIndex(IndexNamespace namespace) {
        try {
            indexMaintenanceService.rebuild(namespace);
        } catch (RuntimeException exception) {
            indexMaintenanceService.invalidate(namespace);
        }
    }

    private boolean hasDatabasePassword() {
        if (System.getProperty("spring.datasource.password") != null) {
            return true;
        }
        return System.getenv("DB_PASSWORD") != null;
    }

    private record Fixture(
            String marker,
            List<Destination> destinations,
            List<Food> foods,
            List<Diary> diaries) {
    }
}
