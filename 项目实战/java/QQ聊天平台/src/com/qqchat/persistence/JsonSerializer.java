package com.qqchat.persistence;

import com.qqchat.model.*;
import java.util.*;

public class JsonSerializer {

    // ——— Serialize ———

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Number) return obj.toString();
        if (obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map) return mapToJson((Map<?, ?>) obj);
        if (obj instanceof List) return listToJson((List<?>) obj);
        if (obj instanceof User) return userToJson((User) obj);
        if (obj instanceof Message) return messageToJson((Message) obj);
        if (obj instanceof Group) return groupToJson((Group) obj);
        if (obj instanceof FriendRelation) return friendRelationToJson((FriendRelation) obj);
        throw new IllegalArgumentException("Unsupported type: " + obj.getClass());
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(e.getKey().toString())).append("\":");
            sb.append(toJson(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String userToJson(User u) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"userId\":\"").append(escape(u.getUserId())).append("\",");
        sb.append("\"username\":\"").append(escape(u.getUsername())).append("\",");
        sb.append("\"passwordHash\":\"").append(escape(u.getPasswordHash())).append("\",");
        sb.append("\"nickname\":\"").append(escape(u.getNickname())).append("\",");
        sb.append("\"friends\":").append(toJson(u.getFriends())).append(",");
        sb.append("\"groups\":").append(toJson(u.getGroups())).append(",");
        sb.append("\"createdAt\":").append(u.getCreatedAt()).append(",");
        sb.append("\"lastSeen\":").append(u.getLastSeen()).append(",");
        sb.append("\"online\":").append(u.isOnline());
        sb.append("}");
        return sb.toString();
    }

    private static String messageToJson(Message m) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"messageId\":\"").append(escape(m.getMessageId())).append("\",");
        sb.append("\"fromId\":\"").append(escape(m.getFromId())).append("\",");
        sb.append("\"toId\":\"").append(escape(m.getToId())).append("\",");
        sb.append("\"content\":\"").append(escape(m.getContent())).append("\",");
        sb.append("\"timestamp\":").append(m.getTimestamp()).append(",");
        sb.append("\"type\":\"").append(m.getType().name()).append("\",");
        sb.append("\"status\":\"").append(m.getStatus().name()).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String groupToJson(Group g) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"groupId\":\"").append(escape(g.getGroupId())).append("\",");
        sb.append("\"name\":\"").append(escape(g.getName())).append("\",");
        sb.append("\"ownerId\":\"").append(escape(g.getOwnerId())).append("\",");
        sb.append("\"members\":").append(toJson(g.getMembers())).append(",");
        sb.append("\"createdAt\":").append(g.getCreatedAt());
        sb.append("}");
        return sb.toString();
    }

    private static String friendRelationToJson(FriendRelation fr) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"userId\":\"").append(escape(fr.getUserId())).append("\",");
        sb.append("\"friendId\":\"").append(escape(fr.getFriendId())).append("\",");
        sb.append("\"status\":\"").append(fr.getStatus().name()).append("\",");
        sb.append("\"since\":").append(fr.getSince());
        sb.append("}");
        return sb.toString();
    }

    // ——— Deserialize ———

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        JsonReader reader = new JsonReader(json.strip());
        Object value = reader.readValue();
        return convert(value, clazz);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, User> loadUsers(String json) {
        Map<String, Object> raw = fromJson(json, Map.class);
        Map<String, User> result = new LinkedHashMap<>();
        if (raw == null) return result;
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            User u = convert(e.getValue(), User.class);
            result.put(e.getKey(), u);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<FriendRelation> loadFriendRelations(String json) {
        List<Object> raw = fromJson(json, List.class);
        List<FriendRelation> result = new ArrayList<>();
        if (raw == null) return result;
        for (Object obj : raw) {
            result.add(convert(obj, FriendRelation.class));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Group> loadGroups(String json) {
        Map<String, Object> raw = fromJson(json, Map.class);
        Map<String, Group> result = new LinkedHashMap<>();
        if (raw == null) return result;
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Group g = convert(e.getValue(), Group.class);
            result.put(e.getKey(), g);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<Message> loadMessages(String json) {
        List<Object> raw = fromJson(json, List.class);
        List<Message> result = new ArrayList<>();
        if (raw == null) return result;
        for (Object obj : raw) {
            result.add(convert(obj, Message.class));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T convert(Object value, Class<T> clazz) {
        if (clazz == User.class) return (T) mapToUser((Map<String, Object>) value);
        if (clazz == Message.class) return (T) mapToMessage((Map<String, Object>) value);
        if (clazz == Group.class) return (T) mapToGroup((Map<String, Object>) value);
        if (clazz == FriendRelation.class) return (T) mapToFriendRelation((Map<String, Object>) value);
        if (value == null) return null;
        if (clazz.isInstance(value)) return clazz.cast(value);
        return clazz.cast(value);
    }

    private static User mapToUser(Map<String, Object> map) {
        User u = new User();
        u.setUserId(getStr(map, "userId"));
        u.setUsername(getStr(map, "username"));
        u.setPasswordHash(getStr(map, "passwordHash"));
        u.setNickname(getStr(map, "nickname"));
        u.setFriends(getStrList(map, "friends"));
        u.setGroups(getStrList(map, "groups"));
        u.setCreatedAt(getLong(map, "createdAt"));
        u.setLastSeen(getLong(map, "lastSeen"));
        u.setOnline(getBool(map, "online"));
        return u;
    }

    private static Message mapToMessage(Map<String, Object> map) {
        Message m = new Message();
        m.setMessageId(getStr(map, "messageId"));
        m.setFromId(getStr(map, "fromId"));
        m.setToId(getStr(map, "toId"));
        m.setContent(getStr(map, "content"));
        m.setTimestamp(getLong(map, "timestamp"));
        m.setType(MessageType.valueOf(getStr(map, "type")));
        m.setStatus(MessageStatus.valueOf(getStr(map, "status")));
        return m;
    }

    private static Group mapToGroup(Map<String, Object> map) {
        Group g = new Group();
        g.setGroupId(getStr(map, "groupId"));
        g.setName(getStr(map, "name"));
        g.setOwnerId(getStr(map, "ownerId"));
        g.setMembers(getStrList(map, "members"));
        g.setCreatedAt(getLong(map, "createdAt"));
        return g;
    }

    private static FriendRelation mapToFriendRelation(Map<String, Object> map) {
        FriendRelation fr = new FriendRelation();
        fr.setUserId(getStr(map, "userId"));
        fr.setFriendId(getStr(map, "friendId"));
        fr.setStatus(FriendStatus.valueOf(getStr(map, "status")));
        fr.setSince(getLong(map, "since"));
        return fr;
    }

    // ——— JSON Reader ———

    private static class JsonReader {
        private final String json;
        private int pos;

        JsonReader(String json) { this.json = json; this.pos = 0; }

        Object readValue() {
            skipWhitespace();
            if (pos >= json.length()) return null;
            char c = json.charAt(pos);
            if (c == '"') return readString();
            if (c == '{') return readObject();
            if (c == '[') return readArray();
            if (c == 't' || c == 'f') return readBoolean();
            if (c == 'n') { pos += 4; return null; }
            return readNumber();
        }

        Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (json.charAt(pos) == '}') { pos++; return map; }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = readValue();
                map.put(key, value);
                skipWhitespace();
                if (json.charAt(pos) == '}') { pos++; return map; }
                expect(',');
            }
        }

        List<Object> readArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (json.charAt(pos) == ']') { pos++; return list; }
            while (true) {
                skipWhitespace();
                list.add(readValue());
                skipWhitespace();
                if (json.charAt(pos) == ']') { pos++; return list; }
                expect(',');
            }
        }

        String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < json.length()) {
                char c = json.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char next = json.charAt(pos++);
                    switch (next) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        default: sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        Number readNumber() {
            int start = pos;
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            String numStr = json.substring(start, pos);
            if (numStr.contains(".")) return Double.parseDouble(numStr);
            return Long.parseLong(numStr);
        }

        boolean readBoolean() {
            if (json.startsWith("true", pos)) { pos += 4; return true; }
            if (json.startsWith("false", pos)) { pos += 5; return false; }
            throw new IllegalArgumentException("Expected boolean at " + pos);
        }

        void expect(char c) {
            if (pos >= json.length() || json.charAt(pos) != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at pos " + pos);
            }
            pos++;
        }

        void skipWhitespace() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        }
    }

    // ——— Helpers ———

    private static String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStrList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<Object>) v) {
                result.add(item != null ? item.toString() : "");
            }
            return result;
        }
        return new ArrayList<>();
    }

    private static long getLong(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) return Long.parseLong((String) v);
        return 0L;
    }

    private static boolean getBool(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return false;
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
