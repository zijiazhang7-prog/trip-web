package com.trip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.dto.request.DiaryListQuery;
import com.trip.dto.request.DiaryRatingRequest;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.DiaryRating;
import com.trip.entity.User;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.DiaryRatingMapper;
import com.trip.mapper.UserMapper;
import com.trip.security.JwtClaims;
import com.trip.service.DiaryRatingService;
import com.trip.service.DiaryService;
import com.trip.vo.response.DiaryRatingVO;
import com.trip.vo.response.DiaryVO;
import com.trip.vo.response.PageResultVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用 MySQL 实库验证评分覆盖、聚合回写和评分排序。
 */
@SpringBootTest
class DiaryRatingDatabaseIntegrationTests {

    @Autowired
    private DiaryRatingService diaryRatingService;

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private DiaryRatingMapper diaryRatingMapper;

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
        SecurityContextHolder.clearContext();
        if (!diaryIds.isEmpty()) {
            diaryRatingMapper.delete(new LambdaQueryWrapper<DiaryRating>()
                    .in(DiaryRating::getDiaryId, diaryIds));
        }
        diaryIds.forEach(diaryMapper::deleteById);
        destinationIds.forEach(destinationMapper::deleteById);
        userIds.forEach(userMapper::deleteById);
    }

    @Test
    void ratingsShouldUpsertAggregateAndAffectRatingSort() {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        String marker = "rating" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User author = insertUser(marker + "author");
        User visitor = insertUser(marker + "visitor");
        Destination destination = insertDestination(marker);
        Diary first = insertDiary(author.getId(), destination.getId(), marker + "first");
        Diary second = insertDiary(author.getId(), destination.getId(), marker + "second");

        authenticate(author);
        assertThat(diaryRatingService.rateDiary(first.getId(), request(5))).isTrue();
        assertThat(diaryRatingService.rateDiary(first.getId(), request(3))).isTrue();
        assertThat(diaryRatingService.rateDiary(second.getId(), request(2))).isTrue();

        authenticate(visitor);
        assertThat(diaryRatingService.rateDiary(first.getId(), request(5))).isTrue();

        DiaryRatingVO myRating = diaryRatingService.getMyRating(first.getId());
        assertThat(myRating.getUserScore()).isEqualTo(5);
        assertThat(myRating.getRatingScore()).isEqualByComparingTo("4.00");
        assertThat(myRating.getRatingCount()).isEqualTo(2);
        assertThat(diaryRatingMapper.selectCount(new LambdaQueryWrapper<DiaryRating>()
                        .eq(DiaryRating::getDiaryId, first.getId())))
                .isEqualTo(2);

        DiaryListQuery query = new DiaryListQuery();
        query.setDestinationId(destination.getId());
        query.setSortBy("rating");
        query.setPageSize(10);
        PageResultVO<DiaryVO> page = diaryService.listDiaries(query);

        assertThat(page.getList()).extracting(DiaryVO::getId).containsExactly(first.getId(), second.getId());
        assertThat(page.getList().get(0).getRatingCount()).isEqualTo(2);
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
        destination.setDescription("Diary rating integration fixture");
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
        diary.setContentText("Diary rating integration content");
        diary.setHeatScore(BigDecimal.ZERO);
        diary.setRatingScore(BigDecimal.ZERO);
        diary.setRatingCount(0);
        diary.setVisibility("public");
        diary.setStatus(1);
        diaryMapper.insert(diary);
        diaryIds.add(diary.getId());
        return diary;
    }

    private DiaryRatingRequest request(int score) {
        DiaryRatingRequest request = new DiaryRatingRequest();
        request.setScore(score);
        return request;
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtClaims(user.getId(), user.getUsername(), user.getRole(), 1L, 2L),
                null,
                List.of()));
    }

    private boolean hasDatabasePassword() {
        String password = System.getenv("DB_PASSWORD");
        return password != null && !password.isBlank();
    }
}
