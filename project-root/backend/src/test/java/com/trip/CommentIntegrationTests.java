package com.trip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.entity.Destination;
import com.trip.entity.DestinationComment;
import com.trip.entity.Diary;
import com.trip.entity.DiaryComment;
import com.trip.entity.Food;
import com.trip.entity.FoodComment;
import com.trip.entity.User;
import com.trip.mapper.DestinationCommentMapper;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryCommentMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.FoodCommentMapper;
import com.trip.mapper.FoodMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DestinationMapper destinationMapper;

    @Autowired
    private FoodMapper foodMapper;

    @Autowired
    private DiaryMapper diaryMapper;

    @Autowired
    private DestinationCommentMapper destinationCommentMapper;

    @Autowired
    private FoodCommentMapper foodCommentMapper;

    @Autowired
    private DiaryCommentMapper diaryCommentMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> destinationIds = new ArrayList<>();
    private final List<Long> foodIds = new ArrayList<>();
    private final List<Long> diaryIds = new ArrayList<>();

    @AfterEach
    void cleanFixtures() {
        if (!diaryIds.isEmpty()) {
            diaryCommentMapper.delete(new LambdaQueryWrapper<DiaryComment>()
                    .in(DiaryComment::getDiaryId, diaryIds));
        }
        if (!foodIds.isEmpty()) {
            foodCommentMapper.delete(new LambdaQueryWrapper<FoodComment>()
                    .in(FoodComment::getFoodId, foodIds));
        }
        if (!destinationIds.isEmpty()) {
            destinationCommentMapper.delete(new LambdaQueryWrapper<DestinationComment>()
                    .in(DestinationComment::getDestinationId, destinationIds));
        }
        diaryIds.forEach(diaryMapper::deleteById);
        foodIds.forEach(foodMapper::deleteById);
        destinationIds.forEach(destinationMapper::deleteById);
        userIds.forEach(userMapper::deleteById);
    }

    @Test
    void commentWritesShouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/diaries/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentText\":\"test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_003"));

        mockMvc.perform(delete("/api/v1/comments/diary/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    @Test
    void threeTargetCommentsShouldCreateListAndSoftDelete() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        String marker = "comment_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User owner = insertUser(marker + "_owner", "user");
        User other = insertUser(marker + "_other", "user");
        User admin = insertUser(marker + "_admin", "admin");
        Destination destination = insertDestination(marker);
        Food food = insertFood(destination.getId(), marker);
        Diary diary = insertDiary(owner.getId(), destination.getId(), marker, "public", 1);
        Diary privateDiary = insertDiary(owner.getId(), destination.getId(), marker + "_private", "private", 1);
        String ownerToken = jwtTokenProvider.generateToken(owner);

        long destinationCommentId = createComment(
                "/api/v1/destinations/" + destination.getId() + "/comments",
                ownerToken,
                "目的地评论");
        createComment(
                "/api/v1/foods/" + food.getId() + "/comments",
                ownerToken,
                "美食评论");
        createComment(
                "/api/v1/diaries/" + diary.getId() + "/comments",
                ownerToken,
                "日记评论");

        mockMvc.perform(get("/api/v1/destinations/{id}/comments", destination.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].targetType").value("destination"))
                .andExpect(jsonPath("$.data.list[0].contentText").value("目的地评论"));

        mockMvc.perform(post("/api/v1/diaries/{id}/comments", privateDiary.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentText\":\"不可发布\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COMMENT_001"));

        mockMvc.perform(delete("/api/v1/comments/destination/{id}", destinationCommentId)
                        .header("Authorization", "Bearer " + jwtTokenProvider.generateToken(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_004"));

        mockMvc.perform(delete("/api/v1/comments/destination/{id}", destinationCommentId)
                        .header("Authorization", "Bearer " + jwtTokenProvider.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/v1/destinations/{id}/comments", destination.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        DestinationComment hidden = destinationCommentMapper.selectById(destinationCommentId);
        org.junit.jupiter.api.Assertions.assertEquals(0, hidden.getStatus());
    }

    private long createComment(String path, String token, String content) throws Exception {
        String response = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentText\":\"" + content + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response)
                .path("data")
                .path("id")
                .asLong();
    }

    private User insertUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("integration-test-password-not-used");
        user.setNickname(username);
        user.setRole(role);
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
        destination.setHeatScore(BigDecimal.ZERO);
        destination.setRatingScore(BigDecimal.ZERO);
        destination.setStatus(1);
        destinationMapper.insert(destination);
        destinationIds.add(destination.getId());
        return destination;
    }

    private Food insertFood(Long destinationId, String name) {
        Food food = new Food();
        food.setDestinationId(destinationId);
        food.setName(name);
        food.setHeatScore(BigDecimal.ZERO);
        food.setRatingScore(BigDecimal.ZERO);
        foodMapper.insert(food);
        foodIds.add(food.getId());
        return food;
    }

    private Diary insertDiary(
            Long userId,
            Long destinationId,
            String title,
            String visibility,
            int status) {
        Diary diary = new Diary();
        diary.setUserId(userId);
        diary.setDestinationId(destinationId);
        diary.setTitle(title);
        diary.setContentText("Comment integration content");
        diary.setHeatScore(0L);
        diary.setRatingScore(BigDecimal.ZERO);
        diary.setRatingCount(0);
        diary.setVisibility(visibility);
        diary.setStatus(status);
        diaryMapper.insert(diary);
        diaryIds.add(diary.getId());
        return diary;
    }

    private boolean hasDatabasePassword() {
        String password = System.getenv("DB_PASSWORD");
        return password != null && !password.isBlank();
    }
}
