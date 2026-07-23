package com.example.eyal.rest.repository;

import com.example.eyal.rest.model.User;
import com.example.eyal.rest.model.UserRole;
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
public class UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String filePath;

    public UserRepository() {
        this("users.dat");
    }

    public UserRepository(String filePath) {
        this.filePath = filePath;
        load();
    }

    private synchronized void load() {
        if (filePath == null) {
            // Seed root user in memory
            User root = new User("root", "Project!!!111", UserRole.ROOT);
            users.put(root.getUsername(), root);
            return;
        }

        try {
            Path path = Path.of(filePath);
            if (Files.exists(path)) {
                String encrypted = Files.readString(path);
                if (!encrypted.isBlank()) {
                    String json = EncryptionUtils.decrypt(encrypted);
                    User[] loadedUsers = objectMapper.readValue(json, User[].class);
                    for (User user : loadedUsers) {
                        if (user != null && user.getUsername() != null) {
                            users.put(user.getUsername(), user);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading users database: " + e.getMessage());
        }

        // Always ensure root exists
        if (!users.containsKey("root")) {
            User root = new User("root", "Project!!!111", UserRole.ROOT);
            users.put(root.getUsername(), root);
            persist();
        }
    }

    private synchronized void persist() {
        if (filePath == null) return;

        try {
            List<User> userList = findAll();
            String json = objectMapper.writeValueAsString(userList);
            String encrypted = EncryptionUtils.encrypt(json);
            Files.writeString(Path.of(filePath), encrypted);
        } catch (Exception e) {
            System.err.println("Error persisting users database: " + e.getMessage());
        }
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(users.get(username));
    }

    public void save(User user) {
        if (user != null && user.getUsername() != null) {
            users.put(user.getUsername(), user);
            persist();
        }
    }

    public boolean delete(String username) {
        // Root user cannot be deleted
        if ("root".equalsIgnoreCase(username)) {
            return false;
        }
        boolean deleted = users.remove(username) != null;
        if (deleted) {
            persist();
        }
        return deleted;
    }
}
