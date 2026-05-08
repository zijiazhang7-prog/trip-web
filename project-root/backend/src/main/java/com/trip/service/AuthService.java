package com.trip.service;

import com.trip.dto.request.LoginRequest;
import com.trip.dto.request.RegisterRequest;
import com.trip.vo.response.LoginResponse;
import com.trip.vo.response.RegisterResponse;
import com.trip.vo.response.UserVO;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserVO getCurrentUser(String authorizationHeader);
}
