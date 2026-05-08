package com.qqchat.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String username;
    private String passwordHash;
    private String nickname;
    private List<String> friends;
    private List<String> groups;
    private long createdAt;
    private long lastSeen;
    private boolean online;

    public User() {
        this.friends = new ArrayList<>();
        this.groups = new ArrayList<>();
    }

    public User(String userId, String username, String passwordHash, String nickname) {
        this();
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.createdAt = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
        this.online = false;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public List<String> getFriends() { return friends; }
    public void setFriends(List<String> friends) { this.friends = friends; }

    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups) { this.groups = groups; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    // Returns a copy suitable for sending to clients (no password hash)
    public User safeCopy() {
        User copy = new User();
        copy.userId = this.userId;
        copy.username = this.username;
        copy.nickname = this.nickname;
        copy.friends = new ArrayList<>(this.friends);
        copy.groups = new ArrayList<>(this.groups);
        copy.createdAt = this.createdAt;
        copy.lastSeen = this.lastSeen;
        copy.online = this.online;
        return copy;
    }
}
