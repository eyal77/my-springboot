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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class TokenRepository {
    private static final Logger log = LoggerFactory.getLogger(TokenRepository.class);
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
        log.debug("Initializing TokenRepository with filePath: {}", filePath);
        load();
    }

    private synchronized void load() {
        log.debug("load called: loading tokens database...");
        if (filePath == null) {
            log.debug("load: filePath is null (in-memory mode), skipping filesystem load.");
            return;
        }

        try {
            Path path = Path.of(filePath);
            if (Files.exists(path)) {
                log.debug("load: Reading encrypted database file from: {}", path.toAbsolutePath());
                String encrypted = Files.readString(path);
                if (!encrypted.isBlank()) {
                    String json = EncryptionUtils.decrypt(encrypted);
                    Token[] loadedTokens = objectMapper.readValue(json, Token[].class);
                    for (Token token : loadedTokens) {
                        if (token != null && token.getTokenValue() != null) {
                            tokens.put(token.getTokenValue(), token);
                        }
                    }
                    log.debug("load: Loaded {} tokens from database file.", tokens.size());
                }
            } else {
                log.debug("load: Tokens database file does not exist yet.");
            }
        } catch (Exception e) {
            log.error("load: Error loading tokens database: {}", e.getMessage(), e);
        }
    }

    private synchronized void persist() {
        log.debug("persist called: saving tokens database to disk...");
        if (filePath == null) {
            log.debug("persist: In-memory mode, skipping filesystem persistence.");
            return;
        }

        try {
            List<Token> tokenList = findAll();
            String json = objectMapper.writeValueAsString(tokenList);
            String encrypted = EncryptionUtils.encrypt(json);
            Files.writeString(Path.of(filePath), encrypted);
            log.debug("persist: Successfully wrote encrypted tokens database file.");
        } catch (Exception e) {
            log.error("persist: Error persisting tokens database: {}", e.getMessage(), e);
        }
    }

    public List<Token> findAll() {
        log.debug("findAll called: fetching all tokens. Current count: {}", tokens.size());
        return new ArrayList<>(tokens.values());
    }

    public Optional<Token> findByTokenValue(String tokenValue) {
        log.debug("findByTokenValue called for: {}", tokenValue != null && tokenValue.length() > 5 ? tokenValue.substring(0, 5) + "..." : tokenValue);
        if (tokenValue == null) return Optional.empty();
        return Optional.ofNullable(tokens.get(tokenValue));
    }

    public void save(Token token) {
        if (token != null && token.getTokenValue() != null) {
            log.debug("save called for token: user={}, role={}", token.getUsername(), token.getRole());
            tokens.put(token.getTokenValue(), token);
            persist();
        } else {
            log.warn("save called with invalid or null token entity.");
        }
    }

    public boolean delete(String tokenValue) {
        log.debug("delete called for token: {}", tokenValue != null && tokenValue.length() > 5 ? tokenValue.substring(0, 5) + "..." : tokenValue);
        if (tokenValue == null) return false;
        boolean deleted = tokens.remove(tokenValue) != null;
        log.debug("delete results: tokenValue deleted={}", deleted);
        if (deleted) {
            persist();
        }
        return deleted;
    }

    public void deleteByUsername(String username) {
        log.debug("deleteByUsername called for username: {}", username);
        if (username != null) {
            boolean removed = tokens.values().removeIf(token -> username.equalsIgnoreCase(token.getUsername()));
            log.debug("deleteByUsername completed. Tokens removed for user {}: {}", username, removed);
            if (removed) {
                persist();
            }
        }
    }
}
