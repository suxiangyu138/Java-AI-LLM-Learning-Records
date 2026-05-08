package com.qqchat.server;

import com.qqchat.persistence.JsonFileStore;
import com.qqchat.persistence.MessageStore;
import com.qqchat.service.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class QQServer {
    private final int port;
    private final JsonFileStore fileStore;
    private final UserService userService;
    private final FriendService friendService;
    private final GroupService groupService;
    private final ChatService chatService;
    private final SessionRegistry sessionRegistry;
    private final HeartbeatMonitor heartbeatMonitor;
    private final ExecutorService threadPool;

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public QQServer(int port, String dataDir) {
        this.port = port;
        this.fileStore = new JsonFileStore(dataDir);
        MessageStore messageStore = new MessageStore(fileStore);

        this.userService = new UserService(fileStore);
        this.friendService = new FriendService(fileStore, userService);
        this.groupService = new GroupService(fileStore, userService);
        this.chatService = new ChatService(messageStore);
        this.sessionRegistry = new SessionRegistry();
        this.heartbeatMonitor = new HeartbeatMonitor(sessionRegistry);
        this.threadPool = Executors.newFixedThreadPool(50);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("============================================");
            System.out.println("  QQ Chat Server started on port " + port);
            System.out.println("  Data directory: " + fileStore.getBasePath().toAbsolutePath());
            System.out.println("  Max threads: 50");
            System.out.println("============================================");

            heartbeatMonitor.start();

            // Shutdown hook for graceful cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(clientSocket, sessionRegistry,
                            userService, friendService, groupService, chatService, heartbeatMonitor);
                    threadPool.submit(handler);
                    System.out.println("[Server] New connection from " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[Server] Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Server] Failed to start: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        System.out.println("[Server] Shutting down...");
        heartbeatMonitor.shutdown();
        threadPool.shutdown();

        // Disconnect all clients
        for (String userId : sessionRegistry.getOnlineUsers()) {
            ClientHandler handler = sessionRegistry.getHandler(userId);
            if (handler != null) {
                handler.disconnect("Server shutting down");
            }
        }

        try { serverSocket.close(); } catch (IOException ignored) {}
        try {
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        System.out.println("[Server] Shutdown complete.");
    }

    public static void main(String[] args) {
        int port = 9999;
        String dataDir = "data";

        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            dataDir = args[1];
        }

        new QQServer(port, dataDir).start();
    }
}
