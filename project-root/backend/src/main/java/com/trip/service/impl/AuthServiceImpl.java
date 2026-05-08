package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.common.ErrorCode;
import com.trip.dto.request.LoginRequest;
import com.trip.dto.request.RegisterRequest;
import com.trip.entity.User;
import com.trip.exception.BusinessException;
import com.trip.mapper.UserMapper;
import com.trip.security.JwtClaims;
import com.trip.security.JwtTokenProvider;
import com.trip.service.AuthService;
import com.trip.vo.response.LoginResponse;
import com.trip.vo.response.RegisterResponse;
import com.trip.vo.response.UserVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "user";
    private static final int ENABLED_STATUS = 1;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = normalize(request.getUsername());
        String password = request.getPassword();
        String nickname = normalize(request.getNickname());

        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.AUTH_007);
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.AUTH_008);
        }
        if (existsByUsername(username)) {
            throw new BusinessException(ErrorCode.AUTH_001);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setRole(DEFAULT_ROLE);
        user.setStatus(ENABLED_STATUS);

        int inserted = userMapper.insert(user);
        if (inserted != 1 || user.getId() == null) {
            throw new BusinessException(ErrorCode.AUTH_010);
        }
        return new RegisterResponse(user.getId());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = normalize(request.getUsername());
        String password = request.getPassword();

        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.AUTH_007);
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.AUTH_008);
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_002);
        }
        if (user.getStatus() == null || user.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.AUTH_006);
        }

        String token = jwtTokenProvider.generateToken(user);
        return new LoginResponse(token, UserVO.from(user));
    }

    @Override
    public UserVO getCurrentUser(String authorizationHeader) {
        JwtClaims claims = resolveClaims(authorizationHeader);

        User user = userMapper.selectById(claims.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_009);
        }
        if (user.getStatus() == null || user.getStatus() != ENABLED_STATUS) {
            throw new BusinessException(ErrorCode.AUTH_006);
        }
        return UserVO.from(user);
    }

    private boolean existsByUsername(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        return count != null && count > 0;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }

        String token = authorizationHeader.substring(prefix.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }
        return token;
    }

    private JwtClaims resolveClaims(String authorizationHeader) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtClaims claims) {
            return claims;
        }

        String token = extractBearerToken(authorizationHeader);
        try {
            return jwtTokenProvider.parseToken(token);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.AUTH_004);
        }
    }
}
