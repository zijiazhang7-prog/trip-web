package com.trip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.dto.request.RegisterRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRegisterIntegrationTests {

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
    void registerShouldCreateUserWithPasswordHash() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        String username = nextUsername();
        usernamesToClean.add(username);

        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("123456");
        request.setNickname("测试用户");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("created"))
                .andExpect(jsonPath("$.data.userId").isNumber());

        User savedUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("123456");
        assertThat(passwordEncoder.matches("123456", savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getRole()).isEqualTo("user");
        assertThat(savedUser.getStatus()).isEqualTo(1);
    }

    @Test
    void registerShouldRejectDuplicateUsername() throws Exception {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");
        String username = nextUsername();
        usernamesToClean.add(username);

        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("123456");
        request.setNickname("测试用户");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    void registerShouldRejectBlankUsername() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setPassword("123456");
        request.setNickname("测试用户");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    @Test
    void registerShouldRejectInvalidUsernameCharacters() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("bad\nname");
        request.setPassword("123456");
        request.setNickname("测试用户");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private String nextUsername() {
        return "test_register_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
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
