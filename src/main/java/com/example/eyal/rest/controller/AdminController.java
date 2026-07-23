package com.example.eyal.rest.controller;

import com.example.eyal.rest.model.Token;
import com.example.eyal.rest.model.User;
import com.example.eyal.rest.model.UserRole;
import com.example.eyal.rest.repository.TokenRepository;
import com.example.eyal.rest.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administration", description = "Endpoints for ROOT to manage users and ADMIN/ROOT to manage access tokens")
public class AdminController {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    public AdminController(UserRepository userRepository, TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    // --- User Administration (ROOT only) ---

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Retrieve list of all registered users (ROOT only).")
    public ResponseEntity<List<User>> listUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/users")
    @Operation(summary = "Create user", description = "Register a new user with root, admin, or user role (ROOT only).")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().isBlank() ||
            user.getPassword() == null || user.getPassword().isBlank() ||
            user.getRole() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username, password, and role are required."));
        }

        String username = user.getUsername().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists."));
        }

        User newUser = new User(username, user.getPassword(), user.getRole());
        userRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
    }

    @DeleteMapping("/users/{username}")
    @Operation(summary = "Delete user", description = "Delete a user account by username. Note: 'root' cannot be deleted (ROOT only).")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        if ("root".equalsIgnoreCase(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "The root user cannot be deleted."));
        }

        boolean deleted = userRepository.delete(username);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found."));
        }

        // Revoke all tokens associated with the deleted user
        tokenRepository.deleteByUsername(username);
        return ResponseEntity.ok(Map.of("message", "User and associated tokens deleted successfully."));
    }

    // --- Token Administration (ADMIN or ROOT) ---

    @GetMapping("/tokens")
    @Operation(summary = "List all tokens", description = "Retrieve all active authentication and session tokens (ADMIN or ROOT only).")
    public ResponseEntity<List<Token>> listTokens() {
        return ResponseEntity.ok(tokenRepository.findAll());
    }

    @PostMapping("/tokens")
    @Operation(summary = "Generate user access token", description = "Creates a new access token for a specific user (ADMIN or ROOT only).")
    public ResponseEntity<?> generateToken(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required."));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User does not exist. Create user first."));
        }

        User user = userOpt.get();
        String tokenVal = UUID.randomUUID().toString();
        Token token = new Token(tokenVal, user.getUsername(), user.getRole(), LocalDateTime.now());
        tokenRepository.save(token);

        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @DeleteMapping("/tokens/{tokenValue}")
    @Operation(summary = "Revoke access token", description = "Revokes and deletes an active token (ADMIN or ROOT only).")
    public ResponseEntity<?> revokeToken(@PathVariable String tokenValue) {
        boolean deleted = tokenRepository.delete(tokenValue);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Token not found."));
        }
        return ResponseEntity.ok(Map.of("message", "Token revoked successfully."));
    }
}
