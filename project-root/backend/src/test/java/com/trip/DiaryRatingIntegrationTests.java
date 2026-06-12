package com.trip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.DiaryRating;
import com.trip.entity.User;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.DiaryRatingMapper;
import com.trip.mapper.UserMapper;
import com.trip.security.JwtTokenProvider;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiaryRatingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DestinationMapper destinationMapper;

    @Autowired
    private DiaryMapper diaryMapper;

    @Autowired
    private DiaryRatingMapper diaryRatingMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final List<Long> diaryIds = new ArrayList<>();
    private final List<Long> destinationIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanFixtures() {
        if (!diaryIds.isEmpty()) {
            diaryRatingMapper.delete(new LambdaQueryWrapper<DiaryRating>()
                    .in(DiaryRating::getDiaryId, diaryIds));
        }
        diaryIds.forEach(diaryMapper::deleteById);
        destinationIds.forEach(destinationMapper::deleteById);
        userIds.forEach(userMapper::deleteById);
    }

    @Test
    void postRatingShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/diaries/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    @Test
    void getMyRatingShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/diaries/1/ratings/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    @Test
    void authenticatedUserShouldRateAndReadAggregate() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        String marker = "rating_http_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User user = insertUser(marker);
        Destination destination = insertDestination(marker);
        Diary diary = insertDiary(user.getId(), destination.getId(), marker);
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(post("/api/v1/diaries/{id}/ratings", diary.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(post("/api/v1/diaries/{id}/ratings", diary.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DIARY_008"));

        mockMvc.perform(get("/api/v1/diaries/{id}/ratings/me", diary.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diaryId").value(diary.getId()))
                .andExpect(jsonPath("$.data.userScore").value(5))
                .andExpect(jsonPath("$.data.ratingScore").value(5.0))
                .andExpect(jsonPath("$.data.ratingCount").value(1));
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
        destination.setDescription("Diary rating HTTP fixture");
        destination.setHeatScore(BigDecimal.ZERO);
        destination.setRatingScore(BigDecimal.ZERO);
        destination.setTagJson("[]");
        destination.setStatus(1);
        destinationMapper.insert(destination);
        destinationIds.add(destination.getId());
        return destination;
    }

    private Diary insertDiary(Long userId, Long destinationId, String title) {
        Diary diary = new Diary();
        diary.setUserId(userId);
        diary.setDestinationId(destinationId);
        diary.setTitle(title);
        diary.setContentText("Diary rating HTTP content");
        diary.setHeatScore(0L);
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
