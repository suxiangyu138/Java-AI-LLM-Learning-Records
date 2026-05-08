package com.qqchat.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {
    private final ConcurrentHashMap<String, ClientHandler> sessions = new ConcurrentHashMap<>();

    public void register(String userId, ClientHandler handler) {
        sessions.put(userId, handler);
    }

    public void unregister(String userId) {
        sessions.remove(userId);
    }

    public boolean isOnline(String userId) {
        return sessions.containsKey(userId);
    }

    public ClientHandler getHandler(String userId) {
        return sessions.get(userId);
    }

    public Set<String> getOnlineUsers() {
        return sessions.keySet();
    }

    public int getOnlineCount() {
        return sessions.size();
    }
}
