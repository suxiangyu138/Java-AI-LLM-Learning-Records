package com.qqchat.model;

import com.qqchat.util.TimestampUtil;

public class Message {
    private String messageId;
    private String fromId;
    private String toId;
    private String content;
    private long timestamp;
    private MessageType type;
    private MessageStatus status;

    public Message() {}

    public Message(String messageId, String fromId, String toId, String content,
                   long timestamp, MessageType type, MessageStatus status) {
        this.messageId = messageId;
        this.fromId = fromId;
        this.toId = toId;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
        this.status = status;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getFromId() { return fromId; }
    public void setFromId(String fromId) { this.fromId = fromId; }

    public String getToId() { return toId; }
    public void setToId(String toId) { this.toId = toId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public String formatForDisplay(String senderNickname) {
        String time = TimestampUtil.format(timestamp);
        if (type == MessageType.GROUP) {
            return "[" + time + "] " + senderNickname + ": " + content;
        }
        return "[" + time + "] " + senderNickname + ": " + content;
    }
}
