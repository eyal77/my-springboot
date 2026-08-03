package com.example.eyal.rest.controller;

import com.example.eyal.rest.model.Token;
import com.example.eyal.rest.model.User;
import com.example.eyal.rest.repository.TokenRepository;
import com.example.eyal.rest.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for user session login")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    public AuthController(UserRepository userRepository, TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Verifies username and password, returning a new session token.")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        log.debug("login API called for username: {}", username);

        if (username == null || password == null) {
            log.warn("login: Bad request. Username or password parameter was null.");
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required."));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)) {
            log.warn("login: Unauthorized. Invalid credentials submitted for username: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password."));
        }

        User user = userOpt.get();
        String tokenVal = UUID.randomUUID().toString();
        Token sessionToken = new Token(tokenVal, user.getUsername(), user.getRole(), LocalDateTime.now(), LocalDateTime.now().plusHours(24));
        tokenRepository.save(sessionToken);

        log.info("login: Successful login for user: {}, role: {}, generated token starts with: {}", 
            user.getUsername(), user.getRole(), tokenVal.substring(0, 5));

        return ResponseEntity.ok(Map.of(
            "token", tokenVal,
            "username", user.getUsername(),
            "role", user.getRole().name()
        ));
    }
}
