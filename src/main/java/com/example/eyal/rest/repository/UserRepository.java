package com.example.eyal.rest.repository;

import com.example.eyal.rest.model.User;
import com.example.eyal.rest.model.UserRole;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public UserRepository() {
        // Pre-seed the system with the default root user
        User root = new User("root", "Project!!!111", UserRole.ROOT);
        users.put(root.getUsername(), root);
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
        }
    }

    public boolean delete(String username) {
        // Root user cannot be deleted
        if ("root".equalsIgnoreCase(username)) {
            return false;
        }
        return users.remove(username) != null;
    }
}
