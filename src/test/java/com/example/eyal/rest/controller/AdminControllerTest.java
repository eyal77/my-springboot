package com.example.eyal.rest.controller;

import com.example.eyal.rest.model.Token;
import com.example.eyal.rest.model.User;
import com.example.eyal.rest.model.UserRole;
import com.example.eyal.rest.repository.TokenRepository;
import com.example.eyal.rest.repository.UserRepository;
import com.example.eyal.rest.security.SecurityFilter;
import com.example.eyal.rest.service.SystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {

    private MockMvc mockMvc;
    private UserRepository userRepository;
    private TokenRepository tokenRepository;

    private String rootToken;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        tokenRepository = new TokenRepository();

        // Create standard test users
        userRepository.save(new User("test-admin", "adminpass", UserRole.ADMIN));
        userRepository.save(new User("test-user", "userpass", UserRole.USER));

        // Register session/API tokens
        rootToken = "root-session-token";
        tokenRepository.save(new Token(rootToken, "root", UserRole.ROOT, LocalDateTime.now()));

        adminToken = "admin-session-token";
        tokenRepository.save(new Token(adminToken, "test-admin", UserRole.ADMIN, LocalDateTime.now()));

        userToken = "user-session-token";
        tokenRepository.save(new Token(userToken, "test-user", UserRole.USER, LocalDateTime.now()));

        AdminController adminController = new AdminController(userRepository, tokenRepository);
        SystemController systemController = new SystemController(new SystemService());
        SecurityFilter securityFilter = new SecurityFilter(tokenRepository);

        this.mockMvc = MockMvcBuilders.standaloneSetup(adminController, systemController)
                .addFilters(securityFilter)
                .build();
    }

    @Test
    void anonymous_ShouldBeUnauthorizedForSecuredEndpoints() throws Exception {
        mockMvc.perform(get("/api/system-info"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRole_ShouldBeAllowedForSystemInfoButForbiddenForAdmin() throws Exception {
        mockMvc.perform(get("/api/system-info")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void rootRole_ShouldBeAllowedToManageUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + rootToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + rootToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newadmin\",\"password\":\"pass\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void adminRole_ShouldBeAllowedToManageTokensButForbiddenForUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/tokens")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test-user\"}"))
                .andExpect(status().isCreated());
    }
}
