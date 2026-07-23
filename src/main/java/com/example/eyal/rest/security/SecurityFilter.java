package com.example.eyal.rest.security;

import com.example.eyal.rest.model.Token;
import com.example.eyal.rest.model.UserRole;
import com.example.eyal.rest.repository.TokenRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(1)
public class SecurityFilter implements Filter {

    private final TokenRepository tokenRepository;

    public SecurityFilter(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // 1. Allow public paths
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Extract Token
        String tokenVal = extractToken(httpRequest);
        if (tokenVal == null || tokenVal.isBlank()) {
            sendUnauthorized(httpResponse, "Authentication token is missing.");
            return;
        }

        Optional<Token> tokenOpt = tokenRepository.findByTokenValue(tokenVal);
        if (tokenOpt.isEmpty()) {
            sendUnauthorized(httpResponse, "Invalid or expired token.");
            return;
        }

        Token token = tokenOpt.get();
        if (token.isExpired()) {
            tokenRepository.delete(tokenVal);
            sendUnauthorized(httpResponse, "Token has expired.");
            return;
        }
        UserRole role = token.getRole();
        SecurityContext.set(token.getUsername(), role);

        try {
            // 3. Enforce Role Permissions
            if (path.startsWith("/api/admin/users")) {
                // Root only
                if (role != UserRole.ROOT) {
                    sendForbidden(httpResponse, "Access denied: Root privilege required.");
                    return;
                }
            } else if (path.startsWith("/api/admin/tokens")) {
                // Admin or Root
                if (role != UserRole.ADMIN && role != UserRole.ROOT) {
                    sendForbidden(httpResponse, "Access denied: Admin or Root privilege required.");
                    return;
                }
            } else if (path.startsWith("/api/system-info")) {
                // Anyone with valid token (USER, ADMIN, ROOT)
                // Permission granted
            }

            chain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }

    private boolean isPublicPath(String path) {
        return path.equals("/") ||
               path.startsWith("/index.html") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/favicon.ico") ||
               path.equals("/api/auth/login") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui");
    }

    private String extractToken(HttpServletRequest request) {
        // Check standard Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        // Fallback to query parameter (e.g. ?token=...)
        return request.getParameter("token");
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message));
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Forbidden\", \"message\": \"%s\"}", message));
    }
}
