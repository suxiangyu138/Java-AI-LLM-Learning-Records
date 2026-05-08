package com.qqchat.model;

public class FriendRelation {
    private String userId;
    private String friendId;
    private FriendStatus status;
    private long since;

    public FriendRelation() {}

    public FriendRelation(String userId, String friendId) {
        this.userId = userId;
        this.friendId = friendId;
        this.status = FriendStatus.ACTIVE;
        this.since = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFriendId() { return friendId; }
    public void setFriendId(String friendId) { this.friendId = friendId; }

    public FriendStatus getStatus() { return status; }
    public void setStatus(FriendStatus status) { this.status = status; }

    public long getSince() { return since; }
    public void setSince(long since) { this.since = since; }
}
