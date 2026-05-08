package com.trip.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.common.ApiResponse;
import com.trip.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 认证过滤器，将合法 Bearer Token 转换为 Spring Security 上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorizationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            writeError(response, ErrorCode.AUTH_003, HttpStatus.UNAUTHORIZED);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            writeError(response, ErrorCode.AUTH_003, HttpStatus.UNAUTHORIZED);
            return;
        }

        try {
            JwtClaims claims = jwtTokenProvider.parseToken(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    claims,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.getRole())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            writeError(response, ErrorCode.AUTH_004, HttpStatus.UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
    }
}
