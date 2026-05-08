package com.trip.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 50, message = "长度不能超过50")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过64")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
