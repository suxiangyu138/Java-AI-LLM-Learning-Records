package com.qqchat.service;

import com.qqchat.model.User;
import com.qqchat.persistence.JsonFileStore;
import com.qqchat.util.PasswordHasher;

import java.util.*;
import java.util.stream.Collectors;

public class UserService {
    private final JsonFileStore fileStore;
    private static final String USERS_FILE = "users.json";

    public UserService(JsonFileStore fileStore) {
        this.fileStore = fileStore;
    }

    public synchronized User register(String username, String password, String nickname) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);

        // Check if username already exists
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        User user = new User(username, username, PasswordHasher.hash(password), nickname);
        users.put(username, user);
        fileStore.saveMap(USERS_FILE, users);
        return user;
    }

    public synchronized User login(String username, String password) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }
        user.setOnline(true);
        user.setLastSeen(System.currentTimeMillis());
        users.put(username, user);
        fileStore.saveMap(USERS_FILE, users);
        return user;
    }

    public synchronized void logout(String userId) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(userId);
        if (user != null) {
            user.setOnline(false);
            user.setLastSeen(System.currentTimeMillis());
            users.put(userId, user);
            fileStore.saveMap(USERS_FILE, users);
        }
    }

    public User getProfile(String userId) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(userId);
        return user != null ? user.safeCopy() : null;
    }

    public synchronized User updateProfile(String userId, String field, String value) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(userId);
        if (user == null) return null;

        switch (field.toLowerCase()) {
            case "nickname":
                user.setNickname(value);
                break;
            default:
                throw new IllegalArgumentException("Unknown field: " + field);
        }

        users.put(userId, user);
        fileStore.saveMap(USERS_FILE, users);
        return user.safeCopy();
    }

    public List<User> searchUsers(String query) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        String lower = query.toLowerCase();
        return users.values().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(lower)
                        || u.getNickname().toLowerCase().contains(lower))
                .map(User::safeCopy)
                .collect(Collectors.toList());
    }

    public User findById(String userId) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(userId);
        return user != null ? user.safeCopy() : null;
    }

    public synchronized void addFriendToUser(String userId, String friendId) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(userId);
        if (user != null && !user.getFriends().contains(friendId)) {
            user.getFriends().add(friendId);
            users.put(userId, user);
            fileStore.saveMap(USERS_FILE, users);
        }
    }

    public synchronized void removeFriendFromUser(String userId, String friendId) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(userId);
        if (user != null) {
            user.getFriends().remove(friendId);
            users.put(userId, user);
            fileStore.saveMap(USERS_FILE, users);
        }
    }

    public synchronized void addGroupToUser(String userId, String groupId) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(userId);
        if (user != null && !user.getGroups().contains(groupId)) {
            user.getGroups().add(groupId);
            users.put(userId, user);
            fileStore.saveMap(USERS_FILE, users);
        }
    }

    public synchronized void removeGroupFromUser(String userId, String groupId) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(userId);
        if (user != null) {
            user.getGroups().remove(groupId);
            users.put(userId, user);
            fileStore.saveMap(USERS_FILE, users);
        }
    }

    public synchronized void setOnline(String userId, boolean online) {
        Map<String, User> users = fileStore.loadMap(USERS_FILE, User.class);
        User user = users.get(userId);
        if (user != null) {
            user.setOnline(online);
            user.setLastSeen(System.currentTimeMillis());
            users.put(userId, user);
            fileStore.saveMap(USERS_FILE, users);
        }
    }
}
