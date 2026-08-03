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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class UserRepository {
    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);
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
        log.debug("Initializing UserRepository with filePath: {}", filePath);
        load();
    }

    private synchronized void load() {
        log.debug("load called: loading users database...");
        if (filePath == null) {
            log.debug("load: filePath is null (in-memory mode). Seeding default root user.");
            String rootPassword = System.getenv("root");
            if (rootPassword == null) {
                rootPassword = "";
            }
            User root = new User("root", rootPassword, UserRole.ROOT);
            users.put(root.getUsername(), root);
            return;
        }

        try {
            Path path = Path.of(filePath);
            if (Files.exists(path)) {
                log.debug("load: Reading encrypted database file from: {}", path.toAbsolutePath());
                String encrypted = Files.readString(path);
                if (!encrypted.isBlank()) {
                    String json = EncryptionUtils.decrypt(encrypted);
                    User[] loadedUsers = objectMapper.readValue(json, User[].class);
                    for (User user : loadedUsers) {
                        if (user != null && user.getUsername() != null) {
                            users.put(user.getUsername(), user);
                        }
                    }
                    log.debug("load: Loaded {} users from database file.", users.size());
                }
            } else {
                log.debug("load: Users database file does not exist yet.");
            }
        } catch (Exception e) {
            log.error("load: Error loading users database: {}", e.getMessage(), e);
        }

        // Always ensure root exists and is updated with the current environment variable password
        String rootPassword = System.getenv("root");
        if (rootPassword == null) {
            rootPassword = "";
        }
        User root = users.get("root");
        if (root == null) {
            log.info("load: root user not found. Seeding new root user.");
            root = new User("root", rootPassword, UserRole.ROOT);
            users.put(root.getUsername(), root);
            persist();
        } else if (!rootPassword.equals(root.getPassword()) || root.getRole() != UserRole.ROOT) {
            log.info("load: Updating existing root user password/role from environment variables.");
            root.setPassword(rootPassword);
            root.setRole(UserRole.ROOT);
            persist();
        }
    }

    private synchronized void persist() {
        log.debug("persist called: saving users database to disk...");
        if (filePath == null) {
            log.debug("persist: In-memory mode, skipping filesystem persistence.");
            return;
        }

        try {
            List<User> userList = findAll();
            String json = objectMapper.writeValueAsString(userList);
            String encrypted = EncryptionUtils.encrypt(json);
            Files.writeString(Path.of(filePath), encrypted);
            log.debug("persist: Successfully wrote encrypted users database file.");
        } catch (Exception e) {
            log.error("persist: Error persisting users database: {}", e.getMessage(), e);
        }
    }

    public List<User> findAll() {
        log.debug("findAll called: fetching all users. Current count: {}", users.size());
        return new ArrayList<>(users.values());
    }

    public Optional<User> findByUsername(String username) {
        log.debug("findByUsername called for: {}", username);
        if (username == null) return Optional.empty();
        return Optional.ofNullable(users.get(username));
    }

    public void save(User user) {
        if (user != null && user.getUsername() != null) {
            log.debug("save called for user: {}, role: {}", user.getUsername(), user.getRole());
            users.put(user.getUsername(), user);
            persist();
        } else {
            log.warn("save called with invalid or null user entity.");
        }
    }

    public boolean delete(String username) {
        log.debug("delete called for user: {}", username);
        // Root user cannot be deleted
        if ("root".equalsIgnoreCase(username)) {
            log.warn("delete: Rejecting deletion request for root user.");
            return false;
        }
        boolean deleted = users.remove(username) != null;
        log.debug("delete results: user={} deleted={}", username, deleted);
        if (deleted) {
            persist();
        }
        return deleted;
    }
}
