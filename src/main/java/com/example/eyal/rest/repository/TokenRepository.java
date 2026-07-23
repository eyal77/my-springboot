package com.example.eyal.rest.repository;

import com.example.eyal.rest.model.Token;
import com.example.eyal.rest.security.EncryptionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TokenRepository {
    private final Map<String, Token> tokens = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String filePath;

    public TokenRepository() {
        this("tokens.dat");
    }

    public TokenRepository(String filePath) {
        this.filePath = filePath;
        load();
    }

    private synchronized void load() {
        if (filePath == null) return;

        try {
            Path path = Path.of(filePath);
            if (Files.exists(path)) {
                String encrypted = Files.readString(path);
                if (!encrypted.isBlank()) {
                    String json = EncryptionUtils.decrypt(encrypted);
                    Token[] loadedTokens = objectMapper.readValue(json, Token[].class);
                    for (Token token : loadedTokens) {
                        if (token != null && token.getTokenValue() != null) {
                            tokens.put(token.getTokenValue(), token);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading tokens database: " + e.getMessage());
        }
    }

    private synchronized void persist() {
        if (filePath == null) return;

        try {
            List<Token> tokenList = findAll();
            String json = objectMapper.writeValueAsString(tokenList);
            String encrypted = EncryptionUtils.encrypt(json);
            Files.writeString(Path.of(filePath), encrypted);
        } catch (Exception e) {
            System.err.println("Error persisting tokens database: " + e.getMessage());
        }
    }

    public List<Token> findAll() {
        return new ArrayList<>(tokens.values());
    }

    public Optional<Token> findByTokenValue(String tokenValue) {
        if (tokenValue == null) return Optional.empty();
        return Optional.ofNullable(tokens.get(tokenValue));
    }

    public void save(Token token) {
        if (token != null && token.getTokenValue() != null) {
            tokens.put(token.getTokenValue(), token);
            persist();
        }
    }

    public boolean delete(String tokenValue) {
        if (tokenValue == null) return false;
        boolean deleted = tokens.remove(tokenValue) != null;
        if (deleted) {
            persist();
        }
        return deleted;
    }

    public void deleteByUsername(String username) {
        if (username != null) {
            boolean removed = tokens.values().removeIf(token -> username.equalsIgnoreCase(token.getUsername()));
            if (removed) {
                persist();
            }
        }
    }
}
