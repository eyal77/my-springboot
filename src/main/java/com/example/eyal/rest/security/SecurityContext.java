package com.example.eyal.rest.security;

import com.example.eyal.rest.model.UserRole;

public class SecurityContext {
    private static final ThreadLocal<UserRole> currentRole = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUsername = new ThreadLocal<>();

    public static void set(String username, UserRole role) {
        currentUsername.set(username);
        currentRole.set(role);
    }

    public static UserRole getCurrentRole() {
        return currentRole.get();
    }

    public static String getCurrentUsername() {
        return currentUsername.get();
    }

    public static void clear() {
        currentUsername.remove();
        currentRole.remove();
    }
}
