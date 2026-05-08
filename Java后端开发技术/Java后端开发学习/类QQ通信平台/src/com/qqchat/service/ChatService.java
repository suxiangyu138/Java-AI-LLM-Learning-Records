package com.qqchat.service;

import com.qqchat.model.Message;
import com.qqchat.model.MessageStatus;
import com.qqchat.model.MessageType;
import com.qqchat.persistence.MessageStore;
import com.qqchat.util.IdGenerator;
import com.qqchat.util.TimestampUtil;

import java.util.*;

public class ChatService {
    private final MessageStore messageStore;

    // In-memory offline message queue: userId -> list of messages
    private final Map<String, List<Message>> offlineQueue = new LinkedHashMap<>();

    public ChatService(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    public Message sendPrivateMessage(String fromId, String toId, String content) {
        String messageId = IdGenerator.generateMessageId();
        Message msg = new Message(messageId, fromId, toId, content,
                TimestampUtil.now(), MessageType.PRIVATE, MessageStatus.SENT);
        messageStore.savePrivateMessage(msg);
        return msg;
    }

    public Message sendGroupMessage(String fromId, String groupId, String content) {
        String messageId = IdGenerator.generateMessageId();
        Message msg = new Message(messageId, fromId, groupId, content,
                TimestampUtil.now(), MessageType.GROUP, MessageStatus.SENT);
        messageStore.saveGroupMessage(msg);
        return msg;
    }

    public List<Message> getPrivateHistory(String user1, String user2, int limit, long before) {
        return messageStore.getPrivateHistory(user1, user2, limit, before);
    }

    public List<Message> getGroupHistory(String groupId, int limit, long before) {
        return messageStore.getGroupHistory(groupId, limit, before);
    }

    // ——— Offline Message Queue ———

    public synchronized void enqueueOffline(String userId, Message msg) {
        offlineQueue.computeIfAbsent(userId, k -> new ArrayList<>()).add(msg);
    }

    public synchronized List<Message> dequeueAllOffline(String userId) {
        List<Message> messages = offlineQueue.remove(userId);
        return messages != null ? messages : Collections.emptyList();
    }

    public synchronized boolean hasOfflineMessages(String userId) {
        List<Message> msgs = offlineQueue.get(userId);
        return msgs != null && !msgs.isEmpty();
    }
}
