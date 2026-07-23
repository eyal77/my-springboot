package com.example.eyal.rest.model;

import java.time.LocalDateTime;

public class Token {
    private String tokenValue;
    private String username;
    private UserRole role;
    private LocalDateTime createdAt;

    public Token() {}

    public Token(String tokenValue, String username, UserRole role, LocalDateTime createdAt) {
        this.tokenValue = tokenValue;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
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
}
