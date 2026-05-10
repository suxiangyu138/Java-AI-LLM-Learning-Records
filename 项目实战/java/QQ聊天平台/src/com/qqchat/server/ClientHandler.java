package com.qqchat.server;

import com.qqchat.model.*;
import com.qqchat.protocol.*;
import com.qqchat.service.*;
import com.qqchat.util.TimestampUtil;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final SessionRegistry sessionRegistry;
    private final UserService userService;
    private final FriendService friendService;
    private final GroupService groupService;
    private final ChatService chatService;
    private final HeartbeatMonitor heartbeatMonitor;

    private PrintWriter out;
    private BufferedReader in;
    private String userId;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, SessionRegistry sessionRegistry,
                         UserService userService, FriendService friendService,
                         GroupService groupService, ChatService chatService,
                         HeartbeatMonitor heartbeatMonitor) {
        this.socket = socket;
        this.sessionRegistry = sessionRegistry;
        this.userService = userService;
        this.friendService = friendService;
        this.groupService = groupService;
        this.chatService = chatService;
        this.heartbeatMonitor = heartbeatMonitor;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);

            String line;
            while (running && (line = in.readLine()) != null) {
                ProtocolFrame frame = ProtocolParser.parse(line);
                if (frame == null) continue;
                dispatch(frame);
            }
        } catch (IOException e) {
            // Client disconnected
        } finally {
            disconnect("Connection closed");
        }
    }

    private void dispatch(ProtocolFrame frame) {
        try {
            switch (frame.getCommand()) {
                case REGISTER:
                    handleRegister(frame);
                    break;
                case LOGIN:
                    handleLogin(frame);
                    break;
                case LOGOUT:
                    handleLogout(frame);
                    break;
                case PING:
                    handlePing(frame);
                    break;
                case ADD_FRIEND:
                    handleAddFriend(frame);
                    break;
                case DEL_FRIEND:
                    handleDeleteFriend(frame);
                    break;
                case FRIEND_LIST:
                    handleFriendList(frame);
                    break;
                case SEND_MSG:
                    handleSendMsg(frame);
                    break;
                case SEND_GROUP_MSG:
                    handleSendGroupMsg(frame);
                    break;
                case CREATE_GROUP:
                    handleCreateGroup(frame);
                    break;
                case JOIN_GROUP:
                    handleJoinGroup(frame);
                    break;
                case LEAVE_GROUP:
                    handleLeaveGroup(frame);
                    break;
                case GROUP_LIST:
                    handleGroupList(frame);
                    break;
                case GROUP_INFO:
                    handleGroupInfo(frame);
                    break;
                case GET_PROFILE:
                    handleGetProfile(frame);
                    break;
                case UPDATE_PROFILE:
                    handleUpdateProfile(frame);
                    break;
                case SEARCH_USER:
                    handleSearchUser(frame);
                    break;
                default:
                    sendError("Unknown command: " + frame.getCommand());
            }
        } catch (Exception e) {
            sendError(e.getMessage());
        }
    }

    // ——— Auth ———

    private void handleRegister(ProtocolFrame frame) {
        String[] parts = frame.getPayload().split("\\|", 3);
        if (parts.length < 3) {
            sendError("Usage: REGISTER|username|password|nickname");
            return;
        }
        User user = userService.register(parts[0], parts[1], parts[2]);
        ProtocolFrame resp = ProtocolFrame.okResponse(Command.AUTH_OK, "SERVER", parts[0],
                "Register successful. Welcome " + user.getNickname() + "!");
        sendFrame(resp);
    }

    private void handleLogin(ProtocolFrame frame) {
        String[] parts = frame.getPayload().split("\\|", 2);
        if (parts.length < 2) {
            sendError("Usage: LOGIN|username|password");
            return;
        }
        User user = userService.login(parts[0], parts[1]);
        this.userId = user.getUserId();

        // Check for duplicate login
        if (sessionRegistry.isOnline(userId)) {
            ClientHandler old = sessionRegistry.getHandler(userId);
            if (old != null) old.disconnect("Logged in from another device");
        }

        sessionRegistry.register(userId, this);
        heartbeatMonitor.recordPing(userId);

        sendFrame(ProtocolFrame.okResponse(Command.AUTH_OK, "SERVER", userId,
                "Login successful. Welcome " + user.getNickname() + "!"));

        // Notify friends of online status
        notifyFriendsStatus(true);

        // Deliver offline messages
        List<Message> offlineMsgs = chatService.dequeueAllOffline(userId);
        if (!offlineMsgs.isEmpty()) {
            for (Message msg : offlineMsgs) {
                ProtocolFrame msgFrame = new ProtocolFrame(Command.OFFLINE_MSGS, msg.getFromId(), userId,
                        msg.getContent());
                sendFrame(msgFrame);
            }
        }

        System.out.println("[Server] " + user.getNickname() + " (" + userId + ") logged in. Online: " + sessionRegistry.getOnlineCount());
    }

    private void handleLogout(ProtocolFrame frame) {
        disconnect("User logged out");
    }

    private void handlePing(ProtocolFrame frame) {
        heartbeatMonitor.recordPing(userId);
        sendFrame(new ProtocolFrame(Command.PONG, "SERVER", userId, "ok"));
    }

    // ——— Friends ———

    private void handleAddFriend(ProtocolFrame frame) {
        ensureAuth();
        friendService.addFriend(userId, frame.getPayload());
        sendFrame(ProtocolFrame.okResponse(Command.AUTH_OK, "SERVER", userId, "Friend added: " + frame.getPayload()));

        // Notify the other user if online
        ClientHandler targetHandler = sessionRegistry.getHandler(frame.getPayload());
        if (targetHandler != null) {
            User me = userService.findById(userId);
            ProtocolFrame notify = new ProtocolFrame(Command.FRIEND_REQUEST, userId, frame.getPayload(),
                    me != null ? me.getNickname() : userId);
            targetHandler.sendFrame(notify);
        }
    }

    private void handleDeleteFriend(ProtocolFrame frame) {
        ensureAuth();
        friendService.deleteFriend(userId, frame.getPayload());
        sendFrame(ProtocolFrame.okResponse(Command.AUTH_OK, "SERVER", userId, "Friend removed: " + frame.getPayload()));
    }

    private void handleFriendList(ProtocolFrame frame) {
        ensureAuth();
        List<User> friends = friendService.getFriendList(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Friends (").append(friends.size()).append(") ===\n");
        for (User f : friends) {
            String status = f.isOnline() ? "[ONLINE]" : "[OFFLINE]";
            sb.append("  ").append(status).append(" ").append(f.getNickname())
                    .append(" (@" ).append(f.getUsername()).append(")\n");
        }
        sendFrame(ProtocolFrame.okResponse(Command.FRIEND_LIST_RESP, "SERVER", userId, sb.toString()));
    }

    // ——— Messages ———

    private void handleSendMsg(ProtocolFrame frame) {
        ensureAuth();
        String targetId = frame.getTargetId();
        String content = frame.getPayload();

        Message msg = chatService.sendPrivateMessage(userId, targetId, content);

        ClientHandler targetHandler = sessionRegistry.getHandler(targetId);
        if (targetHandler != null) {
            // Deliver immediately
            ProtocolFrame msgFrame = new ProtocolFrame(Command.RECV_MSG, userId, targetId, content);
            targetHandler.sendFrame(msgFrame);

            ProtocolFrame delivered = new ProtocolFrame(Command.MSG_DELIVERED, "SERVER", userId, msg.getMessageId());
            sendFrame(delivered);
        } else {
            // Store offline
            chatService.enqueueOffline(targetId, msg);
            sendFrame(ProtocolFrame.okResponse(Command.AUTH_OK, "SERVER", userId, "Message queued (user offline)"));
        }
    }

    private void handleSendGroupMsg(ProtocolFrame frame) {
        ensureAuth();
        String groupId = frame.getTargetId();
        String content = frame.getPayload();

        chatService.sendGroupMessage(userId, groupId, content);
        Group group = groupService.getGroupInfo(groupId);
        if (group == null) {
            sendError("Group not found: " + groupId);
            return;
        }

        User sender = userService.findById(userId);
        String senderName = sender != null ? sender.getNickname() : userId;

        // Broadcast to all online group members except sender
        for (String memberId : group.getMembers()) {
            if (memberId.equals(userId)) continue;
            ClientHandler handler = sessionRegistry.getHandler(memberId);
            if (handler != null) {
                ProtocolFrame msgFrame = new ProtocolFrame(Command.RECV_GROUP_MSG, userId, groupId, content);
                // Attach group name as part of payload for display
                msgFrame.setPayload(group.getName() + "|" + senderName + "|" + content);
                handler.sendFrame(msgFrame);
            } else {
                // Store offline
                Message msg = new Message();
                msg.setFromId(userId);
                msg.setToId(groupId);
                msg.setContent("[Group:" + group.getName() + "] " + senderName + ": " + content);
                msg.setTimestamp(TimestampUtil.now());
                msg.setType(MessageType.GROUP);
                msg.setStatus(MessageStatus.PENDING);
                chatService.enqueueOffline(memberId, msg);
            }
        }
    }

    // ——— Groups ———

    private void handleCreateGroup(ProtocolFrame frame) {
        ensureAuth();
        Group group = groupService.createGroup(userId, frame.getPayload());
        sendFrame(ProtocolFrame.okResponse(Command.AUTH_OK, "SERVER", userId,
                "Group created: " + group.getName() + " (ID: " + group.getGroupId() + ")"));
    }

    private void handleJoinGroup(ProtocolFrame frame) {
        ensureAuth();
        groupService.joinGroup(userId, frame.getPayload());
        Group group = groupService.getGroupInfo(frame.getPayload());
        String groupName = group != null ? group.getName() : frame.getPayload();
        sendFrame(ProtocolFrame.okResponse(Command.AUTH_OK, "SERVER", userId, "Joined group: " + groupName));
    }

    private void handleLeaveGroup(ProtocolFrame frame) {
        ensureAuth();
        groupService.leaveGroup(userId, frame.getPayload());
        sendFrame(ProtocolFrame.okResponse(Command.AUTH_OK, "SERVER", userId, "Left group"));
    }

    private void handleGroupList(ProtocolFrame frame) {
        ensureAuth();
        List<Group> groups = groupService.getGroupList(userId);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Groups (").append(groups.size()).append(") ===\n");
        for (Group g : groups) {
            sb.append("  [").append(g.getGroupId()).append("] ").append(g.getName())
                    .append(" (").append(g.getMembers().size()).append(" members)\n");
        }
        sendFrame(ProtocolFrame.okResponse(Command.GROUP_LIST_RESP, "SERVER", userId, sb.toString()));
    }

    private void handleGroupInfo(ProtocolFrame frame) {
        ensureAuth();
        Group group = groupService.getGroupInfo(frame.getPayload());
        if (group == null) {
            sendError("Group not found: " + frame.getPayload());
            return;
        }
        List<User> members = groupService.getGroupMembers(frame.getPayload());
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(group.getName()).append(" ===\n");
        sb.append("ID: ").append(group.getGroupId()).append("\n");
        sb.append("Owner: ").append(group.getOwnerId()).append("\n");
        sb.append("Members (").append(members.size()).append("):\n");
        for (User m : members) {
            String status = m.isOnline() ? "[ONLINE]" : "[OFFLINE]";
            sb.append("  ").append(status).append(" ").append(m.getNickname())
                    .append(" (@").append(m.getUsername()).append(")\n");
        }
        sendFrame(ProtocolFrame.okResponse(Command.GROUP_INFO_RESP, "SERVER", userId, sb.toString()));
    }

    // ——— Profile ———

    private void handleGetProfile(ProtocolFrame frame) {
        ensureAuth();
        User user = userService.findById(userId);
        if (user == null) {
            sendError("Profile not found");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=== Profile ===\n");
        sb.append("Username: @").append(user.getUsername()).append("\n");
        sb.append("Nickname: ").append(user.getNickname()).append("\n");
        sb.append("User ID: ").append(user.getUserId()).append("\n");
        sb.append("Friends: ").append(user.getFriends().size()).append("\n");
        sb.append("Groups: ").append(user.getGroups().size()).append("\n");
        sb.append("Joined: ").append(TimestampUtil.format(user.getCreatedAt())).append("\n");
        sb.append("Last seen: ").append(TimestampUtil.format(user.getLastSeen())).append("\n");

        sendFrame(ProtocolFrame.okResponse(Command.PROFILE_RESP, "SERVER", userId, sb.toString()));
    }

    private void handleUpdateProfile(ProtocolFrame frame) {
        ensureAuth();
        String[] parts = frame.getPayload().split("\\|", 2);
        if (parts.length < 2) {
            sendError("Usage: UPDATE_PROFILE|field|value");
            return;
        }
        userService.updateProfile(userId, parts[0], parts[1]);
        sendFrame(ProtocolFrame.okResponse(Command.AUTH_OK, "SERVER", userId, "Profile updated"));
    }

    private void handleSearchUser(ProtocolFrame frame) {
        ensureAuth();
        List<User> results = userService.searchUsers(frame.getPayload());
        StringBuilder sb = new StringBuilder();
        sb.append("=== Search: '").append(frame.getPayload()).append("' (").append(results.size()).append(" results) ===\n");
        for (User u : results) {
            if (u.getUserId().equals(userId)) continue; // Skip self
            String status = u.isOnline() ? "[ONLINE]" : "[OFFLINE]";
            sb.append("  ").append(status).append(" ").append(u.getNickname())
                    .append(" (@").append(u.getUsername()).append(")\n");
        }
        sendFrame(ProtocolFrame.okResponse(Command.SEARCH_RESP, "SERVER", userId, sb.toString()));
    }

    // ——— Helpers ———

    private void ensureAuth() {
        if (userId == null) {
            throw new IllegalStateException("Not logged in");
        }
    }

    public void sendFrame(ProtocolFrame frame) {
        if (out != null) {
            out.println(frame.serialize());
        }
    }

    private void sendError(String message) {
        sendFrame(ProtocolFrame.error(userId != null ? userId : "unknown", message));
    }

    public void disconnect(String reason) {
        if (!running) return;
        running = false;
        try {
            sendFrame(new ProtocolFrame(Command.SERVER_SHUTDOWN, "SERVER",
                    userId != null ? userId : "", reason));
        } catch (Exception ignored) {}
        try {
            if (userId != null) {
                notifyFriendsStatus(false);
                userService.setOnline(userId, false);
                System.out.println("[Server] " + userId + " disconnected. Reason: " + reason + ". Online: " + (sessionRegistry.getOnlineCount() - 1));
                sessionRegistry.unregister(userId);
            }
        } catch (Exception ignored) {}
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void notifyFriendsStatus(boolean online) {
        Command cmd = online ? Command.FRIEND_ONLINE : Command.FRIEND_OFFLINE;
        List<String> friendIds = friendService.getFriendIds(userId);
        for (String friendId : friendIds) {
            ClientHandler handler = sessionRegistry.getHandler(friendId);
            if (handler != null && !handler.userId.equals(userId)) {
                handler.sendFrame(new ProtocolFrame(cmd, userId, friendId, ""));
            }
        }
    }

    public String getUserId() { return userId; }
}
