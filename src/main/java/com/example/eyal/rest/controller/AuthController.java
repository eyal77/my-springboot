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

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for user session login")
public class AuthController {

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

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required."));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password."));
        }

        User user = userOpt.get();
        String tokenVal = UUID.randomUUID().toString();
        Token sessionToken = new Token(tokenVal, user.getUsername(), user.getRole(), LocalDateTime.now());
        tokenRepository.save(sessionToken);

        return ResponseEntity.ok(Map.of(
            "token", tokenVal,
            "username", user.getUsername(),
            "role", user.getRole().name()
        ));
    }
}
