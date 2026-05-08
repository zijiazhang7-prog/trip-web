package com.trip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.dto.request.LoginRequest;
import com.trip.entity.User;
import com.trip.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> usernamesToClean = new ArrayList<>();

    @AfterEach
    void cleanTestUsers() {
        if (usernamesToClean.isEmpty() || !hasDatabasePassword()) {
            return;
        }
        userMapper.delete(new LambdaQueryWrapper<User>()
                .in(User::getUsername, usernamesToClean));
    }

    @Test
    void loginShouldReturnTokenAndUserInfo() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        String username = createUser(1);

        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.user.username").value(username))
                .andExpect(jsonPath("$.data.user.role").value("user"));
    }

    @Test
    void loginShouldRejectWrongPassword() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        String username = createUser(1);

        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("wrong123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void loginShouldRejectUnknownUser() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");

        LoginRequest request = new LoginRequest();
        request.setUsername(nextUsername());
        request.setPassword("123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }

    @Test
    void loginShouldRejectDisabledUser() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        String username = createUser(0);

        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword("123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    @Test
    void loginShouldRejectBlankUsername() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void loginShouldRejectInvalidUsernameCharacters() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("bad name");
        request.setPassword("123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private String createUser(int status) {
        String username = nextUsername();
        usernamesToClean.add(username);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setNickname("测试登录用户");
        user.setRole("user");
        user.setStatus(status);
        userMapper.insert(user);
        return username;
    }

    private String nextUsername() {
        return "test_login_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
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
