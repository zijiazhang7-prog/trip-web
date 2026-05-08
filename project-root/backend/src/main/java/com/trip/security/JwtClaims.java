package com.trip.security;

public class JwtClaims {

    private final Long userId;
    private final String username;
    private final String role;
    private final long issuedAt;
    private final long expiresAt;

    public JwtClaims(Long userId, String username, String role, long issuedAt, long expiresAt) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
