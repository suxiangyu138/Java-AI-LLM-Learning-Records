package com.qqchat.model;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String groupId;
    private String name;
    private String ownerId;
    private List<String> members;
    private long createdAt;

    public Group() {
        this.members = new ArrayList<>();
    }

    public Group(String groupId, String name, String ownerId) {
        this();
        this.groupId = groupId;
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = System.currentTimeMillis();
        this.members.add(ownerId);
    }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
