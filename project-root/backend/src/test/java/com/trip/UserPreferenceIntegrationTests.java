package com.trip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.dto.request.UserPreferenceRequest;
import com.trip.entity.User;
import com.trip.entity.UserPreference;
import com.trip.mapper.UserMapper;
import com.trip.mapper.UserPreferenceMapper;
import com.trip.security.JwtTokenProvider;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserPreferenceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserPreferenceMapper userPreferenceMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final List<Long> userIdsToClean = new ArrayList<>();

    @AfterEach
    void cleanTestData() {
        if (userIdsToClean.isEmpty() || !hasDatabasePassword()) {
            return;
        }
        userPreferenceMapper.delete(new LambdaQueryWrapper<UserPreference>()
                .in(UserPreference::getUserId, userIdsToClean));
        userMapper.delete(new LambdaQueryWrapper<User>()
                .in(User::getId, userIdsToClean));
    }

    @Test
    void getPreferenceShouldRejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/user-preferences/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    @Test
    void putPreferenceShouldRejectMissingToken() throws Exception {
        mockMvc.perform(put("/api/v1/user-preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    @Test
    void getPreferenceShouldReturnEmptyPreferenceWhenNotSet() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User user = createUser();

        mockMvc.perform(get("/api/v1/user-preferences/me")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.preferThemeList").isArray())
                .andExpect(jsonPath("$.data.preferThemeList").isEmpty());
    }

    @Test
    void putPreferenceShouldCreateAndReturnCurrentPreference() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User user = createUser();

        mockMvc.perform(put("/api/v1/user-preferences/me")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.preferHotLevel").value(3))
                .andExpect(jsonPath("$.data.preferThemeList[0]").value("人文建筑型"))
                .andExpect(jsonPath("$.data.preferThemeList[1]").value("自然景观型"))
                .andExpect(jsonPath("$.data.preferFoodType").value("面食"))
                .andExpect(jsonPath("$.data.preferCrowdLevel").value(2))
                .andExpect(jsonPath("$.data.travelStyle").value("轻松"))
                .andExpect(jsonPath("$.data.customPreferenceText").value("更喜欢安静、人少、适合拍照的地方"));

        Long count = userPreferenceMapper.selectCount(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, user.getId()));
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void putPreferenceShouldUpdateExistingPreference() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User user = createUser();

        mockMvc.perform(put("/api/v1/user-preferences/me")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk());

        UserPreferenceRequest updateRequest = validRequest();
        updateRequest.setPreferThemeList(List.of("繁华商城型"));
        updateRequest.setPreferHotLevel(5);

        mockMvc.perform(put("/api/v1/user-preferences/me")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferHotLevel").value(5))
                .andExpect(jsonPath("$.data.preferThemeList[0]").value("繁华商城型"));

        Long count = userPreferenceMapper.selectCount(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, user.getId()));
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void preferenceShouldBeIsolatedByCurrentUser() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User firstUser = createUser();
        User secondUser = createUser();

        UserPreferenceRequest firstRequest = validRequest();
        firstRequest.setPreferThemeList(List.of("人文建筑型"));
        mockMvc.perform(put("/api/v1/user-preferences/me")
                        .header("Authorization", bearer(firstUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/user-preferences/me")
                        .header("Authorization", bearer(secondUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(secondUser.getId()))
                .andExpect(jsonPath("$.data.preferThemeList").isEmpty());
    }

    @Test
    void putPreferenceShouldRejectInvalidLevel() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User user = createUser();
        UserPreferenceRequest request = validRequest();
        request.setPreferHotLevel(6);

        mockMvc.perform(put("/api/v1/user-preferences/me")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void putPreferenceShouldRejectTooManyThemes() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User user = createUser();
        UserPreferenceRequest request = validRequest();
        request.setPreferThemeList(List.of("t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10", "t11"));

        mockMvc.perform(put("/api/v1/user-preferences/me")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void putPreferenceShouldRejectTooLongCustomText() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User user = createUser();
        UserPreferenceRequest request = validRequest();
        request.setCustomPreferenceText("x".repeat(501));

        mockMvc.perform(put("/api/v1/user-preferences/me")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private UserPreferenceRequest validRequest() {
        UserPreferenceRequest request = new UserPreferenceRequest();
        request.setPreferHotLevel(3);
        request.setPreferThemeList(List.of("人文建筑型", "自然景观型"));
        request.setPreferFoodType("面食");
        request.setPreferCrowdLevel(2);
        request.setTravelStyle("轻松");
        request.setCustomPreferenceText("更喜欢安静、人少、适合拍照的地方");
        return request;
    }

    private User createUser() {
        User user = new User();
        user.setUsername(nextUsername());
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setNickname("测试偏好用户");
        user.setRole("user");
        user.setStatus(1);
        userMapper.insert(user);
        userIdsToClean.add(user.getId());
        return user;
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenProvider.generateToken(user);
    }

    private String nextUsername() {
        return "test_pref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private boolean hasDatabasePassword() {
        String propertyPassword = System.getProperty("spring.datasource.password");
        if (propertyPassword != null) {
            return true;
        }

        String password = System.getenv("DB_PASSWORD");
        return password != null;
    }
}
