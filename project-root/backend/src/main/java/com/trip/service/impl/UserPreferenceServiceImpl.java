package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.common.ErrorCode;
import com.trip.dto.request.UserPreferenceRequest;
import com.trip.entity.User;
import com.trip.entity.UserPreference;
import com.trip.exception.BusinessException;
import com.trip.mapper.UserMapper;
import com.trip.mapper.UserPreferenceMapper;
import com.trip.security.JwtClaims;
import com.trip.service.UserPreferenceService;
import com.trip.vo.response.UserPreferenceVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private static final int ENABLED_STATUS = 1;
    private static final int MAX_THEME_JSON_LENGTH = 255;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final UserPreferenceMapper userPreferenceMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public UserPreferenceServiceImpl(
            UserPreferenceMapper userPreferenceMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper) {
        this.userPreferenceMapper = userPreferenceMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserPreferenceVO getCurrentPreference() {
        Long userId = currentUserId();
        ensureActiveUser(userId);

        UserPreference preference = findByUserId(userId);
        if (preference == null) {
            return UserPreferenceVO.empty(userId);
        }
        return toVO(preference);
    }

    @Override
    @Transactional
    public UserPreferenceVO saveCurrentPreference(UserPreferenceRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.COMMON_001);
        }

        Long userId = currentUserId();
        ensureActiveUser(userId);

        String themeJson = toThemeJson(request.getPreferThemeList());
        UserPreference preference = findByUserId(userId);
        if (preference == null) {
            preference = new UserPreference();
            preference.setUserId(userId);
        }

        preference.setPreferHotLevel(request.getPreferHotLevel());
        preference.setPreferTheme(themeJson);
        preference.setPreferFoodType(normalize(request.getPreferFoodType()));
        preference.setPreferCrowdLevel(request.getPreferCrowdLevel());
        preference.setTravelStyle(normalize(request.getTravelStyle()));
        preference.setCustomPreferenceText(normalize(request.getCustomPreferenceText()));

        if (preference.getId() == null) {
            int inserted = userPreferenceMapper.insert(preference);
            if (inserted != 1 || preference.getId() == null) {
                throw new BusinessException(ErrorCode.COMMON_006);
            }
        } else {
            int updated = userPreferenceMapper.updateById(preference);
            if (updated != 1) {
                throw new BusinessException(ErrorCode.COMMON_006);
            }
            preference = userPreferenceMapper.selectById(preference.getId());
        }
        return toVO(preference);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtClaims claims)) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }
        return claims.getUserId();
    }

    private void ensureActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_009);
        }
        if (user.getStatus() == null || user.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.AUTH_006);
        }
    }

    private UserPreference findByUserId(Long userId) {
        return userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getUserId, userId));
    }

    private UserPreferenceVO toVO(UserPreference preference) {
        return UserPreferenceVO.from(preference, parseThemeList(preference.getPreferTheme()));
    }

    private String toThemeJson(List<String> themes) {
        List<String> normalizedThemes = new ArrayList<>();
        if (themes != null) {
            for (String theme : themes) {
                String normalizedTheme = normalize(theme);
                if (StringUtils.hasText(normalizedTheme)) {
                    normalizedThemes.add(normalizedTheme);
                }
            }
        }

        try {
            String themeJson = objectMapper.writeValueAsString(normalizedThemes);
            if (themeJson.length() > MAX_THEME_JSON_LENGTH) {
                throw new BusinessException(ErrorCode.COMMON_002, "preferThemeList 长度超出存储限制");
            }
            return themeJson;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.COMMON_002);
        }
    }

    private List<String> parseThemeList(String preferTheme) {
        if (!StringUtils.hasText(preferTheme)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(preferTheme, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return List.of(preferTheme);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }
}
