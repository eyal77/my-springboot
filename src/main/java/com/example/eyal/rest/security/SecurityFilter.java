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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Order(1)
public class SecurityFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);
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
        log.debug("doFilter entry: URI path intercepted: {}", path);

        // 1. Allow public paths
        if (isPublicPath(path)) {
            log.debug("doFilter: Public path allowed: {}", path);
            chain.doFilter(request, response);
            return;
        }

        // 2. Extract Token
        String tokenVal = extractToken(httpRequest);
        if (tokenVal == null || tokenVal.isBlank()) {
            log.warn("doFilter: Authentication token missing for request: {}", path);
            sendUnauthorized(httpResponse, "Authentication token is missing.");
            return;
        }

        Optional<Token> tokenOpt = tokenRepository.findByTokenValue(tokenVal);
        if (tokenOpt.isEmpty()) {
            log.warn("doFilter: Invalid token submitted for request: {}", path);
            sendUnauthorized(httpResponse, "Invalid or expired token.");
            return;
        }

        Token token = tokenOpt.get();
        if (token.isExpired()) {
            log.warn("doFilter: Token has expired for username: {} (request path: {})", token.getUsername(), path);
            tokenRepository.delete(tokenVal);
            sendUnauthorized(httpResponse, "Token has expired.");
            return;
        }
        UserRole role = token.getRole();
        log.debug("doFilter: Valid token found. User: {}, Role: {}", token.getUsername(), role);
        SecurityContext.set(token.getUsername(), role);

        try {
            // 3. Enforce Role Permissions
            if (path.startsWith("/api/admin/users")) {
                // Root only
                if (role != UserRole.ROOT) {
                    log.warn("doFilter: Forbidden. ROOT role required for path {}, actual role: {}", path, role);
                    sendForbidden(httpResponse, "Access denied: Root privilege required.");
                    return;
                }
            } else if (path.startsWith("/api/admin/tokens")) {
                // Admin or Root
                if (role != UserRole.ADMIN && role != UserRole.ROOT) {
                    log.warn("doFilter: Forbidden. ADMIN or ROOT role required for path {}, actual role: {}", path, role);
                    sendForbidden(httpResponse, "Access denied: Admin or Root privilege required.");
                    return;
                }
            } else if (path.startsWith("/api/system-info")) {
                // Anyone with valid token (USER, ADMIN, ROOT)
                // Permission granted
            }

            log.debug("doFilter: Access authorized. Forwarding filter chain.");
            chain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
            log.debug("doFilter exit: Cleared SecurityContext");
        }
    }

    private boolean isPublicPath(String path) {
        boolean result = path.equals("/") ||
               path.startsWith("/index.html") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/favicon.ico") ||
               path.equals("/api/auth/login") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.equals("/admin") ||
               path.equals("/admin/");
        log.debug("isPublicPath lookup: path={}, result={}", path, result);
        return result;
    }

    private String extractToken(HttpServletRequest request) {
        // Check standard Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("extractToken: Found bearer token in Authorization header");
            return authHeader.substring(7).trim();
        }
        // Fallback to query parameter (e.g. ?token=...)
        String paramToken = request.getParameter("token");
        if (paramToken != null) {
            log.debug("extractToken: Found token in request query parameter");
        }
        return paramToken;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        log.debug("sendUnauthorized response triggered: {}", message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message));
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        log.debug("sendForbidden response triggered: {}", message);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Forbidden\", \"message\": \"%s\"}", message));
    }
}
