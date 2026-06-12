package com.trip;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.dto.request.DiaryListQuery;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.User;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.UserMapper;
import com.trip.service.DiaryService;
import com.trip.vo.response.DiaryVO;
import com.trip.vo.response.PageResultVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * 使用 MySQL 实库验证日记浏览量原子自增和实时热度排序。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiaryHeatDatabaseIntegrationTests {

    private static final int CONCURRENT_VIEWS = 8;

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DiaryMapper diaryMapper;

    @Autowired
    private DestinationMapper destinationMapper;

    @Autowired
    private UserMapper userMapper;

    private final List<Long> diaryIds = new ArrayList<>();
    private final List<Long> destinationIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanFixtures() {
        diaryIds.forEach(diaryMapper::deleteById);
        destinationIds.forEach(destinationMapper::deleteById);
        userIds.forEach(userMapper::deleteById);
    }

    @Test
    void detailViewsShouldIncrementAtomicallyAndAffectHeatSort() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        String marker = "heat" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User author = insertUser(marker + "author");
        Destination destination = insertDestination(marker);
        Diary viewedDiary = insertDiary(author.getId(), destination.getId(), marker + "viewed", 2L);
        Diary otherDiary = insertDiary(author.getId(), destination.getId(), marker + "other", 5L);

        ResponseEntity<String> firstResponse =
                restTemplate.getForEntity("/api/v1/diaries/{id}", String.class, viewedDiary.getId());
        ResponseEntity<String> secondResponse =
                restTemplate.getForEntity("/api/v1/diaries/{id}", String.class, viewedDiary.getId());
        JsonNode firstView = objectMapper.readTree(firstResponse.getBody()).path("data");
        JsonNode secondView = objectMapper.readTree(secondResponse.getBody()).path("data");

        assertThat(firstResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(firstView.path("heatScore").asLong()).isEqualTo(3L);
        assertThat(secondView.path("heatScore").asLong()).isEqualTo(4L);

        List<CompletableFuture<Integer>> updates = new ArrayList<>();
        for (int index = 0; index < CONCURRENT_VIEWS; index++) {
            updates.add(CompletableFuture.supplyAsync(
                    () -> diaryMapper.incrementHeatScore(viewedDiary.getId())));
        }
        CompletableFuture.allOf(updates.toArray(CompletableFuture[]::new)).join();
        assertThat(updates).allSatisfy(update -> assertThat(update.join()).isEqualTo(1));

        Diary refreshed = diaryMapper.selectById(viewedDiary.getId());
        assertThat(refreshed.getHeatScore()).isEqualTo(4L + CONCURRENT_VIEWS);

        DiaryListQuery query = new DiaryListQuery();
        query.setDestinationId(destination.getId());
        query.setSortBy("heat");
        query.setPageSize(10);
        PageResultVO<DiaryVO> page = diaryService.listDiaries(query);

        assertThat(page.getList()).extracting(DiaryVO::getId)
                .containsExactly(viewedDiary.getId(), otherDiary.getId());
    }

    private User insertUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("integration-test-password-not-used");
        user.setNickname(username);
        user.setRole("user");
        user.setStatus(1);
        userMapper.insert(user);
        userIds.add(user.getId());
        return user;
    }

    private Destination insertDestination(String name) {
        Destination destination = new Destination();
        destination.setName(name);
        destination.setType("campus");
        destination.setCategory("integration-test");
        destination.setCity("Beijing");
        destination.setDescription("Diary heat integration fixture");
        destination.setHeatScore(BigDecimal.ZERO);
        destination.setRatingScore(BigDecimal.ZERO);
        destination.setTagJson("[]");
        destination.setStatus(1);
        destinationMapper.insert(destination);
        destinationIds.add(destination.getId());
        return destination;
    }

    private Diary insertDiary(Long userId, Long destinationId, String title, Long heatScore) {
        Diary diary = new Diary();
        diary.setUserId(userId);
        diary.setDestinationId(destinationId);
        diary.setTitle(title);
        diary.setContentText("Diary heat integration content");
        diary.setHeatScore(heatScore);
        diary.setRatingScore(BigDecimal.ZERO);
        diary.setRatingCount(0);
        diary.setVisibility("public");
        diary.setStatus(1);
        diaryMapper.insert(diary);
        diaryIds.add(diary.getId());
        return diary;
    }

    private boolean hasDatabasePassword() {
        String password = System.getenv("DB_PASSWORD");
        return password != null && !password.isBlank();
    }
}
