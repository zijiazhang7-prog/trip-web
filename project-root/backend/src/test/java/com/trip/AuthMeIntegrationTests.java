package com.trip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.entity.User;
import com.trip.mapper.UserMapper;
import com.trip.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthMeIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
    void meShouldReturnCurrentUser() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User user = createUser(1);
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.username").value(user.getUsername()))
                .andExpect(jsonPath("$.data.role").value("user"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void meShouldRejectMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    @Test
    void meShouldRejectInvalidAuthorizationFormat() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Token abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    @Test
    void meShouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void meShouldRejectMissingUser() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User user = new User();
        user.setId(9_223_372_036_854_775_000L);
        user.setUsername(nextUsername());
        user.setRole("user");
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_009"));
    }

    @Test
    void meShouldRejectDisabledUser() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        User user = createUser(0);
        String token = jwtTokenProvider.generateToken(user);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    private User createUser(int status) {
        String username = nextUsername();
        usernamesToClean.add(username);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setNickname("测试当前用户");
        user.setRole("user");
        user.setStatus(status);
        userMapper.insert(user);
        return user;
    }

    private String nextUsername() {
        return "test_me_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
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
