package com.qqchat.client;

public class ChatSession {
    private String userId;
    private String nickname;
    private String currentChatTarget;  // userId or groupId for chat mode
    private boolean inChatMode;        // true = in continuous chat, false = global command mode
    private boolean inGroupChat;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getCurrentChatTarget() { return currentChatTarget; }
    public boolean isInChatMode() { return inChatMode; }
    public boolean isInGroupChat() { return inGroupChat; }

    public void enterChatMode(String target, boolean groupChat) {
        this.currentChatTarget = target;
        this.inChatMode = true;
        this.inGroupChat = groupChat;
    }

    public void exitChatMode() {
        this.currentChatTarget = null;
        this.inChatMode = false;
        this.inGroupChat = false;
    }

    public void reset() {
        this.userId = null;
        this.nickname = null;
        exitChatMode();
    }
}
