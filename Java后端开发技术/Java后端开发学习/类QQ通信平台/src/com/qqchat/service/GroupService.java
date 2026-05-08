package com.qqchat.service;

import com.qqchat.model.Group;
import com.qqchat.model.User;
import com.qqchat.persistence.JsonFileStore;
import com.qqchat.util.IdGenerator;

import java.util.*;
import java.util.stream.Collectors;

public class GroupService {
    private final JsonFileStore fileStore;
    private final UserService userService;
    private static final String GROUPS_FILE = "groups.json";

    public GroupService(JsonFileStore fileStore, UserService userService) {
        this.fileStore = fileStore;
        this.userService = userService;
    }

    public synchronized Group createGroup(String ownerId, String groupName) {
        String groupId = IdGenerator.generateGroupId();
        Group group = new Group(groupId, groupName, ownerId);

        Map<String, Group> groups = loadGroups();
        groups.put(groupId, group);
        saveGroups(groups);

        userService.addGroupToUser(ownerId, groupId);
        return group;
    }

    public synchronized void joinGroup(String userId, String groupId) {
        Map<String, Group> groups = loadGroups();
        Group group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        if (group.getMembers().contains(userId)) {
            throw new IllegalArgumentException("Already a member of this group");
        }
        group.getMembers().add(userId);
        saveGroups(groups);
        userService.addGroupToUser(userId, groupId);
    }

    public synchronized void leaveGroup(String userId, String groupId) {
        Map<String, Group> groups = loadGroups();
        Group group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        if (!group.getMembers().contains(userId)) {
            throw new IllegalArgumentException("Not a member of this group");
        }

        group.getMembers().remove(userId);

        // Handle owner transfer or group deletion
        if (group.getOwnerId().equals(userId)) {
            if (group.getMembers().isEmpty()) {
                groups.remove(groupId);
            } else {
                group.setOwnerId(group.getMembers().get(0));
            }
        }

        saveGroups(groups);
        userService.removeGroupFromUser(userId, groupId);
    }

    public List<Group> getGroupList(String userId) {
        User user = userService.findById(userId);
        if (user == null) return Collections.emptyList();
        Map<String, Group> groups = loadGroups();
        return user.getGroups().stream()
                .map(groups::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Group getGroupInfo(String groupId) {
        Map<String, Group> groups = loadGroups();
        return groups.get(groupId);
    }

    public List<User> getGroupMembers(String groupId) {
        Group group = getGroupInfo(groupId);
        if (group == null) return Collections.emptyList();
        return group.getMembers().stream()
                .map(userService::findById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<String> getGroupMemberIds(String groupId) {
        Group group = getGroupInfo(groupId);
        if (group == null) return Collections.emptyList();
        return new ArrayList<>(group.getMembers());
    }

    private Map<String, Group> loadGroups() {
        Map<String, Group> map = fileStore.loadMap(GROUPS_FILE, Group.class);
        return map != null ? map : new LinkedHashMap<>();
    }

    private void saveGroups(Map<String, Group> groups) {
        fileStore.saveMap(GROUPS_FILE, groups);
    }
}
