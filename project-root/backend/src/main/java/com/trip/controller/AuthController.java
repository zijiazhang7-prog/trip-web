package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.dto.request.LoginRequest;
import com.trip.dto.request.RegisterRequest;
import com.trip.service.AuthService;
import com.trip.vo.response.LoginResponse;
import com.trip.vo.response.RegisterResponse;
import com.trip.vo.response.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("created", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return ApiResponse.success(authService.getCurrentUser(authorizationHeader));
    }
}
