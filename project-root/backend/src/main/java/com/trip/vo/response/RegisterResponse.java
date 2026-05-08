package com.trip.vo.response;

public class RegisterResponse {

    private Long userId;

    public RegisterResponse() {
    }

    public RegisterResponse(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
