package com.qqchat.protocol;

public class ProtocolParser {

    public static ProtocolFrame parse(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return null;
        }
        try {
            return ProtocolFrame.parse(rawLine.strip());
        } catch (Exception e) {
            System.err.println("[ProtocolParser] Parse error: " + e.getMessage());
            return null;
        }
    }
}
