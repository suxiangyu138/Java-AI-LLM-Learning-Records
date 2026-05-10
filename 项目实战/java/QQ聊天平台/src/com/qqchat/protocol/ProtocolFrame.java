package com.qqchat.protocol;

import com.qqchat.util.TimestampUtil;
import java.util.Base64;

public class ProtocolFrame {
    private Command command;
    private String senderId;
    private String targetId;
    private long timestamp;
    private String payload;

    public ProtocolFrame() {}

    public ProtocolFrame(Command command, String senderId, String targetId, String payload) {
        this.command = command;
        this.senderId = senderId != null ? senderId : "";
        this.targetId = targetId != null ? targetId : "";
        this.timestamp = TimestampUtil.now();
        this.payload = payload != null ? payload : "";
    }

    public Command getCommand() { return command; }
    public void setCommand(Command command) { this.command = command; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String serialize() {
        String encodedPayload = Base64.getEncoder().encodeToString(
                payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return command.name() + "|" + safe(senderId) + "|" + safe(targetId) + "|"
                + timestamp + "|" + encodedPayload;
    }

    public static ProtocolFrame parse(String line) {
        ProtocolFrame frame = new ProtocolFrame();
        String[] parts = line.split("\\|", 5);
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid frame format: " + line);
        }
        frame.command = Command.valueOf(parts[0]);
        frame.senderId = parts[1];
        frame.targetId = parts[2];
        frame.timestamp = Long.parseLong(parts[3]);
        byte[] decoded = Base64.getDecoder().decode(parts[4]);
        frame.payload = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        return frame;
    }

    public static ProtocolFrame okResponse(Command cmd, String sender, String target, String payload) {
        return new ProtocolFrame(cmd, sender, target, payload);
    }

    public static ProtocolFrame error(String target, String errorMsg) {
        return new ProtocolFrame(Command.ERROR, "SERVER", target, errorMsg);
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}
