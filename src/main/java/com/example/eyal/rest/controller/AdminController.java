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
        com.example.eyal.rest.model.UserRole callerRole = com.example.eyal.rest.security.SecurityContext.getCurrentRole();
        List<Token> allTokens = tokenRepository.findAll();
        
        if (callerRole == com.example.eyal.rest.model.UserRole.ADMIN) {
            // Admin can only view tokens of equal or lesser privilege (ADMIN, USER)
            List<Token> filtered = allTokens.stream()
                    .filter(t -> t.getRole() != com.example.eyal.rest.model.UserRole.ROOT)
                    .toList();
            return ResponseEntity.ok(filtered);
        }
        return ResponseEntity.ok(allTokens);
    }

    @PostMapping("/tokens")
    @Operation(summary = "Generate user access token", description = "Creates a new access token for a specific user (ADMIN or ROOT only). Expiration hours must be between 1 and 24.")
    public ResponseEntity<?> generateToken(@RequestBody Map<String, Object> request) {
        Object usernameObj = request.get("username");
        if (usernameObj == null || usernameObj.toString().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required."));
        }
        String username = usernameObj.toString();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User does not exist. Create user first."));
        }

        User user = userOpt.get();
        com.example.eyal.rest.model.UserRole callerRole = com.example.eyal.rest.security.SecurityContext.getCurrentRole();
        
        // Admin cannot generate a token for ROOT
        if (callerRole == com.example.eyal.rest.model.UserRole.ADMIN && user.getRole() == com.example.eyal.rest.model.UserRole.ROOT) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin is not allowed to generate tokens for users with ROOT privileges."));
        }

        Object expObj = request.get("expirationHours");
        int expirationHours = 24; // default
        if (expObj != null) {
            try {
                if (expObj instanceof Number) {
                    expirationHours = ((Number) expObj).intValue();
                } else {
                    expirationHours = Integer.parseInt(expObj.toString().trim());
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid expirationHours format. Must be an integer."));
            }
        }

        if (expirationHours < 1 || expirationHours > 24) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token expiration hours must be between 1 and 24."));
        }

        String tokenVal = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        Token token = new Token(tokenVal, user.getUsername(), user.getRole(), now, now.plusHours(expirationHours));
        tokenRepository.save(token);

        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @DeleteMapping("/tokens/{tokenValue}")
    @Operation(summary = "Revoke access token", description = "Revokes and deletes an active token (ADMIN or ROOT only).")
    public ResponseEntity<?> revokeToken(@PathVariable String tokenValue) {
        Optional<Token> tokenOpt = tokenRepository.findByTokenValue(tokenValue);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Token not found."));
        }

        Token token = tokenOpt.get();
        com.example.eyal.rest.model.UserRole callerRole = com.example.eyal.rest.security.SecurityContext.getCurrentRole();
        
        // Admin cannot revoke a token belonging to ROOT
        if (callerRole == com.example.eyal.rest.model.UserRole.ADMIN && token.getRole() == com.example.eyal.rest.model.UserRole.ROOT) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin is not allowed to revoke tokens belonging to users with ROOT privileges."));
        }

        tokenRepository.delete(tokenValue);
        return ResponseEntity.ok(Map.of("message", "Token revoked successfully."));
    }
}
