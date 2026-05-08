package com.qqchat.persistence;

import com.qqchat.model.Message;
import java.util.*;
import java.util.stream.Collectors;

public class MessageStore {
    private final JsonFileStore fileStore;
    private static final String MSG_DIR = "messages";

    public MessageStore(JsonFileStore fileStore) {
        this.fileStore = fileStore;
    }

    public synchronized void appendMessage(String scope, Message msg) {
        String filename = MSG_DIR + "/" + scope + ".json";
        List<Message> messages = fileStore.loadList(filename, Message.class);
        messages.add(msg);
        fileStore.saveList(filename, messages);
    }

    public synchronized List<Message> getHistory(String scope, int limit, long beforeTimestamp) {
        String filename = MSG_DIR + "/" + scope + ".json";
        List<Message> messages = fileStore.loadList(filename, Message.class);
        return messages.stream()
                .filter(m -> beforeTimestamp <= 0 || m.getTimestamp() < beforeTimestamp)
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit)
                .sorted((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                .collect(Collectors.toList());
    }

    public synchronized List<Message> getLatest(String scope, int count) {
        return getHistory(scope, count, 0);
    }

    private String getPrivateScope(String user1, String user2) {
        // Sort user IDs to ensure consistent filename regardless of order
        if (user1.compareTo(user2) <= 0) {
            return user1 + "_" + user2 + "_private";
        }
        return user2 + "_" + user1 + "_private";
    }

    public void savePrivateMessage(Message msg) {
        String scope = getPrivateScope(msg.getFromId(), msg.getToId());
        appendMessage(scope, msg);
    }

    public List<Message> getPrivateHistory(String user1, String user2, int limit, long before) {
        String scope = getPrivateScope(user1, user2);
        return getHistory(scope, limit, before);
    }

    public void saveGroupMessage(Message msg) {
        String scope = msg.getToId() + "_group";
        appendMessage(scope, msg);
    }

    public List<Message> getGroupHistory(String groupId, int limit, long before) {
        return getHistory(groupId + "_group", limit, before);
    }
}
