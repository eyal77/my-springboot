package com.example.eyal.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

public class Token {
    private String tokenValue;
    private String username;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public Token() {}

    public Token(String tokenValue, String username, UserRole role, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.tokenValue = tokenValue;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @JsonIgnore
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
