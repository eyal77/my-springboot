package com.example.eyal.rest.repository;

import com.example.eyal.rest.model.Token;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TokenRepository {
    private final Map<String, Token> tokens = new ConcurrentHashMap<>();

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
        }
    }

    public boolean delete(String tokenValue) {
        if (tokenValue == null) return false;
        return tokens.remove(tokenValue) != null;
    }

    public void deleteByUsername(String username) {
        if (username != null) {
            tokens.values().removeIf(token -> username.equalsIgnoreCase(token.getUsername()));
        }
    }
}
