package com.qqchat.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class JsonFileStore {
    private final Path basePath;

    public JsonFileStore(String basePath) {
        this.basePath = Paths.get(basePath);
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data directory: " + basePath, e);
        }
    }

    public synchronized String readFile(String filename) {
        Path file = basePath.resolve(filename);
        if (!Files.exists(file)) return null;
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[JsonFileStore] Read error: " + e.getMessage());
            return null;
        }
    }

    public synchronized void writeFile(String filename, String content) {
        Path file = basePath.resolve(filename);
        Path tmp = basePath.resolve(filename + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("[JsonFileStore] Write error: " + e.getMessage());
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    public synchronized boolean fileExists(String filename) {
        return Files.exists(basePath.resolve(filename));
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> loadMap(String filename, Class<T> clazz) {
        String json = readFile(filename);
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        if (clazz == com.qqchat.model.User.class) {
            return (Map<String, T>) JsonSerializer.loadUsers(json);
        }
        if (clazz == com.qqchat.model.Group.class) {
            return (Map<String, T>) JsonSerializer.loadGroups(json);
        }
        return new LinkedHashMap<>();
    }

    public <T> void saveMap(String filename, Map<String, T> data) {
        writeFile(filename, JsonSerializer.toJson(data));
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> loadList(String filename, Class<T> clazz) {
        String json = readFile(filename);
        if (json == null || json.isBlank()) return new ArrayList<>();
        if (clazz == com.qqchat.model.FriendRelation.class) {
            return (List<T>) JsonSerializer.loadFriendRelations(json);
        }
        if (clazz == com.qqchat.model.Message.class) {
            return (List<T>) JsonSerializer.loadMessages(json);
        }
        return new ArrayList<>();
    }

    public <T> void saveList(String filename, List<T> data) {
        writeFile(filename, JsonSerializer.toJson(data));
    }

    public Path getBasePath() {
        return basePath;
    }
}
