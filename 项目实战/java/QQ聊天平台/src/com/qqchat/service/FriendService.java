package com.qqchat.service;

import com.qqchat.model.FriendRelation;
import com.qqchat.model.FriendStatus;
import com.qqchat.model.User;
import com.qqchat.persistence.JsonFileStore;

import java.util.*;
import java.util.stream.Collectors;

public class FriendService {
    private final JsonFileStore fileStore;
    private final UserService userService;
    private static final String FRIENDS_FILE = "friend_relations.json";

    public FriendService(JsonFileStore fileStore, UserService userService) {
        this.fileStore = fileStore;
        this.userService = userService;
    }

    public synchronized void addFriend(String userId, String friendUsername) {
        User friendUser = userService.findById(friendUsername);
        if (friendUser == null) {
            throw new IllegalArgumentException("User not found: " + friendUsername);
        }
        if (userId.equals(friendUser.getUserId())) {
            throw new IllegalArgumentException("Cannot add yourself as friend");
        }

        List<FriendRelation> relations = loadRelations();
        // Check if already friends
        for (FriendRelation fr : relations) {
            if (fr.getUserId().equals(userId) && fr.getFriendId().equals(friendUser.getUserId())) {
                throw new IllegalArgumentException("Already friends with " + friendUsername);
            }
        }

        // Add bidirectional friend relations
        relations.add(new FriendRelation(userId, friendUser.getUserId()));
        relations.add(new FriendRelation(friendUser.getUserId(), userId));
        saveRelations(relations);

        // Update both users' friend lists
        userService.addFriendToUser(userId, friendUser.getUserId());
        userService.addFriendToUser(friendUser.getUserId(), userId);
    }

    public synchronized void deleteFriend(String userId, String friendId) {
        List<FriendRelation> relations = loadRelations();
        relations.removeIf(fr ->
                (fr.getUserId().equals(userId) && fr.getFriendId().equals(friendId))
                        || (fr.getUserId().equals(friendId) && fr.getFriendId().equals(userId)));
        saveRelations(relations);

        userService.removeFriendFromUser(userId, friendId);
        userService.removeFriendFromUser(friendId, userId);
    }

    public List<User> getFriendList(String userId) {
        User user = userService.findById(userId);
        if (user == null) return Collections.emptyList();
        return user.getFriends().stream()
                .map(userService::findById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<String> getFriendIds(String userId) {
        User user = userService.findById(userId);
        if (user == null) return Collections.emptyList();
        return new ArrayList<>(user.getFriends());
    }

    public List<String> getOnlineFriendIds(String userId) {
        return getFriendList(userId).stream()
                .filter(User::isOnline)
                .map(User::getUserId)
                .collect(Collectors.toList());
    }

    private List<FriendRelation> loadRelations() {
        List<FriendRelation> list = fileStore.loadList(FRIENDS_FILE, FriendRelation.class);
        return list != null ? list : new ArrayList<>();
    }

    private void saveRelations(List<FriendRelation> relations) {
        fileStore.saveList(FRIENDS_FILE, relations);
    }
}
